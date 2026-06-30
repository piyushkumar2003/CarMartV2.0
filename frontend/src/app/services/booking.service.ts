import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private apiUrl = '/api/bookings'; // Proxy to Booking Service

  constructor(private http: HttpClient) { }

  createBooking(bookingData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/book`, bookingData);
  }

  getBooking(bookingId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${bookingId}`);
  }

  getUserBookings(username: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/user/${username}`);
  }

  confirmBooking(bookingId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/confirm/${bookingId}`, {});
  }

  retryPayment(bookingId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${bookingId}/retry-payment`, {});
  }

  cancelBooking(bookingId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${bookingId}/cancel`, {});
  }
}
