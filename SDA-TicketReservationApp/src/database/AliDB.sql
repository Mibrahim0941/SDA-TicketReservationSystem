
CREATE DATABASE Ticket_Genie_DB;
USE Ticket_Genie_DB;
GO

-- ContactInfo Table
CREATE TABLE ContactInfo (
    ContactID INT IDENTITY(1,1) PRIMARY KEY,
    Email NVARCHAR(255) UNIQUE NOT NULL,
    PhoneNum NVARCHAR(20)
);
GO

-- Users Table
CREATE TABLE Users (
    UserID NVARCHAR(50) PRIMARY KEY,
    Name NVARCHAR(100) NOT NULL,
    Password NVARCHAR(255) NOT NULL,
    Username NVARCHAR(50) UNIQUE NOT NULL,
    ContactID INT NOT NULL,
    UserType NVARCHAR(20) NOT NULL CHECK (UserType IN ('Customer', 'Admin', 'SupportStaff')),
    IsActive BIT DEFAULT 1,
    FOREIGN KEY (ContactID) REFERENCES ContactInfo(ContactID)
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

-- SupportQueries Table
CREATE TABLE SupportQueries (
    QueryID VARCHAR(20) PRIMARY KEY,
    Text TEXT NOT NULL,
    AskedOn DATETIME NOT NULL,
    Status BIT DEFAULT 0,
    Response TEXT NULL,
    CustomerID NVARCHAR(50) NULL,
    SupportStaffID NVARCHAR(50) NULL,
    FOREIGN KEY (CustomerID) REFERENCES Users(UserID),
    FOREIGN KEY (SupportStaffID) REFERENCES Users(UserID)
);
GO

select* from SupportQueries

-- Payment Table
CREATE TABLE Payment (
    PaymentID NVARCHAR(50) PRIMARY KEY,
    Amount DECIMAL(10,2) NOT NULL,
    PaymentMethod NVARCHAR(50) NOT NULL,
    PaymentStatus NVARCHAR(20) DEFAULT 'Pending',
    TransactionID NVARCHAR(100),
    PaymentDate DATETIME DEFAULT GETDATE()
);
GO

-- Booking Table
CREATE TABLE Booking (
    BookingID NVARCHAR(50) PRIMARY KEY,
    CustomerID NVARCHAR(50) NOT NULL,
    ScheduleID INT NOT NULL,
    SeatNumbers NVARCHAR(500) NOT NULL,
    TotalAmount DECIMAL(10,2) NOT NULL,
    BookingDate DATETIME DEFAULT GETDATE(),
    Status NVARCHAR(20) DEFAULT 'Confirmed',
    PaymentID NVARCHAR(50),
    FOREIGN KEY (CustomerID) REFERENCES Users(UserID),
    FOREIGN KEY (ScheduleID) REFERENCES Schedule(ScheduleID),
    FOREIGN KEY (PaymentID) REFERENCES Payment(PaymentID)
);
GO

-- Reservation Table
CREATE TABLE Reservation (
    ReservationID INT IDENTITY(1,1) PRIMARY KEY,
    ScheduleID INT NOT NULL,
    RouteID INT NOT NULL,
    FOREIGN KEY (ScheduleID) REFERENCES Schedule(ScheduleID),
    FOREIGN KEY (RouteID) REFERENCES Route(RouteID)
);
GO

-- ETickets Table
CREATE TABLE ETickets (
    TicketID NVARCHAR(50) PRIMARY KEY,
    BookingID NVARCHAR(50) NOT NULL,
    TicketData TEXT NOT NULL,
    GeneratedOn DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (BookingID) REFERENCES Booking(BookingID)
);
GO

-- Insert Sample Data

-- ContactInfo and Users
INSERT INTO ContactInfo (Email, PhoneNum) VALUES 
('admin@ticketgenie.com', '0300-1234567'),
('support@ticketgenie.com', '0300-7654321'),
('john.doe@example.com', '0300-1111111'),
('jane.smith@example.com', '0300-2222222'),
('alice.j@example.com', '0300-3333333'),
('bob.brown@example.com', '0300-4444444');
GO

INSERT INTO Users (UserID, Name, Password, Username, ContactID, UserType) VALUES
('ADM001', 'System Admin', 'admin123', 'admin', 1, 'Admin'),
('SUP001', 'Support Staff', 'support123', 'support', 2, 'SupportStaff'),
('CUST001', 'John Doe', 'customer123', 'johndoe', 3, 'Customer'),
('CUST002', 'Jane Smith', 'customer123', 'janesmith', 4, 'Customer'),
('CUST003', 'Alice Johnson', 'customer123', 'alicej', 5, 'Customer'),
('CUST004', 'Bob Brown', 'customer123', 'bobb', 6, 'Customer');
GO

-- Routes
INSERT INTO Route (Source, Destination, BasePrice) VALUES
('Lahore', 'Islamabad', 1500.00),
('Karachi', 'Lahore', 2500.00),
('Islamabad', 'Peshawar', 1200.00),
('Lahore', 'Karachi', 2800.00),
('Islamabad', 'Karachi', 3000.00);
GO

-- Schedules
INSERT INTO Schedule (RouteID, Date, DepartureTime, ArrivalTime, Class) VALUES
(1, '2024-03-01', '08:00', '12:00', 'Economy'),
(1, '2024-03-01', '14:00', '18:00', 'Business'),
(2, '2024-03-02', '09:00', '17:00', 'Economy'),
(3, '2024-03-03', '07:00', '10:00', 'Economy'),
(4, '2024-03-04', '10:00', '18:00', 'Business'),
(5, '2024-03-05', '06:00', '14:00', 'Economy');
GO

-- Seats
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Price, Availability) VALUES
(1, 'A1', 'Economy', 1500.00, 1),
(1, 'A2', 'Economy', 1500.00, 1),
(1, 'B1', 'Economy', 1500.00, 0),
(2, 'A1', 'Business', 2000.00, 1),
(2, 'A2', 'Business', 2000.00, 1),
(3, 'A1', 'Economy', 2500.00, 1),
(3, 'A2', 'Economy', 2500.00, 1),
(4, 'A1', 'Economy', 1200.00, 1),
(4, 'A2', 'Economy', 1200.00, 1),
(5, 'A1', 'Business', 3360.00, 1),
(5, 'A2', 'Business', 3360.00, 1),
(6, 'A1', 'Economy', 3000.00, 1),
(6, 'A2', 'Economy', 3000.00, 1);
GO

