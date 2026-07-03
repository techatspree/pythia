import {
  VOID3gxj6tk5isa35 as VOID,
  protoOf180f3jzyo7rfj as protoOf,
  toString30pk9tzaqopn as toString,
  getStringHashCode26igk1bx568vk as getStringHashCode,
  getNumberHashCode2l4nbdcihl25f as getNumberHashCode,
  equals2au1ep9vhcato as equals,
  defineProp3ur6h3slcvq4x as defineProp,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  THROW_IAE23kobfj9wdoxr as THROW_IAE,
  Unit_instancev9v8hjid95df as Unit_instance,
  Enum3alwj03lh1n41 as Enum,
  toList383f556t1dixk as toList,
  Companion_getInstance1bxbth0yni76u as Companion_getInstance,
  emptyList1g2z5xcrvp2zy as emptyList,
  toString1pkumu07cwy4m as toString_0,
  hashCodeq5arwsb9dgti as hashCode,
  collectionSizeOrDefault36dulx8yinfqm as collectionSizeOrDefault,
  ArrayList_init_$Create$2byt5m5yn22yy as ArrayList_init_$Create$,
  noWhenBranchMatchedException2a6r7ubxgky5j as noWhenBranchMatchedException,
  asSequence2phdjljfh9jhx as asSequence,
  flatMapgxtanzi5fvh9 as flatMap,
  sequenceOfdrc6uefhtiet as sequenceOf,
  ArrayList_init_$Create$37gc04va6yfo2 as ArrayList_init_$Create$_0,
  toListx6x8nvfmvvht as toList_0,
  addAll1k27qatfgp3k5 as addAll,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  initMetadataForCompanion1wyw17z38v6ac as initMetadataForCompanion,
  copyToArray2j022khrow2yi as copyToArray,
} from './kotlin-kotlin-stdlib.mjs';
import { KotlinLogging_instance2z94y4xmo0rc4 as KotlinLogging_instance } from './kotlin-logging.mjs';
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
initMetadataForObject(PertCalculation, 'PertCalculation');
initMetadataForClass(Project, 'Project', VOID, BaseDomain);
initMetadataForClass(ProjectPhase, 'ProjectPhase', VOID, BaseDomain);
initMetadataForClass(ProjectStatus, 'ProjectStatus', VOID, Enum);
initMetadataForClass(TimeRelativeEstimationItem, 'TimeRelativeEstimationItem', VOID, EstimationItem);
initMetadataForClass(User, 'User', User, BaseDomain);
initMetadataForClass(DraftMutation, 'DraftMutation');
initMetadataForClass(ReplaceWholeDraft, 'ReplaceWholeDraft', VOID, DraftMutation);
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
  this.a7_1 = description;
  this.b7_1 = amount;
  this.c7_1 = type;
  this.d7_1 = amountPerWeek;
  this.e7_1 = phase;
  this.f7_1 = _id;
  this.g7_1 = _createdAt;
  this.h7_1 = _updatedAt;
}
protoOf(AdditionalCost).i7 = function () {
  return this.a7_1;
};
protoOf(AdditionalCost).j7 = function () {
  return this.b7_1;
};
protoOf(AdditionalCost).k7 = function () {
  return this.c7_1;
};
protoOf(AdditionalCost).l7 = function () {
  return this.d7_1;
};
protoOf(AdditionalCost).m7 = function () {
  return this.e7_1;
};
protoOf(AdditionalCost).d5 = function () {
  return this.description;
};
protoOf(AdditionalCost).e5 = function () {
  return this.amount;
};
protoOf(AdditionalCost).n7 = function () {
  return this.type;
};
protoOf(AdditionalCost).o7 = function () {
  return this.amountPerWeek;
};
protoOf(AdditionalCost).p7 = function () {
  return this.phase;
};
protoOf(AdditionalCost).q7 = function (description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt) {
  return new AdditionalCost(description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt);
};
protoOf(AdditionalCost).copy = function (description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt, $super) {
  description = description === VOID ? this.description : description;
  amount = amount === VOID ? this.amount : amount;
  type = type === VOID ? this.type : type;
  amountPerWeek = amountPerWeek === VOID ? this.amountPerWeek : amountPerWeek;
  phase = phase === VOID ? this.phase : phase;
  _id = _id === VOID ? this.f7_1 : _id;
  _createdAt = _createdAt === VOID ? this.g7_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.h7_1 : _updatedAt;
  return $super === VOID ? this.q7(description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt) : $super.q7.call(this, description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt);
};
protoOf(AdditionalCost).toString = function () {
  return 'AdditionalCost(description=' + this.description + ', amount=' + this.amount + ', type=' + this.type.toString() + ', amountPerWeek=' + this.amountPerWeek + ', phase=' + toString(this.phase) + ', _id=' + this.f7_1 + ', _createdAt=' + this.g7_1 + ', _updatedAt=' + this.h7_1 + ')';
};
protoOf(AdditionalCost).hashCode = function () {
  var result = getStringHashCode(this.description);
  result = imul(result, 31) + getNumberHashCode(this.amount) | 0;
  result = imul(result, 31) + this.type.hashCode() | 0;
  result = imul(result, 31) + getNumberHashCode(this.amountPerWeek) | 0;
  result = imul(result, 31) + (this.phase == null ? 0 : this.phase.hashCode()) | 0;
  result = imul(result, 31) + (this.f7_1 == null ? 0 : getStringHashCode(this.f7_1)) | 0;
  result = imul(result, 31) + (this.g7_1 == null ? 0 : getStringHashCode(this.g7_1)) | 0;
  result = imul(result, 31) + (this.h7_1 == null ? 0 : getStringHashCode(this.h7_1)) | 0;
  return result;
};
protoOf(AdditionalCost).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof AdditionalCost))
    return false;
  if (!(this.description === other.description))
    return false;
  if (!equals(this.amount, other.amount))
    return false;
  if (!this.type.equals(other.type))
    return false;
  if (!equals(this.amountPerWeek, other.amountPerWeek))
    return false;
  if (!equals(this.phase, other.phase))
    return false;
  if (!(this.f7_1 == other.f7_1))
    return false;
  if (!(this.g7_1 == other.g7_1))
    return false;
  if (!(this.h7_1 == other.h7_1))
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
      THROW_IAE('No enum constant io.github.theestimator.model.AdditionalCostType.' + value);
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
  this.r7_1 = id;
  this.s7_1 = createdAt;
  this.t7_1 = updatedAt;
}
protoOf(BaseDomain).u7 = function () {
  return this.r7_1;
};
protoOf(BaseDomain).v7 = function () {
  return this.s7_1;
};
protoOf(BaseDomain).w7 = function () {
  return this.t7_1;
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
protoOf(CalculationParameters).z7 = function () {
  return this.riskFactor;
};
protoOf(CalculationParameters).a8 = function () {
  return this.totalDriverFactor;
};
protoOf(CalculationParameters).b8 = function () {
  return this.dailyRate;
};
protoOf(CalculationParameters).c8 = function () {
  return this.salesSurcharge;
};
protoOf(CalculationParameters).d5 = function () {
  return this.riskFactor;
};
protoOf(CalculationParameters).e5 = function () {
  return this.totalDriverFactor;
};
protoOf(CalculationParameters).n7 = function () {
  return this.dailyRate;
};
protoOf(CalculationParameters).o7 = function () {
  return this.salesSurcharge;
};
protoOf(CalculationParameters).d8 = function (riskFactor, totalDriverFactor, dailyRate, salesSurcharge) {
  return new CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
};
protoOf(CalculationParameters).copy = function (riskFactor, totalDriverFactor, dailyRate, salesSurcharge, $super) {
  riskFactor = riskFactor === VOID ? this.riskFactor : riskFactor;
  totalDriverFactor = totalDriverFactor === VOID ? this.totalDriverFactor : totalDriverFactor;
  dailyRate = dailyRate === VOID ? this.dailyRate : dailyRate;
  salesSurcharge = salesSurcharge === VOID ? this.salesSurcharge : salesSurcharge;
  return $super === VOID ? this.d8(riskFactor, totalDriverFactor, dailyRate, salesSurcharge) : $super.d8.call(this, riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
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
  if (!equals(this.riskFactor, other.riskFactor))
    return false;
  if (!equals(this.totalDriverFactor, other.totalDriverFactor))
    return false;
  if (!equals(this.dailyRate, other.dailyRate))
    return false;
  if (!equals(this.salesSurcharge, other.salesSurcharge))
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
  return Companion_getInstance().l5().toString();
}
function EffortDriver(description, factor, comment, _id, _createdAt, _updatedAt) {
  factor = factor === VOID ? 0.0 : factor;
  comment = comment === VOID ? '' : comment;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.h8_1 = description;
  this.i8_1 = factor;
  this.j8_1 = comment;
  this.k8_1 = _id;
  this.l8_1 = _createdAt;
  this.m8_1 = _updatedAt;
}
protoOf(EffortDriver).i7 = function () {
  return this.h8_1;
};
protoOf(EffortDriver).n8 = function () {
  return this.i8_1;
};
protoOf(EffortDriver).o8 = function () {
  return this.j8_1;
};
protoOf(EffortDriver).d5 = function () {
  return this.description;
};
protoOf(EffortDriver).e5 = function () {
  return this.factor;
};
protoOf(EffortDriver).n7 = function () {
  return this.comment;
};
protoOf(EffortDriver).p8 = function (description, factor, comment, _id, _createdAt, _updatedAt) {
  return new EffortDriver(description, factor, comment, _id, _createdAt, _updatedAt);
};
protoOf(EffortDriver).copy = function (description, factor, comment, _id, _createdAt, _updatedAt, $super) {
  description = description === VOID ? this.description : description;
  factor = factor === VOID ? this.factor : factor;
  comment = comment === VOID ? this.comment : comment;
  _id = _id === VOID ? this.k8_1 : _id;
  _createdAt = _createdAt === VOID ? this.l8_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.m8_1 : _updatedAt;
  return $super === VOID ? this.p8(description, factor, comment, _id, _createdAt, _updatedAt) : $super.p8.call(this, description, factor, comment, _id, _createdAt, _updatedAt);
};
protoOf(EffortDriver).toString = function () {
  return 'EffortDriver(description=' + this.description + ', factor=' + this.factor + ', comment=' + this.comment + ', _id=' + this.k8_1 + ', _createdAt=' + this.l8_1 + ', _updatedAt=' + this.m8_1 + ')';
};
protoOf(EffortDriver).hashCode = function () {
  var result = getStringHashCode(this.description);
  result = imul(result, 31) + getNumberHashCode(this.factor) | 0;
  result = imul(result, 31) + getStringHashCode(this.comment) | 0;
  result = imul(result, 31) + (this.k8_1 == null ? 0 : getStringHashCode(this.k8_1)) | 0;
  result = imul(result, 31) + (this.l8_1 == null ? 0 : getStringHashCode(this.l8_1)) | 0;
  result = imul(result, 31) + (this.m8_1 == null ? 0 : getStringHashCode(this.m8_1)) | 0;
  return result;
};
protoOf(EffortDriver).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof EffortDriver))
    return false;
  if (!(this.description === other.description))
    return false;
  if (!equals(this.factor, other.factor))
    return false;
  if (!(this.comment === other.comment))
    return false;
  if (!(this.k8_1 == other.k8_1))
    return false;
  if (!(this.l8_1 == other.l8_1))
    return false;
  if (!(this.m8_1 == other.m8_1))
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
  this.t8_1 = offer;
  this.u8_1 = description;
  this.v8_1 = currentVersion;
  this.w8_1 = versions;
  this.x8_1 = _id;
  this.y8_1 = _createdAt;
  this.z8_1 = _updatedAt;
}
protoOf(Estimation).a9 = function () {
  return this.t8_1;
};
protoOf(Estimation).i7 = function () {
  return this.u8_1;
};
protoOf(Estimation).b9 = function () {
  return this.v8_1;
};
protoOf(Estimation).c9 = function () {
  return this.w8_1;
};
protoOf(Estimation).d5 = function () {
  return this.offer;
};
protoOf(Estimation).e5 = function () {
  return this.description;
};
protoOf(Estimation).n7 = function () {
  return this.currentVersion;
};
protoOf(Estimation).o7 = function () {
  return this.versions;
};
protoOf(Estimation).d9 = function (offer, description, currentVersion, versions, _id, _createdAt, _updatedAt) {
  return new Estimation(offer, description, currentVersion, versions, _id, _createdAt, _updatedAt);
};
protoOf(Estimation).copy = function (offer, description, currentVersion, versions, _id, _createdAt, _updatedAt, $super) {
  offer = offer === VOID ? this.offer : offer;
  description = description === VOID ? this.description : description;
  currentVersion = currentVersion === VOID ? this.currentVersion : currentVersion;
  versions = versions === VOID ? this.versions : versions;
  _id = _id === VOID ? this.x8_1 : _id;
  _createdAt = _createdAt === VOID ? this.y8_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.z8_1 : _updatedAt;
  return $super === VOID ? this.d9(offer, description, currentVersion, versions, _id, _createdAt, _updatedAt) : $super.d9.call(this, offer, description, currentVersion, versions, _id, _createdAt, _updatedAt);
};
protoOf(Estimation).toString = function () {
  return 'Estimation(offer=' + this.offer + ', description=' + this.description + ', currentVersion=' + toString(this.currentVersion) + ', versions=' + toString_0(this.versions) + ', _id=' + this.x8_1 + ', _createdAt=' + this.y8_1 + ', _updatedAt=' + this.z8_1 + ')';
};
protoOf(Estimation).hashCode = function () {
  var result = getStringHashCode(this.offer);
  result = imul(result, 31) + getStringHashCode(this.description) | 0;
  result = imul(result, 31) + (this.currentVersion == null ? 0 : this.currentVersion.hashCode()) | 0;
  result = imul(result, 31) + hashCode(this.versions) | 0;
  result = imul(result, 31) + (this.x8_1 == null ? 0 : getStringHashCode(this.x8_1)) | 0;
  result = imul(result, 31) + (this.y8_1 == null ? 0 : getStringHashCode(this.y8_1)) | 0;
  result = imul(result, 31) + (this.z8_1 == null ? 0 : getStringHashCode(this.z8_1)) | 0;
  return result;
};
protoOf(Estimation).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Estimation))
    return false;
  if (!(this.offer === other.offer))
    return false;
  if (!(this.description === other.description))
    return false;
  if (!equals(this.currentVersion, other.currentVersion))
    return false;
  if (!equals(this.versions, other.versions))
    return false;
  if (!(this.x8_1 == other.x8_1))
    return false;
  if (!(this.y8_1 == other.y8_1))
    return false;
  if (!(this.z8_1 == other.z8_1))
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
  this.i9_1 = title;
  this.j9_1 = children;
  this.k9_1 = _logicalId;
  this.l9_1 = _id;
  this.m9_1 = _createdAt;
  this.n9_1 = _updatedAt;
}
protoOf(EstimationGroup).o9 = function () {
  return this.i9_1;
};
protoOf(EstimationGroup).p9 = function () {
  return this.j9_1;
};
protoOf(EstimationGroup).q9 = function () {
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
protoOf(EstimationGroup).r9 = function () {
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
protoOf(EstimationGroup).s9 = function () {
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
protoOf(EstimationGroup).t9 = function () {
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
protoOf(EstimationGroup).u9 = function () {
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
protoOf(EstimationGroup).v9 = function () {
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
protoOf(EstimationGroup).w9 = function () {
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
    var tmp$ret$2 = item.withCalculationParameters(params);
    destination.p(tmp$ret$2);
  }
  return this.copy(VOID, destination);
};
protoOf(EstimationGroup).d5 = function () {
  return this.title;
};
protoOf(EstimationGroup).e5 = function () {
  return this.children;
};
protoOf(EstimationGroup).x9 = function (title, children, _logicalId, _id, _createdAt, _updatedAt) {
  return new EstimationGroup(title, children, _logicalId, _id, _createdAt, _updatedAt);
};
protoOf(EstimationGroup).copy = function (title, children, _logicalId, _id, _createdAt, _updatedAt, $super) {
  title = title === VOID ? this.title : title;
  children = children === VOID ? this.children : children;
  _logicalId = _logicalId === VOID ? this.k9_1 : _logicalId;
  _id = _id === VOID ? this.l9_1 : _id;
  _createdAt = _createdAt === VOID ? this.m9_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.n9_1 : _updatedAt;
  return $super === VOID ? this.x9(title, children, _logicalId, _id, _createdAt, _updatedAt) : $super.x9.call(this, title, children, _logicalId, _id, _createdAt, _updatedAt);
};
protoOf(EstimationGroup).toString = function () {
  return 'EstimationGroup(title=' + this.title + ', children=' + toString_0(this.children) + ', _logicalId=' + this.k9_1 + ', _id=' + this.l9_1 + ', _createdAt=' + this.m9_1 + ', _updatedAt=' + this.n9_1 + ')';
};
protoOf(EstimationGroup).hashCode = function () {
  var result = getStringHashCode(this.title);
  result = imul(result, 31) + hashCode(this.children) | 0;
  result = imul(result, 31) + getStringHashCode(this.k9_1) | 0;
  result = imul(result, 31) + (this.l9_1 == null ? 0 : getStringHashCode(this.l9_1)) | 0;
  result = imul(result, 31) + (this.m9_1 == null ? 0 : getStringHashCode(this.m9_1)) | 0;
  result = imul(result, 31) + (this.n9_1 == null ? 0 : getStringHashCode(this.n9_1)) | 0;
  return result;
};
protoOf(EstimationGroup).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof EstimationGroup))
    return false;
  if (!(this.title === other.title))
    return false;
  if (!equals(this.children, other.children))
    return false;
  if (!(this.k9_1 === other.k9_1))
    return false;
  if (!(this.l9_1 == other.l9_1))
    return false;
  if (!(this.m9_1 == other.m9_1))
    return false;
  if (!(this.n9_1 == other.n9_1))
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
  this.ha_1 = description;
  this.ia_1 = code;
  this.ja_1 = minEffort;
  this.ka_1 = expectedEffort;
  this.la_1 = maxEffort;
  this.ma_1 = assumptions;
  this.na_1 = phase;
  this.oa_1 = calculationParameters;
}
protoOf(EstimationItem).i7 = function () {
  return this.ha_1;
};
protoOf(EstimationItem).pa = function () {
  return this.ia_1;
};
protoOf(EstimationItem).qa = function () {
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
protoOf(EstimationItem).m7 = function () {
  return this.na_1;
};
protoOf(EstimationItem).ua = function () {
  return this.oa_1;
};
protoOf(EstimationItem).q9 = function () {
  return PertCalculation_instance.mean(this.minEffort, this.expectedEffort, this.maxEffort);
};
protoOf(EstimationItem).r9 = function () {
  return PertCalculation_instance.variance(this.minEffort, this.maxEffort);
};
protoOf(EstimationItem).s9 = function () {
  return this.mean * this.calculationParameters.riskFactor;
};
protoOf(EstimationItem).t9 = function () {
  return this.mean * this.calculationParameters.totalDriverFactor;
};
protoOf(EstimationItem).u9 = function () {
  return this.mean + this.riskSurcharge + this.driverSurcharge;
};
protoOf(EstimationItem).v9 = function () {
  return this.offerPT * this.calculationParameters.dailyRate;
};
protoOf(EstimationItem).w9 = function () {
  return this.cost * (1 + this.calculationParameters.salesSurcharge);
};
function EstimationNode(logicalId, id, createdAt, updatedAt) {
  id = id === VOID ? null : id;
  createdAt = createdAt === VOID ? null : createdAt;
  updatedAt = updatedAt === VOID ? null : updatedAt;
  BaseDomain.call(this, id, createdAt, updatedAt);
  this.ba_1 = logicalId;
}
protoOf(EstimationNode).ca = function () {
  return this.ba_1;
};
function leaves(_this__u8e3s4) {
  var tmp;
  if (_this__u8e3s4 instanceof EstimationItem) {
    tmp = sequenceOf(_this__u8e3s4);
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
  this.ya_1 = name;
  this.za_1 = value;
  this.ab_1 = comment;
  this.bb_1 = _id;
  this.cb_1 = _createdAt;
  this.db_1 = _updatedAt;
}
protoOf(EstimationParameter).c1 = function () {
  return this.ya_1;
};
protoOf(EstimationParameter).eb = function () {
  return this.za_1;
};
protoOf(EstimationParameter).o8 = function () {
  return this.ab_1;
};
protoOf(EstimationParameter).d5 = function () {
  return this.name;
};
protoOf(EstimationParameter).e5 = function () {
  return this.value;
};
protoOf(EstimationParameter).n7 = function () {
  return this.comment;
};
protoOf(EstimationParameter).p8 = function (name, value, comment, _id, _createdAt, _updatedAt) {
  return new EstimationParameter(name, value, comment, _id, _createdAt, _updatedAt);
};
protoOf(EstimationParameter).copy = function (name, value, comment, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  value = value === VOID ? this.value : value;
  comment = comment === VOID ? this.comment : comment;
  _id = _id === VOID ? this.bb_1 : _id;
  _createdAt = _createdAt === VOID ? this.cb_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.db_1 : _updatedAt;
  return $super === VOID ? this.p8(name, value, comment, _id, _createdAt, _updatedAt) : $super.p8.call(this, name, value, comment, _id, _createdAt, _updatedAt);
};
protoOf(EstimationParameter).toString = function () {
  return 'EstimationParameter(name=' + this.name + ', value=' + this.value + ', comment=' + this.comment + ', _id=' + this.bb_1 + ', _createdAt=' + this.cb_1 + ', _updatedAt=' + this.db_1 + ')';
};
protoOf(EstimationParameter).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getNumberHashCode(this.value) | 0;
  result = imul(result, 31) + getStringHashCode(this.comment) | 0;
  result = imul(result, 31) + (this.bb_1 == null ? 0 : getStringHashCode(this.bb_1)) | 0;
  result = imul(result, 31) + (this.cb_1 == null ? 0 : getStringHashCode(this.cb_1)) | 0;
  result = imul(result, 31) + (this.db_1 == null ? 0 : getStringHashCode(this.db_1)) | 0;
  return result;
};
protoOf(EstimationParameter).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof EstimationParameter))
    return false;
  if (!(this.name === other.name))
    return false;
  if (!equals(this.value, other.value))
    return false;
  if (!(this.comment === other.comment))
    return false;
  if (!(this.bb_1 == other.bb_1))
    return false;
  if (!(this.cb_1 == other.cb_1))
    return false;
  if (!(this.db_1 == other.db_1))
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
    return 'calculate(): ' + $leaves.h() + ' leaves, totalMean=' + $totalMean + ', totalEffort=' + $newTotalEffort;
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
  this.ib_1 = versionNumber;
  this.jb_1 = status;
  this.kb_1 = createdBy;
  this.lb_1 = totalEffort;
  this.mb_1 = notes;
  this.nb_1 = parameters;
  this.ob_1 = effortDrivers;
  this.pb_1 = phases;
  this.qb_1 = additionalCosts;
  this.rb_1 = roots;
  this.sb_1 = _id;
  this.tb_1 = _createdAt;
  this.ub_1 = _updatedAt;
}
protoOf(EstimationVersion).vb = function () {
  return this.ib_1;
};
protoOf(EstimationVersion).wb = function () {
  return this.jb_1;
};
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
    var tmp$ret$11 = item.withCalculationParameters(params);
    destination_0.p(tmp$ret$11);
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
  var tmp_3 = get_logger();
  tmp_3.w5(EstimationVersion$calculate$lambda(leaves_0, totalMean, newTotalEffort));
  return this.copy(VOID, VOID, VOID, newTotalEffort, VOID, VOID, VOID, VOID, VOID, newRoots);
};
protoOf(EstimationVersion).d5 = function () {
  return this.versionNumber;
};
protoOf(EstimationVersion).e5 = function () {
  return this.status;
};
protoOf(EstimationVersion).n7 = function () {
  return this.createdBy;
};
protoOf(EstimationVersion).o7 = function () {
  return this.totalEffort;
};
protoOf(EstimationVersion).p7 = function () {
  return this.notes;
};
protoOf(EstimationVersion).fc = function () {
  return this.parameters;
};
protoOf(EstimationVersion).gc = function () {
  return this.effortDrivers;
};
protoOf(EstimationVersion).hc = function () {
  return this.phases;
};
protoOf(EstimationVersion).ic = function () {
  return this.additionalCosts;
};
protoOf(EstimationVersion).jc = function () {
  return this.roots;
};
protoOf(EstimationVersion).kc = function (versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt) {
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
  _id = _id === VOID ? this.sb_1 : _id;
  _createdAt = _createdAt === VOID ? this.tb_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.ub_1 : _updatedAt;
  return $super === VOID ? this.kc(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt) : $super.kc.call(this, versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt);
};
protoOf(EstimationVersion).toString = function () {
  return 'EstimationVersion(versionNumber=' + this.versionNumber + ', status=' + this.status.toString() + ', createdBy=' + toString(this.createdBy) + ', totalEffort=' + this.totalEffort + ', notes=' + this.notes + ', parameters=' + toString_0(this.parameters) + ', effortDrivers=' + toString_0(this.effortDrivers) + ', phases=' + toString_0(this.phases) + ', additionalCosts=' + toString_0(this.additionalCosts) + ', roots=' + toString_0(this.roots) + ', _id=' + this.sb_1 + ', _createdAt=' + this.tb_1 + ', _updatedAt=' + this.ub_1 + ')';
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
  result = imul(result, 31) + (this.sb_1 == null ? 0 : getStringHashCode(this.sb_1)) | 0;
  result = imul(result, 31) + (this.tb_1 == null ? 0 : getStringHashCode(this.tb_1)) | 0;
  result = imul(result, 31) + (this.ub_1 == null ? 0 : getStringHashCode(this.ub_1)) | 0;
  return result;
};
protoOf(EstimationVersion).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof EstimationVersion))
    return false;
  if (!(this.versionNumber === other.versionNumber))
    return false;
  if (!this.status.equals(other.status))
    return false;
  if (!equals(this.createdBy, other.createdBy))
    return false;
  if (!equals(this.totalEffort, other.totalEffort))
    return false;
  if (!(this.notes === other.notes))
    return false;
  if (!equals(this.parameters, other.parameters))
    return false;
  if (!equals(this.effortDrivers, other.effortDrivers))
    return false;
  if (!equals(this.phases, other.phases))
    return false;
  if (!equals(this.additionalCosts, other.additionalCosts))
    return false;
  if (!equals(this.roots, other.roots))
    return false;
  if (!(this.sb_1 == other.sb_1))
    return false;
  if (!(this.tb_1 == other.tb_1))
    return false;
  if (!(this.ub_1 == other.ub_1))
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
    logger = tmp.z5(logger$lambda);
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
      THROW_IAE('No enum constant io.github.theestimator.model.EstimationVersionStatus.' + value);
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
  this.zc_1 = _description;
  this.ad_1 = _code;
  this.bd_1 = _minEffort;
  this.cd_1 = _expectedEffort;
  this.dd_1 = _maxEffort;
  this.ed_1 = _assumptions;
  this.fd_1 = _phase;
  this.gd_1 = _logicalId;
  this.hd_1 = _calculationParameters;
  this.jd_1 = _id;
  this.kd_1 = _createdAt;
  this.ld_1 = _updatedAt;
}
protoOf(FixedEstimationItem).withCalculationParameters = function (params) {
  return this.copy(VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, params);
};
protoOf(FixedEstimationItem).md = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  return new FixedEstimationItem(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(FixedEstimationItem).copy = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
  _description = _description === VOID ? this.zc_1 : _description;
  _code = _code === VOID ? this.ad_1 : _code;
  _minEffort = _minEffort === VOID ? this.bd_1 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? this.cd_1 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? this.dd_1 : _maxEffort;
  _assumptions = _assumptions === VOID ? this.ed_1 : _assumptions;
  _phase = _phase === VOID ? this.fd_1 : _phase;
  _logicalId = _logicalId === VOID ? this.gd_1 : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? this.hd_1 : _calculationParameters;
  _id = _id === VOID ? this.jd_1 : _id;
  _createdAt = _createdAt === VOID ? this.kd_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.ld_1 : _updatedAt;
  return $super === VOID ? this.md(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.md.call(this, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(FixedEstimationItem).toString = function () {
  return 'FixedEstimationItem(_description=' + this.zc_1 + ', _code=' + this.ad_1 + ', _minEffort=' + this.bd_1 + ', _expectedEffort=' + this.cd_1 + ', _maxEffort=' + this.dd_1 + ', _assumptions=' + this.ed_1 + ', _phase=' + toString(this.fd_1) + ', _logicalId=' + this.gd_1 + ', _calculationParameters=' + this.hd_1.toString() + ', _id=' + this.jd_1 + ', _createdAt=' + this.kd_1 + ', _updatedAt=' + this.ld_1 + ')';
};
protoOf(FixedEstimationItem).hashCode = function () {
  var result = getStringHashCode(this.zc_1);
  result = imul(result, 31) + getStringHashCode(this.ad_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.bd_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.cd_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.dd_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.ed_1) | 0;
  result = imul(result, 31) + (this.fd_1 == null ? 0 : this.fd_1.hashCode()) | 0;
  result = imul(result, 31) + getStringHashCode(this.gd_1) | 0;
  result = imul(result, 31) + this.hd_1.hashCode() | 0;
  result = imul(result, 31) + (this.jd_1 == null ? 0 : getStringHashCode(this.jd_1)) | 0;
  result = imul(result, 31) + (this.kd_1 == null ? 0 : getStringHashCode(this.kd_1)) | 0;
  result = imul(result, 31) + (this.ld_1 == null ? 0 : getStringHashCode(this.ld_1)) | 0;
  return result;
};
protoOf(FixedEstimationItem).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof FixedEstimationItem))
    return false;
  if (!(this.zc_1 === other.zc_1))
    return false;
  if (!(this.ad_1 === other.ad_1))
    return false;
  if (!equals(this.bd_1, other.bd_1))
    return false;
  if (!equals(this.cd_1, other.cd_1))
    return false;
  if (!equals(this.dd_1, other.dd_1))
    return false;
  if (!(this.ed_1 === other.ed_1))
    return false;
  if (!equals(this.fd_1, other.fd_1))
    return false;
  if (!(this.gd_1 === other.gd_1))
    return false;
  if (!this.hd_1.equals(other.hd_1))
    return false;
  if (!(this.jd_1 == other.jd_1))
    return false;
  if (!(this.kd_1 == other.kd_1))
    return false;
  if (!(this.ld_1 == other.ld_1))
    return false;
  return true;
};
function PertCalculation() {
  this.nd_1 = 4.0;
  this.od_1 = 6.0;
}
protoOf(PertCalculation).mean = function (min, expected, max) {
  return (min + 4.0 * expected + max) / 6.0;
};
protoOf(PertCalculation).variance = function (min, max) {
  var range = (max - min) / 6.0;
  return range * range;
};
protoOf(PertCalculation).riskFactor = function (totalMean, totalVariance, stdDevFactor) {
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
  this.sd_1 = name;
  this.td_1 = description;
  this.ud_1 = client;
  this.vd_1 = status;
  this.wd_1 = owner;
  this.xd_1 = _id;
  this.yd_1 = _createdAt;
  this.zd_1 = _updatedAt;
}
protoOf(Project).c1 = function () {
  return this.sd_1;
};
protoOf(Project).i7 = function () {
  return this.td_1;
};
protoOf(Project).ae = function () {
  return this.ud_1;
};
protoOf(Project).wb = function () {
  return this.vd_1;
};
protoOf(Project).be = function () {
  return this.wd_1;
};
protoOf(Project).d5 = function () {
  return this.name;
};
protoOf(Project).e5 = function () {
  return this.description;
};
protoOf(Project).n7 = function () {
  return this.client;
};
protoOf(Project).o7 = function () {
  return this.status;
};
protoOf(Project).p7 = function () {
  return this.owner;
};
protoOf(Project).ce = function (name, description, client, status, owner, _id, _createdAt, _updatedAt) {
  return new Project(name, description, client, status, owner, _id, _createdAt, _updatedAt);
};
protoOf(Project).copy = function (name, description, client, status, owner, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  description = description === VOID ? this.description : description;
  client = client === VOID ? this.client : client;
  status = status === VOID ? this.status : status;
  owner = owner === VOID ? this.owner : owner;
  _id = _id === VOID ? this.xd_1 : _id;
  _createdAt = _createdAt === VOID ? this.yd_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.zd_1 : _updatedAt;
  return $super === VOID ? this.ce(name, description, client, status, owner, _id, _createdAt, _updatedAt) : $super.ce.call(this, name, description, client, status, owner, _id, _createdAt, _updatedAt);
};
protoOf(Project).toString = function () {
  return 'Project(name=' + this.name + ', description=' + this.description + ', client=' + this.client + ', status=' + this.status.toString() + ', owner=' + toString(this.owner) + ', _id=' + this.xd_1 + ', _createdAt=' + this.yd_1 + ', _updatedAt=' + this.zd_1 + ')';
};
protoOf(Project).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getStringHashCode(this.description) | 0;
  result = imul(result, 31) + getStringHashCode(this.client) | 0;
  result = imul(result, 31) + this.status.hashCode() | 0;
  result = imul(result, 31) + (this.owner == null ? 0 : this.owner.hashCode()) | 0;
  result = imul(result, 31) + (this.xd_1 == null ? 0 : getStringHashCode(this.xd_1)) | 0;
  result = imul(result, 31) + (this.yd_1 == null ? 0 : getStringHashCode(this.yd_1)) | 0;
  result = imul(result, 31) + (this.zd_1 == null ? 0 : getStringHashCode(this.zd_1)) | 0;
  return result;
};
protoOf(Project).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Project))
    return false;
  if (!(this.name === other.name))
    return false;
  if (!(this.description === other.description))
    return false;
  if (!(this.client === other.client))
    return false;
  if (!this.status.equals(other.status))
    return false;
  if (!equals(this.owner, other.owner))
    return false;
  if (!(this.xd_1 == other.xd_1))
    return false;
  if (!(this.yd_1 == other.yd_1))
    return false;
  if (!(this.zd_1 == other.zd_1))
    return false;
  return true;
};
function ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
  durationWeeks = durationWeeks === VOID ? 0.0 : durationWeeks;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.ge_1 = name;
  this.he_1 = abbreviation;
  this.ie_1 = durationWeeks;
  this.je_1 = _id;
  this.ke_1 = _createdAt;
  this.le_1 = _updatedAt;
}
protoOf(ProjectPhase).c1 = function () {
  return this.ge_1;
};
protoOf(ProjectPhase).me = function () {
  return this.he_1;
};
protoOf(ProjectPhase).ne = function () {
  return this.ie_1;
};
protoOf(ProjectPhase).d5 = function () {
  return this.name;
};
protoOf(ProjectPhase).e5 = function () {
  return this.abbreviation;
};
protoOf(ProjectPhase).n7 = function () {
  return this.durationWeeks;
};
protoOf(ProjectPhase).oe = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
  return new ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
};
protoOf(ProjectPhase).copy = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  abbreviation = abbreviation === VOID ? this.abbreviation : abbreviation;
  durationWeeks = durationWeeks === VOID ? this.durationWeeks : durationWeeks;
  _id = _id === VOID ? this.je_1 : _id;
  _createdAt = _createdAt === VOID ? this.ke_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.le_1 : _updatedAt;
  return $super === VOID ? this.oe(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) : $super.oe.call(this, name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
};
protoOf(ProjectPhase).toString = function () {
  return 'ProjectPhase(name=' + this.name + ', abbreviation=' + this.abbreviation + ', durationWeeks=' + this.durationWeeks + ', _id=' + this.je_1 + ', _createdAt=' + this.ke_1 + ', _updatedAt=' + this.le_1 + ')';
};
protoOf(ProjectPhase).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getStringHashCode(this.abbreviation) | 0;
  result = imul(result, 31) + getNumberHashCode(this.durationWeeks) | 0;
  result = imul(result, 31) + (this.je_1 == null ? 0 : getStringHashCode(this.je_1)) | 0;
  result = imul(result, 31) + (this.ke_1 == null ? 0 : getStringHashCode(this.ke_1)) | 0;
  result = imul(result, 31) + (this.le_1 == null ? 0 : getStringHashCode(this.le_1)) | 0;
  return result;
};
protoOf(ProjectPhase).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof ProjectPhase))
    return false;
  if (!(this.name === other.name))
    return false;
  if (!(this.abbreviation === other.abbreviation))
    return false;
  if (!equals(this.durationWeeks, other.durationWeeks))
    return false;
  if (!(this.je_1 == other.je_1))
    return false;
  if (!(this.ke_1 == other.ke_1))
    return false;
  if (!(this.le_1 == other.le_1))
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
      THROW_IAE('No enum constant io.github.theestimator.model.ProjectStatus.' + value);
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
  this.df_1 = unit;
  this.ef_1 = _description;
  this.ff_1 = _code;
  this.gf_1 = _minEffort;
  this.hf_1 = _expectedEffort;
  this.if_1 = _maxEffort;
  this.jf_1 = _assumptions;
  this.kf_1 = _phase;
  this.lf_1 = _logicalId;
  this.mf_1 = _calculationParameters;
  this.nf_1 = _id;
  this.of_1 = _createdAt;
  this.pf_1 = _updatedAt;
}
protoOf(TimeRelativeEstimationItem).qf = function () {
  return this.df_1;
};
protoOf(TimeRelativeEstimationItem).q9 = function () {
  var tmp = PertCalculation_instance.mean(this.minEffort, this.expectedEffort, this.maxEffort);
  var tmp0_safe_receiver = this.phase;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.durationWeeks;
  return tmp * (tmp1_elvis_lhs == null ? 0.0 : tmp1_elvis_lhs);
};
protoOf(TimeRelativeEstimationItem).r9 = function () {
  var tmp0_safe_receiver = this.phase;
  var tmp1_elvis_lhs = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.durationWeeks;
  var d = tmp1_elvis_lhs == null ? 0.0 : tmp1_elvis_lhs;
  return PertCalculation_instance.variance(this.minEffort, this.maxEffort) * d * d;
};
protoOf(TimeRelativeEstimationItem).withCalculationParameters = function (params) {
  return this.copy(VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, params);
};
protoOf(TimeRelativeEstimationItem).d5 = function () {
  return this.unit;
};
protoOf(TimeRelativeEstimationItem).rf = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  return new TimeRelativeEstimationItem(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(TimeRelativeEstimationItem).copy = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
  unit = unit === VOID ? this.unit : unit;
  _description = _description === VOID ? this.ef_1 : _description;
  _code = _code === VOID ? this.ff_1 : _code;
  _minEffort = _minEffort === VOID ? this.gf_1 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? this.hf_1 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? this.if_1 : _maxEffort;
  _assumptions = _assumptions === VOID ? this.jf_1 : _assumptions;
  _phase = _phase === VOID ? this.kf_1 : _phase;
  _logicalId = _logicalId === VOID ? this.lf_1 : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? this.mf_1 : _calculationParameters;
  _id = _id === VOID ? this.nf_1 : _id;
  _createdAt = _createdAt === VOID ? this.of_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.pf_1 : _updatedAt;
  return $super === VOID ? this.rf(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.rf.call(this, unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(TimeRelativeEstimationItem).toString = function () {
  return 'TimeRelativeEstimationItem(unit=' + this.unit + ', _description=' + this.ef_1 + ', _code=' + this.ff_1 + ', _minEffort=' + this.gf_1 + ', _expectedEffort=' + this.hf_1 + ', _maxEffort=' + this.if_1 + ', _assumptions=' + this.jf_1 + ', _phase=' + toString(this.kf_1) + ', _logicalId=' + this.lf_1 + ', _calculationParameters=' + this.mf_1.toString() + ', _id=' + this.nf_1 + ', _createdAt=' + this.of_1 + ', _updatedAt=' + this.pf_1 + ')';
};
protoOf(TimeRelativeEstimationItem).hashCode = function () {
  var result = getStringHashCode(this.unit);
  result = imul(result, 31) + getStringHashCode(this.ef_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.ff_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.gf_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.hf_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.if_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.jf_1) | 0;
  result = imul(result, 31) + (this.kf_1 == null ? 0 : this.kf_1.hashCode()) | 0;
  result = imul(result, 31) + getStringHashCode(this.lf_1) | 0;
  result = imul(result, 31) + this.mf_1.hashCode() | 0;
  result = imul(result, 31) + (this.nf_1 == null ? 0 : getStringHashCode(this.nf_1)) | 0;
  result = imul(result, 31) + (this.of_1 == null ? 0 : getStringHashCode(this.of_1)) | 0;
  result = imul(result, 31) + (this.pf_1 == null ? 0 : getStringHashCode(this.pf_1)) | 0;
  return result;
};
protoOf(TimeRelativeEstimationItem).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof TimeRelativeEstimationItem))
    return false;
  if (!(this.unit === other.unit))
    return false;
  if (!(this.ef_1 === other.ef_1))
    return false;
  if (!(this.ff_1 === other.ff_1))
    return false;
  if (!equals(this.gf_1, other.gf_1))
    return false;
  if (!equals(this.hf_1, other.hf_1))
    return false;
  if (!equals(this.if_1, other.if_1))
    return false;
  if (!(this.jf_1 === other.jf_1))
    return false;
  if (!equals(this.kf_1, other.kf_1))
    return false;
  if (!(this.lf_1 === other.lf_1))
    return false;
  if (!this.mf_1.equals(other.mf_1))
    return false;
  if (!(this.nf_1 == other.nf_1))
    return false;
  if (!(this.of_1 == other.of_1))
    return false;
  if (!(this.pf_1 == other.pf_1))
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
  this.vf_1 = entraSubjectId;
  this.wf_1 = displayName;
  this.xf_1 = _id;
  this.yf_1 = _createdAt;
  this.zf_1 = _updatedAt;
}
protoOf(User).ag = function () {
  return this.vf_1;
};
protoOf(User).bg = function () {
  return this.wf_1;
};
protoOf(User).d5 = function () {
  return this.entraSubjectId;
};
protoOf(User).e5 = function () {
  return this.displayName;
};
protoOf(User).cg = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt) {
  return new User(entraSubjectId, displayName, _id, _createdAt, _updatedAt);
};
protoOf(User).copy = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt, $super) {
  entraSubjectId = entraSubjectId === VOID ? this.entraSubjectId : entraSubjectId;
  displayName = displayName === VOID ? this.displayName : displayName;
  _id = _id === VOID ? this.xf_1 : _id;
  _createdAt = _createdAt === VOID ? this.yf_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.zf_1 : _updatedAt;
  return $super === VOID ? this.cg(entraSubjectId, displayName, _id, _createdAt, _updatedAt) : $super.cg.call(this, entraSubjectId, displayName, _id, _createdAt, _updatedAt);
};
protoOf(User).toString = function () {
  return 'User(entraSubjectId=' + this.entraSubjectId + ', displayName=' + this.displayName + ', _id=' + this.xf_1 + ', _createdAt=' + this.yf_1 + ', _updatedAt=' + this.zf_1 + ')';
};
protoOf(User).hashCode = function () {
  var result = this.entraSubjectId == null ? 0 : getStringHashCode(this.entraSubjectId);
  result = imul(result, 31) + getStringHashCode(this.displayName) | 0;
  result = imul(result, 31) + (this.xf_1 == null ? 0 : getStringHashCode(this.xf_1)) | 0;
  result = imul(result, 31) + (this.yf_1 == null ? 0 : getStringHashCode(this.yf_1)) | 0;
  result = imul(result, 31) + (this.zf_1 == null ? 0 : getStringHashCode(this.zf_1)) | 0;
  return result;
};
protoOf(User).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof User))
    return false;
  if (!(this.entraSubjectId == other.entraSubjectId))
    return false;
  if (!(this.displayName === other.displayName))
    return false;
  if (!(this.xf_1 == other.xf_1))
    return false;
  if (!(this.yf_1 == other.yf_1))
    return false;
  if (!(this.zf_1 == other.zf_1))
    return false;
  return true;
};
function DraftMutation() {
}
function ReplaceWholeDraft(before, after) {
  DraftMutation.call(this);
  this.eg_1 = before;
  this.fg_1 = after;
  this.gg_1 = 'REPLACE_WHOLE_DRAFT';
}
protoOf(ReplaceWholeDraft).hg = function () {
  return this.eg_1;
};
protoOf(ReplaceWholeDraft).ig = function () {
  return this.fg_1;
};
protoOf(ReplaceWholeDraft).dg = function () {
  return this.gg_1;
};
protoOf(ReplaceWholeDraft).apply = function (current) {
  return this.after;
};
protoOf(ReplaceWholeDraft).inverse = function () {
  return new ReplaceWholeDraft(this.after, this.before);
};
protoOf(ReplaceWholeDraft).d5 = function () {
  return this.before;
};
protoOf(ReplaceWholeDraft).e5 = function () {
  return this.after;
};
protoOf(ReplaceWholeDraft).jg = function (before, after) {
  return new ReplaceWholeDraft(before, after);
};
protoOf(ReplaceWholeDraft).copy = function (before, after, $super) {
  before = before === VOID ? this.before : before;
  after = after === VOID ? this.after : after;
  return $super === VOID ? this.jg(before, after) : $super.jg.call(this, before, after);
};
protoOf(ReplaceWholeDraft).toString = function () {
  return 'ReplaceWholeDraft(before=' + this.before.toString() + ', after=' + this.after.toString() + ')';
};
protoOf(ReplaceWholeDraft).hashCode = function () {
  var result = this.before.hashCode();
  result = imul(result, 31) + this.after.hashCode() | 0;
  return result;
};
protoOf(ReplaceWholeDraft).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof ReplaceWholeDraft))
    return false;
  if (!this.before.equals(other.before))
    return false;
  if (!this.after.equals(other.after))
    return false;
  return true;
};
function Companion() {
  this.kg_1 = 0.2;
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
  results.p(new InvariantResult('Gesamtaufwand = Summe aller AngebotsPT', version.totalEffort - totalOfferPT, tolerance));
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
  results.p(new InvariantResult('Summe mit Risiko im PSP = Summe bei Berechnung', totalOfferPT - calculatedTotal, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_3 = 0;
  var _iterator__ex2g4s_4 = version.roots.e();
  while (_iterator__ex2g4s_4.f()) {
    var element_4 = _iterator__ex2g4s_4.g();
    var tmp_3 = sum_3;
    sum_3 = tmp_3 + element_4.offerPT;
  }
  var sumByRoots = sum_3;
  results.p(new InvariantResult('Summe der Wurzeln = Summe der Bl\xE4tter (Akkumulation konsistent)', sumByRoots - totalOfferPT, tolerance));
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
  results.p(new InvariantResult('Kosten im PSP = Kosten in der Paket\xFCbersicht', totalCost - costFromEffort, tolerance));
  // Inline function 'kotlin.collections.sumOf' call
  var sum_5 = 0;
  var _iterator__ex2g4s_6 = version.roots.e();
  while (_iterator__ex2g4s_6.f()) {
    var element_6 = _iterator__ex2g4s_6.g();
    var tmp_5 = sum_5;
    sum_5 = tmp_5 + element_6.variance;
  }
  var varianceByRoots = sum_5;
  results.p(new InvariantResult('Varianzakkumulation an der Wurzel = Summe der Bl\xE4tter-Varianzen', varianceByRoots - totalVariance, tolerance));
  // Inline function 'kotlin.collections.toTypedArray' call
  return copyToArray(results);
};
function InvariantResult(description, difference, tolerance) {
  this.description = description;
  this.difference = difference;
  this.tolerance = tolerance;
}
protoOf(InvariantResult).i7 = function () {
  return this.description;
};
protoOf(InvariantResult).lg = function () {
  return this.difference;
};
protoOf(InvariantResult).mg = function () {
  return this.tolerance;
};
protoOf(InvariantResult).ng = function () {
  // Inline function 'kotlin.math.abs' call
  var x = this.difference;
  return Math.abs(x) <= this.tolerance;
};
protoOf(InvariantResult).d5 = function () {
  return this.description;
};
protoOf(InvariantResult).e5 = function () {
  return this.difference;
};
protoOf(InvariantResult).n7 = function () {
  return this.tolerance;
};
protoOf(InvariantResult).og = function (description, difference, tolerance) {
  return new InvariantResult(description, difference, tolerance);
};
protoOf(InvariantResult).copy = function (description, difference, tolerance, $super) {
  description = description === VOID ? this.description : description;
  difference = difference === VOID ? this.difference : difference;
  tolerance = tolerance === VOID ? this.tolerance : tolerance;
  return $super === VOID ? this.og(description, difference, tolerance) : $super.og.call(this, description, difference, tolerance);
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
  if (!(this.description === other.description))
    return false;
  if (!equals(this.difference, other.difference))
    return false;
  if (!equals(this.tolerance, other.tolerance))
    return false;
  return true;
};
//region block: post-declaration
defineProp(protoOf(BaseDomain), 'id', function () {
  return this.u7();
});
defineProp(protoOf(BaseDomain), 'createdAt', function () {
  return this.v7();
});
defineProp(protoOf(BaseDomain), 'updatedAt', function () {
  return this.w7();
});
defineProp(protoOf(AdditionalCost), 'description', function () {
  return this.i7();
});
defineProp(protoOf(AdditionalCost), 'amount', function () {
  return this.j7();
});
defineProp(protoOf(AdditionalCost), 'type', function () {
  return this.k7();
});
defineProp(protoOf(AdditionalCost), 'amountPerWeek', function () {
  return this.l7();
});
defineProp(protoOf(AdditionalCost), 'phase', function () {
  return this.m7();
});
defineProp(protoOf(AdditionalCostType), 'name', protoOf(AdditionalCostType).c1);
defineProp(protoOf(AdditionalCostType), 'ordinal', protoOf(AdditionalCostType).d1);
defineProp(protoOf(EffortDriver), 'description', function () {
  return this.i7();
});
defineProp(protoOf(EffortDriver), 'factor', function () {
  return this.n8();
});
defineProp(protoOf(EffortDriver), 'comment', function () {
  return this.o8();
});
defineProp(protoOf(Estimation), 'offer', function () {
  return this.a9();
});
defineProp(protoOf(Estimation), 'description', function () {
  return this.i7();
});
defineProp(protoOf(Estimation), 'currentVersion', function () {
  return this.b9();
});
defineProp(protoOf(Estimation), 'versions', function () {
  return this.c9();
});
defineProp(protoOf(EstimationNode), 'logicalId', function () {
  return this.ca();
});
defineProp(protoOf(EstimationNode), 'mean', function () {
  return this.q9();
});
defineProp(protoOf(EstimationNode), 'variance', function () {
  return this.r9();
});
defineProp(protoOf(EstimationNode), 'riskSurcharge', function () {
  return this.s9();
});
defineProp(protoOf(EstimationNode), 'driverSurcharge', function () {
  return this.t9();
});
defineProp(protoOf(EstimationNode), 'offerPT', function () {
  return this.u9();
});
defineProp(protoOf(EstimationNode), 'cost', function () {
  return this.v9();
});
defineProp(protoOf(EstimationNode), 'offerPrice', function () {
  return this.w9();
});
defineProp(protoOf(EstimationGroup), 'title', function () {
  return this.o9();
});
defineProp(protoOf(EstimationGroup), 'children', function () {
  return this.p9();
});
defineProp(protoOf(EstimationItem), 'description', function () {
  return this.i7();
});
defineProp(protoOf(EstimationItem), 'code', function () {
  return this.pa();
});
defineProp(protoOf(EstimationItem), 'minEffort', function () {
  return this.qa();
});
defineProp(protoOf(EstimationItem), 'expectedEffort', function () {
  return this.ra();
});
defineProp(protoOf(EstimationItem), 'maxEffort', function () {
  return this.sa();
});
defineProp(protoOf(EstimationItem), 'assumptions', function () {
  return this.ta();
});
defineProp(protoOf(EstimationItem), 'phase', function () {
  return this.m7();
});
defineProp(protoOf(EstimationItem), 'calculationParameters', function () {
  return this.ua();
});
defineProp(protoOf(EstimationParameter), 'name', function () {
  return this.c1();
});
defineProp(protoOf(EstimationParameter), 'value', function () {
  return this.eb();
});
defineProp(protoOf(EstimationParameter), 'comment', function () {
  return this.o8();
});
defineProp(protoOf(EstimationVersion), 'versionNumber', function () {
  return this.vb();
});
defineProp(protoOf(EstimationVersion), 'status', function () {
  return this.wb();
});
defineProp(protoOf(EstimationVersion), 'createdBy', function () {
  return this.xb();
});
defineProp(protoOf(EstimationVersion), 'totalEffort', function () {
  return this.yb();
});
defineProp(protoOf(EstimationVersion), 'notes', function () {
  return this.zb();
});
defineProp(protoOf(EstimationVersion), 'parameters', function () {
  return this.ac();
});
defineProp(protoOf(EstimationVersion), 'effortDrivers', function () {
  return this.bc();
});
defineProp(protoOf(EstimationVersion), 'phases', function () {
  return this.cc();
});
defineProp(protoOf(EstimationVersion), 'additionalCosts', function () {
  return this.dc();
});
defineProp(protoOf(EstimationVersion), 'roots', function () {
  return this.ec();
});
defineProp(protoOf(EstimationVersionStatus), 'name', protoOf(EstimationVersionStatus).c1);
defineProp(protoOf(EstimationVersionStatus), 'ordinal', protoOf(EstimationVersionStatus).d1);
defineProp(protoOf(Project), 'name', function () {
  return this.c1();
});
defineProp(protoOf(Project), 'description', function () {
  return this.i7();
});
defineProp(protoOf(Project), 'client', function () {
  return this.ae();
});
defineProp(protoOf(Project), 'status', function () {
  return this.wb();
});
defineProp(protoOf(Project), 'owner', function () {
  return this.be();
});
defineProp(protoOf(ProjectPhase), 'name', function () {
  return this.c1();
});
defineProp(protoOf(ProjectPhase), 'abbreviation', function () {
  return this.me();
});
defineProp(protoOf(ProjectPhase), 'durationWeeks', function () {
  return this.ne();
});
defineProp(protoOf(ProjectStatus), 'name', protoOf(ProjectStatus).c1);
defineProp(protoOf(ProjectStatus), 'ordinal', protoOf(ProjectStatus).d1);
defineProp(protoOf(TimeRelativeEstimationItem), 'unit', function () {
  return this.qf();
});
defineProp(protoOf(User), 'entraSubjectId', function () {
  return this.ag();
});
defineProp(protoOf(User), 'displayName', function () {
  return this.bg();
});
defineProp(protoOf(DraftMutation), 'kind', function () {
  return this.dg();
});
defineProp(protoOf(ReplaceWholeDraft), 'before', function () {
  return this.hg();
});
defineProp(protoOf(ReplaceWholeDraft), 'after', function () {
  return this.ig();
});
defineProp(protoOf(InvariantResult), 'passed', protoOf(InvariantResult).ng);
//endregion
//region block: init
PertCalculation_instance = new PertCalculation();
Companion_instance = new Companion();
//endregion
//region block: exports
AdditionalCostType.values = values;
AdditionalCostType.valueOf = valueOf;
defineProp(AdditionalCostType, 'ONE_TIME', AdditionalCostType_ONE_TIME_getInstance, VOID, true);
defineProp(AdditionalCostType, 'RECURRING', AdditionalCostType_RECURRING_getInstance, VOID, true);
EstimationVersionStatus.values = values_0;
EstimationVersionStatus.valueOf = valueOf_0;
defineProp(EstimationVersionStatus, 'DRAFT', EstimationVersionStatus_DRAFT_getInstance, VOID, true);
defineProp(EstimationVersionStatus, 'SUBMITTED', EstimationVersionStatus_SUBMITTED_getInstance, VOID, true);
var PertCalculation_0 = {getInstance: PertCalculation_getInstance};
ProjectStatus.values = values_1;
ProjectStatus.valueOf = valueOf_1;
defineProp(ProjectStatus, 'ACTIVE', ProjectStatus_ACTIVE_getInstance, VOID, true);
defineProp(ProjectStatus, 'ARCHIVED', ProjectStatus_ARCHIVED_getInstance, VOID, true);
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
  PertCalculation_0 as PertCalculation,
  Project as Project,
  ProjectPhase as ProjectPhase,
  ProjectStatus as ProjectStatus,
  TimeRelativeEstimationItem as TimeRelativeEstimationItem,
  User as User,
  DraftMutation as DraftMutation,
  ReplaceWholeDraft as ReplaceWholeDraft,
  EstimationCalculator as EstimationCalculator,
  InvariantResult as InvariantResult,
};
//endregion

//# sourceMappingURL=domain.mjs.map
