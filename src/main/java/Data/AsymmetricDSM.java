package Data;

import javafx.collections.FXCollections;
import javafx.util.Pair;

import java.util.*;


/**
 * A class that contains data about a matrix. All operations to a matrix come through
 * this class. Handles both symmetrical and non-symmetrical matrices.
 * Note: items in a symmetric dsm will use the property Grouping.group1 to configure groups
 *
 * @author: Aiden Carney
 */
public class AsymmetricDSM extends TemplateGroupedMatrix implements IPropagationAnalysis {

//region Constructors
    /**
     * Creates a new SymmetricDSM object. Creates no row or column items and metadata are empty strings.
     * There is one grouping, which is the default: "(None)"
     */
    public AsymmetricDSM() {
        super();

        connections = new Vector<>();
        groupings = FXCollections.observableSet();

        this.wasModified = true;

        clearStacks();
    }


    /**
     * Copy constructor for SymmetricDSM class. Performs a deep copy
     *
     * @param copy SymmetricDSM object to copy
     */
    public AsymmetricDSM(AsymmetricDSM copy) {
        super();

        undoStack = new Stack<>();
        redoStack = new Stack<>();

        rows = new Vector<>();
        for(DSMItem row : copy.getRows()) {
            rows.add(new DSMItem(row));
        }

        cols = new Vector<>();
        for(DSMItem col : copy.getCols()) {
            cols.add(new DSMItem(col));
        }

        connections = new Vector<>();
        for(DSMConnection conn : copy.getConnections()) {
            connections.add(new DSMConnection(conn));
        }


        groupings = FXCollections.observableSet();
        groupings.addAll(copy.groupings);

        title = copy.getTitle();
        projectName = copy.getProjectName();
        customer = copy.getCustomer();
        versionNumber = copy.getVersionNumber();

        this.wasModified = true;

        clearStacks();
    }
//endregion


    /**
     * Creates a new item and adds it to the matrix and the stack. Overrides to set a default group
     *
     * @param name   the name of the item to create and add
     * @param isRow  is the item a row
     */
    @Override
    public void createItem(String name, boolean isRow) {
        double index = (int)getRowMaxSortIndex() + 1;  // cast to int to remove the decimal place so that the index will be a whole number

        DSMItem item = new DSMItem(index, name);
        item.setGroup1(defaultGroup);
        item.setAliasUid(null);

        addItem(item, isRow);
    }


