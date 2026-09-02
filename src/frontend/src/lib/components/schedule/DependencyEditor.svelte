<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { SvelteSet } from 'svelte/reactivity';
	import { log } from '$lib/log';
	import { formatFixed } from '$lib/format';
	import { locale } from 'svelte-i18n';
	import type { ProjectScheduleView, ScheduleEdge, ScheduledTaskView } from '$lib/adapter';

	// Graphical dependency editor (task-157) — Merlin's "Flow" net-plan shape.
	//
	// It RENDERS the domain's schedule and computes nothing: durations, the
	// roll-up, the critical chain and the dates all come from ProjectSchedule
	// (task-164/task-166). Only the LAYOUT is ours, and layout is not model data
	// — no position is ever persisted.
	//
	// No charting or graph library, deliberately: this repo hand-rolls TreeTable
	// and its drop-zone geometry, and a library would own the interaction model
	// task-158's Gantt has to match.
	let {
		schedule,
		dependencies = $bindable(),
		editable = true
	}: {
		schedule: ProjectScheduleView | null;
		dependencies: ScheduleEdge[];
		editable?: boolean;
	} = $props();

	const CARD_W = 200;
	const CARD_H = 62;
	const GAP_X = 72;
	const GAP_Y = 16;

	// Collapse is a RULE consulted per node, never a seed copied into $state
	// (the TreeTable pattern): a group that appears later is collapsed too.
	// View-only — never persisted, never sent to the backend.
	// SvelteSet, not a plain Set: this is genuinely mutable reactive state, the
	// same choice TreeTable makes for `expandedOverrides` / `draggedSubtreeIds`.
	const expanded = new SvelteSet<string>();
	function isExpanded(id: string): boolean {
		return expanded.has(id);
	}

	function toggle(id: string) {
		if (expanded.has(id)) expanded.delete(id);
		else expanded.add(id);
	}

	const tasks = $derived(schedule?.tasks ?? []);
	// Rebuilt wholesale by $derived on every change, never mutated in place, so a
	// plain Map is correct (SvelteMap would add needless signal overhead).
	const byId = $derived(new Map(tasks.map((t) => [t.logicalId, t])));

	/** A node is hidden when any ancestor is a collapsed group. */
	function hidden(task: ScheduledTaskView): boolean {
		let p = task.parentLogicalId;
		while (p != null) {
			if (!isExpanded(p)) return true;
			p = byId.get(p)?.parentLogicalId ?? null;
		}
		return false;
	}

	const visible = $derived(tasks.filter((t) => !hidden(t)));

	/** The visible ancestor an edge endpoint collapses into. */
	function visibleAnchor(id: string): string | null {
		let cur: ScheduledTaskView | undefined = byId.get(id);
		while (cur != null) {
			if (!hidden(cur)) return cur.logicalId;
			cur = cur.parentLogicalId == null ? undefined : byId.get(cur.parentLogicalId);
		}
		return null;
	}

	// Layered top-down layout, derived from the graph — each card in the earliest
	// layer its predecessors allow. Ours, not the domain's.
	const layout = $derived.by(() => {
		// Transient locals inside a $derived — rebuilt each pass, never reactive.
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const preds = new Map<string, string[]>();
		for (const e of dependencies) {
			const to = visibleAnchor(e.toLogicalId);
			const from = visibleAnchor(e.fromLogicalId);
			if (to == null || from == null || to === from) continue;
			preds.set(to, [...(preds.get(to) ?? []), from]);
		}
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const layer = new Map<string, number>();
		const order = visible.map((t) => t.logicalId);
		// visible follows tree order; a few relaxation passes settle the layers
		// without needing a second topological sort here.
		for (let pass = 0; pass < order.length; pass++) {
			let moved = false;
			for (const id of order) {
				const want = Math.max(0, ...(preds.get(id) ?? []).map((p) => (layer.get(p) ?? 0) + 1));
				if ((layer.get(id) ?? 0) !== want) {
					layer.set(id, want);
					moved = true;
				}
			}
			if (!moved) break;
		}
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const rowInLayer = new Map<number, number>();
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const boxes = new Map<string, { x: number; y: number }>();
		for (const id of order) {
			const l = layer.get(id) ?? 0;
			const row = rowInLayer.get(l) ?? 0;
			rowInLayer.set(l, row + 1);
			boxes.set(id, { x: l * (CARD_W + GAP_X), y: row * (CARD_H + GAP_Y) });
		}
		const width = (Math.max(0, ...[...layer.values()]) + 1) * (CARD_W + GAP_X);
		const height = Math.max(1, ...[...rowInLayer.values()]) * (CARD_H + GAP_Y);
		return { boxes, width, height };
	});

	/** Edges collapsed onto their visible anchors, de-duplicated. */
	const drawnEdges = $derived.by(() => {
		const out: { from: string; to: string; edge: ScheduleEdge }[] = [];
		// eslint-disable-next-line svelte/prefer-svelte-reactivity
		const seen = new Set<string>();
		for (const edge of dependencies) {
			const from = visibleAnchor(edge.fromLogicalId);
			const to = visibleAnchor(edge.toLogicalId);
			if (from == null || to == null || from === to) continue;
			const key = `${from}->${to}`;
			if (seen.has(key)) continue;
			seen.add(key);
			out.push({ from, to, edge });
		}
		return out;
	});

	const cycleIds = $derived(
		new Set(schedule?.error?.kind === 'CYCLE' ? schedule.error.involvedLogicalIds : [])
	);

	let dragFrom = $state<string | null>(null);

	function addEdge(from: string, to: string) {
		// A self-edge is refused: task-155 reports it as a one-node CYCLE.
		if (from === to) {
			log.debug(`schedule: refused self-edge on ${from}`);
			return;
		}
		// A duplicate is refused silently — task-156's unique constraint would
		// reject it anyway.
		if (dependencies.some((d) => d.fromLogicalId === from && d.toLogicalId === to)) {
			log.debug(`schedule: refused duplicate edge ${from} -> ${to}`);
			return;
		}
		dependencies.push({ fromLogicalId: from, toLogicalId: to });
		log.debug(`schedule: added edge ${from} -> ${to}`);
	}

	function removeEdge(edge: ScheduleEdge) {
		const i = dependencies.findIndex(
			(d) => d.fromLogicalId === edge.fromLogicalId && d.toLogicalId === edge.toLogicalId
		);
		if (i >= 0) {
			dependencies.splice(i, 1);
			log.debug(`schedule: removed edge ${edge.fromLogicalId} -> ${edge.toLogicalId}`);
		}
	}

	function path(from: string, to: string): string {
		const a = layout.boxes.get(from);
		const b = layout.boxes.get(to);
		if (a == null || b == null) return '';
		const x1 = a.x + CARD_W;
		const y1 = a.y + CARD_H / 2;
		const x2 = b.x;
		const y2 = b.y + CARD_H / 2;
		const mx = (x1 + x2) / 2;
		return `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2} ${y2}`;
	}

	function days(v: number): string {
		return formatFixed(v, $locale ?? 'de', 1);
	}
