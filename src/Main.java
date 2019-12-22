package projetprog;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(Main.class.getResource("views/Home.fxml"));
        primaryStage.setTitle("Projet");
        primaryStage.setScene(new Scene(root, 1280, 768));
        primaryStage.setMinWidth(768);
        primaryStage.setMinHeight(768);
        primaryStage.show();
    }
}
