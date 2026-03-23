import { environment } from '../../../environments/environment';

export function apiBaseUrl(): string {
  return environment.apiUrl.replace(/\/$/, '');
}

export function autoRefreshIntervalMs(): number {
  return environment.autoRefreshIntervalMs;
}

export function statusPollIntervalMs(): number {
  return environment.statusPollIntervalMs ?? 5_000;
}
