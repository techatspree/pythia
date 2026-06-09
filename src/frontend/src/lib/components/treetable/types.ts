import type { Snippet } from 'svelte';

export type TreeColumn<T> = {
	key: string;
	header: string;
	width: string;
	align?: 'left' | 'right' | 'center';
	cell: Snippet<[T, TreeNodeContext<T>]>;
};

export type TreeNodeContext<T> = {
	depth: number;
	path: number[];
	isGroup: boolean;
	expanded: boolean;
	toggle: () => void;
};

export type ChildrenChangeEvent<T> = {
	parentPath: number[];
	newChildren: T[];
	phase: 'consider' | 'finalize';
};

export type TreeTableProps<T> = {
	roots: T[];
	columns: TreeColumn<T>[];
	getId: (node: T) => string;
	getChildren: (node: T) => T[] | null;
	treeColumnKey?: string;
	editable?: boolean;
	onChildrenChange?: (e: ChildrenChangeEvent<T>) => void;
	rowActions?: Snippet<[T, TreeNodeContext<T>]>;
	footer?: Snippet<[T[]]>;
	initialCollapsed?: Set<string>;
};
