package Matrices.SideBarTools;

import Matrices.Data.AbstractDSMData;
import Matrices.Data.Entities.DSMConnection;
import Matrices.Data.Entities.DSMItem;
import Matrices.Data.Entities.Grouping;
import Matrices.Views.AbstractMatrixView;
import UI.Widgets.Misc;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;


/**
 * Generic class for creating a sidebar to interact with matrices
 */
public abstract class AbstractSideBar {
    protected VBox layout;

    protected final Button addMatrixItems = new Button();
    protected final Button deleteMatrixItems = new Button();
    protected final Button appendConnections = new Button("Append Connections");
    protected final Button setConnections = new Button("Set Connections");
    protected final Button deleteConnections = new Button("Delete Connections");
    protected final Button sort = new Button("Sort");
    protected final Button reDistributeIndices = new Button("Re-Distribute Indices");

    protected AbstractMatrixView matrixView;
    protected AbstractDSMData matrix;


//region Cell Factories
    /**
     * Cell factory for displaying DSM connections in a listview. Takes a DSMConnection and Prepends DELETE
     * if the connection name is empty and the weight is set to Double.MAX_VALUE
     */
    protected final Callback<ListView<DSMConnection>, ListCell<DSMConnection>> CONNECTION_CELL_FACTORY = new Callback<>() {
        @Override
        public ListCell<DSMConnection> call(ListView<DSMConnection> l) {
            return new ListCell<>() {
                @Override
                protected void updateItem(DSMConnection connection, boolean empty) {
                    super.updateItem(connection, empty);

                    if (empty || connection == null) {
                        setText(null);
                    } else if (!connection.getConnectionName().isEmpty() && connection.getWeight() != Double.MAX_VALUE) {
                        setText(
                            matrix.getItem(connection.getRowUid()).getName().getValue() + " (Row):" +
                            matrix.getItem(connection.getColUid()).getName().getValue() + " (Col)" +
                            "  {" + connection.getConnectionName() + ", " + connection.getWeight() + "}"
                        );
                    } else {
                        setText(
                            "DELETE " +
                            matrix.getItem(connection.getRowUid()).getName().getValue() + " (Row):" +
                            matrix.getItem(connection.getColUid()).getName().getValue() + " (Col)"
                        );
                    }
                }
            };
        }
    };


    /**
     * Cell factory for a deleting connections listview. Takes a DSMConnection and always prepends DELETE to it
     */
    protected final Callback<ListView<DSMConnection>, ListCell<DSMConnection>> DELETE_CONNECTION_CELL_FACTORY = new Callback<>() {
        @Override
        public ListCell<DSMConnection> call(ListView<DSMConnection> l) {
            return new ListCell<>() {
                @Override
                protected void updateItem(DSMConnection conn, boolean empty) {
                    super.updateItem(conn, empty);

                    if (empty || conn == null) {
                        setText(null);
                    } else {
                        setText(
                            "DELETE " +
                            matrix.getItem(conn.getRowUid()).getName().getValue() + ":" +
                            matrix.getItem(conn.getColUid()).getName().getValue()
                        );
                    }
                }
            };
        }
    };


    /**
     * Cell factory for a matrix item in a listview. Takes an integer that represents a uid in the matrix. Appends
     * row or column to it based on if the item is in the matrix as a row or a column. If the uid is set to
     * Integer.MAX_VALUE then the string 'All' is displayed
     */
    protected final Callback<ListView<Integer>, ListCell<Integer>> MATRIX_ITEM_INTEGER_COMBOBOX_CELL_FACTORY = new Callback<>() {
        @Override
        public ListCell<Integer> call(ListView<Integer> l) {
            return new ListCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setGraphic(null);
                    } else {
                        if(item == Integer.MAX_VALUE) {
                            setText("All");
                        } else if(matrix.isRow(matrix.getItem(item).getUid())) {
                            setText(matrix.getItem(item).getName().getValue() + " (Row)");
                        } else {
                            setText(matrix.getItem(item).getName().getValue() + " (Column)");
                        }
                    }
                }
            };
        }
    };


    /**
     * Cell factory for a matrix item in a combobox. Takes a DSMItem and displays the name of it
     */
    protected final Callback<ListView<DSMItem>, ListCell<DSMItem>> MATRIX_ITEM_COMBOBOX_CELL_FACTORY = new Callback<>() {
        @Override
        public ListCell<DSMItem> call(ListView<DSMItem> l) {
            return new ListCell<>() {
                @Override
                protected void updateItem(DSMItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName().getValue());
                    }
                }
            };
        }
    };
//endregion


    /**
     * Creates a new Sidebar object. Sets up the gui and all its widgets and puts them in the layout field.
     * Requires a MatrixView object to get the matrix and a EditorPane object to get the current focused tab
     * and call updates to it.
     *
     * @param matrix      the matrix data object instance
     * @param matrixView  the matrix view instance for the matrix
     */
    AbstractSideBar(AbstractDSMData matrix, AbstractMatrixView matrixView) {
        layout = new VBox();
        this.matrixView = matrixView;
        this.matrix = matrix;

        addMatrixItems.setOnAction(e -> addMatrixItemsCallback());
        addMatrixItems.setMaxWidth(Double.MAX_VALUE);

        deleteMatrixItems.setOnAction(e -> deleteMatrixItemsCallback());
        deleteMatrixItems.setMaxWidth(Double.MAX_VALUE);

        appendConnections.setOnAction(e -> appendConnectionsCallback());
        appendConnections.setMaxWidth(Double.MAX_VALUE);

        setConnections.setOnAction(e -> setConnectionsCallback());
        setConnections.setMaxWidth(Double.MAX_VALUE);

        deleteConnections.setOnAction(e -> deleteConnectionsCallback());
        deleteConnections.setMaxWidth(Double.MAX_VALUE);

        sort.setOnAction(e -> sortCallback());
        sort.setMaxWidth(Double.MAX_VALUE);

        reDistributeIndices.setOnAction(e -> reDistributeIndicesCallback());
        reDistributeIndices.setMaxWidth(Double.MAX_VALUE);
    }


    /**
     * Disables the sidebar buttons
     */
    public abstract void setDisabled();


    /**
     * Enables the sidebar buttons
     */
    public abstract void setEnabled();


    /**
     * Sets up the button callback for adding items to the matrix
     */
    protected abstract void addMatrixItemsCallback();


    /**
     * Sets up the button callback for deleting items from the matrix
     */
    protected abstract void deleteMatrixItemsCallback();


    /**
     * Sets up the button callback for appending connections to the matrix
     */
    protected abstract void appendConnectionsCallback();


    /**
     * Sets up the button callback for setting connections in the matrix
     */
    protected abstract void setConnectionsCallback();


    /**
     * Sets up the button callback for deleting connections in the matrix
     */
    protected abstract void deleteConnectionsCallback();


    /**
     * Sets up the button callback for sorting the matrix
     */
    protected void sortCallback() {
        matrixView.refreshView();
    }


    /**
     * Sets up the button callback for re-distributing sort indices
     */
    protected void reDistributeIndicesCallback() {
        matrix.reDistributeSortIndices();
        matrixView.refreshView();
        matrix.setCurrentStateAsCheckpoint();
    }


    /**
     * Returns the VBox of the layout so that it can be added to a scene
     *
     * @return the VBox layout of the toolbar
     */
    public VBox getLayout() {
        return layout;
    }
}
