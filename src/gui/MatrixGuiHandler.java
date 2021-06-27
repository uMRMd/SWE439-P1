package gui;

import DSMData.DSMConnection;
import DSMData.DSMItem;
import DSMData.DataHandler;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VerticalDirection;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class MatrixGuiHandler {
    DataHandler matrix;
    private final Background DEFAULT_BACKGROUND = new Background(new BackgroundFill(Color.color(1, 1, 1), new CornerRadii(3), new Insets(0)));
    private final Background UNEDITABLE_CONNECTION_BACKGROUND = new Background(new BackgroundFill(Color.color(0, 0, 0), new CornerRadii(3), new Insets(0)));
    private final Background HIGHLIGHT_BACKGROUND = new Background(new BackgroundFill(Color.color(.9, 1, 0), new CornerRadii(3), new Insets(0)));
    private final Background CROSS_HIGHLIGHT_BACKGROUND = new Background(new BackgroundFill(Color.color(.2, 1, 0), new CornerRadii(3), new Insets(0)));
    private final Background ERROR_BACKGROUND = new Background(new BackgroundFill(Color.color(1, 0, 0), new CornerRadii(3), new Insets(0)));

    private DoubleProperty fontSize;

    private VBox rootLayout = new VBox();


    private class Cell {
        private Pair<Integer, Integer> gridLocation;
        private HBox guiCell;

        private static Boolean crossHighlightEnabled = false;

        private Background defaultBG = null;
        private Background userHighlightBG = null;
        private Background crossHighlightBG = null;
        private Background errorHighlightBG = null;

        public Cell(Pair<Integer, Integer> gridLocation, HBox guiCell) {
            this.gridLocation = gridLocation;
            this.guiCell = guiCell;
        }

        private void setCellHighlight(Color color) {
            guiCell.setBackground(new Background(new BackgroundFill(color, new CornerRadii(3), new Insets(0))));
        }

        public static Boolean getCrossHighlightEnabled() {
            return crossHighlightEnabled;
        }

        public static void setCrossHighlightEnabled(Boolean crossHighlightEnabled) {
            Cell.crossHighlightEnabled = crossHighlightEnabled;
        }

        public void updateCellHighlight() {
            if (getErrorHighlightBG() != null) {
                guiCell.setBackground(getErrorHighlightBG());
            } else if (getCrossHighlightBG() != null && crossHighlightEnabled) {
                guiCell.setBackground(getCrossHighlightBG());
            } else if (getUserHighlightBG() != null) {
                guiCell.setBackground(getUserHighlightBG());
            } else {  // default background determined by groupings
                Integer rowUid = getUidsFromGridLoc(gridLocation).getKey();
                Integer colUid = getUidsFromGridLoc(gridLocation).getValue();
                Color mergedColor = null;
                if (rowUid == null && colUid != null) {  // highlight with column color
                    mergedColor = matrix.getGroupingColors().get(matrix.getItem(colUid).getGroup());
                    setCellHighlight(mergedColor);
                    return;
                } else if (rowUid != null && colUid == null) {  // highlight with row color
                    mergedColor = matrix.getGroupingColors().get(matrix.getItem(rowUid).getGroup());
                    setCellHighlight(mergedColor);
                    return;
                } else if (rowUid != null && colUid != null) {  // highlight with merged color
                    Color rowColor = matrix.getGroupingColors().get(matrix.getItem(rowUid).getGroup());
                    if (rowColor == null) rowColor = Color.color(1.0, 1.0, 1.0);

                    Color colColor = matrix.getGroupingColors().get(matrix.getItem(colUid).getGroup());
                    if (colColor == null) colColor = Color.color(1.0, 1.0, 1.0);

                    double r = (rowColor.getRed() + colColor.getRed()) / 2;
                    double g = (rowColor.getGreen() + colColor.getGreen()) / 2;
                    double b = (rowColor.getBlue() + colColor.getBlue()) / 2;
                    mergedColor = Color.color(r, g, b);

                    if (matrix.isSymmetrical() && !rowUid.equals(matrix.getItem(colUid).getAliasUid()) && matrix.getItem(rowUid).getGroup().equals(matrix.getItem(colUid).getGroup())) {  // associated row and column are same group
                        setCellHighlight(mergedColor);
                        return;
                    } else if (!matrix.isSymmetrical()) {
                        setCellHighlight(mergedColor);
                        return;
                    }
                }

                setCellHighlight((Color)getDefaultBG().getFills().get(0).getFill());
            }
        }

        public Pair<Integer, Integer> getGridLocation() {
            return gridLocation;
        }

        public HBox getGuiCell() {
            return guiCell;
        }

        public Background getDefaultBG() {
            return defaultBG;
        }

        public void setDefaultBG(Background defaultBG, boolean update) {
            this.defaultBG = defaultBG;
            updateCellHighlight();

        }

        public Background getUserHighlightBG() {
            return userHighlightBG;
        }

        public void setUserHighlightBG(Background userHighlightBG) {
            this.userHighlightBG = userHighlightBG;
            updateCellHighlight();
        }

        public Background getCrossHighlightBG() {
            return crossHighlightBG;
        }

        public void setCrossHighlightBG(Background crossHighlightBG) {
            this.crossHighlightBG = crossHighlightBG;
            updateCellHighlight();
        }

        public Background getErrorHighlightBG() {
            return errorHighlightBG;
        }

        public void setErrorHighlightBG(Background errorHighlightBG) {
            this.errorHighlightBG = errorHighlightBG;
            updateCellHighlight();
        }


    }


    Vector<Cell> cells;  // contains information for highlighting
    HashMap<String, HashMap<Integer, Integer>> gridUidLookup;

    MatrixGuiHandler(DataHandler matrix, double fontSize) {
        this.matrix = matrix;
        cells = new Vector<>();
        gridUidLookup = new HashMap<>();
        gridUidLookup.put("rows", new HashMap<Integer, Integer>());
        gridUidLookup.put("cols", new HashMap<Integer, Integer>());

        this.fontSize = new SimpleDoubleProperty(fontSize);
    }

    private Cell getCellByLoc(Pair<Integer, Integer> cellLoc) {
        for(Cell cell : cells) {  // determine the value to decrease to
            if(cell.getGridLocation().getKey() == cellLoc.getKey() && cell.getGridLocation().getValue() == cellLoc.getValue()) {
                return cell;
            }
        }
        return null;
    }

    private Pair<Integer, Integer> getUidsFromGridLoc(Pair<Integer, Integer> cellLoc) {
        Integer rowUid = gridUidLookup.get("rows").get(cellLoc.getKey());;
        Integer colUid = gridUidLookup.get("cols").get(cellLoc.getValue());;
        return new Pair<>(rowUid, colUid);
    }

    private void toggleUserHighlightCell(Pair<Integer, Integer> cellLoc, Background bg) {
        Cell cell = getCellByLoc(cellLoc);
        if(cell.getUserHighlightBG() == null) {  // is highlighted, so unhighlight it
            cell.setUserHighlightBG(bg);
        } else {
            cell.setUserHighlightBG(null);
        }
    }


    private void setCellHighlight(Pair<Integer, Integer> cellLoc, Background bg, String highlightType) {
        Cell cell = getCellByLoc(cellLoc);
        HashMap<String, Runnable> functions = new HashMap<>();
        functions.put("userHighlight", () -> cell.setUserHighlightBG(bg));
        functions.put("errorHighlight", () -> cell.setErrorHighlightBG(bg));
        functions.get(highlightType).run();
    }


    private void clearCellHighlight(Pair<Integer, Integer> cellLoc, String highlightType) {
        Cell cell = getCellByLoc(cellLoc);
        HashMap<String, Runnable> functions = new HashMap<>();
        functions.put("userHighlight", () -> cell.setUserHighlightBG(null));
        functions.put("errorHighlight", () -> cell.setErrorHighlightBG(null));
        functions.get(highlightType).run();
    }


    private void crossHighlightCell(Pair<Integer, Integer> endLocation, boolean shouldHighlight) {
        int endRow = endLocation.getKey();
        int endCol = endLocation.getValue();

        int minRow = Integer.MAX_VALUE;
        int minCol = Integer.MAX_VALUE;
        for(Cell cell : cells) {  // determine the value to decrease to
            if(cell.getGridLocation().getKey() < minRow) {
                minRow = cell.getGridLocation().getKey();
            }
            if(cell.getGridLocation().getValue() < minCol) {
                minCol = cell.getGridLocation().getValue();
            }
        }

        for(int i=endRow; i>=minRow; i--) {  // highlight vertically
            for(Cell cell : cells) {  // find the cell to modify
                if(cell.getGridLocation().getKey() == i && cell.getGridLocation().getValue() == endCol) {
                    if(shouldHighlight) {
                        cell.setCrossHighlightBG(CROSS_HIGHLIGHT_BACKGROUND);
                    } else {
                        cell.setCrossHighlightBG(null);
                    }
                    break;
                }
            }
        }

        for(int i=endCol - 1; i>=minCol; i--) {  // highlight horizontally, start at one less because first cell it will find is already highlighted
            for(Cell cell : cells) {  // find the cell to modify
                if(cell.getGridLocation().getValue() == i && cell.getGridLocation().getKey() == endRow) {
                    if(shouldHighlight) {
                        cell.setCrossHighlightBG(CROSS_HIGHLIGHT_BACKGROUND);
                    } else {
                        cell.setCrossHighlightBG(null);
                    }
                    break;
                }
            }
        }

    }

    public void toggleCrossHighlighting() {
        Cell.setCrossHighlightEnabled(!Cell.getCrossHighlightEnabled());
        for(Cell cell : cells) {
            cell.updateCellHighlight();
        }
    }

     public VBox getMatrixEditor() {
        cells = new Vector<>();
        gridUidLookup = new HashMap<>();
        gridUidLookup.put("rows", new HashMap<Integer, Integer>());
        gridUidLookup.put("cols", new HashMap<Integer, Integer>());

        rootLayout = new VBox();
        rootLayout.setAlignment(Pos.CENTER);
        rootLayout.styleProperty().bind(Bindings.concat(
                "-fx-font-size: ", fontSize.asString(), "};",
                ".combo-box > .list-cell {-fx-padding: 0 0 0 0; -fx-border-insets: 0 0 0 0;}"
        ));

        Label location = new Label("");
        GridPane grid = new GridPane();

        grid.setAlignment(Pos.CENTER);
        ArrayList<ArrayList<Pair<String, Object>>> template = matrix.getGridArray();
        int rows = template.size();
        int columns = template.get(0).size();

        for(int r=0; r<rows; r++) {
            for(int c=0; c<columns; c++) {
                Pair<String, Object> item = template.get(r).get(c);
                HBox cell = new HBox();  // wrap everything in an HBox so a border can be added easily

                Background defaultBackground = DEFAULT_BACKGROUND;

                if(item.getKey().equals("plain_text")) {
                    Label label = new Label((String)item.getValue());
                    label.setMinWidth(Region.USE_PREF_SIZE);
                    cell.getChildren().add((Node) label);
                } else if(item.getKey().equals("plain_text_v")) {
                    Label label = new Label((String)item.getValue());
                    label.setRotate(-90);
                    cell.setAlignment(Pos.BOTTOM_RIGHT);
                    Group g = new Group();  // label will be added to a group so that it will be formatted correctly if it is vertical
                    g.getChildren().add(label);
                    cell.getChildren().add(g);
                } else if(item.getKey().equals("item_name")) {
                    Label label = new Label(matrix.getItem((Integer) item.getValue()).getName());
                    cell.setAlignment(Pos.BOTTOM_RIGHT);
                    label.setMinWidth(Region.USE_PREF_SIZE);
                    cell.getChildren().add(label);
                } else if(item.getKey().equals("item_name_v")) {
                    Label label = new Label(matrix.getItem((Integer)item.getValue()).getName());
                    label.setRotate(-90);
                    cell.setAlignment(Pos.BOTTOM_RIGHT);
                    Group g = new Group();  // label will be added to a group so that it will be formatted correctly if it is vertical
                    g.getChildren().add(label);
                    cell.getChildren().add(g);
                } else if(item.getKey().equals("grouping_item")) {
                    ComboBox<String> groupings = new ComboBox<String>();
                    groupings.setMinWidth(Region.USE_PREF_SIZE);

                    groupings.setStyle("""
                            -fx-border-insets: -2, -2, -2, -2;
                            -fx-padding: -5, -5, -5, -5;"""
                    );

                    groupings.getItems().addAll(matrix.getGroupings());
                    groupings.getSelectionModel().select(matrix.getItem((Integer)item.getValue()).getGroup());
                    groupings.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal)->{
                        if(matrix.isSymmetrical()) {
                            matrix.setGroupSymmetric((Integer)item.getValue(), groupings.getValue());
                        } else {
                            matrix.setGroup((Integer)item.getValue(), groupings.getValue());
                        }

                        for(Cell c_ : cells) {
                            c_.updateCellHighlight();
                        }
                    });
                    cell.getChildren().add(groupings);
                } else if(item.getKey().equals("grouping_item_v")) {
                    ComboBox<String> groupings = new ComboBox<String>();
                    groupings.getItems().addAll(matrix.getGroupings());
                    groupings.setStyle(  // remove border from button when selecting it because this causes weird resizing bugs in the grouping
                            """
                            -fx-focus-color: transparent;
                            -fx-background-insets: 0, 0, 0;
                            -fx-background-radius: 0, 0, 0;"""
                    );
                    groupings.setRotate(-90);
                    groupings.getSelectionModel().select(matrix.getItem((Integer)item.getValue()).getGroup());
                    groupings.setOnAction(e -> {
                        matrix.setGroup((Integer)item.getValue(), groupings.getValue());
                        for(Cell c_ : cells) {
                            c_.updateCellHighlight();
                        }
                    });
                    Group g = new Group();  // box will be added to a group so that it will be formatted correctly if it is vertical
                    g.getChildren().add(groupings);
                    cell.getChildren().add(g);
                } else if(item.getKey().equals("index_item")) {
                    TextField entry = new TextField(((Double)matrix.getItem((Integer)item.getValue()).getSortIndex()).toString());
                    entry.setPrefColumnCount(3);  // set size to 3 characters fitting
                    entry.setPadding(new Insets(0));

                    // force the field to be numeric only TODO: this stopped working on 6/20
                    entry.textProperty().addListener(new ChangeListener<String>() {
                        @Override
                        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                            if (!newValue.matches("\\d*+\\.")) {
                                entry.setText(newValue.replaceAll("[^\\d]+[.]", ""));
                            }
                        }
                    });
                    cell.setMaxWidth(Region.USE_COMPUTED_SIZE);
                    cell.setAlignment(Pos.CENTER);

                    int finalR = r;
                    int finalC = c;
                    entry.setOnAction(e -> {
                        cell.getParent().requestFocus();
                    });
                    entry.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if(!newVal) {  // if changing to not focused
                            try {
                                Double newSortIndex = Double.parseDouble(entry.getText());
                                if(matrix.isSymmetrical()) {
                                    matrix.setSortIndexSymmetric((Integer)item.getValue(), newSortIndex);
                                } else {
                                    matrix.setSortIndex((Integer)item.getValue(), newSortIndex);
                                }
                                clearCellHighlight(new Pair<Integer, Integer>(finalR, finalC), "errorHighlight");
                            } catch(NumberFormatException ee) {
                                setCellHighlight(new Pair<Integer, Integer>(finalR, finalC), ERROR_BACKGROUND, "errorHighlight");
                            }
                        }
                    });

                    cell.getChildren().add(entry);
                } else if(item.getKey().equals("uneditable_connection")) {
                    HBox label = new HBox();  // use an HBox object because then background color is not tied to the text
                    defaultBackground = UNEDITABLE_CONNECTION_BACKGROUND;
                } else if(item.getKey().equals("editable_connection")) {
                    int rowUid = ((Pair<Integer, Integer>)item.getValue()).getKey();
                    int colUid = ((Pair<Integer, Integer>)item.getValue()).getValue();
                    DSMConnection conn = matrix.getConnection(rowUid, colUid);
                    final Label label;
                    if(conn == null) {
                        label = new Label("");
                    } else {
                        label = new Label(conn.getConnectionName());
                    }
                    cell.setAlignment(Pos.CENTER);  // center the text

                    // this item type will be used to create the lookup table for finding associated uid from grid location
                    if(!gridUidLookup.get("rows").containsKey(r)) {
                        gridUidLookup.get("rows").put(r, rowUid);
                    }

                    if(!gridUidLookup.get("cols").containsKey(c)) {
                        gridUidLookup.get("cols").put(c, colUid);
                    }

                    // set up callback functions
                    int finalR = r;
                    int finalC = c;
                    cell.setOnMouseClicked(e -> {
                        if(e.getButton().equals(MouseButton.PRIMARY)) {

                            // create popup window to edit the connection
                            Stage window = new Stage();

                            // Create Root window
                            window.initModality(Modality.APPLICATION_MODAL); //Block events to other windows
                            window.setTitle("Modify Connection");

                            VBox layout = new VBox();

                            // row 0
                            Label titleLabel = new Label("Connection From " + matrix.getItem(rowUid).getName() + " to " + matrix.getItem(colUid).getName());
                            GridPane.setConstraints(titleLabel, 0, 0, 3, 1);  // span 3 columns

                            // row 1
                            HBox row1 = new HBox();
                            row1.setPadding(new Insets(10, 10, 10, 10));
                            row1.setSpacing(10);
                            Label nameLabel = new Label("Connection Type:  ");

                            String currentName = null;
                            if(matrix.getConnection(rowUid, colUid) != null) {
                                currentName = matrix.getConnection(rowUid, colUid).getConnectionName();
                            } else {
                                currentName = "";
                            }
                            TextField nameField = new TextField(currentName);
                            nameField.setMaxWidth(Double.MAX_VALUE);
                            HBox.setHgrow(nameField, Priority.ALWAYS);
                            row1.getChildren().addAll(nameLabel, nameField);

                            // row 2
                            HBox row2 = new HBox();
                            Label weightLabel = new Label("Connection Weight:");
                            row2.setPadding(new Insets(10, 10, 10, 10));
                            row2.setSpacing(10);

                            String currentWeight = null;
                            if(matrix.getConnection(rowUid, colUid) != null) {
                                currentWeight = ((Double)matrix.getConnection(rowUid, colUid).getWeight()).toString();
                            } else {
                                currentWeight = "1.0";
                            }
                            TextField weightField = new TextField(currentWeight);
                            weightField.setMaxWidth(Double.MAX_VALUE);
                            HBox.setHgrow(weightField, Priority.ALWAYS);
                            // force the field to be numeric only
                            weightField.textProperty().addListener(new ChangeListener<String>() {
                                @Override
                                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                                    if (!newValue.matches("\\d*+\\.")) {
                                        weightField.setText(newValue.replaceAll("[^\\d]+[.]", ""));
                                    }
                                }
                            });
                            row2.getChildren().addAll(weightLabel, weightField);

                            // row 3
                            // create HBox for user to close with our without changes
                            HBox closeArea = new HBox();
                            Button applyButton = new Button("Apply Changes");
                            applyButton.setOnAction(ee -> {
                                if(!nameField.getText().equals("")) {
                                    Double weight = null;
                                    try {
                                        weight = Double.parseDouble(weightField.getText());
                                    } catch(NumberFormatException nfe) {
                                        weight = 1.0;
                                    }
                                    matrix.modifyConnection(rowUid, colUid, nameField.getText(), weight);
                                } else {
                                    matrix.clearConnection(rowUid, colUid);
                                }
                                window.close();
                                label.setText(nameField.getText());
                            });

                            Pane spacer = new Pane();  // used as a spacer between buttons
                            HBox.setHgrow(spacer, Priority.ALWAYS);
                            spacer.setMaxWidth(Double.MAX_VALUE);

                            Button cancelButton = new Button("Cancel");
                            cancelButton.setOnAction(ee -> {
                                window.close();
                            });
                            closeArea.getChildren().addAll(cancelButton, spacer, applyButton);

                            //Display window and wait for it to be closed before returning
                            layout.getChildren().addAll(titleLabel, row1, row2, closeArea);
                            layout.setAlignment(Pos.CENTER);
                            layout.setPadding(new Insets(10, 10, 10, 10));
                            layout.setSpacing(10);

                            Scene scene = new Scene(layout, 400, 200);
                            window.setScene(scene);
                            window.showAndWait();

                        } else if(e.getButton().equals(MouseButton.SECONDARY)) {  // toggle highlighting
                            toggleUserHighlightCell(new Pair<Integer, Integer>(finalR, finalC), HIGHLIGHT_BACKGROUND);
                        }
                    });
                    cell.setOnMouseEntered(e -> {
                        crossHighlightCell(new Pair<Integer, Integer>(finalR, finalC), true);
                        location.setText(matrix.getItem(rowUid).getName() + ":" + matrix.getItem(colUid).getName());
                    });
                    cell.setOnMouseExited(e -> {
                        crossHighlightCell(new Pair<Integer, Integer>(finalR, finalC), false);
                        location.setText("");
                    });

                    cell.getChildren().add(label);
                }
                cell.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
                cell.setPadding(new Insets(0));
                GridPane.setConstraints(cell, c, r);
                grid.getChildren().add(cell);

                Cell cellData = new Cell(new Pair<>(r, c), cell);
                cellData.setDefaultBG(defaultBackground, false);
                cells.add(cellData);

            }
        }
        for(Cell cell : cells) {
            cell.updateCellHighlight();
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        rootLayout.getChildren().addAll(scrollPane, location);


        return rootLayout;
    }

    public void setFontSize(Double newSize) {
        fontSize.setValue(newSize);
    }
}
