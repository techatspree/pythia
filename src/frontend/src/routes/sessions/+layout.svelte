<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { resolve } from '$app/paths';
	import logo from '$lib/assets/logo.svg';

	// Room chrome for the collaborative-session group (task-066). The standard
	// app header is hidden for /sessions (see the root +layout.svelte), so this
	// is the full-screen "room" shell. Auth + i18n still come from the root
	// layout (this one is nested inside it). The per-room session title +
	// connection indicator live in the room page itself.
	let { children } = $props();
</script>

<div class="min-h-screen flex flex-col bg-gray-50">
	<div class="flex items-center gap-3 px-6 py-3 bg-white border-b">
		<!-- Decorative: the label beside it already names the product. -->
		<img src={logo} alt="" aria-hidden="true" width="24" height="24" class="w-6 h-6" />
		<span class="text-brand-green text-xs font-semibold tracking-widest uppercase"
			>{$_('brand.sessionTitle')}</span
		>
		<a
			href={resolve('/projects')}
			class="ml-auto text-sm text-brand-green hover:text-[#007a45] underline-offset-2 hover:underline"
			>{$_('session.layout.leave')}</a
		>
	</div>
	<div class="flex-1">
		{@render children()}
	</div>
</div>
