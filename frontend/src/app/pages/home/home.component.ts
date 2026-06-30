import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // Import CommonModule
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  source: string = 'Delhi';
  destination: string = 'Mumbai';
  date: string = new Date().toISOString().split('T')[0];

  constructor(private router: Router) { }

  onSearch(): void {
    if (this.source && this.destination && this.date) {
      this.router.navigate(['/search'], {
        queryParams: {
          source: this.source,
          destination: this.destination,
          date: this.date
        }
      });
    } else {
      alert('Please fill in all fields');
    }
  }
}
