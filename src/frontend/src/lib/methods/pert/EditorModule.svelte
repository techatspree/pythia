<script lang="ts">
	import EstimationGrid from '$lib/components/EstimationGrid.svelte';
	import ParametersPanel from '$lib/components/ParametersPanel.svelte';
	import EffortDriversPanel from '$lib/components/EffortDriversPanel.svelte';
	import PhasesPanel from '$lib/components/PhasesPanel.svelte';
	import AdditionalCostsPanel from '$lib/components/AdditionalCostsPanel.svelte';
	import type { ApiAdditionalCost } from '$lib/api/types.js';
	import type { CalcEntry } from '$lib/estimationNodes';
	import { ZERO_TOTALS, type EstimationTotalsView } from '$lib/adapter';

	// Three-point PERT editor module (task-101). The route owns the state,
	// autosave and undo/redo; this module is the pure $bindable view over the
	// editable collections. Each collection is $bindable so child-panel edits
	// propagate back to the route (the task-081/109 pattern).
	// A module-level constant, not an inline `new Set()` default: a fresh set per
	// render would be a new prop identity every time.
	const EMPTY_CRITICAL_PATH: ReadonlySet<string> = new Set();

	let {
		roots = $bindable(),
		dailyRate = $bindable<number>(800),
		stdDevFactor = $bindable<number>(2.0),
		salesSurcharge = $bindable<number>(0.1),
		effortDrivers = $bindable(),
		phases = $bindable(),
		additionalCosts = $bindable(),
		// PERT has no buckets; declared only so the route can bind uniformly
		// across method modules (bind:buckets). Unused here.
		// eslint-disable-next-line no-useless-assignment
		buckets: _buckets = $bindable(),
		calcMap,
		totals = ZERO_TOTALS,
		criticalPath = EMPTY_CRITICAL_PATH,
		editable
	}: {
		roots: any[];
		dailyRate: number;
		stdDevFactor: number;
		salesSurcharge: number;
		effortDrivers: any[];
		phases: any[];
		additionalCosts: ApiAdditionalCost[];
		buckets?: unknown[];
		calcMap: Map<string, CalcEntry>;
		totals?: EstimationTotalsView;
		criticalPath?: ReadonlySet<string>;
		editable: boolean;
	} = $props();
</script>

<ParametersPanel bind:dailyRate bind:stdDevFactor bind:salesSurcharge {editable} />

<EffortDriversPanel bind:effortDrivers {editable} />

<PhasesPanel bind:phases {roots} {calcMap} {editable} />

<AdditionalCostsPanel bind:costs={additionalCosts} {phases} {editable} />

<EstimationGrid bind:roots {editable} {calcMap} {phases} {totals} {criticalPath} />
