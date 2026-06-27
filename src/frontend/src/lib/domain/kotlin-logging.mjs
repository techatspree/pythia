import {
  Unit_instance104q5opgivhr8 as Unit_instance,
  VOID7hggqo3abtya as VOID,
  protoOf180f3jzyo7rfj as protoOf,
  initMetadataForInterface1egvbzx539z91 as initMetadataForInterface,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  Enum3alwj03lh1n41 as Enum,
  toString30pk9tzaqopn as toString,
  Exceptiondt2hlxn7j7vw as Exception,
  equals2au1ep9vhcato as equals,
  StringBuilder_init_$Create$2mwec1027v00x as StringBuilder_init_$Create$,
  objectCreate1ve4bgxiu4x98 as objectCreate,
  hashCodeq5arwsb9dgti as hashCode,
  getStringHashCode26igk1bx568vk as getStringHashCode,
  THROW_CCE2g6jy02ryeudk as THROW_CCE,
  noWhenBranchMatchedException2a6r7ubxgky5j as noWhenBranchMatchedException,
  Exception_init_$Create$2bsr4ndahzetu as Exception_init_$Create$,
  stackTraceToString2670q6lbhdojj as stackTraceToString,
  split2bvyvnrlcifjv as split,
  substringBeforekje8w2lxhyb6 as substringBefore,
  substringAfterLastuw9v7gfiiihe as substringAfterLast,
  contains3ue2qo8xhmpf1 as contains,
} from './kotlin-kotlin-stdlib.mjs';
//region block: imports
var imul = Math.imul;
//endregion
//region block: pre-declaration
function debug(message) {
  var tmp = Level_DEBUG_getInstance();
  return this.z5(tmp, VOID, KLogger$debug$lambda(message));
}
function at$default(level, marker, block, $super) {
  marker = marker === VOID ? null : marker;
  var tmp;
  if ($super === VOID) {
    this.a6(level, marker, block);
    tmp = Unit_instance;
  } else {
    tmp = $super.a6.call(this, level, marker, block);
  }
  return tmp;
}
initMetadataForInterface(KLogger, 'KLogger');
initMetadataForClass(KLoggingEventBuilder, 'KLoggingEventBuilder', KLoggingEventBuilder);
initMetadataForObject(KotlinLogging, 'KotlinLogging');
initMetadataForClass(Level, 'Level', VOID, Enum);
initMetadataForObject(DefaultErrorMessageProducer, 'DefaultErrorMessageProducer');
initMetadataForClass(FormattingAppender, 'FormattingAppender');
initMetadataForClass(DefaultMessageFormatter, 'DefaultMessageFormatter', DefaultMessageFormatter);
initMetadataForClass(KLoggingEvent, 'KLoggingEvent');
initMetadataForClass(KLoggerDirect, 'KLoggerDirect', VOID, VOID, [KLogger]);
initMetadataForObject(KLoggerFactory, 'KLoggerFactory');
initMetadataForClass(ConsoleOutputAppender, 'ConsoleOutputAppender', ConsoleOutputAppender, FormattingAppender);
initMetadataForObject(KotlinLoggingConfiguration, 'KotlinLoggingConfiguration');
initMetadataForObject(KLoggerNameResolver, 'KLoggerNameResolver');
//endregion
function KLogger$debug$lambda($message) {
  return function ($this$at) {
    $this$at.t5_1 = toStringSafe($message);
    return Unit_instance;
  };
}
function KLogger() {
}
function KLoggingEventBuilder() {
  this.t5_1 = null;
  this.u5_1 = null;
  this.v5_1 = null;
  this.w5_1 = null;
  this.x5_1 = null;
}
function KotlinLogging() {
}
protoOf(KotlinLogging).b6 = function (func) {
  return this.d6(KLoggerNameResolver_instance.c6(func));
};
protoOf(KotlinLogging).d6 = function (name) {
  return KLoggerFactory_instance.d6(name);
};
var KotlinLogging_instance;
function KotlinLogging_getInstance() {
  return KotlinLogging_instance;
}
var Level_TRACE_instance;
var Level_DEBUG_instance;
var Level_INFO_instance;
var Level_WARN_instance;
var Level_ERROR_instance;
var Level_OFF_instance;
var Level_entriesInitialized;
function Level_initEntries() {
  if (Level_entriesInitialized)
    return Unit_instance;
  Level_entriesInitialized = true;
  Level_TRACE_instance = new Level('TRACE', 0, 0, 'TRACE');
  Level_DEBUG_instance = new Level('DEBUG', 1, 10, 'DEBUG');
  Level_INFO_instance = new Level('INFO', 2, 20, 'INFO');
  Level_WARN_instance = new Level('WARN', 3, 30, 'WARN');
  Level_ERROR_instance = new Level('ERROR', 4, 40, 'ERROR');
  Level_OFF_instance = new Level('OFF', 5, 50, 'OFF');
}
function Level(name, ordinal, levelInt, levelStr) {
  Enum.call(this, name, ordinal);
  this.g6_1 = levelInt;
  this.h6_1 = levelStr;
}
protoOf(Level).toString = function () {
  return this.h6_1;
};
function Level_DEBUG_getInstance() {
  Level_initEntries();
  return Level_DEBUG_instance;
}
function Level_INFO_getInstance() {
  Level_initEntries();
  return Level_INFO_instance;
}
function toStringSafe(_this__u8e3s4) {
  var tmp;
  try {
    tmp = toString(_this__u8e3s4());
  } catch ($p) {
    var tmp_0;
    if ($p instanceof Exception) {
      var e = $p;
      tmp_0 = DefaultErrorMessageProducer_instance.i6(e);
    } else {
      throw $p;
    }
    tmp = tmp_0;
  }
  return tmp;
}
function DefaultErrorMessageProducer() {
}
protoOf(DefaultErrorMessageProducer).i6 = function (e) {
  return 'Log message invocation failed: ' + e.toString();
};
var DefaultErrorMessageProducer_instance;
function DefaultErrorMessageProducer_getInstance() {
  return DefaultErrorMessageProducer_instance;
}
function FormattingAppender() {
}
protoOf(FormattingAppender).k6 = function (loggingEvent) {
  // Inline function 'kotlin.let' call
  var it = KotlinLoggingConfiguration_getInstance().m6_1.o6(loggingEvent);
  this.j6(loggingEvent, it);
};
function prefix($this, level, loggerName) {
  var tmp;
  if ($this.p6_1) {
    tmp = level.a1_1 + ': [' + loggerName + '] ';
  } else {
    tmp = '';
  }
  return tmp;
}
function throwableToString($this, _this__u8e3s4) {
  return createThrowableMsg($this, '', _this__u8e3s4);
}
function createThrowableMsg($this, msg, throwable) {
  var $this_0 = $this;
  var msg_0 = msg;
  var throwable_0 = throwable;
  $l$1: do {
    $l$0: do {
      var tmp;
      if (throwable_0 == null || equals(throwable_0.cause, throwable_0)) {
        tmp = msg_0;
      } else {
        var tmp0 = $this_0;
        var tmp1 = msg_0 + ", Caused by: '" + throwable_0.message + "'";
        var tmp2 = throwable_0.cause;
        $this_0 = tmp0;
        msg_0 = tmp1;
        throwable_0 = tmp2;
        continue $l$0;
      }
      return tmp;
    }
     while (false);
  }
   while (true);
}
function DefaultMessageFormatter(includePrefix) {
  includePrefix = includePrefix === VOID ? true : includePrefix;
  this.p6_1 = includePrefix;
}
protoOf(DefaultMessageFormatter).o6 = function (loggingEvent) {
  // Inline function 'kotlin.with' call
  // Inline function 'kotlin.text.buildString' call
  // Inline function 'kotlin.apply' call
  var this_0 = StringBuilder_init_$Create$();
  this_0.c3(prefix(this, loggingEvent.q6_1, loggingEvent.s6_1));
  var tmp0_safe_receiver = loggingEvent.r6_1;
  var tmp1_safe_receiver = tmp0_safe_receiver == null ? null : tmp0_safe_receiver.w6();
  if (tmp1_safe_receiver == null)
    null;
  else {
    // Inline function 'kotlin.let' call
    this_0.c3(tmp1_safe_receiver);
    this_0.c3(' ');
  }
  this_0.c3(loggingEvent.t6_1);
  this_0.c3(throwableToString(this, loggingEvent.u6_1));
  return this_0.toString();
};
function KLoggingEvent_init_$Init$(level, marker, loggerName, eventBuilder, $this) {
  KLoggingEvent.call($this, level, marker, loggerName, eventBuilder.t5_1, eventBuilder.u5_1, eventBuilder.v5_1);
  return $this;
}
function KLoggingEvent_init_$Create$(level, marker, loggerName, eventBuilder) {
  return KLoggingEvent_init_$Init$(level, marker, loggerName, eventBuilder, objectCreate(protoOf(KLoggingEvent)));
}
function KLoggingEvent(level, marker, loggerName, message, cause, payload) {
  message = message === VOID ? null : message;
  cause = cause === VOID ? null : cause;
  payload = payload === VOID ? null : payload;
  this.q6_1 = level;
  this.r6_1 = marker;
  this.s6_1 = loggerName;
  this.t6_1 = message;
  this.u6_1 = cause;
  this.v6_1 = payload;
}
protoOf(KLoggingEvent).toString = function () {
  return 'KLoggingEvent(level=' + this.q6_1.toString() + ', marker=' + toString(this.r6_1) + ', loggerName=' + this.s6_1 + ', message=' + this.t6_1 + ', cause=' + toString(this.u6_1) + ', payload=' + toString(this.v6_1) + ')';
};
protoOf(KLoggingEvent).hashCode = function () {
  var result = this.q6_1.hashCode();
  result = imul(result, 31) + (this.r6_1 == null ? 0 : hashCode(this.r6_1)) | 0;
  result = imul(result, 31) + getStringHashCode(this.s6_1) | 0;
  result = imul(result, 31) + (this.t6_1 == null ? 0 : getStringHashCode(this.t6_1)) | 0;
  result = imul(result, 31) + (this.u6_1 == null ? 0 : hashCode(this.u6_1)) | 0;
  result = imul(result, 31) + (this.v6_1 == null ? 0 : hashCode(this.v6_1)) | 0;
  return result;
};
protoOf(KLoggingEvent).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof KLoggingEvent))
    return false;
  var tmp0_other_with_cast = other instanceof KLoggingEvent ? other : THROW_CCE();
  if (!this.q6_1.equals(tmp0_other_with_cast.q6_1))
    return false;
  if (!equals(this.r6_1, tmp0_other_with_cast.r6_1))
    return false;
  if (!(this.s6_1 === tmp0_other_with_cast.s6_1))
    return false;
  if (!(this.t6_1 == tmp0_other_with_cast.t6_1))
    return false;
  if (!equals(this.u6_1, tmp0_other_with_cast.u6_1))
    return false;
  if (!equals(this.v6_1, tmp0_other_with_cast.v6_1))
    return false;
  return true;
};
function isLoggingEnabled(_this__u8e3s4) {
  return _this__u8e3s4.b1_1 >= KotlinLoggingConfiguration_getInstance().l6_1.b1_1;
}
function KLoggerDirect(name) {
  this.x6_1 = name;
}
protoOf(KLoggerDirect).a6 = function (level, marker, block) {
  if (this.y6(level, marker)) {
    // Inline function 'kotlin.apply' call
    var this_0 = new KLoggingEventBuilder();
    block(this_0);
    // Inline function 'kotlin.run' call
    if (level.b1_1 !== 5) {
      KotlinLoggingConfiguration_getInstance().n6_1.k6(KLoggingEvent_init_$Create$(level, marker, this.x6_1, this_0));
    }
  }
};
protoOf(KLoggerDirect).y6 = function (level, marker) {
  return isLoggingEnabled(level);
};
function KLoggerFactory() {
}
protoOf(KLoggerFactory).d6 = function (name) {
  return new KLoggerDirect(name);
};
var KLoggerFactory_instance;
function KLoggerFactory_getInstance() {
  return KLoggerFactory_instance;
}
function ConsoleOutputAppender() {
  FormattingAppender.call(this);
}
protoOf(ConsoleOutputAppender).j6 = function (loggingEvent, formattedMessage) {
  switch (loggingEvent.q6_1.b1_1) {
    case 0:
      console.log(formattedMessage);
      break;
    case 1:
      console.log(formattedMessage);
      break;
    case 2:
      console.info(formattedMessage);
      break;
    case 3:
      console.warn(formattedMessage);
      break;
    case 4:
      console.error(formattedMessage);
      break;
    case 5:
      break;
    default:
      noWhenBranchMatchedException();
      break;
  }
};
function KotlinLoggingConfiguration() {
  KotlinLoggingConfiguration_instance = this;
  this.l6_1 = Level_INFO_getInstance();
  this.m6_1 = new DefaultMessageFormatter(true);
  this.n6_1 = new ConsoleOutputAppender();
}
var KotlinLoggingConfiguration_instance;
function KotlinLoggingConfiguration_getInstance() {
  if (KotlinLoggingConfiguration_instance == null)
    new KotlinLoggingConfiguration();
  return KotlinLoggingConfiguration_instance;
}
function KLoggerNameResolver() {
}
protoOf(KLoggerNameResolver).c6 = function (func) {
  var found = false;
  var exception = Exception_init_$Create$();
  var _iterator__ex2g4s = split(stackTraceToString(exception), ['\n']).f();
  while (_iterator__ex2g4s.g()) {
    var line = _iterator__ex2g4s.h();
    if (found) {
      return substringAfterLast(substringAfterLast(substringBefore(line, '.kt'), '.'), '/');
    }
    if (contains(line, 'at KotlinLogging')) {
      found = true;
    }
  }
  return '';
};
var KLoggerNameResolver_instance;
function KLoggerNameResolver_getInstance() {
  return KLoggerNameResolver_instance;
}
//region block: post-declaration
protoOf(KLoggerDirect).z5 = at$default;
protoOf(KLoggerDirect).y5 = debug;
//endregion
//region block: init
KotlinLogging_instance = new KotlinLogging();
DefaultErrorMessageProducer_instance = new DefaultErrorMessageProducer();
KLoggerFactory_instance = new KLoggerFactory();
KLoggerNameResolver_instance = new KLoggerNameResolver();
//endregion
//region block: exports
export {
  KotlinLogging_instance as KotlinLogging_instance20u19uwz7rzsk,
};
//endregion

//# sourceMappingURL=kotlin-logging.mjs.map
