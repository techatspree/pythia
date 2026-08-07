import {
	createVersion,
	createGroup,
	createFixedItem,
	createTimeRelativeItem,
	createBucketedItem,
	ProjectPhase,
	EffortDriver,
	AdditionalCost,
	AdditionalCostType,
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

/**
 * The whole-estimation totals, as plain numbers. Mirrors the domain's
 * `EstimationTotals` so consumers never handle a Kotlin/JS object.
 */
export interface EstimationTotalsView {
	leafCount: number;
	meanPT: number;
	riskSurchargePT: number;
	driverSurchargePT: number;
	offerPT: number;
	developmentCost: number;
	/** The tree's own offer price, excluding additional costs. */
	developmentOfferPrice: number;
	additionalOneTime: number;
	additionalRecurring: number;
	salesSurchargeAmount: number;
	totalOfferPrice: number;
	recurringWithoutPhase: number;
}

export const ZERO_TOTALS: EstimationTotalsView = {
	leafCount: 0,
	meanPT: 0,
	riskSurchargePT: 0,
	driverSurchargePT: 0,
	offerPT: 0,
	developmentCost: 0,
	developmentOfferPrice: 0,
	additionalOneTime: 0,
	additionalRecurring: 0,
	salesSurchargeAmount: 0,
	totalOfferPrice: 0,
	recurringWithoutPhase: 0
};

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

interface EditingDriver {
	description: string;
	factor: number;
	comment: string | null;
}

interface EditingAdditionalCost {
	description: string;
	amount: number;
	type: 'ONE_TIME' | 'RECURRING';
	amountPerWeek: number | null;
	phaseAbbreviation: string | null;
}

export interface EstimationComputation {
	calcMap: Map<string, CalcEntry>;
	totals: EstimationTotalsView;
}

/**
 * Builds the domain version once, calculates it once, and reads BOTH the
 * per-node calc map and the whole-estimation totals off that single result —
 * the totals come from the domain reducer, never from summing in the UI.
 */
export function computeEstimation(
	roots: EditingNode[],
	params: { dailyRate: number; stdDevFactor: number; salesSurcharge: number },
	drivers: EditingDriver[],
	phases: EditingPhase[],
	additionalCosts: EditingAdditionalCost[] = []
): EstimationComputation {
	const phaseByAbbr = new Map(
		phases.map((p) => [
			p.abbreviation,
			new ProjectPhase(p.name, p.abbreviation, p.durationWeeks ?? 0)
		])
	);

	const effortDrivers = drivers.map((d) => new EffortDriver(d.description, d.factor, d.comment ?? ''));

	const costs = additionalCosts.map(
		(c) =>
			new AdditionalCost(
				c.description,
				c.amount ?? 0,
				AdditionalCostType.values().find((v) => v.name === c.type) ?? AdditionalCostType.ONE_TIME,
				c.amountPerWeek ?? 0,
				phaseByAbbr.get(c.phaseAbbreviation ?? '') ?? null
			)
	);

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
	const version = createVersion(
		1,
		true,
		'',
		params.dailyRate,
		params.stdDevFactor,
		params.salesSurcharge,
		effortDrivers,
		Array.from(phaseByAbbr.values()),
		costs,
		domainRoots
	);
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

	const t = calculated.totals();
	return {
		calcMap: m,
		totals: {
			leafCount: t.leafCount,
			meanPT: t.meanPT,
			riskSurchargePT: t.riskSurchargePT,
			driverSurchargePT: t.driverSurchargePT,
			offerPT: t.offerPT,
			developmentCost: t.developmentCost,
			developmentOfferPrice: t.developmentOfferPrice,
			additionalOneTime: t.additionalOneTime,
			additionalRecurring: t.additionalRecurring,
			salesSurchargeAmount: t.salesSurchargeAmount,
			totalOfferPrice: t.totalOfferPrice,
			recurringWithoutPhase: t.recurringWithoutPhase
		}
	};
}
