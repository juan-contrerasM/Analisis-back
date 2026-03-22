import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AppStatusService } from '../../core/services/app-status.service';
import { GlobalErrorService } from '../../core/services/global-error.service';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css',
})
export class MainLayoutComponent implements OnInit {
  protected readonly status = inject(AppStatusService);
  protected readonly globalError = inject(GlobalErrorService);

  protected readonly runBusy = signal(false);

  ngOnInit(): void {
    this.status.loadSymbolsOnly();
    this.status.startPolling();
  }

  protected runEtl(): void {
    if (this.runBusy()) return;
    this.runBusy.set(true);
    this.status.runEtlFromUi().subscribe({
      complete: () => this.runBusy.set(false),
      error: () => this.runBusy.set(false),
    });
  }

  protected refresh(): void {
    this.globalError.clear();
    this.status.forceRefreshFromServer();
  }

  protected dismissError(): void {
    this.globalError.clear();
  }
}
