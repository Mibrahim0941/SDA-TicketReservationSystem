package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private static final Properties properties = new Properties();
    
    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("config/database.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find database.properties");
                // Set default values
                properties.setProperty("db.url", "jdbc:sqlserver://localhost:1433;databaseName=TicketGenieDB;encrypt=true;trustServerCertificate=true");
                properties.setProperty("db.user", "sa");
                properties.setProperty("db.password", "12345678");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static String getDbUrl() {
        return properties.getProperty("db.url");
    }
    
    public static String getDbUser() {
        return properties.getProperty("db.user");
    }
    
    public static String getDbPassword() {
        return properties.getProperty("db.password");
    }
}