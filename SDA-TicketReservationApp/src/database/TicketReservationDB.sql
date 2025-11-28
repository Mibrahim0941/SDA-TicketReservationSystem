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



-- Insert default admin user
INSERT INTO ContactInfo (Email, PhoneNum) 
VALUES ('admin@example.com', '1234567890');

INSERT INTO Users (UserID, Name, Password, Username, ContactID, UserType)
VALUES ('admin', 'Administrator', 'password123', 'admin', SCOPE_IDENTITY(), 'Admin');

-- Insert sample customers
INSERT INTO ContactInfo (Email, PhoneNum) 
VALUES ('john@example.com', '1234567890');

INSERT INTO Users (UserID, Name, Password, Username, ContactID, UserType)
VALUES ('1', 'John Doe', 'password123', 'john_doe', SCOPE_IDENTITY(), 'Customer');

INSERT INTO ContactInfo (Email, PhoneNum) 
VALUES ('jane@example.com', '0987654321');

INSERT INTO Users (UserID, Name, Password, Username, ContactID, UserType)
VALUES ('2', 'Jane Smith', 'password123', 'jane_smith', SCOPE_IDENTITY(), 'Customer');
GO

-- Add Admin User
INSERT INTO ContactInfo (Email, PhoneNum) 
VALUES ('admin@ticketgenie.com', '0300-1234567');

INSERT INTO Users (UserID, Name, Password, Username, ContactID, UserType) 
VALUES ('ADM_001', 'System Administrator', 'admin123', 'admin', IDENT_CURRENT('ContactInfo'), 'Admin');

-- Add Support Staff User
INSERT INTO ContactInfo (Email, PhoneNum) 
VALUES ('support@ticketgenie.com', '0300-7654321');

INSERT INTO Users (UserID, Name, Password, Username, ContactID, UserType) 
VALUES ('SUP_001', 'Support Agent', 'support123', 'support', IDENT_CURRENT('ContactInfo'), 'SupportStaff');


INSERT INTO ContactInfo (Email, PhoneNum) 
VALUES ('support2@ticketgenie.com', '0300-7654322');

INSERT INTO Users (UserID, Name, Password, Username, ContactID, UserType) 
VALUES ('SUP_002', 'Support Agent', 'support123', 'support2', IDENT_CURRENT('ContactInfo'), 'SupportStaff');

delete from Users
delete from ContactInfo
Select* from ContactInfo
select * from Users
Select* from SupportQueries

-- Create SupportQueries Table
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

-- Insert Sample Support Queries

-- Query from Customer C001 (Pending - Unassigned)
INSERT INTO SupportQueries (QueryID, Text, AskedOn, Status, Response, CustomerID, SupportStaffID) 
VALUES ('Q001', 'I cannot login to my account. It says invalid credentials even though I am sure my password is correct.', '2024-01-15 09:30:00', 0, NULL, 'CUST-029de333-c535-4172-953c-37dee5231046', NULL);

-- Query from Customer C002 (Assigned but pending response)
INSERT INTO SupportQueries (QueryID, Text, AskedOn, Status, Response, CustomerID, SupportStaffID) 
VALUES ('Q002', 'My order #ORD-12345 has not been delivered yet. It was supposed to arrive 3 days ago. Can you check the status?', '2024-01-16 14:20:00', 0, NULL, 'CUST-30666e01-66ea-41de-a4f9-82bd53cc8af0', 'SUP_001');

-- Query from Customer C003 (Resolved)
INSERT INTO SupportQueries (QueryID, Text, AskedOn, Status, Response, CustomerID, SupportStaffID) 
VALUES ('Q003', 'How do I reset my password? I forgot my password and cannot access my account.', '2024-01-10 11:15:00', 1, 'You can reset your password by clicking on "Forgot Password" on the login page. We have sent a password reset link to your registered email address.', 'CUST-7a8a92bf-26a2-4f39-8fc1-075e151af615', 'SUP_002');


-- Create Database
CREATE DATABASE TicketBookingSystem;
GO

USE TicketBookingSystem;
GO

-- Price Table
CREATE TABLE Price (
    PriceID INT IDENTITY(1,1) PRIMARY KEY,
    Amount DECIMAL(10,2) NOT NULL,
    Currency NVARCHAR(3) DEFAULT 'USD'
);
GO

-- Route Table
CREATE TABLE Route (
    RouteID INT IDENTITY(1,1) PRIMARY KEY,
    Source NVARCHAR(100) NOT NULL,
    Destination NVARCHAR(100) NOT NULL,
    BasePrice DECIMAL(10,2) NOT NULL,
    IsActive BIT DEFAULT 1
);
GO


-- Schedule Table
CREATE TABLE Schedule (
    ScheduleID INT IDENTITY(1,1) PRIMARY KEY,
    RouteID INT NOT NULL,
    Date DATE NOT NULL,
    DepartureTime TIME NOT NULL,
    ArrivalTime TIME NOT NULL,
    Class NVARCHAR(20) NOT NULL,
    IsActive BIT DEFAULT 1,
    FOREIGN KEY (RouteID) REFERENCES Route(RouteID)
);
GO

