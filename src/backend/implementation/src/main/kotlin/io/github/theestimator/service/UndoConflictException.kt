package io.github.theestimator.service

import io.github.theestimator.domain.draft.DraftMutationLogEntry

// Thrown when an undo/redo would clobber a newer change on the same draft. The
// blocking entry (the change that got in the way) is carried so the REST layer
// (task-075) can render a 409 with conflict details.
class UndoConflictException(val blockingEntry: DraftMutationLogEntry) :
    RuntimeException("Undo/redo blocked by a newer change (entry ${blockingEntry.id})")
