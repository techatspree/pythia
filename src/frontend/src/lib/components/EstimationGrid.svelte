<script lang="ts">
	import {
		dndzone,
		overrideItemIdKeyNameBeforeInitialisingDndZones
	} from 'svelte-dnd-action';
	import EstimationNodeRow, {
		COLS,
		type Node,
		type Leaf,
		type Group,
		type NodePath,
		type CalcEntry,
		type ZoneEvent
	} from './EstimationNodeRow.svelte';

	// svelte-dnd-action uses item.id by default; our nodes use logicalId.
	// Tell the library to look at logicalId instead. This must run ONCE
	// before any dndzone instance is mounted; module scope is the safest spot.
	overrideItemIdKeyNameBeforeInitialisingDndZones('logicalId');

	let {
		version,
		editable,
		onchange,
		calcMap = new Map<string, CalcEntry>(),
		phases = []
	}: {
		version: any;
		editable: boolean;
		onchange: (roots: Node[]) => void;
		calcMap?: Map<string, CalcEntry>;
		phases?: any[];
	} = $props();

	function pert(o: number | null, m: number | null, p: number | null): number {
		return ((o ?? 0) + 4 * (m ?? 0) + (p ?? 0)) / 6;
	}

	function newId(): string {
		return crypto.randomUUID();
	}

	function initRoots(v: any): Node[] {
		return (v?.roots ?? []).map(initNode);
	}

	function initNode(n: any): Node {
		if (n.type === 'GROUP') {
			return {
				logicalId: n.logicalId ?? newId(),
				type: 'GROUP',
				title: n.title ?? '',
				children: (n.children ?? []).map(initNode)
			};
		}
		return {
			logicalId: n.logicalId ?? newId(),
			type: n.type === 'TIME_RELATIVE' ? 'TIME_RELATIVE' : 'FIXED',
			description: n.description ?? '',
			minEffort: n.minEffort ?? null,
			expectedEffort: n.expectedEffort ?? null,
			maxEffort: n.maxEffort ?? null,
			assumptions: n.assumptions ?? null,
			phaseAbbreviation: n.phaseAbbreviation ?? null,
			unit: n.unit ?? null
		};
	}

	let roots = $state<Node[]>(initRoots(version));
	let collapsed = $state(new Set<string>());

	// Snapshot for cycle-protection: captured on the first 'consider' of a
	// drag, restored on 'finalize' if the drop would create a cycle.
	let preDragSnapshot: Node[] | null = null;
	let dragSourceId: string | null = null;

	function allLeaves(nodes: Node[]): Leaf[] {
		const out: Leaf[] = [];
		for (const n of nodes) {
			if (n.type === 'GROUP') out.push(...allLeaves(n.children));
			else out.push(n);
		}
		return out;
	}

	const leaves = $derived(allLeaves(roots));
	const totalOpt = $derived(leaves.reduce((s, i) => s + (i.minEffort ?? 0), 0));
	const totalLik = $derived(leaves.reduce((s, i) => s + (i.expectedEffort ?? 0), 0));
	const totalPes = $derived(leaves.reduce((s, i) => s + (i.maxEffort ?? 0), 0));
	const totalExp = $derived(
		leaves.reduce((s, i) => s + pert(i.minEffort, i.expectedEffort, i.maxEffort), 0)
	);

	const calcEntries = $derived(Array.from(calcMap.values()));
	const totalOfferPT = $derived(calcEntries.reduce((s, v) => s + v.offerPT, 0));
	const totalCost = $derived(calcEntries.reduce((s, v) => s + v.cost, 0));
	const totalOfferPrice = $derived(calcEntries.reduce((s, v) => s + v.offerPrice, 0));

	function notify() {
		onchange(serialize(roots));
	}

	function serialize(nodes: Node[]): any[] {
		return nodes.map((n) => {
			if (n.type === 'GROUP') {
				return {
					logicalId: n.logicalId,
					type: 'GROUP',
					title: n.title,
					children: serialize(n.children)
				};
			}
			return {
				logicalId: n.logicalId,
				type: n.type,
				description: n.description,
				minEffort: n.minEffort,
				expectedEffort: n.expectedEffort,
				maxEffort: n.maxEffort,
				assumptions: n.assumptions,
				phaseAbbreviation: n.phaseAbbreviation,
				unit: n.unit
			};
		});
	}

	function pathKey(p: NodePath): string {
		return p.join('-');
	}

	function nodeAt(path: NodePath): Node {
		let current: Node[] = roots;
		let node: Node | undefined;
		for (const idx of path) {
			node = current[idx];
			if (node && node.type === 'GROUP') current = node.children;
		}
		return node!;
	}

	function deepClone(nodes: Node[]): Node[] {
		// Structured clone preserves nested objects without aliasing.
		return JSON.parse(JSON.stringify(nodes));
	}

	function countId(nodes: Node[], id: string): number {
		let n = 0;
		for (const node of nodes) {
			if (node.logicalId === id) n++;
			if (node.type === 'GROUP') n += countId(node.children, id);
		}
		return n;
	}

	function hasDuplicateIds(nodes: Node[]): boolean {
		const seen = new Set<string>();
		let duplicate = false;
		const walk = (n: Node) => {
			if (duplicate) return;
			if (seen.has(n.logicalId)) {
				duplicate = true;
				return;
			}
			seen.add(n.logicalId);
			if (n.type === 'GROUP') n.children.forEach(walk);
		};
		nodes.forEach(walk);
		return duplicate;
	}

	function applyZoneItems(zonePath: NodePath, items: Node[]) {
		if (zonePath.length === 0) {
			roots = items;
		} else {
			const group = nodeAt(zonePath) as Group;
			group.children = items;
		}
	}

	// Cross-zone moves fire TWO finalize events (one per zone) synchronously.
	// Cycle-check after each would false-trigger on the intermediate transit
	// state (source emptied, destination not yet updated). Defer the check to
	// a microtask so all sibling events for one drop are processed first.
	let cycleCheckPending = false;

	function handleZoneEvent(e: ZoneEvent) {
		if (e.type === 'consider') {
			if (preDragSnapshot === null) {
				preDragSnapshot = deepClone(roots);
				dragSourceId = e.sourceId;
			}
			applyZoneItems(e.path, e.items);
		} else {
			// finalize
			applyZoneItems(e.path, e.items);
			if (cycleCheckPending) return;
			cycleCheckPending = true;
			queueMicrotask(() => {
				// A real cycle = the source node now sits inside its own
				// subtree, which manifests as a duplicate logicalId because
				// the library moves the reference (not a copy).
				if (hasDuplicateIds(roots) && preDragSnapshot !== null) {
					roots = preDragSnapshot;
				} else {
					notify();
				}
				preDragSnapshot = null;
				dragSourceId = null;
				cycleCheckPending = false;
			});
		}
	}

	function handleRootsConsider(
		e: CustomEvent<{ items: Node[]; info: { id: string } }>
	) {
		handleZoneEvent({
			path: [],
			type: 'consider',
			items: e.detail.items,
			sourceId: e.detail.info?.id ?? null
		});
	}

	function handleRootsFinalize(
		e: CustomEvent<{ items: Node[]; info: { id: string } }>
	) {
		handleZoneEvent({
			path: [],
			type: 'finalize',
			items: e.detail.items,
			sourceId: e.detail.info?.id ?? null
		});
	}

	function deleteAt(path: NodePath) {
		if (path.length === 0) return;
		const parentPath = path.slice(0, -1);
		const idx = path[path.length - 1];
		const siblings = parentPath.length === 0 ? roots : (nodeAt(parentPath) as Group).children;
		siblings.splice(idx, 1);
		notify();
	}

	function addChildLeafTo(groupPath: NodePath) {
		const group = nodeAt(groupPath) as Group;
		group.children.push({
			logicalId: newId(),
			type: 'FIXED',
			description: '',
			minEffort: null,
			expectedEffort: null,
			maxEffort: null,
			assumptions: null,
			phaseAbbreviation: null,
			unit: null
		});
		const next = new Set(collapsed);
		next.delete(group.logicalId);
		collapsed = next;
		notify();
	}

	function addChildGroupTo(groupPath: NodePath) {
		const group = nodeAt(groupPath) as Group;
		group.children.push({
			logicalId: newId(),
			type: 'GROUP',
			title: 'New group',
			children: []
		});
		const next = new Set(collapsed);
		next.delete(group.logicalId);
		collapsed = next;
		notify();
	}

	function addRootGroup() {
		roots.push({
			logicalId: newId(),
			type: 'GROUP',
			title: 'New group',
			children: [
				{
					logicalId: newId(),
					type: 'FIXED',
					description: '',
					minEffort: null,
					expectedEffort: null,
					maxEffort: null,
					assumptions: null,
					phaseAbbreviation: null,
					unit: null
				}
			]
		});
		notify();
	}

	function toggle(id: string) {
		const next = new Set(collapsed);
		next.has(id) ? next.delete(id) : next.add(id);
		collapsed = next;
	}