</script>

<!-- The CYCLE branch comes first on purpose: task-155 returns an EMPTY `tasks`
     alongside the error, so an `tasks.length < 2` empty-state check would
     swallow the notice and tell the user there is "nothing to connect" when in
     fact they have just drawn a cycle. The cards cannot be drawn (there are no
     scheduled tasks), so the notice plus the edges is all there is to show. -->
{#if schedule?.error?.kind === 'CYCLE'}
	<p
		class="mb-3 rounded border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900"
		data-testid="schedule-cycle"
	>
		{$_('schedule.error.cycle')}
	</p>
	<ul class="text-sm text-gray-700" data-testid="schedule-cycle-edges">
		{#each dependencies as edge (edge.fromLogicalId + '->' + edge.toLogicalId)}
			<li class="flex items-center gap-2 py-0.5">
				<span class="font-mono text-xs">{edge.fromLogicalId.slice(0, 8)} → {edge.toLogicalId.slice(0, 8)}</span>
				{#if editable}
					<button
						type="button"
						class="text-xs text-gray-500 underline hover:text-brand-green"
						data-testid="schedule-cycle-remove"
						aria-label={$_('schedule.editor.edgeAria', {
							values: { from: edge.fromLogicalId, to: edge.toLogicalId }
						})}
						onclick={() => removeEdge(edge)}>{$_('common.delete')}</button
					>
				{/if}
			</li>
		{/each}
	</ul>
{:else if tasks.length < 2}
	<p class="text-sm text-gray-600" data-testid="schedule-empty">{$_('schedule.editor.empty')}</p>
{:else}
	<div class="overflow-x-auto" data-testid="dependency-editor">
		<div
			class="relative"
			style="width: {layout.width}px; height: {layout.height}px; min-height: 80px;"
			role="presentation"
			ondragover={(e) => e.preventDefault()}
			ondrop={() => (dragFrom = null)}
		>
			<svg
				class="pointer-events-none absolute inset-0 overflow-visible"
				width={layout.width}
				height={layout.height}
			>
				{#each drawnEdges as e (e.from + '->' + e.to)}
					<path
						d={path(e.from, e.to)}
						fill="none"
						stroke="currentColor"
						stroke-width="2"
						class="pointer-events-auto cursor-pointer text-gray-400 hover:text-brand-green"
						role="button"
						tabindex="-1"
						aria-label={$_('schedule.editor.edgeAria', {
							values: { from: byId.get(e.from)?.title ?? '', to: byId.get(e.to)?.title ?? '' }
						})}
						data-testid="schedule-edge"
						onclick={() => editable && removeEdge(e.edge)}
						onkeydown={(k) => k.key === 'Enter' && editable && removeEdge(e.edge)}
					/>
				{/each}
			</svg>

			{#each visible as task (task.logicalId)}
				{@const box = layout.boxes.get(task.logicalId)}
				{#if box}
					<div
						class="absolute rounded-lg border bg-white px-3 py-2 shadow-sm {task.onCriticalPath
							? 'border-brand-green'
							: 'border-gray-200'} {cycleIds.has(task.logicalId) ? 'ring-2 ring-amber-400' : ''}"
						style="left: {box.x}px; top: {box.y}px; width: {CARD_W}px; height: {CARD_H}px;"
						data-testid="schedule-card"
						data-logical-id={task.logicalId}
						role="button"
						tabindex="0"
						ondragover={(e) => e.preventDefault()}
						ondrop={(e) => {
							e.preventDefault();
							e.stopPropagation();
							if (editable && dragFrom != null) addEdge(dragFrom, task.logicalId);
							dragFrom = null;
						}}
					>
						<div class="flex items-center gap-1">
							{#if task.isGroup}
								<button
									type="button"
									class="text-xs text-gray-500 hover:text-brand-green"
									aria-label={isExpanded(task.logicalId)
										? $_('schedule.editor.collapseAria')
										: $_('schedule.editor.expandAria')}
									data-testid="schedule-toggle"
									onclick={() => toggle(task.logicalId)}>{isExpanded(task.logicalId) ? '▾' : '▸'}</button
								>
							{/if}
							<span class="truncate text-sm font-medium" title={task.title}>{task.title}</span>
						</div>
						<div class="flex items-center justify-between text-xs text-gray-600">
							<span>{days(task.durationDays)} {$_('schedule.days')}</span>
							{#if editable}
								<span
									class="cursor-grab rounded border border-gray-300 px-1 select-none"
									draggable="true"
									role="button"
									tabindex="-1"
									aria-label={$_('schedule.editor.handleAria', { values: { title: task.title } })}
									data-dnd-handle
									data-testid="schedule-handle"
									ondragstart={() => (dragFrom = task.logicalId)}
									ondragend={() => (dragFrom = null)}>⋮⋮</span
								>
							{/if}
						</div>
					</div>
				{/if}
			{/each}
		</div>
	</div>
{/if}