-- Cancellation Policies
INSERT INTO CancellationPolicies (PolicyID, RefundAmount, TimeBeforeDeparture, Description) VALUES
('POL001', 100.0, 24, 'Full refund if cancelled 24+ hours before departure'),
('POL002', 50.0, 12, '50% refund if cancelled 12-24 hours before departure'),
('POL003', 0.0, 0, 'No refund if cancelled less than 12 hours before departure');
GO

-- Promotional Codes
INSERT INTO PromotionalCodes (Code, Percentage, ValidityDate, IsActive) VALUES
('WELCOME10', 10.0, '2024-12-31', 1),
('SUMMER25', 25.0, '2024-08-31', 1),
('FALL15', 15.0, '2024-11-30', 1);
GO

-- Support Queries
INSERT INTO SupportQueries (QueryID, Text, AskedOn, Status, Response, CustomerID, SupportStaffID) VALUES
('Q001', 'I cannot login to my account. It says invalid credentials.', '2024-01-15 09:30:00', 1, 'Please reset your password using the forgot password feature.', 'CUST001', 'SUP001'),
('Q002', 'My booking was not confirmed but payment was deducted.', '2024-01-16 14:20:00', 0, NULL, 'CUST002', NULL),
('Q003', 'How do I change my seat selection?', '2024-01-17 11:15:00', 1, 'You can change your seat up to 2 hours before departure from the manage booking section.', 'CUST003', 'SUP001');
GO

-- Payments
INSERT INTO Payment (PaymentID, Amount, PaymentMethod, PaymentStatus, TransactionID) VALUES
('PAY001', 3000.00, 'Credit Card', 'Completed', 'TXN0012024'),
('PAY002', 2000.00, 'Debit Card', 'Completed', 'TXN0022024'),
('PAY003', 5000.00, 'Credit Card', 'Completed', 'TXN0032024'),
('PAY004', 2400.00, 'JazzCash', 'Pending', 'TXN0042024'),
('PAY005', 6720.00, 'Credit Card', 'Completed', 'TXN0052024'),
('PAY006', 6000.00, 'Debit Card', 'Completed', 'TXN0062024');
GO

-- Reservations
INSERT INTO Reservation (ScheduleID, RouteID) VALUES
(1, 1),
(2, 1),
(3, 2),
(4, 3),
(5, 4),
(6, 5);
GO

-- Bookings
INSERT INTO Booking (BookingID, CustomerID, ScheduleID, SeatNumbers, TotalAmount, Status, PaymentID) VALUES
('BKG001', 'CUST001', 1, 'A1,A2', 3000.00, 'Confirmed', 'PAY001'),
('BKG002', 'CUST002', 2, 'A1', 2000.00, 'Confirmed', 'PAY002'),
('BKG003', 'CUST003', 3, 'A1,A2', 5000.00, 'Confirmed', 'PAY003'),
('BKG004', 'CUST004', 4, 'A1', 1200.00, 'Pending', 'PAY004'),
('BKG005', 'CUST001', 5, 'A1,A2', 6720.00, 'Confirmed', 'PAY005'),
('BKG006', 'CUST002', 6, 'A1', 3000.00, 'Confirmed', 'PAY006');
GO

-- ETickets
INSERT INTO ETickets (TicketID, BookingID, TicketData) VALUES
('TKT001', 'BKG001', 'Electronic Ticket for John Doe - Lahore to Islamabad - Seats: A1,A2'),
('TKT002', 'BKG002', 'Electronic Ticket for Jane Smith - Lahore to Islamabad - Seat: A1'),
('TKT003', 'BKG003', 'Electronic Ticket for Alice Johnson - Karachi to Lahore - Seats: A1,A2'),
('TKT005', 'BKG005', 'Electronic Ticket for John Doe - Lahore to Karachi - Seats: A1,A2'),
('TKT006', 'BKG006', 'Electronic Ticket for Jane Smith - Islamabad to Karachi - Seat: A1');
GO

-- Verify all data
SELECT 'ContactInfo' AS Table_Name, COUNT(*) AS Record_Count FROM ContactInfo
UNION ALL SELECT 'Users', COUNT(*) FROM Users
UNION ALL SELECT 'Route', COUNT(*) FROM Route
UNION ALL SELECT 'Schedule', COUNT(*) FROM Schedule
UNION ALL SELECT 'Seat', COUNT(*) FROM Seat
UNION ALL SELECT 'CancellationPolicies', COUNT(*) FROM CancellationPolicies
UNION ALL SELECT 'PromotionalCodes', COUNT(*) FROM PromotionalCodes
UNION ALL SELECT 'SupportQueries', COUNT(*) FROM SupportQueries
UNION ALL SELECT 'Payment', COUNT(*) FROM Payment
UNION ALL SELECT 'Reservation', COUNT(*) FROM Reservation
UNION ALL SELECT 'Booking', COUNT(*) FROM Booking
UNION ALL SELECT 'ETickets', COUNT(*) FROM ETickets;