    /**
     * Sets the group of an item in the matrix symmetrically. This method should be called instead of directly modifying the item
     * because this method puts the change on the stack but does not set a checkpoint.
     *
     * @param item     the item to change the name of
     * @param newGroup the new group for the item
     */
    @Override
    public void setItemGroup(DSMItem item, Grouping newGroup) {
        Grouping oldGroup = item.getGroup1();

        boolean addedNewGroup = !groupings.contains(newGroup) && !newGroup.equals(defaultGroup);

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    if(addedNewGroup) {
                        addGrouping(newGroup);
                    }
                    item.setGroup1(newGroup);
                },
                () -> {  // undo function
                    if(addedNewGroup) {
                        removeGrouping(newGroup);
                    }
                    item.setGroup1(oldGroup);
                },
                false
        ));
    }


    /**
     * Creates a 2d ArrayList of the matrix so that it can be displayed. Each cell in the grid is made
     * up of a String, which is the key, and an Object that is different based on the key.
     * Possible keys:
     *   plain_text            : String -> text
     *   plain_text_v          : String -> text
     *   item_name             : Integer -> item uid
     *   item_name_v           : Integer -> item uid
     *   grouping_item         : Integer -> item uid
     *   grouping_item_v       : Integer -> item uid
     *   index_item            : Integer -> item uid
     *   uneditable_connection : null
     *   editable_connection   : Pair<Integer, Integer> -> rowUid, colUid
     *
     * @return 2d ArrayList of matrix
     */
    public ArrayList<ArrayList<Pair<String, Object>>> getGridArray() {
        ArrayList<ArrayList<Pair<String, Object>>> grid = new ArrayList<>();

        // sort row and columns by sortIndex
        rows.sort(Comparator.comparing(DSMItem::getSortIndex));
        cols.sort(Comparator.comparing(DSMItem::getSortIndex));

        // create header row
        ArrayList<Pair< String, Object> > row0 = new ArrayList<>();
        row0.add(new Pair<>("plain_text_v", ""));
        row0.add(new Pair<>("plain_text_v", ""));
        row0.add(new Pair<>("plain_text_v", "Column Items"));
        for(DSMItem c : cols) {
            row0.add(new Pair<>("item_name_v", c));
        }
        grid.add(row0);

        ArrayList<Pair<String, Object>> row1 = new ArrayList<>();
        row1.add(new Pair<>("plain_text_v", ""));
        row1.add(new Pair<>("plain_text_v", ""));
        row1.add(new Pair<>("plain_text_v", "Grouping"));
        for (DSMItem c : cols) {
            row1.add(new Pair<>("grouping_item_v", c));
        }
        grid.add(row1);

        // create third header row
        ArrayList<Pair< String, Object> > row2 = new ArrayList<>();
        row2.add(new Pair<>("plain_text", "Grouping"));
        row2.add(new Pair<>("plain_text", "Row Items"));
        row2.add(new Pair<>("plain_text", "Re-Sort Index"));
        for(DSMItem c : cols) {
            row2.add(new Pair<>("index_item", c));
        }

        grid.add(row2);

        // create rows
        for(DSMItem r : rows) {
            ArrayList<Pair< String, Object> > row = new ArrayList<>();
            row.add(new Pair<>("grouping_item", r));
            row.add(new Pair<>("item_name", r));
            row.add(new Pair<>("index_item", r));
            for(DSMItem c : cols) {  // create connection items for all columns
                row.add(new Pair<>("editable_connection", new Pair<>(r, c)));
            }
            grid.add(row);
        }

        return grid;
    }


    /**
     * Runs propagation analysis for a matrix. Pick a start item and each level find the connections of the items in the
     * previous level. Items that are excluded are added to the count, but not propagated through.
     *
     * @param startItem     the item to start at
     * @param numLevels     number of levels to run
     * @param exclusions    array of item uids to be excluded
     * @param minWeight     minimum weight for item to be included
     * @param countByWeight count by weight or by occurrence
     * @return              HashMap(level : Hashmap(uid, occurrences/weights))
     */
    @Override
    public HashMap<Integer, HashMap<Integer, Double>> propagationAnalysis(Integer startItem, int numLevels, ArrayList<Integer> exclusions, double minWeight, boolean countByWeight) {
        int currentLevel = 1;
        HashMap<Integer, HashMap<Integer, Double>> results = new HashMap<>();
        ArrayList<Integer> dependentConnections = new ArrayList<>();
        dependentConnections.add(startItem);
        exclusions.add(startItem);

        // check if start item is a row or column item
        boolean startIsRow = rows.contains(getItem(startItem));

        while(currentLevel <= numLevels) {
            ArrayList<Integer> newDependentConnections = new ArrayList<>();
            results.put(currentLevel, new HashMap<>());  // add default item

            if((currentLevel % 2 == 1 && startIsRow) || (currentLevel % 2 == 0 && !startIsRow)) {  // currentLevel is odd so choose row
                for(Integer uid : dependentConnections) {  // find dependent connections of each item from the previous level

                    // find connections with uid as the row item
                    for(DSMItem col : cols) {  // iterate over column items finding the ones that match the row
                        DSMConnection conn = getConnection(uid, col.getUid());

                        // define exit conditions
                        if(conn == null) continue;
                        if(conn.getWeight() < minWeight) continue;

                        Integer resultEntryUid = col.getUid();
                        results.get(currentLevel).putIfAbsent(resultEntryUid, 0.0);

                        if(countByWeight) {
                            results.get(currentLevel).put(resultEntryUid, results.get(currentLevel).get(resultEntryUid) + conn.getWeight());
                        } else {
                            results.get(currentLevel).put(resultEntryUid, results.get(currentLevel).get(resultEntryUid) + 1.0);
                        }

                        if(!exclusions.contains(resultEntryUid) && !newDependentConnections.contains(resultEntryUid)) {  // add to next level if not present and not excluded
                            newDependentConnections.add(col.getUid());  // add the actual item uid
                        }
                    }
                }
            } else {  // currentLevel is even so choose column
                for(Integer uid : dependentConnections) {  // find dependent connections of each item from the previous level

                    // find connections with uid as the row item
                    for(DSMItem row : rows) {  // iterate over row items finding the ones that match the column
                        DSMConnection conn = getConnection(row.getUid(), uid);

                        // define exit conditions
                        if(conn == null) continue;
                        if(conn.getWeight() < minWeight) continue;

                        Integer itemUid = row.getUid();
                        results.get(currentLevel).putIfAbsent(itemUid, 0.0);

                        if(countByWeight) {
                            results.get(currentLevel).put(itemUid, results.get(currentLevel).get(itemUid) + conn.getWeight());
                        } else {
                            results.get(currentLevel).put(itemUid, results.get(currentLevel).get(itemUid) + 1.0);
                        }

                        if(!exclusions.contains(itemUid) && !newDependentConnections.contains(itemUid)) {  // add to next level if not present and not excluded
                            newDependentConnections.add(itemUid);
                        }
                    }
                }
            }

            dependentConnections.clear();
            dependentConnections = newDependentConnections;
            currentLevel += 1;
        }

        return results;
    }
}