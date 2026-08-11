<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { formatDate, DEFAULT_LOCALE } from '$lib/format';
	import { log } from '$lib/log';
	import type { SessionDto } from '$lib/session/api';

	// The bucket the group landed on plus every write that lost the LWW race
	// (task-106). Only bucket+sampled sessions populate it; PERT sessions send
	// null, so this component renders nothing at all for them.
	type BucketAssignment = NonNullable<SessionDto['items'][number]['bucketAssignment']>;

	let { assignment }: { assignment: BucketAssignment | null | undefined } = $props();

	const loc = $derived($locale ?? DEFAULT_LOCALE);
	const conflicts = $derived(assignment?.conflictingAssignments ?? []);

	let open = $state(false);

	function toggle() {
		open = !open;
		log.debug(`ConflictBadge ${open ? 'opened' : 'closed'} (${conflicts.length} conflict(s))`);
	}
</script>

{#if conflicts.length > 0}
	<span class="relative inline-block">
		<button
			type="button"
			onclick={toggle}
			aria-expanded={open}
			data-testid="conflict-badge"
			class="inline-flex items-center gap-1 rounded-full border border-amber-300 bg-amber-50
			       px-2 py-0.5 text-xs font-medium text-amber-800 hover:bg-amber-100"
			title={$_('session.conflict.tooltip')}
		>
			⚠ {$_('session.conflict.badge', { values: { count: conflicts.length } })}
		</button>

		{#if open}
			<div
				data-testid="conflict-drawer"
				class="absolute left-0 z-20 mt-1 w-72 rounded-lg border border-gray-200 bg-white p-3 shadow-lg"
			>
				<p class="mb-2 text-xs text-gray-600">
					{$_('session.conflict.explain', { values: { bucket: assignment?.bucketId ?? '' } })}
				</p>
				<ul class="space-y-1">
					{#each conflicts as c (c.estimatorId + c.at)}
						<li class="flex items-baseline justify-between gap-2 text-xs">
							<span class="font-medium text-gray-800">{c.displayName ?? c.estimatorId}</span>
							<span class="text-gray-600">{c.bucketId}</span>
							<span class="text-gray-400">{formatDate(c.at, loc)}</span>
						</li>
					{/each}
				</ul>
			</div>
		{/if}
	</span>
{/if}
