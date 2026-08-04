<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { _ } from 'svelte-i18n';
	import { log } from '$lib/log';

	// Root error boundary (task-026): render a friendly message for uncaught
	// load/render errors and log the error via the shared $lib/log helper.
	$effect(() => {
		log.error(`Unhandled page error (${page.status})`, page.error);
	});
</script>

<div class="p-8 max-w-lg mx-auto text-center">
	<h1 class="text-2xl font-bold mb-2">{$_('error.title')}</h1>
	<p class="text-gray-600 mb-6">{page.error?.message ?? $_('error.generic')}</p>
	<a href={resolve('/')} class="text-brand-green hover:underline">{$_('error.backHome')}</a>
</div>
