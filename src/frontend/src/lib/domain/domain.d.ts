type Nullable<T> = T | null | undefined
export declare interface KtList<E> /* extends Collection<E> */ {
    asJsReadonlyArrayView(): ReadonlyArray<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtList": unique symbol;
    };
}
export declare const KtList: {
    getInstance(): {
        fromJsArray<E>(array: ReadonlyArray<E>): KtList<E>;
    };
};
export declare class AdditionalCost extends BaseDomain {
    constructor(description: string, amount?: number, type?: AdditionalCostType, amountPerWeek?: number, phase?: Nullable<ProjectPhase>, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get description(): string;
    get amount(): number;
    get type(): AdditionalCostType;
    get amountPerWeek(): number;
    get phase(): Nullable<ProjectPhase>;
    copy(description?: string, amount?: number, type?: AdditionalCostType, amountPerWeek?: number, phase?: Nullable<ProjectPhase>, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): AdditionalCost;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare abstract class AdditionalCostType {
    private constructor();
    static get ONE_TIME(): AdditionalCostType & {
        get name(): "ONE_TIME";
        get ordinal(): 0;
    };
    static get RECURRING(): AdditionalCostType & {
        get name(): "RECURRING";
        get ordinal(): 1;
    };
    get name(): "ONE_TIME" | "RECURRING";
    get ordinal(): 0 | 1;
    static values(): Array<AdditionalCostType>;
    static valueOf(value: string): AdditionalCostType;
}
export declare abstract class BaseDomain {
    constructor(id?: Nullable<string>, createdAt?: Nullable<string>, updatedAt?: Nullable<string>);
    get id(): Nullable<string>;
    get createdAt(): Nullable<string>;
    get updatedAt(): Nullable<string>;
}
export declare class CalculationParameters {
    constructor(riskFactor?: number, totalDriverFactor?: number, dailyRate?: number, salesSurcharge?: number);
    get riskFactor(): number;
    get totalDriverFactor(): number;
    get dailyRate(): number;
    get salesSurcharge(): number;
    copy(riskFactor?: number, totalDriverFactor?: number, dailyRate?: number, salesSurcharge?: number): CalculationParameters;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare function createFixedItem(description: string, minEffort?: number, expectedEffort?: number, maxEffort?: number, assumptions?: string, logicalId?: string): FixedEstimationItem;
export declare function createTimeRelativeItem(description: string, unit?: string, minEffort?: number, expectedEffort?: number, maxEffort?: number, assumptions?: string, logicalId?: string, phase?: Nullable<ProjectPhase>): TimeRelativeEstimationItem;
export declare function createGroup(title: string, logicalId?: string, children?: Array<EstimationNode>): EstimationGroup;
export declare function createVersion(versionNumber: number, isDraft: boolean, notes?: string, parameters?: Array<EstimationParameter>, effortDrivers?: Array<EffortDriver>, phases?: Array<ProjectPhase>, roots?: Array<EstimationNode>): EstimationVersion;
export declare class EffortDriver extends BaseDomain {
    constructor(description: string, factor?: number, comment?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get description(): string;
    get factor(): number;
    get comment(): string;
    copy(description?: string, factor?: number, comment?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): EffortDriver;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare class Estimation extends BaseDomain {
    constructor(offer?: string, description?: string, currentVersion?: Nullable<EstimationVersion>, versions?: KtList<EstimationVersion>, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get offer(): string;
    get description(): string;
    get currentVersion(): Nullable<EstimationVersion>;
    get versions(): KtList<EstimationVersion>;
    copy(offer?: string, description?: string, currentVersion?: Nullable<EstimationVersion>, versions?: KtList<EstimationVersion>, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): Estimation;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare class EstimationGroup extends EstimationNode {
    constructor(title: string, children?: KtList<EstimationNode>, _logicalId?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get title(): string;
    get children(): KtList<EstimationNode>;
    get mean(): number;
    get variance(): number;
    get riskSurcharge(): number;
    get driverSurcharge(): number;
    get offerPT(): number;
    get cost(): number;
    get offerPrice(): number;
    withCalculationParameters(params: CalculationParameters): EstimationGroup;
    copy(title?: string, children?: KtList<EstimationNode>, _logicalId?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): EstimationGroup;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare abstract class EstimationItem extends EstimationNode {
    protected constructor(description: string, code?: string, minEffort?: number, expectedEffort?: number, maxEffort?: number, assumptions?: string, phase?: Nullable<ProjectPhase>, logicalId?: string, calculationParameters?: CalculationParameters, id?: Nullable<string>, createdAt?: Nullable<string>, updatedAt?: Nullable<string>);
    get description(): string;
    get code(): string;
    get minEffort(): number;
    get expectedEffort(): number;
    get maxEffort(): number;
    get assumptions(): string;
    get phase(): Nullable<ProjectPhase>;
    get calculationParameters(): CalculationParameters;
    get mean(): number;
    get variance(): number;
    get riskSurcharge(): number;
    get driverSurcharge(): number;
    get offerPT(): number;
    get cost(): number;
    get offerPrice(): number;
    abstract withCalculationParameters(params: CalculationParameters): EstimationItem;
}
export declare abstract class EstimationNode extends BaseDomain {
    protected constructor(logicalId: string, id?: Nullable<string>, createdAt?: Nullable<string>, updatedAt?: Nullable<string>);
    get logicalId(): string;
    abstract get mean(): number;
    abstract get variance(): number;
    abstract get riskSurcharge(): number;
    abstract get driverSurcharge(): number;
    abstract get offerPT(): number;
    abstract get cost(): number;
    abstract get offerPrice(): number;
    abstract withCalculationParameters(params: CalculationParameters): EstimationNode;
}
export declare class EstimationParameter extends BaseDomain {
    constructor(name: string, value?: number, comment?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get name(): string;
    get value(): number;
    get comment(): string;
    copy(name?: string, value?: number, comment?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): EstimationParameter;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare class EstimationVersion extends BaseDomain {
    constructor(versionNumber: number, status?: EstimationVersionStatus, createdBy?: Nullable<User>, totalEffort?: number, notes?: string, parameters?: KtList<EstimationParameter>, effortDrivers?: KtList<EffortDriver>, phases?: KtList<ProjectPhase>, additionalCosts?: KtList<AdditionalCost>, roots?: KtList<EstimationNode>, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get versionNumber(): number;
    get status(): EstimationVersionStatus;
    get createdBy(): Nullable<User>;
    get totalEffort(): number;
    get notes(): string;
    get parameters(): KtList<EstimationParameter>;
    get effortDrivers(): KtList<EffortDriver>;
    get phases(): KtList<ProjectPhase>;
    get additionalCosts(): KtList<AdditionalCost>;
    get roots(): KtList<EstimationNode>;
    parameterValue(name: string): Nullable<number>;
    calculate(): EstimationVersion;
    copy(versionNumber?: number, status?: EstimationVersionStatus, createdBy?: Nullable<User>, totalEffort?: number, notes?: string, parameters?: KtList<EstimationParameter>, effortDrivers?: KtList<EffortDriver>, phases?: KtList<ProjectPhase>, additionalCosts?: KtList<AdditionalCost>, roots?: KtList<EstimationNode>, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): EstimationVersion;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare abstract class EstimationVersionStatus {
    private constructor();
    static get DRAFT(): EstimationVersionStatus & {
        get name(): "DRAFT";
        get ordinal(): 0;
    };
    static get SUBMITTED(): EstimationVersionStatus & {
        get name(): "SUBMITTED";
        get ordinal(): 1;
    };
    get name(): "DRAFT" | "SUBMITTED";
    get ordinal(): 0 | 1;
    static values(): Array<EstimationVersionStatus>;
    static valueOf(value: string): EstimationVersionStatus;
}
export declare class FixedEstimationItem extends EstimationItem {
    constructor(_description: string, _code?: string, _minEffort?: number, _expectedEffort?: number, _maxEffort?: number, _assumptions?: string, _phase?: Nullable<ProjectPhase>, _logicalId?: string, _calculationParameters?: CalculationParameters, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    withCalculationParameters(params: CalculationParameters): FixedEstimationItem;
    copy(_description?: string, _code?: string, _minEffort?: number, _expectedEffort?: number, _maxEffort?: number, _assumptions?: string, _phase?: Nullable<ProjectPhase>, _logicalId?: string, _calculationParameters?: CalculationParameters, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): FixedEstimationItem;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare const PertCalculation: {
    getInstance(): {
        mean(min: number, expected: number, max: number): number;
        variance(min: number, max: number): number;
        riskFactor(totalMean: number, totalVariance: number, stdDevFactor: number): number;
    };
};
export declare class Project extends BaseDomain {
    constructor(name: string, description?: string, client?: string, status?: ProjectStatus, owner?: Nullable<User>, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get name(): string;
    get description(): string;
    get client(): string;
    get status(): ProjectStatus;
    get owner(): Nullable<User>;
    copy(name?: string, description?: string, client?: string, status?: ProjectStatus, owner?: Nullable<User>, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): Project;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare class ProjectPhase extends BaseDomain {
    constructor(name: string, abbreviation: string, durationWeeks?: number, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get name(): string;
    get abbreviation(): string;
    get durationWeeks(): number;
    copy(name?: string, abbreviation?: string, durationWeeks?: number, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): ProjectPhase;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare abstract class ProjectStatus {
    private constructor();
    static get ACTIVE(): ProjectStatus & {
        get name(): "ACTIVE";
        get ordinal(): 0;
    };
    static get ARCHIVED(): ProjectStatus & {
        get name(): "ARCHIVED";
        get ordinal(): 1;
    };
    get name(): "ACTIVE" | "ARCHIVED";
    get ordinal(): 0 | 1;
    static values(): Array<ProjectStatus>;
    static valueOf(value: string): ProjectStatus;
}
export declare class TimeRelativeEstimationItem extends EstimationItem {
    constructor(unit: string | undefined, _description: string, _code?: string, _minEffort?: number, _expectedEffort?: number, _maxEffort?: number, _assumptions?: string, _phase?: Nullable<ProjectPhase>, _logicalId?: string, _calculationParameters?: CalculationParameters, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get unit(): string;
    get mean(): number;
    get variance(): number;
    withCalculationParameters(params: CalculationParameters): TimeRelativeEstimationItem;
    copy(unit?: string, _description?: string, _code?: string, _minEffort?: number, _expectedEffort?: number, _maxEffort?: number, _assumptions?: string, _phase?: Nullable<ProjectPhase>, _logicalId?: string, _calculationParameters?: CalculationParameters, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): TimeRelativeEstimationItem;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare class User extends BaseDomain {
    constructor(entraSubjectId?: Nullable<string>, displayName?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get entraSubjectId(): Nullable<string>;
    get displayName(): string;
    copy(entraSubjectId?: Nullable<string>, displayName?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): User;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare class EstimationCalculator {
    constructor();
    calculate(version: EstimationVersion): EstimationVersion;
    validateInvariants(version: EstimationVersion): Array<InvariantResult>;
}
export declare class InvariantResult {
    constructor(description: string, difference: number, tolerance: number);
    get description(): string;
    get difference(): number;
    get tolerance(): number;
    get passed(): boolean;
    copy(description?: string, difference?: number, tolerance?: number): InvariantResult;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}