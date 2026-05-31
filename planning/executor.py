#!/usr/bin/env python3
"""
Plan executor for the estimation-tool project.

Usage:
    ./executor.py status              # show overall progress
    ./executor.py next                # show the next runnable task
    ./executor.py show <task-id>      # show full task definition
    ./executor.py start <task-id>     # mark a task as in_progress
    ./executor.py done <task-id>      # mark a task as done
    ./executor.py block <task-id> <reason>   # mark a task as blocked
    ./executor.py reset <task-id>     # reset a task back to pending
    ./executor.py add-task <title> [--phase <phase>] [--depends-on <id> ...]
                                      # create a new task and register it

A task is "runnable" when its status is pending and all dependencies are done.
"""
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent
STATUS_FILE = ROOT / "status.json"
TASKS_DIR = ROOT / "tasks"


def load_status():
    return json.loads(STATUS_FILE.read_text())


def save_status(status):
    STATUS_FILE.write_text(json.dumps(status, indent=2) + "\n")


def load_task(task_id):
    """Minimal YAML-ish loader. Tasks use simple key: value at top level."""
    path = TASKS_DIR / f"{task_id}.yaml"
    if not path.exists():
        return None
    text = path.read_text()
    task = {"id": task_id, "raw": text}
    for line in text.splitlines():
        if line.startswith("title:"):
            task["title"] = line.split(":", 1)[1].strip()
        elif line.startswith("phase:"):
            task["phase"] = line.split(":", 1)[1].strip()
        elif line.startswith("depends_on:"):
            deps = line.split(":", 1)[1].strip()
            deps = deps.strip("[]").strip()
            task["depends_on"] = [d.strip() for d in deps.split(",") if d.strip()]
    task.setdefault("depends_on", [])
    return task


def all_tasks():
    return sorted(p.stem for p in TASKS_DIR.glob("task-*.yaml"))


def now():
    return datetime.now(timezone.utc).isoformat()


def cmd_status():
    status = load_status()
    counts = {"pending": 0, "in_progress": 0, "blocked": 0, "done": 0, "skipped": 0}
    for t in status["tasks"].values():
        counts[t["status"]] = counts.get(t["status"], 0) + 1
    total = sum(counts.values())
    print(f"Plan progress: {counts['done']}/{total} tasks done")
    for state, count in counts.items():
        if count:
            print(f"  {state:12s} {count}")
    print()
    in_progress = [tid for tid, t in status["tasks"].items() if t["status"] == "in_progress"]
    if in_progress:
        print("In progress:")
        for tid in in_progress:
            task = load_task(tid)
            print(f"  {tid}  {task['title'] if task else ''}")


def cmd_next():
    status = load_status()
    for tid in all_tasks():
        s = status["tasks"][tid]
        if s["status"] != "pending":
            continue
        task = load_task(tid)
        deps = task["depends_on"]
        if all(status["tasks"].get(d, {}).get("status") == "done" for d in deps):
            print(f"Next runnable task: {tid}")
            print(f"  Title: {task['title']}")
            print(f"  Phase: {task.get('phase', '?')}")
            print(f"  Depends on: {deps if deps else 'nothing'}")
            print()
            print(f"Run: ./executor.py show {tid}   to see full details")
            return
    print("No runnable tasks. Either everything is done or remaining tasks are blocked.")


def cmd_show(task_id):
    task = load_task(task_id)
    if not task:
        print(f"Unknown task: {task_id}")
        sys.exit(1)
    status = load_status()
    s = status["tasks"].get(task_id, {})
    print(f"=== {task_id} ===")
    print(f"Status: {s.get('status', '?')}")
    if s.get("started_at"):
        print(f"Started: {s['started_at']}")
    if s.get("completed_at"):
        print(f"Completed: {s['completed_at']}")
    print()
    print(task["raw"])


def cmd_start(task_id):
    status = load_status()
    if task_id not in status["tasks"]:
        print(f"Unknown task: {task_id}")
        sys.exit(1)
    task = load_task(task_id)
    unmet = [d for d in task["depends_on"] if status["tasks"].get(d, {}).get("status") != "done"]
    if unmet:
        print(f"Cannot start {task_id}, unmet dependencies: {unmet}")
        sys.exit(1)
    status["tasks"][task_id]["status"] = "in_progress"
    status["tasks"][task_id]["started_at"] = now()
    if not status.get("started_at"):
        status["started_at"] = now()
    save_status(status)
    print(f"Started {task_id}")


