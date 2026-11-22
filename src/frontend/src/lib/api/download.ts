// Turning a downloaded response body into a file the browser saves.
//
// A plain `<a href="/api/…" download>` cannot be used for exports: browser
// navigation carries no `Authorization` header, so the backend answers `401`
// and the browser happily saves that JSON error body as the "export" file.
// Every download therefore goes through `apiFetch` and lands here.
export function downloadResponse(blob: Blob, disposition: string | null, fallbackName: string) {
	const match = disposition?.match(/filename="([^"]+)"/);
	downloadBlob(blob, match?.[1] ?? fallbackName);
}

// Saves a blob under the given name. For a body that is not the file itself,
// e.g. the merge into Merlin (task-223), whose JSON carries the file as base64.
export function downloadBlob(blob: Blob, fileName: string) {
	const url = URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url;
	a.download = fileName;
	document.body.appendChild(a);
	a.click();
	a.remove();
	URL.revokeObjectURL(url);
}

// Standard (padded) base64 → Blob.
export function base64ToBlob(b64: string, type: string): Blob {
	const binary = atob(b64);
	const bytes = new Uint8Array(binary.length);
	for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
	return new Blob([bytes], { type });
}
