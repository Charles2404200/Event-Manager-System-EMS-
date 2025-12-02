package org.ems.ui.stage;

/**
 * @author <your group number>
 */

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static void switchTo(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource("/org/ems/ui/view/" + fxml)
            );

            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);

        } catch (Exception e) {
            System.err.println(" Error loading FXML: " + fxml);
            e.printStackTrace(System.err);
        }
    }
}