-- Seat Table
CREATE TABLE Seat (
    SeatID INT IDENTITY(1,1) PRIMARY KEY,
    ScheduleID INT NOT NULL,
    SeatNumber NVARCHAR(10) NOT NULL,
    SeatType NVARCHAR(20) NOT NULL, -- Economy, Business, First
    Availability BIT DEFAULT 1,
    PriceAdjustment DECIMAL(5,2) DEFAULT 0.0,
    FOREIGN KEY (ScheduleID) REFERENCES Schedule(ScheduleID)
);
GO


-- Payment Table (Added as required by Booking table)
CREATE TABLE Payment (
    PaymentID INT IDENTITY(1,1) PRIMARY KEY,
    Amount DECIMAL(10,2) NOT NULL,
    PaymentMethod NVARCHAR(50) NOT NULL, -- Credit Card, Debit Card, PayPal, etc.
    PaymentStatus NVARCHAR(20) DEFAULT 'Pending', -- Pending, Completed, Failed, Refunded
    TransactionID NVARCHAR(100), -- External transaction ID from payment gateway
    PaymentDate DATETIME DEFAULT GETDATE()
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

-- Booking Table
CREATE TABLE Booking (
    BookingID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID NVARCHAR(50) NOT NULL,
    ReservationID INT NOT NULL,
    BookingDateTime DATETIME DEFAULT GETDATE(),
    TotalAmount DECIMAL(10,2) NOT NULL,
    Status NVARCHAR(20) DEFAULT 'Confirmed', -- Confirmed, Cancelled, Completed
    PaymentID INT,
    FOREIGN KEY (CustomerID) REFERENCES Users(UserID),
    FOREIGN KEY (ReservationID) REFERENCES Reservation(ReservationID),
    FOREIGN KEY (PaymentID) REFERENCES Payment(PaymentID)
);
GO


-- Insert Sample Data

-- Insert into Price Table
INSERT INTO Price (Amount, Currency) VALUES
(100.00, 'USD'),
(150.00, 'USD'),
(200.00, 'USD'),
(75.00, 'USD'),
(120.00, 'USD'),
(180.00, 'USD'),
(220.00, 'USD'),
(90.00, 'USD');
GO

-- Insert into Route Table
INSERT INTO Route (Source, Destination, BasePrice) VALUES
('New York', 'Los Angeles', 300.00),
('Chicago', 'Miami', 250.00),
('San Francisco', 'Seattle', 150.00),
('Boston', 'Washington DC', 120.00),
('Denver', 'Phoenix', 180.00),
('Las Vegas', 'Dallas', 220.00),
('Atlanta', 'Orlando', 130.00),
('Portland', 'Salt Lake City', 160.00);
GO

-- Insert into Schedule Table
INSERT INTO Schedule (RouteID, Date, DepartureTime, ArrivalTime, Class) VALUES
-- Route 1: New York to Los Angeles
(17, '2026-02-15', '08:00', '11:00', 'Economy'),
(17, '2026-02-15', '14:00', '17:00', 'Business'),
(17, '2026-02-16', '09:00', '12:00', 'First')

-- Route 2: Chicago to Miami
(2, '2024-02-16', '10:30', '13:30', 'Economy'),
(2, '2024-02-17', '15:00', '18:00', 'Business'),

-- Route 3: San Francisco to Seattle
(3, '2024-02-17', '07:00', '09:00', 'Economy'),
(3, '2024-02-18', '12:00', '14:00', 'First'),

-- Route 4: Boston to Washington DC
(4, '2024-02-18', '11:00', '12:30', 'Economy'),

-- Route 5: Denver to Phoenix
(5, '2024-02-19', '13:00', '15:00', 'Business');
GO

-- Insert into Seat Table
-- Schedule 1 (Economy)
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Availability, PriceAdjustment) VALUES
(13, '1A', 'Economy', 1, 0.0),
(13, '1B', 'Economy', 1, 0.0),
(13, '1C', 'Economy', 0, 0.0),
(13, '2A', 'Economy', 1, 0.0),
(13, '2B', 'Economy', 1, 0.0),
(13, '2C', 'Economy', 1, 0.0),
(13, '3A', 'Economy', 1, 0.0),
(13, '3B', 'Economy', 1, 0.0),
(13, '3C', 'Economy', 0, 0.0);

-- Schedule 2 (Business)
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Availability, PriceAdjustment) VALUES
(2, '1A', 'Business', 1, 50.0),
(2, '1B', 'Business', 1, 50.0),
(2, '2A', 'Business', 0, 50.0),
(2, '2B', 'Business', 1, 50.0),
(2, '3A', 'Business', 1, 50.0),
(2, '3B', 'Business', 1, 50.0);

-- Schedule 3 (First Class)
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Availability, PriceAdjustment) VALUES
(3, '1A', 'First', 1, 100.0),
(3, '1B', 'First', 1, 100.0),
(3, '2A', 'First', 1, 100.0),
(3, '2B', 'First', 0, 100.0);

