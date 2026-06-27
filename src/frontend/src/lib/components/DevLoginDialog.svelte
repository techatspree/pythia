<script lang="ts">
	import { DEV_USERS, setDevSubject } from '$lib/auth/DevAuthProvider';

	let { onlogin }: { onlogin: () => void } = $props();

	function pick(subjectId: string) {
		setDevSubject(subjectId);
		onlogin();
	}
</script>

<div
	class="fixed inset-0 z-50 bg-black/40 flex items-center justify-center"
	role="dialog"
	aria-modal="true"
	aria-label="Dev login picker"
>
	<div class="bg-white rounded-lg shadow-lg p-6 w-full max-w-sm">
		<h2 class="text-lg font-semibold text-brand-green mb-1">Dev login</h2>
		<p class="text-xs text-gray-500 mb-4">
			Local development only. Pick a role to act as.
		</p>
		<div class="flex flex-col gap-2">
			{#each Object.values(DEV_USERS) as user (user.subjectId)}
				<button
					type="button"
					onclick={() => pick(user.subjectId)}
					class="w-full text-left px-3 py-2 border border-gray-200 rounded hover:bg-brand-green/10 hover:border-brand-green/40"
					data-testid="dev-login-{user.subjectId}"
				>
					<div class="text-sm font-medium text-gray-800">{user.displayName}</div>
					<div class="text-xs text-gray-500">{user.roles.join(', ')}</div>
				</button>
			{/each}
		</div>
	</div>
</div>
