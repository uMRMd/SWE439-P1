package View.Widgets;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


/**
 * A class that contains miscellaneous widgets that do not have many functions
 */
public class Misc {
    /**
     * Creates a JavaFx Pane that grows vertically to fill up remaining space in a window
     *
     * @return the Pane object that grows vertically
     */
    public static Pane getVerticalSpacer() {
        Pane spacer = new Pane();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        spacer.setMaxHeight(Double.MAX_VALUE);

        return spacer;
    }


    /**
     * Creates a JavaFx Pane that grows horizontally to fill up remaining space in a window
     *
     * @return the Pane object that grows vertically
     */
    public static Pane getHorizontalSpacer() {
        Pane spacer = new Pane();  // used as a spacer between buttons
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMaxWidth(Double.MAX_VALUE);

        return spacer;
    }


    public static Bounds calculateNodeSize(Node node) {
        Pane ghostPane = new Pane();
        Scene ghostScene = new Scene(ghostPane);  // a scene is needed to calculate preferred sizes of nodes

        ghostPane.getChildren().add(node);
        ghostPane.applyCss();
        ghostPane.layout();
        ghostPane.getChildren().clear();

        Bounds b = node.getBoundsInLocal();
        return b;
    }
}