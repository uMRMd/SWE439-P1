package gui;

import DSMData.DSMItem;
import IOHandler.IOHandler;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ToolbarHandler {
    private VBox layout;

    private Button addMatrixItem;
    private Button deleteMatrixItem;
    private Button renameMatrixItem;
    private Button modifyConnections;
    private Button sort;

    private TabView editor;
    private IOHandler ioHandler;

    public ToolbarHandler(IOHandler ioHandler, TabView editor) {
        layout = new VBox();
        this.editor = editor;
        this.ioHandler = ioHandler;

        addMatrixItem = new Button("Add Row/Column");
        addMatrixItem.setOnAction(e -> {
            if(editor.getFocusedMatrixUid() == null) {
                return;
            }
            Stage window = new Stage();

            // Create Root window
            window.initModality(Modality.APPLICATION_MODAL); //Block events to other windows
            window.setTitle("Add Row/Column");

            // Create changes view and button for it
            Label label = new Label("Changes to be made");
            ListView< Pair<String, String> > changesToMakeView = new ListView<>();
            changesToMakeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            changesToMakeView.setCellFactory(param -> new ListCell< Pair<String, String> >() {  // row/column/symmetric | item name
                @Override
                protected void updateItem(Pair<String, String> item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null || item.getKey() == null) {
                        setText(null);
                    } else {
                        if(item.getKey().equals("symmetric")) {
                            setText(item.getValue() + " (Row/Column)");
                        } else if(item.getKey().equals("row")) {
                            setText(item.getValue() + " (Row)");
                        } else {
                            setText(item.getValue() + "(Col)");
                        }
                    }
                }
            });

            Button deleteSelected = new Button("Delete Selected Item(s)");
            deleteSelected.setOnAction(ee -> {
                changesToMakeView.getItems().removeAll(changesToMakeView.getSelectionModel().getSelectedItems());
            });

            // Create user input area
            HBox entryArea = new HBox();

            TextField textField = new TextField();
            textField.setMaxWidth(Double.MAX_VALUE);
            textField.setPromptText("Row/Column Name");
            HBox.setHgrow(textField, Priority.ALWAYS);

            if(ioHandler.getMatrix(editor.getFocusedMatrixUid()).isSymmetrical()) {
                Button addItem = new Button("Add Item");
                addItem.setOnAction(ee -> {
                    String itemName = textField.getText();
                    changesToMakeView.getItems().add(new Pair<String, String>("symmetric", itemName));
                });
                entryArea.getChildren().addAll(textField, addItem);
                entryArea.setPadding(new Insets(10, 10, 10, 10));
                entryArea.setSpacing(20);
            } else {
                Button addRow = new Button("Add as Row");
                addRow.setOnAction(ee -> {
                    String itemName = textField.getText();
                    changesToMakeView.getItems().add(new Pair<String, String>("row", itemName));
                });

                Button addColumn = new Button("Add as Column");
                addColumn.setOnAction(ee -> {
                    String itemName = textField.getText();
                    changesToMakeView.getItems().add(new Pair<String, String>("col", itemName));
                });

                entryArea.getChildren().addAll(textField, addRow, addColumn);
                entryArea.setPadding(new Insets(10, 10, 10, 10));
                entryArea.setSpacing(20);
            }

            // create HBox for user to close with our without changes
            HBox closeArea = new HBox();
            Button applyButton = new Button("Apply Changes");
            applyButton.setOnAction(ee -> {
                for(Pair<String, String> item : changesToMakeView.getItems()) {
                    if(item.getKey().equals("row")) {
                        ioHandler.getMatrix(editor.getFocusedMatrixUid()).addNewItem(item.getValue(), true);
                    } else if(item.getKey().equals("col")) {
                        ioHandler.getMatrix(editor.getFocusedMatrixUid()).addNewItem(item.getValue(), false);
                    } else {
                        ioHandler.getMatrix(editor.getFocusedMatrixUid()).addNewSymmetricItem(item.getValue());
                    }
                }
                window.close();
                editor.refreshTab();
            });

            Pane spacer = new Pane();  // used as a spacer between buttons
            HBox.setHgrow(spacer, Priority.ALWAYS);
            spacer.setMaxWidth(Double.MAX_VALUE);

            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(ee -> {
                window.close();
            });
            closeArea.getChildren().addAll(cancelButton, spacer, applyButton);

            VBox layout = new VBox(10);
            layout.getChildren().addAll(label, changesToMakeView, deleteSelected, entryArea, closeArea);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10, 10, 10, 10));
            layout.setSpacing(10);

            //Display window and wait for it to be closed before returning
            Scene scene = new Scene(layout, 500, 300);
            window.setScene(scene);
            window.showAndWait();
        });
        addMatrixItem.setMaxWidth(Double.MAX_VALUE);




        deleteMatrixItem = new Button("Delete Row/Column");
        deleteMatrixItem.setOnAction(e -> {
            System.out.println("Deleting row or column");
        });
        deleteMatrixItem.setMaxWidth(Double.MAX_VALUE);




        renameMatrixItem = new Button("Rename Row/Column");
        renameMatrixItem.setOnAction(e -> {
            System.out.println("Renaming row or column");
        });
        renameMatrixItem.setMaxWidth(Double.MAX_VALUE);




        modifyConnections = new Button("Modify Connections");
        modifyConnections.setOnAction(e -> {
            if(editor.getFocusedMatrixUid() == null) {
                return;
            }
            Stage window = new Stage();

            // Create Root window
            window.initModality(Modality.APPLICATION_MODAL); //Block events to other windows
            window.setTitle("Modify Connections");


            // Create changes view (does not have button to remove items from it
            Label label = new Label("Changes to be made");
            ListView< Vector<String> > changesToMakeView = new ListView<>();  // rowUid | colUid | name | weight
            changesToMakeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            changesToMakeView.setCellFactory(param -> new ListCell< Vector<String> >() {
                @Override
                protected void updateItem(Vector<String> item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(
                                ioHandler.getMatrix(editor.getFocusedMatrixUid()).getItem(Integer.parseInt(item.get(0))).getName() + " (Row):" +
                                ioHandler.getMatrix(editor.getFocusedMatrixUid()).getItem(Integer.parseInt(item.get(1))).getName() + " (Col)" +
                                "  {" + item.get(2) + ", " + item.get(3) + "}"
                        );
                    }
                }
            });
            Button deleteSelected = new Button("Delete Selected Item(s)");
            deleteSelected.setOnAction(ee -> {
                changesToMakeView.getItems().removeAll(changesToMakeView.getSelectionModel().getSelectedItems());
            });


            // area to interact with the connections
            HBox connectionsArea = new HBox();
            connectionsArea.setSpacing(10);
            connectionsArea.setPadding(new Insets(10, 10, 10, 10));

            // HBox area full of checklists to modify the connections, default to columns
            HBox connectionsModifier = new HBox();
            connectionsModifier.setSpacing(10);
            connectionsModifier.setPadding(new Insets(10, 10, 10, 10));
            HashMap<CheckBox, DSMItem> connections = new HashMap<>();

            for(DSMItem conn : ioHandler.getMatrix(editor.getFocusedMatrixUid()).getCols()) {
                VBox connectionVBox = new VBox();
                connectionVBox.setAlignment(Pos.CENTER);

                Label name = new Label(conn.getName());
                CheckBox box = new CheckBox();
                connections.put(box, conn);
                connectionVBox.getChildren().addAll(name, box);
                connectionsModifier.getChildren().add(connectionVBox);
            }
            ScrollPane scrollPane = new ScrollPane(connectionsModifier);
            scrollPane.setFitToHeight(true);

            // vbox to choose row or column
            VBox itemSelectorView = new VBox();
            itemSelectorView.setMinWidth(Region.USE_PREF_SIZE);

            // ComboBox to choose which row or column to modify connections of
            ComboBox< DSMItem > itemSelector = new ComboBox<>();  // rowUid | colUid | name | weight
            itemSelector.setCellFactory(param -> new ListCell< DSMItem >() {
                @Override
                protected void updateItem(DSMItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });

            itemSelector.getItems().addAll(ioHandler.getMatrix(editor.getFocusedMatrixUid()).getRows());  // default to choosing a row item

            Label l = new Label("Create connections by row or column?");
            l.setWrapText(true);
            l.prefWidthProperty().bind(itemSelector.widthProperty());  // this will make sure the label will not be bigger than the biggest object
            VBox.setVgrow(l, Priority.ALWAYS);
            HBox.setHgrow(l, Priority.ALWAYS);
            l.setMinHeight(Region.USE_PREF_SIZE);  // make sure all text will be displayed

            // radio buttons
            HBox rowColRadioButtons = new HBox();
            HBox.setHgrow(rowColRadioButtons, Priority.ALWAYS);
            rowColRadioButtons.setSpacing(10);
            rowColRadioButtons.setPadding(new Insets(10, 10, 10, 10));
            rowColRadioButtons.setMinHeight(Region.USE_PREF_SIZE);

            ToggleGroup tg = new ToggleGroup();
            RadioButton r1 = new RadioButton("Row");
            RadioButton r2 = new RadioButton("Column");
            HBox.setHgrow(r1, Priority.ALWAYS);
            HBox.setHgrow(r2, Priority.ALWAYS);
            r1.setMinHeight(Region.USE_PREF_SIZE);
            r2.setMinHeight(Region.USE_PREF_SIZE);

            r1.setToggleGroup(tg);  // add RadioButtons to toggle group
            r2.setToggleGroup(tg);
            r1.setSelected(true);  // default to r1

            // add a change listener
            tg.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                public void changed(ObservableValue<? extends Toggle> ob, Toggle o, Toggle n) {
                    RadioButton rb = (RadioButton)tg.getSelectedToggle();
                    if(rb.equals(r1)) {  // clear all items and add rows to it
                        itemSelector.getItems().removeAll(itemSelector.getItems());
                        itemSelector.getItems().addAll(ioHandler.getMatrix(editor.getFocusedMatrixUid()).getRows());

                        connectionsModifier.getChildren().removeAll(connectionsModifier.getChildren());
                        connections.clear();

                        for(DSMItem conn : ioHandler.getMatrix(editor.getFocusedMatrixUid()).getCols()) {
                            VBox connectionVBox = new VBox();
                            connectionVBox.setAlignment(Pos.CENTER);

                            Label name = new Label(conn.getName());
                            CheckBox box = new CheckBox();
                            connections.put(box, conn);
                            connectionVBox.getChildren().addAll(name, box);
                            connectionsModifier.getChildren().add(connectionVBox);
                        }
                    } else if(rb.equals(r2)) {  // clear all items and add cols to it
                        itemSelector.getItems().removeAll(itemSelector.getItems());
                        itemSelector.getItems().addAll(ioHandler.getMatrix(editor.getFocusedMatrixUid()).getCols());

                        connectionsModifier.getChildren().removeAll(connectionsModifier.getChildren());
                        connections.clear();

                        for(DSMItem conn : ioHandler.getMatrix(editor.getFocusedMatrixUid()).getRows()) {
                            VBox connectionVBox = new VBox();
                            connectionVBox.setAlignment(Pos.CENTER);

                            Label name = new Label(conn.getName());
                            CheckBox box = new CheckBox();
                            connections.put(box, conn);
                            connectionVBox.getChildren().addAll(name, box);
                            connectionsModifier.getChildren().add(connectionVBox);
                        }
                    } else {  // clear all items
                        System.out.println("here");
                        itemSelector.getItems().removeAll(itemSelector.getItems());

                        connectionsModifier.getChildren().removeAll(connectionsModifier.getChildren());
                        connections.clear();
                    }
                }
            });
            rowColRadioButtons.getChildren().addAll(r1, r2);
            itemSelectorView.getChildren().addAll(l, rowColRadioButtons, itemSelector);

            // add a spacer to ensure that the connections details are to the far side of the window
            Pane connectionsSpacer = new Pane();  // used as a spacer between buttons
            HBox.setHgrow(connectionsSpacer, Priority.ALWAYS);
            connectionsSpacer.setMaxWidth(Double.MAX_VALUE);

            // area to set details for the connection
            VBox connectionDetailsLayout = new VBox();
            connectionDetailsLayout.setSpacing(10);
            connectionDetailsLayout.setPadding(new Insets(10, 10, 10, 10));
            VBox.setVgrow(connectionDetailsLayout, Priority.ALWAYS);
