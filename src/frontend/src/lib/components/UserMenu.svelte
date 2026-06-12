<script lang="ts">
	import type { AuthAccount } from '$lib/auth/AuthProvider';
	import { getAuthProvider } from '$lib/auth';

	const ROLE_ORDER = [
		'VIEWER',
		'ESTIMATOR',
		'ADMIN'
	] as const;

	let {
		account,
		onlogout
	}: {
		account: AuthAccount;
		onlogout: () => void;
	} = $props();
</script>

<div class="flex items-center gap-3">
	<span class="text-sm text-gray-700">
		{account.displayName ?? account.subjectId}
	</span>
	<div class="flex items-center gap-1">
		{#each ROLE_ORDER as r}
			{#if account.roles.includes(r)}
				<span
					class="text-xs uppercase tracking-wide px-1.5 py-0.5 rounded bg-brand-green/10 text-brand-green"
				>{r}</span>
			{/if}
		{/each}
	</div>
	<button
		type="button"
		data-testid="logout-button"
		class="text-sm text-brand-green hover:text-[#007a45] underline-offset-2 hover:underline"
		onclick={async () => {
			await getAuthProvider().logout();
			onlogout();
		}}
	>Logout</button>
</div>
