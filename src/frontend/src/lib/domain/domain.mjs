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
  ArrayList_init_$Create$1s1wkrw82c0iw as ArrayList_init_$Create$,
  Companion_getInstance3tnw2k4njrdpv as Companion_getInstance,
  emptyList1g2z5xcrvp2zy as emptyList,
  toString1pkumu07cwy4m as toString_0,
  hashCodeq5arwsb9dgti as hashCode,
  collectionSizeOrDefault36dulx8yinfqm as collectionSizeOrDefault,
  noWhenBranchMatchedException2a6r7ubxgky5j as noWhenBranchMatchedException,
  asSequence2phdjljfh9jhx as asSequence,
  flatMapgxtanzi5fvh9 as flatMap,
  sequenceOf1mtha40gp6gat as sequenceOf,
  objectCreate1ve4bgxiu4x98 as objectCreate,
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
initMetadataForClass(EstimationItemGroup, 'EstimationItemGroup', VOID, BaseDomain);
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
function createGroup(title, logicalId, items) {
  logicalId = logicalId === VOID ? newId() : logicalId;
  var tmp;
  if (items === VOID) {
    // Inline function 'kotlin.emptyArray' call
    tmp = [];
  } else {
    tmp = items;
  }
  items = tmp;
  return new EstimationItemGroup(title, logicalId, toList(items));
}
function createVersion(versionNumber, isDraft, notes, parameters, effortDrivers, phases, itemGroups) {
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
  if (itemGroups === VOID) {
    // Inline function 'kotlin.emptyArray' call
    tmp_2 = [];
  } else {
    tmp_2 = itemGroups;
  }
  itemGroups = tmp_2;
  var tmp_3 = isDraft ? EstimationVersionStatus_DRAFT_getInstance() : EstimationVersionStatus_SUBMITTED_getInstance();
  var tmp_4 = toList(parameters);
  var tmp_5 = toList(effortDrivers);
  var tmp_6 = toList(phases);
  // Inline function 'kotlin.collections.map' call
  // Inline function 'kotlin.collections.mapTo' call
  var destination = ArrayList_init_$Create$(itemGroups.length);
  var inductionVariable = 0;
  var last = itemGroups.length;
  while (inductionVariable < last) {
    var item = itemGroups[inductionVariable];
    inductionVariable = inductionVariable + 1 | 0;
    var tmp$ret$4 = new EstimationGroup(item.title, item.items, item.logicalId);
    destination.l(tmp$ret$4);
  }
  return new EstimationVersion(versionNumber, tmp_3, VOID, VOID, notes, tmp_4, tmp_5, tmp_6, VOID, destination);
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
function EstimationItemGroup(title, logicalId, items, _id, _createdAt, _updatedAt) {
  logicalId = logicalId === VOID ? newId() : logicalId;
  items = items === VOID ? emptyList() : items;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.c8_1 = title;
  this.d8_1 = logicalId;
  this.e8_1 = items;
  this.f8_1 = _id;
  this.g8_1 = _createdAt;
  this.h8_1 = _updatedAt;
}
protoOf(EstimationItemGroup).s6 = function () {
  return this.c8_1;
};
protoOf(EstimationItemGroup).g7 = function () {
  return this.d8_1;
};
protoOf(EstimationItemGroup).i8 = function () {
  return this.e8_1;
};
protoOf(EstimationItemGroup).p4 = function () {
  return this.title;
};
protoOf(EstimationItemGroup).q4 = function () {
  return this.logicalId;
};
protoOf(EstimationItemGroup).r4 = function () {
  return this.items;
};
protoOf(EstimationItemGroup).j8 = function (title, logicalId, items, _id, _createdAt, _updatedAt) {
  return new EstimationItemGroup(title, logicalId, items, _id, _createdAt, _updatedAt);
};
protoOf(EstimationItemGroup).copy = function (title, logicalId, items, _id, _createdAt, _updatedAt, $super) {
  title = title === VOID ? this.title : title;
  logicalId = logicalId === VOID ? this.logicalId : logicalId;
  items = items === VOID ? this.items : items;
  _id = _id === VOID ? this.f8_1 : _id;
  _createdAt = _createdAt === VOID ? this.g8_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.h8_1 : _updatedAt;
  return $super === VOID ? this.j8(title, logicalId, items, _id, _createdAt, _updatedAt) : $super.j8.call(this, title, logicalId, items, _id, _createdAt, _updatedAt);
};
protoOf(EstimationItemGroup).toString = function () {
  return 'EstimationItemGroup(title=' + this.title + ', logicalId=' + this.logicalId + ', items=' + toString_0(this.items) + ', _id=' + this.f8_1 + ', _createdAt=' + this.g8_1 + ', _updatedAt=' + this.h8_1 + ')';
};
protoOf(EstimationItemGroup).hashCode = function () {
  var result = getStringHashCode(this.title);
  result = imul(result, 31) + getStringHashCode(this.logicalId) | 0;
  result = imul(result, 31) + hashCode(this.items) | 0;
  result = imul(result, 31) + (this.f8_1 == null ? 0 : getStringHashCode(this.f8_1)) | 0;
  result = imul(result, 31) + (this.g8_1 == null ? 0 : getStringHashCode(this.g8_1)) | 0;
  result = imul(result, 31) + (this.h8_1 == null ? 0 : getStringHashCode(this.h8_1)) | 0;
  return result;
};
protoOf(EstimationItemGroup).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof EstimationItemGroup))
    return false;
  var tmp0_other_with_cast = other instanceof EstimationItemGroup ? other : THROW_CCE();
  if (!(this.title === tmp0_other_with_cast.title))
    return false;
  if (!(this.logicalId === tmp0_other_with_cast.logicalId))
    return false;
  if (!equals(this.items, tmp0_other_with_cast.items))
    return false;
  if (!(this.f8_1 == tmp0_other_with_cast.f8_1))
    return false;
  if (!(this.g8_1 == tmp0_other_with_cast.g8_1))
    return false;
  if (!(this.h8_1 == tmp0_other_with_cast.h8_1))
    return false;
  return true;
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
  this.n8_1 = name;
  this.o8_1 = value;
  this.p8_1 = comment;
  this.q8_1 = _id;
  this.r8_1 = _createdAt;
  this.s8_1 = _updatedAt;
}
protoOf(EstimationParameter).y = function () {
  return this.n8_1;
};
protoOf(EstimationParameter).t8 = function () {
  return this.o8_1;
};
protoOf(EstimationParameter).s5 = function () {
  return this.p8_1;
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
  _id = _id === VOID ? this.q8_1 : _id;
  _createdAt = _createdAt === VOID ? this.r8_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.s8_1 : _updatedAt;
  return $super === VOID ? this.t5(name, value, comment, _id, _createdAt, _updatedAt) : $super.t5.call(this, name, value, comment, _id, _createdAt, _updatedAt);
};
protoOf(EstimationParameter).toString = function () {
  return 'EstimationParameter(name=' + this.name + ', value=' + this.value + ', comment=' + this.comment + ', _id=' + this.q8_1 + ', _createdAt=' + this.r8_1 + ', _updatedAt=' + this.s8_1 + ')';
};
protoOf(EstimationParameter).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getNumberHashCode(this.value) | 0;
  result = imul(result, 31) + getStringHashCode(this.comment) | 0;
  result = imul(result, 31) + (this.q8_1 == null ? 0 : getStringHashCode(this.q8_1)) | 0;
  result = imul(result, 31) + (this.r8_1 == null ? 0 : getStringHashCode(this.r8_1)) | 0;
  result = imul(result, 31) + (this.s8_1 == null ? 0 : getStringHashCode(this.s8_1)) | 0;
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
  if (!(this.q8_1 == tmp0_other_with_cast.q8_1))
    return false;
  if (!(this.r8_1 == tmp0_other_with_cast.r8_1))
    return false;
  if (!(this.s8_1 == tmp0_other_with_cast.s8_1))
    return false;
  return true;
};
function EstimationVersion_init_$Init$(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, itemGroups, $this) {
  status = status === VOID ? EstimationVersionStatus_DRAFT_getInstance() : status;
  createdBy = createdBy === VOID ? null : createdBy;
  totalEffort = totalEffort === VOID ? 0.0 : totalEffort;
  notes = notes === VOID ? '' : notes;
  parameters = parameters === VOID ? emptyList() : parameters;
  effortDrivers = effortDrivers === VOID ? emptyList() : effortDrivers;
  phases = phases === VOID ? emptyList() : phases;
  additionalCosts = additionalCosts === VOID ? emptyList() : additionalCosts;
  // Inline function 'kotlin.collections.map' call
  // Inline function 'kotlin.collections.mapTo' call
  var destination = ArrayList_init_$Create$(collectionSizeOrDefault(itemGroups, 10));
  var _iterator__ex2g4s = itemGroups.e();
  while (_iterator__ex2g4s.f()) {
    var item = _iterator__ex2g4s.g();
    var tmp$ret$0 = new EstimationGroup(item.title, item.items, item.logicalId);
    destination.l(tmp$ret$0);
  }
  EstimationVersion.call($this, versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, destination);
  return $this;
}
function createFromItemGroups(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, itemGroups) {
  return EstimationVersion_init_$Init$(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, itemGroups, objectCreate(protoOf(EstimationVersion)));
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
  this.x8_1 = versionNumber;
  this.y8_1 = status;
  this.z8_1 = createdBy;
  this.a9_1 = totalEffort;
  this.b9_1 = notes;
  this.c9_1 = parameters;
  this.d9_1 = effortDrivers;
  this.e9_1 = phases;
  this.f9_1 = additionalCosts;
  this.g9_1 = roots;
  this.h9_1 = _id;
  this.i9_1 = _createdAt;
  this.j9_1 = _updatedAt;
}
protoOf(EstimationVersion).k9 = function () {
  return this.x8_1;
};
protoOf(EstimationVersion).l9 = function () {
  return this.y8_1;
};
protoOf(EstimationVersion).m9 = function () {
  return this.z8_1;
};
protoOf(EstimationVersion).n9 = function () {
  return this.a9_1;
};
protoOf(EstimationVersion).o9 = function () {
  return this.b9_1;
};
protoOf(EstimationVersion).p9 = function () {
  return this.c9_1;
};
protoOf(EstimationVersion).q9 = function () {
  return this.d9_1;
};
protoOf(EstimationVersion).r9 = function () {
  return this.e9_1;
};
protoOf(EstimationVersion).s9 = function () {
  return this.f9_1;
};
protoOf(EstimationVersion).t9 = function () {
  return this.g9_1;
};
protoOf(EstimationVersion).u9 = function () {
  // Inline function 'kotlin.collections.filterIsInstance' call
  var tmp0 = this.roots;
  // Inline function 'kotlin.collections.filterIsInstanceTo' call
  var destination = ArrayList_init_$Create$_0();
  var _iterator__ex2g4s = tmp0.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    if (element instanceof EstimationGroup) {
      destination.l(element);
    }
  }
  // Inline function 'kotlin.collections.map' call
  // Inline function 'kotlin.collections.mapTo' call
  var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(destination, 10));
  var _iterator__ex2g4s_0 = destination.e();
  while (_iterator__ex2g4s_0.f()) {
    var item = _iterator__ex2g4s_0.g();
    var tmp = item.title;
    var tmp_0 = item.logicalId;
    // Inline function 'kotlin.collections.filterIsInstance' call
    var tmp0_0 = item.children;
    // Inline function 'kotlin.collections.filterIsInstanceTo' call
    var destination_1 = ArrayList_init_$Create$_0();
    var _iterator__ex2g4s_1 = tmp0_0.e();
    while (_iterator__ex2g4s_1.f()) {
      var element_0 = _iterator__ex2g4s_1.g();
      if (element_0 instanceof EstimationItem) {
        destination_1.l(element_0);
      }
    }
    var tmp$ret$4 = new EstimationItemGroup(tmp, tmp_0, destination_1);
    destination_0.l(tmp$ret$4);
  }
  return destination_0;
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
protoOf(EstimationVersion).v9 = function () {
  return this.parameters;
};
protoOf(EstimationVersion).w9 = function () {
  return this.effortDrivers;
};
protoOf(EstimationVersion).x9 = function () {
  return this.phases;
};
protoOf(EstimationVersion).y9 = function () {
  return this.additionalCosts;
};
protoOf(EstimationVersion).z9 = function () {
  return this.roots;
};
protoOf(EstimationVersion).aa = function (versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt) {
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
  _id = _id === VOID ? this.h9_1 : _id;
  _createdAt = _createdAt === VOID ? this.i9_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.j9_1 : _updatedAt;
  return $super === VOID ? this.aa(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt) : $super.aa.call(this, versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, roots, _id, _createdAt, _updatedAt);
};
protoOf(EstimationVersion).toString = function () {
  return 'EstimationVersion(versionNumber=' + this.versionNumber + ', status=' + this.status.toString() + ', createdBy=' + toString(this.createdBy) + ', totalEffort=' + this.totalEffort + ', notes=' + this.notes + ', parameters=' + toString_0(this.parameters) + ', effortDrivers=' + toString_0(this.effortDrivers) + ', phases=' + toString_0(this.phases) + ', additionalCosts=' + toString_0(this.additionalCosts) + ', roots=' + toString_0(this.roots) + ', _id=' + this.h9_1 + ', _createdAt=' + this.i9_1 + ', _updatedAt=' + this.j9_1 + ')';
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
  result = imul(result, 31) + (this.h9_1 == null ? 0 : getStringHashCode(this.h9_1)) | 0;
  result = imul(result, 31) + (this.i9_1 == null ? 0 : getStringHashCode(this.i9_1)) | 0;
  result = imul(result, 31) + (this.j9_1 == null ? 0 : getStringHashCode(this.j9_1)) | 0;
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
  if (!(this.h9_1 == tmp0_other_with_cast.h9_1))
    return false;
  if (!(this.i9_1 == tmp0_other_with_cast.i9_1))
    return false;
  if (!(this.j9_1 == tmp0_other_with_cast.j9_1))
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
  this.pa_1 = _description;
  this.qa_1 = _code;
  this.ra_1 = _minEffort;
  this.sa_1 = _expectedEffort;
  this.ta_1 = _maxEffort;
  this.ua_1 = _assumptions;
  this.va_1 = _phase;
  this.wa_1 = _logicalId;
  this.xa_1 = _calculationParameters;
  this.ya_1 = _id;
  this.za_1 = _createdAt;
  this.ab_1 = _updatedAt;
}
protoOf(FixedEstimationItem).withCalculationParameters = function (params) {
  return this.copy(VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, params);
};
protoOf(FixedEstimationItem).bb = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  return new FixedEstimationItem(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(FixedEstimationItem).copy = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
  _description = _description === VOID ? this.pa_1 : _description;
  _code = _code === VOID ? this.qa_1 : _code;
  _minEffort = _minEffort === VOID ? this.ra_1 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? this.sa_1 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? this.ta_1 : _maxEffort;
  _assumptions = _assumptions === VOID ? this.ua_1 : _assumptions;
  _phase = _phase === VOID ? this.va_1 : _phase;
  _logicalId = _logicalId === VOID ? this.wa_1 : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? this.xa_1 : _calculationParameters;
  _id = _id === VOID ? this.ya_1 : _id;
  _createdAt = _createdAt === VOID ? this.za_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.ab_1 : _updatedAt;
  return $super === VOID ? this.bb(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.bb.call(this, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(FixedEstimationItem).toString = function () {
  return 'FixedEstimationItem(_description=' + this.pa_1 + ', _code=' + this.qa_1 + ', _minEffort=' + this.ra_1 + ', _expectedEffort=' + this.sa_1 + ', _maxEffort=' + this.ta_1 + ', _assumptions=' + this.ua_1 + ', _phase=' + toString(this.va_1) + ', _logicalId=' + this.wa_1 + ', _calculationParameters=' + this.xa_1.toString() + ', _id=' + this.ya_1 + ', _createdAt=' + this.za_1 + ', _updatedAt=' + this.ab_1 + ')';
};
protoOf(FixedEstimationItem).hashCode = function () {
  var result = getStringHashCode(this.pa_1);
  result = imul(result, 31) + getStringHashCode(this.qa_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.ra_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.sa_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.ta_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.ua_1) | 0;
  result = imul(result, 31) + (this.va_1 == null ? 0 : this.va_1.hashCode()) | 0;
  result = imul(result, 31) + getStringHashCode(this.wa_1) | 0;
  result = imul(result, 31) + this.xa_1.hashCode() | 0;
  result = imul(result, 31) + (this.ya_1 == null ? 0 : getStringHashCode(this.ya_1)) | 0;
  result = imul(result, 31) + (this.za_1 == null ? 0 : getStringHashCode(this.za_1)) | 0;
  result = imul(result, 31) + (this.ab_1 == null ? 0 : getStringHashCode(this.ab_1)) | 0;
  return result;
};
protoOf(FixedEstimationItem).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof FixedEstimationItem))
    return false;
  var tmp0_other_with_cast = other instanceof FixedEstimationItem ? other : THROW_CCE();
  if (!(this.pa_1 === tmp0_other_with_cast.pa_1))
    return false;
  if (!(this.qa_1 === tmp0_other_with_cast.qa_1))
    return false;
  if (!equals(this.ra_1, tmp0_other_with_cast.ra_1))
    return false;
  if (!equals(this.sa_1, tmp0_other_with_cast.sa_1))
    return false;
  if (!equals(this.ta_1, tmp0_other_with_cast.ta_1))
    return false;
  if (!(this.ua_1 === tmp0_other_with_cast.ua_1))
    return false;
  if (!equals(this.va_1, tmp0_other_with_cast.va_1))
    return false;
  if (!(this.wa_1 === tmp0_other_with_cast.wa_1))
    return false;
  if (!this.xa_1.equals(tmp0_other_with_cast.xa_1))
    return false;
  if (!(this.ya_1 == tmp0_other_with_cast.ya_1))
    return false;
  if (!(this.za_1 == tmp0_other_with_cast.za_1))
    return false;
  if (!(this.ab_1 == tmp0_other_with_cast.ab_1))
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
  this.fb_1 = name;
  this.gb_1 = description;
  this.hb_1 = client;
  this.ib_1 = status;
  this.jb_1 = owner;
  this.kb_1 = _id;
  this.lb_1 = _createdAt;
  this.mb_1 = _updatedAt;
}
protoOf(Project).y = function () {
  return this.fb_1;
};
protoOf(Project).k4 = function () {
  return this.gb_1;
};
protoOf(Project).nb = function () {
  return this.hb_1;
};
protoOf(Project).l9 = function () {
  return this.ib_1;
};
protoOf(Project).ob = function () {
  return this.jb_1;
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
protoOf(Project).pb = function (name, description, client, status, owner, _id, _createdAt, _updatedAt) {
  return new Project(name, description, client, status, owner, _id, _createdAt, _updatedAt);
};
protoOf(Project).copy = function (name, description, client, status, owner, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  description = description === VOID ? this.description : description;
  client = client === VOID ? this.client : client;
  status = status === VOID ? this.status : status;
  owner = owner === VOID ? this.owner : owner;
  _id = _id === VOID ? this.kb_1 : _id;
  _createdAt = _createdAt === VOID ? this.lb_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.mb_1 : _updatedAt;
  return $super === VOID ? this.pb(name, description, client, status, owner, _id, _createdAt, _updatedAt) : $super.pb.call(this, name, description, client, status, owner, _id, _createdAt, _updatedAt);
};
protoOf(Project).toString = function () {
  return 'Project(name=' + this.name + ', description=' + this.description + ', client=' + this.client + ', status=' + this.status.toString() + ', owner=' + toString(this.owner) + ', _id=' + this.kb_1 + ', _createdAt=' + this.lb_1 + ', _updatedAt=' + this.mb_1 + ')';
};
protoOf(Project).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getStringHashCode(this.description) | 0;
  result = imul(result, 31) + getStringHashCode(this.client) | 0;
  result = imul(result, 31) + this.status.hashCode() | 0;
  result = imul(result, 31) + (this.owner == null ? 0 : this.owner.hashCode()) | 0;
  result = imul(result, 31) + (this.kb_1 == null ? 0 : getStringHashCode(this.kb_1)) | 0;
  result = imul(result, 31) + (this.lb_1 == null ? 0 : getStringHashCode(this.lb_1)) | 0;
  result = imul(result, 31) + (this.mb_1 == null ? 0 : getStringHashCode(this.mb_1)) | 0;
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
  if (!(this.kb_1 == tmp0_other_with_cast.kb_1))
    return false;
  if (!(this.lb_1 == tmp0_other_with_cast.lb_1))
    return false;
  if (!(this.mb_1 == tmp0_other_with_cast.mb_1))
    return false;
  return true;
};
function ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
  durationWeeks = durationWeeks === VOID ? 0.0 : durationWeeks;
  _id = _id === VOID ? null : _id;
  _createdAt = _createdAt === VOID ? null : _createdAt;
  _updatedAt = _updatedAt === VOID ? null : _updatedAt;
  BaseDomain.call(this, _id, _createdAt, _updatedAt);
  this.tb_1 = name;
  this.ub_1 = abbreviation;
  this.vb_1 = durationWeeks;
  this.wb_1 = _id;
  this.xb_1 = _createdAt;
  this.yb_1 = _updatedAt;
}
protoOf(ProjectPhase).y = function () {
  return this.tb_1;
};
protoOf(ProjectPhase).zb = function () {
  return this.ub_1;
};
protoOf(ProjectPhase).ac = function () {
  return this.vb_1;
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
protoOf(ProjectPhase).bc = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
  return new ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
};
protoOf(ProjectPhase).copy = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt, $super) {
  name = name === VOID ? this.name : name;
  abbreviation = abbreviation === VOID ? this.abbreviation : abbreviation;
  durationWeeks = durationWeeks === VOID ? this.durationWeeks : durationWeeks;
  _id = _id === VOID ? this.wb_1 : _id;
  _createdAt = _createdAt === VOID ? this.xb_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.yb_1 : _updatedAt;
  return $super === VOID ? this.bc(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) : $super.bc.call(this, name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
};
protoOf(ProjectPhase).toString = function () {
  return 'ProjectPhase(name=' + this.name + ', abbreviation=' + this.abbreviation + ', durationWeeks=' + this.durationWeeks + ', _id=' + this.wb_1 + ', _createdAt=' + this.xb_1 + ', _updatedAt=' + this.yb_1 + ')';
};
protoOf(ProjectPhase).hashCode = function () {
  var result = getStringHashCode(this.name);
  result = imul(result, 31) + getStringHashCode(this.abbreviation) | 0;
  result = imul(result, 31) + getNumberHashCode(this.durationWeeks) | 0;
  result = imul(result, 31) + (this.wb_1 == null ? 0 : getStringHashCode(this.wb_1)) | 0;
  result = imul(result, 31) + (this.xb_1 == null ? 0 : getStringHashCode(this.xb_1)) | 0;
  result = imul(result, 31) + (this.yb_1 == null ? 0 : getStringHashCode(this.yb_1)) | 0;
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
  if (!(this.wb_1 == tmp0_other_with_cast.wb_1))
    return false;
  if (!(this.xb_1 == tmp0_other_with_cast.xb_1))
    return false;
  if (!(this.yb_1 == tmp0_other_with_cast.yb_1))
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
  this.qc_1 = unit;
  this.rc_1 = _description;
  this.sc_1 = _code;
  this.tc_1 = _minEffort;
  this.uc_1 = _expectedEffort;
  this.vc_1 = _maxEffort;
  this.wc_1 = _assumptions;
  this.xc_1 = _phase;
  this.yc_1 = _logicalId;
  this.zc_1 = _calculationParameters;
  this.ad_1 = _id;
  this.bd_1 = _createdAt;
  this.cd_1 = _updatedAt;
}
protoOf(TimeRelativeEstimationItem).dd = function () {
  return this.qc_1;
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
protoOf(TimeRelativeEstimationItem).ed = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
  return new TimeRelativeEstimationItem(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(TimeRelativeEstimationItem).copy = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
  unit = unit === VOID ? this.unit : unit;
  _description = _description === VOID ? this.rc_1 : _description;
  _code = _code === VOID ? this.sc_1 : _code;
  _minEffort = _minEffort === VOID ? this.tc_1 : _minEffort;
  _expectedEffort = _expectedEffort === VOID ? this.uc_1 : _expectedEffort;
  _maxEffort = _maxEffort === VOID ? this.vc_1 : _maxEffort;
  _assumptions = _assumptions === VOID ? this.wc_1 : _assumptions;
  _phase = _phase === VOID ? this.xc_1 : _phase;
  _logicalId = _logicalId === VOID ? this.yc_1 : _logicalId;
  _calculationParameters = _calculationParameters === VOID ? this.zc_1 : _calculationParameters;
  _id = _id === VOID ? this.ad_1 : _id;
  _createdAt = _createdAt === VOID ? this.bd_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.cd_1 : _updatedAt;
  return $super === VOID ? this.ed(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.ed.call(this, unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
};
protoOf(TimeRelativeEstimationItem).toString = function () {
  return 'TimeRelativeEstimationItem(unit=' + this.unit + ', _description=' + this.rc_1 + ', _code=' + this.sc_1 + ', _minEffort=' + this.tc_1 + ', _expectedEffort=' + this.uc_1 + ', _maxEffort=' + this.vc_1 + ', _assumptions=' + this.wc_1 + ', _phase=' + toString(this.xc_1) + ', _logicalId=' + this.yc_1 + ', _calculationParameters=' + this.zc_1.toString() + ', _id=' + this.ad_1 + ', _createdAt=' + this.bd_1 + ', _updatedAt=' + this.cd_1 + ')';
};
protoOf(TimeRelativeEstimationItem).hashCode = function () {
  var result = getStringHashCode(this.unit);
  result = imul(result, 31) + getStringHashCode(this.rc_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.sc_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.tc_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.uc_1) | 0;
  result = imul(result, 31) + getNumberHashCode(this.vc_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.wc_1) | 0;
  result = imul(result, 31) + (this.xc_1 == null ? 0 : this.xc_1.hashCode()) | 0;
  result = imul(result, 31) + getStringHashCode(this.yc_1) | 0;
  result = imul(result, 31) + this.zc_1.hashCode() | 0;
  result = imul(result, 31) + (this.ad_1 == null ? 0 : getStringHashCode(this.ad_1)) | 0;
  result = imul(result, 31) + (this.bd_1 == null ? 0 : getStringHashCode(this.bd_1)) | 0;
  result = imul(result, 31) + (this.cd_1 == null ? 0 : getStringHashCode(this.cd_1)) | 0;
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
  if (!(this.rc_1 === tmp0_other_with_cast.rc_1))
    return false;
  if (!(this.sc_1 === tmp0_other_with_cast.sc_1))
    return false;
  if (!equals(this.tc_1, tmp0_other_with_cast.tc_1))
    return false;
  if (!equals(this.uc_1, tmp0_other_with_cast.uc_1))
    return false;
  if (!equals(this.vc_1, tmp0_other_with_cast.vc_1))
    return false;
  if (!(this.wc_1 === tmp0_other_with_cast.wc_1))
    return false;
  if (!equals(this.xc_1, tmp0_other_with_cast.xc_1))
    return false;
  if (!(this.yc_1 === tmp0_other_with_cast.yc_1))
    return false;
  if (!this.zc_1.equals(tmp0_other_with_cast.zc_1))
    return false;
  if (!(this.ad_1 == tmp0_other_with_cast.ad_1))
    return false;
  if (!(this.bd_1 == tmp0_other_with_cast.bd_1))
    return false;
  if (!(this.cd_1 == tmp0_other_with_cast.cd_1))
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
  this.jd_1 = entraSubjectId;
  this.kd_1 = displayName;
  this.ld_1 = _id;
  this.md_1 = _createdAt;
  this.nd_1 = _updatedAt;
}
protoOf(User).od = function () {
  return this.jd_1;
};
protoOf(User).pd = function () {
  return this.kd_1;
};
protoOf(User).p4 = function () {
  return this.entraSubjectId;
};
protoOf(User).q4 = function () {
  return this.displayName;
};
protoOf(User).qd = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt) {
  return new User(entraSubjectId, displayName, _id, _createdAt, _updatedAt);
};
protoOf(User).copy = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt, $super) {
  entraSubjectId = entraSubjectId === VOID ? this.entraSubjectId : entraSubjectId;
  displayName = displayName === VOID ? this.displayName : displayName;
  _id = _id === VOID ? this.ld_1 : _id;
  _createdAt = _createdAt === VOID ? this.md_1 : _createdAt;
  _updatedAt = _updatedAt === VOID ? this.nd_1 : _updatedAt;
  return $super === VOID ? this.qd(entraSubjectId, displayName, _id, _createdAt, _updatedAt) : $super.qd.call(this, entraSubjectId, displayName, _id, _createdAt, _updatedAt);
};
protoOf(User).toString = function () {
  return 'User(entraSubjectId=' + this.entraSubjectId + ', displayName=' + this.displayName + ', _id=' + this.ld_1 + ', _createdAt=' + this.md_1 + ', _updatedAt=' + this.nd_1 + ')';
};
protoOf(User).hashCode = function () {
  var result = this.entraSubjectId == null ? 0 : getStringHashCode(this.entraSubjectId);
  result = imul(result, 31) + getStringHashCode(this.displayName) | 0;
  result = imul(result, 31) + (this.ld_1 == null ? 0 : getStringHashCode(this.ld_1)) | 0;
  result = imul(result, 31) + (this.md_1 == null ? 0 : getStringHashCode(this.md_1)) | 0;
  result = imul(result, 31) + (this.nd_1 == null ? 0 : getStringHashCode(this.nd_1)) | 0;
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
  if (!(this.ld_1 == tmp0_other_with_cast.ld_1))
    return false;
  if (!(this.md_1 == tmp0_other_with_cast.md_1))
    return false;
  if (!(this.nd_1 == tmp0_other_with_cast.nd_1))
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
protoOf(InvariantResult).rd = function () {
  return this.difference;
};
protoOf(InvariantResult).sd = function () {
  return this.tolerance;
};
protoOf(InvariantResult).td = function () {
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
protoOf(InvariantResult).ud = function (description, difference, tolerance) {
  return new InvariantResult(description, difference, tolerance);
};
protoOf(InvariantResult).copy = function (description, difference, tolerance, $super) {
  description = description === VOID ? this.description : description;
  difference = difference === VOID ? this.difference : difference;
  tolerance = tolerance === VOID ? this.tolerance : tolerance;
  return $super === VOID ? this.ud(description, difference, tolerance) : $super.ud.call(this, description, difference, tolerance);
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
defineProp(protoOf(EstimationItemGroup), 'title', function () {
  return this.s6();
});
defineProp(protoOf(EstimationItemGroup), 'logicalId', function () {
  return this.g7();
});
defineProp(protoOf(EstimationItemGroup), 'items', function () {
  return this.i8();
});
defineProp(protoOf(EstimationParameter), 'name', function () {
  return this.y();
});
defineProp(protoOf(EstimationParameter), 'value', function () {
  return this.t8();
});
defineProp(protoOf(EstimationParameter), 'comment', function () {
  return this.s5();
});
defineProp(protoOf(EstimationVersion), 'versionNumber', function () {
  return this.k9();
});
defineProp(protoOf(EstimationVersion), 'status', function () {
  return this.l9();
});
defineProp(protoOf(EstimationVersion), 'createdBy', function () {
  return this.m9();
});
defineProp(protoOf(EstimationVersion), 'totalEffort', function () {
  return this.n9();
});
defineProp(protoOf(EstimationVersion), 'notes', function () {
  return this.o9();
});
defineProp(protoOf(EstimationVersion), 'parameters', function () {
  return this.p9();
});
defineProp(protoOf(EstimationVersion), 'effortDrivers', function () {
  return this.q9();
});
defineProp(protoOf(EstimationVersion), 'phases', function () {
  return this.r9();
});
defineProp(protoOf(EstimationVersion), 'additionalCosts', function () {
  return this.s9();
});
defineProp(protoOf(EstimationVersion), 'roots', function () {
  return this.t9();
});
defineProp(protoOf(EstimationVersion), 'itemGroups', function () {
  return this.u9();
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
  return this.nb();
});
defineProp(protoOf(Project), 'status', function () {
  return this.l9();
});
defineProp(protoOf(Project), 'owner', function () {
  return this.ob();
});
defineProp(protoOf(ProjectPhase), 'name', function () {
  return this.y();
});
defineProp(protoOf(ProjectPhase), 'abbreviation', function () {
  return this.zb();
});
defineProp(protoOf(ProjectPhase), 'durationWeeks', function () {
  return this.ac();
});
defineProp(protoOf(ProjectStatus), 'name', protoOf(ProjectStatus).y);
defineProp(protoOf(ProjectStatus), 'ordinal', protoOf(ProjectStatus).z);
defineProp(protoOf(TimeRelativeEstimationItem), 'unit', function () {
  return this.dd();
});
defineProp(protoOf(User), 'entraSubjectId', function () {
  return this.od();
});
defineProp(protoOf(User), 'displayName', function () {
  return this.pd();
});
defineProp(protoOf(InvariantResult), 'passed', protoOf(InvariantResult).td);
//endregion
//region block: init
PertCalculation_instance = new PertCalculation_0();
//endregion
//region block: exports
AdditionalCostType.values = values;
AdditionalCostType.valueOf = valueOf;
defineProp(AdditionalCostType, 'ONE_TIME', AdditionalCostType_ONE_TIME_getInstance);
defineProp(AdditionalCostType, 'RECURRING', AdditionalCostType_RECURRING_getInstance);
EstimationVersion.createFromItemGroups = createFromItemGroups;
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
  EstimationItemGroup as EstimationItemGroup,
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
