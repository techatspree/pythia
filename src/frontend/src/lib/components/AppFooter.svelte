<script lang="ts">
	import { _ } from 'svelte-i18n';
	import OssNoticesDialog from '#lib/components/OssNoticesDialog.svelte';
	import type { AuthAccount } from '#lib/auth/AuthProvider.ts';

	// The ONE app footer, the counterpart to AppHeader: rendered by the root
	// layout on every route. It holds the legal information, deliberately kept
	// out of the header so it does not compete with navigation (task-210 moved
	// the licences link here from the user menu). Add further legal links (an
	// imprint, a privacy notice) to the row below rather than to the header.
	// Like the header's navigation, the links show only to a signed-in user.
	let { account }: { account: AuthAccount | null } = $props();

	let ossNoticesOpen = $state(false);
</script>

{#if account}
	<footer class="border-t border-hairline bg-white" data-testid="app-footer">
		<div class="px-6 py-2 flex flex-wrap items-center justify-end gap-4 text-xs">
			<button
				type="button"
				data-testid="app-footer.oss-notices"
				class="text-ink-muted hover:text-ink-strong underline-offset-2 hover:underline"
				onclick={() => (ossNoticesOpen = true)}
			>{$_('footer.ossNotices')}</button>
		</div>
	</footer>

	<OssNoticesDialog bind:open={ossNoticesOpen} />
{/if}
