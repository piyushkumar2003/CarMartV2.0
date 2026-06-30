import requests
import re
import sys

BASE_URL = "http://localhost:8081"
FLIGHT_ID = 5
FLIGHT_NUMBER = "DP-001"
# Use Row 2 seats to avoid conflict with previous run
SEATS = ["2A", "2B", "2C", "2D", "2E"]

session = requests.Session()
login_payload = {'username': 'passenger', 'password': 'pass123'}
session.post(f"{BASE_URL}/login", data=login_payload)

def get_current_price():
    r = session.get(f"{BASE_URL}/book-flight?source=Pune&destination=Goa&date=2024-02-01")
    match = re.search(r'<span class="price">\$([\d\.]+)</span>', r.text)
    return float(match.group(1)) if match else None

def book_seat(seat_number):
    print(f"\n--- Booking Seat {seat_number} ---")
    r = session.post(f"{BASE_URL}/initiate-booking", data={'flightId': FLIGHT_ID}, allow_redirects=False)
    if 'Location' not in r.headers: 
        print("Failed to initiate")
        return False
    booking_id = r.headers['Location'].split('=')[1]
    
    # Try locking
    r = session.post(f"{BASE_URL}/lock-seat-and-pending", data={'bookingId': booking_id, 'seatNumber': seat_number})
    # Confirm
    r = session.post(f"{BASE_URL}/confirm-booking", data={'bookingId': booking_id})
    return True

print(f"Initial Price: ${get_current_price()}")

# Book 3 more seats. Currenly 4 booked. 
# 4 + 3 = 7 bookings.
# 7/10 = 70% occupancy. 
# Rule: 70% - 90% -> 1.4x.
# Price should jump from 2400 (1.2x) to 2800 (1.4x).

for i in range(3):
    book_seat(SEATS[i])
    print(f"Price after additional booking {i+1}: ${get_current_price()}")
