import { Component, computed, input, signal } from '@angular/core';

export interface DataColumn {
  key: string;
  label: string;
}

@Component({
  selector: 'app-data-table',
  imports: [],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.css',
})
export class DataTableComponent {
  readonly columns = input.required<DataColumn[]>();
  readonly rows = input.required<Record<string, unknown>[]>();

  protected readonly filterText = signal('');
  protected readonly sortKey = signal<string | null>(null);
  protected readonly sortAsc = signal(true);

  protected readonly filteredRows = computed(() => {
    const q = this.filterText().trim().toLowerCase();
    let list = this.rows();
    if (q) {
      list = list.filter((row) =>
        Object.values(row).some((v) => String(v).toLowerCase().includes(q)),
      );
    }
    const sk = this.sortKey();
    if (!sk) {
      return list;
    }
    const asc = this.sortAsc();
    return [...list].sort((a, b) => {
      const va = a[sk];
      const vb = b[sk];
      const na = typeof va === 'number' ? va : String(va);
      const nb = typeof vb === 'number' ? vb : String(vb);
      if (na < nb) return asc ? -1 : 1;
      if (na > nb) return asc ? 1 : -1;
      return 0;
    });
  });

  protected sortBy(key: string): void {
    if (this.sortKey() === key) {
      this.sortAsc.update((v) => !v);
    } else {
      this.sortKey.set(key);
      this.sortAsc.set(true);
    }
  }

  protected sortIcon(key: string): string {
    if (this.sortKey() !== key) return '↕';
    return this.sortAsc() ? '↑' : '↓';
  }

  protected displayCell(row: Record<string, unknown>, key: string): string {
    const v = row[key];
    if (typeof v === 'number') {
      return Number.isFinite(v) ? v.toFixed(6) : String(v);
    }
    if (v == null) return '—';
    return String(v);
  }
}
