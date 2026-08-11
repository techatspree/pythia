---
name: merlin-integration
description: Merlin Project (.mproject) WBS import and effort export — the SQLite/Core Data file format, the person-day mapping, WBS-path correlation and the 409 structure-drift protocol, and the cluster-only upload-size trap. Use when touching MerlinImporter, MerlinExporter, the /versions/import/merlin or /versions/{n}/export/merlin endpoints, MerlinStructureDialog, or nginx client_max_body_size.
---

# Merlin Project integration

Two endpoints on `EstimationVersionResource`:

- `POST /api/estimations/{eid}/versions/import/merlin` — import a Merlin project WBS as a new draft version (multipart `file`; `409` if a draft already exists, `400` if the upload is unreadable).
- `POST /api/estimations/{eid}/versions/{n}/export/merlin?overwriteStructure=` — write this version's calculated effort back into an uploaded Merlin document and return the modified **copy** (multipart `file`; `409` + `MerlinStructureDiffDto` when the structure drifted, `400` if the upload is unreadable).

## WBS import (task-131)

`MerlinImporter` (`io.pythia.service`, alongside `ExcelImporter`) turns a **Merlin Project** document's Work Breakdown Structure into a new draft version. A `.mproject` is a macOS bundle whose `state.sql` is a **SQLite 3 database** (an Apple Core Data store, `Z`-prefixed tables); activities live in `ZSCHEDULEITEM` (`ZTITLE`, `ZPARENTACTIVITY_` for hierarchy, `ZORDERINPARENTACTIVITY`, `ZISMILESTONE`, `ZGIVENWORK_` = a short string like `"1d"`). Activities are filtered by `ZTITLE IS NOT NULL` (resource-assignment rows have no title; **do not** gate on the version-specific `Z_ENT`).

The upload is accepted either as a **zipped** `.mproject` (the importer finds the `state.sql` entry) or the raw `state.sql`; it is read via a plain `org.xerial:sqlite-jdbc` `DriverManager` connection on a temp file — **not** a Quarkus datasource (the app DB stays PostgreSQL). Work maps to person-days (d×1, w×5, h×0.125) and a single value seeds optimistic=likely=pessimistic (the task's "single work → triple").

Output **preserves the WBS tree (groups + nesting) for both methods**; only the leaf type is method-specific (off `estimation.method`): `THREE_POINT_PERT` → `DraftFixedItemNode`, `BUCKET_SAMPLED_PERT` → `DraftBucketedItemNode` assigned to a single "Imported" bucket (`isSample` = has work). Buckets classify items; they do NOT replace the tree grouping. `EstimationVersionService.importMerlinDraft` guards "one draft at a time" (`409`) and does the version-numbering/persistence like `createDraft`; the frontend control is on the estimation detail page.

## Effort export (task-133)

`MerlinExporter` (same package) is the mirror of the importer — it writes the estimation's results back into a Merlin document. It **only ever edits a COPY**: the uploaded bytes go to a temp file, are modified there, and are streamed back as an attachment download; the user's own `.mproject` is never touched (a zipped upload is repacked with only its `state.sql` entry swapped).

The value written is the calculated **`offerPT`** (mean + risk surcharge) of each **LEAF** — Merlin derives a parent's work from its children, so group activities are left alone — encoded into `ZGIVENWORK_` exactly as Merlin writes it: ASCII `"<days>d"` plus a trailing `0x3F` marker byte (`X'31643F'` is `"1d?"`), with `Z_OPT` bumped on every changed row and `NULL` written for a leaf with no effort.

The version is resolved by the resource's existing `resolveVersion(...)` helper, so `draft` and a submitted number both work and `MerlinExporter` consumes the same `SubmittedEstimationVersion` the xlsx/csv exporters take. The frontend picks that ref itself (`exportVersionRef`): the draft while one exists, otherwise the **highest submitted version** — submitting CONSUMES the draft, so hardcoding `draft` would 404 exactly when the estimate is finished and you want it back in Merlin (the button is disabled when the offer has no version at all).

**Correlation is by WBS path** (titles joined with `" / "`), the only key the import leaves behind — so before writing anything the exporter compares the ordered path lists of both sides and, on drift, throws `MerlinStructureChangedException` → `409` `MerlinStructureDiffDto` (`missingInMerlin` / `missingInEstimation` / `reordered`). The protocol is **stateless**: the frontend re-POSTs the same file with `?overwriteStructure=true` once the user confirms in `MerlinStructureDialog.svelte`.

That overwrite **reconciles by path** rather than wiping and rebuilding — activities that still exist keep their row (and their dates, dependencies and assignments), missing ones are INSERTed, and obsolete ones are DELETEd together with the assignment rows referencing them via `ZACTIVITY_`. New rows take their `Z_PK` from the Core Data `Z_PRIMARYKEY` table using the row for **`Z_ENT = 48`** (`ScheduleItem`, the ROOT of the hierarchy `Activity`=49 and `Assignment`=51 inherit from — the subclass rows carry `Z_MAX = 0` and must not be used) and get a fresh 22-char URL-safe base64 `ZUNIQUEID`.

**A Merlin title may itself contain the `" / "` separator** (the sample has `"… Layer-2 / Layer-3"`), so a node's parent and title are captured during the tree walk and never recovered by splitting the path string — doing that reparents such activities to the root.

## Upload size is a GATEWAY concern, and it is cluster-only

Both Merlin endpoints POST a whole `.mproject` as multipart — real documents are several MB (even the committed `planning/inputdata/MerlinDemoProject.mproject/state.sql` is 1.5 MB). nginx' default `client_max_body_size` is **1m**, so in the cluster a normal-sized Merlin file was rejected with a bare `413` *before* being proxied: nothing reached the backend, nothing appeared in the backend log, and only `kubectl logs deploy/frontend` showed `client intended to send too large body`.

Local dev never reproduces it — the Vite dev proxy imposes no limit — and neither does the Playwright suite, which also goes through Vite; **no test covers the gateway config**. `src/frontend/nginx.conf` therefore sets `client_max_body_size 32m` on `location /api/`, matched by `quarkus.http.limits.max-body-size=32M` (Quarkus' own default is 10M). Keep the two numbers in sync: the smaller one is the effective limit, and raising only one just moves the rejection one hop. A change here lives in the frontend IMAGE, so it needs a rebuild + redeploy, not a `kubectl apply`.
