import type { Component } from 'svelte';
import type { components } from '$lib/api/schema';
import { log } from '$lib/log';

type EstimationMethod = components['schemas']['EstimationMethod'];

// A lazy-loaded estimation-method editor module. A `.svelte` dynamic import
// exposes the component as its `default` export. `any` props: each method's
// editor has a different prop shape, so the registry holds them loosely.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type EditorModule = { default: Component<any> };

// One editor module per estimation method, loaded on demand via import().
const methodRegistry: Record<EstimationMethod, () => Promise<EditorModule>> = {
	THREE_POINT_PERT: () => import('./pert/EditorModule.svelte'),
	BUCKET_SAMPLED_PERT: () => import('./bucketSampled/EditorModule.svelte')
};

// Human-readable (German) label per method. Typed as a full Record so the
// compiler forces a label whenever a new method is added to methodRegistry.
const methodLabels: Record<EstimationMethod, string> = {
	THREE_POINT_PERT: '3-Punkt-PERT',
	BUCKET_SAMPLED_PERT: 'Bucket + Stichprobe'
};

// The methods offered in the create-estimation picker, DERIVED from the
// registered editor modules — a newly registered (fully-supported) method
// appears here automatically. Registering a method in methodRegistry implies
// it is supported end-to-end (backend persistence + editor).
export const availableMethods: { method: EstimationMethod; label: string }[] = (
	Object.keys(methodRegistry) as EstimationMethod[]
).map((m) => ({ method: m, label: methodLabels[m] }));

// Resolve the editor module for a method. Logs on success; on a missing entry
// it logs and throws so the caller's ErrorBanner surfaces the failure.
export async function loadEditorModule(method: EstimationMethod): Promise<EditorModule> {
	const loader = methodRegistry[method];
	if (!loader) {
		log.error(`No editor module registered for estimation method: ${method}`);
		throw new Error(`No editor module registered for estimation method: ${method}`);
	}
	const mod = await loader();
	log.info(`Loaded editor module for estimation method: ${method}`);
	return mod;
}
