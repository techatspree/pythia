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
	// A module-level constant, not an inline `new Map()` default: a fresh map per
	// render would be a new prop identity every time.
	const NO_LABELS: ReadonlyMap<string, string> = new Map();

	let {
		schedule,
		dependencies = $bindable(),
		editable = true,
		labels = NO_LABELS
	}: {
		schedule: ProjectScheduleView | null;
		dependencies: ScheduleEdge[];
		editable?: boolean;
		labels?: ReadonlyMap<string, string>;
	} = $props();

	/**
	 * One logical id → the name to show a user.
	 *
	 * Deliberately NOT `byId` (which is built from `schedule.tasks`): a failed
	 * schedule returns an EMPTY task list, so at the moment the cycle notice
	 * renders the schedule can name nothing. The caller passes names read off
	 * the estimation tree instead (task-171). A miss is reachable — a
	 * dependency outlives the item it referenced, because the version editor's
	 * PUT omits `dependencies` — so it gets a placeholder, never a raw id.
	 */
	function nameOf(logicalId: string): string {
		return labels.get(logicalId) ?? $_('schedule.editor.unknownItem');
	}

	const CARD_W = 200;
	const CARD_H = 62;
	const GAP_X = 72;
	const GAP_Y = 16;
	// Indentation per nesting level (task-167). Deliberately small relative to
	// CARD_W + GAP_X (272): `x` carries BOTH the dependency layer and the depth,
	// so a deeply nested card must never drift far enough right to look like it
	// sits in the next layer.
	const INDENT = 24;
	// Padding of a group's container outline around its subtree's cards.
	const BOX_PAD = 10;

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
		const boxes = new Map<string, { x: number; y: number }>();
		// ROW assignment walks the tree DEPTH-FIRST and hands out the next free
		// row, so a group's descendants occupy a CONTIGUOUS band — which is what
		// makes indentation and the container outline legible. It used to be a
		// flat per-layer fill counter, which scattered a group's children across
		// the canvas with nothing tying them to their parent (task-167).
		//
		// LAYER assignment above is untouched: `x` must keep encoding dependency
		// order, and none of the schedule semantics change here.
		let nextRow = 0;
		const visibleIds = new Set(order);
		// A child must never be placed LEFT of its parent, or the indentation and
		// the container outline stop reading as containment. `layer` is derived
		// from DEPENDENCIES alone, so an undependent child of a layer-2 group
		// would otherwise get layer 0 and land far to its parent's left. Clamp
		// each child's effective layer to at least its parent's, then indent
		// within that layer — so x is monotonic down a branch by construction.
		const place = (task: ScheduledTaskView, parentLayer: number, parentX: number) => {
			if (!visibleIds.has(task.logicalId)) return;
			const effectiveLayer = Math.max(layer.get(task.logicalId) ?? 0, parentLayer);
			const x = Math.max(
				effectiveLayer * (CARD_W + GAP_X) + task.depth * INDENT,
				// belt and braces: even at the same layer, sit strictly right of the
				// parent so a child is always visibly nested under it.
				parentX + (task.depth > 0 ? INDENT : 0)
			);
			boxes.set(task.logicalId, { x, y: nextRow * (CARD_H + GAP_Y) });
			nextRow += 1;
			// `tasks` is already in tree order (task-164), so a node's children are
			// exactly the following entries that name it as parent.
			for (const child of tasks) {
				if (child.parentLogicalId === task.logicalId) place(child, effectiveLayer, x);
			}
		};
		for (const task of tasks) {
			if (task.parentLogicalId == null) place(task, 0, 0);
		}
		// Width from the placed boxes, not from the raw layer map: clamping can
		// push a card further right than its own layer would suggest.
		const maxX = Math.max(0, ...[...boxes.values()].map((b) => b.x));
		const width = maxX + CARD_W + GAP_X;
		const height = Math.max(1, nextRow) * (CARD_H + GAP_Y);
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

	/**
	 * A dashed rect around each EXPANDED group's own card and all its
	 * descendants' cards, so a subitem's parent is always recognisable. Derived
	 * from the boxes the layout already computed — no second layout pass.
	 *
	 * A COLLAPSED group gets none: it is a single card with no descendants on
	 * canvas, so the card already IS the group and a rect around one card is
	 * noise.
	 */
	const groupBoxes = $derived.by(() => {
		const out: { id: string; title: string; x: number; y: number; w: number; h: number }[] = [];
		for (const g of visible) {
			if (!g.isGroup || !isExpanded(g.logicalId)) continue;
			const members = [g, ...tasks.filter((t) => isDescendantOf(t, g.logicalId))]
				.map((t) => layout.boxes.get(t.logicalId))
				.filter((b): b is { x: number; y: number } => b != null);
			if (members.length < 2) continue;
			const minX = Math.min(...members.map((b) => b.x));
			const minY = Math.min(...members.map((b) => b.y));
			const maxX = Math.max(...members.map((b) => b.x)) + CARD_W;
			const maxY = Math.max(...members.map((b) => b.y)) + CARD_H;
			out.push({
				id: g.logicalId,
				title: g.title,
				x: minX - BOX_PAD,
				y: minY - BOX_PAD,
				w: maxX - minX + 2 * BOX_PAD,
				h: maxY - minY + 2 * BOX_PAD
			});
		}
		return out;
	});

	function isDescendantOf(task: ScheduledTaskView, ancestorId: string): boolean {
		let p = task.parentLogicalId;
		while (p != null) {
			if (p === ancestorId) return true;
			p = byId.get(p)?.parentLogicalId ?? null;
		}
		return false;
	}

	const cycleIds = $derived(
		new Set(schedule?.error?.kind === 'CYCLE' ? schedule.error.involvedLogicalIds : [])
	);

	let dragFrom = $state<string | null>(null);
	// The in-flight arrow's endpoint, in CANVAS-LOCAL coordinates.
	//
	// Native HTML5 drag does not give usable coordinates: `dragover` fires on the
	// drop target rather than continuously, and `drag` events report
	// clientX/clientY as 0 in some browsers. So the live arrow is driven by
	// `pointermove` on the canvas. The DROP itself stays native
	// (`draggable`/`ondrop`) — it has no cursor-geometry race, which is why
	// task-157 chose it and why task-163's failure class does not apply here.
	let pointerPos = $state<{ x: number; y: number } | null>(null);
	let canvasEl = $state<HTMLDivElement | null>(null);

	// Driven by `dragover`, NOT `pointermove`: the browser SUPPRESSES pointer
	// events for the duration of a native HTML5 drag, so a pointermove handler
	// never fires once the drag starts and the arrow stayed invisible. `dragover`
	// fires repeatedly while the pointer moves over the canvas and carries usable
	// clientX/clientY (unlike `drag` on the source, which reports 0 in some
	// browsers). The DROP stays native, so there is still no cursor-geometry
	// race — task-163's failure class does not reach this component.
	function trackDragPointer(e: DragEvent) {
		e.preventDefault();
		if (dragFrom == null || canvasEl == null) return;
		const r = canvasEl.getBoundingClientRect();
		pointerPos = { x: e.clientX - r.left, y: e.clientY - r.top };
	}

	function endDrag() {
		dragFrom = null;
		pointerPos = null;
	}

	/** The dashed rubber-band path from the source card to the pointer. */
	const dragPath = $derived.by(() => {
		if (dragFrom == null || pointerPos == null) return '';
		const a = layout.boxes.get(dragFrom);
		if (a == null) return '';
		const x1 = a.x + CARD_W;
		const y1 = a.y + CARD_H / 2;
		const mx = (x1 + pointerPos.x) / 2;
		return `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${pointerPos.y}, ${pointerPos.x} ${pointerPos.y}`;
	});

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
			<li
				class="flex items-center gap-2 py-0.5"
				title="{edge.fromLogicalId} → {edge.toLogicalId}"
			>
				<span>{nameOf(edge.fromLogicalId)} → {nameOf(edge.toLogicalId)}</span>
				{#if editable}
					<button
						type="button"
						class="text-xs text-gray-500 underline hover:text-brand-green"
						data-testid="schedule-cycle-remove"
						aria-label={$_('schedule.editor.edgeAria', {
							values: { from: nameOf(edge.fromLogicalId), to: nameOf(edge.toLogicalId) }
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
			bind:this={canvasEl}
			class="relative"
			style="width: {layout.width}px; height: {layout.height}px; min-height: 80px;"
			role="presentation"
			ondragover={trackDragPointer}
			ondrop={endDrag}
		>
			<svg
				class="pointer-events-none absolute inset-0 overflow-visible"
				width={layout.width}
				height={layout.height}
			>
				<defs>
					<!-- One marker, referenced by every edge, so direction is visible
					     on a committed edge as well as on the in-flight one. -->
					<marker
						id="schedule-arrow"
						viewBox="0 0 10 10"
						refX="9"
						refY="5"
						markerWidth="6"
						markerHeight="6"
						orient="auto"
					>
						<path d="M 0 0 L 10 5 L 0 10 z" fill="currentColor" />
					</marker>
				</defs>

				<!-- Group containers: a dashed rect around an expanded group's own
				     card and its whole subtree, so a subitem's parent is always
				     recognisable. Drawn first so cards sit on top.

				     The box carries NO visible text label. Its owner is the card
				     in its top-left corner by construction — `place()` emits a
				     group before its descendants, and task-167 clamps a child to
				     never sit left of its parent — so a label at that corner is
				     both redundant with the card's own title and painted BEHIND
				     that opaque `z-10` card, which is how it read as an
				     unreadable smudge. The name still reaches assistive tech via
				     the rect's aria-label. Do not re-add a corner <text>: the
				     canvas has no top padding and `overflow-x-auto` makes the
				     y axis scrollable too, so there is no clear band above the
				     rect to move it into either. -->
				{#each groupBoxes as g (g.id)}
					<g class="text-gray-300" data-testid="schedule-group-box" data-logical-id={g.id}>
						<rect
							x={g.x}
							y={g.y}
							width={g.w}
							height={g.h}
							rx="12"
							fill="none"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-dasharray="6 4"
							aria-label={$_('schedule.editor.groupBoxAria', { values: { title: g.title } })}
						/>
					</g>
				{/each}

				{#each drawnEdges as e (e.from + '->' + e.to)}
					<path
						d={path(e.from, e.to)}
						fill="none"
						stroke="currentColor"
						stroke-width="2"
						marker-end="url(#schedule-arrow)"
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
						class="absolute z-10 rounded-lg border bg-white px-3 py-2 shadow-sm {task.onCriticalPath
							? 'border-brand-green'
							: 'border-gray-200'} {cycleIds.has(task.logicalId) ? 'ring-2 ring-amber-400' : ''}"
						style="left: {box.x}px; top: {box.y}px; width: {CARD_W}px; height: {CARD_H}px;"
						data-testid="schedule-card"
						data-logical-id={task.logicalId}
						role="button"
						tabindex="0"
						ondragover={trackDragPointer}
						ondrop={(e) => {
							e.preventDefault();
							e.stopPropagation();
							if (editable && dragFrom != null) addEdge(dragFrom, task.logicalId);
							endDrag();
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
									ondragend={endDrag}>⋮⋮</span
								>
							{/if}
						</div>
					</div>
				{/if}
			{/each}

			<!-- The in-flight arrow is its own layer, ON TOP of the cards.
			     Everything here is absolutely positioned with no z-index, so paint
			     order followed DOM order and the arrow — emitted with the edges
			     BELOW the cards — was hidden behind whichever card the pointer was
			     over, exactly where the user needs to see it. Committed edges stay
			     underneath on purpose: they run card-edge to card-edge, and passing
			     under an intervening card is normal for a net plan.
			     `pointer-events-none` is load-bearing: without it this overlay would
			     swallow the dragover/drop the cards need. -->
			<svg
				class="pointer-events-none absolute inset-0 z-20 overflow-visible"
				width={layout.width}
				height={layout.height}
			>
				{#if dragPath}
					<defs>
						<!-- A distinct id: duplicate ids in one document are invalid and
						     the first would win. -->
						<marker
							id="schedule-arrow-live"
							viewBox="0 0 10 10"
							refX="9"
							refY="5"
							markerWidth="6"
							markerHeight="6"
							orient="auto"
						>
							<path d="M 0 0 L 10 5 L 0 10 z" fill="currentColor" />
						</marker>
					</defs>
					<path
						d={dragPath}
						fill="none"
						stroke="currentColor"
						stroke-width="2"
						stroke-dasharray="5 4"
						marker-end="url(#schedule-arrow-live)"
						class="text-brand-green"
						data-testid="schedule-drag-arrow"
						aria-label={$_('schedule.editor.dragAria')}
					/>
				{/if}
			</svg>
		</div>
	</div>
{/if}
