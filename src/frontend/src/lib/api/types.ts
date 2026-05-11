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

export interface ApiItem {
	logicalId: string | null;
	type: string;
	description: string;
	code: string | null;
	minEffort: number;
	expectedEffort: number;
	maxEffort: number;
	assumptions: string | null;
	mean: number;
	variance: number;
	riskSurcharge: number;
	driverSurcharge: number;
	offerPT: number;
	cost: number;
	offerPrice: number;
	unit: string | null;
}

export interface ApiItemGroup {
	logicalId: string | null;
	title: string;
	phaseAbbreviation: string | null;
	items: ApiItem[];
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
	itemGroups: ApiItemGroup[];
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

export interface ApiProjectDetail {
	id: string | null;
	name: string | null;
	description: string | null;
	client: string | null;
	status: string;
	createdAt: string | null;
	estimations: ApiEstimationSummary[];
}
