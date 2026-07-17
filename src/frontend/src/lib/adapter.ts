import {
	createVersion,
	createGroup,
	createFixedItem,
	createTimeRelativeItem,
	createBucketedItem,
	ProjectPhase,
	EstimationParameter,
	EffortDriver,
	type EstimationNode,
	EstimationGroup
} from './domain/domain.mjs';

export interface CalcEntry {
	// `mean` is the pre-surcharge effort; the bucket editor shows it read-only on
	// non-sample rows (the domain reducer derives it from the bucket's samples).
	mean: number;
	offerPT: number;
	cost: number;
	offerPrice: number;
}

export interface EditingPhase {
	name: string;
	abbreviation: string;
	durationWeeks: number | null;
}

interface EditingLeaf {
	logicalId: string;
	type: 'FIXED' | 'TIME_RELATIVE' | 'BUCKETED';
	description: string;
	minEffort: number | null;
	expectedEffort: number | null;
	maxEffort: number | null;
	assumptions: string | null;
	unit: string | null;
	phaseAbbreviation: string | null;
	// Bucket + sampled (task-104): a sample's optimistic/likely/pessimistic
	// reuse min/expected/max; non-samples inherit the bucket mean in the domain.
	bucketId?: string | null;
	isSample?: boolean;
}

interface EditingGroup {
	logicalId: string;
	type: 'GROUP';
	title: string;
	children: EditingNode[];
}

type EditingNode = EditingLeaf | EditingGroup;

interface EditingParam {
	name: string;
	value: number;
	comment: string | null;
}

interface EditingDriver {
	description: string;
	factor: number;
	comment: string | null;
}

export function computeCalcMap(
	roots: EditingNode[],
	parameters: EditingParam[],
	drivers: EditingDriver[],
	phases: EditingPhase[]
): Map<string, CalcEntry> {
	const phaseByAbbr = new Map(
		phases.map((p) => [
			p.abbreviation,
			new ProjectPhase(p.name, p.abbreviation, p.durationWeeks ?? 0)
		])
	);

	const params = parameters.map((p) => new EstimationParameter(p.name, p.value, p.comment ?? ''));
	const effortDrivers = drivers.map((d) => new EffortDriver(d.description, d.factor, d.comment ?? ''));

	function buildNode(node: EditingNode): EstimationNode {
		if (node.type === 'GROUP') {
			return createGroup(
				node.title,
				node.logicalId,
				node.children.map(buildNode)
			);
		}
		if (node.type === 'TIME_RELATIVE') {
			return createTimeRelativeItem(
				node.description,
				node.unit ?? 'h/Woche',
				node.minEffort ?? 0,
				node.expectedEffort ?? 0,
				node.maxEffort ?? 0,
				node.assumptions ?? '',
				node.logicalId,
				phaseByAbbr.get(node.phaseAbbreviation ?? '') ?? null
			);
		}
		if (node.type === 'BUCKETED') {
			// The domain reducer (task-102) derives the per-bucket mean; the
			// adapter only plumbs the leaf in. Sample values reuse min/expected/max.
			return createBucketedItem(
				node.description,
				node.bucketId ?? '',
				node.isSample ?? false,
				node.minEffort ?? 0,
				node.expectedEffort ?? 0,
				node.maxEffort ?? 0,
				node.logicalId
			);
		}
		return createFixedItem(
			node.description,
			node.minEffort ?? 0,
			node.expectedEffort ?? 0,
			node.maxEffort ?? 0,
			node.assumptions ?? '',
			node.logicalId
		);
	}

	const domainRoots = roots.map(buildNode);
	const version = createVersion(1, true, '', params, effortDrivers, [], domainRoots);
	const calculated = version.calculate();

	const m = new Map<string, CalcEntry>();
	function walk(node: EstimationNode): void {
		m.set(node.logicalId, {
			mean: node.mean,
			offerPT: node.offerPT,
			cost: node.cost,
			offerPrice: node.offerPrice
		});
		if (node instanceof EstimationGroup) {
			const children = node.children.asJsReadonlyArrayView();
			for (const child of children) walk(child);
		}
	}
	const calcRoots = calculated.roots.asJsReadonlyArrayView();
	for (const root of calcRoots) walk(root);
	return m;
}
