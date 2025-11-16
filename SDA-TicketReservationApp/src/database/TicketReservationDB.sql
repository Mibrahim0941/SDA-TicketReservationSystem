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
