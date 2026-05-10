package com.equipment.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String SERVER = "LALEYKA\\SQLEXPRESS";
    private static final String DATABASE = "equipment_booking_system";
    private static final String USER = "app_user";
    private static final String PASSWORD = "sajal1234567";

    private static final String URL =
            "jdbc:sqlserver://" + SERVER + ";"
            + "databaseName=" + DATABASE + ";"
            + "encrypt=true;"
            + "trustServerCertificate=true;";

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            // ✔ CORRECT WAY (credentials passed here ONLY)
            connection = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("[DB] Connected successfully to " + SERVER);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC Driver missing", e);
        } catch (SQLException e) {
            throw new RuntimeException("DB Connection failed: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null || instance.connection == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}