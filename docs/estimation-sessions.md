# Collaborative estimation sessions

A collaborative estimation session runs a **Wideband-Delphi** style, two-phase
group estimate over the leaf items of an estimation's draft. One participant is
the **moderator** (the creator); the others are **estimators**. Every change is
made through the REST control plane and pushed to all joined clients over a
push-only WebSocket, so everyone sees the same live state.

The session votes/finalize on the **THREE_POINT_PERT** shape (optimistic /
likely / pessimistic). The bucket+sampled method votes differently and extends
storage separately (task-106).

## Roles and lifecycle

- **Moderator** — creates the session, starts it, edits the shared discussion
  notes, reveals phase 2, and finalizes each item. Whether the moderator **also
  estimates** is chosen at creation (`moderatorEstimates`, default on): when on,
  the moderator submits a blind triple in phase 1 and can revise it in phase 2
  like any estimator; when off the moderator only moderates (the estimate/revise
  forms are hidden, they are excluded from the submitted/total denominator, and
  the backend rejects a vote from them with `409`).
- **Estimator** — joins the room, submits a blind estimate in phase 1, and in
  phase 2 may submit a revised estimate and mark "I agree".

Coarse session status: `CREATED → RUNNING → FINALIZED` (or `CANCELLED`). Within
`RUNNING`, each item walks its own status `PHASE1 → PHASE2 → FINALIZED`; the
session tracks `currentItemIndex` + `currentPhase`. When the last item is
finalized the session becomes `FINALIZED`.

## The two phases

**Phase 1 — blind individual estimate.** Each participant enters a PERT triple
and submits it. Nobody sees anyone else's numbers; the only shared signal is the
submitted/total count. The moderator can capture discussion notes (visible to
everyone) and, when ready, **reveals** phase 2.

**Phase 2 — reveal and converge.** Everyone now sees the table of submitted
estimates and the **aggregate**:

- the mean triple and the PERT mean, and
- the **spread**: range, standard deviation, and coefficient of variation (CV).

When the estimates diverge strongly (CV above the divergence threshold), the UI
highlights the aggregate with an amber banner prompting discussion and revision.
Estimators may submit a revised triple and toggle "I agree"; the moderator sees
who has agreed and can **finalize** the item.

### Single source of truth for mean/spread

The mean and spread are **never** computed ad-hoc in the backend service or the
frontend component. They come from the domain `VoteAggregation` (see
`src/domain/.../model/VoteAggregation.kt`, task-062), which is the one
implementation both sides call:

- the backend service reduces the effective votes with `VoteAggregation.aggregate`
  and serialises the result as `AggregateDto`;
- the frontend computes the **same** aggregate locally for snappy display, via
  the JS-friendly `aggregateVotes(Array<EstimatorVote>)` factory in
  `DomainFactory.kt` (the same Kotlin/JS-bundle pattern `adapter.ts` uses).

Because both paths run identical domain code, the locally-displayed mean matches
the backend `AggregateDto` exactly — the end-to-end test asserts this.

The **divergence threshold** is `VoteAggregation.DIVERGENCE_CV_THRESHOLD = 0.20`
(a CV above 0.20 on the expected values sets the aggregate's `diverged` flag).

## Finalize → write-back to the draft

When the moderator finalizes an item, the aggregated mean triple is written back
onto the matching draft leaf **through the normal draft-update path**
(`DraftUpdateApplier`), which records an entry in the undo log (task-076). The
write-back is therefore undoable and behaves exactly like a manual edit — no
ad-hoc SQL. The item's discussion notes, if any, are **appended to that leaf's
`assumptions`** in the same write (prefixed with the session name, e.g.
`"<session title>: <notes>"`, newline-separated after any existing assumptions),
so the discussion survives the session. The moderator may edit the notes in both
phase 1 and phase 2. After the last item, the session is `FINALIZED` and the
draft now carries every finalized estimate; the FINALIZED summary links back to
the estimation so the user can open the draft editor.

## Realtime / ws-ticket model

Mutations go **only** through the REST endpoints under `/api/sessions`; the
WebSocket at `/ws/sessions/{id}` is **push-only** and broadcasts the fresh
`SessionDto` to every joined connection on every change. Because a browser
cannot attach an `Authorization` header to a WS handshake, a joined participant
first `POST`s `/api/sessions/{id}/ws-ticket` to mint a short-lived, single-use
opaque ticket, then opens `…/ws/sessions/{id}?ticket=…`. See the "Realtime
session channel" note in `CLAUDE.md` (task-065) for the server-side detail.

## Frontend layout

- Route group `src/frontend/src/routes/sessions/` with its own room chrome (the
  standard app header is hidden for `/sessions`).
- `sessions/+page.svelte` — moderator setup (pick project → offer, choose leaf
  items, name, create). It can be launched **directly from the active offer**:
  the estimation detail page's "Start estimation session" button links here with
  `?estimationId=&projectId=`, which pre-selects the offer and jumps straight to
  the item picker. The picker default-checks only the **not-yet-estimated**
  leaves (PERT triple all null-or-0) — the ones a session is convened to
  estimate — badging the already-estimated ones, and offers select-all /
  deselect-all. The page also shows an **open-sessions** list at the top (all
  joinable CREATED/RUNNING sessions across every estimation) so anyone can find
  and **join** a running session without re-deriving the moderator's offer.
- `sessions/[id]/+page.svelte` — the room; picks the panel by
  status/phase and auto-joins as an estimator.
- `$lib/session/PhaseOnePanel.svelte`, `PhaseTwoPanel.svelte`,
  `SessionSummary.svelte` — the phase panels.
- `$lib/session/{api,socket,store.svelte}.ts` — REST wrappers, the socket
  client, and the live `SessionStore`.

All numbers render through `$lib/format.ts` (locale-aware), all strings through
the `session.*` svelte-i18n keys (`de.json` / `en.json`), and every error path
both logs via `$lib/log.ts` and surfaces through `ErrorBanner`.

## End-to-end test

`src/frontend/e2e/session.test.ts` drives the full flow with **two browser
contexts** (moderator `dev-admin` + estimator `dev-estimator`): it proves the
socket broadcast (participant list mirrors in both), the blind phase-1 count,
the phase-2 reveal with the domain aggregate matching the backend `AggregateDto`,
the divergence highlight, agree + finalize, the FINALIZED summary, and that the
finalized triples were written back onto the draft leaves.
