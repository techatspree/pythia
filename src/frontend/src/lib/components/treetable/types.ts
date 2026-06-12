import type { Snippet } from 'svelte';

export type TreeColumn<T> = {
	key: string;
	header: string;
	width: string;
	align?: 'left' | 'right' | 'center';
	cell: Snippet<[T, TreeNodeContext<T>]>;
	/** When true, the TreeTable auto-collapses this column when its
	 *  container is narrower than `TreeTableProps.collapseBreakpointPx`.
	 *  Default false. */
	collapsible?: boolean;
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
	rowAttrs?: (node: T, ctx: TreeNodeContext<T>) => Record<string, string>;
	childrenZoneAttrs?: (parent: T | null) => Record<string, string>;
	/** Container width (px) below which columns marked `collapsible`
	 *  are hidden. Default 900. */
	collapseBreakpointPx?: number;
	footer?: Snippet<[T[]]>;
	initialCollapsed?: Set<string>;
};
