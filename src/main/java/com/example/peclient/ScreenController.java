package com.example.peclient;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.HashMap;

/*
* Set-up for screens allowing them to be loaded and set
* */
public class ScreenController extends StackPane {

    private final HashMap<String, Node> screens = new HashMap<>();

    public ScreenController() {
        super();
    }

    public void addScreen(String name, Node screen) {
        screens.put(name, screen);
    }

    public Node getScreen(String name) {
        return screens.get(name);
    }

    public void loadScreen(String name, String resource) {
        try {
            System.out.println(name + " " +  resource);
            FXMLLoader myLoader = new FXMLLoader(getClass().getResource(resource));
            System.out.println(myLoader);
            Parent loadScreen = myLoader.load();
            System.out.println(loadScreen);
            Screen myScreenController = myLoader.getController();
            System.out.println(myScreenController);
            myScreenController.setScreenParent(this);
            addScreen(name, loadScreen);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void setScreen(final String name) {
        try {
            if (screens.get(name) != null) {
                System.out.println(name);
                System.out.println("screen name: " + screens.get(name));
                System.out.println(screens.keySet());
                System.out.println(screens.values());
                final DoubleProperty opacity = opacityProperty();

                if (!getChildren().isEmpty()) {
                    Timeline fade = new Timeline(
                            new KeyFrame(Duration.ZERO, new KeyValue(opacity, 1.0)),
                            new KeyFrame(new Duration(1000), new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent t) {
                                    getChildren().remove(0);                    //remove the displayed screen
                                    getChildren().add(0, screens.get(name));     //add the screen
                                    Timeline fadeIn = new Timeline(
                                            new KeyFrame(Duration.ZERO, new KeyValue(opacity, 0.0)),
                                            new KeyFrame(new Duration(800), new KeyValue(opacity, 1.0)));
                                    fadeIn.play();
                                }
                            }, new KeyValue(opacity, 0.0)));
                    fade.play();

                } else {
                    setOpacity(0.0);
                    getChildren().add(screens.get(name));       //no one else been displayed, then just show
                    System.out.println(screens.size());
                    Timeline fadeIn = new Timeline(
                            new KeyFrame(Duration.ZERO, new KeyValue(opacity, 0.0)),
                            new KeyFrame(new Duration(1500), new KeyValue(opacity, 1.0)));
                    fadeIn.play();
                }
            } else {
                System.out.println("screen hasn't been loaded!!! \n");
            }
        }
        catch (Exception e){
            System.out.println(e);
        }

    }

    //This method will remove the screen with the given name from the collection of screens
    public boolean unloadScreen(String name) {
        if (screens.remove(name) == null) {
            System.out.println("Screen didn't exist");
            return false;
        } else {
            return true;
        }
    }


}
