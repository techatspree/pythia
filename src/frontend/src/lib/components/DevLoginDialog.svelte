<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { DEV_USERS, setDevSubject } from '$lib/auth/DevAuthProvider';
	import logo from '$lib/assets/logo.svg';

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
	aria-label={$_('dialog.devLogin.ariaLabel')}
>
	<div class="bg-white rounded-lg shadow-lg p-6 w-full max-w-sm">
		<img src={logo} alt={$_('brand.logoAlt')} width="40" height="40" class="w-10 h-10 mb-2" />
		<h2 class="text-lg font-semibold text-brand-green mb-1">{$_('dialog.devLogin.title')}</h2>
		<p class="text-xs text-gray-500 mb-4">
			{$_('dialog.devLogin.subtitle')}
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
