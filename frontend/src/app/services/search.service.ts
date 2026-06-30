import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Flight } from '../models/flight.model';

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  private apiUrl = '/api/flights/search'; // Proxy redirects to gateway -> flight service

  constructor(private http: HttpClient) { }

  searchFlights(source: string, destination: string, date: string): Observable<Flight[]> {
    let params = new HttpParams()
      .set('source', source)
      .set('destination', destination)
      .set('date', date);

    return this.http.get<Flight[]>(this.apiUrl, { params });
  }
}
