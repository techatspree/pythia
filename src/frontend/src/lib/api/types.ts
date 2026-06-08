export interface ApiParameter {
	id: string | null;
	name: string;
	value: number;
	comment: string | null;
}

export interface ApiEffortDriver {
	id: string | null;
	description: string;
	factor: number;
	comment: string | null;
}

export interface ApiPhase {
	id: string | null;
	name: string;
	abbreviation: string;
	durationWeeks: number | null;
}

/** Recursive tree node returned by the backend (groups + leaves). */
export interface ApiEstimationNode {
	logicalId: string | null;
	type: 'GROUP' | 'FIXED' | 'TIME_RELATIVE';
	title: string | null;
	description: string | null;
	code: string | null;
	minEffort: number | null;
	expectedEffort: number | null;
	maxEffort: number | null;
	assumptions: string | null;
	mean: number;
	variance: number;
	riskSurcharge: number;
	driverSurcharge: number;
	offerPT: number;
	cost: number;
	offerPrice: number;
	unit: string | null;
	phaseAbbreviation: string | null;
	children: ApiEstimationNode[];
}

/** Tree node sent back on PUT (user-editable fields only). */
export interface ApiEstimationNodeUpdate {
	logicalId: string | null;
	type: 'GROUP' | 'FIXED' | 'TIME_RELATIVE';
	title?: string | null;
	description?: string | null;
	code?: string | null;
	minEffort?: number | null;
	expectedEffort?: number | null;
	maxEffort?: number | null;
	assumptions?: string | null;
	unit?: string | null;
	phaseAbbreviation?: string | null;
	children?: ApiEstimationNodeUpdate[];
}

export interface ApiAdditionalCost {
	id: string | null;
	description: string;
	amount: number;
	type: 'ONE_TIME' | 'RECURRING';
	amountPerWeek: number | null;
	phaseAbbreviation: string | null;
}

export interface ApiVersionResponse {
	versionNumber: number;
	isDraft: boolean;
	totalEffort: number;
	notes: string | null;
	createdAt: string | null;
	submittedAt: string | null;
	parameters: ApiParameter[];
	effortDrivers: ApiEffortDriver[];
	phases: ApiPhase[];
	roots: ApiEstimationNode[];
	additionalCosts: ApiAdditionalCost[];
}

export interface ApiVersionSummary {
	versionNumber: number;
	isDraft: boolean;
	totalEffort: number | null;
	notes: string | null;
	createdAt: string | null;
}

export interface ApiEstimationSummary {
	id: string | null;
	offer: string | null;
	description: string | null;
	latestVersionNumber: number | null;
	versionCount: number;
	hasDraft: boolean;
	createdAt: string | null;
}

export interface ApiEstimationDetail {
	id: string | null;
	offer: string | null;
	description: string | null;
	projectId: string | null;
	projectName: string | null;
	latestVersionNumber: number | null;
	hasDraft: boolean;
	createdAt: string | null;
	versions: ApiVersionSummary[];
}

export interface ApiProjectSummary {
	id: string | null;
	name: string | null;
	description: string | null;
	client: string | null;
	status: string;
	createdAt: string | null;
}

export interface ApiComparisonNode {
	logicalId: string;
	type: string;
	title: string | null;
	description: string | null;
	path: string[];
	minEffort: number | null;
	expectedEffort: number | null;
	maxEffort: number | null;
	offerPT: number | null;
}

export interface ApiNodeModification {
	logicalId: string;
	type: string;
	before: ApiComparisonNode;
	after: ApiComparisonNode;
	changedFields: string[];
}

export interface ApiParameterChange {
	name: string;
	oldValue: number | null;
	newValue: number | null;
	changeType: string;
}

export interface ApiVersionComparison {
	versionA: number;
	versionB: number;
	addedNodes: ApiComparisonNode[];
	removedNodes: ApiComparisonNode[];
	modifiedNodes: ApiNodeModification[];
	parameterChanges: ApiParameterChange[];
}

export interface ApiProjectDetail {
	id: string | null;
	name: string | null;
	description: string | null;
	client: string | null;
	status: string;
	createdAt: string | null;
	estimations: ApiEstimationSummary[];
}
