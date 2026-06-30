import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { BookingService } from '../../services/booking.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  user: any;
  bookings: any[] = [];
  loading = true;
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private bookingService: BookingService,
    private router: Router
  ) { }

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    this.user = this.authService.getUser();
    this.loadBookings();
  }

  loadBookings(): void {
    const identifier = this.user.username;
    this.errorMessage = '';

    if (identifier) {
      this.bookingService.getUserBookings(identifier).subscribe({
        next: (data) => {
          this.bookings = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading bookings', err);
          this.loading = false;
          this.errorMessage = err.error || err.message || 'Failed to load bookings. Please try again.';
        }
      });
    } else {
      this.loading = false;
    }
  }

  retryPayment(booking: any): void {
    this.errorMessage = '';
    this.bookingService.retryPayment(booking.id || booking.bookingId).subscribe({
      next: (updatedBooking) => {
        const bookingId = updatedBooking.id || updatedBooking.bookingId;
        this.bookings = this.bookings.map((item) =>
          (item.id || item.bookingId) === bookingId ? updatedBooking : item
        );
      },
      error: (err) => {
        console.error('Payment retry failed', err);
        if (err.error?.status || err.error?.bookingStatus) {
          const updatedBooking = err.error;
          const bookingId = updatedBooking.id || updatedBooking.bookingId;
          this.bookings = this.bookings.map((item) =>
            (item.id || item.bookingId) === bookingId ? updatedBooking : item
          );
        }
        this.errorMessage = err.error?.message || err.message || 'Payment retry failed. Please try again.';
      }
    });
  }

  cancelBooking(booking: any): void {
    this.errorMessage = '';
    this.bookingService.cancelBooking(booking.id || booking.bookingId).subscribe({
      next: (updatedBooking) => {
        const bookingId = updatedBooking.id || updatedBooking.bookingId;
        this.bookings = this.bookings.map((item) =>
          (item.id || item.bookingId) === bookingId ? updatedBooking : item
        );
      },
      error: (err) => {
        console.error('Cancellation failed', err);
        this.errorMessage = err.error?.message || err.message || 'Cancellation failed. Please try again.';
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
