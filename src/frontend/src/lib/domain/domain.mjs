import {
  VOID7hggqo3abtya as VOID,
  protoOf180f3jzyo7rfj as protoOf,
  toString30pk9tzaqopn as toString,
  getStringHashCode26igk1bx568vk as getStringHashCode,
  getNumberHashCode2l4nbdcihl25f as getNumberHashCode,
  THROW_CCE2g6jy02ryeudk as THROW_CCE,
  equals2au1ep9vhcato as equals,
  defineProp3hxgpk2knu2px as defineProp,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  THROW_IAE23kobfj9wdoxr as THROW_IAE,
  Unit_instance104q5opgivhr8 as Unit_instance,
  Enum3alwj03lh1n41 as Enum,
  toList383f556t1dixk as toList,
  Companion_getInstance3tnw2k4njrdpv as Companion_getInstance,
  emptyList1g2z5xcrvp2zy as emptyList,
  toString1pkumu07cwy4m as toString_0,
  hashCodeq5arwsb9dgti as hashCode,
  collectionSizeOrDefault36dulx8yinfqm as collectionSizeOrDefault,
  ArrayList_init_$Create$1s1wkrw82c0iw as ArrayList_init_$Create$,
  noWhenBranchMatchedException2a6r7ubxgky5j as noWhenBranchMatchedException,
  asSequence2phdjljfh9jhx as asSequence,
  flatMapgxtanzi5fvh9 as flatMap,
  sequenceOf1mtha40gp6gat as sequenceOf,
  ArrayList_init_$Create$2qnngtk1et9r9 as ArrayList_init_$Create$_0,
  toListx6x8nvfmvvht as toList_0,
  addAll1k27qatfgp3k5 as addAll,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  copyToArray2j022khrow2yi as copyToArray,
} from './kotlin-kotlin-stdlib.mjs';
//region block: imports
var imul = Math.imul;
//endregion
//region block: pre-declaration
initMetadataForClass(BaseDomain, 'BaseDomain');
initMetadataForClass(AdditionalCost, 'AdditionalCost', VOID, BaseDomain);
initMetadataForClass(AdditionalCostType, 'AdditionalCostType', VOID, Enum);
initMetadataForClass(CalculationParameters, 'CalculationParameters', CalculationParameters);
initMetadataForClass(EffortDriver, 'EffortDriver', VOID, BaseDomain);
initMetadataForClass(Estimation, 'Estimation', Estimation, BaseDomain);
initMetadataForClass(EstimationNode, 'EstimationNode', VOID, BaseDomain);
initMetadataForClass(EstimationGroup, 'EstimationGroup', VOID, EstimationNode);
initMetadataForClass(EstimationItem, 'EstimationItem', VOID, EstimationNode);
initMetadataForClass(EstimationParameter, 'EstimationParameter', VOID, BaseDomain);
initMetadataForClass(EstimationVersion, 'EstimationVersion', VOID, BaseDomain);
initMetadataForClass(EstimationVersionStatus, 'EstimationVersionStatus', VOID, Enum);
initMetadataForClass(FixedEstimationItem, 'FixedEstimationItem', VOID, EstimationItem);
initMetadataForObject(PertCalculation_0, 'PertCalculation');
initMetadataForClass(Project, 'Project', VOID, BaseDomain);
initMetadataForClass(ProjectPhase, 'ProjectPhase', VOID, BaseDomain);
initMetadataForClass(ProjectStatus, 'ProjectStatus', VOID, Enum);
initMetadataForClass(TimeRelativeEstimationItem, 'TimeRelativeEstimationItem', VOID, EstimationItem);
initMetadataForClass(User, 'User', User, BaseDomain);
initMetadataForClass(EstimationCalculator, 'EstimationCalculator', EstimationCalculator);
initMetadataForClass(InvariantResult, 'InvariantResult');
//endregion
function AdditionalCost(description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt) {
  amount = amount === VOID ? 0.0 : amount;
  type = type === VOID ? AdditionalCostType_ONE_TIME_getInstance() : type;
  amountPerWeek = amountPerWeek === VOID ? 0.0 : amountPerWeek;
  phase = phase === VOID ? null : phase;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.c4_1 = description;
  this.d4_1 = amount;
  this.e4_1 = type;
  this.f4_1 = amountPerWeek;
  this.g4_1 = phase;
  this.h4_1 = _id;
  this.i4_1 = _createdAt;
  this.j4_1 = _updatedAt;
}
protoOf(AdditionalCost).k4 = function () {
  return this.c4_1;
};
protoOf(AdditionalCost).l4 = function () {
  return this.d4_1;
};
protoOf(AdditionalCost).m4 = function () {
  return this.e4_1;
};
protoOf(AdditionalCost).n4 = function () {
  return this.f4_1;
};
protoOf(AdditionalCost).o4 = function () {
  return this.g4_1;
};
protoOf(AdditionalCost).p4 = function () {
  return this.description;
};
protoOf(AdditionalCost).q4 = function () {
  return this.amount;
};
protoOf(AdditionalCost).r4 = function () {
  return this.type;
};
protoOf(AdditionalCost).s4 = function () {
  return this.amountPerWeek;
};
protoOf(AdditionalCost).t4 = function () {
  return this.phase;
};
protoOf(AdditionalCost).u4 = function (description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt) {
  return new AdditionalCost(description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt);
};
protoOf(AdditionalCost).copy = function (description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt, $super) {
  description = description === VOID ? this.description : description;
  amount = amount === VOID ? this.amount : amount;
  type = type === VOID ? this.type : type;
  amountPerWeek = amountPerWeek === VOID ? this.amountPerWeek : amountPerWeek;
  phase = phase === VOID ? this.phase : phase;
  _id = _id === VOID ? this.h4_1 : _id;
  _createdAt = _createdAt === VOID ? this.i4_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.j4_1 : _updatedAt;
  return $super === VOID ? this.u4(description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt) : $super.u4.call(this, description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt);
};
protoOf(AdditionalCost).toString = function () {
  return 'AdditionalCost(description=' + this.description + ', amount=' + this.amount + ', type=' + this.type.toString() + ', amountPerWeek=' + this.amountPerWeek + ', phase=' + toString(this.phase) + ', _id=' + this.h4_1 + ', _createdAt=' + this.i4_1 + ', _updatedAt=' + this.j4_1 + ')';
};
protoOf(AdditionalCost).hashCode = function () {
  var result = getStringHashCode(this.description);
  result = imul(result, 31) + getNumberHashCode(this.amount) | 0;
  result = imul(result, 31) + this.type.hashCode() | 0;
  result = imul(result, 31) + getNumberHashCode(this.amountPerWeek) | 0;
  result = imul(result, 31) + (this.phase == null ? 0 : this.phase.hashCode()) | 0;
  result = imul(result, 31) + (this.h4_1 == null ? 0 : getStringHashCode(this.h4_1)) | 0;
  result = imul(result, 31) + (this.i4_1 == null ? 0 : getStringHashCode(this.i4_1)) | 0;
  result = imul(result, 31) + (this.j4_1 == null ? 0 : getStringHashCode(this.j4_1)) | 0;
  return result;
};
protoOf(AdditionalCost).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof AdditionalCost))
    return false;
  var tmp0_other_with_cast = other instanceof AdditionalCost ? other : THROW_CCE();
  if (!(this.description === tmp0_other_with_cast.description))
    return false;
  if (!equals(this.amount, tmp0_other_with_cast.amount))
    return false;
  if (!this.type.equals(tmp0_other_with_cast.type))
    return false;
  if (!equals(this.amountPerWeek, tmp0_other_with_cast.amountPerWeek))
    return false;
  if (!equals(this.phase, tmp0_other_with_cast.phase))
    return false;
  if (!(this.h4_1 == tmp0_other_with_cast.h4_1))
    return false;
  if (!(this.i4_1 == tmp0_other_with_cast.i4_1))
    return false;
  if (!(this.j4_1 == tmp0_other_with_cast.j4_1))
    return false;
  return true;
};
var AdditionalCostType_ONE_TIME_instance;
var AdditionalCostType_RECURRING_instance;
function values() {
  return [AdditionalCostType_ONE_TIME_getInstance(), AdditionalCostType_RECURRING_getInstance()];
}
function valueOf(value) {
  switch (value) {
    case 'ONE_TIME':
      return AdditionalCostType_ONE_TIME_getInstance();
    case 'RECURRING':
      return AdditionalCostType_RECURRING_getInstance();
    default:
      AdditionalCostType_initEntries();
      THROW_IAE('No enum constant value.');
      break;
  }
}
var AdditionalCostType_entriesInitialized;
function AdditionalCostType_initEntries() {
  if (AdditionalCostType_entriesInitialized)
    return Unit_instance;
  AdditionalCostType_entriesInitialized = true;
  AdditionalCostType_ONE_TIME_instance = new AdditionalCostType('ONE_TIME', 0);
  AdditionalCostType_RECURRING_instance = new AdditionalCostType('RECURRING', 1);
}
function AdditionalCostType(name, ordinal) {
  Enum.call(this, name, ordinal);
}
function AdditionalCostType_ONE_TIME_getInstance() {
  AdditionalCostType_initEntries();
  return AdditionalCostType_ONE_TIME_instance;
}
function AdditionalCostType_RECURRING_getInstance() {
  AdditionalCostType_initEntries();
  return AdditionalCostType_RECURRING_instance;
}
function BaseDomain(id, createdAt, updatedAt) {
  id = id === VOID ? null : id;
  createdAt = createdAt === VOID ? null : createdAt;
  updatedAt = updatedAt === VOID ? null : updatedAt;
  this.v4_1 = id;
  this.w4_1 = createdAt;
  this.x4_1 = updatedAt;
}
protoOf(BaseDomain).y4 = function () {
  return this.v4_1;
};
protoOf(BaseDomain).z4 = function () {
  return this.w4_1;
};
protoOf(BaseDomain).a5 = function () {
  return this.x4_1;
};
function CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge) {
  riskFactor = riskFactor === VOID ? 0.0 : riskFactor;
  totalDriverFactor = totalDriverFactor === VOID ? 0.0 : totalDriverFactor;
  dailyRate = dailyRate === VOID ? 0.0 : dailyRate;
  salesSurcharge = salesSurcharge === VOID ? 0.0 : salesSurcharge;
  this.riskFactor = riskFactor;
  this.totalDriverFactor = totalDriverFactor;
  this.dailyRate = dailyRate;
  this.salesSurcharge = salesSurcharge;
}
protoOf(CalculationParameters).d5 = function () {
  return this.riskFactor;
};
protoOf(CalculationParameters).e5 = function () {
  return this.totalDriverFactor;
};
protoOf(CalculationParameters).f5 = function () {
  return this.dailyRate;
};
protoOf(CalculationParameters).g5 = function () {
  return this.salesSurcharge;
};
protoOf(CalculationParameters).p4 = function () {
  return this.riskFactor;
};
protoOf(CalculationParameters).q4 = function () {
  return this.totalDriverFactor;
};
protoOf(CalculationParameters).r4 = function () {
  return this.dailyRate;
};
protoOf(CalculationParameters).s4 = function () {
  return this.salesSurcharge;
};
protoOf(CalculationParameters).h5 = function (riskFactor, totalDriverFactor, dailyRate, salesSurcharge) {
  return new CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
};
protoOf(CalculationParameters).copy = function (riskFactor, totalDriverFactor, dailyRate, salesSurcharge, $super) {
  riskFactor = riskFactor === VOID ? this.riskFactor : riskFactor;
  totalDriverFactor = totalDriverFactor === VOID ? this.totalDriverFactor : totalDriverFactor;
  dailyRate = dailyRate === VOID ? this.dailyRate : dailyRate;
  salesSurcharge = salesSurcharge === VOID ? this.salesSurcharge : salesSurcharge;
  return $super === VOID ? this.h5(riskFactor, totalDriverFactor, dailyRate, salesSurcharge) : $super.h5.call(this, riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
};
protoOf(CalculationParameters).toString = function () {
  return 'CalculationParameters(riskFactor=' + this.riskFactor + ', totalDriverFactor=' + this.totalDriverFactor + ', dailyRate=' + this.dailyRate + ', salesSurcharge=' + this.salesSurcharge + ')';
};
protoOf(CalculationParameters).hashCode = function () {
  var result = getNumberHashCode(this.riskFactor);
  result = imul(result, 31) + getNumberHashCode(this.totalDriverFactor) | 0;
  result = imul(result, 31) + getNumberHashCode(this.dailyRate) | 0;
  result = imul(result, 31) + getNumberHashCode(this.salesSurcharge) | 0;
  return result;
};
protoOf(CalculationParameters).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof CalculationParameters))
    return false;
  var tmp0_other_with_cast = other instanceof CalculationParameters ? other : THROW_CCE();
  if (!equals(this.riskFactor, tmp0_other_with_cast.riskFactor))
    return false;
  if (!equals(this.totalDriverFactor, tmp0_other_with_cast.totalDriverFactor))
    return false;
  if (!equals(this.dailyRate, tmp0_other_with_cast.dailyRate))
    return false;
  if (!equals(this.salesSurcharge, tmp0_other_with_cast.salesSurcharge))
    return false;
  return true;
};
function createFixedItem(description, minEffort, expectedEffort, maxEffort, assumptions, logicalId) {
  minEffort = minEffort === VOID ? 0.0 : minEffort;
  expectedEffort = expectedEffort === VOID ? 0.0 : expectedEffort;
  maxEffort = maxEffort === VOID ? 0.0 : maxEffort;
  assumptions = assumptions === VOID ? '' : assumptions;
  logicalId = logicalId === VOID ? newId() : logicalId;
  return new FixedEstimationItem(description, VOID, minEffort, expectedEffort, maxEffort, assumptions, VOID, logicalId);
}
function createTimeRelativeItem(description, unit, minEffort, expectedEffort, maxEffort, assumptions, logicalId, phase) {
  unit = unit === VOID ? 'h/Woche' : unit;
  minEffort = minEffort === VOID ? 0.0 : minEffort;
  expectedEffort = expectedEffort === VOID ? 0.0 : expectedEffort;
  maxEffort = maxEffort === VOID ? 0.0 : maxEffort;
  assumptions = assumptions === VOID ? '' : assumptions;
  logicalId = logicalId === VOID ? newId() : logicalId;
  phase = phase === VOID ? null : phase;
  return new TimeRelativeEstimationItem(unit, description, VOID, minEffort, expectedEffort, maxEffort, assumptions, phase, logicalId);
}
function createGroup(title, logicalId, children) {
  logicalId = logicalId === VOID ? newId() : logicalId;
  var tmp;
  if (children === VOID) {
    // Inline function 'kotlin.emptyArray' call
    tmp = [];
  } else {
    tmp = children;
  }
  children = tmp;
  return new EstimationGroup(title, toList(children), logicalId);
}
function createVersion(versionNumber, isDraft, notes, parameters, effortDrivers, phases, roots) {
  notes = notes === VOID ? '' : notes;
  var tmp;
  if (parameters === VOID) {
    // Inline function 'kotlin.emptyArray' call
    tmp = [];
  } else {
    tmp = parameters;
  }
  parameters = tmp;
  var tmp_0;
  if (effortDrivers === VOID) {
    // Inline function 'kotlin.emptyArray' call
    tmp_0 = [];
  } else {
    tmp_0 = effortDrivers;
  }
  effortDrivers = tmp_0;
  var tmp_1;
  if (phases === VOID) {
    // Inline function 'kotlin.emptyArray' call
    tmp_1 = [];
  } else {
    tmp_1 = phases;
  }
  phases = tmp_1;
  var tmp_2;
  if (roots === VOID) {
    // Inline function 'kotlin.emptyArray' call
    tmp_2 = [];
  } else {
    tmp_2 = roots;
  }
  roots = tmp_2;
  return new EstimationVersion(versionNumber, isDraft ? EstimationVersionStatus_DRAFT_getInstance() : EstimationVersionStatus_SUBMITTED_getInstance(), VOID, VOID, notes, toList(parameters), toList(effortDrivers), toList(phases), VOID, toList(roots));
}
function newId() {
  return Companion_getInstance().u3().toString();
}
function EffortDriver(description, factor, comment, _id, _createdAt, _updatedAt) {
  factor = factor === VOID ? 0.0 : factor;
  comment = comment === VOID ? '' : comment;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.l5_1 = description;
  this.m5_1 = factor;
  this.n5_1 = comment;
  this.o5_1 = _id;
  this.p5_1 = _createdAt;
  this.q5_1 = _updatedAt;
}
protoOf(EffortDriver).k4 = function () {
  return this.l5_1;
};
protoOf(EffortDriver).r5 = function () {
  return this.m5_1;
};
protoOf(EffortDriver).s5 = function () {
  return this.n5_1;
};
protoOf(EffortDriver).p4 = function () {
  return this.description;
};
protoOf(EffortDriver).q4 = function () {
  return this.factor;
};
protoOf(EffortDriver).r4 = function () {
  return this.comment;
};
protoOf(EffortDriver).t5 = function (description, factor, comment, _id, _createdAt, _updatedAt) {
  return new EffortDriver(description, factor, comment, _id, _createdAt, _updatedAt);
};
protoOf(EffortDriver).copy = function (description, factor, comment, _id, _createdAt, _updatedAt, $super) {
  description = description === VOID ? this.description : description;
  factor = factor === VOID ? this.factor : factor;
  comment = comment === VOID ? this.comment : comment;
  _id = _id === VOID ? this.o5_1 : _id;
  _createdAt = _createdAt === VOID ? this.p5_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.q5_1 : _updatedAt;
  return $super === VOID ? this.t5(description, factor, comment, _id, _createdAt, _updatedAt) : $super.t5.call(this, description, factor, comment, _id, _createdAt, _updatedAt);
};
protoOf(EffortDriver).toString = function () {
  return 'EffortDriver(description=' + this.description + ', factor=' + this.factor + ', comment=' + this.comment + ', _id=' + this.o5_1 + ', _createdAt=' + this.p5_1 + ', _updatedAt=' + this.q5_1 + ')';
};
protoOf(EffortDriver).hashCode = function () {
  var result = getStringHashCode(this.description);
  result = imul(result, 31) + getNumberHashCode(this.factor) | 0;
  result = imul(result, 31) + getStringHashCode(this.comment) | 0;
  result = imul(result, 31) + (this.o5_1 == null ? 0 : getStringHashCode(this.o5_1)) | 0;
  result = imul(result, 31) + (this.p5_1 == null ? 0 : getStringHashCode(this.p5_1)) | 0;
  result = imul(result, 31) + (this.q5_1 == null ? 0 : getStringHashCode(this.q5_1)) | 0;
  return result;
};
protoOf(EffortDriver).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof EffortDriver))
    return false;
  var tmp0_other_with_cast = other instanceof EffortDriver ? other : THROW_CCE();
  if (!(this.description === tmp0_other_with_cast.description))
    return false;
  if (!equals(this.factor, tmp0_other_with_cast.factor))
    return false;
  if (!(this.comment === tmp0_other_with_cast.comment))
    return false;
  if (!(this.o5_1 == tmp0_other_with_cast.o5_1))
    return false;
  if (!(this.p5_1 == tmp0_other_with_cast.p5_1))
    return false;
  if (!(this.q5_1 == tmp0_other_with_cast.q5_1))
    return false;
  return true;
};
function Estimation(offer, description, currentVersion, versions, _id, _createdAt, _updatedAt) {
  offer = offer === VOID ? '' : offer;
  description = description === VOID ? '' : description;
  currentVersion = currentVersion === VOID ? null : currentVersion;
  versions = versions === VOID ? emptyList() : versions;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.x5_1 = offer;
  this.y5_1 = description;
  this.z5_1 = currentVersion;
  this.a6_1 = versions;
  this.b6_1 = _id;
  this.c6_1 = _createdAt;
  this.d6_1 = _updatedAt;
}
protoOf(Estimation).e6 = function () {
  return this.x5_1;
};
protoOf(Estimation).k4 = function () {
  return this.y5_1;
};
protoOf(Estimation).f6 = function () {
  return this.z5_1;
};
protoOf(Estimation).g6 = function () {
  return this.a6_1;
};
protoOf(Estimation).p4 = function () {
  return this.offer;
};
protoOf(Estimation).q4 = function () {
  return this.description;
};
protoOf(Estimation).r4 = function () {
  return this.currentVersion;
};
protoOf(Estimation).s4 = function () {
  return this.versions;
};
protoOf(Estimation).h6 = function (offer, description, currentVersion, versions, _id, _createdAt, _updatedAt) {
  return new Estimation(offer, description, currentVersion, versions, _id, _createdAt, _updatedAt);
};
protoOf(Estimation).copy = function (offer, description, currentVersion, versions, _id, _createdAt, _updatedAt, $super) {
  offer = offer === VOID ? this.offer : offer;
  description = description === VOID ? this.description : description;
  currentVersion = currentVersion === VOID ? this.currentVersion : currentVersion;
  versions = versions === VOID ? this.versions : versions;
  _id = _id === VOID ? this.b6_1 : _id;
  _createdAt = _createdAt === VOID ? this.c6_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.d6_1 : _updatedAt;
  return $super === VOID ? this.h6(offer, description, currentVersion, versions, _id, _createdAt, _updatedAt) : $super.h6.call(this, offer, description, currentVersion, versions, _id, _createdAt, _updatedAt);
};
protoOf(Estimation).toString = function () {
  return 'Estimation(offer=' + this.offer + ', description=' + this.description + ', currentVersion=' + toString(this.currentVersion) + ', versions=' + toString_0(this.versions) + ', _id=' + this.b6_1 + ', _createdAt=' + this.c6_1 + ', _updatedAt=' + this.d6_1 + ')';
};
protoOf(Estimation).hashCode = function () {
  var result = getStringHashCode(this.offer);
  result = imul(result, 31) + getStringHashCode(this.description) | 0;
  result = imul(result, 31) + (this.currentVersion == null ? 0 : this.currentVersion.hashCode()) | 0;
  result = imul(result, 31) + hashCode(this.versions) | 0;
  result = imul(result, 31) + (this.b6_1 == null ? 0 : getStringHashCode(this.b6_1)) | 0;
  result = imul(result, 31) + (this.c6_1 == null ? 0 : getStringHashCode(this.c6_1)) | 0;
  result = imul(result, 31) + (this.d6_1 == null ? 0 : getStringHashCode(this.d6_1)) | 0;
  return result;
};
protoOf(Estimation).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Estimation))
    return false;
  var tmp0_other_with_cast = other instanceof Estimation ? other : THROW_CCE();
  if (!(this.offer === tmp0_other_with_cast.offer))
    return false;
  if (!(this.description === tmp0_other_with_cast.description))
    return false;
  if (!equals(this.currentVersion, tmp0_other_with_cast.currentVersion))
    return false;
  if (!equals(this.versions, tmp0_other_with_cast.versions))
    return false;
  if (!(this.b6_1 == tmp0_other_with_cast.b6_1))
    return false;
  if (!(this.c6_1 == tmp0_other_with_cast.c6_1))
    return false;
  if (!(this.d6_1 == tmp0_other_with_cast.d6_1))
    return false;
  return true;
};
function EstimationGroup(title, children, _logicalId, _id, _createdAt, _updatedAt) {
  children = children === VOID ? emptyList() : children;
  _logicalId = _logicalId === VOID ? newId() : _logicalId;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  EstimationNode.call(this, _logicalId, _id, _createdAt, _updatedAt);
  this.m6_1 = title;
  this.n6_1 = children;
  this.o6_1 = _logicalId;
  this.p6_1 = _id;
  this.q6_1 = _createdAt;
  this.r6_1 = _updatedAt;
}
protoOf(EstimationGroup).s6 = function () {
  return this.m6_1;
};
protoOf(EstimationGroup).t6 = function () {
  return this.n6_1;
};
protoOf(EstimationGroup).u6 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    var tmp = sum;
    sum = tmp + element.mean;
  }
  return sum;
};
protoOf(EstimationGroup).v6 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    var tmp = sum;
    sum = tmp + element.variance;
  }
  return sum;
};
protoOf(EstimationGroup).w6 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    var tmp = sum;
    sum = tmp + element.riskSurcharge;
  }
  return sum;
};
protoOf(EstimationGroup).x6 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    var tmp = sum;
    sum = tmp + element.driverSurcharge;
  }
  return sum;
};
protoOf(EstimationGroup).y6 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    var tmp = sum;
    sum = tmp + element.offerPT;
  }
  return sum;
};
protoOf(EstimationGroup).z6 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    var tmp = sum;
    sum = tmp + element.cost;
  }
  return sum;
};
protoOf(EstimationGroup).a7 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    var tmp = sum;
    sum = tmp + element.offerPrice;
  }
  return sum;
};
protoOf(EstimationGroup).withCalculationParameters = function (params) {
  // Inline function 'kotlin.collections.map' call
  var this_0 = this.children;
  // Inline function 'kotlin.collections.mapTo' call
  var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
  var _iterator__ex2g4s = this_0.e();
  while (_iterator__ex2g4s.f()) {
    var item = _iterator__ex2g4s.g();
    var tmp$ret$0 = item.withCalculationParameters(params);
    destination.l(tmp$ret$0);
  }
  return this.copy(VOID, destination);
};
protoOf(EstimationGroup).p4 = function () {
  return this.title;
};
protoOf(EstimationGroup).q4 = function () {
  return this.children;
};
protoOf(EstimationGroup).b7 = function (title, children, _logicalId, _id, _createdAt, _updatedAt) {
  return new EstimationGroup(title, children, _logicalId, _id, _createdAt, _updatedAt);
};
protoOf(EstimationGroup).copy = function (title, children, _logicalId, _id, _createdAt, _updatedAt, $super) {
  title = title === VOID ? this.title : title;
  children = children === VOID ? this.children : children;
  _logicalId = _logicalId === VOID ? this.o6_1 : _logicalId;
  _id = _id === VOID ? this.p6_1 : _id;
  _createdAt = _createdAt === VOID ? this.q6_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.r6_1 : _updatedAt;
  return $super === VOID ? this.b7(title, children, _logicalId, _id, _createdAt, _updatedAt) : $super.b7.call(this, title, children, _logicalId, _id, _createdAt, _updatedAt);
};
protoOf(EstimationGroup).toString = function () {
  return 'EstimationGroup(title=' + this.title + ', children=' + toString_0(this.children) + ', _logicalId=' + this.o6_1 + ', _id=' + this.p6_1 + ', _createdAt=' + this.q6_1 + ', _updatedAt=' + this.r6_1 + ')';
};
protoOf(EstimationGroup).hashCode = function () {
  var result = getStringHashCode(this.title);
  result = imul(result, 31) + hashCode(this.children) | 0;
  result = imul(result, 31) + getStringHashCode(this.o6_1) | 0;
  result = imul(result, 31) + (this.p6_1 == null ? 0 : getStringHashCode(this.p6_1)) | 0;
  result = imul(result, 31) + (this.q6_1 == null ? 0 : getStringHashCode(this.q6_1)) | 0;
  result = imul(result, 31) + (this.r6_1 == null ? 0 : getStringHashCode(this.r6_1)) | 0;
  return result;
};
protoOf(EstimationGroup).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof EstimationGroup))
    return false;
  var tmp0_other_with_cast = other instanceof EstimationGroup ? other : THROW_CCE();
  if (!(this.title === tmp0_other_with_cast.title))
    return false;
  if (!equals(this.children, tmp0_other_with_cast.children))
    return false;
  if (!(this.o6_1 === tmp0_other_with_cast.o6_1))
    return false;
  if (!(this.p6_1 == tmp0_other_with_cast.p6_1))
    return false;
  if (!(this.q6_1 == tmp0_other_with_cast.q6_1))
    return false;
  if (!(this.r6_1 == tmp0_other_with_cast.r6_1))
    return false;
  return true;
};
function EstimationItem(description, code, minEffort, expectedEffort, maxEffort, assumptions, phase, logicalId, calculationParameters, id, createdAt, updatedAt) {
  code = code === VOID ? '' : code;
  minEffort = minEffort === VOID ? 0.0 : minEffort;
  expectedEffort = expectedEffort === VOID ? 0.0 : expectedEffort;
  maxEffort = maxEffort === VOID ? 0.0 : maxEffort;
  assumptions = assumptions === VOID ? '' : assumptions;
  phase = phase === VOID ? null : phase;
  logicalId = logicalId === VOID ? newId() : logicalId;
  calculationParameters = calculationParameters === VOID ? new CalculationParameters() : calculationParameters;
  id = id === VOID ? null : id;
  createdAt = createdAt === VOID ? null : createdAt;
  updatedAt = updatedAt === VOID ? null : updatedAt;
  EstimationNode.call(this, logicalId, id, createdAt, updatedAt);
  this.l7_1 = description;
  this.m7_1 = code;
  this.n7_1 = minEffort;
  this.o7_1 = expectedEffort;
  this.p7_1 = maxEffort;
  this.q7_1 = assumptions;
  this.r7_1 = phase;
  this.s7_1 = calculationParameters;
}
protoOf(EstimationItem).k4 = function () {
  return this.l7_1;
};
protoOf(EstimationItem).t7 = function () {
  return this.m7_1;
};
protoOf(EstimationItem).u7 = function () {
  return this.n7_1;
};
protoOf(EstimationItem).v7 = function () {
  return this.o7_1;
};
protoOf(EstimationItem).w7 = function () {
  return this.p7_1;
};
protoOf(EstimationItem).x7 = function () {
  return this.q7_1;
};
protoOf(EstimationItem).o4 = function () {
  return this.r7_1;
};
protoOf(EstimationItem).y7 = function () {
  return this.s7_1;
};
protoOf(EstimationItem).u6 = function () {
  return PertCalculation_instance.mean(this.minEffort, this.expectedEffort, this.maxEffort);
};
protoOf(EstimationItem).v6 = function () {
  return PertCalculation_instance.variance(this.minEffort, this.maxEffort);
};
protoOf(EstimationItem).w6 = function () {
  return this.mean * this.calculationParameters.riskFactor;
};
protoOf(EstimationItem).x6 = function () {
  return this.mean * this.calculationParameters.totalDriverFactor;
};
protoOf(EstimationItem).y6 = function () {
  return this.mean + this.riskSurcharge + this.driverSurcharge;
};
protoOf(EstimationItem).z6 = function () {
  return this.offerPT * this.calculationParameters.dailyRate;
};
protoOf(EstimationItem).a7 = function () {
  return this.cost * (1 + this.calculationParameters.salesSurcharge);
};
function EstimationNode(logicalId, id, createdAt, updatedAt) {
  id = id === VOID ? null : id;
  createdAt = createdAt === VOID ? null : createdAt;
  updatedAt = updatedAt === VOID ? null : updatedAt;
  BaseDomain.call(this, id, createdAt, updatedAt);
  this.f7_1 = logicalId;
}
protoOf(EstimationNode).g7 = function () {
  return this.f7_1;
};
function leaves(_this__u8e3s4) {
  var tmp;
  if (_this__u8e3s4 instanceof EstimationItem) {
    tmp = sequenceOf([_this__u8e3s4]);
  } else {
    if (_this__u8e3s4 instanceof EstimationGroup) {
      var tmp_0 = asSequence(_this__u8e3s4.children);
      tmp = flatMap(tmp_0, leaves$lambda);
    } else {
      noWhenBranchMatchedException();
    }
  }
  return tmp;
}
function leaves$lambda(it) {
  return leaves(it);
}
function EstimationParameter(name, value, comment, _id, _createdAt, _updatedAt) {
  value = value === VOID ? 0.0 : value;
  comment = comment === VOID ? '' : comment;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.c8_1 = name;
  this.d8_1 = value;
  this.e8_1 = comment;
  this.f8_1 = _id;
  this.g8_1 = _createdAt;
  this.h8_1 = _updatedAt;
}
protoOf(EstimationParameter).y = function () {
  return this.c8_1;
};
protoOf(EstimationParameter).i8 = function () {
  return this.d8_1;
};
protoOf(EstimationParameter).s5 = function () {
  return this.e8_1;
};
protoOf(EstimationParameter).p4 = function () {
  return this.name;
};
protoOf(EstimationParameter).q4 = function () {
  return this.value;
};
protoOf(EstimationParameter).r4 = function () {
  return this.comment;
};
protoOf(EstimationParameter).t5 = function (name, value, comment, _id, _createdAt, _updatedAt) {
  return new EstimationParameter(name, value, comment, _id, _createdAt, _updatedAt);
};
protoOf(EstimationParameter).copy = function (name, value, comment, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  value = value === VOID ? this.value : value;
  comment = comment === VOID ? this.comment : comment;
  _id = _id === VOID ? this.f8_1 : _id;
  _createdAt = _createdAt === VOID ? this.g8_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.h8_1 : _updatedAt;
  return $super === VOID ? this.t5(name, value, comment, _id, _createdAt, _updatedAt) : $super.t5.call(this, name, value, comment, _id, _createdAt, _updatedAt);
};
protoOf(EstimationParameter).toString = function () {
  return 'EstimationParameter(name=' + this.name + ', value=' + this.value + ', comment=' + this.comment + ', _id=' + this.f8_1 + ', _createdAt=' + this.g8_1 + ', _updatedAt=' + this.h8_1 + ')';
};
protoOf(EstimationParameter).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getNumberHashCode(this.value) | 0;
  result = imul(result, 31) + getStringHashCode(this.comment) | 0;
  result = imul(result, 31) + (this.f8_1 == null ? 0 : getStringHashCode(this.f8_1)) | 0;
  result = imul(result, 31) + (this.g8_1 == null ? 0 : getStringHashCode(this.g8_1)) | 0;
  result = imul(result, 31) + (this.h8_1 == null ? 0 : getStringHashCode(this.h8_1)) | 0;
  return result;
};
protoOf(EstimationParameter).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof EstimationParameter))
    return false;
  var tmp0_other_with_cast = other instanceof EstimationParameter ? other : THROW_CCE();
  if (!(this.name === tmp0_other_with_cast.name))
    return false;
  if (!equals(this.value, tmp0_other_with_cast.value))
    return false;
  if (!(this.comment === tmp0_other_with_cast.comment))
    return false;
  if (!(this.f8_1 == tmp0_other_with_cast.f8_1))
    return false;
  if (!(this.g8_1 == tmp0_other_with_cast.g8_1))
    return false;
  if (!(this.h8_1 == tmp0_other_with_cast.h8_1))
    return false;
  return true;
};
function EstimationVersion(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt) {
  status = status === VOID ? EstimationVersionStatus_DRAFT_getInstance() : status;
  createdBy = createdBy === VOID ? null : createdBy;
  totalEffort = totalEffort === VOID ? 0.0 : totalEffort;
  notes = notes === VOID ? '' : notes;
  parameters = parameters === VOID ? emptyList() : parameters;
  effortDrivers = effortDrivers === VOID ? emptyList() : effortDrivers;
  phases = phases === VOID ? emptyList() : phases;
  additionalCosts = additionalCosts === VOID ? emptyList() : additionalCosts;
  roots = roots === VOID ? emptyList() : roots;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.m8_1 = versionNumber;
  this.n8_1 = status;
  this.o8_1 = createdBy;
  this.p8_1 = totalEffort;
  this.q8_1 = notes;
  this.r8_1 = parameters;
  this.s8_1 = effortDrivers;
  this.t8_1 = phases;
  this.u8_1 = additionalCosts;
  this.v8_1 = roots;
  this.w8_1 = _id;
  this.x8_1 = _createdAt;
  this.y8_1 = _updatedAt;
}
protoOf(EstimationVersion).z8 = function () {
  return this.m8_1;
};
protoOf(EstimationVersion).a9 = function () {
  return this.n8_1;
};
protoOf(EstimationVersion).b9 = function () {
  return this.o8_1;
};
protoOf(EstimationVersion).c9 = function () {
  return this.p8_1;
};
protoOf(EstimationVersion).d9 = function () {
  return this.q8_1;
};
protoOf(EstimationVersion).e9 = function () {
  return this.r8_1;
};
protoOf(EstimationVersion).f9 = function () {
  return this.s8_1;
};
protoOf(EstimationVersion).g9 = function () {
  return this.t8_1;
};
protoOf(EstimationVersion).h9 = function () {
  return this.u8_1;
};
protoOf(EstimationVersion).i9 = function () {
  return this.v8_1;
};
protoOf(EstimationVersion).parameterValue = function (name) {
  // Inline function 'kotlin.collections.find' call
  var tmp0 = this.parameters;
  var tmp$ret$1;
  $l$block: {
    // Inline function 'kotlin.collections.firstOrNull' call
    var _iterator__ex2g4s = tmp0.e();
    while (_iterator__ex2g4s.f()) {
      var element = _iterator__ex2g4s.g();
      if (element.name === name) {
        tmp$ret$1 = element;
        break $l$block;
      }
    }
    tmp$ret$1 = null;
  }
  var tmp0_safe_receiver = tmp$ret$1;
  return tmp0_safe_receiver == null ? null : tmp0_safe_receiver.value;
};
protoOf(EstimationVersion).calculate = function () {
  var tmp0_elvis_lhs = this.parameterValue('Standardabweichungsfaktor');
  var stdDevFactor = tmp0_elvis_lhs == null ? 2.0 : tmp0_elvis_lhs;
  var tmp1_elvis_lhs = this.parameterValue('Tagessatz');
  var dailyRate = tmp1_elvis_lhs == null ? 800.0 : tmp1_elvis_lhs;
  var tmp2_elvis_lhs = this.parameterValue('Vertriebszuschlag');
  var salesSurcharge = tmp2_elvis_lhs == null ? 0.1 : tmp2_elvis_lhs;
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.effortDrivers.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    var tmp = sum;
    sum = tmp + element.factor;
  }
  var totalDriverFactor = sum;
  // Inline function 'kotlin.collections.flatMap' call
  var tmp0 = this.roots;
  // Inline function 'kotlin.collections.flatMapTo' call
  var destination = ArrayList_init_$Create$_0();
  var _iterator__ex2g4s_0 = tmp0.e();
  while (_iterator__ex2g4s_0.f()) {
    var element_0 = _iterator__ex2g4s_0.g();
    var list = toList_0(leaves(element_0));
    addAll(destination, list);
  }
  var leaves_0 = destination;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_0 = 0;
  var _iterator__ex2g4s_1 = leaves_0.e();
  while (_iterator__ex2g4s_1.f()) {
    var element_1 = _iterator__ex2g4s_1.g();
    var tmp_0 = sum_0;
    sum_0 = tmp_0 + element_1.variance;
  }
  var totalVariance = sum_0;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_1 = 0;
  var _iterator__ex2g4s_2 = leaves_0.e();
  while (_iterator__ex2g4s_2.f()) {
    var element_2 = _iterator__ex2g4s_2.g();
    var tmp_1 = sum_1;
    sum_1 = tmp_1 + element_2.mean;
  }
  var totalMean = sum_1;
  var riskFactor = PertCalculation_instance.riskFactor(totalMean, totalVariance, stdDevFactor);
  var params = new CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
  // Inline function 'kotlin.collections.map' call
  var this_0 = this.roots;
  // Inline function 'kotlin.collections.mapTo' call
  var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
  var _iterator__ex2g4s_3 = this_0.e();
  while (_iterator__ex2g4s_3.f()) {
    var item = _iterator__ex2g4s_3.g();
    var tmp$ret$9 = item.withCalculationParameters(params);
    destination_0.l(tmp$ret$9);
  }
  var newRoots = destination_0;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_2 = 0;
  var _iterator__ex2g4s_4 = newRoots.e();
  while (_iterator__ex2g4s_4.f()) {
    var element_3 = _iterator__ex2g4s_4.g();
    var tmp_2 = sum_2;
    sum_2 = tmp_2 + element_3.offerPT;
  }
  var newTotalEffort = sum_2;
  return this.copy(VOID, VOID, VOID, newTotalEffort, VOID, VOID, VOID, VOID, VOID, newRoots);
};
protoOf(EstimationVersion).p4 = function () {
  return this.versionNumber;
};
protoOf(EstimationVersion).q4 = function () {
  return this.status;
};
protoOf(EstimationVersion).r4 = function () {
  return this.createdBy;
};
protoOf(EstimationVersion).s4 = function () {
  return this.totalEffort;
};
protoOf(EstimationVersion).t4 = function () {
  return this.notes;
};
protoOf(EstimationVersion).j9 = function () {
  return this.parameters;
};
protoOf(EstimationVersion).k9 = function () {
  return this.effortDrivers;
};
protoOf(EstimationVersion).l9 = function () {
  return this.phases;
};
protoOf(EstimationVersion).m9 = function () {
  return this.additionalCosts;
};
protoOf(EstimationVersion).n9 = function () {
  return this.roots;
};
protoOf(EstimationVersion).o9 = function (versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt) {
  return new EstimationVersion(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt);
};
protoOf(EstimationVersion).copy = function (versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt, $super) {
  versionNumber = versionNumber === VOID ? this.versionNumber : versionNumber;
  status = status === VOID ? this.status : status;
  createdBy = createdBy === VOID ? this.createdBy : createdBy;
  totalEffort = totalEffort === VOID ? this.totalEffort : totalEffort;
  notes = notes === VOID ? this.notes : notes;
  parameters = parameters === VOID ? this.parameters : parameters;
  effortDrivers = effortDrivers === VOID ? this.effortDrivers : effortDrivers;
  phases = phases === VOID ? this.phases : phases;
  additionalCosts = additionalCosts === VOID ? this.additionalCosts : additionalCosts;
  roots = roots === VOID ? this.roots : roots;
  _id = _id === VOID ? this.w8_1 : _id;
  _createdAt = _createdAt === VOID ? this.x8_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.y8_1 : _updatedAt;
  return $super === VOID ? this.o9(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt) : $super.o9.call(this, versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt);
};
protoOf(EstimationVersion).toString = function () {
  return 'EstimationVersion(versionNumber=' + this.versionNumber + ', status=' + this.status.toString() + ', createdBy=' + toString(this.createdBy) + ', totalEffort=' + this.totalEffort + ', notes=' + this.notes + ', parameters=' + toString_0(this.parameters) + ', effortDrivers=' + toString_0(this.effortDrivers) + ', phases=' + toString_0(this.phases) + ', additionalCosts=' + toString_0(this.additionalCosts) + ', roots=' + toString_0(this.roots) + ', _id=' + this.w8_1 + ', _createdAt=' + this.x8_1 + ', _updatedAt=' + this.y8_1 + ')';
};
protoOf(EstimationVersion).hashCode = function () {
  var result = this.versionNumber;
  result = imul(result, 31) + this.status.hashCode() | 0;
  result = imul(result, 31) + (this.createdBy == null ? 0 : this.createdBy.hashCode()) | 0;
  result = imul(result, 31) + getNumberHashCode(this.totalEffort) | 0;
  result = imul(result, 31) + getStringHashCode(this.notes) | 0;
  result = imul(result, 31) + hashCode(this.parameters) | 0;
  result = imul(result, 31) + hashCode(this.effortDrivers) | 0;
  result = imul(result, 31) + hashCode(this.phases) | 0;
  result = imul(result, 31) + hashCode(this.additionalCosts) | 0;
  result = imul(result, 31) + hashCode(this.roots) | 0;
  result = imul(result, 31) + (this.w8_1 == null ? 0 : getStringHashCode(this.w8_1)) | 0;
  result = imul(result, 31) + (this.x8_1 == null ? 0 : getStringHashCode(this.x8_1)) | 0;
  result = imul(result, 31) + (this.y8_1 == null ? 0 : getStringHashCode(this.y8_1)) | 0;
  return result;
};
protoOf(EstimationVersion).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof EstimationVersion))
    return false;
  var tmp0_other_with_cast = other instanceof EstimationVersion ? other : THROW_CCE();
  if (!(this.versionNumber === tmp0_other_with_cast.versionNumber))
    return false;
  if (!this.status.equals(tmp0_other_with_cast.status))
    return false;
  if (!equals(this.createdBy, tmp0_other_with_cast.createdBy))
    return false;
  if (!equals(this.totalEffort, tmp0_other_with_cast.totalEffort))
    return false;
  if (!(this.notes === tmp0_other_with_cast.notes))
    return false;
  if (!equals(this.parameters, tmp0_other_with_cast.parameters))
    return false;
  if (!equals(this.effortDrivers, tmp0_other_with_cast.effortDrivers))
    return false;
  if (!equals(this.phases, tmp0_other_with_cast.phases))
    return false;
  if (!equals(this.additionalCosts, tmp0_other_with_cast.additionalCosts))
    return false;
  if (!equals(this.roots, tmp0_other_with_cast.roots))
    return false;
  if (!(this.w8_1 == tmp0_other_with_cast.w8_1))
    return false;
  if (!(this.x8_1 == tmp0_other_with_cast.x8_1))
    return false;
  if (!(this.y8_1 == tmp0_other_with_cast.y8_1))
    return false;
  return true;
};
var EstimationVersionStatus_DRAFT_instance;
var EstimationVersionStatus_SUBMITTED_instance;
function values_0() {
  return [EstimationVersionStatus_DRAFT_getInstance(), EstimationVersionStatus_SUBMITTED_getInstance()];
}
function valueOf_0(value) {
  switch (value) {
    case 'DRAFT':
      return EstimationVersionStatus_DRAFT_getInstance();
    case 'SUBMITTED':
      return EstimationVersionStatus_SUBMITTED_getInstance();
    default:
      EstimationVersionStatus_initEntries();
      THROW_IAE('No enum constant value.');
      break;
  }
}
var EstimationVersionStatus_entriesInitialized;
function EstimationVersionStatus_initEntries() {
  if (EstimationVersionStatus_entriesInitialized)
    return Unit_instance;
  EstimationVersionStatus_entriesInitialized = true;
  EstimationVersionStatus_DRAFT_instance = new EstimationVersionStatus('DRAFT', 0);
  EstimationVersionStatus_SUBMITTED_instance = new EstimationVersionStatus('SUBMITTED', 1);
}
function EstimationVersionStatus(name, ordinal) {
  Enum.call(this, name, ordinal);
}
function EstimationVersionStatus_DRAFT_getInstance() {
  EstimationVersionStatus_initEntries();
  return EstimationVersionStatus_DRAFT_instance;
}
function EstimationVersionStatus_SUBMITTED_getInstance() {
  EstimationVersionStatus_initEntries();
  return EstimationVersionStatus_SUBMITTED_instance;
}
function FixedEstimationItem(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  _code = _code === VOID ? '' : _code;
  _minEffort = _minEffort === VOID ? 0.0 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? 0.0 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? 0.0 : _maxEffort;
  _assumptions = _assumptions === VOID ? '' : _assumptions;
  _phase = _phase === VOID ? null : _phase;
  _logicalId = _logicalId === VOID ? newId() : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? new CalculationParameters() : _calculationParameters;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  EstimationItem.call(this, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
  this.da_1 = _description;
  this.ea_1 = _code;
  this.fa_1 = _minEffort;
  this.ga_1 = _expectedEffort;
  this.ha_1 = _maxEffort;
  this.ia_1 = _assumptions;
  this.ja_1 = _phase;
  this.ka_1 = _logicalId;
  this.la_1 = _calculationParameters;
  this.ma_1 = _id;
  this.na_1 = _createdAt;
  this.oa_1 = _updatedAt;
}
protoOf(FixedEstimationItem).withCalculationParameters = function (params) {
  return this.copy(VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, params);
};
protoOf(FixedEstimationItem).pa = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  return new FixedEstimationItem(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(FixedEstimationItem).copy = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
  _description = _description === VOID ? this.da_1 : _description;
  _code = _code === VOID ? this.ea_1 : _code;
  _minEffort = _minEffort === VOID ? this.fa_1 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? this.ga_1 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? this.ha_1 : _maxEffort;
  _assumptions = _assumptions === VOID ? this.ia_1 : _assumptions;
  _phase = _phase === VOID ? this.ja_1 : _phase;
  _logicalId = _logicalId === VOID ? this.ka_1 : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? this.la_1 : _calculationParameters;
  _id = _id === VOID ? this.ma_1 : _id;
  _createdAt = _createdAt === VOID ? this.na_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.oa_1 : _updatedAt;
  return $super === VOID ? this.pa(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.pa.call(this, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(FixedEstimationItem).toString = function () {
  return 'FixedEstimationItem(_description=' + this.da_1 + ', _code=' + this.ea_1 + ', _minEffort=' + this.fa_1 + ', _expectedEffort=' + this.ga_1 + ', _maxEffort=' + this.ha_1 + ', _assumptions=' + this.ia_1 + ', _phase=' + toString(this.ja_1) + ', _logicalId=' + this.ka_1 + ', _calculationParameters=' + this.la_1.toString() + ', _id=' + this.ma_1 + ', _createdAt=' + this.na_1 + ', _updatedAt=' + this.oa_1 + ')';
};
protoOf(FixedEstimationItem).hashCode = function () {
  var result = getStringHashCode(this.da_1);
  result = imul(result, 31) + getStringHashCode(this.ea_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.fa_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.ga_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.ha_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.ia_1) | 0;
  result = imul(result, 31) + (this.ja_1 == null ? 0 : this.ja_1.hashCode()) | 0;
  result = imul(result, 31) + getStringHashCode(this.ka_1) | 0;
  result = imul(result, 31) + this.la_1.hashCode() | 0;
  result = imul(result, 31) + (this.ma_1 == null ? 0 : getStringHashCode(this.ma_1)) | 0;
  result = imul(result, 31) + (this.na_1 == null ? 0 : getStringHashCode(this.na_1)) | 0;
  result = imul(result, 31) + (this.oa_1 == null ? 0 : getStringHashCode(this.oa_1)) | 0;
  return result;
};
protoOf(FixedEstimationItem).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof FixedEstimationItem))
    return false;
  var tmp0_other_with_cast = other instanceof FixedEstimationItem ? other : THROW_CCE();
  if (!(this.da_1 === tmp0_other_with_cast.da_1))
    return false;
  if (!(this.ea_1 === tmp0_other_with_cast.ea_1))
    return false;
  if (!equals(this.fa_1, tmp0_other_with_cast.fa_1))
    return false;
  if (!equals(this.ga_1, tmp0_other_with_cast.ga_1))
    return false;
  if (!equals(this.ha_1, tmp0_other_with_cast.ha_1))
    return false;
  if (!(this.ia_1 === tmp0_other_with_cast.ia_1))
    return false;
  if (!equals(this.ja_1, tmp0_other_with_cast.ja_1))
    return false;
  if (!(this.ka_1 === tmp0_other_with_cast.ka_1))
    return false;
  if (!this.la_1.equals(tmp0_other_with_cast.la_1))
    return false;
  if (!(this.ma_1 == tmp0_other_with_cast.ma_1))
    return false;
  if (!(this.na_1 == tmp0_other_with_cast.na_1))
    return false;
  if (!(this.oa_1 == tmp0_other_with_cast.oa_1))
    return false;
  return true;
};
function PertCalculation_0() {
}
protoOf(PertCalculation_0).mean = function (min, expected, max) {
  return (min + 4 * expected + max) / 6.0;
};
protoOf(PertCalculation_0).variance = function (min, max) {
  var range = (max - min) / 6.0;
  return range * range;
};
protoOf(PertCalculation_0).riskFactor = function (totalMean, totalVariance, stdDevFactor) {
  if (totalMean <= 0)
    return 0.0;
  // Inline function 'kotlin.math.sqrt' call
  return Math.sqrt(totalVariance) * stdDevFactor / totalMean;
};
var PertCalculation_instance;
function PertCalculation_getInstance() {
  return PertCalculation_instance;
}
function Project(name, description, client, status, owner, _id, _createdAt, _updatedAt) {
  description = description === VOID ? '' : description;
  client = client === VOID ? '' : client;
  status = status === VOID ? ProjectStatus_ACTIVE_getInstance() : status;
  owner = owner === VOID ? null : owner;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.ta_1 = name;
  this.ua_1 = description;
  this.va_1 = client;
  this.wa_1 = status;
  this.xa_1 = owner;
  this.ya_1 = _id;
  this.za_1 = _createdAt;
  this.ab_1 = _updatedAt;
}
protoOf(Project).y = function () {
  return this.ta_1;
};
protoOf(Project).k4 = function () {
  return this.ua_1;
};
protoOf(Project).bb = function () {
  return this.va_1;
};
protoOf(Project).a9 = function () {
  return this.wa_1;
};
protoOf(Project).cb = function () {
  return this.xa_1;
};
protoOf(Project).p4 = function () {
  return this.name;
};
protoOf(Project).q4 = function () {
  return this.description;
};
protoOf(Project).r4 = function () {
  return this.client;
};
protoOf(Project).s4 = function () {
  return this.status;
};
protoOf(Project).t4 = function () {
  return this.owner;
};
protoOf(Project).db = function (name, description, client, status, owner, _id, _createdAt, _updatedAt) {
  return new Project(name, description, client, status, owner, _id, _createdAt, _updatedAt);
};
protoOf(Project).copy = function (name, description, client, status, owner, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  description = description === VOID ? this.description : description;
  client = client === VOID ? this.client : client;
  status = status === VOID ? this.status : status;
  owner = owner === VOID ? this.owner : owner;
  _id = _id === VOID ? this.ya_1 : _id;
  _createdAt = _createdAt === VOID ? this.za_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.ab_1 : _updatedAt;
  return $super === VOID ? this.db(name, description, client, status, owner, _id, _createdAt, _updatedAt) : $super.db.call(this, name, description, client, status, owner, _id, _createdAt, _updatedAt);
};
protoOf(Project).toString = function () {
  return 'Project(name=' + this.name + ', description=' + this.description + ', client=' + this.client + ', status=' + this.status.toString() + ', owner=' + toString(this.owner) + ', _id=' + this.ya_1 + ', _createdAt=' + this.za_1 + ', _updatedAt=' + this.ab_1 + ')';
};
protoOf(Project).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getStringHashCode(this.description) | 0;
  result = imul(result, 31) + getStringHashCode(this.client) | 0;
  result = imul(result, 31) + this.status.hashCode() | 0;
  result = imul(result, 31) + (this.owner == null ? 0 : this.owner.hashCode()) | 0;
  result = imul(result, 31) + (this.ya_1 == null ? 0 : getStringHashCode(this.ya_1)) | 0;
  result = imul(result, 31) + (this.za_1 == null ? 0 : getStringHashCode(this.za_1)) | 0;
  result = imul(result, 31) + (this.ab_1 == null ? 0 : getStringHashCode(this.ab_1)) | 0;
  return result;
};
protoOf(Project).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Project))
    return false;
  var tmp0_other_with_cast = other instanceof Project ? other : THROW_CCE();
  if (!(this.name === tmp0_other_with_cast.name))
    return false;
  if (!(this.description === tmp0_other_with_cast.description))
    return false;
  if (!(this.client === tmp0_other_with_cast.client))
    return false;
  if (!this.status.equals(tmp0_other_with_cast.status))
    return false;
  if (!equals(this.owner, tmp0_other_with_cast.owner))
    return false;
  if (!(this.ya_1 == tmp0_other_with_cast.ya_1))
    return false;
  if (!(this.za_1 == tmp0_other_with_cast.za_1))
    return false;
  if (!(this.ab_1 == tmp0_other_with_cast.ab_1))
    return false;
  return true;
};
function ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
  durationWeeks = durationWeeks === VOID ? 0.0 : durationWeeks;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.hb_1 = name;
  this.ib_1 = abbreviation;
  this.jb_1 = durationWeeks;
  this.kb_1 = _id;
  this.lb_1 = _createdAt;
  this.mb_1 = _updatedAt;
}
protoOf(ProjectPhase).y = function () {
  return this.hb_1;
};
protoOf(ProjectPhase).nb = function () {
  return this.ib_1;
};
protoOf(ProjectPhase).ob = function () {
  return this.jb_1;
};
protoOf(ProjectPhase).p4 = function () {
  return this.name;
};
protoOf(ProjectPhase).q4 = function () {
  return this.abbreviation;
};
protoOf(ProjectPhase).r4 = function () {
  return this.durationWeeks;
};
protoOf(ProjectPhase).pb = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
  return new ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
};
protoOf(ProjectPhase).copy = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  abbreviation = abbreviation === VOID ? this.abbreviation : abbreviation;
  durationWeeks = durationWeeks === VOID ? this.durationWeeks : durationWeeks;
  _id = _id === VOID ? this.kb_1 : _id;
  _createdAt = _createdAt === VOID ? this.lb_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.mb_1 : _updatedAt;
  return $super === VOID ? this.pb(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) : $super.pb.call(this, name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
};
protoOf(ProjectPhase).toString = function () {
  return 'ProjectPhase(name=' + this.name + ', abbreviation=' + this.abbreviation + ', durationWeeks=' + this.durationWeeks + ', _id=' + this.kb_1 + ', _createdAt=' + this.lb_1 + ', _updatedAt=' + this.mb_1 + ')';
};
protoOf(ProjectPhase).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getStringHashCode(this.abbreviation) | 0;
  result = imul(result, 31) + getNumberHashCode(this.durationWeeks) | 0;
  result = imul(result, 31) + (this.kb_1 == null ? 0 : getStringHashCode(this.kb_1)) | 0;
  result = imul(result, 31) + (this.lb_1 == null ? 0 : getStringHashCode(this.lb_1)) | 0;
  result = imul(result, 31) + (this.mb_1 == null ? 0 : getStringHashCode(this.mb_1)) | 0;
  return result;
};
protoOf(ProjectPhase).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof ProjectPhase))
    return false;
  var tmp0_other_with_cast = other instanceof ProjectPhase ? other : THROW_CCE();
  if (!(this.name === tmp0_other_with_cast.name))
    return false;
  if (!(this.abbreviation === tmp0_other_with_cast.abbreviation))
    return false;
  if (!equals(this.durationWeeks, tmp0_other_with_cast.durationWeeks))
    return false;
  if (!(this.kb_1 == tmp0_other_with_cast.kb_1))
    return false;
  if (!(this.lb_1 == tmp0_other_with_cast.lb_1))
    return false;
  if (!(this.mb_1 == tmp0_other_with_cast.mb_1))
    return false;
  return true;
};
var ProjectStatus_ACTIVE_instance;
var ProjectStatus_ARCHIVED_instance;
function values_1() {
  return [ProjectStatus_ACTIVE_getInstance(), ProjectStatus_ARCHIVED_getInstance()];
}
function valueOf_1(value) {
  switch (value) {
    case 'ACTIVE':
      return ProjectStatus_ACTIVE_getInstance();
    case 'ARCHIVED':
      return ProjectStatus_ARCHIVED_getInstance();
    default:
      ProjectStatus_initEntries();
      THROW_IAE('No enum constant value.');
      break;
  }
}
var ProjectStatus_entriesInitialized;
function ProjectStatus_initEntries() {
  if (ProjectStatus_entriesInitialized)
    return Unit_instance;
  ProjectStatus_entriesInitialized = true;
  ProjectStatus_ACTIVE_instance = new ProjectStatus('ACTIVE', 0);
  ProjectStatus_ARCHIVED_instance = new ProjectStatus('ARCHIVED', 1);
}
function ProjectStatus(name, ordinal) {
  Enum.call(this, name, ordinal);
}
function ProjectStatus_ACTIVE_getInstance() {
  ProjectStatus_initEntries();
  return ProjectStatus_ACTIVE_instance;
}
function ProjectStatus_ARCHIVED_getInstance() {
  ProjectStatus_initEntries();
  return ProjectStatus_ARCHIVED_instance;
}
function TimeRelativeEstimationItem(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  unit = unit === VOID ? 'h/Woche' : unit;
  _code = _code === VOID ? '' : _code;
  _minEffort = _minEffort === VOID ? 0.0 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? 0.0 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? 0.0 : _maxEffort;
  _assumptions = _assumptions === VOID ? '' : _assumptions;
  _phase = _phase === VOID ? null : _phase;
  _logicalId = _logicalId === VOID ? newId() : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? new CalculationParameters() : _calculationParameters;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  EstimationItem.call(this, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
  this.ec_1 = unit;
  this.fc_1 = _description;
  this.gc_1 = _code;
  this.hc_1 = _minEffort;
  this.ic_1 = _expectedEffort;
  this.jc_1 = _maxEffort;
  this.kc_1 = _assumptions;
  this.lc_1 = _phase;
  this.mc_1 = _logicalId;
  this.nc_1 = _calculationParameters;
  this.oc_1 = _id;
  this.pc_1 = _createdAt;
  this.qc_1 = _updatedAt;
}
protoOf(TimeRelativeEstimationItem).rc = function () {
  return this.ec_1;
};
protoOf(TimeRelativeEstimationItem).u6 = function () {
  var tmp = PertCalculation_instance.mean(this.minEffort, this.expectedEffort, this.maxEffort);
  var tmp0_safe_receiver = this.phase;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.durationWeeks;
  return tmp * (tmp1_elvis_lhs == null ? 0.0 : tmp1_elvis_lhs);
};
protoOf(TimeRelativeEstimationItem).v6 = function () {
  var tmp0_safe_receiver = this.phase;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.durationWeeks;
  var d = tmp1_elvis_lhs == null ? 0.0 : tmp1_elvis_lhs;
  return PertCalculation_instance.variance(this.minEffort, this.maxEffort) * d * d;
};
protoOf(TimeRelativeEstimationItem).withCalculationParameters = function (params) {
  return this.copy(VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, params);
};
protoOf(TimeRelativeEstimationItem).p4 = function () {
  return this.unit;
};
protoOf(TimeRelativeEstimationItem).sc = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  return new TimeRelativeEstimationItem(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(TimeRelativeEstimationItem).copy = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
  unit = unit === VOID ? this.unit : unit;
  _description = _description === VOID ? this.fc_1 : _description;
  _code = _code === VOID ? this.gc_1 : _code;
  _minEffort = _minEffort === VOID ? this.hc_1 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? this.ic_1 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? this.jc_1 : _maxEffort;
  _assumptions = _assumptions === VOID ? this.kc_1 : _assumptions;
  _phase = _phase === VOID ? this.lc_1 : _phase;
  _logicalId = _logicalId === VOID ? this.mc_1 : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? this.nc_1 : _calculationParameters;
  _id = _id === VOID ? this.oc_1 : _id;
  _createdAt = _createdAt === VOID ? this.pc_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.qc_1 : _updatedAt;
  return $super === VOID ? this.sc(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.sc.call(this, unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(TimeRelativeEstimationItem).toString = function () {
  return 'TimeRelativeEstimationItem(unit=' + this.unit + ', _description=' + this.fc_1 + ', _code=' + this.gc_1 + ', _minEffort=' + this.hc_1 + ', _expectedEffort=' + this.ic_1 + ', _maxEffort=' + this.jc_1 + ', _assumptions=' + this.kc_1 + ', _phase=' + toString(this.lc_1) + ', _logicalId=' + this.mc_1 + ', _calculationParameters=' + this.nc_1.toString() + ', _id=' + this.oc_1 + ', _createdAt=' + this.pc_1 + ', _updatedAt=' + this.qc_1 + ')';
};
protoOf(TimeRelativeEstimationItem).hashCode = function () {
  var result = getStringHashCode(this.unit);
  result = imul(result, 31) + getStringHashCode(this.fc_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.gc_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.hc_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.ic_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.jc_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.kc_1) | 0;
  result = imul(result, 31) + (this.lc_1 == null ? 0 : this.lc_1.hashCode()) | 0;
  result = imul(result, 31) + getStringHashCode(this.mc_1) | 0;
  result = imul(result, 31) + this.nc_1.hashCode() | 0;
  result = imul(result, 31) + (this.oc_1 == null ? 0 : getStringHashCode(this.oc_1)) | 0;
  result = imul(result, 31) + (this.pc_1 == null ? 0 : getStringHashCode(this.pc_1)) | 0;
  result = imul(result, 31) + (this.qc_1 == null ? 0 : getStringHashCode(this.qc_1)) | 0;
  return result;
};
protoOf(TimeRelativeEstimationItem).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof TimeRelativeEstimationItem))
    return false;
  var tmp0_other_with_cast = other instanceof TimeRelativeEstimationItem ? other : THROW_CCE();
  if (!(this.unit === tmp0_other_with_cast.unit))
    return false;
  if (!(this.fc_1 === tmp0_other_with_cast.fc_1))
    return false;
  if (!(this.gc_1 === tmp0_other_with_cast.gc_1))
    return false;
  if (!equals(this.hc_1, tmp0_other_with_cast.hc_1))
    return false;
  if (!equals(this.ic_1, tmp0_other_with_cast.ic_1))
    return false;
  if (!equals(this.jc_1, tmp0_other_with_cast.jc_1))
    return false;
  if (!(this.kc_1 === tmp0_other_with_cast.kc_1))
    return false;
  if (!equals(this.lc_1, tmp0_other_with_cast.lc_1))
    return false;
  if (!(this.mc_1 === tmp0_other_with_cast.mc_1))
    return false;
  if (!this.nc_1.equals(tmp0_other_with_cast.nc_1))
    return false;
  if (!(this.oc_1 == tmp0_other_with_cast.oc_1))
    return false;
  if (!(this.pc_1 == tmp0_other_with_cast.pc_1))
    return false;
  if (!(this.qc_1 == tmp0_other_with_cast.qc_1))
    return false;
  return true;
};
function User(entraSubjectId, displayName, _id, _createdAt, _updatedAt) {
  entraSubjectId = entraSubjectId === VOID ? null : entraSubjectId;
  displayName = displayName === VOID ? '' : displayName;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.wc_1 = entraSubjectId;
  this.xc_1 = displayName;
  this.yc_1 = _id;
  this.zc_1 = _createdAt;
  this.ad_1 = _updatedAt;
}
protoOf(User).bd = function () {
  return this.wc_1;
};
protoOf(User).cd = function () {
  return this.xc_1;
};
protoOf(User).p4 = function () {
  return this.entraSubjectId;
};
protoOf(User).q4 = function () {
  return this.displayName;
};
protoOf(User).dd = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt) {
  return new User(entraSubjectId, displayName, _id, _createdAt, _updatedAt);
};
protoOf(User).copy = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt, $super) {
  entraSubjectId = entraSubjectId === VOID ? this.entraSubjectId : entraSubjectId;
  displayName = displayName === VOID ? this.displayName : displayName;
  _id = _id === VOID ? this.yc_1 : _id;
  _createdAt = _createdAt === VOID ? this.zc_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.ad_1 : _updatedAt;
  return $super === VOID ? this.dd(entraSubjectId, displayName, _id, _createdAt, _updatedAt) : $super.dd.call(this, entraSubjectId, displayName, _id, _createdAt, _updatedAt);
};
protoOf(User).toString = function () {
  return 'User(entraSubjectId=' + this.entraSubjectId + ', displayName=' + this.displayName + ', _id=' + this.yc_1 + ', _createdAt=' + this.zc_1 + ', _updatedAt=' + this.ad_1 + ')';
};
protoOf(User).hashCode = function () {
  var result = this.entraSubjectId == null ? 0 : getStringHashCode(this.entraSubjectId);
  result = imul(result, 31) + getStringHashCode(this.displayName) | 0;
  result = imul(result, 31) + (this.yc_1 == null ? 0 : getStringHashCode(this.yc_1)) | 0;
  result = imul(result, 31) + (this.zc_1 == null ? 0 : getStringHashCode(this.zc_1)) | 0;
  result = imul(result, 31) + (this.ad_1 == null ? 0 : getStringHashCode(this.ad_1)) | 0;
  return result;
};
protoOf(User).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof User))
    return false;
  var tmp0_other_with_cast = other instanceof User ? other : THROW_CCE();
  if (!(this.entraSubjectId == tmp0_other_with_cast.entraSubjectId))
    return false;
  if (!(this.displayName === tmp0_other_with_cast.displayName))
    return false;
  if (!(this.yc_1 == tmp0_other_with_cast.yc_1))
    return false;
  if (!(this.zc_1 == tmp0_other_with_cast.zc_1))
    return false;
  if (!(this.ad_1 == tmp0_other_with_cast.ad_1))
    return false;
  return true;
};
function EstimationCalculator() {
}
protoOf(EstimationCalculator).calculate = function (version) {
  return version.calculate();
};
protoOf(EstimationCalculator).validateInvariants = function (version) {
  // Inline function 'kotlin.collections.mutableListOf' call
  var results = ArrayList_init_$Create$_0();
  // Inline function 'kotlin.collections.flatMap' call
  var tmp0 = version.roots;
  // Inline function 'kotlin.collections.flatMapTo' call
  var destination = ArrayList_init_$Create$_0();
  var _iterator__ex2g4s = tmp0.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    var list = toList_0(leaves(element));
    addAll(destination, list);
  }
  var allItems = destination;
  var tolerance = 0.2;
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s_0 = allItems.e();
  while (_iterator__ex2g4s_0.f()) {
    var element_0 = _iterator__ex2g4s_0.g();
    var tmp = sum;
    sum = tmp + element_0.offerPT;
  }
  var totalOfferPT = sum;
  results.l(new InvariantResult('Gesamtaufwand = Summe aller AngebotsPT', version.totalEffort - totalOfferPT, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_0 = 0;
  var _iterator__ex2g4s_1 = allItems.e();
  while (_iterator__ex2g4s_1.f()) {
    var element_1 = _iterator__ex2g4s_1.g();
    var tmp_0 = sum_0;
    sum_0 = tmp_0 + element_1.mean;
  }
  var totalMean = sum_0;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_1 = 0;
  var _iterator__ex2g4s_2 = allItems.e();
  while (_iterator__ex2g4s_2.f()) {
    var element_2 = _iterator__ex2g4s_2.g();
    var tmp_1 = sum_1;
    sum_1 = tmp_1 + element_2.variance;
  }
  var totalVariance = sum_1;
  var tmp0_elvis_lhs = version.parameterValue('Standardabweichungsfaktor');
  var stdDevFactor = tmp0_elvis_lhs == null ? 2.0 : tmp0_elvis_lhs;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_2 = 0;
  var _iterator__ex2g4s_3 = version.effortDrivers.e();
  while (_iterator__ex2g4s_3.f()) {
    var element_3 = _iterator__ex2g4s_3.g();
    var tmp_2 = sum_2;
    sum_2 = tmp_2 + element_3.factor;
  }
  var totalDriverFactor = sum_2;
  // Inline function 'kotlin.math.sqrt' call
  var calculatedTotal = totalMean + Math.sqrt(totalVariance) * stdDevFactor + totalMean * totalDriverFactor;
  results.l(new InvariantResult('Summe mit Risiko im PSP = Summe bei Berechnung', totalOfferPT - calculatedTotal, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_3 = 0;
  var _iterator__ex2g4s_4 = version.roots.e();
  while (_iterator__ex2g4s_4.f()) {
    var element_4 = _iterator__ex2g4s_4.g();
    var tmp_3 = sum_3;
    sum_3 = tmp_3 + element_4.offerPT;
  }
  var sumByRoots = sum_3;
  results.l(new InvariantResult('Summe der Wurzeln = Summe der Bl\xE4tter (Akkumulation konsistent)', sumByRoots - totalOfferPT, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_4 = 0;
  var _iterator__ex2g4s_5 = allItems.e();
  while (_iterator__ex2g4s_5.f()) {
    var element_5 = _iterator__ex2g4s_5.g();
    var tmp_4 = sum_4;
    sum_4 = tmp_4 + element_5.cost;
  }
  var totalCost = sum_4;
  var tmp1_elvis_lhs = version.parameterValue('Tagessatz');
  var dailyRate = tmp1_elvis_lhs == null ? 800.0 : tmp1_elvis_lhs;
  var costFromEffort = totalOfferPT * dailyRate;
  results.l(new InvariantResult('Kosten im PSP = Kosten in der Paket\xFCbersicht', totalCost - costFromEffort, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_5 = 0;
  var _iterator__ex2g4s_6 = version.roots.e();
  while (_iterator__ex2g4s_6.f()) {
    var element_6 = _iterator__ex2g4s_6.g();
    var tmp_5 = sum_5;
    sum_5 = tmp_5 + element_6.variance;
  }
  var varianceByRoots = sum_5;
  results.l(new InvariantResult('Varianzakkumulation an der Wurzel = Summe der Bl\xE4tter-Varianzen', varianceByRoots - totalVariance, tolerance));
  // Inline function 'kotlin.collections.toTypedArray' call
  return copyToArray(results);
};
function InvariantResult(description, difference, tolerance) {
  this.description = description;
  this.difference = difference;
  this.tolerance = tolerance;
}
protoOf(InvariantResult).k4 = function () {
  return this.description;
};
protoOf(InvariantResult).ed = function () {
  return this.difference;
};
protoOf(InvariantResult).fd = function () {
  return this.tolerance;
};
protoOf(InvariantResult).gd = function () {
  // Inline function 'kotlin.math.abs' call
  var x = this.difference;
  return Math.abs(x) <= this.tolerance;
};
protoOf(InvariantResult).p4 = function () {
  return this.description;
};
protoOf(InvariantResult).q4 = function () {
  return this.difference;
};
protoOf(InvariantResult).r4 = function () {
  return this.tolerance;
};
protoOf(InvariantResult).hd = function (description, difference, tolerance) {
  return new InvariantResult(description, difference, tolerance);
};
protoOf(InvariantResult).copy = function (description, difference, tolerance, $super) {
  description = description === VOID ? this.description : description;
  difference = difference === VOID ? this.difference : difference;
  tolerance = tolerance === VOID ? this.tolerance : tolerance;
  return $super === VOID ? this.hd(description, difference, tolerance) : $super.hd.call(this, description, difference, tolerance);
};
protoOf(InvariantResult).toString = function () {
  return 'InvariantResult(description=' + this.description + ', difference=' + this.difference + ', tolerance=' + this.tolerance + ')';
};
protoOf(InvariantResult).hashCode = function () {
  var result = getStringHashCode(this.description);
  result = imul(result, 31) + getNumberHashCode(this.difference) | 0;
  result = imul(result, 31) + getNumberHashCode(this.tolerance) | 0;
  return result;
};
protoOf(InvariantResult).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof InvariantResult))
    return false;
  var tmp0_other_with_cast = other instanceof InvariantResult ? other : THROW_CCE();
  if (!(this.description === tmp0_other_with_cast.description))
    return false;
  if (!equals(this.difference, tmp0_other_with_cast.difference))
    return false;
  if (!equals(this.tolerance, tmp0_other_with_cast.tolerance))
    return false;
  return true;
};
//region block: post-declaration
defineProp(protoOf(BaseDomain), 'id', function () {
  return this.y4();
});
defineProp(protoOf(BaseDomain), 'createdAt', function () {
  return this.z4();
});
defineProp(protoOf(BaseDomain), 'updatedAt', function () {
  return this.a5();
});
defineProp(protoOf(AdditionalCost), 'description', function () {
  return this.k4();
});
defineProp(protoOf(AdditionalCost), 'amount', function () {
  return this.l4();
});
defineProp(protoOf(AdditionalCost), 'type', function () {
  return this.m4();
});
defineProp(protoOf(AdditionalCost), 'amountPerWeek', function () {
  return this.n4();
});
defineProp(protoOf(AdditionalCost), 'phase', function () {
  return this.o4();
});
defineProp(protoOf(AdditionalCostType), 'name', protoOf(AdditionalCostType).y);
defineProp(protoOf(AdditionalCostType), 'ordinal', protoOf(AdditionalCostType).z);
defineProp(protoOf(EffortDriver), 'description', function () {
  return this.k4();
});
defineProp(protoOf(EffortDriver), 'factor', function () {
  return this.r5();
});
defineProp(protoOf(EffortDriver), 'comment', function () {
  return this.s5();
});
defineProp(protoOf(Estimation), 'offer', function () {
  return this.e6();
});
defineProp(protoOf(Estimation), 'description', function () {
  return this.k4();
});
defineProp(protoOf(Estimation), 'currentVersion', function () {
  return this.f6();
});
defineProp(protoOf(Estimation), 'versions', function () {
  return this.g6();
});
defineProp(protoOf(EstimationNode), 'logicalId', function () {
  return this.g7();
});
defineProp(protoOf(EstimationNode), 'mean', function () {
  return this.u6();
});
defineProp(protoOf(EstimationNode), 'variance', function () {
  return this.v6();
});
defineProp(protoOf(EstimationNode), 'riskSurcharge', function () {
  return this.w6();
});
defineProp(protoOf(EstimationNode), 'driverSurcharge', function () {
  return this.x6();
});
defineProp(protoOf(EstimationNode), 'offerPT', function () {
  return this.y6();
});
defineProp(protoOf(EstimationNode), 'cost', function () {
  return this.z6();
});
defineProp(protoOf(EstimationNode), 'offerPrice', function () {
  return this.a7();
});
defineProp(protoOf(EstimationGroup), 'title', function () {
  return this.s6();
});
defineProp(protoOf(EstimationGroup), 'children', function () {
  return this.t6();
});
defineProp(protoOf(EstimationItem), 'description', function () {
  return this.k4();
});
defineProp(protoOf(EstimationItem), 'code', function () {
  return this.t7();
});
defineProp(protoOf(EstimationItem), 'minEffort', function () {
  return this.u7();
});
defineProp(protoOf(EstimationItem), 'expectedEffort', function () {
  return this.v7();
});
defineProp(protoOf(EstimationItem), 'maxEffort', function () {
  return this.w7();
});
defineProp(protoOf(EstimationItem), 'assumptions', function () {
  return this.x7();
});
defineProp(protoOf(EstimationItem), 'phase', function () {
  return this.o4();
});
defineProp(protoOf(EstimationItem), 'calculationParameters', function () {
  return this.y7();
});
defineProp(protoOf(EstimationParameter), 'name', function () {
  return this.y();
});
defineProp(protoOf(EstimationParameter), 'value', function () {
  return this.i8();
});
defineProp(protoOf(EstimationParameter), 'comment', function () {
  return this.s5();
});
defineProp(protoOf(EstimationVersion), 'versionNumber', function () {
  return this.z8();
});
defineProp(protoOf(EstimationVersion), 'status', function () {
  return this.a9();
});
defineProp(protoOf(EstimationVersion), 'createdBy', function () {
  return this.b9();
});
defineProp(protoOf(EstimationVersion), 'totalEffort', function () {
  return this.c9();
});
defineProp(protoOf(EstimationVersion), 'notes', function () {
  return this.d9();
});
defineProp(protoOf(EstimationVersion), 'parameters', function () {
  return this.e9();
});
defineProp(protoOf(EstimationVersion), 'effortDrivers', function () {
  return this.f9();
});
defineProp(protoOf(EstimationVersion), 'phases', function () {
  return this.g9();
});
defineProp(protoOf(EstimationVersion), 'additionalCosts', function () {
  return this.h9();
});
defineProp(protoOf(EstimationVersion), 'roots', function () {
  return this.i9();
});
defineProp(protoOf(EstimationVersionStatus), 'name', protoOf(EstimationVersionStatus).y);
defineProp(protoOf(EstimationVersionStatus), 'ordinal', protoOf(EstimationVersionStatus).z);
defineProp(protoOf(Project), 'name', function () {
  return this.y();
});
defineProp(protoOf(Project), 'description', function () {
  return this.k4();
});
defineProp(protoOf(Project), 'client', function () {
  return this.bb();
});
defineProp(protoOf(Project), 'status', function () {
  return this.a9();
});
defineProp(protoOf(Project), 'owner', function () {
  return this.cb();
});
defineProp(protoOf(ProjectPhase), 'name', function () {
  return this.y();
});
defineProp(protoOf(ProjectPhase), 'abbreviation', function () {
  return this.nb();
});
defineProp(protoOf(ProjectPhase), 'durationWeeks', function () {
  return this.ob();
});
defineProp(protoOf(ProjectStatus), 'name', protoOf(ProjectStatus).y);
defineProp(protoOf(ProjectStatus), 'ordinal', protoOf(ProjectStatus).z);
defineProp(protoOf(TimeRelativeEstimationItem), 'unit', function () {
  return this.rc();
});
defineProp(protoOf(User), 'entraSubjectId', function () {
  return this.bd();
});
defineProp(protoOf(User), 'displayName', function () {
  return this.cd();
});
defineProp(protoOf(InvariantResult), 'passed', protoOf(InvariantResult).gd);
//endregion
//region block: init
PertCalculation_instance = new PertCalculation_0();
//endregion
//region block: exports
AdditionalCostType.values = values;
AdditionalCostType.valueOf = valueOf;
defineProp(AdditionalCostType, 'ONE_TIME', AdditionalCostType_ONE_TIME_getInstance);
defineProp(AdditionalCostType, 'RECURRING', AdditionalCostType_RECURRING_getInstance);
EstimationVersionStatus.values = values_0;
EstimationVersionStatus.valueOf = valueOf_0;
defineProp(EstimationVersionStatus, 'DRAFT', EstimationVersionStatus_DRAFT_getInstance);
defineProp(EstimationVersionStatus, 'SUBMITTED', EstimationVersionStatus_SUBMITTED_getInstance);
var PertCalculation = {getInstance: PertCalculation_getInstance};
ProjectStatus.values = values_1;
ProjectStatus.valueOf = valueOf_1;
defineProp(ProjectStatus, 'ACTIVE', ProjectStatus_ACTIVE_getInstance);
defineProp(ProjectStatus, 'ARCHIVED', ProjectStatus_ARCHIVED_getInstance);
export {
  AdditionalCost as AdditionalCost,
  AdditionalCostType as AdditionalCostType,
  BaseDomain as BaseDomain,
  CalculationParameters as CalculationParameters,
  createFixedItem as createFixedItem,
  createTimeRelativeItem as createTimeRelativeItem,
  createGroup as createGroup,
  createVersion as createVersion,
  EffortDriver as EffortDriver,
  Estimation as Estimation,
  EstimationGroup as EstimationGroup,
  EstimationItem as EstimationItem,
  EstimationNode as EstimationNode,
  EstimationParameter as EstimationParameter,
  EstimationVersion as EstimationVersion,
  EstimationVersionStatus as EstimationVersionStatus,
  FixedEstimationItem as FixedEstimationItem,
  PertCalculation as PertCalculation,
  Project as Project,
  ProjectPhase as ProjectPhase,
  ProjectStatus as ProjectStatus,
  TimeRelativeEstimationItem as TimeRelativeEstimationItem,
  User as User,
  EstimationCalculator as EstimationCalculator,
  InvariantResult as InvariantResult,
};
//endregion

//# sourceMappingURL=domain.mjs.map