def cmd_done(task_id):
    status = load_status()
    if task_id not in status["tasks"]:
        print(f"Unknown task: {task_id}")
        sys.exit(1)
    status["tasks"][task_id]["status"] = "done"
    status["tasks"][task_id]["completed_at"] = now()
    if not status["tasks"][task_id].get("started_at"):
        status["tasks"][task_id]["started_at"] = now()
    save_status(status)
    print(f"Marked {task_id} as done")


def cmd_block(task_id, reason):
    status = load_status()
    if task_id not in status["tasks"]:
        print(f"Unknown task: {task_id}")
        sys.exit(1)
    status["tasks"][task_id]["status"] = "blocked"
    status["tasks"][task_id]["notes"] = reason
    save_status(status)
    print(f"Marked {task_id} as blocked: {reason}")


def cmd_reset(task_id):
    status = load_status()
    if task_id not in status["tasks"]:
        print(f"Unknown task: {task_id}")
        sys.exit(1)
    status["tasks"][task_id] = {"status": "pending", "started_at": None, "completed_at": None, "notes": ""}
    save_status(status)
    print(f"Reset {task_id} to pending")


def cmd_add_task(title, phase="phase-4", depends_on=None):
    depends_on = depends_on or []

    # Determine the next task number.
    existing = sorted(TASKS_DIR.glob("task-*.yaml"))
    if existing:
        last_num = max(int(p.stem.split("-")[1]) for p in existing)
    else:
        last_num = 0
    new_num = last_num + 1
    task_id = f"task-{new_num:03d}"

    # Render the YAML stub.
    deps_str = "[" + ", ".join(depends_on) + "]"
    yaml_content = f"""\
id: {task_id}
phase: {phase}
title: {title}
depends_on: {deps_str}
description: |
  TODO: describe what this task should accomplish.

steps:
  - |
    TODO: add implementation steps.

validation:
  - |
    TODO: add validation commands.

outputs: []
"""
    task_path = TASKS_DIR / f"{task_id}.yaml"
    task_path.write_text(yaml_content)

    # Register in status.json.
    status = load_status()
    status["tasks"][task_id] = {"status": "pending", "started_at": None, "completed_at": None, "notes": ""}
    save_status(status)

    print(f"Created {task_id}: {title}")
    print(f"  File:    planning/tasks/{task_id}.yaml")
    print(f"  Phase:   {phase}")
    print(f"  Depends: {depends_on if depends_on else 'nothing'}")


def parse_add_task_args(args):
    """Parse add-task arguments: <title> [--phase <p>] [--depends-on <id> ...]"""
    if not args:
        print("Usage: ./executor.py add-task <title> [--phase <phase>] [--depends-on <id> ...]")
        sys.exit(1)
    title = args[0]
    rest = args[1:]
    phase = "phase-4"
    depends_on = []
    i = 0
    while i < len(rest):
        if rest[i] == "--phase" and i + 1 < len(rest):
            phase = rest[i + 1]
            i += 2
        elif rest[i] == "--depends-on":
            i += 1
            while i < len(rest) and not rest[i].startswith("--"):
                depends_on.append(rest[i])
                i += 1
        else:
            print(f"Unknown option: {rest[i]}")
            sys.exit(1)
    return title, phase, depends_on


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    cmd = sys.argv[1]
    args = sys.argv[2:]
    handlers = {
        "status":   lambda: cmd_status(),
        "next":     lambda: cmd_next(),
        "show":     lambda: cmd_show(args[0]),
        "start":    lambda: cmd_start(args[0]),
        "done":     lambda: cmd_done(args[0]),
        "block":    lambda: cmd_block(args[0], " ".join(args[1:])),
        "reset":    lambda: cmd_reset(args[0]),
        "add-task": lambda: cmd_add_task(*parse_add_task_args(args)),
    }
    if cmd not in handlers:
        print(f"Unknown command: {cmd}")
        print(__doc__)
        sys.exit(1)
    handlers[cmd]()


if __name__ == "__main__":
    main()
