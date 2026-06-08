<script lang="ts">
	type Leaf = {
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

	type Group = {
		logicalId: string;
		type: 'GROUP';
		title: string;
		children: Node[];
	};

	type Node = Leaf | Group;

	type CalcEntry = { offerPT: number; cost: number; offerPrice: number };

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

	// All leaves anywhere in the tree, for footer totals.
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

	type PathSeg = number; // index into the children list at that level
	type NodePath = PathSeg[];

	/** Depth-first flattened view of the tree for rendering and keyboard nav. */
	type Row = {
		node: Node;
		depth: number;
		path: NodePath;
		visible: boolean; // false if any ancestor is collapsed
	};

	function flatten(nodes: Node[]): Row[] {
		const out: Row[] = [];
		function walk(list: Node[], depth: number, prefix: NodePath, hiddenByAncestor: boolean) {
			list.forEach((node, idx) => {
				const path = [...prefix, idx];
				const visible = !hiddenByAncestor;
				out.push({ node, depth, path, visible });
				if (node.type === 'GROUP') {
					const childHidden = hiddenByAncestor || collapsed.has(node.logicalId);
					walk(node.children, depth + 1, path, childHidden);
				}
			});
		}
		walk(nodes, 0, [], false);
		return out;
	}

	const rows = $derived(flatten(roots));

	function nodeAt(path: NodePath): Node {
		let current: Node[] = roots;
		let node: Node | undefined;
		for (const idx of path) {
			node = current[idx];
			if (node && node.type === 'GROUP') current = node.children;
		}
		return node!;
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

	function updateLeafField(path: NodePath, field: keyof Leaf, raw: string) {
		const leaf = nodeAt(path) as Leaf;
		const numericFields: (keyof Leaf)[] = ['minEffort', 'expectedEffort', 'maxEffort'];
		const nullableStringFields: (keyof Leaf)[] = ['assumptions'];
		(leaf as any)[field] = numericFields.includes(field)
			? raw === ''
				? null
				: parseFloat(raw)
			: nullableStringFields.includes(field)
				? raw || null
				: raw;
		notify();
	}

	function updateLeafPhase(path: NodePath, val: string) {
		const leaf = nodeAt(path) as Leaf;
		leaf.phaseAbbreviation = val === '' ? null : val;
		notify();
	}

	function updateLeafType(path: NodePath, val: string) {
		const leaf = nodeAt(path) as Leaf;
		leaf.type = val === 'TIME_RELATIVE' ? 'TIME_RELATIVE' : 'FIXED';
		leaf.unit = val === 'TIME_RELATIVE' ? 'h/Woche' : null;
		notify();
	}

	function updateGroupTitle(path: NodePath, val: string) {
		const group = nodeAt(path) as Group;
		group.title = val;
		notify();
	}

	function pathKey(path: NodePath): string {
		return path.join('-');
	}

	// Keyboard nav: index over visible leaves in depth-first order. Editable
	// columns: description (0), optimistic (1), likely (2), pessimistic (3),
	// assumptions (5). Type (col 2 visually) and phase (col 3 visually) are
	// <select> elements outside the Tab cycle.
	const editCols = [0, 1, 2, 3, 5];

	function visibleLeafPaths(): NodePath[] {
		return rows.filter((r) => r.visible && r.node.type !== 'GROUP').map((r) => r.path);
	}

	function focusCell(path: NodePath, col: number) {
		(document.querySelector(`[data-cell="${pathKey(path)}-${col}"]`) as HTMLElement)?.focus();
	}

	function onKeyDown(e: KeyboardEvent, path: NodePath, col: number) {
		const flat = visibleLeafPaths();
		const fi = flat.findIndex((p) => pathKey(p) === pathKey(path));
		const ci = editCols.indexOf(col);
		const isNumeric = col === 1 || col === 2 || col === 3;

		if (e.key === 'Tab') {
			e.preventDefault();
			if (!e.shiftKey) {
				if (ci < editCols.length - 1) {
					focusCell(path, editCols[ci + 1]);
				} else if (fi < flat.length - 1) {
					focusCell(flat[fi + 1], editCols[0]);
				}
			} else {
				if (ci > 0) {
					focusCell(path, editCols[ci - 1]);
				} else if (fi > 0) {
					focusCell(flat[fi - 1], editCols[editCols.length - 1]);
				}
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

	const colCount = $derived(editable ? 13 : 12);
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
		<table class="w-full text-sm border-collapse">
			<thead>
				<tr class="bg-brand-green/10 border-b text-left text-xs text-brand-green uppercase tracking-wide">
					<th class="py-2 px-3 w-6"></th>
					<th class="py-2 px-3">Description</th>
					<th class="py-2 px-2 w-20">Type</th>
					<th class="py-2 px-2 w-24">Phase</th>
					<th class="py-2 px-2 text-right w-24">Optimistic</th>
					<th class="py-2 px-2 text-right w-24">Likely</th>
					<th class="py-2 px-2 text-right w-24">Pessimistic</th>
					<th class="py-2 px-2 text-right w-24">PERT</th>
					<th class="py-2 px-3">Assumptions</th>
					<th class="py-2 px-2 text-right w-24">offerPT (PT)</th>
					<th class="py-2 px-2 text-right w-28">Cost (EUR)</th>
					<th class="py-2 px-2 text-right w-28">Offer Price (EUR)</th>
					{#if editable}<th class="py-2 px-3 w-8"></th>{/if}
				</tr>
			</thead>
			<tbody>
				{#each rows as row (pathKey(row.path))}
					{#if row.visible}
						{@const indent = row.depth * 1.25}
						{#if row.node.type === 'GROUP'}
							{@const group = row.node}
							{@const calc = calcMap.get(group.logicalId)}
							<tr class="bg-gray-100 border-b">
								<td class="py-2 px-3 text-gray-400 text-xs cursor-pointer select-none hover:bg-gray-200"
									onclick={() => toggle(group.logicalId)}>
									<span style="display:inline-block;width:{indent}rem"></span>
									{collapsed.has(group.logicalId) ? '▶' : '▼'}
								</td>
								<td class="py-2 px-3 font-semibold text-gray-700">
									<span style="display:inline-block;width:{indent}rem"></span>
									{#if editable}
										<input
											type="text"
											class="bg-transparent font-semibold focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={group.title}
											oninput={(e) => updateGroupTitle(row.path, e.currentTarget.value)}
										/>
									{:else}
										{group.title}
									{/if}
									<span class="ml-2 text-xs font-normal text-gray-400"
										>{group.children.length} child{group.children.length !== 1 ? 'ren' : ''}</span
									>
								</td>
								<td class="py-2 px-2"></td>
								<td class="py-2 px-2"></td>
								<td class="py-2 px-2"></td>
								<td class="py-2 px-2"></td>
								<td class="py-2 px-2"></td>
								<td class="py-2 px-2"></td>
								<td class="py-2 px-3">
									{#if editable}
										<button
											onclick={() => addChildGroupTo(row.path)}
											class="text-xs text-brand-green hover:text-[#007a45] mr-3">+ group</button>
										<button
											onclick={() => addChildLeafTo(row.path)}
											class="text-xs text-brand-green hover:text-[#007a45]">+ item</button>
									{/if}
								</td>
								<td class="py-2 px-2 text-right text-gray-600 tabular-nums">
									{calc != null ? calc.offerPT.toFixed(2) : '—'}
								</td>
								<td class="py-2 px-2 text-right text-gray-600 tabular-nums">
									{calc != null ? calc.cost.toFixed(0) : '—'}
								</td>
								<td class="py-2 px-2 text-right text-gray-600 tabular-nums">
									{calc != null ? calc.offerPrice.toFixed(0) : '—'}
								</td>
								{#if editable}
									<td class="py-2 px-3">
										<button
											onclick={() => deleteAt(row.path)}
											class="text-gray-300 hover:text-red-500 transition-colors leading-none"
											title="Delete group">
											✕
										</button>
									</td>
								{/if}
							</tr>
						{:else}
							{@const leaf = row.node}
							{@const calc = calcMap.get(leaf.logicalId)}
							<tr class="border-b hover:bg-gray-50">
								<td class="py-1 px-3">
									<span style="display:inline-block;width:{indent}rem"></span>
								</td>

								<!-- Description -->
								<td class="py-1 px-2">
									<span style="display:inline-block;width:{indent}rem"></span>
									{#if editable}
										<input
											type="text"
											class="bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											style="width: calc(100% - {indent}rem)"
											value={leaf.description}
											data-cell="{pathKey(row.path)}-0"
											oninput={(e) => updateLeafField(row.path, 'description', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, row.path, 0)}
										/>
									{:else}
										<span class="px-1">{leaf.description}</span>
									{/if}
								</td>

								<!-- Type -->
								<td class="py-1 px-2">
									{#if editable}
										<select
											class="w-full bg-transparent text-xs focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={leaf.type}
											onchange={(e) => updateLeafType(row.path, e.currentTarget.value)}
										>
											<option value="FIXED">Fixed</option>
											<option value="TIME_RELATIVE">h/Woche</option>
										</select>
									{:else if leaf.type === 'TIME_RELATIVE'}
										<span class="px-1.5 py-0.5 text-xs bg-blue-50 text-blue-600 rounded">{leaf.unit ?? 'h/Woche'}</span>
									{/if}
								</td>

								<!-- Phase -->
								<td class="py-1 px-2">
									{#if editable && phases.length > 0}
										<select
											class="w-full bg-transparent text-sm focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={leaf.phaseAbbreviation ?? ''}
											onchange={(e) => updateLeafPhase(row.path, e.currentTarget.value)}
										>
											<option value="">— none —</option>
											{#each phases as p}
												<option value={p.abbreviation}>{p.abbreviation}</option>
											{/each}
										</select>
									{:else}
										<span class="px-1 text-xs text-gray-500">{leaf.phaseAbbreviation ?? ''}</span>
									{/if}
								</td>

								<!-- Optimistic -->
								<td class="py-1 px-2 text-right">
									{#if editable}
										<input
											type="number"
											step="0.1"
											min="0"
											class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={leaf.minEffort ?? ''}
											data-cell="{pathKey(row.path)}-1"
											oninput={(e) => updateLeafField(row.path, 'minEffort', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, row.path, 1)}
										/>
									{:else}
										<span class="tabular-nums">{leaf.minEffort ?? ''}</span>
									{/if}
								</td>

								<!-- Likely -->
								<td class="py-1 px-2 text-right">
									{#if editable}
										<input
											type="number"
											step="0.1"
											min="0"
											class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={leaf.expectedEffort ?? ''}
											data-cell="{pathKey(row.path)}-2"
											oninput={(e) => updateLeafField(row.path, 'expectedEffort', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, row.path, 2)}
										/>
									{:else}
										<span class="tabular-nums">{leaf.expectedEffort ?? ''}</span>
									{/if}
								</td>

								<!-- Pessimistic -->
								<td class="py-1 px-2 text-right">
									{#if editable}
										<input
											type="number"
											step="0.1"
											min="0"
											class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={leaf.maxEffort ?? ''}
											data-cell="{pathKey(row.path)}-3"
											oninput={(e) => updateLeafField(row.path, 'maxEffort', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, row.path, 3)}
										/>
									{:else}
										<span class="tabular-nums">{leaf.maxEffort ?? ''}</span>
									{/if}
								</td>

								<!-- PERT (always read-only) -->
								<td class="py-1 px-3 text-right text-brand-green tabular-nums">
									{pert(leaf.minEffort, leaf.expectedEffort, leaf.maxEffort).toFixed(2)}
								</td>

								<!-- Assumptions -->
								<td class="py-1 px-2">
									{#if editable}
										<input
											type="text"
											class="w-full bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
											value={leaf.assumptions ?? ''}
											placeholder="…"
											data-cell="{pathKey(row.path)}-5"
											oninput={(e) => updateLeafField(row.path, 'assumptions', e.currentTarget.value)}
											onkeydown={(e) => onKeyDown(e, row.path, 5)}
										/>
									{:else}
										<span class="px-1 text-gray-500">{leaf.assumptions ?? ''}</span>
									{/if}
								</td>

								<!-- offerPT (server-calculated, read-only) -->
								<td class="py-1 px-2 text-right text-gray-600 tabular-nums">
									{#if leaf.type === 'TIME_RELATIVE' && !leaf.phaseAbbreviation}
										<span class="text-amber-500 text-xs">⚠ needs phase</span>
									{:else}
										{calc != null ? calc.offerPT.toFixed(2) : '—'}
									{/if}
								</td>

								<!-- Cost (server-calculated, read-only) -->
								<td class="py-1 px-2 text-right text-gray-600 tabular-nums">
									{calc != null ? calc.cost.toFixed(0) : '—'}
								</td>

								<!-- Offer Price (server-calculated, read-only) -->
								<td class="py-1 px-2 text-right text-gray-600 tabular-nums">
									{calc != null ? calc.offerPrice.toFixed(0) : '—'}
								</td>

								{#if editable}
									<td class="py-1 px-3">
										<button
											onclick={() => deleteAt(row.path)}
											class="text-gray-300 hover:text-red-500 transition-colors leading-none"
											title="Delete item"
										>
											✕
										</button>
									</td>
								{/if}
							</tr>
						{/if}
					{/if}
				{/each}

				<!-- Footer totals -->
				<tr class="bg-gray-50 border-t-2 border-gray-300 font-semibold text-sm">
					<td class="py-2 px-3"></td>
					<td class="py-2 px-3 text-xs text-gray-400 uppercase tracking-wide">Total</td>
					<td class="py-2 px-2"></td>
					<td class="py-2 px-2"></td>
					<td class="py-2 px-2 text-right tabular-nums">{totalOpt.toFixed(2)}</td>
					<td class="py-2 px-2 text-right tabular-nums">{totalLik.toFixed(2)}</td>
					<td class="py-2 px-2 text-right tabular-nums">{totalPes.toFixed(2)}</td>
					<td class="py-2 px-2 text-right text-brand-green tabular-nums">{totalExp.toFixed(2)}</td>
					<td class="py-2 px-3"></td>
					<td class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalOfferPT.toFixed(2)}</td>
					<td class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalCost.toFixed(0)}</td>
					<td class="py-2 px-2 text-right text-gray-600 tabular-nums"
						>{totalOfferPrice.toFixed(0)}</td
					>
					{#if editable}<td class="py-2 px-3"></td>{/if}
				</tr>
			</tbody>
		</table>

		{#if editable}
			<div class="p-3 border-t bg-gray-50/40">
				<button onclick={addRootGroup} class="text-sm text-brand-green hover:text-[#007a45]">
					+ Add group
				</button>
			</div>
		{/if}
	{/if}
</div>
