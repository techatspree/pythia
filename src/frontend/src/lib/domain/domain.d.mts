type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare interface KtList<E> /* extends Collection<E> */ {
    asJsReadonlyArrayView(): ReadonlyArray<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtList": unique symbol;
    };
}
export declare namespace KtList {
    function fromJsArray<E>(array: ReadonlyArray<E>): KtList<E>;
}
export declare class AdditionalCost extends BaseDomain.$metadata$.constructor {
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
export declare namespace AdditionalCost {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => AdditionalCost;
    }
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
    static values(): [typeof AdditionalCostType.ONE_TIME, typeof AdditionalCostType.RECURRING];
    static valueOf(value: string): AdditionalCostType;
    get name(): "ONE_TIME" | "RECURRING";
    get ordinal(): 0 | 1;
}
export declare namespace AdditionalCostType {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => AdditionalCostType;
    }
}
export declare abstract class BaseDomain {
    constructor(id?: Nullable<string>, createdAt?: Nullable<string>, updatedAt?: Nullable<string>);
    get id(): Nullable<string>;
    get createdAt(): Nullable<string>;
    get updatedAt(): Nullable<string>;
}
export declare namespace BaseDomain {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => BaseDomain;
    }
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
export declare namespace CalculationParameters {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => CalculationParameters;
    }
}
export declare function createFixedItem(description: string, minEffort?: number, expectedEffort?: number, maxEffort?: number, assumptions?: string, logicalId?: string): FixedEstimationItem;
export declare function createTimeRelativeItem(description: string, unit?: string, minEffort?: number, expectedEffort?: number, maxEffort?: number, assumptions?: string, logicalId?: string, phase?: Nullable<ProjectPhase>): TimeRelativeEstimationItem;
export declare function createGroup(title: string, logicalId?: string, children?: Array<EstimationNode>): EstimationGroup;
export declare function createVersion(versionNumber: number, isDraft: boolean, notes?: string, parameters?: Array<EstimationParameter>, effortDrivers?: Array<EffortDriver>, phases?: Array<ProjectPhase>, roots?: Array<EstimationNode>): EstimationVersion;
export declare class EffortDriver extends BaseDomain.$metadata$.constructor {
    constructor(description: string, factor?: number, comment?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get description(): string;
    get factor(): number;
    get comment(): string;
    copy(description?: string, factor?: number, comment?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): EffortDriver;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace EffortDriver {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => EffortDriver;
    }
}
export declare class Estimation extends BaseDomain.$metadata$.constructor {
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
export declare namespace Estimation {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => Estimation;
    }
}
export declare class EstimationGroup extends EstimationNode.$metadata$.constructor {
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
export declare namespace EstimationGroup {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => EstimationGroup;
    }
}
export declare abstract class EstimationItem extends EstimationNode.$metadata$.constructor {
    private constructor();
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
export declare namespace EstimationItem {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => EstimationItem;
    }
}
export declare abstract class EstimationNode extends BaseDomain.$metadata$.constructor {
    private constructor();
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
export declare namespace EstimationNode {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => EstimationNode;
    }
}
export declare class EstimationParameter extends BaseDomain.$metadata$.constructor {
    constructor(name: string, value?: number, comment?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get name(): string;
    get value(): number;
    get comment(): string;
    copy(name?: string, value?: number, comment?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): EstimationParameter;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace EstimationParameter {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => EstimationParameter;
    }
}
export declare class EstimationVersion extends BaseDomain.$metadata$.constructor {
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
export declare namespace EstimationVersion {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => EstimationVersion;
    }
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
    static values(): [typeof EstimationVersionStatus.DRAFT, typeof EstimationVersionStatus.SUBMITTED];
    static valueOf(value: string): EstimationVersionStatus;
    get name(): "DRAFT" | "SUBMITTED";
    get ordinal(): 0 | 1;
}
export declare namespace EstimationVersionStatus {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => EstimationVersionStatus;
    }
}
export declare class FixedEstimationItem extends EstimationItem.$metadata$.constructor {
    constructor(_description: string, _code?: string, _minEffort?: number, _expectedEffort?: number, _maxEffort?: number, _assumptions?: string, _phase?: Nullable<ProjectPhase>, _logicalId?: string, _calculationParameters?: CalculationParameters, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    withCalculationParameters(params: CalculationParameters): FixedEstimationItem;
    copy(_description?: string, _code?: string, _minEffort?: number, _expectedEffort?: number, _maxEffort?: number, _assumptions?: string, _phase?: Nullable<ProjectPhase>, _logicalId?: string, _calculationParameters?: CalculationParameters, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): FixedEstimationItem;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace FixedEstimationItem {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => FixedEstimationItem;
    }
}
export declare abstract class PertCalculation {
    static readonly getInstance: () => typeof PertCalculation.$metadata$.type;
    private constructor();
}
export declare namespace PertCalculation {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        abstract class type extends KtSingleton<constructor>() {
            private constructor();
        }
        abstract class constructor {
            mean(min: number, expected: number, max: number): number;
            variance(min: number, max: number): number;
            riskFactor(totalMean: number, totalVariance: number, stdDevFactor: number): number;
            private constructor();
        }
    }
}
export declare class Project extends BaseDomain.$metadata$.constructor {
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
export declare namespace Project {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => Project;
    }
}
export declare class ProjectPhase extends BaseDomain.$metadata$.constructor {
    constructor(name: string, abbreviation: string, durationWeeks?: number, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get name(): string;
    get abbreviation(): string;
    get durationWeeks(): number;
    copy(name?: string, abbreviation?: string, durationWeeks?: number, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): ProjectPhase;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace ProjectPhase {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => ProjectPhase;
    }
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
    static values(): [typeof ProjectStatus.ACTIVE, typeof ProjectStatus.ARCHIVED];
    static valueOf(value: string): ProjectStatus;
    get name(): "ACTIVE" | "ARCHIVED";
    get ordinal(): 0 | 1;
}
export declare namespace ProjectStatus {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => ProjectStatus;
    }
}
export declare class TimeRelativeEstimationItem extends EstimationItem.$metadata$.constructor {
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
export declare namespace TimeRelativeEstimationItem {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => TimeRelativeEstimationItem;
    }
}
export declare class User extends BaseDomain.$metadata$.constructor {
    constructor(entraSubjectId?: Nullable<string>, displayName?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>);
    get entraSubjectId(): Nullable<string>;
    get displayName(): string;
    copy(entraSubjectId?: Nullable<string>, displayName?: string, _id?: Nullable<string>, _createdAt?: Nullable<string>, _updatedAt?: Nullable<string>): User;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace User {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => User;
    }
}
export declare abstract class DraftMutation {
    private constructor();
    abstract get kind(): string;
    abstract apply(current: EstimationVersion): EstimationVersion;
    abstract inverse(): DraftMutation;
}
export declare namespace DraftMutation {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => DraftMutation;
    }
}
export declare class ReplaceWholeDraft extends DraftMutation.$metadata$.constructor {
    constructor(before: EstimationVersion, after: EstimationVersion);
    get before(): EstimationVersion;
    get after(): EstimationVersion;
    get kind(): string;
    apply(current: EstimationVersion): EstimationVersion;
    inverse(): DraftMutation;
    copy(before?: EstimationVersion, after?: EstimationVersion): ReplaceWholeDraft;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace ReplaceWholeDraft {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => ReplaceWholeDraft;
    }
}
export declare class EstimationCalculator {
    constructor();
    calculate(version: EstimationVersion): EstimationVersion;
    validateInvariants(version: EstimationVersion): Array<InvariantResult>;
}
export declare namespace EstimationCalculator {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => EstimationCalculator;
    }
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
export declare namespace InvariantResult {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => InvariantResult;
    }
}