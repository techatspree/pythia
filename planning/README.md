# Estimation Tool — Executable Development Plan

This directory contains the development plan for the project effort estimation tool, structured so it can be executed step-by-step. Each task is a self-contained YAML file with prerequisites, steps, validation criteria, and outputs. A small Python executor tracks progress and tells you what to work on next.

## Directory layout

```
estimation-tool-plan/
├── README.md              # This file
├── plan.yaml              # High-level plan: stack, domain, phases, task index
├── status.json            # Mutable progress tracker (modified by the executor)
├── tasks/                 # 32 task definitions, one YAML file each
│   ├── task-001.yaml      # Initialize repository structure
│   ├── task-002.yaml      # Bootstrap Quarkus backend
│   └── ...
├── templates/             # (Reserved for code/config templates)
└── scripts/
    └── executor.py        # Plan executor CLI
```

## How to execute the plan

The executor reads task files and `status.json` and helps you walk through the work in dependency order.

```
# See overall progress
./scripts/executor.py status

# Find the next task you can start (deps satisfied, status = pending)
./scripts/executor.py next

# Read the full definition of a task
./scripts/executor.py show task-002

# Mark a task as in progress when you start working
./scripts/executor.py start task-002

# Mark a task done after its validation criteria pass
./scripts/executor.py done task-002

# Mark a task blocked with a reason (e.g. waiting on Entra ID admin)
./scripts/executor.py block task-005 "Awaiting tenant admin consent"

# Reset a task back to pending (e.g. need to redo it)
./scripts/executor.py reset task-002
```

Typical flow: run `next`, run `show` to read the task, do the work, run `done`. Repeat.

## Phase summary

| Phase | Weeks | Theme | Tasks |
|-------|-------|-------|-------|
| 1 | 1 | Foundation and local environment | 001–004 |
| 2 | 1 | Modular Authentication and Authorization | 005–008, 060, 068, 120 |
| 3 | 1 | Core domain and persistence | 009–012 |
| 4 | 1 | REST API and business logic | 013–016 |
| 5 | 2 | Frontend application | 017–021 |
| 6 | 1 | Containerization and Kubernetes | 022–025 |
| 7 | 1 | Observability, CI/CD, hardening | 026–029 |
| 8 | 1 | Polish and production readiness | 030–032 |
| 9 | 1 | Tree-shaped Estimation Hierarchy | 049–055, 057–059, 061, 069–070 |
| 10 | 1 | Code Quality and Static Analysis | 056, 071, 078, 084–086 |
| 11 | 2 | Collaborative Estimation Sessions | 062–067 |
| 12 | 2 | Persistent Undo Across Sessions | 072–077, 109–110 |
| 13 | 3 | Excel-aligned Domain, KMP & Build Reactor | 033–048 |
| 14 | 3 | Post-MVP Hardening & Build Modernization | 079–083, 087–095, 112, 114, 117–119 |
| 15 | 4 | Pluggable Estimation Methods | 096 (planning), 097–108, 113, 115–116 |
| 16 | 2 | Internationalization | 111 (planning), 121–127 |

Total: roughly 10 weeks of focused work for one developer, less with parallel tracks.

## Anatomy of a task file

Each task in `tasks/` follows the same shape:

- **id** — stable identifier (`task-NNN`)
- **phase** — which phase it belongs to
- **title** — one-line summary
- **depends_on** — list of task IDs that must be done first
- **description** — the why and what
- **steps** — concrete actions to take
- **validation** — how you know it worked
- **outputs** — the artifacts produced

Tasks are deliberately written at a level that a developer (or another Claude session) can pick up and execute without needing to re-derive the design. Where decisions are open, the task names them explicitly so they get made consciously rather than by accident.

## Modifying the plan

The plan is a starting point, not a contract. If you discover a task should be split, merged, reordered, or replaced:

1. Edit or add the task YAML file in `tasks/`
2. Update `plan.yaml` if phases or task lists change
3. Update `status.json` to add/remove the corresponding entry
4. Commit the change with a note explaining why

Treat the plan like any other code: review changes, keep history, prefer small commits.