</script>

<div class="border rounded-lg overflow-hidden">
	{#if roots.length === 0}
		<div class="p-10 text-center text-gray-400">
			<p class="mb-4 text-sm">No items yet.</p>
			{#if editable}
				<button
					onclick={addRootGroup}
					class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
					>Add group</button
				>
			{/if}
		</div>
	{:else}
		<!-- Header row -->
		<div
			class="grid items-center bg-brand-green/10 border-b text-xs text-brand-green uppercase tracking-wide"
			style="grid-template-columns: {COLS}{editable ? ' 2rem' : ''}"
		>
			<div class="py-2 px-3"></div>
			<div class="py-2 px-3 text-left">Description</div>
			<div class="py-2 px-2 text-left">Type</div>
			<div class="py-2 px-2 text-left">Phase</div>
			<div class="py-2 px-2 text-right">Optimistic</div>
			<div class="py-2 px-2 text-right">Likely</div>
			<div class="py-2 px-2 text-right">Pessimistic</div>
			<div class="py-2 px-2 text-right">PERT</div>
			<div class="py-2 px-3 text-left">Assumptions</div>
			<div class="py-2 px-2 text-right">offerPT (PT)</div>
			<div class="py-2 px-2 text-right">Cost (EUR)</div>
			<div class="py-2 px-2 text-right">Offer Price (EUR)</div>
			{#if editable}<div class="py-2 px-3"></div>{/if}
		</div>

		<!-- Top-level dndzone for roots -->
		<div
			use:dndzone={{
				items: roots,
				type: 'estimation-node',
				flipDurationMs: 200,
				dragDisabled: !editable
			}}
			onconsider={handleRootsConsider}
			onfinalize={handleRootsFinalize}
			aria-label="Estimation root nodes"
		>
			{#each roots as root, idx (root.logicalId)}
				<EstimationNodeRow
					node={root}
					depth={0}
					path={[idx]}
					{collapsed}
					{editable}
					{calcMap}
					{phases}
					onChange={notify}
					onZoneEvent={handleZoneEvent}
					onAddChildGroup={addChildGroupTo}
					onAddChildLeaf={addChildLeafTo}
					onDelete={deleteAt}
					onToggle={toggle}
				/>
			{/each}
		</div>

		<!-- Footer totals -->
		<div
			class="grid items-center bg-gray-50 border-t-2 border-gray-300 font-semibold text-sm"
			style="grid-template-columns: {COLS}{editable ? ' 2rem' : ''}"
		>
			<div class="py-2 px-3"></div>
			<div class="py-2 px-3 text-xs text-gray-400 uppercase tracking-wide">Total</div>
			<div class="py-2 px-2"></div>
			<div class="py-2 px-2"></div>
			<div class="py-2 px-2 text-right tabular-nums">{totalOpt.toFixed(2)}</div>
			<div class="py-2 px-2 text-right tabular-nums">{totalLik.toFixed(2)}</div>
			<div class="py-2 px-2 text-right tabular-nums">{totalPes.toFixed(2)}</div>
			<div class="py-2 px-2 text-right text-brand-green tabular-nums">{totalExp.toFixed(2)}</div>
			<div class="py-2 px-3"></div>
			<div class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalOfferPT.toFixed(2)}</div>
			<div class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalCost.toFixed(0)}</div>
			<div class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalOfferPrice.toFixed(0)}</div>
			{#if editable}<div class="py-2 px-3"></div>{/if}
		</div>

		{#if editable}
			<div class="p-3 border-t bg-gray-50/40">
				<button onclick={addRootGroup} class="text-sm text-brand-green hover:text-[#007a45]">
					+ Add group
				</button>
			</div>
		{/if}
	{/if}
</div>
