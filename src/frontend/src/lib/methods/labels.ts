import type { components } from '$lib/api/schema';

type EstimationMethod = components['schemas']['EstimationMethod'];

// Human-readable label for an estimation method. The `switch` is exhaustive:
// the TypeScript compiler fails if a new EstimationMethod arrives without a
// label mapping here.
export function formatMethodLabel(m: EstimationMethod): string {
	switch (m) {
		case 'THREE_POINT_PERT':
			return '3-Point PERT';
		case 'BUCKET_SAMPLED_PERT':
			return 'Bucket + Sampled 3-Point';
	}
}
