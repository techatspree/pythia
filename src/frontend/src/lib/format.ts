// Locale-aware presentation formatting (phase-16, task-126). This is the single
// seam for number/currency/date display: no component should call `.toFixed()`,
// `toLocale*()`, or a hardcoded `de-DE` locale directly. Pure leaf module — the
// active locale is a parameter, so this never imports the i18n store; the caller
// supplies `$locale ?? DEFAULT_LOCALE`. Currency is always EUR/€ (the tool is
// EUR-only); only grouping/decimal/date presentation follows the locale.

/** Locale used when the i18n store has not resolved a locale yet. */
export const DEFAULT_LOCALE = 'de';

/** Locale-aware number formatting (grouping + decimals). */
export function formatNumber(
	value: number,
	locale: string,
	options?: Intl.NumberFormatOptions
): string {
	return new Intl.NumberFormat(locale, options).format(value);
}

/**
 * Locale-aware number with a fixed number of fraction digits — the drop-in
 * replacement for `value.toFixed(fractionDigits)`, but with locale grouping
 * (German `1.234,56`, English `1,234.56`).
 */
export function formatFixed(value: number, locale: string, fractionDigits: number): string {
	return formatNumber(value, locale, {
		minimumFractionDigits: fractionDigits,
		maximumFractionDigits: fractionDigits
	});
}

/** Locale-aware currency string. Always EUR/€ regardless of locale. */
export function formatCurrency(value: number, locale: string): string {
	return new Intl.NumberFormat(locale, { style: 'currency', currency: 'EUR' }).format(value);
}

/**
 * Locale-aware date/time formatting. Returns '' for a null/blank/invalid input
 * so callers keep their own placeholder (`— ` / '') for the empty case. With no
 * options it mirrors the previous locale-default numeric date.
 */
export function formatDate(
	iso: string | null | undefined,
	locale: string,
	options?: Intl.DateTimeFormatOptions
): string {
	if (!iso) return '';
	const d = new Date(iso);
	if (Number.isNaN(d.getTime())) return '';
	return new Intl.DateTimeFormat(locale, options).format(d);
}
