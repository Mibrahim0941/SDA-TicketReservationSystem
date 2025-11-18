package catalogs;

import models.Route;
import config.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;

public class RouteCatalog {
    private ArrayList<Route> routes;
    private Connection connection;
    private boolean databaseAvailable;
    
    public RouteCatalog() {
        this.routes = new ArrayList<>();
        this.databaseAvailable = false;
        
        try {
            // Use the new DatabaseConfig methods
            String url = DatabaseConfig.getDbUrl();
            String user = DatabaseConfig.getDbUser();
            String password = DatabaseConfig.getDbPassword();
            
            // Load SQL Server JDBC driver
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            
            this.connection = DriverManager.getConnection(url, user, password);
            this.databaseAvailable = true;
            loadRoutesFromDB();
            
        } catch (ClassNotFoundException e) {
            System.err.println("SQL Server JDBC driver not found: " + e.getMessage());
            System.out.println("Using in-memory storage instead");
            initializeSampleRoutes();
        } catch (SQLException e) {
            System.err.println("Failed to initialize RouteCatalog with database: " + e.getMessage());
            System.out.println("Using in-memory storage instead");
            initializeSampleRoutes();
        }
    }
    
    private void initializeSampleRoutes() {
        // Add some sample routes for testing
        routes.add(new Route("R001", "Lahore", "Karachi", 3500.0));
        routes.add(new Route("R002", "Lahore", "Islamabad", 1500.0));
        routes.add(new Route("R003", "Karachi", "Islamabad", 4000.0));
        routes.add(new Route("R004", "Islamabad", "Peshawar", 1200.0));
        routes.add(new Route("R005", "Karachi", "Quetta", 3200.0));
        System.out.println("Initialized with " + routes.size() + " sample routes");
    }
    
    private void loadRoutesFromDB() {
        if (!databaseAvailable) {
            System.out.println("Database not available, using in-memory data");
            return;
        }
        
        // First, ensure the table exists
        createTableIfNotExists();
        
        String query = "SELECT * FROM Routes ORDER BY RouteID";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            routes.clear();
            
            while (rs.next()) {
                String routeID = rs.getString("RouteID");
                String source = rs.getString("Source");
                String destination = rs.getString("Destination");
                double basePrice = rs.getDouble("BasePrice");
                
                Route route = new Route(routeID, source, destination, basePrice);
                routes.add(route);
            }
            
            System.out.println("Loaded " + routes.size() + " routes from database");
            
        } catch (SQLException e) {
            System.err.println("Error loading routes from database: " + e.getMessage());
            e.printStackTrace();
            // Fallback to sample data if database fails
            if (routes.isEmpty()) {
                initializeSampleRoutes();
            }
        }
    }
    
    private void createTableIfNotExists() {
        if (!databaseAvailable) {
            return;
        }
        
        String createTableSQL = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Routes' AND xtype='U') " +
                               "CREATE TABLE Routes (" +
                               "RouteID VARCHAR(20) PRIMARY KEY, " +
                               "Source VARCHAR(100) NOT NULL, " +
                               "Destination VARCHAR(100) NOT NULL, " +
                               "BasePrice DECIMAL(10,2) NOT NULL, " +
                               "CreatedAt DATETIME DEFAULT GETDATE(), " +
                               "UpdatedAt DATETIME DEFAULT GETDATE(), " +
                               "UNIQUE(Source, Destination)" +
                               ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Routes table ensured to exist");
        } catch (SQLException e) {
            System.err.println("Error creating Routes table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean addToCatalog(Route route) {
        // Check if route already exists
        if (checkRoute(route.getSource(), route.getDestination())) {
            System.err.println("Route already exists: " + route.getSource() + " -> " + route.getDestination());
            return false;
        }
        
        if (databaseAvailable) {
            String query = "INSERT INTO Routes (RouteID, Source, Destination, BasePrice) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, route.getRouteID());
                pstmt.setString(2, route.getSource());
                pstmt.setString(3, route.getDestination());
                pstmt.setDouble(4, route.getBasePrice());
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    routes.add(route);
                    System.out.println("Route added successfully to database: " + route.getRouteID());
                    return true;
                }
                
            } catch (SQLException e) {
                System.err.println("Error adding route to database: " + e.getMessage());
                e.printStackTrace();
                // Fallback to in-memory storage
                System.out.println("Falling back to in-memory storage");
            }
        }
        
        // Add to in-memory list if database is not available or insertion failed
        routes.add(route);
        System.out.println("Route added to in-memory storage: " + route.getRouteID());
        return true;
    }
    
    public boolean checkRoute(String source, String destination) {
        return routes.stream().anyMatch(route -> 
            route.verifySrcDst(source, destination));
    }
    
    public Route getRoute(String routeID) {
        return routes.stream()
                .filter(route -> route.getRouteID().equals(routeID))
                .findFirst()
                .orElse(null);
    }
    
    public ArrayList<Route> getAllRoutes() {
        return new ArrayList<>(routes);
    }
    
    public ArrayList<Route> getRoutesBySource(String source) {
        ArrayList<Route> result = new ArrayList<>();
        for (Route route : routes) {
            if (route.getSource().equalsIgnoreCase(source)) {
                result.add(route);
            }
        }
        return result;
    }
    
    public ArrayList<Route> getRoutesByDestination(String destination) {
        ArrayList<Route> result = new ArrayList<>();
        for (Route route : routes) {
            if (route.getDestination().equalsIgnoreCase(destination)) {
                result.add(route);
            }
        }
        return result;
    }
    
    public boolean updateRoute(Route updatedRoute) {
        if (databaseAvailable) {
            String query = "UPDATE Routes SET Source = ?, Destination = ?, BasePrice = ?, UpdatedAt = GETDATE() WHERE RouteID = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, updatedRoute.getSource());
                pstmt.setString(2, updatedRoute.getDestination());
                pstmt.setDouble(3, updatedRoute.getBasePrice());
                pstmt.setString(4, updatedRoute.getRouteID());
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Update local list
                    updateLocalRoute(updatedRoute);
                    System.out.println("Route updated successfully in database: " + updatedRoute.getRouteID());
                    return true;
                }
                
            } catch (SQLException e) {
                System.err.println("Error updating route in database: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Update in-memory list if database is not available or update failed
        boolean updated = updateLocalRoute(updatedRoute);
        if (updated) {
            System.out.println("Route updated in in-memory storage: " + updatedRoute.getRouteID());
        }
        return updated;
    }
    
    private boolean updateLocalRoute(Route updatedRoute) {
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).getRouteID().equals(updatedRoute.getRouteID())) {
                routes.set(i, updatedRoute);
                return true;
            }
        }
        return false;
    }
    
    public boolean deleteRoute(String routeID) {
        if (databaseAvailable) {
            String query = "DELETE FROM Routes WHERE RouteID = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, routeID);
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    routes.removeIf(route -> route.getRouteID().equals(routeID));
                    System.out.println("Route deleted successfully from database: " + routeID);
                    return true;
                }
                
            } catch (SQLException e) {
                System.err.println("Error deleting route from database: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Delete from in-memory list if database is not available or deletion failed
        boolean deleted = routes.removeIf(route -> route.getRouteID().equals(routeID));
        if (deleted) {
            System.out.println("Route deleted from in-memory storage: " + routeID);
        }
        return deleted;
    }
    
    public void refresh() {
        if (databaseAvailable) {
            loadRoutesFromDB();
        }
    }
    
    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }
    
    public int getRouteCount() {
        return routes.size();
    }
    
    // Close connection when done
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}