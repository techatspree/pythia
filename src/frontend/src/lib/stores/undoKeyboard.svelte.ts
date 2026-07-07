import type { UndoStore } from './undo.svelte';

function isTextEntry(el: HTMLElement): boolean {
	const tag = el.tagName;
	return tag === 'INPUT' || tag === 'TEXTAREA' || el.isContentEditable;
}

// Install global undo/redo keyboard shortcuts driving `store`; returns a
// cleanup fn that removes the listener. Ctrl/Cmd+Z = undo,
// Ctrl/Cmd+Shift+Z or Ctrl+Y = redo. A shortcut fired inside a text field that
// is NOT part of the grid's undo surface (grid cells carry
// data-undo-aware="true") is left to the browser's native input undo.
export function installUndoShortcuts(store: UndoStore): () => void {
	function handler(e: KeyboardEvent): void {
		const mod = e.ctrlKey || e.metaKey;
		const key = e.key.toLowerCase();
		if (!mod || (key !== 'z' && key !== 'y')) return;

		const target = e.target;
		if (
			target instanceof HTMLElement &&
			isTextEntry(target) &&
			target.closest('[data-undo-aware="true"]') == null
		) {
			return;
		}

		const isRedo = key === 'y' || (key === 'z' && e.shiftKey);
		if (isRedo) {
			if (!store.canRedo) return;
			e.preventDefault();
			void store.redo();
		} else {
			if (!store.canUndo) return;
			e.preventDefault();
			void store.undo();
		}
	}

	window.addEventListener('keydown', handler);
	return () => window.removeEventListener('keydown', handler);
}
