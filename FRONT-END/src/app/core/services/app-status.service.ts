import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of, timer } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  AnalysisResponse,
  DatasetRow,
  EtlStatusResponse,
  SyncState,
} from '../../shared/models/etl.models';
import { EtlApiService } from './etl-api.service';
import { GlobalErrorService } from './global-error.service';

@Injectable({ providedIn: 'root' })
export class AppStatusService {
  private readonly api = inject(EtlApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly globalError = inject(GlobalErrorService);

  readonly etlStatus = signal<EtlStatusResponse | null>(null);
  readonly syncState = signal<SyncState>('idle');
  readonly lastPollError = signal<string | null>(null);

  readonly symbols = signal<string[]>([]);
  readonly dataset = signal<DatasetRow[]>([]);
  readonly analysis = signal<AnalysisResponse | null>(null);

  private refreshLock = false;

  /** Texto para la UI (p. ej. “Auto cada 1 min”). */
  readonly autoRefreshHint = computed(() => {
    const ms = environment.autoRefreshIntervalMs;
    const min = Math.round(ms / 60_000);
    if (min >= 1 && ms % 60_000 === 0) {
      return min === 1 ? 'Auto cada 1 min' : `Auto cada ${min} min`;
    }
    const s = Math.round(ms / 1000);
    return `Auto cada ${s}s`;
  });

  readonly dataReady = computed(
    () =>
      this.etlStatus()?.etlEjecutado === true &&
      this.etlStatus()?.estado === 'LISTO' &&
      this.dataset().length > 0,
  );

  readonly uiStatusLabel = computed(() => {
    const st = this.etlStatus();
    const sync = this.syncState();
    if (sync === 'refreshing') return 'Sincronizando datos…';
    if (st?.estado === 'EJECUTANDO') return 'ETL en ejecución…';
    if (st?.estado === 'ERROR') return 'Error en ETL';
    if (this.lastPollError()) return 'Error de conexión';
    if (this.dataReady()) return 'Actualizado';
    if (st?.etlEjecutado) return 'Listo (sin datos en caché)';
    return 'ETL no ejecutado';
  });

  readonly lastUpdateDisplay = computed(() => {
    const iso = this.etlStatus()?.ultimaActualizacion;
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    return d.toLocaleTimeString(undefined, {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  });

  loadSymbolsOnly(): void {
    this.api.getSymbols().subscribe({
      next: (s) => this.symbols.set(s),
      error: () => {},
    });
  }

  startPolling(): void {
    const pollMs = environment.autoRefreshIntervalMs;
    timer(0, pollMs)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        tap(() => {
          if (this.syncState() !== 'refreshing' && !this.refreshLock) {
            this.syncState.set('polling');
          }
        }),
        switchMap(() =>
          this.api.getStatus().pipe(
            catchError((err: Error) => {
              this.lastPollError.set(err?.message ?? 'Error al consultar estado');
              this.syncState.set('error');
              return of(null);
            }),
          ),
        ),
      )
      .subscribe((status) => {
        if (!status) {
          return;
        }
        this.lastPollError.set(null);
        this.globalError.clear();
        this.etlStatus.set(status);
        if (this.syncState() === 'error') {
          this.syncState.set('idle');
        }
        this.applyStatusForAutoRefresh(status);
        if (status.estado !== 'EJECUTANDO' && this.syncState() === 'polling') {
          this.syncState.set('idle');
        }
      });
  }

  /**
   * En cada ciclo del timer: si el backend está LISTO, recarga siempre dataset / símbolos / análisis
   * (sin comparar timestamps). Mientras EJECUTANDO solo se actualiza el estado vía getStatus.
   */
  private applyStatusForAutoRefresh(status: EtlStatusResponse): void {
    if (this.refreshLock) {
      return;
    }
    if (status.estado === 'EJECUTANDO') {
      return;
    }
    if (!status.etlEjecutado || status.estado !== 'LISTO') {
      return;
    }
    this.loadHeavyData().subscribe();
  }

  forceRefreshFromServer(): void {
    this.api
      .getStatus()
      .pipe(
        tap((s) => this.etlStatus.set(s)),
        switchMap((s) => {
          if (s.etlEjecutado && s.estado === 'LISTO') {
            return this.loadHeavyData();
          }
          return of(void 0);
        }),
      )
      .subscribe();
  }

  runEtlFromUi() {
    this.syncState.set('refreshing');
    this.globalError.clear();
    return this.api.runEtl().pipe(
      switchMap(() => this.api.getStatus()),
      tap((s) => this.etlStatus.set(s)),
      switchMap((s) => {
        if (s.etlEjecutado && s.estado === 'LISTO') {
          return this.loadHeavyData();
        }
        return of(void 0);
      }),
      finalize(() => {
        if (this.etlStatus()?.estado !== 'EJECUTANDO') {
          this.syncState.set('idle');
        }
      }),
    );
  }

  private loadHeavyData() {
    if (this.refreshLock) {
      return of(void 0);
    }
    this.refreshLock = true;
    this.syncState.set('refreshing');
    return forkJoin({
      symbols: this.api.getSymbols(),
      dataset: this.api.getDataset(),
      analysis: this.api.getAnalysis(),
    }).pipe(
      tap(({ symbols, dataset, analysis }) => {
        this.symbols.set(symbols);
        this.dataset.set(dataset);
        this.analysis.set(analysis);
      }),
      catchError((err: Error) => {
        this.lastPollError.set(err?.message ?? 'Error al cargar datos');
        return of(void 0);
      }),
      finalize(() => {
        this.refreshLock = false;
        const st = this.etlStatus();
        if (st?.estado !== 'EJECUTANDO') {
          this.syncState.set('idle');
        }
      }),
    );
  }
}
