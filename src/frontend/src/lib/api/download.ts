// Turning a downloaded response body into a file the browser saves.
//
// A plain `<a href="/api/…" download>` cannot be used for exports: browser
// navigation carries no `Authorization` header, so the backend answers `401`
// and the browser happily saves that JSON error body as the "export" file.
// Every download therefore goes through `apiFetch` and lands here.
export function downloadResponse(blob: Blob, disposition: string | null, fallbackName: string) {
	const match = disposition?.match(/filename="([^"]+)"/);
	const url = URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url;
	a.download = match?.[1] ?? fallbackName;
	document.body.appendChild(a);
	a.click();
	a.remove();
	URL.revokeObjectURL(url);
}
