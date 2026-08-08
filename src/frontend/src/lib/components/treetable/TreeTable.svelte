<script lang="ts" module>
	import { overrideItemIdKeyNameBeforeInitialisingDndZones } from 'svelte-dnd-action';
	overrideItemIdKeyNameBeforeInitialisingDndZones('__treeTableId');
</script>

<script lang="ts" generics="T">
	import { dndzone } from 'svelte-dnd-action';
	import { SvelteMap, SvelteSet } from 'svelte/reactivity';
	import type { TreeNodeContext, TreeTableProps } from './types';

	let {
		roots = $bindable<T[]>([]),
		columns,
		getId,
		getChildren,
		treeColumnKey = columns[0].key,
		editable = true,
		onChildrenChange,
		rowActions,
		actionsPlacement = 'trailing',
		rowAttrs,
		childrenZoneAttrs,
		collapseBreakpointPx = 900,
		footer,
		defaultCollapsed = () => false
	}: TreeTableProps<T> & { roots?: T[] } = $props();

	// Only the rows the user has EXPLICITLY toggled, id → expanded?. Everything
	// else falls through to the caller's `defaultCollapsed` rule, so the prop
	// stays live (no snapshot of it is taken) while the user's own choices still
	// win. A row that only shows up later — a bucket added after mount — obeys
	// the rule without any re-seeding.
	const expandedOverrides = new SvelteMap<string, boolean>();
	let preDragSnapshot: T[] | null = null;
	let cycleCheckPending = false;

	// Live drag buffers, keyed by the zone's parent path (`a-b-c`, root = `''`).
	// While a pointer drag is in flight svelte-dnd-action inserts a shadow
	// placeholder (id `id:dnd-shadow-placeholder-0000`) into the `consider`
	// items, and it requires us to hand that exact array back to the zone so the
	// shadow identity survives. We therefore render the affected zone(s) straight
	// from these buffers during the drag and only commit unwrapped nodes to the
	// real model on `finalize`. (Unwrapping + re-wrapping on every `consider`
	// regenerates the shadow's id and breaks the drag.)
	const dragItems = new SvelteMap<string, Wrapped[]>();

	// Ids of the node currently being dragged plus all of its descendants. While
	// a group is in flight, its own subtree zones must refuse the drop, otherwise
	// the group would be dropped into itself (a cycle). Empty when idle.
	const draggedSubtreeIds = new SvelteSet<string>();

	// True while a pointer/keyboard drag is in flight (the set above is filled on
	// the first `consider` and cleared on finalize). Used to give collapsed rows
	// a hittable drop strip only when it can actually be used.
	const dragActive = $derived(draggedSubtreeIds.size > 0);

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
		// Transient local for duplicate detection — never reactive state, so a
		// plain Set is correct (SvelteSet would add needless signal overhead).
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
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

	// Commit a zone's children to the model, keyed by the OWNER NODE (null = root)
	// rather than an index path: a multi-zone finalize removes the dragged node
	// from one zone, which would shift the index path of the other zone — owner
	// identity is stable across that.
	function applyChildren(owner: T | null, newChildren: T[]) {
		if (owner == null) {
			roots = newChildren;
		} else {
			const kids = getChildren(owner);
			if (kids == null) return;
			kids.length = 0;
			for (const c of newChildren) kids.push(c);
		}
	}

	function zoneKeyOf(owner: T | null): string {
		return owner == null ? '__root__' : getId(owner);
	}

	function collectSubtreeIds(node: T, into: Set<string>) {
		into.add(getId(node));
		const kids = getChildren(node);
		if (kids != null) for (const k of kids) collectSubtreeIds(k, into);
	}

	function findNodeById(nodes: T[], id: string): T | null {
		for (const n of nodes) {
			if (getId(n) === id) return n;
			const kids = getChildren(n);
			if (kids != null) {
				const found = findNodeById(kids, id);
				if (found != null) return found;
			}
		}
		return null;
	}

	// Items a zone should render: its live drag buffer while a drag touches it,
	// otherwise the wrapped model children.
	function itemsForZone(owner: T | null, children: T[]): Wrapped[] {
		return dragItems.get(zoneKeyOf(owner)) ?? children.map(wrap);
	}

	function handleZoneEvent(
		parentPath: number[],
		owner: T | null,
		e: CustomEvent<{ items: Wrapped[]; info: { trigger?: string; id?: string } }>
	) {
		const eventType = (e.type as 'consider' | 'finalize') ?? 'consider';
		if (eventType === 'consider') {
			// The keyboard drop sequence ends with a `dragStopped` consider AFTER the
			// move's finalize already ran. Re-applying its (stale) items here would
			// undo a cycle revert, so treat it purely as end-of-drag cleanup.
			if (e.detail.info?.trigger === 'dragStopped') {
				dragItems.clear();
				draggedSubtreeIds.clear();
				return;
			}
			if (preDragSnapshot === null) {
				preDragSnapshot = deepClone(roots);
			}
			// Identify the dragged node (works for both pointer and keyboard, which
			// carry the dragged id in `info.id`) and remember its whole subtree, so
			// its own zones can refuse the drop — cycle prevention via
			// dropFromOthersDisabled below (effective for pointer drags).
			if (draggedSubtreeIds.size === 0) {
				const draggedId = e.detail.info?.id;
				const dragged = draggedId != null ? findNodeById(preDragSnapshot ?? roots, draggedId) : null;
				if (dragged != null) {
					const ids = new Set<string>();
					collectSubtreeIds(dragged, ids);
					for (const id of ids) draggedSubtreeIds.add(id);
				}
			}
			// Render this zone straight from the library's array (incl. the shadow
			// placeholder) so the shadow's id survives the drag — re-wrapping the
			// model would regenerate it and break the drag. The model is still kept
			// in sync below so totals/consumers update live during the drag.
			const newChildren = unwrap(e.detail.items);
			dragItems.set(zoneKeyOf(owner), e.detail.items);
			applyChildren(owner, newChildren);
			onChildrenChange?.({ parentPath, newChildren, phase: 'consider' });
		} else {
			const newChildren = unwrap(e.detail.items);
			// The drag is ending: drop all live buffers so every zone reverts to the
			// model, then commit this zone's final order to the model.
			dragItems.clear();
			applyChildren(owner, newChildren);
			if (cycleCheckPending) return;
			cycleCheckPending = true;
			queueMicrotask(() => {
				// Reject structurally invalid results (e.g. a group dropped into its
				// own descendant via keyboard, which dropFromOthersDisabled does not
				// cover): restore the pre-drag snapshot.
				if (preDragSnapshot !== null && isStructuralAnomaly(roots, preDragSnapshot)) {
					roots = preDragSnapshot;
				} else {
					onChildrenChange?.({ parentPath, newChildren, phase: 'finalize' });
				}
				preDragSnapshot = null;
				cycleCheckPending = false;
				draggedSubtreeIds.clear();
			});
		}
	}

	function isExpanded(node: T): boolean {
		return expandedOverrides.get(getId(node)) ?? !defaultCollapsed(node);
	}

	function toggle(node: T) {
		expandedOverrides.set(getId(node), !isExpanded(node));
	}

	function makeCtx(node: T, depth: number, path: number[], isGroup: boolean): TreeNodeContext<T> {
		return {
			depth,
			path,
			isGroup,
			expanded: isExpanded(node),
			toggle: () => toggle(node)
		};
	}

	const wrappedRoots = $derived(itemsForZone(null, roots));

	const gridTemplateColumns = $derived(
		(editable ? '2rem ' : '') +
			columns.map((c) => (isCollapsed(c.key) ? '0.5rem' : c.width)).join(' ') +
			(rowActions && editable && actionsPlacement === 'trailing' ? ' 4rem' : '')
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
	{#each Array(d) as _, i (i)}
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

{#snippet renderRow(node: T, path: number[], depth: number, hidden: boolean)}
	{@const kids = getChildren(node)}
	{@const isGroup = kids != null}
	{@const ctx = makeCtx(node, depth, path, isGroup)}
	{@const extraAttrs = rowAttrs ? rowAttrs(node, ctx) : {}}
	<!-- A collapsed row's descendants stay in the DOM (see the zone below) but
	     are collapsed to nothing. `visibility: hidden` rather than
	     `display: none` on purpose: it keeps a layout box, which the drag
	     library needs to measure, while still removing the subtree from the tab
	     order so hidden inputs are not keyboard-reachable. -->
	<div
		data-testid="tt-row-{getId(node)}"
		class:h-0={hidden}
		class:overflow-hidden={hidden}
		class:invisible={hidden}
		{...extraAttrs}
	>
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
				{#if col.key === treeColumnKey && actionsPlacement === 'treeColumn'}
					<div class="py-1 px-2 flex items-center gap-1 min-w-0 {alignClass(col.align)}">
						{#if !isCollapsed(col.key)}
							{@render indent(depth)}
							{@render chevron(ctx)}
							{@render col.cell(node, ctx)}
							{#if rowActions && editable}
								{@render rowActions(node, ctx)}
							{/if}
						{/if}
					</div>
				{:else}
					<div class="py-1 px-2 {alignClass(col.align)}">
						{#if !isCollapsed(col.key)}
							{#if col.key === treeColumnKey}
								{@render indent(depth)}
								{@render chevron(ctx)}
							{/if}
							{@render col.cell(node, ctx)}
						{/if}
					</div>
				{/if}
			{/each}
			{#if rowActions && editable && actionsPlacement === 'trailing'}
				<div class="py-1 px-2">
					{@render rowActions(node, ctx)}
				</div>
			{/if}
		</div>

		{#if isGroup}
			{@const wrappedChildren = itemsForZone(node, kids as T[])}
			{@const childZoneAttrs = childrenZoneAttrs
				? childrenZoneAttrs(node)
				: { 'aria-label': 'Children' }}
			<!-- The zone is rendered even when the row is COLLAPSED (its children
			     are then hidden by renderRow above). svelte-dnd-action captures its
			     drop zones AT DRAG START — `watchDraggedElement` instruments exactly
			     the zones registered at that moment — so a zone that only appears
			     once a drag is under way can never receive the drop. Rendering it
			     always is what makes a collapsed group/bucket a valid target, and
			     keeping the child rows in the DOM keeps `items` and the DOM in
			     one-to-one correspondence, so the replace semantics and the cycle
			     protection are unchanged.
			     Heights: an EMPTY zone would collapse to 0px and, with
			     `useCursorForDetection`, could never be hovered; a COLLAPSED zone
			     has the same problem but only matters mid-drag, so it claims its
			     strip only then. -->
			<div
				class:min-h-8={wrappedChildren.length === 0 || (!ctx.expanded && dragActive)}
				use:dndzone={{
					items: wrappedChildren,
					type: 'tree-table-node',
					flipDurationMs: 200,
					dropTargetStyle: {},
					dragDisabled: !editable,
					dropFromOthersDisabled: draggedSubtreeIds.has(getId(node)),
					useCursorForDetection: true
				}}
				onconsider={(e) => handleZoneEvent(path, node, e)}
				onfinalize={(e) => handleZoneEvent(path, node, e)}
				{...childZoneAttrs}
			>
				{#each wrappedChildren as w, idx (w.__treeTableId)}
					{@render renderRow(w.node, [...path, idx], depth + 1, hidden || !ctx.expanded)}
				{/each}
			</div>
		{/if}
	</div>
{/snippet}

<div class="border rounded-lg overflow-hidden">
	<div class="overflow-x-auto" bind:this={scrollHost}>
		<div style="min-width: max-content">
			<!-- Normal-case on purpose: `uppercase tracking-wide` cost ~20-25% extra
			     width for no legibility gain, which is what pushed labels out of
			     their tracks. `items-end` keeps a wrapped two-line label on the same
			     baseline as its single-line neighbours. -->
			<div
				data-testid="tt-header"
				class="grid items-end bg-brand-green/10 border-b text-xs font-semibold text-brand-green"
				style="grid-template-columns: {gridTemplateColumns}"
			>
				{#if editable}
					<div class="py-2 px-1"></div>
				{/if}
				{#each columns as col (col.key)}
					<!-- A single long word cannot wrap, so without `min-w-0` +
					     `overflow-hidden` it overflows this grid item and paints over
					     the neighbouring header — the overlap this guards against. -->
					<div
						class="py-2 px-2 min-w-0 overflow-hidden whitespace-normal break-words leading-tight {alignClass(
							col.align
						)}"
						title={col.header}
					>
						{isCollapsed(col.key) ? '' : col.header}
					</div>
				{/each}
				{#if rowActions && editable && actionsPlacement === 'trailing'}
					<div class="py-2 px-2"></div>
				{/if}
			</div>

			<div
				use:dndzone={{
					items: wrappedRoots,
					type: 'tree-table-node',
					flipDurationMs: 200,
					dropTargetStyle: {},
					dragDisabled: !editable,
					useCursorForDetection: true
				}}
				onconsider={(e) => handleZoneEvent([], null, e)}
				onfinalize={(e) => handleZoneEvent([], null, e)}
				{...rootZoneAttrs}
			>
				{#each wrappedRoots as w, idx (w.__treeTableId)}
					{@render renderRow(w.node, [idx], 0, false)}
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
