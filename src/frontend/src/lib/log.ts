import loglevel from 'loglevel';

// Single application logger. Verbose in dev, quiet in production builds.
// Never write bare `console.*` — import `log` from `$lib/log.ts` instead.
// Errors must still be surfaced to the user via ErrorBanner; logging is
// in addition to, not instead of, user-facing surfacing.
const log = loglevel;
log.setLevel(import.meta.env.DEV ? 'debug' : 'warn');

export { log };
export default log;