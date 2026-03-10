import { Component, OnInit, OnDestroy, ApplicationRef } from '@angular/core';
import { LoaderService } from '../loader.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-loader',
  templateUrl: './loader.component.html',
  styleUrls: ['./loader.component.css']
})
export class LoaderComponent implements OnInit, OnDestroy {
  loading: boolean = false;
  private loaderSubscription: Subscription | undefined;

  constructor(
    private loaderService: LoaderService,
    private appRef: ApplicationRef
  ) {}

  ngOnInit(): void {
    // Subscribe to loader state changes
    this.loaderSubscription = this.loaderService.loaderState.subscribe(state => {
      // Use setTimeout to defer the update to the next change detection cycle
      setTimeout(() => {
        this.loading = state;
        // Manually trigger change detection
        this.appRef.tick();
      }, 0);
    });
  }

  ngOnDestroy(): void {
    if (this.loaderSubscription) {
      this.loaderSubscription.unsubscribe();
    }
  }
}
