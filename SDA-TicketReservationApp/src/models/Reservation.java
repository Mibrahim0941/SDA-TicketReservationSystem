package models;
import java.util.ArrayList;
import java.util.List;

public class Reservation {
    private String reservationID;
    private Schedule schedule;
    private Route route;
    private String seatClass;
    private List<String> seats;

    public Reservation(String reservationID, Schedule schedule, Route route, String seatClass) {
        this.reservationID = reservationID;
        this.schedule = schedule;
        this.route = route;
        this.seatClass = seatClass;
        this.seats = new ArrayList<>();
    }

    public String getReservationID() {
        return reservationID;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public Route getRoute() {
        return route;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void searchTicket() {
        System.out.println("Searching for tickets...");
    }

    public void viewAvailableSeats() {
        System.out.println("Available seats for reservation " + reservationID + ":");
        for (Seat seat : schedule.getSeats()) {
            if (seat.isAvailability()) {
                System.out.println("Seat: " + seat.getSeatNo() + " - Type: " + seat.getSeatType());
            }
        }
    }

    public void selectSeats(List<String> selectedSeats) {
        this.seats.addAll(selectedSeats);
        System.out.println("Selected seats: " + selectedSeats);
    }

    public Booking createBooking(String bookingID, String customerID) {
        return new Booking(bookingID, customerID, this, new java.util.Date());
    }
}