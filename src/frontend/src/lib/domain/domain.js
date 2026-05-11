(function (factory) {
  if (typeof define === 'function' && define.amd)
    define(['exports', './kotlin-kotlin-stdlib.js'], factory);
  else if (typeof exports === 'object')
    factory(module.exports, require('./kotlin-kotlin-stdlib.js'));
  else {
    if (typeof globalThis['kotlin-kotlin-stdlib'] === 'undefined') {
      throw new Error("Error loading module 'io.github.theestimator:domain'. Its dependency 'kotlin-kotlin-stdlib' was not found. Please, check whether 'kotlin-kotlin-stdlib' is loaded prior to 'io.github.theestimator:domain'.");
    }
    globalThis['io.github.theestimator:domain'] = factory(typeof globalThis['io.github.theestimator:domain'] === 'undefined' ? {} : globalThis['io.github.theestimator:domain'], globalThis['kotlin-kotlin-stdlib']);
  }
}(function (_, kotlin_kotlin) {
  'use strict';
  //region block: imports
  var imul = Math.imul;
  var VOID = kotlin_kotlin.$_$.a;
  var protoOf = kotlin_kotlin.$_$.r;
  var toString = kotlin_kotlin.$_$.w;
  var getStringHashCode = kotlin_kotlin.$_$.n;
  var getNumberHashCode = kotlin_kotlin.$_$.m;
  var THROW_CCE = kotlin_kotlin.$_$.u;
  var equals = kotlin_kotlin.$_$.l;
  var defineProp = kotlin_kotlin.$_$.k;
  var initMetadataForClass = kotlin_kotlin.$_$.p;
  var THROW_IAE = kotlin_kotlin.$_$.v;
  var Unit_instance = kotlin_kotlin.$_$.e;
  var Enum = kotlin_kotlin.$_$.t;
  var toList = kotlin_kotlin.$_$.j;
  var Companion_getInstance = kotlin_kotlin.$_$.d;
  var emptyList = kotlin_kotlin.$_$.i;
  var toString_0 = kotlin_kotlin.$_$.s;
  var hashCode = kotlin_kotlin.$_$.o;
  var ArrayList_init_$Create$ = kotlin_kotlin.$_$.c;
  var addAll = kotlin_kotlin.$_$.f;
  var collectionSizeOrDefault = kotlin_kotlin.$_$.g;
  var ArrayList_init_$Create$_0 = kotlin_kotlin.$_$.b;
  var initMetadataForObject = kotlin_kotlin.$_$.q;
  var copyToArray = kotlin_kotlin.$_$.h;
  //endregion
  //region block: pre-declaration
  initMetadataForClass(BaseDomain, 'BaseDomain');
  initMetadataForClass(AdditionalCost, 'AdditionalCost', VOID, BaseDomain);
  initMetadataForClass(AdditionalCostType, 'AdditionalCostType', VOID, Enum);
  initMetadataForClass(CalculationParameters, 'CalculationParameters', CalculationParameters);
  initMetadataForClass(EffortDriver, 'EffortDriver', VOID, BaseDomain);
  initMetadataForClass(Estimation, 'Estimation', Estimation, BaseDomain);
  initMetadataForClass(EstimationItem, 'EstimationItem', VOID, BaseDomain);
  initMetadataForClass(EstimationItemGroup, 'EstimationItemGroup', VOID, BaseDomain);
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
    this.t3_1 = description;
    this.u3_1 = amount;
    this.v3_1 = type;
    this.w3_1 = amountPerWeek;
    this.x3_1 = phase;
    this.y3_1 = _id;
    this.z3_1 = _createdAt;
    this.a4_1 = _updatedAt;
  }
  protoOf(AdditionalCost).b4 = function () {
    return this.t3_1;
  };
  protoOf(AdditionalCost).c4 = function () {
    return this.u3_1;
  };
  protoOf(AdditionalCost).d4 = function () {
    return this.v3_1;
  };
  protoOf(AdditionalCost).e4 = function () {
    return this.w3_1;
  };
  protoOf(AdditionalCost).f4 = function () {
    return this.x3_1;
  };
  protoOf(AdditionalCost).g4 = function () {
    return this.description;
  };
  protoOf(AdditionalCost).h4 = function () {
    return this.amount;
  };
  protoOf(AdditionalCost).i4 = function () {
    return this.type;
  };
  protoOf(AdditionalCost).j4 = function () {
    return this.amountPerWeek;
  };
  protoOf(AdditionalCost).k4 = function () {
    return this.phase;
  };
  protoOf(AdditionalCost).l4 = function (description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt) {
    return new AdditionalCost(description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt);
  };
  protoOf(AdditionalCost).copy = function (description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt, $super) {
    description = description === VOID ? this.description : description;
    amount = amount === VOID ? this.amount : amount;
    type = type === VOID ? this.type : type;
    amountPerWeek = amountPerWeek === VOID ? this.amountPerWeek : amountPerWeek;
    phase = phase === VOID ? this.phase : phase;
    _id = _id === VOID ? this.y3_1 : _id;
    _createdAt = _createdAt === VOID ? this.z3_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.a4_1 : _updatedAt;
    return $super === VOID ? this.l4(description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt) : $super.l4.call(this, description, amount, type, amountPerWeek, phase, _id, _createdAt, _updatedAt);
  };
  protoOf(AdditionalCost).toString = function () {
    return 'AdditionalCost(description=' + this.description + ', amount=' + this.amount + ', type=' + this.type.toString() + ', amountPerWeek=' + this.amountPerWeek + ', phase=' + toString(this.phase) + ', _id=' + this.y3_1 + ', _createdAt=' + this.z3_1 + ', _updatedAt=' + this.a4_1 + ')';
  };
  protoOf(AdditionalCost).hashCode = function () {
    var result = getStringHashCode(this.description);
    result = imul(result, 31) + getNumberHashCode(this.amount) | 0;
    result = imul(result, 31) + this.type.hashCode() | 0;
    result = imul(result, 31) + getNumberHashCode(this.amountPerWeek) | 0;
    result = imul(result, 31) + (this.phase == null ? 0 : this.phase.hashCode()) | 0;
    result = imul(result, 31) + (this.y3_1 == null ? 0 : getStringHashCode(this.y3_1)) | 0;
    result = imul(result, 31) + (this.z3_1 == null ? 0 : getStringHashCode(this.z3_1)) | 0;
    result = imul(result, 31) + (this.a4_1 == null ? 0 : getStringHashCode(this.a4_1)) | 0;
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
    if (!(this.y3_1 == tmp0_other_with_cast.y3_1))
      return false;
    if (!(this.z3_1 == tmp0_other_with_cast.z3_1))
      return false;
    if (!(this.a4_1 == tmp0_other_with_cast.a4_1))
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
    this.m4_1 = id;
    this.n4_1 = createdAt;
    this.o4_1 = updatedAt;
  }
  protoOf(BaseDomain).p4 = function () {
    return this.m4_1;
  };
  protoOf(BaseDomain).q4 = function () {
    return this.n4_1;
  };
  protoOf(BaseDomain).r4 = function () {
    return this.o4_1;
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
  protoOf(CalculationParameters).u4 = function () {
    return this.riskFactor;
  };
  protoOf(CalculationParameters).v4 = function () {
    return this.totalDriverFactor;
  };
  protoOf(CalculationParameters).w4 = function () {
    return this.dailyRate;
  };
  protoOf(CalculationParameters).x4 = function () {
    return this.salesSurcharge;
  };
  protoOf(CalculationParameters).g4 = function () {
    return this.riskFactor;
  };
  protoOf(CalculationParameters).h4 = function () {
    return this.totalDriverFactor;
  };
  protoOf(CalculationParameters).i4 = function () {
    return this.dailyRate;
  };
  protoOf(CalculationParameters).j4 = function () {
    return this.salesSurcharge;
  };
  protoOf(CalculationParameters).y4 = function (riskFactor, totalDriverFactor, dailyRate, salesSurcharge) {
    return new CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
  };
  protoOf(CalculationParameters).copy = function (riskFactor, totalDriverFactor, dailyRate, salesSurcharge, $super) {
    riskFactor = riskFactor === VOID ? this.riskFactor : riskFactor;
    totalDriverFactor = totalDriverFactor === VOID ? this.totalDriverFactor : totalDriverFactor;
    dailyRate = dailyRate === VOID ? this.dailyRate : dailyRate;
    salesSurcharge = salesSurcharge === VOID ? this.salesSurcharge : salesSurcharge;
    return $super === VOID ? this.y4(riskFactor, totalDriverFactor, dailyRate, salesSurcharge) : $super.y4.call(this, riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
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
  function createTimeRelativeItem(description, unit, minEffort, expectedEffort, maxEffort, assumptions, logicalId) {
    unit = unit === VOID ? 'h/Woche' : unit;
    minEffort = minEffort === VOID ? 0.0 : minEffort;
    expectedEffort = expectedEffort === VOID ? 0.0 : expectedEffort;
    maxEffort = maxEffort === VOID ? 0.0 : maxEffort;
    assumptions = assumptions === VOID ? '' : assumptions;
    logicalId = logicalId === VOID ? newId() : logicalId;
    return new TimeRelativeEstimationItem(unit, description, VOID, minEffort, expectedEffort, maxEffort, assumptions, VOID, logicalId);
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
    return new EstimationItemGroup(title, VOID, logicalId, toList(items));
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
    return new EstimationVersion(versionNumber, isDraft ? EstimationVersionStatus_DRAFT_getInstance() : EstimationVersionStatus_SUBMITTED_getInstance(), VOID, VOID, notes, toList(parameters), toList(effortDrivers), toList(phases), VOID, toList(itemGroups));
  }
  function newId() {
    return Companion_getInstance().l3().toString();
  }
  function EffortDriver(description, factor, comment, _id, _createdAt, _updatedAt) {
    factor = factor === VOID ? 0.0 : factor;
    comment = comment === VOID ? '' : comment;
    _id = _id === VOID ? null : _id;
    _createdAt = _createdAt === VOID ? null : _createdAt;
    _updatedAt = _updatedAt === VOID ? null : _updatedAt;
    BaseDomain.call(this, _id, _createdAt, _updatedAt);
    this.c5_1 = description;
    this.d5_1 = factor;
    this.e5_1 = comment;
    this.f5_1 = _id;
    this.g5_1 = _createdAt;
    this.h5_1 = _updatedAt;
  }
  protoOf(EffortDriver).b4 = function () {
    return this.c5_1;
  };
  protoOf(EffortDriver).i5 = function () {
    return this.d5_1;
  };
  protoOf(EffortDriver).j5 = function () {
    return this.e5_1;
  };
  protoOf(EffortDriver).g4 = function () {
    return this.description;
  };
  protoOf(EffortDriver).h4 = function () {
    return this.factor;
  };
  protoOf(EffortDriver).i4 = function () {
    return this.comment;
  };
  protoOf(EffortDriver).k5 = function (description, factor, comment, _id, _createdAt, _updatedAt) {
    return new EffortDriver(description, factor, comment, _id, _createdAt, _updatedAt);
  };
  protoOf(EffortDriver).copy = function (description, factor, comment, _id, _createdAt, _updatedAt, $super) {
    description = description === VOID ? this.description : description;
    factor = factor === VOID ? this.factor : factor;
    comment = comment === VOID ? this.comment : comment;
    _id = _id === VOID ? this.f5_1 : _id;
    _createdAt = _createdAt === VOID ? this.g5_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.h5_1 : _updatedAt;
    return $super === VOID ? this.k5(description, factor, comment, _id, _createdAt, _updatedAt) : $super.k5.call(this, description, factor, comment, _id, _createdAt, _updatedAt);
  };
  protoOf(EffortDriver).toString = function () {
    return 'EffortDriver(description=' + this.description + ', factor=' + this.factor + ', comment=' + this.comment + ', _id=' + this.f5_1 + ', _createdAt=' + this.g5_1 + ', _updatedAt=' + this.h5_1 + ')';
  };
  protoOf(EffortDriver).hashCode = function () {
    var result = getStringHashCode(this.description);
    result = imul(result, 31) + getNumberHashCode(this.factor) | 0;
    result = imul(result, 31) + getStringHashCode(this.comment) | 0;
    result = imul(result, 31) + (this.f5_1 == null ? 0 : getStringHashCode(this.f5_1)) | 0;
    result = imul(result, 31) + (this.g5_1 == null ? 0 : getStringHashCode(this.g5_1)) | 0;
    result = imul(result, 31) + (this.h5_1 == null ? 0 : getStringHashCode(this.h5_1)) | 0;
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
    if (!(this.f5_1 == tmp0_other_with_cast.f5_1))
      return false;
    if (!(this.g5_1 == tmp0_other_with_cast.g5_1))
      return false;
    if (!(this.h5_1 == tmp0_other_with_cast.h5_1))
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
    this.o5_1 = offer;
    this.p5_1 = description;
    this.q5_1 = currentVersion;
    this.r5_1 = versions;
    this.s5_1 = _id;
    this.t5_1 = _createdAt;
    this.u5_1 = _updatedAt;
  }
  protoOf(Estimation).v5 = function () {
    return this.o5_1;
  };
  protoOf(Estimation).b4 = function () {
    return this.p5_1;
  };
  protoOf(Estimation).w5 = function () {
    return this.q5_1;
  };
  protoOf(Estimation).x5 = function () {
    return this.r5_1;
  };
  protoOf(Estimation).g4 = function () {
    return this.offer;
  };
  protoOf(Estimation).h4 = function () {
    return this.description;
  };
  protoOf(Estimation).i4 = function () {
    return this.currentVersion;
  };
  protoOf(Estimation).j4 = function () {
    return this.versions;
  };
  protoOf(Estimation).y5 = function (offer, description, currentVersion, versions, _id, _createdAt, _updatedAt) {
    return new Estimation(offer, description, currentVersion, versions, _id, _createdAt, _updatedAt);
  };
  protoOf(Estimation).copy = function (offer, description, currentVersion, versions, _id, _createdAt, _updatedAt, $super) {
    offer = offer === VOID ? this.offer : offer;
    description = description === VOID ? this.description : description;
    currentVersion = currentVersion === VOID ? this.currentVersion : currentVersion;
    versions = versions === VOID ? this.versions : versions;
    _id = _id === VOID ? this.s5_1 : _id;
    _createdAt = _createdAt === VOID ? this.t5_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.u5_1 : _updatedAt;
    return $super === VOID ? this.y5(offer, description, currentVersion, versions, _id, _createdAt, _updatedAt) : $super.y5.call(this, offer, description, currentVersion, versions, _id, _createdAt, _updatedAt);
  };
  protoOf(Estimation).toString = function () {
    return 'Estimation(offer=' + this.offer + ', description=' + this.description + ', currentVersion=' + toString(this.currentVersion) + ', versions=' + toString_0(this.versions) + ', _id=' + this.s5_1 + ', _createdAt=' + this.t5_1 + ', _updatedAt=' + this.u5_1 + ')';
  };
  protoOf(Estimation).hashCode = function () {
    var result = getStringHashCode(this.offer);
    result = imul(result, 31) + getStringHashCode(this.description) | 0;
    result = imul(result, 31) + (this.currentVersion == null ? 0 : this.currentVersion.hashCode()) | 0;
    result = imul(result, 31) + hashCode(this.versions) | 0;
    result = imul(result, 31) + (this.s5_1 == null ? 0 : getStringHashCode(this.s5_1)) | 0;
    result = imul(result, 31) + (this.t5_1 == null ? 0 : getStringHashCode(this.t5_1)) | 0;
    result = imul(result, 31) + (this.u5_1 == null ? 0 : getStringHashCode(this.u5_1)) | 0;
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
    if (!(this.s5_1 == tmp0_other_with_cast.s5_1))
      return false;
    if (!(this.t5_1 == tmp0_other_with_cast.t5_1))
      return false;
    if (!(this.u5_1 == tmp0_other_with_cast.u5_1))
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
    BaseDomain.call(this, id, createdAt, updatedAt);
    this.c6_1 = description;
    this.d6_1 = code;
    this.e6_1 = minEffort;
    this.f6_1 = expectedEffort;
    this.g6_1 = maxEffort;
    this.h6_1 = assumptions;
    this.i6_1 = phase;
    this.j6_1 = logicalId;
    this.k6_1 = calculationParameters;
  }
  protoOf(EstimationItem).b4 = function () {
    return this.c6_1;
  };
  protoOf(EstimationItem).l6 = function () {
    return this.d6_1;
  };
  protoOf(EstimationItem).m6 = function () {
    return this.e6_1;
  };
  protoOf(EstimationItem).n6 = function () {
    return this.f6_1;
  };
  protoOf(EstimationItem).o6 = function () {
    return this.g6_1;
  };
  protoOf(EstimationItem).p6 = function () {
    return this.h6_1;
  };
  protoOf(EstimationItem).f4 = function () {
    return this.i6_1;
  };
  protoOf(EstimationItem).q6 = function () {
    return this.j6_1;
  };
  protoOf(EstimationItem).r6 = function () {
    return this.k6_1;
  };
  protoOf(EstimationItem).s6 = function () {
    return PertCalculation_instance.mean(this.minEffort, this.expectedEffort, this.maxEffort);
  };
  protoOf(EstimationItem).t6 = function () {
    return PertCalculation_instance.variance(this.minEffort, this.maxEffort);
  };
  protoOf(EstimationItem).u6 = function () {
    return this.mean * this.calculationParameters.riskFactor;
  };
  protoOf(EstimationItem).v6 = function () {
    return this.mean * this.calculationParameters.totalDriverFactor;
  };
  protoOf(EstimationItem).w6 = function () {
    return this.mean + this.riskSurcharge + this.driverSurcharge;
  };
  protoOf(EstimationItem).x6 = function () {
    return this.offerPT * this.calculationParameters.dailyRate;
  };
  protoOf(EstimationItem).y6 = function () {
    return this.cost * (1 + this.calculationParameters.salesSurcharge);
  };
  function EstimationItemGroup(title, phase, logicalId, items, _id, _createdAt, _updatedAt) {
    phase = phase === VOID ? null : phase;
    logicalId = logicalId === VOID ? newId() : logicalId;
    items = items === VOID ? emptyList() : items;
    _id = _id === VOID ? null : _id;
    _createdAt = _createdAt === VOID ? null : _createdAt;
    _updatedAt = _updatedAt === VOID ? null : _updatedAt;
    BaseDomain.call(this, _id, _createdAt, _updatedAt);
    this.c7_1 = title;
    this.d7_1 = phase;
    this.e7_1 = logicalId;
    this.f7_1 = items;
    this.g7_1 = _id;
    this.h7_1 = _createdAt;
    this.i7_1 = _updatedAt;
  }
  protoOf(EstimationItemGroup).j7 = function () {
    return this.c7_1;
  };
  protoOf(EstimationItemGroup).f4 = function () {
    return this.d7_1;
  };
  protoOf(EstimationItemGroup).q6 = function () {
    return this.e7_1;
  };
  protoOf(EstimationItemGroup).k7 = function () {
    return this.f7_1;
  };
  protoOf(EstimationItemGroup).g4 = function () {
    return this.title;
  };
  protoOf(EstimationItemGroup).h4 = function () {
    return this.phase;
  };
  protoOf(EstimationItemGroup).i4 = function () {
    return this.logicalId;
  };
  protoOf(EstimationItemGroup).j4 = function () {
    return this.items;
  };
  protoOf(EstimationItemGroup).l7 = function (title, phase, logicalId, items, _id, _createdAt, _updatedAt) {
    return new EstimationItemGroup(title, phase, logicalId, items, _id, _createdAt, _updatedAt);
  };
  protoOf(EstimationItemGroup).copy = function (title, phase, logicalId, items, _id, _createdAt, _updatedAt, $super) {
    title = title === VOID ? this.title : title;
    phase = phase === VOID ? this.phase : phase;
    logicalId = logicalId === VOID ? this.logicalId : logicalId;
    items = items === VOID ? this.items : items;
    _id = _id === VOID ? this.g7_1 : _id;
    _createdAt = _createdAt === VOID ? this.h7_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.i7_1 : _updatedAt;
    return $super === VOID ? this.l7(title, phase, logicalId, items, _id, _createdAt, _updatedAt) : $super.l7.call(this, title, phase, logicalId, items, _id, _createdAt, _updatedAt);
  };
  protoOf(EstimationItemGroup).toString = function () {
    return 'EstimationItemGroup(title=' + this.title + ', phase=' + toString(this.phase) + ', logicalId=' + this.logicalId + ', items=' + toString_0(this.items) + ', _id=' + this.g7_1 + ', _createdAt=' + this.h7_1 + ', _updatedAt=' + this.i7_1 + ')';
  };
  protoOf(EstimationItemGroup).hashCode = function () {
    var result = getStringHashCode(this.title);
    result = imul(result, 31) + (this.phase == null ? 0 : this.phase.hashCode()) | 0;
    result = imul(result, 31) + getStringHashCode(this.logicalId) | 0;
    result = imul(result, 31) + hashCode(this.items) | 0;
    result = imul(result, 31) + (this.g7_1 == null ? 0 : getStringHashCode(this.g7_1)) | 0;
    result = imul(result, 31) + (this.h7_1 == null ? 0 : getStringHashCode(this.h7_1)) | 0;
    result = imul(result, 31) + (this.i7_1 == null ? 0 : getStringHashCode(this.i7_1)) | 0;
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
    if (!equals(this.phase, tmp0_other_with_cast.phase))
      return false;
    if (!(this.logicalId === tmp0_other_with_cast.logicalId))
      return false;
    if (!equals(this.items, tmp0_other_with_cast.items))
      return false;
    if (!(this.g7_1 == tmp0_other_with_cast.g7_1))
      return false;
    if (!(this.h7_1 == tmp0_other_with_cast.h7_1))
      return false;
    if (!(this.i7_1 == tmp0_other_with_cast.i7_1))
      return false;
    return true;
  };
  function EstimationParameter(name, value, comment, _id, _createdAt, _updatedAt) {
    value = value === VOID ? 0.0 : value;
    comment = comment === VOID ? '' : comment;
    _id = _id === VOID ? null : _id;
    _createdAt = _createdAt === VOID ? null : _createdAt;
    _updatedAt = _updatedAt === VOID ? null : _updatedAt;
    BaseDomain.call(this, _id, _createdAt, _updatedAt);
    this.p7_1 = name;
    this.q7_1 = value;
    this.r7_1 = comment;
    this.s7_1 = _id;
    this.t7_1 = _createdAt;
    this.u7_1 = _updatedAt;
  }
  protoOf(EstimationParameter).s = function () {
    return this.p7_1;
  };
  protoOf(EstimationParameter).v7 = function () {
    return this.q7_1;
  };
  protoOf(EstimationParameter).j5 = function () {
    return this.r7_1;
  };
  protoOf(EstimationParameter).g4 = function () {
    return this.name;
  };
  protoOf(EstimationParameter).h4 = function () {
    return this.value;
  };
  protoOf(EstimationParameter).i4 = function () {
    return this.comment;
  };
  protoOf(EstimationParameter).k5 = function (name, value, comment, _id, _createdAt, _updatedAt) {
    return new EstimationParameter(name, value, comment, _id, _createdAt, _updatedAt);
  };
  protoOf(EstimationParameter).copy = function (name, value, comment, _id, _createdAt, _updatedAt, $super) {
    name = name === VOID ? this.name : name;
    value = value === VOID ? this.value : value;
    comment = comment === VOID ? this.comment : comment;
    _id = _id === VOID ? this.s7_1 : _id;
    _createdAt = _createdAt === VOID ? this.t7_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.u7_1 : _updatedAt;
    return $super === VOID ? this.k5(name, value, comment, _id, _createdAt, _updatedAt) : $super.k5.call(this, name, value, comment, _id, _createdAt, _updatedAt);
  };
  protoOf(EstimationParameter).toString = function () {
    return 'EstimationParameter(name=' + this.name + ', value=' + this.value + ', comment=' + this.comment + ', _id=' + this.s7_1 + ', _createdAt=' + this.t7_1 + ', _updatedAt=' + this.u7_1 + ')';
  };
  protoOf(EstimationParameter).hashCode = function () {
    var result = getStringHashCode(this.name);
    result = imul(result, 31) + getNumberHashCode(this.value) | 0;
    result = imul(result, 31) + getStringHashCode(this.comment) | 0;
    result = imul(result, 31) + (this.s7_1 == null ? 0 : getStringHashCode(this.s7_1)) | 0;
    result = imul(result, 31) + (this.t7_1 == null ? 0 : getStringHashCode(this.t7_1)) | 0;
    result = imul(result, 31) + (this.u7_1 == null ? 0 : getStringHashCode(this.u7_1)) | 0;
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
    if (!(this.s7_1 == tmp0_other_with_cast.s7_1))
      return false;
    if (!(this.t7_1 == tmp0_other_with_cast.t7_1))
      return false;
    if (!(this.u7_1 == tmp0_other_with_cast.u7_1))
      return false;
    return true;
  };
  function EstimationVersion(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, itemGroups, _id, _createdAt, _updatedAt) {
    status = status === VOID ? EstimationVersionStatus_DRAFT_getInstance() : status;
    createdBy = createdBy === VOID ? null : createdBy;
    totalEffort = totalEffort === VOID ? 0.0 : totalEffort;
    notes = notes === VOID ? '' : notes;
    parameters = parameters === VOID ? emptyList() : parameters;
    effortDrivers = effortDrivers === VOID ? emptyList() : effortDrivers;
    phases = phases === VOID ? emptyList() : phases;
    additionalCosts = additionalCosts === VOID ? emptyList() : additionalCosts;
    itemGroups = itemGroups === VOID ? emptyList() : itemGroups;
    _id = _id === VOID ? null : _id;
    _createdAt = _createdAt === VOID ? null : _createdAt;
    _updatedAt = _updatedAt === VOID ? null : _updatedAt;
    BaseDomain.call(this, _id, _createdAt, _updatedAt);
    this.z7_1 = versionNumber;
    this.a8_1 = status;
    this.b8_1 = createdBy;
    this.c8_1 = totalEffort;
    this.d8_1 = notes;
    this.e8_1 = parameters;
    this.f8_1 = effortDrivers;
    this.g8_1 = phases;
    this.h8_1 = additionalCosts;
    this.i8_1 = itemGroups;
    this.j8_1 = _id;
    this.k8_1 = _createdAt;
    this.l8_1 = _updatedAt;
  }
  protoOf(EstimationVersion).m8 = function () {
    return this.z7_1;
  };
  protoOf(EstimationVersion).n8 = function () {
    return this.a8_1;
  };
  protoOf(EstimationVersion).o8 = function () {
    return this.b8_1;
  };
  protoOf(EstimationVersion).p8 = function () {
    return this.c8_1;
  };
  protoOf(EstimationVersion).q8 = function () {
    return this.d8_1;
  };
  protoOf(EstimationVersion).r8 = function () {
    return this.e8_1;
  };
  protoOf(EstimationVersion).s8 = function () {
    return this.f8_1;
  };
  protoOf(EstimationVersion).t8 = function () {
    return this.g8_1;
  };
  protoOf(EstimationVersion).u8 = function () {
    return this.h8_1;
  };
  protoOf(EstimationVersion).v8 = function () {
    return this.i8_1;
  };
  protoOf(EstimationVersion).parameterValue = function (name) {
    // Inline function 'kotlin.collections.find' call
    var tmp0 = this.parameters;
    var tmp$ret$1;
    $l$block: {
      // Inline function 'kotlin.collections.firstOrNull' call
      var _iterator__ex2g4s = tmp0.d();
      while (_iterator__ex2g4s.e()) {
        var element = _iterator__ex2g4s.f();
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
    var _iterator__ex2g4s = this.effortDrivers.d();
    while (_iterator__ex2g4s.e()) {
      var element = _iterator__ex2g4s.f();
      var tmp = sum;
      sum = tmp + element.factor;
    }
    var totalDriverFactor = sum;
    // Inline function 'kotlin.collections.flatMap' call
    var tmp0 = this.itemGroups;
    // Inline function 'kotlin.collections.flatMapTo' call
    var destination = ArrayList_init_$Create$();
    var _iterator__ex2g4s_0 = tmp0.d();
    while (_iterator__ex2g4s_0.e()) {
      var element_0 = _iterator__ex2g4s_0.f();
      var list = element_0.items;
      addAll(destination, list);
    }
    var allItems = destination;
    // Inline function 'kotlin.collections.sumOf' call
    var sum_0 = 0;
    var _iterator__ex2g4s_1 = allItems.d();
    while (_iterator__ex2g4s_1.e()) {
      var element_1 = _iterator__ex2g4s_1.f();
      var tmp_0 = sum_0;
      sum_0 = tmp_0 + element_1.variance;
    }
    var totalVariance = sum_0;
    // Inline function 'kotlin.collections.sumOf' call
    var sum_1 = 0;
    var _iterator__ex2g4s_2 = allItems.d();
    while (_iterator__ex2g4s_2.e()) {
      var element_2 = _iterator__ex2g4s_2.f();
      var tmp_1 = sum_1;
      sum_1 = tmp_1 + element_2.mean;
    }
    var totalMean = sum_1;
    var riskFactor = PertCalculation_instance.riskFactor(totalMean, totalVariance, stdDevFactor);
    var params = new CalculationParameters(riskFactor, totalDriverFactor, dailyRate, salesSurcharge);
    // Inline function 'kotlin.collections.map' call
    var this_0 = this.itemGroups;
    // Inline function 'kotlin.collections.mapTo' call
    var destination_0 = ArrayList_init_$Create$_0(collectionSizeOrDefault(this_0, 10));
    var _iterator__ex2g4s_3 = this_0.d();
    while (_iterator__ex2g4s_3.e()) {
      var item = _iterator__ex2g4s_3.f();
      // Inline function 'kotlin.collections.map' call
      var this_1 = item.items;
      // Inline function 'kotlin.collections.mapTo' call
      var destination_1 = ArrayList_init_$Create$_0(collectionSizeOrDefault(this_1, 10));
      var _iterator__ex2g4s_4 = this_1.d();
      while (_iterator__ex2g4s_4.e()) {
        var item_0 = _iterator__ex2g4s_4.f();
        var tmp$ret$9 = item_0.withCalculationParameters(params);
        destination_1.r1(tmp$ret$9);
      }
      var tmp$ret$12 = item.copy(VOID, VOID, VOID, destination_1);
      destination_0.r1(tmp$ret$12);
    }
    var newGroups = destination_0;
    // Inline function 'kotlin.collections.flatMap' call
    // Inline function 'kotlin.collections.flatMapTo' call
    var destination_2 = ArrayList_init_$Create$();
    var _iterator__ex2g4s_5 = newGroups.d();
    while (_iterator__ex2g4s_5.e()) {
      var element_3 = _iterator__ex2g4s_5.f();
      var list_0 = element_3.items;
      addAll(destination_2, list_0);
    }
    // Inline function 'kotlin.collections.sumOf' call
    var sum_2 = 0;
    var _iterator__ex2g4s_6 = destination_2.d();
    while (_iterator__ex2g4s_6.e()) {
      var element_4 = _iterator__ex2g4s_6.f();
      var tmp_2 = sum_2;
      sum_2 = tmp_2 + element_4.offerPT;
    }
    var newTotalEffort = sum_2;
    return this.copy(VOID, VOID, VOID, newTotalEffort, VOID, VOID, VOID, VOID, VOID, newGroups);
  };
  protoOf(EstimationVersion).g4 = function () {
    return this.versionNumber;
  };
  protoOf(EstimationVersion).h4 = function () {
    return this.status;
  };
  protoOf(EstimationVersion).i4 = function () {
    return this.createdBy;
  };
  protoOf(EstimationVersion).j4 = function () {
    return this.totalEffort;
  };
  protoOf(EstimationVersion).k4 = function () {
    return this.notes;
  };
  protoOf(EstimationVersion).w8 = function () {
    return this.parameters;
  };
  protoOf(EstimationVersion).x8 = function () {
    return this.effortDrivers;
  };
  protoOf(EstimationVersion).y8 = function () {
    return this.phases;
  };
  protoOf(EstimationVersion).z8 = function () {
    return this.additionalCosts;
  };
  protoOf(EstimationVersion).a9 = function () {
    return this.itemGroups;
  };
  protoOf(EstimationVersion).b9 = function (versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, itemGroups, _id, _createdAt, _updatedAt) {
    return new EstimationVersion(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, itemGroups, _id, _createdAt, _updatedAt);
  };
  protoOf(EstimationVersion).copy = function (versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, itemGroups, _id, _createdAt, _updatedAt, $super) {
    versionNumber = versionNumber === VOID ? this.versionNumber : versionNumber;
    status = status === VOID ? this.status : status;
    createdBy = createdBy === VOID ? this.createdBy : createdBy;
    totalEffort = totalEffort === VOID ? this.totalEffort : totalEffort;
    notes = notes === VOID ? this.notes : notes;
    parameters = parameters === VOID ? this.parameters : parameters;
    effortDrivers = effortDrivers === VOID ? this.effortDrivers : effortDrivers;
    phases = phases === VOID ? this.phases : phases;
    additionalCosts = additionalCosts === VOID ? this.additionalCosts : additionalCosts;
    itemGroups = itemGroups === VOID ? this.itemGroups : itemGroups;
    _id = _id === VOID ? this.j8_1 : _id;
    _createdAt = _createdAt === VOID ? this.k8_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.l8_1 : _updatedAt;
    return $super === VOID ? this.b9(versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, itemGroups, _id, _createdAt, _updatedAt) : $super.b9.call(this, versionNumber, status, createdBy, totalEffort, notes, parameters, effortDrivers, phases, additionalCosts, itemGroups, _id, _createdAt, _updatedAt);
  };
  protoOf(EstimationVersion).toString = function () {
    return 'EstimationVersion(versionNumber=' + this.versionNumber + ', status=' + this.status.toString() + ', createdBy=' + toString(this.createdBy) + ', totalEffort=' + this.totalEffort + ', notes=' + this.notes + ', parameters=' + toString_0(this.parameters) + ', effortDrivers=' + toString_0(this.effortDrivers) + ', phases=' + toString_0(this.phases) + ', additionalCosts=' + toString_0(this.additionalCosts) + ', itemGroups=' + toString_0(this.itemGroups) + ', _id=' + this.j8_1 + ', _createdAt=' + this.k8_1 + ', _updatedAt=' + this.l8_1 + ')';
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
    result = imul(result, 31) + hashCode(this.itemGroups) | 0;
    result = imul(result, 31) + (this.j8_1 == null ? 0 : getStringHashCode(this.j8_1)) | 0;
    result = imul(result, 31) + (this.k8_1 == null ? 0 : getStringHashCode(this.k8_1)) | 0;
    result = imul(result, 31) + (this.l8_1 == null ? 0 : getStringHashCode(this.l8_1)) | 0;
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
    if (!equals(this.itemGroups, tmp0_other_with_cast.itemGroups))
      return false;
    if (!(this.j8_1 == tmp0_other_with_cast.j8_1))
      return false;
    if (!(this.k8_1 == tmp0_other_with_cast.k8_1))
      return false;
    if (!(this.l8_1 == tmp0_other_with_cast.l8_1))
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
    this.q9_1 = _description;
    this.r9_1 = _code;
    this.s9_1 = _minEffort;
    this.t9_1 = _expectedEffort;
    this.u9_1 = _maxEffort;
    this.v9_1 = _assumptions;
    this.w9_1 = _phase;
    this.x9_1 = _logicalId;
    this.y9_1 = _calculationParameters;
    this.z9_1 = _id;
    this.aa_1 = _createdAt;
    this.ba_1 = _updatedAt;
  }
  protoOf(FixedEstimationItem).withCalculationParameters = function (params) {
    return this.copy(VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, params);
  };
  protoOf(FixedEstimationItem).ca = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
    return new FixedEstimationItem(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
  };
  protoOf(FixedEstimationItem).copy = function (_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
    _description = _description === VOID ? this.q9_1 : _description;
    _code = _code === VOID ? this.r9_1 : _code;
    _minEffort = _minEffort === VOID ? this.s9_1 : _minEffort;
    _expectedEffort = _expectedEffort === VOID ? this.t9_1 : _expectedEffort;
    _maxEffort = _maxEffort === VOID ? this.u9_1 : _maxEffort;
    _assumptions = _assumptions === VOID ? this.v9_1 : _assumptions;
    _phase = _phase === VOID ? this.w9_1 : _phase;
    _logicalId = _logicalId === VOID ? this.x9_1 : _logicalId;
    _calculationParameters = _calculationParameters === VOID ? this.y9_1 : _calculationParameters;
    _id = _id === VOID ? this.z9_1 : _id;
    _createdAt = _createdAt === VOID ? this.aa_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.ba_1 : _updatedAt;
    return $super === VOID ? this.ca(_description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.ca.call(this, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
  };
  protoOf(FixedEstimationItem).toString = function () {
    return 'FixedEstimationItem(_description=' + this.q9_1 + ', _code=' + this.r9_1 + ', _minEffort=' + this.s9_1 + ', _expectedEffort=' + this.t9_1 + ', _maxEffort=' + this.u9_1 + ', _assumptions=' + this.v9_1 + ', _phase=' + toString(this.w9_1) + ', _logicalId=' + this.x9_1 + ', _calculationParameters=' + this.y9_1.toString() + ', _id=' + this.z9_1 + ', _createdAt=' + this.aa_1 + ', _updatedAt=' + this.ba_1 + ')';
  };
  protoOf(FixedEstimationItem).hashCode = function () {
    var result = getStringHashCode(this.q9_1);
    result = imul(result, 31) + getStringHashCode(this.r9_1) | 0;
    result = imul(result, 31) + getNumberHashCode(this.s9_1) | 0;
    result = imul(result, 31) + getNumberHashCode(this.t9_1) | 0;
    result = imul(result, 31) + getNumberHashCode(this.u9_1) | 0;
    result = imul(result, 31) + getStringHashCode(this.v9_1) | 0;
    result = imul(result, 31) + (this.w9_1 == null ? 0 : this.w9_1.hashCode()) | 0;
    result = imul(result, 31) + getStringHashCode(this.x9_1) | 0;
    result = imul(result, 31) + this.y9_1.hashCode() | 0;
    result = imul(result, 31) + (this.z9_1 == null ? 0 : getStringHashCode(this.z9_1)) | 0;
    result = imul(result, 31) + (this.aa_1 == null ? 0 : getStringHashCode(this.aa_1)) | 0;
    result = imul(result, 31) + (this.ba_1 == null ? 0 : getStringHashCode(this.ba_1)) | 0;
    return result;
  };
  protoOf(FixedEstimationItem).equals = function (other) {
    if (this === other)
      return true;
    if (!(other instanceof FixedEstimationItem))
      return false;
    var tmp0_other_with_cast = other instanceof FixedEstimationItem ? other : THROW_CCE();
    if (!(this.q9_1 === tmp0_other_with_cast.q9_1))
      return false;
    if (!(this.r9_1 === tmp0_other_with_cast.r9_1))
      return false;
    if (!equals(this.s9_1, tmp0_other_with_cast.s9_1))
      return false;
    if (!equals(this.t9_1, tmp0_other_with_cast.t9_1))
      return false;
    if (!equals(this.u9_1, tmp0_other_with_cast.u9_1))
      return false;
    if (!(this.v9_1 === tmp0_other_with_cast.v9_1))
      return false;
    if (!equals(this.w9_1, tmp0_other_with_cast.w9_1))
      return false;
    if (!(this.x9_1 === tmp0_other_with_cast.x9_1))
      return false;
    if (!this.y9_1.equals(tmp0_other_with_cast.y9_1))
      return false;
    if (!(this.z9_1 == tmp0_other_with_cast.z9_1))
      return false;
    if (!(this.aa_1 == tmp0_other_with_cast.aa_1))
      return false;
    if (!(this.ba_1 == tmp0_other_with_cast.ba_1))
      return false;
    return true;
  };
  function PertCalculation() {
  }
  protoOf(PertCalculation).mean = function (min, expected, max) {
    return (min + 4 * expected + max) / 6.0;
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
    this.ga_1 = name;
    this.ha_1 = description;
    this.ia_1 = client;
    this.ja_1 = status;
    this.ka_1 = owner;
    this.la_1 = _id;
    this.ma_1 = _createdAt;
    this.na_1 = _updatedAt;
  }
  protoOf(Project).s = function () {
    return this.ga_1;
  };
  protoOf(Project).b4 = function () {
    return this.ha_1;
  };
  protoOf(Project).oa = function () {
    return this.ia_1;
  };
  protoOf(Project).n8 = function () {
    return this.ja_1;
  };
  protoOf(Project).pa = function () {
    return this.ka_1;
  };
  protoOf(Project).g4 = function () {
    return this.name;
  };
  protoOf(Project).h4 = function () {
    return this.description;
  };
  protoOf(Project).i4 = function () {
    return this.client;
  };
  protoOf(Project).j4 = function () {
    return this.status;
  };
  protoOf(Project).k4 = function () {
    return this.owner;
  };
  protoOf(Project).qa = function (name, description, client, status, owner, _id, _createdAt, _updatedAt) {
    return new Project(name, description, client, status, owner, _id, _createdAt, _updatedAt);
  };
  protoOf(Project).copy = function (name, description, client, status, owner, _id, _createdAt, _updatedAt, $super) {
    name = name === VOID ? this.name : name;
    description = description === VOID ? this.description : description;
    client = client === VOID ? this.client : client;
    status = status === VOID ? this.status : status;
    owner = owner === VOID ? this.owner : owner;
    _id = _id === VOID ? this.la_1 : _id;
    _createdAt = _createdAt === VOID ? this.ma_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.na_1 : _updatedAt;
    return $super === VOID ? this.qa(name, description, client, status, owner, _id, _createdAt, _updatedAt) : $super.qa.call(this, name, description, client, status, owner, _id, _createdAt, _updatedAt);
  };
  protoOf(Project).toString = function () {
    return 'Project(name=' + this.name + ', description=' + this.description + ', client=' + this.client + ', status=' + this.status.toString() + ', owner=' + toString(this.owner) + ', _id=' + this.la_1 + ', _createdAt=' + this.ma_1 + ', _updatedAt=' + this.na_1 + ')';
  };
  protoOf(Project).hashCode = function () {
    var result = getStringHashCode(this.name);
    result = imul(result, 31) + getStringHashCode(this.description) | 0;
    result = imul(result, 31) + getStringHashCode(this.client) | 0;
    result = imul(result, 31) + this.status.hashCode() | 0;
    result = imul(result, 31) + (this.owner == null ? 0 : this.owner.hashCode()) | 0;
    result = imul(result, 31) + (this.la_1 == null ? 0 : getStringHashCode(this.la_1)) | 0;
    result = imul(result, 31) + (this.ma_1 == null ? 0 : getStringHashCode(this.ma_1)) | 0;
    result = imul(result, 31) + (this.na_1 == null ? 0 : getStringHashCode(this.na_1)) | 0;
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
    if (!(this.la_1 == tmp0_other_with_cast.la_1))
      return false;
    if (!(this.ma_1 == tmp0_other_with_cast.ma_1))
      return false;
    if (!(this.na_1 == tmp0_other_with_cast.na_1))
      return false;
    return true;
  };
  function ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
    durationWeeks = durationWeeks === VOID ? 0.0 : durationWeeks;
    _id = _id === VOID ? null : _id;
    _createdAt = _createdAt === VOID ? null : _createdAt;
    _updatedAt = _updatedAt === VOID ? null : _updatedAt;
    BaseDomain.call(this, _id, _createdAt, _updatedAt);
    this.ua_1 = name;
    this.va_1 = abbreviation;
    this.wa_1 = durationWeeks;
    this.xa_1 = _id;
    this.ya_1 = _createdAt;
    this.za_1 = _updatedAt;
  }
  protoOf(ProjectPhase).s = function () {
    return this.ua_1;
  };
  protoOf(ProjectPhase).ab = function () {
    return this.va_1;
  };
  protoOf(ProjectPhase).bb = function () {
    return this.wa_1;
  };
  protoOf(ProjectPhase).g4 = function () {
    return this.name;
  };
  protoOf(ProjectPhase).h4 = function () {
    return this.abbreviation;
  };
  protoOf(ProjectPhase).i4 = function () {
    return this.durationWeeks;
  };
  protoOf(ProjectPhase).cb = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) {
    return new ProjectPhase(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
  };
  protoOf(ProjectPhase).copy = function (name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt, $super) {
    name = name === VOID ? this.name : name;
    abbreviation = abbreviation === VOID ? this.abbreviation : abbreviation;
    durationWeeks = durationWeeks === VOID ? this.durationWeeks : durationWeeks;
    _id = _id === VOID ? this.xa_1 : _id;
    _createdAt = _createdAt === VOID ? this.ya_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.za_1 : _updatedAt;
    return $super === VOID ? this.cb(name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt) : $super.cb.call(this, name, abbreviation, durationWeeks, _id, _createdAt, _updatedAt);
  };
  protoOf(ProjectPhase).toString = function () {
    return 'ProjectPhase(name=' + this.name + ', abbreviation=' + this.abbreviation + ', durationWeeks=' + this.durationWeeks + ', _id=' + this.xa_1 + ', _createdAt=' + this.ya_1 + ', _updatedAt=' + this.za_1 + ')';
  };
  protoOf(ProjectPhase).hashCode = function () {
    var result = getStringHashCode(this.name);
    result = imul(result, 31) + getStringHashCode(this.abbreviation) | 0;
    result = imul(result, 31) + getNumberHashCode(this.durationWeeks) | 0;
    result = imul(result, 31) + (this.xa_1 == null ? 0 : getStringHashCode(this.xa_1)) | 0;
    result = imul(result, 31) + (this.ya_1 == null ? 0 : getStringHashCode(this.ya_1)) | 0;
    result = imul(result, 31) + (this.za_1 == null ? 0 : getStringHashCode(this.za_1)) | 0;
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
    if (!(this.xa_1 == tmp0_other_with_cast.xa_1))
      return false;
    if (!(this.ya_1 == tmp0_other_with_cast.ya_1))
      return false;
    if (!(this.za_1 == tmp0_other_with_cast.za_1))
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
    this.rb_1 = unit;
    this.sb_1 = _description;
    this.tb_1 = _code;
    this.ub_1 = _minEffort;
    this.vb_1 = _expectedEffort;
    this.wb_1 = _maxEffort;
    this.xb_1 = _assumptions;
    this.yb_1 = _phase;
    this.zb_1 = _logicalId;
    this.ac_1 = _calculationParameters;
    this.bc_1 = _id;
    this.cc_1 = _createdAt;
    this.dc_1 = _updatedAt;
  }
  protoOf(TimeRelativeEstimationItem).ec = function () {
    return this.rb_1;
  };
  protoOf(TimeRelativeEstimationItem).withCalculationParameters = function (params) {
    return this.copy(VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, VOID, params);
  };
  protoOf(TimeRelativeEstimationItem).g4 = function () {
    return this.unit;
  };
  protoOf(TimeRelativeEstimationItem).fc = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) {
    return new TimeRelativeEstimationItem(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
  };
  protoOf(TimeRelativeEstimationItem).copy = function (unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt, $super) {
    unit = unit === VOID ? this.unit : unit;
    _description = _description === VOID ? this.sb_1 : _description;
    _code = _code === VOID ? this.tb_1 : _code;
    _minEffort = _minEffort === VOID ? this.ub_1 : _minEffort;
    _expectedEffort = _expectedEffort === VOID ? this.vb_1 : _expectedEffort;
    _maxEffort = _maxEffort === VOID ? this.wb_1 : _maxEffort;
    _assumptions = _assumptions === VOID ? this.xb_1 : _assumptions;
    _phase = _phase === VOID ? this.yb_1 : _phase;
    _logicalId = _logicalId === VOID ? this.zb_1 : _logicalId;
    _calculationParameters = _calculationParameters === VOID ? this.ac_1 : _calculationParameters;
    _id = _id === VOID ? this.bc_1 : _id;
    _createdAt = _createdAt === VOID ? this.cc_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.dc_1 : _updatedAt;
    return $super === VOID ? this.fc(unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt) : $super.fc.call(this, unit, _description, _code, _minEffort, _expectedEffort, _maxEffort, _assumptions, _phase, _logicalId, _calculationParameters, _id, _createdAt, _updatedAt);
  };
  protoOf(TimeRelativeEstimationItem).toString = function () {
    return 'TimeRelativeEstimationItem(unit=' + this.unit + ', _description=' + this.sb_1 + ', _code=' + this.tb_1 + ', _minEffort=' + this.ub_1 + ', _expectedEffort=' + this.vb_1 + ', _maxEffort=' + this.wb_1 + ', _assumptions=' + this.xb_1 + ', _phase=' + toString(this.yb_1) + ', _logicalId=' + this.zb_1 + ', _calculationParameters=' + this.ac_1.toString() + ', _id=' + this.bc_1 + ', _createdAt=' + this.cc_1 + ', _updatedAt=' + this.dc_1 + ')';
  };
  protoOf(TimeRelativeEstimationItem).hashCode = function () {
    var result = getStringHashCode(this.unit);
    result = imul(result, 31) + getStringHashCode(this.sb_1) | 0;
    result = imul(result, 31) + getStringHashCode(this.tb_1) | 0;
    result = imul(result, 31) + getNumberHashCode(this.ub_1) | 0;
    result = imul(result, 31) + getNumberHashCode(this.vb_1) | 0;
    result = imul(result, 31) + getNumberHashCode(this.wb_1) | 0;
    result = imul(result, 31) + getStringHashCode(this.xb_1) | 0;
    result = imul(result, 31) + (this.yb_1 == null ? 0 : this.yb_1.hashCode()) | 0;
    result = imul(result, 31) + getStringHashCode(this.zb_1) | 0;
    result = imul(result, 31) + this.ac_1.hashCode() | 0;
    result = imul(result, 31) + (this.bc_1 == null ? 0 : getStringHashCode(this.bc_1)) | 0;
    result = imul(result, 31) + (this.cc_1 == null ? 0 : getStringHashCode(this.cc_1)) | 0;
    result = imul(result, 31) + (this.dc_1 == null ? 0 : getStringHashCode(this.dc_1)) | 0;
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
    if (!(this.sb_1 === tmp0_other_with_cast.sb_1))
      return false;
    if (!(this.tb_1 === tmp0_other_with_cast.tb_1))
      return false;
    if (!equals(this.ub_1, tmp0_other_with_cast.ub_1))
      return false;
    if (!equals(this.vb_1, tmp0_other_with_cast.vb_1))
      return false;
    if (!equals(this.wb_1, tmp0_other_with_cast.wb_1))
      return false;
    if (!(this.xb_1 === tmp0_other_with_cast.xb_1))
      return false;
    if (!equals(this.yb_1, tmp0_other_with_cast.yb_1))
      return false;
    if (!(this.zb_1 === tmp0_other_with_cast.zb_1))
      return false;
    if (!this.ac_1.equals(tmp0_other_with_cast.ac_1))
      return false;
    if (!(this.bc_1 == tmp0_other_with_cast.bc_1))
      return false;
    if (!(this.cc_1 == tmp0_other_with_cast.cc_1))
      return false;
    if (!(this.dc_1 == tmp0_other_with_cast.dc_1))
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
    this.jc_1 = entraSubjectId;
    this.kc_1 = displayName;
    this.lc_1 = _id;
    this.mc_1 = _createdAt;
    this.nc_1 = _updatedAt;
  }
  protoOf(User).oc = function () {
    return this.jc_1;
  };
  protoOf(User).pc = function () {
    return this.kc_1;
  };
  protoOf(User).g4 = function () {
    return this.entraSubjectId;
  };
  protoOf(User).h4 = function () {
    return this.displayName;
  };
  protoOf(User).qc = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt) {
    return new User(entraSubjectId, displayName, _id, _createdAt, _updatedAt);
  };
  protoOf(User).copy = function (entraSubjectId, displayName, _id, _createdAt, _updatedAt, $super) {
    entraSubjectId = entraSubjectId === VOID ? this.entraSubjectId : entraSubjectId;
    displayName = displayName === VOID ? this.displayName : displayName;
    _id = _id === VOID ? this.lc_1 : _id;
    _createdAt = _createdAt === VOID ? this.mc_1 : _createdAt;
    _updatedAt = _updatedAt === VOID ? this.nc_1 : _updatedAt;
    return $super === VOID ? this.qc(entraSubjectId, displayName, _id, _createdAt, _updatedAt) : $super.qc.call(this, entraSubjectId, displayName, _id, _createdAt, _updatedAt);
  };
  protoOf(User).toString = function () {
    return 'User(entraSubjectId=' + this.entraSubjectId + ', displayName=' + this.displayName + ', _id=' + this.lc_1 + ', _createdAt=' + this.mc_1 + ', _updatedAt=' + this.nc_1 + ')';
  };
  protoOf(User).hashCode = function () {
    var result = this.entraSubjectId == null ? 0 : getStringHashCode(this.entraSubjectId);
    result = imul(result, 31) + getStringHashCode(this.displayName) | 0;
    result = imul(result, 31) + (this.lc_1 == null ? 0 : getStringHashCode(this.lc_1)) | 0;
    result = imul(result, 31) + (this.mc_1 == null ? 0 : getStringHashCode(this.mc_1)) | 0;
    result = imul(result, 31) + (this.nc_1 == null ? 0 : getStringHashCode(this.nc_1)) | 0;
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
    if (!(this.lc_1 == tmp0_other_with_cast.lc_1))
      return false;
    if (!(this.mc_1 == tmp0_other_with_cast.mc_1))
      return false;
    if (!(this.nc_1 == tmp0_other_with_cast.nc_1))
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
    var results = ArrayList_init_$Create$();
    // Inline function 'kotlin.collections.flatMap' call
    var tmp0 = version.itemGroups;
    // Inline function 'kotlin.collections.flatMapTo' call
    var destination = ArrayList_init_$Create$();
    var _iterator__ex2g4s = tmp0.d();
    while (_iterator__ex2g4s.e()) {
      var element = _iterator__ex2g4s.f();
      var list = element.items;
      addAll(destination, list);
    }
    var allItems = destination;
    var tolerance = 0.2;
    // Inline function 'kotlin.collections.sumOf' call
    var sum = 0;
    var _iterator__ex2g4s_0 = allItems.d();
    while (_iterator__ex2g4s_0.e()) {
      var element_0 = _iterator__ex2g4s_0.f();
      var tmp = sum;
      sum = tmp + element_0.offerPT;
    }
    var totalOfferPT = sum;
    results.r1(new InvariantResult('Gesamtaufwand = Summe aller AngebotsPT', version.totalEffort - totalOfferPT, tolerance));
    // Inline function 'kotlin.collections.sumOf' call
    var sum_0 = 0;
    var _iterator__ex2g4s_1 = allItems.d();
    while (_iterator__ex2g4s_1.e()) {
      var element_1 = _iterator__ex2g4s_1.f();
      var tmp_0 = sum_0;
      sum_0 = tmp_0 + element_1.mean;
    }
    var totalMean = sum_0;
    // Inline function 'kotlin.collections.sumOf' call
    var sum_1 = 0;
    var _iterator__ex2g4s_2 = allItems.d();
    while (_iterator__ex2g4s_2.e()) {
      var element_2 = _iterator__ex2g4s_2.f();
      var tmp_1 = sum_1;
      sum_1 = tmp_1 + element_2.variance;
    }
    var totalVariance = sum_1;
    var tmp0_elvis_lhs = version.parameterValue('Standardabweichungsfaktor');
    var stdDevFactor = tmp0_elvis_lhs == null ? 2.0 : tmp0_elvis_lhs;
    // Inline function 'kotlin.collections.sumOf' call
    var sum_2 = 0;
    var _iterator__ex2g4s_3 = version.effortDrivers.d();
    while (_iterator__ex2g4s_3.e()) {
      var element_3 = _iterator__ex2g4s_3.f();
      var tmp_2 = sum_2;
      sum_2 = tmp_2 + element_3.factor;
    }
    var totalDriverFactor = sum_2;
    // Inline function 'kotlin.math.sqrt' call
    var calculatedTotal = totalMean + Math.sqrt(totalVariance) * stdDevFactor + totalMean * totalDriverFactor;
    results.r1(new InvariantResult('Summe mit Risiko im PSP = Summe bei Berechnung', totalOfferPT - calculatedTotal, tolerance));
    // Inline function 'kotlin.collections.sumOf' call
    var sum_3 = 0;
    var _iterator__ex2g4s_4 = version.itemGroups.d();
    while (_iterator__ex2g4s_4.e()) {
      var element_4 = _iterator__ex2g4s_4.f();
      var tmp_3 = sum_3;
      // Inline function 'kotlin.collections.sumOf' call
      var sum_4 = 0;
      var _iterator__ex2g4s_5 = element_4.items.d();
      while (_iterator__ex2g4s_5.e()) {
        var element_5 = _iterator__ex2g4s_5.f();
        var tmp_4 = sum_4;
        sum_4 = tmp_4 + element_5.offerPT;
      }
      sum_3 = tmp_3 + sum_4;
    }
    var sumByGroups = sum_3;
    results.r1(new InvariantResult('Summe \xFCber Arbeitspakete = Summe \xFCber Teilsummen', totalOfferPT - sumByGroups, tolerance));
    // Inline function 'kotlin.collections.sumOf' call
    var sum_5 = 0;
    var _iterator__ex2g4s_6 = allItems.d();
    while (_iterator__ex2g4s_6.e()) {
      var element_6 = _iterator__ex2g4s_6.f();
      var tmp_5 = sum_5;
      sum_5 = tmp_5 + element_6.cost;
    }
    var totalCost = sum_5;
    var tmp1_elvis_lhs = version.parameterValue('Tagessatz');
    var dailyRate = tmp1_elvis_lhs == null ? 800.0 : tmp1_elvis_lhs;
    var costFromEffort = totalOfferPT * dailyRate;
    results.r1(new InvariantResult('Kosten im PSP = Kosten in der Paket\xFCbersicht', totalCost - costFromEffort, tolerance));
    // Inline function 'kotlin.collections.sumOf' call
    var sum_6 = 0;
    var _iterator__ex2g4s_7 = allItems.d();
    while (_iterator__ex2g4s_7.e()) {
      var element_7 = _iterator__ex2g4s_7.f();
      var tmp_6 = sum_6;
      sum_6 = tmp_6 + element_7.variance;
    }
    var varianceTotal = sum_6;
    // Inline function 'kotlin.collections.sumOf' call
    var sum_7 = 0;
    var _iterator__ex2g4s_8 = version.itemGroups.d();
    while (_iterator__ex2g4s_8.e()) {
      var element_8 = _iterator__ex2g4s_8.f();
      var tmp_7 = sum_7;
      // Inline function 'kotlin.collections.sumOf' call
      var sum_8 = 0;
      var _iterator__ex2g4s_9 = element_8.items.d();
      while (_iterator__ex2g4s_9.e()) {
        var element_9 = _iterator__ex2g4s_9.f();
        var tmp_8 = sum_8;
        sum_8 = tmp_8 + element_9.variance;
      }
      sum_7 = tmp_7 + sum_8;
    }
    var varianceByGroups = sum_7;
    results.r1(new InvariantResult('Summe der Varianzen = Summe der Varianzen der Gruppen', varianceTotal - varianceByGroups, tolerance));
    // Inline function 'kotlin.collections.toTypedArray' call
    return copyToArray(results);
  };
  function InvariantResult(description, difference, tolerance) {
    this.description = description;
    this.difference = difference;
    this.tolerance = tolerance;
  }
  protoOf(InvariantResult).b4 = function () {
    return this.description;
  };
  protoOf(InvariantResult).rc = function () {
    return this.difference;
  };
  protoOf(InvariantResult).sc = function () {
    return this.tolerance;
  };
  protoOf(InvariantResult).tc = function () {
    // Inline function 'kotlin.math.abs' call
    var x = this.difference;
    return Math.abs(x) <= this.tolerance;
  };
  protoOf(InvariantResult).g4 = function () {
    return this.description;
  };
  protoOf(InvariantResult).h4 = function () {
    return this.difference;
  };
  protoOf(InvariantResult).i4 = function () {
    return this.tolerance;
  };
  protoOf(InvariantResult).uc = function (description, difference, tolerance) {
    return new InvariantResult(description, difference, tolerance);
  };
  protoOf(InvariantResult).copy = function (description, difference, tolerance, $super) {
    description = description === VOID ? this.description : description;
    difference = difference === VOID ? this.difference : difference;
    tolerance = tolerance === VOID ? this.tolerance : tolerance;
    return $super === VOID ? this.uc(description, difference, tolerance) : $super.uc.call(this, description, difference, tolerance);
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
    return this.p4();
  });
  defineProp(protoOf(BaseDomain), 'createdAt', function () {
    return this.q4();
  });
  defineProp(protoOf(BaseDomain), 'updatedAt', function () {
    return this.r4();
  });
  defineProp(protoOf(AdditionalCost), 'description', function () {
    return this.b4();
  });
  defineProp(protoOf(AdditionalCost), 'amount', function () {
    return this.c4();
  });
  defineProp(protoOf(AdditionalCost), 'type', function () {
    return this.d4();
  });
  defineProp(protoOf(AdditionalCost), 'amountPerWeek', function () {
    return this.e4();
  });
  defineProp(protoOf(AdditionalCost), 'phase', function () {
    return this.f4();
  });
  defineProp(protoOf(AdditionalCostType), 'name', protoOf(AdditionalCostType).s);
  defineProp(protoOf(AdditionalCostType), 'ordinal', protoOf(AdditionalCostType).t);
  defineProp(protoOf(EffortDriver), 'description', function () {
    return this.b4();
  });
  defineProp(protoOf(EffortDriver), 'factor', function () {
    return this.i5();
  });
  defineProp(protoOf(EffortDriver), 'comment', function () {
    return this.j5();
  });
  defineProp(protoOf(Estimation), 'offer', function () {
    return this.v5();
  });
  defineProp(protoOf(Estimation), 'description', function () {
    return this.b4();
  });
  defineProp(protoOf(Estimation), 'currentVersion', function () {
    return this.w5();
  });
  defineProp(protoOf(Estimation), 'versions', function () {
    return this.x5();
  });
  defineProp(protoOf(EstimationItem), 'description', function () {
    return this.b4();
  });
  defineProp(protoOf(EstimationItem), 'code', function () {
    return this.l6();
  });
  defineProp(protoOf(EstimationItem), 'minEffort', function () {
    return this.m6();
  });
  defineProp(protoOf(EstimationItem), 'expectedEffort', function () {
    return this.n6();
  });
  defineProp(protoOf(EstimationItem), 'maxEffort', function () {
    return this.o6();
  });
  defineProp(protoOf(EstimationItem), 'assumptions', function () {
    return this.p6();
  });
  defineProp(protoOf(EstimationItem), 'phase', function () {
    return this.f4();
  });
  defineProp(protoOf(EstimationItem), 'logicalId', function () {
    return this.q6();
  });
  defineProp(protoOf(EstimationItem), 'calculationParameters', function () {
    return this.r6();
  });
  defineProp(protoOf(EstimationItem), 'mean', function () {
    return this.s6();
  });
  defineProp(protoOf(EstimationItem), 'variance', function () {
    return this.t6();
  });
  defineProp(protoOf(EstimationItem), 'riskSurcharge', function () {
    return this.u6();
  });
  defineProp(protoOf(EstimationItem), 'driverSurcharge', function () {
    return this.v6();
  });
  defineProp(protoOf(EstimationItem), 'offerPT', function () {
    return this.w6();
  });
  defineProp(protoOf(EstimationItem), 'cost', function () {
    return this.x6();
  });
  defineProp(protoOf(EstimationItem), 'offerPrice', function () {
    return this.y6();
  });
  defineProp(protoOf(EstimationItemGroup), 'title', function () {
    return this.j7();
  });
  defineProp(protoOf(EstimationItemGroup), 'phase', function () {
    return this.f4();
  });
  defineProp(protoOf(EstimationItemGroup), 'logicalId', function () {
    return this.q6();
  });
  defineProp(protoOf(EstimationItemGroup), 'items', function () {
    return this.k7();
  });
  defineProp(protoOf(EstimationParameter), 'name', function () {
    return this.s();
  });
  defineProp(protoOf(EstimationParameter), 'value', function () {
    return this.v7();
  });
  defineProp(protoOf(EstimationParameter), 'comment', function () {
    return this.j5();
  });
  defineProp(protoOf(EstimationVersion), 'versionNumber', function () {
    return this.m8();
  });
  defineProp(protoOf(EstimationVersion), 'status', function () {
    return this.n8();
  });
  defineProp(protoOf(EstimationVersion), 'createdBy', function () {
    return this.o8();
  });
  defineProp(protoOf(EstimationVersion), 'totalEffort', function () {
    return this.p8();
  });
  defineProp(protoOf(EstimationVersion), 'notes', function () {
    return this.q8();
  });
  defineProp(protoOf(EstimationVersion), 'parameters', function () {
    return this.r8();
  });
  defineProp(protoOf(EstimationVersion), 'effortDrivers', function () {
    return this.s8();
  });
  defineProp(protoOf(EstimationVersion), 'phases', function () {
    return this.t8();
  });
  defineProp(protoOf(EstimationVersion), 'additionalCosts', function () {
    return this.u8();
  });
  defineProp(protoOf(EstimationVersion), 'itemGroups', function () {
    return this.v8();
  });
  defineProp(protoOf(EstimationVersionStatus), 'name', protoOf(EstimationVersionStatus).s);
  defineProp(protoOf(EstimationVersionStatus), 'ordinal', protoOf(EstimationVersionStatus).t);
  defineProp(protoOf(Project), 'name', function () {
    return this.s();
  });
  defineProp(protoOf(Project), 'description', function () {
    return this.b4();
  });
  defineProp(protoOf(Project), 'client', function () {
    return this.oa();
  });
  defineProp(protoOf(Project), 'status', function () {
    return this.n8();
  });
  defineProp(protoOf(Project), 'owner', function () {
    return this.pa();
  });
  defineProp(protoOf(ProjectPhase), 'name', function () {
    return this.s();
  });
  defineProp(protoOf(ProjectPhase), 'abbreviation', function () {
    return this.ab();
  });
  defineProp(protoOf(ProjectPhase), 'durationWeeks', function () {
    return this.bb();
  });
  defineProp(protoOf(ProjectStatus), 'name', protoOf(ProjectStatus).s);
  defineProp(protoOf(ProjectStatus), 'ordinal', protoOf(ProjectStatus).t);
  defineProp(protoOf(TimeRelativeEstimationItem), 'unit', function () {
    return this.ec();
  });
  defineProp(protoOf(User), 'entraSubjectId', function () {
    return this.oc();
  });
  defineProp(protoOf(User), 'displayName', function () {
    return this.pc();
  });
  defineProp(protoOf(InvariantResult), 'passed', protoOf(InvariantResult).tc);
  //endregion
  //region block: init
  PertCalculation_instance = new PertCalculation();
  //endregion
  //region block: exports
  function $jsExportAll$(_) {
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.AdditionalCost = AdditionalCost;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.AdditionalCostType = AdditionalCostType;
    $de$spree$estimation$model.AdditionalCostType.values = values;
    $de$spree$estimation$model.AdditionalCostType.valueOf = valueOf;
    defineProp($de$spree$estimation$model.AdditionalCostType, 'ONE_TIME', AdditionalCostType_ONE_TIME_getInstance);
    defineProp($de$spree$estimation$model.AdditionalCostType, 'RECURRING', AdditionalCostType_RECURRING_getInstance);
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.BaseDomain = BaseDomain;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.CalculationParameters = CalculationParameters;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.createFixedItem = createFixedItem;
    $de$spree$estimation$model.createTimeRelativeItem = createTimeRelativeItem;
    $de$spree$estimation$model.createGroup = createGroup;
    $de$spree$estimation$model.createVersion = createVersion;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.EffortDriver = EffortDriver;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.Estimation = Estimation;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.EstimationItem = EstimationItem;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.EstimationItemGroup = EstimationItemGroup;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.EstimationParameter = EstimationParameter;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.EstimationVersion = EstimationVersion;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.EstimationVersionStatus = EstimationVersionStatus;
    $de$spree$estimation$model.EstimationVersionStatus.values = values_0;
    $de$spree$estimation$model.EstimationVersionStatus.valueOf = valueOf_0;
    defineProp($de$spree$estimation$model.EstimationVersionStatus, 'DRAFT', EstimationVersionStatus_DRAFT_getInstance);
    defineProp($de$spree$estimation$model.EstimationVersionStatus, 'SUBMITTED', EstimationVersionStatus_SUBMITTED_getInstance);
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.FixedEstimationItem = FixedEstimationItem;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    defineProp($de$spree$estimation$model, 'PertCalculation', PertCalculation_getInstance);
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.Project = Project;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.ProjectPhase = ProjectPhase;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.ProjectStatus = ProjectStatus;
    $de$spree$estimation$model.ProjectStatus.values = values_1;
    $de$spree$estimation$model.ProjectStatus.valueOf = valueOf_1;
    defineProp($de$spree$estimation$model.ProjectStatus, 'ACTIVE', ProjectStatus_ACTIVE_getInstance);
    defineProp($de$spree$estimation$model.ProjectStatus, 'ARCHIVED', ProjectStatus_ARCHIVED_getInstance);
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.TimeRelativeEstimationItem = TimeRelativeEstimationItem;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$model = $de$spree$estimation.model || ($de$spree$estimation.model = {});
    $de$spree$estimation$model.User = User;
    var $de = _.de || (_.de = {});
    var $de$spree = $theestimator || ($theestimator = {});
    var $de$spree$estimation = $de$spree.estimation || ($de$spree.estimation = {});
    var $de$spree$estimation$service = $de$spree$estimation.service || ($de$spree$estimation.service = {});
    $de$spree$estimation$service.EstimationCalculator = EstimationCalculator;
    $de$spree$estimation$service.InvariantResult = InvariantResult;
  }
  $jsExportAll$(_);
  kotlin_kotlin.$jsExportAll$(_);
  //endregion
  return _;
}));

//# sourceMappingURL=domain.js.map
