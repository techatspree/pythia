// Shared editing-model types for the estimation tree plus the one-time
// normalisation from an API version payload into that model. Lives outside the
// components so the version page can normalise once and bind the result.

export type Leaf = {
	logicalId: string;
	type: 'FIXED' | 'TIME_RELATIVE';
	description: string;
	minEffort: number | null;
	expectedEffort: number | null;
	maxEffort: number | null;
	assumptions: string | null;
	phaseAbbreviation: string | null;
	unit: string | null;
};

export type Group = {
	logicalId: string;
	type: 'GROUP';
	title: string;
	children: Node[];
};

export type Node = Leaf | Group;

export type NodePath = number[];

export type CalcEntry = { offerPT: number; cost: number; offerPrice: number };

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
	return {
		logicalId: n.logicalId ?? newId(),
		type: n.type === 'TIME_RELATIVE' ? 'TIME_RELATIVE' : 'FIXED',
		description: n.description ?? '',
		minEffort: n.minEffort ?? null,
		expectedEffort: n.expectedEffort ?? null,
		maxEffort: n.maxEffort ?? null,
		assumptions: n.assumptions ?? null,
		phaseAbbreviation: n.phaseAbbreviation ?? null,
		unit: n.unit ?? null
	};
}

export function normalizeRoots(version: { roots?: RawNode[] } | null | undefined): Node[] {
	return (version?.roots ?? []).map(initNode);
}
