import { getSystemSettings, type SystemSettingsDto } from '$lib/api/system';
import { log } from '$lib/log';

// The installation's settings, loaded once at startup (task-146).
//
// `displayName` is null until loaded and stays null when none is configured —
// callers fall back to the i18n `brand.name`. A failed load is logged and left
// at null on purpose: branding must never gate rendering, and the login screen
// has to paint even when the backend is unreachable.
class SystemStore {
	settings = $state<SystemSettingsDto | null>(null);

	get displayName(): string | null {
		const name = this.settings?.displayName?.trim();
		return name ? name : null;
	}

	async load(): Promise<void> {
		try {
			this.settings = await getSystemSettings();
		} catch (e: unknown) {
			log.error('system settings: load failed, falling back to the built-in name', e);
			this.settings = null;
		}
	}

	apply(settings: SystemSettingsDto): void {
		this.settings = settings;
	}
}

export const system = new SystemStore();
