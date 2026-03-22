import { Component, computed, inject } from '@angular/core';
import { AppStatusService } from '../../core/services/app-status.service';
import { DataColumn, DataTableComponent } from '../../shared/ui/data-table.component';
import { PageSkeletonComponent } from '../../shared/ui/page-skeleton.component';

@Component({
  selector: 'app-activos-page',
  imports: [DataTableComponent, PageSkeletonComponent],
  templateUrl: './activos-page.component.html',
  styleUrl: './activos-page.component.css',
})
export class ActivosPageComponent {
  protected readonly status = inject(AppStatusService);

  protected readonly columns = computed<DataColumn[]>(() => {
    const rows = this.status.dataset();
    if (!rows.length) {
      return [{ key: 'date', label: 'Fecha' }];
    }
    return Object.keys(rows[0]).map((key) => ({
      key,
      label: key === 'date' ? 'Fecha' : key,
    }));
  });

  protected readonly rows = computed(() => this.status.dataset() as Record<string, unknown>[]);
}
