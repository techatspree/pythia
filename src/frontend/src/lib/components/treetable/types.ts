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

// `T` is kept for API symmetry with the other generic TreeTable types
// (TreeColumn<T>, TreeTableProps<T>, …) and the call sites that pass it; the
// context shape itself happens not to reference the node type today.
// eslint-disable-next-line @typescript-eslint/no-unused-vars
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
	/** Where the `rowActions` snippet renders. `'trailing'` (default)
	 *  reserves a dedicated 4rem column at the far right of every row.
	 *  `'treeColumn'` inlines the actions inside the tree column, right
	 *  after the cell content — no trailing column is reserved. */
	actionsPlacement?: 'trailing' | 'treeColumn';
	rowAttrs?: (node: T, ctx: TreeNodeContext<T>) => Record<string, string>;
	childrenZoneAttrs?: (parent: T | null) => Record<string, string>;
	/** Container width (px) below which columns marked `collapsible`
	 *  are hidden. Default 900. */
	collapseBreakpointPx?: number;
	footer?: Snippet<[T[]]>;
	/** Whether a row starts collapsed. A RULE, not a one-time seed: it is
	 *  consulted for every row the user has not toggled, so a row that only
	 *  appears later obeys it too. Expand/collapse itself stays private to the
	 *  TreeTable — the caller states the default, the user overrides it.
	 *  Default: everything expanded. */
	defaultCollapsed?: (node: T) => boolean;
};
