package models;
public class ETicket {
    private String ticketID;
    private Booking booking;

    public ETicket(String ticketID, Booking booking) {
        this.ticketID = ticketID;
        this.booking = booking;
    }

    public String getTicketID() {
        return ticketID;
    }

    public Booking getBooking() {
        return booking;
    }

    public void getTicketDetails() {
        System.out.println("Ticket ID: " + ticketID);
        booking.getBookingDetails();
    }
}