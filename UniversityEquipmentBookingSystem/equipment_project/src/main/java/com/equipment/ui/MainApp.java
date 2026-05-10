package com.equipment.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * MainApp - JavaFX Application Entry Point
 *
 * HOW TO CONNECT TO BACKEND:
 * 1. Copy this entire "ui" folder into your existing project under:
 *    src/main/java/com/equipment/ui/
 * 2. Copy fxml/ and css/ folders into:
 *    src/main/resources/com/equipment/ui/
 * 3. Add JavaFX dependencies to your pom.xml (see README)
 * 4. Run MainApp instead of Main.java
 *
 * No changes needed in Service or DAO classes.
 */
public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Operix — Equipment Booking & Fault Tracking");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);

        // Start at login screen
        navigateTo("login");
        stage.show();
    }

    /**
     * Central navigation method.
     * Controllers call MainApp.navigateTo("dashboard_teacher") etc.
     *
     * @param screen  one of: login | dashboard_teacher | dashboard_manager | dashboard_technician
     */
    public static void navigateTo(String screen) throws Exception {
        String fxml = "/com/equipment/ui/fxml/" + screen + ".fxml";
        Parent root = FXMLLoader.load(MainApp.class.getResource(fxml));
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1200, 760);
        } else {
            scene.setRoot(root);
        }
        primaryStage.setScene(scene);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
