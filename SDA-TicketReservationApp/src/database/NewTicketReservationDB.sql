DROP DATABASE TicketGenieDB
-- Create Database
CREATE DATABASE TicketGenieDB;
GO

USE TicketGenieDB;
GO

-- Create ContactInfo Table
CREATE TABLE ContactInfo (
    ContactID INT IDENTITY(1,1) PRIMARY KEY,
    Email NVARCHAR(255) UNIQUE NOT NULL,
    PhoneNum NVARCHAR(20)
);
GO

-- Create Users Table
CREATE TABLE Users (
    UserID NVARCHAR(50) PRIMARY KEY,
    Name NVARCHAR(100) NOT NULL,
    Password NVARCHAR(255) NOT NULL,
    Username NVARCHAR(50) UNIQUE NOT NULL,
    ContactID INT NOT NULL,
    UserType NVARCHAR(20) NOT NULL CHECK (UserType IN ('Customer', 'Admin', 'SupportStaff')),
    FOREIGN KEY (ContactID) REFERENCES ContactInfo(ContactID)
);
GO

CREATE TABLE SupportQueries (
    QueryID VARCHAR(20) PRIMARY KEY,
    Text TEXT NOT NULL,
    AskedOn DATETIME NOT NULL,
    Status BIT DEFAULT 0,
    Response TEXT NULL,
    CustomerID NVARCHAR(50) NULL,
    SupportStaffID NVARCHAR(50) NULL,
    CreatedAt DATETIME DEFAULT GETDATE(),
    UpdatedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (CustomerID) REFERENCES Users(UserID),
    FOREIGN KEY (SupportStaffID) REFERENCES Users(UserID)
);
GO

-- Route Table
CREATE TABLE Route (
    RouteID NVARCHAR(20) PRIMARY KEY,
    Source NVARCHAR(100) NOT NULL,
    Destination NVARCHAR(100) NOT NULL,
    BasePrice DECIMAL(10,2) NOT NULL,
    IsActive BIT DEFAULT 1
);
GO

-- Schedule Table
CREATE TABLE Schedule (
    ScheduleID NVARCHAR(20) PRIMARY KEY,
    RouteID NVARCHAR(20) NOT NULL,
    Date DATE NOT NULL,
    DepartureTime TIME NOT NULL,
    ArrivalTime TIME NOT NULL,
    Class NVARCHAR(20) NOT NULL,
    TypePercentage DECIMAL(5,2) DEFAULT 100.0,
    IsActive BIT DEFAULT 1,
    FOREIGN KEY (RouteID) REFERENCES Route(RouteID) ON DELETE CASCADE
);
GO

-- Seat Table
CREATE TABLE Seat (
    SeatID INT IDENTITY(1,1) PRIMARY KEY,
    ScheduleID NVARCHAR(20) NOT NULL,
    SeatNumber NVARCHAR(10) NOT NULL,
    SeatType NVARCHAR(20) NOT NULL,
    Price DECIMAL(10,2) NOT NULL,
    Availability BIT DEFAULT 1,
    FOREIGN KEY (ScheduleID) REFERENCES Schedule(ScheduleID) ON DELETE CASCADE
);
ALTER TABLE Seat 
ADD ReservationID NVARCHAR(20) DEFAULT NULL;
ALTER TABLE Seat 
ADD CONSTRAINT FK_Seat_Reservation
FOREIGN KEY (ReservationID) REFERENCES Reservation(ReservationID);

select * from seat
GO

-- CancellationPolicies Table
CREATE TABLE CancellationPolicies (
    PolicyID VARCHAR(20) PRIMARY KEY,
    RefundAmount DECIMAL(5,2) NOT NULL,
    TimeBeforeDeparture INT NOT NULL,
    Description VARCHAR(500) NOT NULL,
    UNIQUE(TimeBeforeDeparture)
);
GO

-- PromotionalCodes Table
CREATE TABLE PromotionalCodes (
    Code VARCHAR(50) PRIMARY KEY,
    Percentage DECIMAL(5,2) NOT NULL,
    ValidityDate DATE NOT NULL,
    IsActive BIT DEFAULT 1
);
GO

CREATE TABLE Payment (
    PaymentID NVARCHAR(50) PRIMARY KEY,
    Amount DECIMAL(10,2) NOT NULL,
    PaymentMethod NVARCHAR(50) NOT NULL,
    PaymentStatus NVARCHAR(20) DEFAULT 'Pending',
    TransactionID NVARCHAR(100),
    PaymentDate DATETIME DEFAULT GETDATE()
);
GO


-- Reservation Table
CREATE TABLE Reservation (
    ReservationID NVARCHAR(20) PRIMARY KEY,
    ScheduleID NVARCHAR(20) NOT NULL,
    RouteID NVARCHAR(20) NOT NULL,
    FOREIGN KEY (ScheduleID) REFERENCES Schedule(ScheduleID),
    FOREIGN KEY (RouteID) REFERENCES Route(RouteID)
);
GO

-- Booking Table
CREATE TABLE Booking (
    BookingID NVARCHAR(20) PRIMARY KEY,
    CustomerID NVARCHAR(50) NOT NULL,
    ReservationID NVARCHAR(20) NOT NULL,
    BookingDateTime DATETIME DEFAULT GETDATE(),
    TotalAmount DECIMAL(10,2) NOT NULL,
    Status NVARCHAR(20) DEFAULT 'Confirmed', -- Confirmed, Cancelled, Completed
    PaymentID NVARCHAR(20),
    FOREIGN KEY (CustomerID) REFERENCES Users(UserID),
    FOREIGN KEY (ReservationID) REFERENCES Reservation(ReservationID),
    FOREIGN KEY (PaymentID) REFERENCES Payment(PaymentID)
);
GO

CREATE TABLE ETickets (
    TicketID NVARCHAR(50) PRIMARY KEY,
    BookingID NVARCHAR(50) NOT NULL,
    TicketData TEXT NOT NULL,
    GeneratedOn DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (BookingID) REFERENCES Booking(BookingID)
);
GO

select*from Reservation
select * from booking
Select* from Schedule
select * from Seat

-- Create Notifications Table
CREATE TABLE Notifications (
    NotificationID INT IDENTITY(1,1) PRIMARY KEY,
    UserID NVARCHAR(50) NOT NULL,
    Title NVARCHAR(200) NOT NULL,
    Message NVARCHAR(1000) NOT NULL,
    Type NVARCHAR(20) NOT NULL CHECK (Type IN ('booking', 'payment', 'reminder', 'system', 'promotion')),
    IsRead BIT DEFAULT 0,
    CreatedAt DATETIME DEFAULT GETDATE(),
    RelatedID NVARCHAR(100) NULL, -- BookingID, PaymentID, etc.
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE
);

-- Insert sample notifications (optional)
INSERT INTO Notifications (UserID, Title, Message, Type, RelatedID) VALUES
('CUST-8299105d-4063-45fd-bc64-d86c7271dbee', '🎉 Welcome to TicketGenie!', 'Thank you for registering with TicketGenie. Enjoy seamless bus ticket booking experience.', 'system', NULL),
('CUST-8299105d-4063-45fd-bc64-d86c7271dbee', '🎫 Booking Successful', 'Your booking #B12345 from Lahore to Islamabad has been confirmed. Please complete payment.', 'booking', 'B12345');

Select * from Notifications
select * from users