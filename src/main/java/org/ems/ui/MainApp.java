package org.ems.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.ems.config.AppContext;
import org.ems.infrastructure.db.DatabaseInitializer;
import org.ems.ui.stage.SceneManager;

/**
 * @author <your group number>
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // INIT DB + APP CONTEXT
            DatabaseInitializer.initialize();
            AppContext.get();

            // INIT SCENEMANAGER
            SceneManager.init(primaryStage);

            // LOAD HOME UI
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/ems/ui/view/home.fxml")
            );

            Parent root = loader.load();

            Scene scene = new Scene(root, 1000, 700);

            primaryStage.setTitle("Event Manager System - Home");
            primaryStage.setScene(scene);
            primaryStage.show();

            System.out.println(" Home Screen Loaded Successfully!");

        } catch (Exception e) {
            System.err.println(" Error starting JavaFX with Home View: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace(System.err);
        }
    }

    static void main(String[] args) {
        launch(args);
    }
}