//            connectionDetailsLayout.setMinWidth(Region.USE_PREF_SIZE);

            TextField connectionName = new TextField();
            TextField weight = new TextField();
            connectionName.setPromptText("Connection Name");
            weight.setPromptText("Connection Weight");

            connectionDetailsLayout.getChildren().addAll(connectionName, weight);


            connectionsArea.getChildren().addAll(itemSelectorView, scrollPane, connectionsSpacer, connectionDetailsLayout);

            // Pane to modify the connections
            HBox modifyPane = new HBox();
            modifyPane.setAlignment(Pos.CENTER);

            Button applyButton = new Button("Modify Connections");
            applyButton.setOnAction(ee -> {
                if(itemSelector.getValue() == null || connectionName.getText().isEmpty() || weight.getText().isEmpty()) {  // ensure connection can be added
                    // TODO: add popup window saying why it cannot make the changes
                    return;
                }
                for (Map.Entry<CheckBox, DSMItem> entry : connections.entrySet()) {
                    if(entry.getKey().isSelected()) {
                        // rowUid | colUid | name | weight
                        Vector<String> data = new Vector<String>();

                        if(((RadioButton)tg.getSelectedToggle()).equals(r1)) {  // selecting by row
                            data.add(Integer.toString(itemSelector.getValue().getUid()));  // row uid
                            data.add(Integer.toString(entry.getValue().getUid()));  // col uid
                        } else if(((RadioButton)tg.getSelectedToggle()).equals(r2)) {  // selecting by column
                            data.add(Integer.toString(entry.getValue().getUid()));  // row uid
                            data.add(Integer.toString(itemSelector.getValue().getUid()));  // col uid
                        }
                        data.add(connectionName.getText());
                        data.add(weight.getText());

                        if(!changesToMakeView.getItems().contains(data)) {  // ensure no duplicates
                            changesToMakeView.getItems().add(data);
                        }
                        System.out.println(data);
                    }
                }
            });

            Button applySymmetricButton = new Button("Modify Connections Symmetrically");
            applySymmetricButton.setOnAction(ee -> {
                if(itemSelector.getValue() == null || connectionName.getText().isEmpty() || weight.getText().isEmpty()) {  // ensure connection can be added
                    return;
                }
                for (Map.Entry<CheckBox, DSMItem> entry : connections.entrySet()) {
                    if(entry.getKey().isSelected()) {
                        // rowUid | colUid | name | weight
                        Vector<String> data1 = new Vector<String>();  // original connection
                        Vector<String> data2 = new Vector<String>();  // symmetric connection

                        if(((RadioButton)tg.getSelectedToggle()).equals(r1)) {  // selecting by row
                            data1.add(Integer.toString(itemSelector.getValue().getUid()));  // row uid
                            data1.add(Integer.toString(entry.getValue().getUid()));  // col uid

                            data2.add(Integer.toString(entry.getValue().getAliasUid()));  // row uid for symmetric connection
                            // iterate over columns and find the one that corresponds to the selected row
                            for(DSMItem item : ioHandler.getMatrix(editor.getFocusedMatrixUid()).getCols()) {
                                if(item.getAliasUid() == itemSelector.getValue().getUid()) {
                                    data2.add(Integer.toString(item.getUid()));
                                }
                            }
                        } else if(((RadioButton)tg.getSelectedToggle()).equals(r2)) {  // selecting by column
                            data1.add(Integer.toString(entry.getValue().getUid()));  // row uid
                            data1.add(Integer.toString(itemSelector.getValue().getUid()));  // col uid

                            data2.add(Integer.toString(itemSelector.getValue().getAliasUid()));  // row uid for symmetric connection
                            for(DSMItem item : ioHandler.getMatrix(editor.getFocusedMatrixUid()).getCols()) {
                                if(item.getAliasUid() == entry.getValue().getUid()) {
                                    data2.add(Integer.toString(item.getUid()));
                                }
                            }

                            // iterate over columns to find the column that corresponds to the row
                            for(DSMItem item : ioHandler.getMatrix(editor.getFocusedMatrixUid()).getCols()) {
                                if(item.getAliasUid() == itemSelector.getValue().getUid()) {
                                    data2.add(Integer.toString(item.getUid()));
                                }
                            }
                        }
                        data1.add(connectionName.getText());
                        data1.add(weight.getText());
                        data2.add(connectionName.getText());
                        data2.add(weight.getText());

                        if(!changesToMakeView.getItems().contains(data1)) {  // ensure no duplicates
                            changesToMakeView.getItems().add(data1);
                        }
                        if(!changesToMakeView.getItems().contains(data2)) {  // ensure no duplicates
                            changesToMakeView.getItems().add(data2);
                        }
                    }
                }
            });
            if(!ioHandler.getMatrix(editor.getFocusedMatrixUid()).isSymmetrical()) {  // hide button if not a symmetric matrix
                applySymmetricButton.setManaged(false);
                applySymmetricButton.setVisible(false);
            }
            modifyPane.getChildren().addAll(applyButton, applySymmetricButton);


            // create HBox for user to close with our without changes
            HBox closeArea = new HBox();
            Button applyAllButton = new Button("Apply All Changes");
            applyAllButton.setOnAction(ee -> {
                for(Vector<String> item : changesToMakeView.getItems()) {  // rowUid | colUid | name | weight
                    ioHandler.getMatrix(editor.getFocusedMatrixUid()).modifyConnection(Integer.parseInt(item.get(0)), Integer.parseInt(item.get(1)), item.get(2), Double.parseDouble(item.get(3)));
                }
                window.close();
                editor.refreshTab();
            });

            Pane spacer = new Pane();  // used as a spacer between buttons
            HBox.setHgrow(spacer, Priority.ALWAYS);
            spacer.setMaxWidth(Double.MAX_VALUE);

            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(ee -> {
                window.close();
            });
            closeArea.getChildren().addAll(cancelButton, spacer, applyAllButton);


            VBox layout = new VBox(10);
            layout.getChildren().addAll(label, changesToMakeView, deleteSelected, connectionsArea, modifyPane, closeArea);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10, 10, 10, 10));
            layout.setSpacing(10);


            //Display window and wait for it to be closed before returning
            Scene scene = new Scene(layout, 700, 300);
            window.setScene(scene);
            window.showAndWait();
        });
        modifyConnections.setMaxWidth(Double.MAX_VALUE);




        sort = new Button("Sort");
        sort.setOnAction(e -> {
            editor.refreshTab();
        });
        sort.setMaxWidth(Double.MAX_VALUE);




        layout.getChildren().addAll(addMatrixItem, deleteMatrixItem, renameMatrixItem, modifyConnections, sort);
        layout.setPadding(new Insets(10, 10, 10, 10));
        layout.setSpacing(20);
        layout.setAlignment(Pos.CENTER);
    }

    public VBox getLayout() {
        return layout;
    }
}
