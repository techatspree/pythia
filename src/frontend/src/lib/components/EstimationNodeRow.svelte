<script lang="ts" module>
	export type Leaf = {
		logicalId: string;
		type: 'FIXED' | 'TIME_RELATIVE';
		description: string;
		minEffort: number | null;
		expectedEffort: number | null;
		maxEffort: number | null;
		assumptions: string | null;
		phaseAbbreviation: string | null;
		unit: string | null;
	};

	export type Group = {
		logicalId: string;
		type: 'GROUP';
		title: string;
		children: Node[];
	};

	export type Node = Leaf | Group;

	export type NodePath = number[];

	export type CalcEntry = { offerPT: number; cost: number; offerPrice: number };

	export type ZoneEvent = {
		path: NodePath; // path of the GROUP whose children-zone fired (empty path = roots)
		type: 'consider' | 'finalize';
		items: Node[];
		sourceId: string | null;
	};

	export const COLS = '1.5rem 1fr 5rem 6rem 6rem 6rem 6rem 6rem 1fr 6rem 7rem 7rem';
</script>

<script lang="ts">
	import { dndzone } from 'svelte-dnd-action';
	import Self from './EstimationNodeRow.svelte';

	let {
		node,
		depth,
		path,
		collapsed,
		editable,
		calcMap,
		phases,
		onChange,
		onZoneEvent,
		onAddChildGroup,
		onAddChildLeaf,
		onDelete,
		onToggle
	}: {
		node: Node;
		depth: number;
		path: NodePath;
		collapsed: Set<string>;
		editable: boolean;
		calcMap: Map<string, CalcEntry>;
		phases: any[];
		onChange: () => void;
		onZoneEvent: (e: ZoneEvent) => void;
		onAddChildGroup: (path: NodePath) => void;
		onAddChildLeaf: (path: NodePath) => void;
		onDelete: (path: NodePath) => void;
		onToggle: (id: string) => void;
	} = $props();

	function pathKey(p: NodePath): string {
		return p.join('-');
	}

	function pert(o: number | null, m: number | null, p: number | null): number {
		return ((o ?? 0) + 4 * (m ?? 0) + (p ?? 0)) / 6;
	}

	function updateLeafField(field: keyof Leaf, raw: string) {
		const leaf = node as Leaf;
		const numericFields: (keyof Leaf)[] = ['minEffort', 'expectedEffort', 'maxEffort'];
		const nullableStringFields: (keyof Leaf)[] = ['assumptions'];
		(leaf as any)[field] = numericFields.includes(field)
			? raw === ''
				? null
				: parseFloat(raw)
			: nullableStringFields.includes(field)
				? raw || null
				: raw;
		onChange();
	}

	function updateLeafPhase(val: string) {
		(node as Leaf).phaseAbbreviation = val === '' ? null : val;
		onChange();
	}

	function updateLeafType(val: string) {
		const leaf = node as Leaf;
		leaf.type = val === 'TIME_RELATIVE' ? 'TIME_RELATIVE' : 'FIXED';
		leaf.unit = val === 'TIME_RELATIVE' ? 'h/Woche' : null;
		onChange();
	}

	function updateGroupTitle(val: string) {
		(node as Group).title = val;
		onChange();
	}

	function handleConsider(e: CustomEvent<{ items: Node[]; info: { id: string } }>) {
		onZoneEvent({ path, type: 'consider', items: e.detail.items, sourceId: e.detail.info?.id ?? null });
	}

	function handleFinalize(e: CustomEvent<{ items: Node[]; info: { id: string } }>) {
		onZoneEvent({ path, type: 'finalize', items: e.detail.items, sourceId: e.detail.info?.id ?? null });
	}

	const editCols = [0, 1, 2, 3, 5];

	function focusCell(p: NodePath, col: number) {
		(document.querySelector(`[data-cell="${pathKey(p)}-${col}"]`) as HTMLElement)?.focus();
	}

	function visibleLeafPathsInDoc(): NodePath[] {
		const inputs = Array.from(document.querySelectorAll('[data-cell$="-0"]')) as HTMLElement[];
		return inputs.map((el) => {
			const key = el.getAttribute('data-cell')!.replace(/-0$/, '');
			return key.split('-').map((s) => parseInt(s, 10));
		});
	}

	function onKeyDown(e: KeyboardEvent, col: number) {
		const flat = visibleLeafPathsInDoc();
		const fi = flat.findIndex((p) => pathKey(p) === pathKey(path));
		const ci = editCols.indexOf(col);
		const isNumeric = col === 1 || col === 2 || col === 3;

		if (e.key === 'Tab') {
			e.preventDefault();
			if (!e.shiftKey) {
				if (ci < editCols.length - 1) focusCell(path, editCols[ci + 1]);
				else if (fi < flat.length - 1) focusCell(flat[fi + 1], editCols[0]);
			} else {
				if (ci > 0) focusCell(path, editCols[ci - 1]);
				else if (fi > 0) focusCell(flat[fi - 1], editCols[editCols.length - 1]);
			}
		} else if (e.key === 'Enter' || e.key === 'ArrowDown') {
			e.preventDefault();
			if (fi < flat.length - 1) focusCell(flat[fi + 1], col);
		} else if (e.key === 'ArrowUp') {
			e.preventDefault();
			if (fi > 0) focusCell(flat[fi - 1], col);
		} else if (e.key === 'ArrowRight') {
			const input = e.currentTarget as HTMLInputElement;
			const atEnd = isNumeric || input.selectionEnd === input.value.length;
			if (atEnd && ci < editCols.length - 1) {
				e.preventDefault();
				focusCell(path, editCols[ci + 1]);
			}
		} else if (e.key === 'ArrowLeft') {
			const input = e.currentTarget as HTMLInputElement;
			const atStart = isNumeric || input.selectionStart === 0;
			if (atStart && ci > 0) {
				e.preventDefault();
				focusCell(path, editCols[ci - 1]);
			}
		}
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

{#if node.type === 'GROUP'}
	{@const calc = calcMap.get(node.logicalId)}
	<!-- Single wrapper per group: svelte-dnd-action's parent zone treats
	     each direct child as ONE draggable item, so the row + the nested
	     children-zone must live inside ONE outer div. -->
	<div data-node-id={node.logicalId}>
	<div
		class="grid items-center bg-gray-100 border-b"
		style="grid-template-columns: {COLS}{editable ? ' 2rem' : ''}"
		data-testid="row-{pathKey(path)}"
	>
		<div class="flex items-center">
			{#if editable}
				<span
					data-dnd-handle
					class="cursor-grab text-gray-300 hover:text-gray-500 px-1 select-none"
					title="Drag to move"
					aria-hidden="true">⋮⋮</span
				>
			{/if}
			<button
				type="button"
				class="py-2 px-1 text-gray-400 text-xs cursor-pointer select-none hover:bg-gray-200 text-left flex-1"
				onclick={() => onToggle(node.logicalId)}
				aria-label={collapsed.has(node.logicalId) ? 'Expand group' : 'Collapse group'}
			>
				{@render indent(depth)}
				{collapsed.has(node.logicalId) ? '▶' : '▼'}
			</button>
		</div>
		<div class="py-2 px-3 font-semibold text-gray-700">
			{@render indent(depth)}
			{#if editable}
				<input
					type="text"
					class="bg-transparent font-semibold focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
					value={node.title}
					oninput={(e) => updateGroupTitle(e.currentTarget.value)}
				/>
			{:else}
				{node.title}
			{/if}
			<span class="ml-2 text-xs font-normal text-gray-400"
				>{node.children.length} child{node.children.length !== 1 ? 'ren' : ''}</span
			>
		</div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-3">
			{#if editable}
				<button
					onclick={() => onAddChildGroup(path)}
					class="text-xs text-brand-green hover:text-[#007a45] mr-3">+ group</button
				>
				<button
					onclick={() => onAddChildLeaf(path)}
					class="text-xs text-brand-green hover:text-[#007a45]">+ item</button
				>
			{/if}
		</div>
		<div class="py-2 px-2 text-right text-gray-600 tabular-nums">
			{calc != null ? calc.offerPT.toFixed(2) : '—'}
		</div>
		<div class="py-2 px-2 text-right text-gray-600 tabular-nums">
			{calc != null ? calc.cost.toFixed(0) : '—'}
		</div>
		<div class="py-2 px-2 text-right text-gray-600 tabular-nums">
			{calc != null ? calc.offerPrice.toFixed(0) : '—'}
		</div>
		{#if editable}
			<div class="py-2 px-3">
				<button
					onclick={() => onDelete(path)}
					class="text-gray-300 hover:text-red-500 transition-colors leading-none"
					title="Delete group"
				>
					✕
				</button>
			</div>
		{/if}
	</div>

	{#if !collapsed.has(node.logicalId)}
		<div
			use:dndzone={{
				items: node.children,
				type: 'estimation-node',
				flipDurationMs: 200,
				dragDisabled: !editable
			}}
			onconsider={handleConsider}
			onfinalize={handleFinalize}
			aria-label="Children of {node.title}"
		>
			{#each node.children as child, idx (child.logicalId)}
				<Self
					node={child}
					depth={depth + 1}
					path={[...path, idx]}
					{collapsed}
					{editable}
					{calcMap}
					{phases}
					{onChange}
					{onZoneEvent}
					{onAddChildGroup}
					{onAddChildLeaf}
					{onDelete}
					{onToggle}
				/>
			{/each}
		</div>
	{/if}
	</div>
{:else}
	{@const leaf = node}
	{@const calc = calcMap.get(leaf.logicalId)}
	<div
		class="grid items-center border-b hover:bg-gray-50"
		style="grid-template-columns: {COLS}{editable ? ' 2rem' : ''}"
		data-testid="row-{pathKey(path)}"
	>
		<div class="py-1 px-1">
			{@render indent(depth)}
			{#if editable}
				<span
					data-dnd-handle
					class="cursor-grab text-gray-300 hover:text-gray-500 select-none"
					title="Drag to move"
					aria-hidden="true">⋮⋮</span
				>
			{/if}
		</div>
		<div class="py-1 px-2">
			{@render indent(depth)}
			{#if editable}
				<input
					type="text"
					class="bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
					style="width: calc(100% - {depth * 1.5}rem)"
					value={leaf.description}
					data-cell="{pathKey(path)}-0"
					oninput={(e) => updateLeafField('description', e.currentTarget.value)}
					onkeydown={(e) => onKeyDown(e, 0)}
				/>
			{:else}
				<span class="px-1">{leaf.description}</span>
			{/if}
		</div>
		<div class="py-1 px-2">
			{#if editable}
				<select
					class="w-full bg-transparent text-xs focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
					value={leaf.type}
					onchange={(e) => updateLeafType(e.currentTarget.value)}
				>
					<option value="FIXED">Fixed</option>
					<option value="TIME_RELATIVE">h/Woche</option>
				</select>
			{:else if leaf.type === 'TIME_RELATIVE'}
				<span class="px-1.5 py-0.5 text-xs bg-blue-50 text-blue-600 rounded"
					>{leaf.unit ?? 'h/Woche'}</span
				>
			{/if}
		</div>
		<div class="py-1 px-2">
			{#if editable && phases.length > 0}
				<select
					class="w-full bg-transparent text-sm focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
					value={leaf.phaseAbbreviation ?? ''}
					onchange={(e) => updateLeafPhase(e.currentTarget.value)}
				>
					<option value="">— none —</option>
					{#each phases as p}
						<option value={p.abbreviation}>{p.abbreviation}</option>
					{/each}
				</select>
			{:else}
				<span class="px-1 text-xs text-gray-500">{leaf.phaseAbbreviation ?? ''}</span>
			{/if}
		</div>
		<div class="py-1 px-2 text-right">
			{#if editable}
				<input
					type="number"
					step="0.1"
					min="0"
					class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
					value={leaf.minEffort ?? ''}
					data-cell="{pathKey(path)}-1"
					oninput={(e) => updateLeafField('minEffort', e.currentTarget.value)}
					onkeydown={(e) => onKeyDown(e, 1)}
				/>
			{:else}
				<span class="tabular-nums">{leaf.minEffort ?? ''}</span>
			{/if}
		</div>
		<div class="py-1 px-2 text-right">
			{#if editable}
				<input
					type="number"
					step="0.1"
					min="0"
					class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
					value={leaf.expectedEffort ?? ''}
					data-cell="{pathKey(path)}-2"
					oninput={(e) => updateLeafField('expectedEffort', e.currentTarget.value)}
					onkeydown={(e) => onKeyDown(e, 2)}
				/>
			{:else}
				<span class="tabular-nums">{leaf.expectedEffort ?? ''}</span>
			{/if}
		</div>
		<div class="py-1 px-2 text-right">
			{#if editable}
				<input
					type="number"
					step="0.1"
					min="0"
					class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
					value={leaf.maxEffort ?? ''}
					data-cell="{pathKey(path)}-3"
					oninput={(e) => updateLeafField('maxEffort', e.currentTarget.value)}
					onkeydown={(e) => onKeyDown(e, 3)}
				/>
			{:else}
				<span class="tabular-nums">{leaf.maxEffort ?? ''}</span>
			{/if}
		</div>
		<div class="py-1 px-3 text-right text-brand-green tabular-nums">
			{pert(leaf.minEffort, leaf.expectedEffort, leaf.maxEffort).toFixed(2)}
		</div>
		<div class="py-1 px-2">
			{#if editable}
				<input
					type="text"
					class="w-full bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
					value={leaf.assumptions ?? ''}
					placeholder="…"
					data-cell="{pathKey(path)}-5"
					oninput={(e) => updateLeafField('assumptions', e.currentTarget.value)}
					onkeydown={(e) => onKeyDown(e, 5)}
				/>
			{:else}
				<span class="px-1 text-gray-500">{leaf.assumptions ?? ''}</span>
			{/if}
		</div>
		<div class="py-1 px-2 text-right text-gray-600 tabular-nums">
			{#if leaf.type === 'TIME_RELATIVE' && !leaf.phaseAbbreviation}
				<span class="text-amber-500 text-xs">⚠ needs phase</span>
			{:else}
				{calc != null ? calc.offerPT.toFixed(2) : '—'}
			{/if}
		</div>
		<div class="py-1 px-2 text-right text-gray-600 tabular-nums">
			{calc != null ? calc.cost.toFixed(0) : '—'}
		</div>
		<div class="py-1 px-2 text-right text-gray-600 tabular-nums">
			{calc != null ? calc.offerPrice.toFixed(0) : '—'}
		</div>
		{#if editable}
			<div class="py-1 px-3">
				<button
					onclick={() => onDelete(path)}
					class="text-gray-300 hover:text-red-500 transition-colors leading-none"
					title="Delete item"
				>
					✕
				</button>
			</div>
		{/if}
	</div>
{/if}
