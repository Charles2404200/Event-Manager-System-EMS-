package org;
/**
 * @author <your group number>
 */

import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        // Launch the JavaFX UI
        // Use reflection to avoid direct AWS SDK imports at startup
        try {
            Class<?> mainAppClass = Class.forName("org.ems.ui.MainApp");
            Application.launch((Class<? extends Application>) mainAppClass, args);
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load MainApp class: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
