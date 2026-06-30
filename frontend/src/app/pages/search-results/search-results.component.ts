import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { SearchService } from '../../services/search.service';
import { Flight } from '../../models/flight.model';

@Component({
  selector: 'app-search-results',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './search-results.component.html',
  styleUrl: './search-results.component.scss'
})
export class SearchResultsComponent implements OnInit {
  flights: Flight[] = [];
  loading = true;
  source = '';
  destination = '';
  date = '';

  constructor(
    private route: ActivatedRoute,
    private searchService: SearchService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.source = params['source'];
      this.destination = params['destination'];
      this.date = params['date'];

      if (this.source && this.destination && this.date) {
        this.searchFlights();
      } else {
        this.loading = false;
      }
    });
  }

  searchFlights(): void {
    this.loading = true;
    this.searchService.searchFlights(this.source, this.destination, this.date).subscribe({
      next: (data) => {
        this.flights = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error fetching flights', err);
        this.loading = false;
      }
    });
  }

  bookFlight(flight: Flight): void {
    this.router.navigate(['/book'], { state: { flight: flight } });
  }
}
