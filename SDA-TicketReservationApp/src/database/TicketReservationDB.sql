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