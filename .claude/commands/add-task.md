Create a new planning task for the problem or feature described in $ARGUMENTS. Write the task file to `planning/tasks/` and register it in `planning/status.json`.

## Protocol

### 1. Determine the next task ID

Read `planning/status.json` and find the highest numeric task ID currently present. The new task ID is that number + 1, zero-padded to three digits (e.g. if the highest is `task-055`, the new one is `task-056`).

### 2. Read planning context

Read these files in parallel before writing anything:

- `planning/plan.yaml` — phases, tech stack, domain entities
- `planning/status.json` — existing tasks and their statuses
- Any `CLAUDE.md` at the project root or module level (`find . -maxdepth 3 -name CLAUDE.md -not -path "*/node_modules/*"`)
- The last 2–3 task YAML files (highest IDs) to understand the current frontier of the plan

### 3. Understand the problem

Read the problem statement in $ARGUMENTS. If the problem is vague, use the codebase to sharpen it:

- Grep for relevant class, file, or symbol names mentioned
- Read affected source files to understand what currently exists
- Identify which modules are involved: `src/domain/`, `src/backend/`, `src/frontend/`, `k8s/`, `planning/`

Do NOT start writing the task file until you have a concrete understanding of what must change and why.

### 4. Determine phase and dependencies

- Find the most appropriate existing phase in `plan.yaml`. If the work fits within an existing phase, use that phase ID. If it clearly represents new work beyond the current plan, note that and suggest adding a new phase (but still proceed with the task).
- Set `depends_on` to the task IDs that must complete before this one can start. At minimum, the most recently completed or the last task in the same workstream. If this task touches the KMP domain, it must depend on the last task that touched the domain. If it touches the backend API, it must depend on the last task that changed the REST layer.
- If there are no meaningful dependencies (e.g. a documentation-only task), set `depends_on: []`.

### 5. Write the task YAML

Create `planning/tasks/<new-id>.yaml` following this structure exactly:

```yaml
id: <new-id>
phase: <phase-id>
title: <concise one-line summary, max 60 characters>
depends_on: [<dep-id>, ...]
description: |
  <Explain WHY this task is needed and WHAT it achieves. Include:
  - The current state (what exists, what's broken or missing)
  - The target state after the task
  - Key design decisions already made — leave no ambiguity for
    the implementer. If it touches the KMP domain, explain the
    Kotlin/JS constraints relevant to this specific change.
  - If it touches multiple modules, describe the interface
    between them clearly.
  Do NOT describe how to implement — that goes in steps.>

steps:
  - |
    <Each step is a concrete, ordered action. Use imperative form.
    Include exact file paths, class names, and method signatures
    where they are known. Each step should be independently
    completable and verifiable.>
  - |
    <If the task touches the KMP domain, include a step to build
    and publish the domain module:
      ./mvnw -pl src/domain -am test
    and (if the frontend uses the domain):
      cd src/frontend && npm run check>
  - |
    <The final step must be the build/test command appropriate to
    the scope:
    - Backend changes:  ./mvnw -pl src/backend/implementation -am test
    - Domain changes:   ./mvnw -pl src/domain -am test
    - Frontend changes: cd src/frontend && npm run check
    - Full stack:       ./mvnw test (then npm run check)>

validation:
  - |
    <Each validation item is either a shell command that returns
    exit 0 on success, or a grep/test assertion.
    Always include at least one structural check (file exists,
    class/method is present) and at least one build/test check.
    Example:
      test -f src/backend/implementation/src/main/kotlin/.../Foo.kt
      grep -q "class Foo" src/backend/implementation/src/main/kotlin/.../Foo.kt
      ./mvnw -pl src/backend/implementation -am test>

outputs:
  - <created|modified|deleted>: <path>
  - ...
```

**Style rules:**

- Write steps at the level of a developer who knows the stack but has not read the code yet — precise enough to follow without guessing, not so detailed that every line is prescribed.
- Use the exact Kotlin/JS patterns already established in the codebase where relevant (underscore-forwarding constructors, `@JsExport @DomainEntity`, `get() =` lazy properties on data classes, the `leaves()` extension pattern).
- If the task adds a REST endpoint, specify: HTTP method, path, request/response body shape, and which JAX-RS resource class it goes into.
- If the task adds a Flyway migration, specify the migration file name (`V<N>__<description>.sql`) and the key DDL changes.
- If the task touches the KMP domain, flag the super-property pitfall: `super.<computed prop>` causes infinite recursion on Kotlin/JS — inline the parent formula instead.
- Outputs must list every file the steps create, modify, or delete. Include test files.

### 6. Update status.json

Add the new task entry to `planning/status.json` inside the `"tasks"` object:

```json
"<new-id>": {
  "status": "pending",
  "started_at": null,
  "completed_at": null,
  "notes": ""
}
```

Use `python3` to read, update, and write status.json so the JSON stays valid and existing entries are preserved:

```bash
python3 - <<'EOF'
import json, sys
with open('planning/status.json', 'r') as f:
    d = json.load(f)
d['tasks']['<new-id>'] = {
    'status': 'pending',
    'started_at': None,
    'completed_at': None,
    'notes': ''
}
with open('planning/status.json', 'w') as f:
    json.dump(d, f, indent=2)
    f.write('\n')
print('status.json updated')
EOF
```

### 7. Report

One short paragraph: the new task ID and title, which phase and dependencies it was assigned, and a one-sentence summary of what it will do. Then offer to run `/improve-task <new-id>` to validate the task before it is implemented.

## Hard rules

- Never reuse an existing task ID, even for a deleted or skipped task.
- Never edit existing task YAML files.
- Never change the `status`, `started_at`, or `completed_at` of any existing entry in `status.json`.
- If $ARGUMENTS is too vague to write concrete steps and validation, ask one focused clarifying question before writing anything.
- The task must be implementable directly by `/implement-task <new-id>` without any further conversation.
