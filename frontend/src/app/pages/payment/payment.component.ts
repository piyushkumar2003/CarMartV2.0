import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BookingService } from '../../services/booking.service';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.scss'
})
export class PaymentComponent implements OnInit {
  booking: any;
  flight: any;
  processing = false;
  errorMessage = '';

  constructor(private router: Router, private bookingService: BookingService) {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras?.state) {
      this.booking = navigation.extras.state['booking'];
      this.flight = navigation.extras.state['flight'];
      if (this.booking) {
        sessionStorage.setItem('pendingBooking', JSON.stringify(this.booking));
      }
      if (this.flight) {
        sessionStorage.setItem('paymentFlight', JSON.stringify(this.flight));
      }
    }
  }

  ngOnInit(): void {
    if (!this.booking) {
      if (history.state.booking) {
        this.booking = history.state.booking;
        this.flight = history.state.flight;
        sessionStorage.setItem('pendingBooking', JSON.stringify(this.booking));
        sessionStorage.setItem('paymentFlight', JSON.stringify(this.flight));
      } else {
        const storedBooking = sessionStorage.getItem('pendingBooking');
        const storedFlight = sessionStorage.getItem('paymentFlight');
        if (storedBooking && storedFlight) {
          this.booking = JSON.parse(storedBooking);
          this.flight = JSON.parse(storedFlight);
        } else {
          this.router.navigate(['/']);
        }
      }
    }
  }

  processPayment(): void {
    this.processing = true;
    this.errorMessage = '';

    const paymentRequest = this.booking?.status === 'PAYMENT_PENDING'
      ? this.bookingService.retryPayment(this.booking.id)
      : this.bookingService.confirmBooking(this.booking.id);

    paymentRequest.subscribe({
      next: (res) => {
        this.processing = false;
        this.booking = res;
        sessionStorage.setItem('pendingBooking', JSON.stringify(this.booking));

        if (res?.status === 'CONFIRMED' || res?.bookingStatus === 'CONFIRMED') {
          sessionStorage.removeItem('pendingBooking');
          sessionStorage.removeItem('paymentFlight');
          alert('Payment Successful! Booking Confirmed.');
          this.router.navigate(['/dashboard']);
          return;
        }

        if (res?.status === 'PAYMENT_PENDING' || res?.bookingStatus === 'PAYMENT_PENDING') {
          this.errorMessage = 'Payment could not be completed. Please retry before your seat hold expires.';
          return;
        }

        this.errorMessage = 'Payment is not complete yet. Current status: ' + (res?.status || res?.bookingStatus || 'UNKNOWN');
      },
      error: (err) => {
        console.error('Confirmation failed', err);
        this.processing = false;
        if (err.error?.status || err.error?.bookingStatus) {
          this.booking = err.error;
          sessionStorage.setItem('pendingBooking', JSON.stringify(this.booking));
        }
        this.errorMessage = err.error?.message || err.message || 'Payment confirmation failed. Please try again.';
      }
    });
  }
}