-- Schedule 4 (Economy)
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Availability) VALUES
(4, '1A', 'Economy', 1),
(4, '1B', 'Economy', 1),
(4, '1C', 'Economy', 1),
(4, '2A', 'Economy', 1),
(4, '2B', 'Economy', 0),
(4, '2C', 'Economy', 1);

-- Schedule 5 (Economy)
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Availability) VALUES
(5, '1A', 'Economy', 1),
(5, '1B', 'Economy', 1),
(5, '1C', 'Economy', 1),
(5, '2A', 'Economy', 0),
(5, '2B', 'Economy', 1),
(5, '2C', 'Economy', 1);

-- Schedule 6 (Business)
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Availability, PriceAdjustment) VALUES
(6, '1A', 'Business', 1, 40.0),
(6, '1B', 'Business', 1, 40.0),
(6, '2A', 'Business', 1, 40.0),
(6, '2B', 'Business', 0, 40.0);

-- Schedule 7 (First Class)
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Availability, PriceAdjustment) VALUES
(7, '1A', 'First', 1, 120.0),
(7, '1B', 'First', 1, 120.0),
(7, '2A', 'First', 1, 120.0),
(7, '2B', 'First', 1, 120.0);

-- Schedule 8 (Economy)
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Availability) VALUES
(8, '1A', 'Economy', 1),
(8, '1B', 'Economy', 1),
(8, '1C', 'Economy', 0),
(8, '2A', 'Economy', 1),
(8, '2B', 'Economy', 1),
(8, '2C', 'Economy', 1);

-- Schedule 9 (Business)
INSERT INTO Seat (ScheduleID, SeatNumber, SeatType, Availability, PriceAdjustment) VALUES
(9, '1A', 'Business', 1, 60.0),
(9, '1B', 'Business', 1, 60.0),
(9, '2A', 'Business', 1, 60.0),
(9, '2B', 'Business', 0, 60.0);
GO

-- Insert into Payment Table
INSERT INTO Payment (Amount, PaymentMethod, PaymentStatus, TransactionID) VALUES
(300.00, 'Credit Card', 'Completed', 'TXN0012024'),
(450.00, 'Credit Card', 'Completed', 'TXN0022024'),
(400.00, 'Debit Card', 'Completed', 'TXN0032024'),
(250.00, 'PayPal', 'Completed', 'TXN0042024'),
(180.00, 'Credit Card', 'Completed', 'TXN0052024'),
(520.00, 'Debit Card', 'Completed', 'TXN0062024'),
(320.00, 'Credit Card', 'Pending', 'TXN0072024'),
(280.00, 'PayPal', 'Failed', 'TXN0082024'),
(190.00, 'Credit Card', 'Completed', 'TXN0092024'),
(460.00, 'Debit Card', 'Completed', 'TXN0102024');
GO

-- Insert into Reservation Table
INSERT INTO Reservation (ScheduleID, RouteID) VALUES
(1, 1),  -- Schedule 1, Route 1
(2, 1),  -- Schedule 2, Route 1
(3, 1),  -- Schedule 3, Route 1
(4, 2),  -- Schedule 4, Route 2
(5, 2),  -- Schedule 5, Route 2
(6, 3),  -- Schedule 6, Route 3
(7, 3),  -- Schedule 7, Route 3
(8, 4),  -- Schedule 8, Route 4
(9, 5),  -- Schedule 9, Route 5
(1, 1);  -- Schedule 1, Route 1 (another reservation)
GO

-- Insert into Booking Table
-- Assuming you have Users table with these UserIDs
INSERT INTO Booking (CustomerID, ReservationID, TotalAmount, Status, PaymentID) VALUES
('CUST-029de333-c535-4172-953c-37dee5231046', 1, 300.00, 'Confirmed', 1),
('CUST-30666e01-66ea-41de-a4f9-82bd53cc8af0', 2, 450.00, 'Confirmed', 2),
('CUST-7a8a92bf-26a2-4f39-8fc1-075e151af615', 3, 400.00, 'Confirmed', 3),
('CUST-029de333-c535-4172-953c-37dee5231046', 4, 250.00, 'Confirmed', 4),
('CUST-30666e01-66ea-41de-a4f9-82bd53cc8af0', 5, 180.00, 'Confirmed', 5),
('CUST-7a8a92bf-26a2-4f39-8fc1-075e151af615', 6, 520.00, 'Confirmed', 6),
('CUST-029de333-c535-4172-953c-37dee5231046', 7, 320.00, 'Pending', 7),
('CUST-30666e01-66ea-41de-a4f9-82bd53cc8af0', 8, 280.00, 'Cancelled', 8),
('CUST-7a8a92bf-26a2-4f39-8fc1-075e151af615', 9, 190.00, 'Confirmed', 9),
('CUST-029de333-c535-4172-953c-37dee5231046', 10, 460.00, 'Confirmed', 10);
GO

select*from Users
select * from booking
select * from Reservation
Delete from Route
Select*from Route
Select* from Schedule

INSERT INTO Route (Source, Destination, BasePrice) VALUES
('Lahore', 'Islamabad', 2500)