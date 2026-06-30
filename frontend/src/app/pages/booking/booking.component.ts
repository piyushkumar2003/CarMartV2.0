import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Flight } from '../../models/flight.model';
import { BookingService } from '../../services/booking.service';

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './booking.component.html',
  styleUrl: './booking.component.scss'
})
export class BookingComponent implements OnInit {
  flight: Flight | undefined;
  passengerName: string = '';
  passengerEmail: string = '';
  passengerCount: number = 1;
  errorMessage: string = '';

  constructor(private router: Router, private bookingService: BookingService) {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras?.state) {
      this.flight = navigation.extras.state['flight'];
      if (this.flight) {
        sessionStorage.setItem('pendingFlight', JSON.stringify(this.flight));
      }
    }
  }

  ngOnInit(): void {
    if (!this.flight) {
      // Fallback or redirect if no flight data (e.g. direct access)
      // handling via history state fallback if already navigated
      if (history.state.flight) {
        this.flight = history.state.flight;
        sessionStorage.setItem('pendingFlight', JSON.stringify(this.flight));
      } else {
        const stored = sessionStorage.getItem('pendingFlight');
        if (stored) {
          this.flight = JSON.parse(stored);
        } else {
          this.router.navigate(['/']);
        }
      }
    }
  }

  confirmBooking(): void {
    if (!this.passengerName || !this.passengerEmail) {
      alert('Please fill all details');
      return;
    }

    this.errorMessage = '';

    const bookingPayload = {
      flightId: this.flight?.id,
      bookingDate: new Date().toISOString(),
      status: 'PENDING',
      passengerName: this.passengerName,
      passengerEmail: this.passengerEmail,
      seatsBooked: this.passengerCount
    };

    this.bookingService.createBooking(bookingPayload).subscribe({
      next: (res) => {
        sessionStorage.removeItem('pendingFlight');
        // Navigate to payment with booking ID
        this.router.navigate(['/payment'], { state: { booking: res, flight: this.flight } });
      },
      error: (err) => {
        console.error('Booking failed', err);
        this.errorMessage = err.error || err.message || 'Booking failed. Please try again.';
      }
    });
  }
}
