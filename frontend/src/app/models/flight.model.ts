export interface Flight {
    id: number;
    flightNumber: string;
    airlineName: string;
    source: string;
    destination: string;
    dateOfJourney: string;
    totalSeats: number;
    availableSeats: number;
    basePrice: number;
    currentPrice: number;
    price: number;
}
