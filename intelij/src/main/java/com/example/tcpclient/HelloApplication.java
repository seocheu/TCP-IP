package com.example.tcpclient;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 380);
        stage.setTitle("TCP");
        stage.setScene(scene);
        stage.show();

        HelloController helloController = fxmlLoader.getController();
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                helloController.key_down(keyEvent.getCode());
            }
        });
        scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                helloController.key_up(keyEvent.getCode());
            }
        });

        Slider red_slider = helloController.red_slider;
        red_slider.setOnMouseClicked(e -> {
            helloController.ctrl_led(red_slider, "RED");
        });

        Slider green_slider = helloController.green_slider;
        green_slider.setOnMouseClicked(e -> {
            helloController.ctrl_led(green_slider, "GREEN");
        });

        Slider blue_slider = helloController.blue_slider;
        blue_slider.setOnMouseClicked(e -> {
            helloController.ctrl_led(blue_slider, "BLUE");
        });
    }

    public static void main(String[] args) {
        launch();
    }
}