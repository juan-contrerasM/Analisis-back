import { Component, computed, effect, inject, signal } from '@angular/core';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { AppStatusService } from '../../core/services/app-status.service';
import { EtlApiService } from '../../core/services/etl-api.service';
import { SimilarityResult } from '../../shared/models/etl.models';
import { PageSkeletonComponent } from '../../shared/ui/page-skeleton.component';

@Component({
  selector: 'app-similitud-page',
  imports: [BaseChartDirective, PageSkeletonComponent],
  templateUrl: './similitud-page.component.html',
  styleUrl: './similitud-page.component.css',
})
export class SimilitudPageComponent {
  private readonly api = inject(EtlApiService);
  protected readonly status = inject(AppStatusService);

  protected readonly asset1 = signal('');
  protected readonly asset2 = signal('');
  protected readonly result = signal<SimilarityResult | null>(null);
  protected readonly seriesChart = signal<ChartConfiguration<'line'> | null>(null);
  protected readonly loading = signal(false);
  protected readonly seriesLoading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly metricBar = computed<ChartConfiguration<'bar'> | null>(() => {
    const r = this.result();
    if (!r) return null;
    const pearN = Math.min(100, Math.max(0, ((r.pearson + 1) / 2) * 100));
    const cosN = Math.min(100, Math.max(0, r.coseno * 100));
    const eucN = Math.min(100, r.euclidiana * 15);
    const dtwN = Math.min(100, r.dtw / 50);
    return {
      type: 'bar',
      data: {
        labels: ['Pearson', 'Coseno x100', 'Euclid x15', 'DTW/50'],
        datasets: [
          {
            label: 'Normalizado (visual)',
            data: [pearN, cosN, eucN, dtwN],
            backgroundColor: 'rgba(167, 139, 250, 0.45)',
            borderColor: 'rgba(167, 139, 250, 0.95)',
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { labels: { color: '#9fb0c3' } },
        },
        scales: {
          x: { ticks: { color: '#7d8ea3' }, grid: { color: 'rgba(148,163,184,0.12)' } },
          y: { ticks: { color: '#7d8ea3' }, grid: { color: 'rgba(148,163,184,0.12)' }, max: 100 },
        },
      },
    };
  });

  constructor() {
    effect(() => {
      const syms = this.status.symbols();
      if (syms.length >= 2 && !this.asset1()) {
        this.asset1.set(syms[0]);
        this.asset2.set(syms[1]);
      }
    });
  }

  protected fmt(n: number): string {
    return n.toFixed(4);
  }

  protected compare(): void {
    const a = this.asset1();
    const b = this.asset2();
    if (!a || !b || a === b) {
      this.error.set('Seleccione dos activos distintos.');
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    this.result.set(null);
    this.seriesChart.set(null);
    this.api.getSimilarity(a, b).subscribe({
      next: (res) => {
        this.result.set(res);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e?.error?.message ?? 'No se pudo calcular similitud (ETL ejecutado?)');
        this.loading.set(false);
      },
    });
  }

  protected loadSeries(): void {
    const a = this.asset1();
    const b = this.asset2();
    if (!a || !b) return;
    this.seriesLoading.set(true);
    this.api.getSeries(a, b).subscribe({
      next: (map) => {
        const s1 = map[a] ?? [];
        const s2 = map[b] ?? [];
        const n = Math.min(s1.length, s2.length);
        const labels = Array.from({ length: n }, (_, i) => String(i));
        this.seriesChart.set({
          type: 'line',
          data: {
            labels,
            datasets: [
              {
                label: 'Retornos ' + a,
                data: s1.slice(-n),
                borderColor: 'rgba(56, 189, 248, 0.95)',
                backgroundColor: 'rgba(56, 189, 248, 0.1)',
                pointRadius: 0,
                tension: 0.2,
              },
              {
                label: 'Retornos ' + b,
                data: s2.slice(-n),
                borderColor: 'rgba(251, 191, 36, 0.95)',
                backgroundColor: 'rgba(251, 191, 36, 0.1)',
                pointRadius: 0,
                tension: 0.2,
              },
            ],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { labels: { color: '#9fb0c3' } } },
            scales: {
              x: { ticks: { color: '#7d8ea3' }, grid: { color: 'rgba(148,163,184,0.12)' } },
              y: { ticks: { color: '#7d8ea3' }, grid: { color: 'rgba(148,163,184,0.12)' } },
            },
          },
        });
        this.seriesLoading.set(false);
      },
      error: () => {
        this.seriesLoading.set(false);
      },
    });
  }
}
