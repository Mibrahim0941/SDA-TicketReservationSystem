package catalogs;

import models.SupportQuery;
import models.Customer;
import models.SupportStaff;
import database.DatabaseConnection;
import java.util.Date;

public class QueryCatalog {
    private java.util.ArrayList<SupportQuery> queries;

    public QueryCatalog() {
        this.queries = new java.util.ArrayList<>();
        loadQueriesFromDatabase();
    }

    private void loadQueriesFromDatabase() {
        String query = "SELECT " +
                  "q.QueryID, q.Text, q.AskedOn, q.Status, q.Response, " +
                  "q.CustomerID, q.SupportStaffID, " +
                  "cust.Name as CustomerName, " +
                  "cust_contact.Email as CustomerEmail, " +
                  "cust_contact.PhoneNum as CustomerPhone, " +
                  "staff.Name as StaffName, " +
                  "staff_contact.Email as StaffEmail, " +
                  "staff_contact.PhoneNum as StaffPhone " +
                  "FROM SupportQueries q " +
                  "LEFT JOIN Users cust ON q.CustomerID = cust.UserID " +
                  "LEFT JOIN ContactInfo cust_contact ON cust.ContactID = cust_contact.ContactID " +
                  "LEFT JOIN Users staff ON q.SupportStaffID = staff.UserID " +
                  "LEFT JOIN ContactInfo staff_contact ON staff.ContactID = staff_contact.ContactID " +
                  "ORDER BY q.AskedOn DESC";

        try {
            java.sql.Connection conn = DatabaseConnection.getConnection();
            java.sql.PreparedStatement stmt = conn.prepareStatement(query);
            java.sql.ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                SupportQuery supportQuery = createQueryFromResultSet(rs);
                if (supportQuery != null) {
                    queries.add(supportQuery);
                }
            }
            
            rs.close();
            stmt.close();
            
            System.out.println("Loaded " + queries.size() + " queries from database.");
            
        } catch (java.sql.SQLException e) {
            System.err.println("Error loading queries from database: " + e.getMessage());
        }
    }

    public boolean addToCatalog(SupportQuery query) {
        queries.add(query);
        return persistQueryToDatabase(query);
    }

    private boolean persistQueryToDatabase(SupportQuery query) {
        java.sql.Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            String insertQuery = "INSERT INTO SupportQueries (QueryID, Text, AskedOn, Status, Response, CustomerID, SupportStaffID) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            java.sql.PreparedStatement stmt = conn.prepareStatement(insertQuery);
            stmt.setString(1, query.getQueryID());
            stmt.setString(2, query.getText());
            stmt.setTimestamp(3, new java.sql.Timestamp(query.getAskedOn().getTime()));
            stmt.setBoolean(4, query.isStatus());
            stmt.setString(5, query.getResponse());
            
            if (query.getCustomer() != null) {
                stmt.setString(6, query.getCustomer().getUserID());
            } else {
                stmt.setNull(6, java.sql.Types.VARCHAR);
            }
            
            if (query.getSupportStaff() != null) {
                stmt.setString(7, query.getSupportStaff().getUserID());
            } else {
                stmt.setNull(7, java.sql.Types.VARCHAR);
            }
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            if (rowsAffected > 0) {
                conn.commit();
                System.out.println("Query persisted to database: " + query.getQueryID());
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (java.sql.SQLException e) {
            if (conn != null) {
                try { 
                    conn.rollback(); 
                } catch (java.sql.SQLException ex) {
                    System.err.println("Error during rollback: " + ex.getMessage());
                }
            }
            System.err.println("Error persisting query to database: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { 
                    conn.setAutoCommit(true); 
                } catch (java.sql.SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }

    private SupportQuery createQueryFromResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        try {
            String queryID = rs.getString("QueryID");
            String text = rs.getString("Text");
            Date askedOn = new Date(rs.getTimestamp("AskedOn").getTime());
            boolean status = rs.getBoolean("Status");
            String response = rs.getString("Response");
            
            Customer customer = null;
            String customerID = rs.getString("CustomerID");
            if (customerID != null && !rs.wasNull()) {
                customer = new Customer(
                    customerID,
                    rs.getString("CustomerName"),
                    "", 
                    "", 
                    rs.getString("CustomerEmail"),
                    rs.getString("CustomerPhone")
                );
            }
            
            SupportStaff supportStaff = null;
            String staffID = rs.getString("SupportStaffID");
            if (staffID != null && !rs.wasNull()) {
                supportStaff = new SupportStaff(
                    staffID,
                    rs.getString("StaffName"),
                    "", 
                    "",
                    rs.getString("StaffEmail"),
                    rs.getString("StaffPhone")
                );
            }
            
            SupportQuery query = new SupportQuery(text, askedOn, queryID, supportStaff, customer);
            query.setStatus(status);
            query.setResponse(response);
            
            return query;
            
        } catch (Exception e) {
            System.err.println("Error creating SupportQuery from ResultSet: " + e.getMessage());
            return null;
        }
    }

    
    public SupportQuery findQuery(String queryID) {
        for (SupportQuery query : queries) {
            if (query.matchID(queryID)) {
                return query;
            }
        }
        return null;
    }

    public SupportQuery supportQuery() {
        return new SupportQuery();
    }

    public boolean updateQueryResponse(String queryID, String response) {
        SupportQuery query = findQuery(queryID);
        if (query != null) {
            query.selfResponse(response); 
            return updateQueryInDatabase(query);
        }
        return false;
    }

    private boolean updateQueryInDatabase(SupportQuery query) {
        String updateQuery = "UPDATE SupportQueries SET Text = ?, Status = ?, Response = ?, SupportStaffID = ? WHERE QueryID = ?";
        try {
            java.sql.Connection conn = DatabaseConnection.getConnection();
            java.sql.PreparedStatement stmt = conn.prepareStatement(updateQuery);
            stmt.setString(1, query.getText());
            stmt.setBoolean(2, query.isStatus());
            stmt.setString(3, query.getResponse());
            
            if (query.getSupportStaff() != null) {
                stmt.setString(4, query.getSupportStaff().getUserID());
                System.out.println("Updating SupportStaffID to: " + query.getSupportStaff().getUserID());
            } else {
                stmt.setNull(4, java.sql.Types.VARCHAR);
            }
            
            stmt.setString(5, query.getQueryID());
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (java.sql.SQLException e) {
            System.err.println("Error updating query in database: " + e.getMessage());
            return false;
        }
    }

    public java.util.ArrayList<SupportQuery> getQueriesByCustomer(Customer customer) {
        java.util.ArrayList<SupportQuery> result = new java.util.ArrayList<>();
        for (SupportQuery query : queries) {
            if (query.getCustomer() != null && query.getCustomer().getUserID().equals(customer.getUserID())) {
                result.add(query);
            }
        }
        return result;
    }

    public java.util.ArrayList<SupportQuery> getQueriesBySupportStaff(SupportStaff staff) {
        java.util.ArrayList<SupportQuery> result = new java.util.ArrayList<>();
        for (SupportQuery query : queries) {
            if (query.getSupportStaff() != null && query.getSupportStaff().getUserID().equals(staff.getUserID())) {
                result.add(query);
            }
        }
        return result;
    }

    public java.util.ArrayList<SupportQuery> getPendingQueries() {
        java.util.ArrayList<SupportQuery> result = new java.util.ArrayList<>();
        for (SupportQuery query : queries) {
            if (!query.isStatus()) {
                result.add(query);
            }
        }
        return result;
    }

    public java.util.ArrayList<SupportQuery> getResolvedQueries() {
        java.util.ArrayList<SupportQuery> result = new java.util.ArrayList<>();
        for (SupportQuery query : queries) {
            if (query.isStatus()) {
                result.add(query);
            }
        }
        return result;
    }

    public java.util.ArrayList<SupportQuery> getAllQueries() {
        return new java.util.ArrayList<>(queries);
    }

    public int getQueryCount() {
        return queries.size();
    }

    public int getPendingQueryCount() {
        int count = 0;
        for (SupportQuery query : queries) {
            if (!query.isStatus()) {
                count++;
            }
        }
        return count;
    }

    public boolean removeQuery(String queryID) {
        SupportQuery query = findQuery(queryID);
        if (query != null) {
            queries.remove(query);
            return removeQueryFromDatabase(queryID);
        }
        return false;
    }

    private boolean removeQueryFromDatabase(String queryID) {
        String deleteQuery = "DELETE FROM SupportQueries WHERE QueryID = ?";
        
        try {
            java.sql.Connection conn = DatabaseConnection.getConnection();
            java.sql.PreparedStatement stmt = conn.prepareStatement(deleteQuery);
            stmt.setString(1, queryID);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (java.sql.SQLException e) {
            System.err.println("Error removing query from database: " + e.getMessage());
            return false;
        }
    }

    public boolean assignSupportStaff(String queryID, SupportStaff staff) {
        SupportQuery query = findQuery(queryID);
        if (query != null) {
            query.setSupportStaff(staff);
            return updateQueryInDatabase(query);
        }
        return false;
    }

    public boolean updateQuery(SupportQuery query) {
        return updateQueryInDatabase(query);
    }

    public java.util.ArrayList<SupportQuery> getUnassignedQueries() {
        java.util.ArrayList<SupportQuery> result = new java.util.ArrayList<>();
        for (SupportQuery query : queries) {
            if (query.getSupportStaff() == null) {
                result.add(query);
            }
        }
        return result;
    }
}