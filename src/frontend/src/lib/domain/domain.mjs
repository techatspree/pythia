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
  initMetadataForCompanion1wyw17z38v6ac as initMetadataForCompanion,
  copyToArray2j022khrow2yi as copyToArray,
} from './kotlin-kotlin-stdlib.mjs';
import { KotlinLogging_instance20u19uwz7rzsk as KotlinLogging_instance } from './kotlin-logging.mjs';
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
initMetadataForCompanion(Companion);
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
  this.c7_1 = description;
  this.d7_1 = amount;
  this.e7_1 = type;
  this.f7_1 = amountPerWeek;
  this.g7_1 = phase;
  this.h7_1 = _id;
  this.i7_1 = _createdAt;
  this.j7_1 = _updatedAt;
}
protoOf(AdditionalCost).k7 = function () {
  return this.c7_1;
};
protoOf(AdditionalCost).l7 = function () {
  return this.d7_1;
};
protoOf(AdditionalCost).m7 = function () {
  return this.e7_1;
};
protoOf(AdditionalCost).n7 = function () {
  return this.f7_1;
};
protoOf(AdditionalCost).o7 = function () {
  return this.g7_1;
};
protoOf(AdditionalCost).g5 = function () {
  return this.description;
};
protoOf(AdditionalCost).h5 = function () {
  return this.amount;
};
protoOf(AdditionalCost).p7 = function () {
  return this.type;
};
protoOf(AdditionalCost).q7 = function () {
  return this.amountPerWeek;
};
protoOf(AdditionalCost).r7 = function () {
  return this.phase;
};
protoOf(AdditionalCost).s7 = function (description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt) {
  return new AdditionalCost(description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt);
};
protoOf(AdditionalCost).copy = function (description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt, $super) {
  description = description === VOID ? this.description : description;
  amount = amount === VOID ? this.amount : amount;
  type = type === VOID ? this.type : type;
  amountPerWeek = amountPerWeek === VOID ? this.amountPerWeek : amountPerWeek;
  phase = phase === VOID ? this.phase : phase;
  _id = _id === VOID ? this.h7_1 : _id;
  _createdAt = _createdAt === VOID ? this.i7_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.j7_1 : _updatedAt;
  return $super === VOID ? this.s7(description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt) : $super.s7.call(this, description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt);
};
protoOf(AdditionalCost).toString = function () {
  return 'AdditionalCost(description=' + this.description + ', amount=' + this.amount + ', type=' + this.type.toString() + ', amountPerWeek=' + this.amountPerWeek + ', phase=' + toString(this.phase) + ', _id=' + this.h7_1 + ', _createdAt=' + this.i7_1 + ', _updatedAt=' + this.j7_1 + ')';
};
protoOf(AdditionalCost).hashCode = function () {
  var result = getStringHashCode(this.description);
  result = imul(result, 31) + getNumberHashCode(this.amount) | 0;
  result = imul(result, 31) + this.type.hashCode() | 0;
  result = imul(result, 31) + getNumberHashCode(this.amountPerWeek) | 0;
  result = imul(result, 31) + (this.phase == null ? 0 : this.phase.hashCode()) | 0;
  result = imul(result, 31) + (this.h7_1 == null ? 0 : getStringHashCode(this.h7_1)) | 0;
  result = imul(result, 31) + (this.i7_1 == null ? 0 : getStringHashCode(this.i7_1)) | 0;
  result = imul(result, 31) + (this.j7_1 == null ? 0 : getStringHashCode(this.j7_1)) | 0;
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
  if (!(this.h7_1 == tmp0_other_with_cast.h7_1))
    return false;
  if (!(this.i7_1 == tmp0_other_with_cast.i7_1))
    return false;
  if (!(this.j7_1 == tmp0_other_with_cast.j7_1))
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
  this.t7_1 = id;
  this.u7_1 = createdAt;
  this.v7_1 = updatedAt;
}
protoOf(BaseDomain).w7 = function () {
  return this.t7_1;
};
protoOf(BaseDomain).x7 = function () {
  return this.u7_1;
};
protoOf(BaseDomain).y7 = function () {
  return this.v7_1;
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
protoOf(CalculationParameters).b8 = function () {
  return this.riskFactor;
};
protoOf(CalculationParameters).c8 = function () {
  return this.totalDriverFactor;
};
protoOf(CalculationParameters).d8 = function () {
  return this.dailyRate;
};
protoOf(CalculationParameters).e8 = function () {
  return this.salesSurcharge;
};
protoOf(CalculationParameters).g5 = function () {
  return this.riskFactor;
};
protoOf(CalculationParameters).h5 = function () {
  return this.totalDriverFactor;
};
protoOf(CalculationParameters).p7 = function () {
  return this.dailyRate;
};
protoOf(CalculationParameters).q7 = function () {
  return this.salesSurcharge;
};
protoOf(CalculationParameters).f8 = function (riskFactor, totalDriverFactor, dailyRate, salesSurcharge) {
  return new CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
};
protoOf(CalculationParameters).copy = function (riskFactor, totalDriverFactor, dailyRate, salesSurcharge, $super) {
  riskFactor = riskFactor === VOID ? this.riskFactor : riskFactor;
  totalDriverFactor = totalDriverFactor === VOID ? this.totalDriverFactor : totalDriverFactor;
  dailyRate = dailyRate === VOID ? this.dailyRate : dailyRate;
  salesSurcharge = salesSurcharge === VOID ? this.salesSurcharge : salesSurcharge;
  return $super === VOID ? this.f8(riskFactor, totalDriverFactor, dailyRate, salesSurcharge) : $super.f8.call(this, riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
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
  return Companion_getInstance().o5().toString();
}
function EffortDriver(description, factor, comment, _id, _createdAt, _updatedAt) {
  factor = factor === VOID ? 0.0 : factor;
  comment = comment === VOID ? '' : comment;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.j8_1 = description;
  this.k8_1 = factor;
  this.l8_1 = comment;
  this.m8_1 = _id;
  this.n8_1 = _createdAt;
  this.o8_1 = _updatedAt;
}
protoOf(EffortDriver).k7 = function () {
  return this.j8_1;
};
protoOf(EffortDriver).p8 = function () {
  return this.k8_1;
};
protoOf(EffortDriver).q8 = function () {
  return this.l8_1;
};
protoOf(EffortDriver).g5 = function () {
  return this.description;
};
protoOf(EffortDriver).h5 = function () {
  return this.factor;
};
protoOf(EffortDriver).p7 = function () {
  return this.comment;
};
protoOf(EffortDriver).r8 = function (description, factor, comment, _id, _createdAt, _updatedAt) {
  return new EffortDriver(description, factor, comment, _id, _createdAt, _updatedAt);
};
protoOf(EffortDriver).copy = function (description, factor, comment, _id, _createdAt, _updatedAt, $super) {
  description = description === VOID ? this.description : description;
  factor = factor === VOID ? this.factor : factor;
  comment = comment === VOID ? this.comment : comment;
  _id = _id === VOID ? this.m8_1 : _id;
  _createdAt = _createdAt === VOID ? this.n8_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.o8_1 : _updatedAt;
  return $super === VOID ? this.r8(description, factor, comment, _id, _createdAt, _updatedAt) : $super.r8.call(this, description, factor, comment, _id, _createdAt, _updatedAt);
};
protoOf(EffortDriver).toString = function () {
  return 'EffortDriver(description=' + this.description + ', factor=' + this.factor + ', comment=' + this.comment + ', _id=' + this.m8_1 + ', _createdAt=' + this.n8_1 + ', _updatedAt=' + this.o8_1 + ')';
};
protoOf(EffortDriver).hashCode = function () {
  var result = getStringHashCode(this.description);
  result = imul(result, 31) + getNumberHashCode(this.factor) | 0;
  result = imul(result, 31) + getStringHashCode(this.comment) | 0;
  result = imul(result, 31) + (this.m8_1 == null ? 0 : getStringHashCode(this.m8_1)) | 0;
  result = imul(result, 31) + (this.n8_1 == null ? 0 : getStringHashCode(this.n8_1)) | 0;
  result = imul(result, 31) + (this.o8_1 == null ? 0 : getStringHashCode(this.o8_1)) | 0;
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
  if (!(this.m8_1 == tmp0_other_with_cast.m8_1))
    return false;
  if (!(this.n8_1 == tmp0_other_with_cast.n8_1))
    return false;
  if (!(this.o8_1 == tmp0_other_with_cast.o8_1))
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
  this.v8_1 = offer;
  this.w8_1 = description;
  this.x8_1 = currentVersion;
  this.y8_1 = versions;
  this.z8_1 = _id;
  this.a9_1 = _createdAt;
  this.b9_1 = _updatedAt;
}
protoOf(Estimation).c9 = function () {
  return this.v8_1;
};
protoOf(Estimation).k7 = function () {
  return this.w8_1;
};
protoOf(Estimation).d9 = function () {
  return this.x8_1;
};
protoOf(Estimation).e9 = function () {
  return this.y8_1;
};
protoOf(Estimation).g5 = function () {
  return this.offer;
};
protoOf(Estimation).h5 = function () {
  return this.description;
};
protoOf(Estimation).p7 = function () {
  return this.currentVersion;
};
protoOf(Estimation).q7 = function () {
  return this.versions;
};
protoOf(Estimation).f9 = function (offer, description, currentVersion, versions, _id, _createdAt, _updatedAt) {
  return new Estimation(offer, description, currentVersion, versions, _id, _createdAt, _updatedAt);
};
protoOf(Estimation).copy = function (offer, description, currentVersion, versions, _id, _createdAt, _updatedAt, $super) {
  offer = offer === VOID ? this.offer : offer;
  description = description === VOID ? this.description : description;
  currentVersion = currentVersion === VOID ? this.currentVersion : currentVersion;
  versions = versions === VOID ? this.versions : versions;
  _id = _id === VOID ? this.z8_1 : _id;
  _createdAt = _createdAt === VOID ? this.a9_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.b9_1 : _updatedAt;
  return $super === VOID ? this.f9(offer, description, currentVersion, versions, _id, _createdAt, _updatedAt) : $super.f9.call(this, offer, description, currentVersion, versions, _id, _createdAt, _updatedAt);
};
protoOf(Estimation).toString = function () {
  return 'Estimation(offer=' + this.offer + ', description=' + this.description + ', currentVersion=' + toString(this.currentVersion) + ', versions=' + toString_0(this.versions) + ', _id=' + this.z8_1 + ', _createdAt=' + this.a9_1 + ', _updatedAt=' + this.b9_1 + ')';
};
protoOf(Estimation).hashCode = function () {
  var result = getStringHashCode(this.offer);
  result = imul(result, 31) + getStringHashCode(this.description) | 0;
  result = imul(result, 31) + (this.currentVersion == null ? 0 : this.currentVersion.hashCode()) | 0;
  result = imul(result, 31) + hashCode(this.versions) | 0;
  result = imul(result, 31) + (this.z8_1 == null ? 0 : getStringHashCode(this.z8_1)) | 0;
  result = imul(result, 31) + (this.a9_1 == null ? 0 : getStringHashCode(this.a9_1)) | 0;
  result = imul(result, 31) + (this.b9_1 == null ? 0 : getStringHashCode(this.b9_1)) | 0;
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
  if (!(this.z8_1 == tmp0_other_with_cast.z8_1))
    return false;
  if (!(this.a9_1 == tmp0_other_with_cast.a9_1))
    return false;
  if (!(this.b9_1 == tmp0_other_with_cast.b9_1))
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
  this.k9_1 = title;
  this.l9_1 = children;
  this.m9_1 = _logicalId;
  this.n9_1 = _id;
  this.o9_1 = _createdAt;
  this.p9_1 = _updatedAt;
}
protoOf(EstimationGroup).q9 = function () {
  return this.k9_1;
};
protoOf(EstimationGroup).r9 = function () {
  return this.l9_1;
};
protoOf(EstimationGroup).s9 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.f();
  while (_iterator__ex2g4s.g()) {
    var element = _iterator__ex2g4s.h();
    var tmp = sum;
    sum = tmp + element.mean;
  }
  return sum;
};
protoOf(EstimationGroup).t9 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.f();
  while (_iterator__ex2g4s.g()) {
    var element = _iterator__ex2g4s.h();
    var tmp = sum;
    sum = tmp + element.variance;
  }
  return sum;
};
protoOf(EstimationGroup).u9 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.f();
  while (_iterator__ex2g4s.g()) {
    var element = _iterator__ex2g4s.h();
    var tmp = sum;
    sum = tmp + element.riskSurcharge;
  }
  return sum;
};
protoOf(EstimationGroup).v9 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.f();
  while (_iterator__ex2g4s.g()) {
    var element = _iterator__ex2g4s.h();
    var tmp = sum;
    sum = tmp + element.driverSurcharge;
  }
  return sum;
};
protoOf(EstimationGroup).w9 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.f();
  while (_iterator__ex2g4s.g()) {
    var element = _iterator__ex2g4s.h();
    var tmp = sum;
    sum = tmp + element.offerPT;
  }
  return sum;
};
protoOf(EstimationGroup).x9 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.f();
  while (_iterator__ex2g4s.g()) {
    var element = _iterator__ex2g4s.h();
    var tmp = sum;
    sum = tmp + element.cost;
  }
  return sum;
};
protoOf(EstimationGroup).y9 = function () {
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s = this.children.f();
  while (_iterator__ex2g4s.g()) {
    var element = _iterator__ex2g4s.h();
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
  var _iterator__ex2g4s = this_0.f();
  while (_iterator__ex2g4s.g()) {
    var item = _iterator__ex2g4s.h();
    var tmp$ret$0 = item.withCalculationParameters(params);
    destination.q(tmp$ret$0);
  }
  return this.copy(VOID, destination);
};
protoOf(EstimationGroup).g5 = function () {
  return this.title;
};
protoOf(EstimationGroup).h5 = function () {
  return this.children;
};
protoOf(EstimationGroup).z9 = function (title, children, _logicalId, _id, _createdAt, _updatedAt) {
  return new EstimationGroup(title, children, _logicalId, _id, _createdAt, _updatedAt);
};
protoOf(EstimationGroup).copy = function (title, children, _logicalId, _id, _createdAt, _updatedAt, $super) {
  title = title === VOID ? this.title : title;
  children = children === VOID ? this.children : children;
  _logicalId = _logicalId === VOID ? this.m9_1 : _logicalId;
  _id = _id === VOID ? this.n9_1 : _id;
  _createdAt = _createdAt === VOID ? this.o9_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.p9_1 : _updatedAt;
  return $super === VOID ? this.z9(title, children, _logicalId, _id, _createdAt, _updatedAt) : $super.z9.call(this, title, children, _logicalId, _id, _createdAt, _updatedAt);
};
protoOf(EstimationGroup).toString = function () {
  return 'EstimationGroup(title=' + this.title + ', children=' + toString_0(this.children) + ', _logicalId=' + this.m9_1 + ', _id=' + this.n9_1 + ', _createdAt=' + this.o9_1 + ', _updatedAt=' + this.p9_1 + ')';
};
protoOf(EstimationGroup).hashCode = function () {
  var result = getStringHashCode(this.title);
  result = imul(result, 31) + hashCode(this.children) | 0;
  result = imul(result, 31) + getStringHashCode(this.m9_1) | 0;
  result = imul(result, 31) + (this.n9_1 == null ? 0 : getStringHashCode(this.n9_1)) | 0;
  result = imul(result, 31) + (this.o9_1 == null ? 0 : getStringHashCode(this.o9_1)) | 0;
  result = imul(result, 31) + (this.p9_1 == null ? 0 : getStringHashCode(this.p9_1)) | 0;
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
  if (!(this.m9_1 === tmp0_other_with_cast.m9_1))
    return false;
  if (!(this.n9_1 == tmp0_other_with_cast.n9_1))
    return false;
  if (!(this.o9_1 == tmp0_other_with_cast.o9_1))
    return false;
  if (!(this.p9_1 == tmp0_other_with_cast.p9_1))
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
  this.ja_1 = description;
  this.ka_1 = code;
  this.la_1 = minEffort;
  this.ma_1 = expectedEffort;
  this.na_1 = maxEffort;
  this.oa_1 = assumptions;
  this.pa_1 = phase;
  this.qa_1 = calculationParameters;
}
protoOf(EstimationItem).k7 = function () {
  return this.ja_1;
};
protoOf(EstimationItem).ra = function () {
  return this.ka_1;
};
protoOf(EstimationItem).sa = function () {
  return this.la_1;
};
protoOf(EstimationItem).ta = function () {
  return this.ma_1;
};
protoOf(EstimationItem).ua = function () {
  return this.na_1;
};
protoOf(EstimationItem).va = function () {
  return this.oa_1;
};
protoOf(EstimationItem).o7 = function () {
  return this.pa_1;
};
protoOf(EstimationItem).wa = function () {
  return this.qa_1;
};
protoOf(EstimationItem).s9 = function () {
  return PertCalculation_instance.mean(this.minEffort, this.expectedEffort, this.maxEffort);
};
protoOf(EstimationItem).t9 = function () {
  return PertCalculation_instance.variance(this.minEffort, this.maxEffort);
};
protoOf(EstimationItem).u9 = function () {
  return this.mean * this.calculationParameters.riskFactor;
};
protoOf(EstimationItem).v9 = function () {
  return this.mean * this.calculationParameters.totalDriverFactor;
};
protoOf(EstimationItem).w9 = function () {
  return this.mean + this.riskSurcharge + this.driverSurcharge;
};
protoOf(EstimationItem).x9 = function () {
  return this.offerPT * this.calculationParameters.dailyRate;
};
protoOf(EstimationItem).y9 = function () {
  return this.cost * (1 + this.calculationParameters.salesSurcharge);
};
function EstimationNode(logicalId, id, createdAt, updatedAt) {
  id = id === VOID ? null : id;
  createdAt = createdAt === VOID ? null : createdAt;
  updatedAt = updatedAt === VOID ? null : updatedAt;
  BaseDomain.call(this, id, createdAt, updatedAt);
  this.da_1 = logicalId;
}
protoOf(EstimationNode).ea = function () {
  return this.da_1;
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
  this.ab_1 = name;
  this.bb_1 = value;
  this.cb_1 = comment;
  this.db_1 = _id;
  this.eb_1 = _createdAt;
  this.fb_1 = _updatedAt;
}
protoOf(EstimationParameter).c1 = function () {
  return this.ab_1;
};
protoOf(EstimationParameter).gb = function () {
  return this.bb_1;
};
protoOf(EstimationParameter).q8 = function () {
  return this.cb_1;
};
protoOf(EstimationParameter).g5 = function () {
  return this.name;
};
protoOf(EstimationParameter).h5 = function () {
  return this.value;
};
protoOf(EstimationParameter).p7 = function () {
  return this.comment;
};
protoOf(EstimationParameter).r8 = function (name, value, comment, _id, _createdAt, _updatedAt) {
  return new EstimationParameter(name, value, comment, _id, _createdAt, _updatedAt);
};
protoOf(EstimationParameter).copy = function (name, value, comment, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  value = value === VOID ? this.value : value;
  comment = comment === VOID ? this.comment : comment;
  _id = _id === VOID ? this.db_1 : _id;
  _createdAt = _createdAt === VOID ? this.eb_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.fb_1 : _updatedAt;
  return $super === VOID ? this.r8(name, value, comment, _id, _createdAt, _updatedAt) : $super.r8.call(this, name, value, comment, _id, _createdAt, _updatedAt);
};
protoOf(EstimationParameter).toString = function () {
  return 'EstimationParameter(name=' + this.name + ', value=' + this.value + ', comment=' + this.comment + ', _id=' + this.db_1 + ', _createdAt=' + this.eb_1 + ', _updatedAt=' + this.fb_1 + ')';
};
protoOf(EstimationParameter).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getNumberHashCode(this.value) | 0;
  result = imul(result, 31) + getStringHashCode(this.comment) | 0;
  result = imul(result, 31) + (this.db_1 == null ? 0 : getStringHashCode(this.db_1)) | 0;
  result = imul(result, 31) + (this.eb_1 == null ? 0 : getStringHashCode(this.eb_1)) | 0;
  result = imul(result, 31) + (this.fb_1 == null ? 0 : getStringHashCode(this.fb_1)) | 0;
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
  if (!(this.db_1 == tmp0_other_with_cast.db_1))
    return false;
  if (!(this.eb_1 == tmp0_other_with_cast.eb_1))
    return false;
  if (!(this.fb_1 == tmp0_other_with_cast.fb_1))
    return false;
  return true;
};
function get_logger() {
  _init_properties_EstimationVersion_kt__varg2t();
  return logger;
}
var logger;
function EstimationVersion$calculate$lambda($leaves, $totalMean, $newTotalEffort) {
  return function () {
    return 'calculate(): ' + $leaves.i() + ' leaves, totalMean=' + $totalMean + ', totalEffort=' + $newTotalEffort;
  };
}
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
  this.kb_1 = versionNumber;
  this.lb_1 = status;
  this.mb_1 = createdBy;
  this.nb_1 = totalEffort;
  this.ob_1 = notes;
  this.pb_1 = parameters;
  this.qb_1 = effortDrivers;
  this.rb_1 = phases;
  this.sb_1 = additionalCosts;
  this.tb_1 = roots;
  this.ub_1 = _id;
  this.vb_1 = _createdAt;
  this.wb_1 = _updatedAt;
}
protoOf(EstimationVersion).xb = function () {
  return this.kb_1;
};
protoOf(EstimationVersion).yb = function () {
  return this.lb_1;
};
protoOf(EstimationVersion).zb = function () {
  return this.mb_1;
};
protoOf(EstimationVersion).ac = function () {
  return this.nb_1;
};
protoOf(EstimationVersion).bc = function () {
  return this.ob_1;
};
protoOf(EstimationVersion).cc = function () {
  return this.pb_1;
};
protoOf(EstimationVersion).dc = function () {
  return this.qb_1;
};
protoOf(EstimationVersion).ec = function () {
  return this.rb_1;
};
protoOf(EstimationVersion).fc = function () {
  return this.sb_1;
};
protoOf(EstimationVersion).gc = function () {
  return this.tb_1;
};
protoOf(EstimationVersion).parameterValue = function (name) {
  // Inline function 'kotlin.collections.find' call
  var tmp0 = this.parameters;
  var tmp$ret$1;
  $l$block: {
    // Inline function 'kotlin.collections.firstOrNull' call
    var _iterator__ex2g4s = tmp0.f();
    while (_iterator__ex2g4s.g()) {
      var element = _iterator__ex2g4s.h();
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
  var _iterator__ex2g4s = this.effortDrivers.f();
  while (_iterator__ex2g4s.g()) {
    var element = _iterator__ex2g4s.h();
    var tmp = sum;
    sum = tmp + element.factor;
  }
  var totalDriverFactor = sum;
  // Inline function 'kotlin.collections.flatMap' call
  var tmp0 = this.roots;
  // Inline function 'kotlin.collections.flatMapTo' call
  var destination = ArrayList_init_$Create$_0();
  var _iterator__ex2g4s_0 = tmp0.f();
  while (_iterator__ex2g4s_0.g()) {
    var element_0 = _iterator__ex2g4s_0.h();
    var list = toList_0(leaves(element_0));
    addAll(destination, list);
  }
  var leaves_0 = destination;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_0 = 0;
  var _iterator__ex2g4s_1 = leaves_0.f();
  while (_iterator__ex2g4s_1.g()) {
    var element_1 = _iterator__ex2g4s_1.h();
    var tmp_0 = sum_0;
    sum_0 = tmp_0 + element_1.variance;
  }
  var totalVariance = sum_0;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_1 = 0;
  var _iterator__ex2g4s_2 = leaves_0.f();
  while (_iterator__ex2g4s_2.g()) {
    var element_2 = _iterator__ex2g4s_2.h();
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
  var _iterator__ex2g4s_3 = this_0.f();
  while (_iterator__ex2g4s_3.g()) {
    var item = _iterator__ex2g4s_3.h();
    var tmp$ret$9 = item.withCalculationParameters(params);
    destination_0.q(tmp$ret$9);
  }
  var newRoots = destination_0;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_2 = 0;
  var _iterator__ex2g4s_4 = newRoots.f();
  while (_iterator__ex2g4s_4.g()) {
    var element_3 = _iterator__ex2g4s_4.h();
    var tmp_2 = sum_2;
    sum_2 = tmp_2 + element_3.offerPT;
  }
  var newTotalEffort = sum_2;
  var tmp_3 = get_logger();
  tmp_3.y5(EstimationVersion$calculate$lambda(leaves_0, totalMean, newTotalEffort));
  return this.copy(VOID, VOID, VOID, newTotalEffort, VOID, VOID, VOID, VOID, VOID, newRoots);
};
protoOf(EstimationVersion).g5 = function () {
  return this.versionNumber;
};
protoOf(EstimationVersion).h5 = function () {
  return this.status;
};
protoOf(EstimationVersion).p7 = function () {
  return this.createdBy;
};
protoOf(EstimationVersion).q7 = function () {
  return this.totalEffort;
};
protoOf(EstimationVersion).r7 = function () {
  return this.notes;
};
protoOf(EstimationVersion).hc = function () {
  return this.parameters;
};
protoOf(EstimationVersion).ic = function () {
  return this.effortDrivers;
};
protoOf(EstimationVersion).jc = function () {
  return this.phases;
};
protoOf(EstimationVersion).kc = function () {
  return this.additionalCosts;
};
protoOf(EstimationVersion).lc = function () {
  return this.roots;
};
protoOf(EstimationVersion).mc = function (versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt) {
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
  _id = _id === VOID ? this.ub_1 : _id;
  _createdAt = _createdAt === VOID ? this.vb_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.wb_1 : _updatedAt;
  return $super === VOID ? this.mc(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt) : $super.mc.call(this, versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt);
};
protoOf(EstimationVersion).toString = function () {
  return 'EstimationVersion(versionNumber=' + this.versionNumber + ', status=' + this.status.toString() + ', createdBy=' + toString(this.createdBy) + ', totalEffort=' + this.totalEffort + ', notes=' + this.notes + ', parameters=' + toString_0(this.parameters) + ', effortDrivers=' + toString_0(this.effortDrivers) + ', phases=' + toString_0(this.phases) + ', additionalCosts=' + toString_0(this.additionalCosts) + ', roots=' + toString_0(this.roots) + ', _id=' + this.ub_1 + ', _createdAt=' + this.vb_1 + ', _updatedAt=' + this.wb_1 + ')';
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
  result = imul(result, 31) + (this.ub_1 == null ? 0 : getStringHashCode(this.ub_1)) | 0;
  result = imul(result, 31) + (this.vb_1 == null ? 0 : getStringHashCode(this.vb_1)) | 0;
  result = imul(result, 31) + (this.wb_1 == null ? 0 : getStringHashCode(this.wb_1)) | 0;
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
  if (!(this.ub_1 == tmp0_other_with_cast.ub_1))
    return false;
  if (!(this.vb_1 == tmp0_other_with_cast.vb_1))
    return false;
  if (!(this.wb_1 == tmp0_other_with_cast.wb_1))
    return false;
  return true;
};
function logger$lambda() {
  _init_properties_EstimationVersion_kt__varg2t();
  return Unit_instance;
}
var properties_initialized_EstimationVersion_kt_p0j46b;
function _init_properties_EstimationVersion_kt__varg2t() {
  if (!properties_initialized_EstimationVersion_kt_p0j46b) {
    properties_initialized_EstimationVersion_kt_p0j46b = true;
    var tmp = KotlinLogging_instance;
    logger = tmp.b6(logger$lambda);
  }
}
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
  this.bd_1 = _description;
  this.cd_1 = _code;
  this.dd_1 = _minEffort;
  this.ed_1 = _expectedEffort;
  this.fd_1 = _maxEffort;
  this.gd_1 = _assumptions;
  this.hd_1 = _phase;
  this.jd_1 = _logicalId;
  this.kd_1 = _calculationParameters;
  this.ld_1 = _id;
  this.md_1 = _createdAt;
  this.nd_1 = _updatedAt;
}
protoOf(FixedEstimationItem).withCalculationParameters = function (params) {
  return this.copy(VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, params);
};
protoOf(FixedEstimationItem).od = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  return new FixedEstimationItem(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(FixedEstimationItem).copy = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
  _description = _description === VOID ? this.bd_1 : _description;
  _code = _code === VOID ? this.cd_1 : _code;
  _minEffort = _minEffort === VOID ? this.dd_1 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? this.ed_1 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? this.fd_1 : _maxEffort;
  _assumptions = _assumptions === VOID ? this.gd_1 : _assumptions;
  _phase = _phase === VOID ? this.hd_1 : _phase;
  _logicalId = _logicalId === VOID ? this.jd_1 : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? this.kd_1 : _calculationParameters;
  _id = _id === VOID ? this.ld_1 : _id;
  _createdAt = _createdAt === VOID ? this.md_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.nd_1 : _updatedAt;
  return $super === VOID ? this.od(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.od.call(this, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(FixedEstimationItem).toString = function () {
  return 'FixedEstimationItem(_description=' + this.bd_1 + ', _code=' + this.cd_1 + ', _minEffort=' + this.dd_1 + ', _expectedEffort=' + this.ed_1 + ', _maxEffort=' + this.fd_1 + ', _assumptions=' + this.gd_1 + ', _phase=' + toString(this.hd_1) + ', _logicalId=' + this.jd_1 + ', _calculationParameters=' + this.kd_1.toString() + ', _id=' + this.ld_1 + ', _createdAt=' + this.md_1 + ', _updatedAt=' + this.nd_1 + ')';
};
protoOf(FixedEstimationItem).hashCode = function () {
  var result = getStringHashCode(this.bd_1);
  result = imul(result, 31) + getStringHashCode(this.cd_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.dd_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.ed_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.fd_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.gd_1) | 0;
  result = imul(result, 31) + (this.hd_1 == null ? 0 : this.hd_1.hashCode()) | 0;
  result = imul(result, 31) + getStringHashCode(this.jd_1) | 0;
  result = imul(result, 31) + this.kd_1.hashCode() | 0;
  result = imul(result, 31) + (this.ld_1 == null ? 0 : getStringHashCode(this.ld_1)) | 0;
  result = imul(result, 31) + (this.md_1 == null ? 0 : getStringHashCode(this.md_1)) | 0;
  result = imul(result, 31) + (this.nd_1 == null ? 0 : getStringHashCode(this.nd_1)) | 0;
  return result;
};
protoOf(FixedEstimationItem).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof FixedEstimationItem))
    return false;
  var tmp0_other_with_cast = other instanceof FixedEstimationItem ? other : THROW_CCE();
  if (!(this.bd_1 === tmp0_other_with_cast.bd_1))
    return false;
  if (!(this.cd_1 === tmp0_other_with_cast.cd_1))
    return false;
  if (!equals(this.dd_1, tmp0_other_with_cast.dd_1))
    return false;
  if (!equals(this.ed_1, tmp0_other_with_cast.ed_1))
    return false;
  if (!equals(this.fd_1, tmp0_other_with_cast.fd_1))
    return false;
  if (!(this.gd_1 === tmp0_other_with_cast.gd_1))
    return false;
  if (!equals(this.hd_1, tmp0_other_with_cast.hd_1))
    return false;
  if (!(this.jd_1 === tmp0_other_with_cast.jd_1))
    return false;
  if (!this.kd_1.equals(tmp0_other_with_cast.kd_1))
    return false;
  if (!(this.ld_1 == tmp0_other_with_cast.ld_1))
    return false;
  if (!(this.md_1 == tmp0_other_with_cast.md_1))
    return false;
  if (!(this.nd_1 == tmp0_other_with_cast.nd_1))
    return false;
  return true;
};
function PertCalculation_0() {
  this.pd_1 = 4.0;
  this.qd_1 = 6.0;
}
protoOf(PertCalculation_0).mean = function (min, expected, max) {
  return (min + 4.0 * expected + max) / 6.0;
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
  this.ud_1 = name;
  this.vd_1 = description;
  this.wd_1 = client;
  this.xd_1 = status;
  this.yd_1 = owner;
  this.zd_1 = _id;
  this.ae_1 = _createdAt;
  this.be_1 = _updatedAt;
}
protoOf(Project).c1 = function () {
  return this.ud_1;
};
protoOf(Project).k7 = function () {
  return this.vd_1;
};
protoOf(Project).ce = function () {
  return this.wd_1;
};
protoOf(Project).yb = function () {
  return this.xd_1;
};
protoOf(Project).de = function () {
  return this.yd_1;
};
protoOf(Project).g5 = function () {
  return this.name;
};
protoOf(Project).h5 = function () {
  return this.description;
};
protoOf(Project).p7 = function () {
  return this.client;
};
protoOf(Project).q7 = function () {
  return this.status;
};
protoOf(Project).r7 = function () {
  return this.owner;
};
protoOf(Project).ee = function (name, description, client, status, owner, _id, _createdAt, _updatedAt) {
  return new Project(name, description, client, status, owner, _id, _createdAt, _updatedAt);
};
protoOf(Project).copy = function (name, description, client, status, owner, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  description = description === VOID ? this.description : description;
  client = client === VOID ? this.client : client;
  status = status === VOID ? this.status : status;
  owner = owner === VOID ? this.owner : owner;
  _id = _id === VOID ? this.zd_1 : _id;
  _createdAt = _createdAt === VOID ? this.ae_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.be_1 : _updatedAt;
  return $super === VOID ? this.ee(name, description, client, status, owner, _id, _createdAt, _updatedAt) : $super.ee.call(this, name, description, client, status, owner, _id, _createdAt, _updatedAt);
};
protoOf(Project).toString = function () {
  return 'Project(name=' + this.name + ', description=' + this.description + ', client=' + this.client + ', status=' + this.status.toString() + ', owner=' + toString(this.owner) + ', _id=' + this.zd_1 + ', _createdAt=' + this.ae_1 + ', _updatedAt=' + this.be_1 + ')';
};
protoOf(Project).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getStringHashCode(this.description) | 0;
  result = imul(result, 31) + getStringHashCode(this.client) | 0;
  result = imul(result, 31) + this.status.hashCode() | 0;
  result = imul(result, 31) + (this.owner == null ? 0 : this.owner.hashCode()) | 0;
  result = imul(result, 31) + (this.zd_1 == null ? 0 : getStringHashCode(this.zd_1)) | 0;
  result = imul(result, 31) + (this.ae_1 == null ? 0 : getStringHashCode(this.ae_1)) | 0;
  result = imul(result, 31) + (this.be_1 == null ? 0 : getStringHashCode(this.be_1)) | 0;
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
  if (!(this.zd_1 == tmp0_other_with_cast.zd_1))
    return false;
  if (!(this.ae_1 == tmp0_other_with_cast.ae_1))
    return false;
  if (!(this.be_1 == tmp0_other_with_cast.be_1))
    return false;
  return true;
};
function ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
  durationWeeks = durationWeeks === VOID ? 0.0 : durationWeeks;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.ie_1 = name;
  this.je_1 = abbreviation;
  this.ke_1 = durationWeeks;
  this.le_1 = _id;
  this.me_1 = _createdAt;
  this.ne_1 = _updatedAt;
}
protoOf(ProjectPhase).c1 = function () {
  return this.ie_1;
};
protoOf(ProjectPhase).oe = function () {
  return this.je_1;
};
protoOf(ProjectPhase).pe = function () {
  return this.ke_1;
};
protoOf(ProjectPhase).g5 = function () {
  return this.name;
};
protoOf(ProjectPhase).h5 = function () {
  return this.abbreviation;
};
protoOf(ProjectPhase).p7 = function () {
  return this.durationWeeks;
};
protoOf(ProjectPhase).qe = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
  return new ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
};
protoOf(ProjectPhase).copy = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  abbreviation = abbreviation === VOID ? this.abbreviation : abbreviation;
  durationWeeks = durationWeeks === VOID ? this.durationWeeks : durationWeeks;
  _id = _id === VOID ? this.le_1 : _id;
  _createdAt = _createdAt === VOID ? this.me_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.ne_1 : _updatedAt;
  return $super === VOID ? this.qe(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) : $super.qe.call(this, name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
};
protoOf(ProjectPhase).toString = function () {
  return 'ProjectPhase(name=' + this.name + ', abbreviation=' + this.abbreviation + ', durationWeeks=' + this.durationWeeks + ', _id=' + this.le_1 + ', _createdAt=' + this.me_1 + ', _updatedAt=' + this.ne_1 + ')';
};
protoOf(ProjectPhase).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getStringHashCode(this.abbreviation) | 0;
  result = imul(result, 31) + getNumberHashCode(this.durationWeeks) | 0;
  result = imul(result, 31) + (this.le_1 == null ? 0 : getStringHashCode(this.le_1)) | 0;
  result = imul(result, 31) + (this.me_1 == null ? 0 : getStringHashCode(this.me_1)) | 0;
  result = imul(result, 31) + (this.ne_1 == null ? 0 : getStringHashCode(this.ne_1)) | 0;
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
  if (!(this.le_1 == tmp0_other_with_cast.le_1))
    return false;
  if (!(this.me_1 == tmp0_other_with_cast.me_1))
    return false;
  if (!(this.ne_1 == tmp0_other_with_cast.ne_1))
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
  this.ff_1 = unit;
  this.gf_1 = _description;
  this.hf_1 = _code;
  this.if_1 = _minEffort;
  this.jf_1 = _expectedEffort;
  this.kf_1 = _maxEffort;
  this.lf_1 = _assumptions;
  this.mf_1 = _phase;
  this.nf_1 = _logicalId;
  this.of_1 = _calculationParameters;
  this.pf_1 = _id;
  this.qf_1 = _createdAt;
  this.rf_1 = _updatedAt;
}
protoOf(TimeRelativeEstimationItem).sf = function () {
  return this.ff_1;
};
protoOf(TimeRelativeEstimationItem).s9 = function () {
  var tmp = PertCalculation_instance.mean(this.minEffort, this.expectedEffort, this.maxEffort);
  var tmp0_safe_receiver = this.phase;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.durationWeeks;
  return tmp * (tmp1_elvis_lhs == null ? 0.0 : tmp1_elvis_lhs);
};
protoOf(TimeRelativeEstimationItem).t9 = function () {
  var tmp0_safe_receiver = this.phase;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.durationWeeks;
  var d = tmp1_elvis_lhs == null ? 0.0 : tmp1_elvis_lhs;
  return PertCalculation_instance.variance(this.minEffort, this.maxEffort) * d * d;
};
protoOf(TimeRelativeEstimationItem).withCalculationParameters = function (params) {
  return this.copy(VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, params);
};
protoOf(TimeRelativeEstimationItem).g5 = function () {
  return this.unit;
};
protoOf(TimeRelativeEstimationItem).tf = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  return new TimeRelativeEstimationItem(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(TimeRelativeEstimationItem).copy = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
  unit = unit === VOID ? this.unit : unit;
  _description = _description === VOID ? this.gf_1 : _description;
  _code = _code === VOID ? this.hf_1 : _code;
  _minEffort = _minEffort === VOID ? this.if_1 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? this.jf_1 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? this.kf_1 : _maxEffort;
  _assumptions = _assumptions === VOID ? this.lf_1 : _assumptions;
  _phase = _phase === VOID ? this.mf_1 : _phase;
  _logicalId = _logicalId === VOID ? this.nf_1 : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? this.of_1 : _calculationParameters;
  _id = _id === VOID ? this.pf_1 : _id;
  _createdAt = _createdAt === VOID ? this.qf_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.rf_1 : _updatedAt;
  return $super === VOID ? this.tf(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.tf.call(this, unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(TimeRelativeEstimationItem).toString = function () {
  return 'TimeRelativeEstimationItem(unit=' + this.unit + ', _description=' + this.gf_1 + ', _code=' + this.hf_1 + ', _minEffort=' + this.if_1 + ', _expectedEffort=' + this.jf_1 + ', _maxEffort=' + this.kf_1 + ', _assumptions=' + this.lf_1 + ', _phase=' + toString(this.mf_1) + ', _logicalId=' + this.nf_1 + ', _calculationParameters=' + this.of_1.toString() + ', _id=' + this.pf_1 + ', _createdAt=' + this.qf_1 + ', _updatedAt=' + this.rf_1 + ')';
};
protoOf(TimeRelativeEstimationItem).hashCode = function () {
  var result = getStringHashCode(this.unit);
  result = imul(result, 31) + getStringHashCode(this.gf_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.hf_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.if_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.jf_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.kf_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.lf_1) | 0;
  result = imul(result, 31) + (this.mf_1 == null ? 0 : this.mf_1.hashCode()) | 0;
  result = imul(result, 31) + getStringHashCode(this.nf_1) | 0;
  result = imul(result, 31) + this.of_1.hashCode() | 0;
  result = imul(result, 31) + (this.pf_1 == null ? 0 : getStringHashCode(this.pf_1)) | 0;
  result = imul(result, 31) + (this.qf_1 == null ? 0 : getStringHashCode(this.qf_1)) | 0;
  result = imul(result, 31) + (this.rf_1 == null ? 0 : getStringHashCode(this.rf_1)) | 0;
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
  if (!(this.gf_1 === tmp0_other_with_cast.gf_1))
    return false;
  if (!(this.hf_1 === tmp0_other_with_cast.hf_1))
    return false;
  if (!equals(this.if_1, tmp0_other_with_cast.if_1))
    return false;
  if (!equals(this.jf_1, tmp0_other_with_cast.jf_1))
    return false;
  if (!equals(this.kf_1, tmp0_other_with_cast.kf_1))
    return false;
  if (!(this.lf_1 === tmp0_other_with_cast.lf_1))
    return false;
  if (!equals(this.mf_1, tmp0_other_with_cast.mf_1))
    return false;
  if (!(this.nf_1 === tmp0_other_with_cast.nf_1))
    return false;
  if (!this.of_1.equals(tmp0_other_with_cast.of_1))
    return false;
  if (!(this.pf_1 == tmp0_other_with_cast.pf_1))
    return false;
  if (!(this.qf_1 == tmp0_other_with_cast.qf_1))
    return false;
  if (!(this.rf_1 == tmp0_other_with_cast.rf_1))
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
  this.xf_1 = entraSubjectId;
  this.yf_1 = displayName;
  this.zf_1 = _id;
  this.ag_1 = _createdAt;
  this.bg_1 = _updatedAt;
}
protoOf(User).cg = function () {
  return this.xf_1;
};
protoOf(User).dg = function () {
  return this.yf_1;
};
protoOf(User).g5 = function () {
  return this.entraSubjectId;
};
protoOf(User).h5 = function () {
  return this.displayName;
};
protoOf(User).eg = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt) {
  return new User(entraSubjectId, displayName, _id, _createdAt, _updatedAt);
};
protoOf(User).copy = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt, $super) {
  entraSubjectId = entraSubjectId === VOID ? this.entraSubjectId : entraSubjectId;
  displayName = displayName === VOID ? this.displayName : displayName;
  _id = _id === VOID ? this.zf_1 : _id;
  _createdAt = _createdAt === VOID ? this.ag_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.bg_1 : _updatedAt;
  return $super === VOID ? this.eg(entraSubjectId, displayName, _id, _createdAt, _updatedAt) : $super.eg.call(this, entraSubjectId, displayName, _id, _createdAt, _updatedAt);
};
protoOf(User).toString = function () {
  return 'User(entraSubjectId=' + this.entraSubjectId + ', displayName=' + this.displayName + ', _id=' + this.zf_1 + ', _createdAt=' + this.ag_1 + ', _updatedAt=' + this.bg_1 + ')';
};
protoOf(User).hashCode = function () {
  var result = this.entraSubjectId == null ? 0 : getStringHashCode(this.entraSubjectId);
  result = imul(result, 31) + getStringHashCode(this.displayName) | 0;
  result = imul(result, 31) + (this.zf_1 == null ? 0 : getStringHashCode(this.zf_1)) | 0;
  result = imul(result, 31) + (this.ag_1 == null ? 0 : getStringHashCode(this.ag_1)) | 0;
  result = imul(result, 31) + (this.bg_1 == null ? 0 : getStringHashCode(this.bg_1)) | 0;
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
  if (!(this.zf_1 == tmp0_other_with_cast.zf_1))
    return false;
  if (!(this.ag_1 == tmp0_other_with_cast.ag_1))
    return false;
  if (!(this.bg_1 == tmp0_other_with_cast.bg_1))
    return false;
  return true;
};
function Companion() {
  this.fg_1 = 0.2;
}
var Companion_instance;
function Companion_getInstance_0() {
  return Companion_instance;
}
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
  var _iterator__ex2g4s = tmp0.f();
  while (_iterator__ex2g4s.g()) {
    var element = _iterator__ex2g4s.h();
    var list = toList_0(leaves(element));
    addAll(destination, list);
  }
  var allItems = destination;
  var tolerance = 0.2;
  // Inline function 'kotlin.collections.sumOf' call
  var sum = 0;
  var _iterator__ex2g4s_0 = allItems.f();
  while (_iterator__ex2g4s_0.g()) {
    var element_0 = _iterator__ex2g4s_0.h();
    var tmp = sum;
    sum = tmp + element_0.offerPT;
  }
  var totalOfferPT = sum;
  results.q(new InvariantResult('Gesamtaufwand = Summe aller AngebotsPT', version.totalEffort - totalOfferPT, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_0 = 0;
  var _iterator__ex2g4s_1 = allItems.f();
  while (_iterator__ex2g4s_1.g()) {
    var element_1 = _iterator__ex2g4s_1.h();
    var tmp_0 = sum_0;
    sum_0 = tmp_0 + element_1.mean;
  }
  var totalMean = sum_0;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_1 = 0;
  var _iterator__ex2g4s_2 = allItems.f();
  while (_iterator__ex2g4s_2.g()) {
    var element_2 = _iterator__ex2g4s_2.h();
    var tmp_1 = sum_1;
    sum_1 = tmp_1 + element_2.variance;
  }
  var totalVariance = sum_1;
  var tmp0_elvis_lhs = version.parameterValue('Standardabweichungsfaktor');
  var stdDevFactor = tmp0_elvis_lhs == null ? 2.0 : tmp0_elvis_lhs;
  // Inline function 'kotlin.collections.sumOf' call
  var sum_2 = 0;
  var _iterator__ex2g4s_3 = version.effortDrivers.f();
  while (_iterator__ex2g4s_3.g()) {
    var element_3 = _iterator__ex2g4s_3.h();
    var tmp_2 = sum_2;
    sum_2 = tmp_2 + element_3.factor;
  }
  var totalDriverFactor = sum_2;
  // Inline function 'kotlin.math.sqrt' call
  var calculatedTotal = totalMean + Math.sqrt(totalVariance) * stdDevFactor + totalMean * totalDriverFactor;
  results.q(new InvariantResult('Summe mit Risiko im PSP = Summe bei Berechnung', totalOfferPT - calculatedTotal, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_3 = 0;
  var _iterator__ex2g4s_4 = version.roots.f();
  while (_iterator__ex2g4s_4.g()) {
    var element_4 = _iterator__ex2g4s_4.h();
    var tmp_3 = sum_3;
    sum_3 = tmp_3 + element_4.offerPT;
  }
  var sumByRoots = sum_3;
  results.q(new InvariantResult('Summe der Wurzeln = Summe der Bl\xE4tter (Akkumulation konsistent)', sumByRoots - totalOfferPT, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_4 = 0;
  var _iterator__ex2g4s_5 = allItems.f();
  while (_iterator__ex2g4s_5.g()) {
    var element_5 = _iterator__ex2g4s_5.h();
    var tmp_4 = sum_4;
    sum_4 = tmp_4 + element_5.cost;
  }
  var totalCost = sum_4;
  var tmp1_elvis_lhs = version.parameterValue('Tagessatz');
  var dailyRate = tmp1_elvis_lhs == null ? 800.0 : tmp1_elvis_lhs;
  var costFromEffort = totalOfferPT * dailyRate;
  results.q(new InvariantResult('Kosten im PSP = Kosten in der Paket\xFCbersicht', totalCost - costFromEffort, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_5 = 0;
  var _iterator__ex2g4s_6 = version.roots.f();
  while (_iterator__ex2g4s_6.g()) {
    var element_6 = _iterator__ex2g4s_6.h();
    var tmp_5 = sum_5;
    sum_5 = tmp_5 + element_6.variance;
  }
  var varianceByRoots = sum_5;
  results.q(new InvariantResult('Varianzakkumulation an der Wurzel = Summe der Bl\xE4tter-Varianzen', varianceByRoots - totalVariance, tolerance));
  // Inline function 'kotlin.collections.toTypedArray' call
  return copyToArray(results);
};
function InvariantResult(description, difference, tolerance) {
  this.description = description;
  this.difference = difference;
  this.tolerance = tolerance;
}
protoOf(InvariantResult).k7 = function () {
  return this.description;
};
protoOf(InvariantResult).gg = function () {
  return this.difference;
};
protoOf(InvariantResult).hg = function () {
  return this.tolerance;
};
protoOf(InvariantResult).ig = function () {
  // Inline function 'kotlin.math.abs' call
  var x = this.difference;
  return Math.abs(x) <= this.tolerance;
};
protoOf(InvariantResult).g5 = function () {
  return this.description;
};
protoOf(InvariantResult).h5 = function () {
  return this.difference;
};
protoOf(InvariantResult).p7 = function () {
  return this.tolerance;
};
protoOf(InvariantResult).jg = function (description, difference, tolerance) {
  return new InvariantResult(description, difference, tolerance);
};
protoOf(InvariantResult).copy = function (description, difference, tolerance, $super) {
  description = description === VOID ? this.description : description;
  difference = difference === VOID ? this.difference : difference;
  tolerance = tolerance === VOID ? this.tolerance : tolerance;
  return $super === VOID ? this.jg(description, difference, tolerance) : $super.jg.call(this, description, difference, tolerance);
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
  return this.w7();
});
defineProp(protoOf(BaseDomain), 'createdAt', function () {
  return this.x7();
});
defineProp(protoOf(BaseDomain), 'updatedAt', function () {
  return this.y7();
});
defineProp(protoOf(AdditionalCost), 'description', function () {
  return this.k7();
});
defineProp(protoOf(AdditionalCost), 'amount', function () {
  return this.l7();
});
defineProp(protoOf(AdditionalCost), 'type', function () {
  return this.m7();
});
defineProp(protoOf(AdditionalCost), 'amountPerWeek', function () {
  return this.n7();
});
defineProp(protoOf(AdditionalCost), 'phase', function () {
  return this.o7();
});
defineProp(protoOf(AdditionalCostType), 'name', protoOf(AdditionalCostType).c1);
defineProp(protoOf(AdditionalCostType), 'ordinal', protoOf(AdditionalCostType).d1);
defineProp(protoOf(EffortDriver), 'description', function () {
  return this.k7();
});
defineProp(protoOf(EffortDriver), 'factor', function () {
  return this.p8();
});
defineProp(protoOf(EffortDriver), 'comment', function () {
  return this.q8();
});
defineProp(protoOf(Estimation), 'offer', function () {
  return this.c9();
});
defineProp(protoOf(Estimation), 'description', function () {
  return this.k7();
});
defineProp(protoOf(Estimation), 'currentVersion', function () {
  return this.d9();
});
defineProp(protoOf(Estimation), 'versions', function () {
  return this.e9();
});
defineProp(protoOf(EstimationNode), 'logicalId', function () {
  return this.ea();
});
defineProp(protoOf(EstimationNode), 'mean', function () {
  return this.s9();
});
defineProp(protoOf(EstimationNode), 'variance', function () {
  return this.t9();
});
defineProp(protoOf(EstimationNode), 'riskSurcharge', function () {
  return this.u9();
});
defineProp(protoOf(EstimationNode), 'driverSurcharge', function () {
  return this.v9();
});
defineProp(protoOf(EstimationNode), 'offerPT', function () {
  return this.w9();
});
defineProp(protoOf(EstimationNode), 'cost', function () {
  return this.x9();
});
defineProp(protoOf(EstimationNode), 'offerPrice', function () {
  return this.y9();
});
defineProp(protoOf(EstimationGroup), 'title', function () {
  return this.q9();
});
defineProp(protoOf(EstimationGroup), 'children', function () {
  return this.r9();
});
defineProp(protoOf(EstimationItem), 'description', function () {
  return this.k7();
});
defineProp(protoOf(EstimationItem), 'code', function () {
  return this.ra();
});
defineProp(protoOf(EstimationItem), 'minEffort', function () {
  return this.sa();
});
defineProp(protoOf(EstimationItem), 'expectedEffort', function () {
  return this.ta();
});
defineProp(protoOf(EstimationItem), 'maxEffort', function () {
  return this.ua();
});
defineProp(protoOf(EstimationItem), 'assumptions', function () {
  return this.va();
});
defineProp(protoOf(EstimationItem), 'phase', function () {
  return this.o7();
});
defineProp(protoOf(EstimationItem), 'calculationParameters', function () {
  return this.wa();
});
defineProp(protoOf(EstimationParameter), 'name', function () {
  return this.c1();
});
defineProp(protoOf(EstimationParameter), 'value', function () {
  return this.gb();
});
defineProp(protoOf(EstimationParameter), 'comment', function () {
  return this.q8();
});
defineProp(protoOf(EstimationVersion), 'versionNumber', function () {
  return this.xb();
});
defineProp(protoOf(EstimationVersion), 'status', function () {
  return this.yb();
});
defineProp(protoOf(EstimationVersion), 'createdBy', function () {
  return this.zb();
});
defineProp(protoOf(EstimationVersion), 'totalEffort', function () {
  return this.ac();
});
defineProp(protoOf(EstimationVersion), 'notes', function () {
  return this.bc();
});
defineProp(protoOf(EstimationVersion), 'parameters', function () {
  return this.cc();
});
defineProp(protoOf(EstimationVersion), 'effortDrivers', function () {
  return this.dc();
});
defineProp(protoOf(EstimationVersion), 'phases', function () {
  return this.ec();
});
defineProp(protoOf(EstimationVersion), 'additionalCosts', function () {
  return this.fc();
});
defineProp(protoOf(EstimationVersion), 'roots', function () {
  return this.gc();
});
defineProp(protoOf(EstimationVersionStatus), 'name', protoOf(EstimationVersionStatus).c1);
defineProp(protoOf(EstimationVersionStatus), 'ordinal', protoOf(EstimationVersionStatus).d1);
defineProp(protoOf(Project), 'name', function () {
  return this.c1();
});
defineProp(protoOf(Project), 'description', function () {
  return this.k7();
});
defineProp(protoOf(Project), 'client', function () {
  return this.ce();
});
defineProp(protoOf(Project), 'status', function () {
  return this.yb();
});
defineProp(protoOf(Project), 'owner', function () {
  return this.de();
});
defineProp(protoOf(ProjectPhase), 'name', function () {
  return this.c1();
});
defineProp(protoOf(ProjectPhase), 'abbreviation', function () {
  return this.oe();
});
defineProp(protoOf(ProjectPhase), 'durationWeeks', function () {
  return this.pe();
});
defineProp(protoOf(ProjectStatus), 'name', protoOf(ProjectStatus).c1);
defineProp(protoOf(ProjectStatus), 'ordinal', protoOf(ProjectStatus).d1);
defineProp(protoOf(TimeRelativeEstimationItem), 'unit', function () {
  return this.sf();
});
defineProp(protoOf(User), 'entraSubjectId', function () {
  return this.cg();
});
defineProp(protoOf(User), 'displayName', function () {
  return this.dg();
});
defineProp(protoOf(InvariantResult), 'passed', protoOf(InvariantResult).ig);
//endregion
//region block: init
PertCalculation_instance = new PertCalculation_0();
Companion_instance = new Companion();
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
