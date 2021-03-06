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

/**
 * Object to store multiple screens for PreLoaded screen switching
 * Allowing multiple screens to be displayed, removed, or added
 */
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

    /**
     * Loads screen into ScreenList through the addScreen method
     * @param name - Sets Key-name for screen in Screens
     * @param resource - associated resource FXML file
     */
    public void loadScreen(String name, String resource) {
        try {
            FXMLLoader myLoader = new FXMLLoader(getClass().getResource(resource));
            Parent loadScreen = myLoader.load();
            Screen myScreenController = myLoader.getController();
            myScreenController.setScreenParent(this);
            addScreen(name, loadScreen);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets screen from Screens as primary allowing it to take over presentation for client view
     * @param name - Key-name for screen to be displayed from Screens
     */
    public void setScreen(final String name) {
        try {
            if (screens.get(name) != null) {
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
            e.printStackTrace();
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
