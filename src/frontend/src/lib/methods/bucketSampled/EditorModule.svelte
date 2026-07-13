<script lang="ts">
	/* eslint-disable no-useless-assignment -- placeholder declares the PERT
	   prop shape so the route binds uniformly; props are unused until task-104. */
	import { log } from '$lib/log';

	// Placeholder bucket+sampled editor (task-101). It declares the same
	// $bindable prop shape as the PERT module so the route can bind uniformly;
	// the props are aliased to unused `_`-prefixed locals until task-104 fills
	// in the real editor.
	let {
		roots: _roots = $bindable(),
		parameters: _parameters = $bindable(),
		effortDrivers: _effortDrivers = $bindable(),
		phases: _phases = $bindable(),
		additionalCosts: _additionalCosts = $bindable(),
		calcMap: _calcMap,
		editable: _editable
	} = $props();

	$effect(() => {
		log.warn('Bucket editor stub rendered; real implementation lands in task-104');
	});
</script>

<div class="p-4 text-gray-600">Bucket + sampled editor not yet available</div>
