import { Component, computed, inject } from '@angular/core';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { AppStatusService } from '../../core/services/app-status.service';
import { MetricCardComponent } from '../../shared/ui/metric-card.component';
import { PageSkeletonComponent } from '../../shared/ui/page-skeleton.component';

@Component({
  selector: 'app-dashboard-page',
  imports: [MetricCardComponent, PageSkeletonComponent, BaseChartDirective],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.css',
})
export class DashboardPageComponent {
  protected readonly status = inject(AppStatusService);

  protected readonly metrics = computed(() => {
    const a = this.status.analysis();
    const ds = this.status.dataset();
    const r = a?.ranking ?? [];
    const n = r.length;
    const avgVol = n ? r.reduce((s, x) => s + x.volatilidad, 0) / n : 0;
    const maxVol = n ? Math.max(...r.map((x) => x.volatilidad)) : 0;
    const agresivos = r.filter((x) => x.riesgo === 'AGRESIVO').length;
    return {
      activos: String(n),
      obs: String(ds.length),
      avgVol: avgVol.toFixed(4),
      maxVol: maxVol.toFixed(4),
      agresivos: String(agresivos),
    };
  });

  protected readonly volBar = computed<ChartConfiguration<'bar'>>(() => {
    const r = this.status.analysis()?.ranking ?? [];
    const top = [...r].sort((a, b) => b.volatilidad - a.volatilidad).slice(0, 8);
    return {
      type: 'bar',
      data: {
        labels: top.map((x) => x.activo),
        datasets: [
          {
            label: 'Volatilidad anualizada',
            data: top.map((x) => x.volatilidad),
            backgroundColor: 'rgba(56, 189, 248, 0.45)',
            borderColor: 'rgba(56, 189, 248, 0.9)',
            borderWidth: 1,
          },
        ],
      },
      options: this.chartOpts(),
    };
  });

  protected readonly priceLine = computed<ChartConfiguration<'line'>>(() => {
    const ds = this.status.dataset();
    const syms = this.status.symbols();
    const sym = syms.includes('AAPL') ? 'AAPL' : syms[0] ?? 'AAPL';
    const slice = ds.slice(-90);
    return {
      type: 'line',
      data: {
        labels: slice.map((row) => String(row['date'])),
        datasets: [
          {
            label: `Cierre ${sym}`,
            data: slice.map((row) => Number(row[sym])),
            borderColor: 'rgba(52, 211, 153, 0.95)',
            backgroundColor: 'rgba(52, 211, 153, 0.12)',
            fill: true,
            tension: 0.25,
            pointRadius: 0,
          },
        ],
      },
      options: this.chartOpts(),
    };
  });

  private chartOpts() {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          labels: { color: '#9fb0c3' },
        },
      },
      scales: {
        x: {
          ticks: { color: '#7d8ea3', maxRotation: 0 },
          grid: { color: 'rgba(148,163,184,0.12)' },
        },
        y: {
          ticks: { color: '#7d8ea3' },
          grid: { color: 'rgba(148,163,184,0.12)' },
        },
      },
    };
  }
}
