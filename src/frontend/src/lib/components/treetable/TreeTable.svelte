<script lang="ts" module>
	import { overrideItemIdKeyNameBeforeInitialisingDndZones } from 'svelte-dnd-action';
	overrideItemIdKeyNameBeforeInitialisingDndZones('__treeTableId');
</script>

<script lang="ts" generics="T">
	import { dndzone } from 'svelte-dnd-action';
	import type {
		TreeColumn,
		TreeNodeContext,
		TreeTableProps,
		ChildrenChangeEvent
	} from './types';

	let {
		roots = $bindable<T[]>([]),
		columns,
		getId,
		getChildren,
		treeColumnKey = columns[0].key,
		editable = true,
		onChildrenChange,
		rowActions,
		rowAttrs,
		childrenZoneAttrs,
		collapseBreakpointPx = 900,
		footer,
		initialCollapsed = new Set<string>()
	}: TreeTableProps<T> & { roots?: T[] } = $props();

	let collapsed = $state(new Set(initialCollapsed));
	let preDragSnapshot: T[] | null = null;
	let cycleCheckPending = false;

	let scrollHost: HTMLDivElement | null = $state(null);
	let containerWidth = $state(Infinity);

	$effect(() => {
		if (scrollHost == null) return;
		const ro = new ResizeObserver((entries) => {
			containerWidth = entries[0].contentRect.width;
		});
		ro.observe(scrollHost);
		return () => ro.disconnect();
	});

	const isCollapsed = $derived(
		(key: string) =>
			containerWidth < collapseBreakpointPx &&
			(columns.find((c) => c.key === key)?.collapsible ?? false)
	);

	type Wrapped = { __treeTableId: string; node: T };

	function wrap(node: T): Wrapped {
		return { __treeTableId: getId(node), node };
	}

	function unwrap(items: Wrapped[]): T[] {
		return items.map((w) => w.node);
	}

	function deepClone(nodes: T[]): T[] {
		return JSON.parse(JSON.stringify(nodes));
	}

	function collectIds(nodes: T[]): { ids: Set<string>; duplicate: boolean } {
		const ids = new Set<string>();
		let duplicate = false;
		const walk = (n: T) => {
			if (duplicate) return;
			const id = getId(n);
			if (ids.has(id)) {
				duplicate = true;
				return;
			}
			ids.add(id);
			const kids = getChildren(n);
			if (kids != null) kids.forEach(walk);
		};
		nodes.forEach(walk);
		return { ids, duplicate };
	}

	function isStructuralAnomaly(after: T[], before: T[]): boolean {
		const a = collectIds(after);
		if (a.duplicate) return true;
		const b = collectIds(before);
		if (a.ids.size !== b.ids.size) return true;
		for (const id of b.ids) if (!a.ids.has(id)) return true;
		return false;
	}

	function nodeAtPath(path: number[]): T | null {
		if (path.length === 0) return null;
		let current: T[] = roots;
		let node: T | null = null;
		for (const idx of path) {
			node = current[idx] ?? null;
			if (node == null) return null;
			const kids = getChildren(node);
			if (kids != null) current = kids;
		}
		return node;
	}

	function applyChildren(parentPath: number[], newChildren: T[]) {
		if (parentPath.length === 0) {
			roots = newChildren;
		} else {
			const parent = nodeAtPath(parentPath);
			if (parent == null) return;
			const kids = getChildren(parent);
			if (kids == null) return;
			kids.length = 0;
			for (const c of newChildren) kids.push(c);
		}
	}

	function handleZoneEvent(
		parentPath: number[],
		e: CustomEvent<{ items: Wrapped[]; info: { trigger?: string } }>
	) {
		const eventType = (e.type as 'consider' | 'finalize') ?? 'consider';
		const newChildren = unwrap(e.detail.items);
		if (eventType === 'consider') {
			if (preDragSnapshot === null) {
				preDragSnapshot = deepClone(roots);
			}
			applyChildren(parentPath, newChildren);
			onChildrenChange?.({ parentPath, newChildren, phase: 'consider' });
		} else {
			applyChildren(parentPath, newChildren);
			if (cycleCheckPending) return;
			cycleCheckPending = true;
			queueMicrotask(() => {
				if (preDragSnapshot !== null && isStructuralAnomaly(roots, preDragSnapshot)) {
					roots = preDragSnapshot;
				} else {
					onChildrenChange?.({ parentPath, newChildren, phase: 'finalize' });
				}
				preDragSnapshot = null;
				cycleCheckPending = false;
			});
		}
	}

	function toggle(node: T) {
		const id = getId(node);
		const next = new Set(collapsed);
		if (next.has(id)) next.delete(id);
		else next.add(id);
		collapsed = next;
	}

	function makeCtx(node: T, depth: number, path: number[], isGroup: boolean): TreeNodeContext<T> {
		return {
			depth,
			path,
			isGroup,
			expanded: !collapsed.has(getId(node)),
			toggle: () => toggle(node)
		};
	}

	const wrappedRoots = $derived(roots.map(wrap));

	const gridTemplateColumns = $derived(
		(editable ? '2rem ' : '') +
			columns.map((c) => (isCollapsed(c.key) ? '0.5rem' : c.width)).join(' ') +
			(rowActions && editable ? ' 4rem' : '')
	);

	const rootZoneAttrs = $derived(
		childrenZoneAttrs ? childrenZoneAttrs(null) : { 'aria-label': 'Root nodes' }
	);

	function alignClass(a: 'left' | 'right' | 'center' | undefined): string {
		if (a === 'right') return 'text-right';
		if (a === 'center') return 'text-center';
		return 'text-left';
	}
