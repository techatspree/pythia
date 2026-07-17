// Shared editing-model types for the estimation tree plus the one-time
// normalisation from an API version payload into that model. Lives outside the
// components so the version page can normalise once and bind the result.

export type Leaf = {
	logicalId: string;
	type: 'FIXED' | 'TIME_RELATIVE' | 'BUCKETED';
	description: string;
	minEffort: number | null;
	expectedEffort: number | null;
	maxEffort: number | null;
	assumptions: string | null;
	phaseAbbreviation: string | null;
	unit: string | null;
	// Bucket + sampled method (task-104). Optional so FIXED/TIME_RELATIVE leaf
	// literals elsewhere still typecheck. A sample stores its three-point values
	// in minEffort/expectedEffort/maxEffort (reused columns, matching task-103).
	bucketId?: string | null;
	isSample?: boolean;
};

export type Group = {
	logicalId: string;
	type: 'GROUP';
	title: string;
	children: Node[];
};

export type Node = Leaf | Group;

export type NodePath = number[];

export type CalcEntry = { mean: number; offerPT: number; cost: number; offerPrice: number };

export function newId(): string {
	return crypto.randomUUID();
}

/** Loose shape of a node as it arrives in an API version payload. */
type RawNode = {
	type?: string;
	logicalId?: string | null;
	title?: string | null;
	description?: string | null;
	children?: RawNode[];
	minEffort?: number | null;
	expectedEffort?: number | null;
	maxEffort?: number | null;
	assumptions?: string | null;
	phaseAbbreviation?: string | null;
	unit?: string | null;
	bucketId?: string | null;
	isSample?: boolean;
};

function initNode(n: RawNode): Node {
	if (n.type === 'GROUP') {
		return {
			logicalId: n.logicalId ?? newId(),
			type: 'GROUP',
			title: n.title ?? '',
			children: (n.children ?? []).map(initNode)
		};
	}
	const leafType: Leaf['type'] =
		n.type === 'TIME_RELATIVE' ? 'TIME_RELATIVE' : n.type === 'BUCKETED' ? 'BUCKETED' : 'FIXED';
	return {
		logicalId: n.logicalId ?? newId(),
		type: leafType,
		description: n.description ?? '',
		minEffort: n.minEffort ?? null,
		expectedEffort: n.expectedEffort ?? null,
		maxEffort: n.maxEffort ?? null,
		assumptions: n.assumptions ?? null,
		phaseAbbreviation: n.phaseAbbreviation ?? null,
		unit: n.unit ?? null,
		bucketId: n.bucketId ?? null,
		isSample: n.isSample ?? false
	};
}

export function normalizeRoots(version: { roots?: RawNode[] } | null | undefined): Node[] {
	return (version?.roots ?? []).map(initNode);
}
