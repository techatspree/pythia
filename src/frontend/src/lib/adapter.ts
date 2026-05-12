import {
	createVersion,
	createGroup,
	createFixedItem,
	createTimeRelativeItem,
	ProjectPhase,
	EstimationParameter,
	EffortDriver
} from './domain/domain.mjs';

export interface CalcEntry {
	offerPT: number;
	cost: number;
	offerPrice: number;
}

export interface EditingPhase {
	name: string;
	abbreviation: string;
	durationWeeks: number | null;
}

interface EditingItem {
	logicalId: string;
	description: string;
	minEffort: number | null;
	expectedEffort: number | null;
	maxEffort: number | null;
	assumptions: string | null;
	type: string;
	unit: string | null;
	phaseAbbreviation: string | null;
}

interface EditingGroup {
	logicalId: string;
	title: string;
	items: EditingItem[];
}

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
	groups: EditingGroup[],
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
	const itemGroups = groups.map((g) =>
		createGroup(
			g.title,
			g.logicalId,
			g.items.map((i) =>
				i.type === 'TIME_RELATIVE'
					? createTimeRelativeItem(
							i.description,
							i.unit ?? 'h/Woche',
							i.minEffort ?? 0,
							i.expectedEffort ?? 0,
							i.maxEffort ?? 0,
							i.assumptions ?? '',
							i.logicalId,
							phaseByAbbr.get(i.phaseAbbreviation ?? '') ?? null
						)
					: createFixedItem(
							i.description,
							i.minEffort ?? 0,
							i.expectedEffort ?? 0,
							i.maxEffort ?? 0,
							i.assumptions ?? '',
							i.logicalId
						)
			)
		)
	);

	const version = createVersion(1, true, '', params, effortDrivers, [], itemGroups);
	const calculated = version.calculate();

	const m = new Map<string, CalcEntry>();
	const calcGroups = calculated.itemGroups.asJsReadonlyArrayView();
	for (const group of calcGroups) {
		const items = group.items.asJsReadonlyArrayView();
		for (const item of items) {
			m.set(item.logicalId, {
				offerPT: item.offerPT,
				cost: item.cost,
				offerPrice: item.offerPrice
			});
		}
	}
	return m;
}