</script>

{#snippet indent(d: number)}
	{#each Array(d) as _}
		<span
			class="inline-block border-l-2 border-gray-300 align-middle"
			style="width: 1.5rem; height: 1.5rem;"
			aria-hidden="true"
		></span>
	{/each}
{/snippet}

{#snippet chevron(ctx: { isGroup: boolean; expanded: boolean; toggle: () => void })}
	{#if ctx.isGroup}
		<button
			type="button"
			tabindex="0"
			class="inline-block w-5 text-gray-400 hover:text-gray-700 cursor-pointer select-none"
			onclick={ctx.toggle}
			onkeydown={(e) => {
				if (e.key === ' ' || e.key === 'Enter') {
					e.preventDefault();
					ctx.toggle();
				}
			}}
			aria-label={ctx.expanded ? 'Collapse' : 'Expand'}
		>
			{ctx.expanded ? '▼' : '▶'}
		</button>
	{:else}
		<span class="inline-block w-5" aria-hidden="true"></span>
	{/if}
{/snippet}

{#snippet renderRow(node: T, path: number[], depth: number)}
	{@const kids = getChildren(node)}
	{@const isGroup = kids != null}
	{@const ctx = makeCtx(node, depth, path, isGroup)}
	{@const extraAttrs = rowAttrs ? rowAttrs(node, ctx) : {}}
	<div data-testid="tt-row-{getId(node)}" {...extraAttrs}>
		<div
			class="grid items-center border-b hover:bg-gray-50"
			style="grid-template-columns: {gridTemplateColumns}"
		>
			{#if editable}
				<div class="py-1 px-1 flex items-center">
					<span
						data-dnd-handle
						class="cursor-grab text-gray-300 hover:text-gray-500 select-none"
						title="Drag to move"
						aria-hidden="true">⋮⋮</span
					>
				</div>
			{/if}
			{#each columns as col (col.key)}
				<div class="py-1 px-2 {alignClass(col.align)}">
					{#if !isCollapsed(col.key)}
						{#if col.key === treeColumnKey}
							{@render indent(depth)}
							{@render chevron(ctx)}
						{/if}
						{@render col.cell(node, ctx)}
					{/if}
				</div>
			{/each}
			{#if rowActions && editable}
				<div class="py-1 px-2">
					{@render rowActions(node, ctx)}
				</div>
			{/if}
		</div>

		{#if isGroup && ctx.expanded}
			{@const wrappedChildren = (kids as T[]).map(wrap)}
			{@const childZoneAttrs = childrenZoneAttrs
				? childrenZoneAttrs(node)
				: { 'aria-label': 'Children' }}
			<div
				use:dndzone={{
					items: wrappedChildren,
					type: 'tree-table-node',
					flipDurationMs: 200,
					dropTargetStyle: {},
					dragDisabled: !editable
				}}
				onconsider={(e) => handleZoneEvent(path, e)}
				onfinalize={(e) => handleZoneEvent(path, e)}
				{...childZoneAttrs}
			>
				{#each wrappedChildren as w, idx (w.__treeTableId)}
					{@render renderRow(w.node, [...path, idx], depth + 1)}
				{/each}
			</div>
		{/if}
	</div>
{/snippet}

<div class="border rounded-lg overflow-hidden">
	<div class="overflow-x-auto" bind:this={scrollHost}>
		<div style="min-width: max-content">
			<div
				class="grid items-center bg-brand-green/10 border-b text-xs font-semibold uppercase tracking-wide text-brand-green"
				style="grid-template-columns: {gridTemplateColumns}"
			>
				{#if editable}
					<div class="py-2 px-1"></div>
				{/if}
				{#each columns as col (col.key)}
					<div class="py-2 px-2 {alignClass(col.align)}" title={col.header}>
						{isCollapsed(col.key) ? '' : col.header}
					</div>
				{/each}
				{#if rowActions && editable}
					<div class="py-2 px-2"></div>
				{/if}
			</div>

			<div
				use:dndzone={{
					items: wrappedRoots,
					type: 'tree-table-node',
					flipDurationMs: 200,
					dropTargetStyle: {},
					dragDisabled: !editable
				}}
				onconsider={(e) => handleZoneEvent([], e)}
				onfinalize={(e) => handleZoneEvent([], e)}
				{...rootZoneAttrs}
			>
				{#each wrappedRoots as w, idx (w.__treeTableId)}
					{@render renderRow(w.node, [idx], 0)}
				{/each}
			</div>

			{#if footer}
				<div class="bg-gray-50 border-t-2 border-gray-300 py-2 px-3">
					{@render footer(roots)}
				</div>
			{/if}
		</div>
	</div>
</div>
