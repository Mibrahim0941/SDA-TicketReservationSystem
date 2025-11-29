package catalogs;

import models.Route;
import models.Schedule;
import config.DatabaseConfig;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteCatalog {
    private static RouteCatalog instance;
    private Map<String, Route> routes;
    private Connection connection;
    private boolean databaseAvailable;

    private RouteCatalog() {
        this.routes = new HashMap<>();
        this.databaseAvailable = false;
        initializeDatabase();
        loadRoutesFromDB();
    }

    public static RouteCatalog getInstance() {
        if (instance == null) {
            instance = new RouteCatalog();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            String url = DatabaseConfig.getDbUrl();
            String user = DatabaseConfig.getDbUser();
            String password = DatabaseConfig.getDbPassword();
            
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            this.connection = DriverManager.getConnection(url, user, password);
            this.databaseAvailable = true;
            createTablesIfNotExists();
            
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            System.out.println("Using in-memory storage for routes");
            initializeSampleRoutes();
        }
    }

    private void createTablesIfNotExists() {
        if (!databaseAvailable) return;
        
        try (Statement stmt = connection.createStatement()) {
            // Create Route table if not exists
            String createRouteTable = """
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Route' AND xtype='U')
                CREATE TABLE Route (
                    RouteID NVARCHAR(50) PRIMARY KEY,
                    Source NVARCHAR(100) NOT NULL,
                    Destination NVARCHAR(100) NOT NULL,
                    BasePrice DECIMAL(10,2) NOT NULL,
                    IsActive BIT DEFAULT 1
                )
                """;
            
            // Create Schedule table if not exists
            String createScheduleTable = """
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Schedule' AND xtype='U')
                CREATE TABLE Schedule (
                    ScheduleID NVARCHAR(50) PRIMARY KEY,
                    RouteID NVARCHAR(50) NOT NULL,
                    Date DATE NOT NULL,
                    DepartureTime TIME NOT NULL,
                    ArrivalTime TIME NOT NULL,
                    Class NVARCHAR(20) NOT NULL,
                    TypePercentage DECIMAL(5,2) DEFAULT 100.0,
                    IsActive BIT DEFAULT 1,
                    FOREIGN KEY (RouteID) REFERENCES Route(RouteID) ON DELETE CASCADE
                )
                """;
            
            stmt.execute(createRouteTable);
            stmt.execute(createScheduleTable);
            System.out.println("Route and Schedule tables ensured to exist");
            
        } catch (SQLException e) {
            System.err.println("Table creation failed: " + e.getMessage());
        }
    }

    private void initializeSampleRoutes() {
        Route route1 = new Route("R001", "Lahore", "Islamabad", 1500.0);
        Route route2 = new Route("R002", "Karachi", "Lahore", 2500.0);
        Route route3 = new Route("R003", "Islamabad", "Peshawar", 1200.0);
        
        routes.put(route1.getRouteID(), route1);
        routes.put(route2.getRouteID(), route2);
        routes.put(route3.getRouteID(), route3);
        
        System.out.println("Initialized with " + routes.size() + " sample routes");
    }

    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    public List<Route> getAllRoutes() {
        return new ArrayList<>(routes.values());
    }

    public boolean addRoute(Route route) {
        if (route == null || routes.containsKey(route.getRouteID())) {
            return false;
        }
        
        // Check if route with same source and destination already exists
        boolean duplicateRoute = routes.values().stream()
            .anyMatch(r -> r.getSource().equalsIgnoreCase(route.getSource()) && 
                          r.getDestination().equalsIgnoreCase(route.getDestination()));
        
        if (duplicateRoute) {
            System.err.println("Route already exists: " + route.getSource() + " → " + route.getDestination());
            return false;
        }
        
        routes.put(route.getRouteID(), route);
        
        if (databaseAvailable) {
            return saveRouteToDB(route);
        }
        return true;
    }

    public boolean addScheduleToRoute(String routeId, Schedule schedule) {
        Route route = getRoute(routeId);
        if (route == null) {
            System.err.println("Route not found: " + routeId);
            return false;
        }
        
        route.addSchedule(schedule);
        
        if (databaseAvailable) {
            return saveScheduleToDB(routeId, schedule);
        }
        return true;
    }

    public List<Schedule> getRouteSchedules(String routeId) {
        Route route = getRoute(routeId);
        return route != null ? route.getAllSchedules() : new ArrayList<>();
    }

    public boolean updateRoute(Route route) {
        if (route == null || !routes.containsKey(route.getRouteID())) {
            return false;
        }
        
        routes.put(route.getRouteID(), route);
        
        if (databaseAvailable) {
            return updateRouteInDB(route);
        }
        return true;
    }

    public void refresh() {
        if (databaseAvailable) {
            loadRoutesFromDB();
        }
    }

    private boolean saveRouteToDB(Route route) {
        String sql = "INSERT INTO Route (RouteID, Source, Destination, BasePrice) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, route.getRouteID());
            pstmt.setString(2, route.getSource());
            pstmt.setString(3, route.getDestination());
            pstmt.setDouble(4, route.getBasePrice());
            pstmt.executeUpdate();
            System.out.println("Route saved to database: " + route.getRouteID());
            return true;
        } catch (SQLException e) {
            System.err.println("Save route failed: " + e.getMessage());
            // Rollback in-memory change
            routes.remove(route.getRouteID());
            return false;
        }
    }

    private boolean saveScheduleToDB(String routeId, Schedule schedule) {
        String sql = "INSERT INTO Schedule (ScheduleID, RouteID, Date, DepartureTime, ArrivalTime, Class, TypePercentage) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, schedule.getScheduleID());
            pstmt.setString(2, routeId);
            pstmt.setDate(3, Date.valueOf(schedule.getDate()));
            pstmt.setTime(4, Time.valueOf(schedule.getDepartureTime()));
            pstmt.setTime(5, Time.valueOf(schedule.getArrivalTime()));
            pstmt.setString(6, schedule.getScheduleClass());
            pstmt.setDouble(7, schedule.getTypePercentage());
            pstmt.executeUpdate();
            System.out.println("Schedule saved to database: " + schedule.getScheduleID());
            return true;
        } catch (SQLException e) {
            System.err.println("Save schedule failed: " + e.getMessage());
            return false;
        }
    }

    private boolean updateRouteInDB(Route route) {
        String sql = "UPDATE Route SET Source = ?, Destination = ?, BasePrice = ? WHERE RouteID = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, route.getSource());
            pstmt.setString(2, route.getDestination());
            pstmt.setDouble(3, route.getBasePrice());
            pstmt.setString(4, route.getRouteID());
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Route updated in database: " + route.getRouteID() + " (" + rowsAffected + " rows affected)");
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Update route failed: " + e.getMessage());
            return false;
        }
    }

    private void loadRoutesFromDB() {
        if (!databaseAvailable) {
            System.out.println("Database not available, using in-memory routes");
            return;
        }
        
        String routesSql = "SELECT * FROM Route WHERE IsActive = 1";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(routesSql)) {
            
            routes.clear();
            
            while (rs.next()) {
                String routeId = rs.getString("RouteID");
                String source = rs.getString("Source");
                String destination = rs.getString("Destination");
                double basePrice = rs.getDouble("BasePrice");
                
                Route route = new Route(routeId, source, destination, basePrice);
                loadSchedulesForRouteFromDB(route);
                routes.put(routeId, route);
            }
            System.out.println("Loaded " + routes.size() + " routes from database");
            
        } catch (SQLException e) {
            System.err.println("Load routes failed: " + e.getMessage());
            if (routes.isEmpty()) {
                initializeSampleRoutes();
            }
        }
    }

    private void loadSchedulesForRouteFromDB(Route route) {
        String sql = "SELECT * FROM Schedule WHERE RouteID = ? AND IsActive = 1";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, route.getRouteID());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String scheduleId = rs.getString("ScheduleID");
                LocalDate date = rs.getDate("Date").toLocalDate();
                LocalTime departureTime = rs.getTime("DepartureTime").toLocalTime();
                LocalTime arrivalTime = rs.getTime("ArrivalTime").toLocalTime();
                String scheduleClass = rs.getString("Class");
                double typePercentage = rs.getDouble("TypePercentage");
                
                Schedule schedule = new Schedule(scheduleId, date, departureTime, arrivalTime, scheduleClass);
                schedule.setTypePercentage(typePercentage);
                route.addSchedule(schedule);
            }
            System.out.println("Loaded " + route.getSchedules().size() + " schedules for route " + route.getRouteID());
            
        } catch (SQLException e) {
            System.err.println("Load schedules failed for route " + route.getRouteID() + ": " + e.getMessage());
        }
    }

    public boolean routeExists(String routeId) {
        return routes.containsKey(routeId);
    }

    public boolean deleteRoute(String routeId) {
        if (!routes.containsKey(routeId)) {
            return false;
        }
        
        if (databaseAvailable) {
            String sql = "UPDATE Route SET IsActive = 0 WHERE RouteID = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, routeId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    routes.remove(routeId);
                    System.out.println("Route deleted from database: " + routeId);
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("Delete route failed: " + e.getMessage());
            }
        } else {
            routes.remove(routeId);
            System.out.println("Route deleted from memory: " + routeId);
            return true;
        }
        return false;
    }

    public void closeConnection() {
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