package Matrices.Data;


import Matrices.Data.Entities.*;
import Matrices.Data.Flags.IZoomable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import org.javatuples.Triplet;

import java.util.*;


/**
 * A class that contains data about a matrix. All operations to a matrix come through
 * this class. Handles both symmetrical and non-symmetrical matrices.
 * Note: items in a multi-domain dsm will use the property Grouping.group1 to configure groups
 * and Grouping.group2 to configure domains
 * TODO: probably need override for adding an item to ensure item doesn't have a grouping that is not being tracked
 *
 * @author: Aiden Carney
 */
public class MultiDomainDSMData extends AbstractDSMData implements IZoomable {
    private final Grouping defaultDomain = new Grouping(DEFAULT_GROUP_UID, "default", Color.WHITE, Grouping.defaultFontColor);
    private ObservableMap<Grouping, ObservableList<Grouping>> domains;  // hashmap of domains and list of groupings corresponding to that domain

    public static final Integer DEFAULT_GROUP_UID = Integer.MAX_VALUE;

    /**
     * Creates a default domain-grouping and adds it to the hashmap
     *
     * @param domain  the domain that this is the default of
     */
    private void createNewDefaultDomainGroup(Grouping domain) {
        if(getDefaultDomainGroup(domain) == null) {
            Grouping group = new Grouping(DEFAULT_GROUP_UID, "default", Color.WHITE, Grouping.defaultFontColor);
            addDomainGrouping(domain, group);
        }
    }

    /**
     * @param domain  the domain to get the default group for
     * @return        the default domain grouping object
     */
    private Grouping getDefaultDomainGroup(Grouping domain) {
        for(Grouping domainGrouping : domains.get(domain)) {
            if(domainGrouping.getUid().equals(DEFAULT_GROUP_UID)) {
                return domainGrouping;
            }
        }
        return null;
    }


//region Constructors
    /**
     * Creates a new MultiDomainDSMData object. Creates no row or column items and metadata are empty strings.
     * There is one domain, which is the default: "(None)"
     */
    public MultiDomainDSMData() {
        super();

        connections = new Vector<>();
        domains = FXCollections.observableHashMap();
        domains.put(defaultDomain, FXCollections.observableArrayList());  // add the default
        createNewDefaultDomainGroup(defaultDomain);

        setWasModified();

        clearStacks();
    }


    /**
     * Creates a new MultiDomainDSMData object. Creates no row or column items and metadata are empty strings.
     * Creates the domains so that the default need not be present. If the arraylist is empty it will put
     * the default domain in so that there is always at least one domain
     */
    public MultiDomainDSMData(HashMap<Grouping, Collection<Grouping>> domains) {
        super();

        connections = new Vector<>();

        this.domains = FXCollections.observableHashMap();
        if(domains.size() > 0) {
            for(Map.Entry<Grouping, Collection<Grouping>> domain : domains.entrySet()) {
                ObservableList<Grouping> domainGroupings = FXCollections.observableArrayList();
                domainGroupings.addAll(domain.getValue());
                this.domains.put(domain.getKey(), domainGroupings);
            }
        } else {
            this.domains.put(defaultDomain, FXCollections.observableArrayList());
        }
        setWasModified();

        clearStacks();
    }


    /**
     * Copy constructor for SymmetricDSMData class. Performs a deep copy
     *
     * @return  the copy of the current symmetric DSM
     */
    @Override
    public MultiDomainDSMData createCopy() {
        MultiDomainDSMData copy = new MultiDomainDSMData();

        for(DSMItem row : getRows()) {
            copy.rows.add(new DSMItem(row));
        }

        for(DSMItem col : getCols()) {
            copy.cols.add(new DSMItem(col));
        }

        for(DSMConnection conn : getConnections()) {
            copy.connections.add(new DSMConnection(conn));
        }

        copy.domains = FXCollections.observableHashMap();
        for(ObservableMap.Entry<Grouping, ObservableList<Grouping>> entry : domains.entrySet()) {
            ObservableList<Grouping> domainGroupings = FXCollections.observableArrayList();
            for(Grouping domainGrouping : getDomainGroupings(entry.getKey())) {
                domainGroupings.add(new Grouping(domainGrouping));
            }
            copy.domains.put(new Grouping(entry.getKey()), domainGroupings);
        }

        for(Map.Entry<String, Vector<DSMInterfaceType>> interfaceGroup : getInterfaceTypes().entrySet()) {
            Vector<DSMInterfaceType> interfaces = new Vector<>();
            for(DSMInterfaceType i : interfaceGroup.getValue()) {
                interfaces.add(new DSMInterfaceType(i));
            }
            copy.interfaceTypes.put(interfaceGroup.getKey(), interfaces);
        }

        copy.setTitle(getTitle());
        copy.setProjectName(getProjectName());
        copy.setCustomer(getCustomer());
        copy.setVersionNumber(getVersionNumber());

        copy.setWasModified();
        copy.setWasModified();

        return copy;
    }

//endregion


//region configure domains and domain groupings

    /**
     * Adds a new domain to the matrix. Puts the change on the stack but does not set a checkpoint
     *
     * @param domain  the object of type Grouping to add
     */
    public void addDomain(Grouping domain) {
        if(domains.containsKey(domain)) return;

        Grouping group = new Grouping(DEFAULT_GROUP_UID, "default", Color.WHITE, Grouping.defaultFontColor);
        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    domains.put(domain, FXCollections.observableArrayList());
                    domains.get(domain).add(group);
                },
                () -> {  // undo function
                    domains.remove(domain);

                    // items cannot exist outside of a domain so no items in this domain should exist because items
                    // in this domain have to be created after the domain was created. As such there is no need
                    // to change any of the items
                },
                false
        ));
    }


    /**
     * Adds a new domain grouping to the matrix for a given domain. Puts the change on the stack but does not set
     * a checkpoint
     *
     * @param domain  the domain to add the new group to
     * @param group   the object of type Grouping to add
     */
    public void addDomainGrouping(Grouping domain, Grouping group) {
        if(domains.get(domain).contains(group)) return;

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    domains.get(domain).add(group);
                },
                () -> {  // undo function
                    domains.get(domain).remove(group);
                },
                false
        ));
    }


    /**
     * Removes a domain from the matrix. Puts the change on the stack but does not set a checkpoint. Only removes
     * the domain if the domains will not be empty
     *
     * @param domain  the object of type Grouping to remove
     * @return        -1 if the operation was not completed because there always has to be one domain, or 0 if successful
     */
    public int removeDomain(Grouping domain) {
        if(domains.size() <= 1) return -1;

        ObservableList<Grouping> domainGroupings = domains.get(domain);

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    // remove all the items with the given domain
                    ArrayList<DSMItem> rowsToDelete = new ArrayList<>();
                    for (DSMItem item : rows) {
                        if (item.getGroup2().equals(domain)) rowsToDelete.add(item);
                    }

                    for (DSMItem item : rowsToDelete) {  // remove all the items symmetrically
                        deleteItem(item);
                    }

                    domains.remove(domain);
                },
                () -> {  // undo function
                    domains.put(domain, domainGroupings);
                },
                false
        ));
        return 0;
    }


    /**
     * Removes a grouping from the matrix. Puts the change on the stack but does not set a checkpoint
     *
     * @param group  the object of type Grouping to remove
     */
    public void removeDomainGrouping(Grouping domain, Grouping group) {
        if(group.equals(getDefaultDomainGroup(domain))) {  // don't allow deleting the default domain-grouping
            return;
        }

        for(DSMItem item : rows) {  // these changes already get put on the stack so no need to add them a second time
            if(item.getGroup2().equals(domain) && item.getGroup1().equals(group)) {
                setItemDomainGroup(item, domain, getDefaultDomainGroup(domain));
            }
        }
        for(DSMItem item : cols) {
            if(item.getGroup2().equals(domain) && item.getGroup1().equals(group)) {
                setItemDomainGroup(item, domain, getDefaultDomainGroup(domain));
            }
        }

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    domains.get(domain).remove(group);
                },
                () -> {  // undo function
                    domains.get(domain).add(group);
                },
                false
        ));
    }


    /**
     * Removes all groupings from the matrix. Puts the change on the stack but does not set a checkpoint
     */
    public void clearDomainGroupings(Grouping domain) {
        ObservableList<Grouping> oldGroupings = FXCollections.observableArrayList();
        oldGroupings.addAll(domains.get(domain));

        for(DSMItem r : rows) {
            setItemDomainGroup(r, r.getGroup2(), getDefaultDomainGroup(domain));
        }
        for(DSMItem c : cols) {
            setItemDomainGroup(c, c.getGroup2(), getDefaultDomainGroup(domain));
        }

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    domains.get(domain).clear();
                    createNewDefaultDomainGroup(domain);  // add the default back
                },
                () -> {  // undo function
                    domains.put(domain, oldGroupings);
                },
                false
        ));
    }


    /**
     * @return  ObservableList of the matrix domains
     */
    public ObservableList<Grouping> getDomains() {
        return FXCollections.observableArrayList(domains.keySet());
    }


    /**
     * @return  the ObservableList of the groupings in a current domain
     */
    public ObservableList<Grouping> getDomainGroupings(Grouping domain) {
        Comparator<Grouping> groupingComparator = (o1, o2) -> {
            if(o1.getUid().equals(DEFAULT_GROUP_UID)) return -1;
            if(o2.getUid().equals(DEFAULT_GROUP_UID)) return 1;

            return o1.getName().compareTo(o2.getName());
        };

        FXCollections.sort(domains.get(domain), groupingComparator);
        return domains.get(domain);
    }


    /**
     * @return  an ObservableList of all domain groupings
     */
    public ObservableList<Grouping> getDomainGroupings() {
        ObservableList<Grouping> domainGroupings = FXCollections.observableArrayList();
        for(ObservableList<Grouping> groupings : domains.values()) {
            domainGroupings.addAll(groupings);
        }
        return domainGroupings;
    }


    /**
     * Renames a grouping. Puts the change on the stack but does not set a checkpoint. This method can be used
     * for either domains or domain-groupings
     *
     * @param grouping  the group who's name should be changed
     * @param newName   the new name for the group
     */
    public void renameGrouping(Grouping grouping, String newName) {
        String oldName = grouping.getName();

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    grouping.setName(newName);
                },
                () -> {  // undo function
                    grouping.setName(oldName);
                },
                false
        ));
    }


    /**
     * Changes a color of a grouping. Puts the change on the stack but does not set a checkpoint.
     * This method can be used for either domains or domain-groupings
     *
     * @param grouping  the group who's name should be changed
     * @param newColor  the new color of the grouping
     */
    public void updateGroupingColor(Grouping grouping, Color newColor) {
        Color oldColor = grouping.getColor();
        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    grouping.setColor(newColor);
                },
                () -> {  // undo function
                    grouping.setColor(oldColor);
                },
                false
        ));
    }


    /**
     * Changes a color of a grouping. Puts the change on the stack but does not set a checkpoint. This method
     * can be used for either domains or domain-groupings
     *
     * @param grouping  the grouping who's font color should be changed
     * @param newColor  the new color of the grouping
     */
    public void updateGroupingFontColor(Grouping grouping, Color newColor) {
        Color oldColor = grouping.getFontColor();
        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    grouping.setFontColor(newColor);
                },
                () -> {  // undo function
                    grouping.setFontColor(oldColor);
                },
                false
        ));
    }


    /**
     * Sets the domain group of an item in the matrix symmetrically. This method should be called instead of directly modifying the item
     * because this method puts the change on the stack but does not set a checkpoint.
     *
     * @param item     the item to change the name of
     * @param newGroup the new group for the item
     */
    public void setItemDomainGroup(DSMItem item, Grouping domain, Grouping newGroup) {
        DSMItem aliasedItem = getItemByAlias(item.getUid());
        Grouping oldGroup = item.getGroup1();

        assert oldGroup.getUid().equals(aliasedItem.getGroup1().getUid()) : "Symmetric item groupings were not the same";

        if(!domains.containsKey(domain)) {  // add the domain if it is not there already
            addDomain(domain);
        }

        boolean addedNewGroup = !domains.get(domain).contains(newGroup);

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    if (addedNewGroup) {
                        domains.get(domain).add(newGroup);
                    }
                    item.setGroup1(newGroup);
                    aliasedItem.setGroup1(newGroup);
                },
                () -> {  // undo function
                    if (addedNewGroup) {
                        domains.get(domain).remove(newGroup);
                    }
                    item.setGroup1(oldGroup);
                    aliasedItem.setGroup1(oldGroup);
                },
                false
        ));
    }
//endregion


//region Add and Delete Items
    /**
     * Creates a new item and adds it to the matrix and the stack. Also configures its domain. Creates the domain if
     * it is not already in the domain list
     *
     * @param name    the name of the item to create and add
     * @param isRow   is the item a row
     * @param domain  the domain for the item (for MDMs this method should be used because item domains cannot be changed)
     */
    public void createItem(String name, boolean isRow, Grouping domain) {
        double index = (int)getRowMaxSortIndex() + 1;  // cast to int to remove the decimal place so that the index will be a whole number

        DSMItem rowItem = new DSMItem(index, name);
        DSMItem colItem = new DSMItem(index, name);

        // configure domain and group
        if(!domains.containsKey(domain)) {  // add the domain if it does not exist already
            addDomain(domain);
        }
        rowItem.setGroup2(domain);
        colItem.setGroup2(domain);
        rowItem.setGroup1(getDefaultDomainGroup(domain));
        colItem.setGroup1(getDefaultDomainGroup(domain));

        colItem.setAliasUid(rowItem.getUid());
        rowItem.setAliasUid(colItem.getUid());

        addItem(rowItem, true);
        addItem(colItem, false);
    }


    /**
     * Creates a new item and adds it to the matrix and the stack. Sets the domain the default domain of the class
     *
     * @param name   the name of the item to create and add
     * @param isRow  is the item a row
     */
    @Override
    public void createItem(String name, boolean isRow) {
        createItem(name, isRow, defaultDomain);
    }



    /**
     * Deletes an item from the matrix symmetrically. Puts the change on the stack but does not set a checkpoint
     *
     * @param item the item to delete
     */
    @Override
    public void deleteItem(DSMItem item) {
        boolean isRow = rows.contains(item);  // check if the item was a row in case it needs to be added again
        DSMItem aliasedItem = getItemByAlias(item.getUid());

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    removeItem(item);
                    removeItem(aliasedItem);
                },
                () -> {  // undo function
                    if (isRow) {
                        this.rows.add(item);
                        this.cols.add(aliasedItem);
                    } else {
                        this.rows.add(aliasedItem);
                        this.cols.add(item);
                    }
                },
                false
        ));
    }
//endregion


//region Setters for Items (name, index, group)
    /**
     * Sets the name of an item in the matrix symmetrically. This method should be called instead of directly modifying the
     * item name because this method puts the change on the stack but does not set a checkpoint.
     *
     * @param item    the item to change the name of
     * @param newName the new name for the item
     */
    @Override
    public void setItemName(DSMItem item, String newName) {
        DSMItem aliasedItem = getItemByAlias(item.getUid());
        String oldName = item.getName().getValue();

        assert oldName.equals(aliasedItem.getName().getValue()) : "Symmetric item names were not the same";

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    item.setName(newName);
                    aliasedItem.setName(newName);
                },
                () -> {  // undo function
                    item.setName(oldName);
                    aliasedItem.setName(oldName);
                },
                false
        ));
    }


    /**
     * Sets the sort index of an item in the matrix symmetrically. This method should be called instead of directly
     * modifying the item because this method puts the change on the stack but does not set a checkpoint.
     *
     * @param item     the item to change the name of
     * @param newIndex the new index for the item
     */
    @Override
    public void setItemSortIndex(DSMItem item, double newIndex) {
        DSMItem aliasedItem = getItemByAlias(item.getUid());
        double oldIndex = item.getSortIndex();

        assert oldIndex == aliasedItem.getSortIndex() : "Symmetric item sort indices were not the same";

        addChangeToStack(new MatrixChange(
                () -> {  // do function
                    item.setSortIndex(newIndex);
                    aliasedItem.setSortIndex(newIndex);
                },
                () -> {  // undo function
                    item.setSortIndex(oldIndex);
                    aliasedItem.setSortIndex(oldIndex);
                },
                false
        ));
    }
//endregion


//region Connection Modification Methods
    /**
     * Creates a connection and adds it to the matrix, but does not add the change to the stack. Overrides to add assertion
     * that row and column do not alias to each other because this should never happen with a symmetric matrix
     *
     * @param rowUid         the row item uid
     * @param colUid         the column item uid
     * @param connectionName the name of the connection
     * @param weight         the weight of the connection
     */
    @Override
    protected void createConnection(int rowUid, int colUid, String connectionName, double weight, ArrayList<DSMInterfaceType> interfaces) {
        // add assertion in this override
        assert getItem(rowUid).getUid() != getItem(colUid).getAliasUid();  // corresponds to where row and column are same and thus connection cannot be made

        DSMConnection connection = new DSMConnection(connectionName, weight, rowUid, colUid, interfaces);
        connections.add(connection);
    }


    /**
     * Modifies a connection symmetrically. Can only be used with symmetric matrices. Puts the change on the
     * stack but does not set a checkpoint
     *
     * @param rowUid         the row item uid of one of the connections
     * @param colUid         the column item uid of one of the connections
     * @param connectionName the new name of the connections
     * @param weight         the new weight of the connections
     */
    public void modifyConnectionSymmetric(int rowUid, int colUid, String connectionName, double weight, ArrayList<DSMInterfaceType> interfaces) {
        Pair<Integer, Integer> uids = getSymmetricConnectionUids(rowUid, colUid);
        modifyConnection(rowUid, colUid, connectionName, weight, interfaces);
        modifyConnection(uids.getKey(), uids.getValue(), connectionName, weight, interfaces);
    }
//endregion

    /**
     * Sorts the current matrix rows and columns by sort index and modifies all the sort Indices
     * such that they are now 1 to n. Used to make the sort Indices "clean" numbers. Puts multiple changes on the
     * stack but does not set any checkpoint. Re-distributes by domain so count resets once the domain changes
     */
    @Override
    public void reDistributeSortIndices() {
        // go domain by domain sorting the rows
        for(Grouping domain : domains.keySet()) {
            ArrayList<DSMItem> domainRows = new ArrayList<>(rows.stream().filter(r -> r.getGroup2().equals(domain)).toList());
            ArrayList<DSMItem> domainCols = new ArrayList<>(cols.stream().filter(c -> c.getGroup2().equals(domain)).toList());
            domainRows.sort(Comparator.comparing(DSMItem::getSortIndex));
            domainCols.sort(Comparator.comparing(DSMItem::getSortIndex));
            for(int i=0; i<domainRows.size(); i++) {  // reset row sort Indices 1 -> n
                setItemSortIndex(domainRows.get(i), i + 1);
            }
            for(int i=0; i<domainCols.size(); i++) {  // reset col sort Indices 1 -> n
                setItemSortIndex(domainCols.get(i), i + 1);
            }
        }
    }


    /**
     * Finds the uids of the connection symmetric to the connection with uids rowUid and colUid. For example,
     * x, y returns y, column uid with alias x
     *
     * @param rowUid the uid of the row item of the connection
     * @param colUid the uid of the column item of the connection
     * @return       Pair of type Integer of symmetric row uid, symmetric col uid
     */
    public Pair<Integer, Integer> getSymmetricConnectionUids(int rowUid, int colUid) {
        Integer newRowUid = getItem(colUid).getAliasUid();
        Integer newColUid = null;
        for(DSMItem item : getCols()) {
            if(item.getAliasUid() != null && item.getAliasUid() == rowUid) {
                newColUid = item.getUid();
                break;
            }
        }

        if(newColUid != null && newRowUid != null) {
            return new Pair<>(newRowUid, newColUid);
        }
        return null;
    }


    /**
     * Returns the symmetric connection of a given pair of uids
     *
     * @param   rowUid the uid of the row item of the connection
     * @param   colUid the uid of the column item of the connection
     * @return  the DSMConnection object of the symmetric connection
     */
    public DSMConnection getSymmetricConnection(int rowUid, int colUid) {
        Pair<Integer, Integer> symmetricUids = getSymmetricConnectionUids(rowUid, colUid);
        return getConnection(symmetricUids.getKey(), symmetricUids.getValue());
    }


    /**
     * Creates a 2d ArrayList of the matrix so that it can be displayed. Each cell in the grid is made
     * up of a RenderMode, which is the key, and an Object that is different based on the key.
     *
     * @return 2d ArrayList of matrix
     */
    public ArrayList<ArrayList<Pair<RenderMode, Object>>> getGridArray() {
        ArrayList<ArrayList<Pair<RenderMode, Object>>> grid = new ArrayList<>();

        // sort row and columns by sortIndex
        rows.sort(Comparator.comparing((DSMItem item) -> item.getGroup2().getName()).thenComparing(DSMItem::getSortIndex));
        cols.sort(Comparator.comparing((DSMItem item) -> item.getGroup2().getName()).thenComparing(DSMItem::getSortIndex));

        // create header row
        ArrayList<Pair<RenderMode, Object>> row0 = new ArrayList<>();
        row0.add(new Pair<>(RenderMode.PLAIN_TEXT_V, ""));
        row0.add(new Pair<>(RenderMode.PLAIN_TEXT_V, ""));
        row0.add(new Pair<>(RenderMode.PLAIN_TEXT_V, ""));
        row0.add(new Pair<>(RenderMode.PLAIN_TEXT_V, "Column Items"));
        for(DSMItem c : cols) {
            row0.add(new Pair<>(RenderMode.ITEM_NAME_V, c));
        }
        grid.add(row0);

        // create third header row
        ArrayList<Pair<RenderMode, Object>> row1 = new ArrayList<>();
        row1.add(new Pair<>(RenderMode.PLAIN_TEXT, "Domain"));
        row1.add(new Pair<>(RenderMode.PLAIN_TEXT, "Grouping"));
        row1.add(new Pair<>(RenderMode.PLAIN_TEXT, "Row Items"));
        row1.add(new Pair<>(RenderMode.PLAIN_TEXT, "Re-Sort Index"));
        for(DSMItem c : cols) {
            row1.add(new Pair<>(RenderMode.PLAIN_TEXT, ""));
        }
        grid.add(row1);

        // create rows
        boolean firstItemInDomain;
        Grouping previousItemDomain = null;
        for(DSMItem r : rows) {
            ArrayList<Pair<RenderMode, Object>> row = new ArrayList<>();
            // if domain switched then this is the first item in the domain
            firstItemInDomain = !r.getGroup2().equals(previousItemDomain);

            if(firstItemInDomain) {
                int numItemsInDomain = (int) rows.stream().filter(item -> item.getGroup2().equals(r.getGroup2())).count();
                row.add(new Pair<>(RenderMode.MULTI_SPAN_DOMAIN_TEXT, new Triplet<>(r.getGroup2(), numItemsInDomain, 1)));  // text, row span, col span
            } else {
                row.add(new Pair<>(RenderMode.MULTI_SPAN_NULL, null));
            }
            row.add(new Pair<>(RenderMode.GROUPING_ITEM, r));
            row.add(new Pair<>(RenderMode.ITEM_NAME, r));
            row.add(new Pair<>(RenderMode.INDEX_ITEM, r));
            for(DSMItem c : cols) {  // create connection items for all columns
                if(c.getAliasUid() == r.getUid()) {  // can't have connection to itself in a symmetrical matrix
                    row.add(new Pair<>(RenderMode.UNEDITABLE_CONNECTION, null));
                } else {
                    row.add(new Pair<>(RenderMode.EDITABLE_CONNECTION, new Pair<>(r, c)));
                }
            }
            grid.add(row);
            previousItemDomain = r.getGroup2();
        }

        return grid;
    }


    /**
     * Takes a matrix from a breakout view and merges its changes
     *
     * @param fromGroup     the "from" grouping item that defines where the breakout view is from
     * @param toGroup       the "to" grouping item that defines where the breakout view is from
     * @param importMatrix  the breakout view matrix
     */
    @Override
    public void importZoom(Grouping fromGroup, Grouping toGroup, AbstractDSMData importMatrix) {

        // ensure all items from the import matrix have the correct domain
        for(DSMItem row : importMatrix.getRows()) {
            row.setGroup2(fromGroup);
        }
        for(DSMItem col : importMatrix.getCols()) {
            col.setGroup2(toGroup);
        }

        if(importMatrix instanceof SymmetricDSMData symmetricMatrix) {  // importing a symmetric matrix
            assert fromGroup.equals(toGroup) : "Importing a symmetric matrix, but they don't have the same domain";


            // merge in the groupings
            ObservableList<Grouping> currentDomainGroupings = getDomainGroupings(fromGroup);
            addChangeToStack(new MatrixChange(
                    () -> {  // do function
                        domains.put(fromGroup, symmetricMatrix.getGroupings());
                    },
                    () -> {  // undo function
                        domains.put(fromGroup, currentDomainGroupings);
                    },
                    false
            ));

            // merge in the dsm items
            Vector<DSMItem> rowsToDelete = new Vector<>(rows.stream().filter(o -> o.getGroup2().equals(fromGroup)).toList());
            for(DSMItem importedRow : importMatrix.getRows()) {
                if(rows.stream().anyMatch(r -> r.getUid() == importedRow.getUid())) {  // item is already contained so modify it and its connections by doing a copy replace
                    DSMItem rowItem = getItem(importedRow.getUid());
                    DSMItem colItem = getItemByAlias(importedRow.getUid());

                    // update the item fields
                    setItemName(rowItem, importedRow.getName().getValue());
                    setItemSortIndex(rowItem, importedRow.getSortIndex());
                    setItemDomainGroup(rowItem, fromGroup, importedRow.getGroup1());
                    setItemName(colItem, importedRow.getName().getValue());
                    setItemSortIndex(colItem, importedRow.getSortIndex());
                    setItemDomainGroup(colItem, fromGroup, importedRow.getGroup1());


                    // clear connections in this domain and this row
                    ArrayList<DSMConnection> domainConnections = new ArrayList<>();
                    for(DSMConnection conn : getConnections()) {
                        DSMItem connRowItem = getItem(conn.getRowUid());
                        DSMItem connColItem = getItem(conn.getColUid());
                        if(connRowItem == null || connColItem == null) {
                            continue;
                        }
                        if(connRowItem.getUid() == rowItem.getUid() && connRowItem.getGroup2().equals(fromGroup) && connColItem.getGroup2().equals(toGroup)) {
                            domainConnections.add(conn);
                        }
                    }
                    for(DSMConnection conn : domainConnections) {  // remove connections, but put change on the stack
                        addChangeToStack(new MatrixChange(
                            () -> {  // do function
                                removeConnection(conn.getRowUid(), conn.getColUid());
                            },
                            () -> {  // undo function
                                createConnection(conn.getRowUid(), conn.getColUid(), conn.getConnectionName(), conn.getWeight(), conn.getInterfaces());
                            },
                            false
                        ));
                    }

                    rowsToDelete.remove(rowItem);
                } else {  // item is not contained so add it
                    DSMItem col = new DSMItem(importedRow);
                    col.setAliasUid(importedRow.getUid());
                    importedRow.setAliasUid(col.getUid());

                    addItem(importedRow, true);
                    addItem(col, false);
                }
            }
            for(DSMItem row : rowsToDelete) {  // delete the remaining items to finish the merge
                deleteItem(row);
            }

            // merge the connections
            for(DSMConnection conn : symmetricMatrix.getConnections()) {
                DSMItem connRowItem = getItem(conn.getRowUid());
                DSMItem connColItem = getItem(conn.getColUid());
                assert connRowItem != null && connColItem != null : "Merging rows from symmetric DSM failed";

                modifyConnection(conn.getRowUid(), conn.getColUid(), conn.getConnectionName(), conn.getWeight(), conn.getInterfaces());
            }


        } else if(importMatrix instanceof AsymmetricDSMData asymmetricMatrix) {  // importing an asymmetric matrix
            ObservableList<Grouping> currentFromDomainGroupings = getDomainGroupings(fromGroup);
            ObservableList<Grouping> currentToDomainGroupings = getDomainGroupings(toGroup);

            ObservableList<Grouping> fromDomainGroupings = FXCollections.observableArrayList();
            fromDomainGroupings.addAll(asymmetricMatrix.getGroupings(true));

            ObservableList<Grouping> toDomainGroupings = FXCollections.observableArrayList();
            toDomainGroupings.addAll(asymmetricMatrix.getGroupings(false));

            addChangeToStack(new MatrixChange(
                    () -> {  // do function
                        domains.put(fromGroup, fromDomainGroupings);
                        domains.put(toGroup, toDomainGroupings);
                    },
                    () -> {  // undo function
                        domains.put(fromGroup, currentFromDomainGroupings);
                        domains.put(toGroup, currentToDomainGroupings);
                    },
                    false
            ));


            // merge in the row items
            Vector<DSMItem> rowsToDelete = new Vector<>(rows.stream().filter(o -> o.getGroup2().equals(fromGroup)).toList());
            for(DSMItem importedRow : importMatrix.getRows()) {
                if(rows.stream().anyMatch(r -> r.getUid() == importedRow.getUid())) {  // item is already contained so modify it and its connections by doing a copy replace
                    DSMItem rowItem = getItem(importedRow.getUid());
                    DSMItem colItem = getItemByAlias(importedRow.getUid());

                    // update the item fields
                    setItemName(rowItem, importedRow.getName().getValue());
                    setItemSortIndex(rowItem, importedRow.getSortIndex());
                    setItemDomainGroup(rowItem, fromGroup, importedRow.getGroup1());
                    setItemName(colItem, importedRow.getName().getValue());
                    setItemSortIndex(colItem, importedRow.getSortIndex());
                    setItemDomainGroup(colItem, fromGroup, importedRow.getGroup1());

                    // clear connections in this domain and this row
                    ArrayList<DSMConnection> domainConnections = new ArrayList<>();
                    for(DSMConnection conn : getConnections()) {
                        DSMItem connRowItem = getItem(conn.getRowUid());
                        DSMItem connColItem = getItem(conn.getColUid());
                        if(connRowItem == null || connColItem == null) {
                            continue;
                        }
                        if(connRowItem.getUid() == rowItem.getUid() && connRowItem.getGroup2().equals(fromGroup) && connColItem.getGroup2().equals(toGroup)) {
                            domainConnections.add(conn);
                        }
                    }
                    for(DSMConnection conn : domainConnections) {
                        addChangeToStack(new MatrixChange(
                                () -> {  // do function
                                    removeConnection(conn.getRowUid(), conn.getColUid());
                                },
                                () -> {  // undo function
                                    createConnection(conn.getRowUid(), conn.getColUid(), conn.getConnectionName(), conn.getWeight(), conn.getInterfaces());
                                },
                                false
                        ));
                    }

                    rowsToDelete.remove(rowItem);

                } else {  // item is not contained so add it
                    DSMItem col = new DSMItem();  // add a new column item because mdm is symmetric
                    col.copyProperties(importedRow);
                    col.setAliasUid(importedRow.getUid());
                    importedRow.setAliasUid(col.getUid());

                    addItem(importedRow, true);
                    addItem(new DSMItem(col), false);
                }
            }
            for(DSMItem row : rowsToDelete) {  // delete the remaining items to finish the merge
                deleteItem(row);
            }


            // merge in the column items
            Vector<DSMItem> colsToDelete = new Vector<>(cols.stream().filter(o -> o.getGroup2().equals(toGroup)).toList());
            for(DSMItem importedCol : importMatrix.getCols()) {
                if(cols.stream().anyMatch(c -> c.getUid() == importedCol.getUid())) {  // item is already contained so modify it and its connections by doing a copy replace
                    DSMItem rowItem = getItemByAlias(importedCol.getUid());
                    DSMItem colItem = getItem(importedCol.getUid());

                    // update the item fields
                    setItemName(rowItem, importedCol.getName().getValue());
                    setItemSortIndex(rowItem, importedCol.getSortIndex());
                    setItemDomainGroup(rowItem, fromGroup, importedCol.getGroup1());
                    setItemName(colItem, importedCol.getName().getValue());
                    setItemSortIndex(colItem, importedCol.getSortIndex());
                    setItemDomainGroup(colItem, fromGroup, importedCol.getGroup1());

                    // clear connections in this domain and this column
                    ArrayList<DSMConnection> domainConnections = new ArrayList<>();
                    for(DSMConnection conn : getConnections()) {
                        DSMItem connRowItem = getItem(conn.getRowUid());
                        DSMItem connColItem = getItem(conn.getColUid());
                        if(connRowItem == null || connColItem == null) {
                            continue;
                        }
                        if(connColItem.getUid() == colItem.getUid() && connRowItem.getGroup2().equals(fromGroup) && connColItem.getGroup2().equals(toGroup)) {
                            domainConnections.add(conn);
                        }
                    }
                    for(DSMConnection conn : domainConnections) {
                        addChangeToStack(new MatrixChange(
                                () -> {  // do function
                                    removeConnection(conn.getRowUid(), conn.getColUid());
                                },
                                () -> {  // undo function
                                    createConnection(conn.getRowUid(), conn.getColUid(), conn.getConnectionName(), conn.getWeight(), conn.getInterfaces());
                                },
                                false
                        ));
                    }

                    colsToDelete.remove(colItem);

                } else {  // item is not contained so add it
                    DSMItem row = new DSMItem();  // add a new row item because mdm is symmetric
                    row.copyProperties(importedCol);
                    row.setAliasUid(importedCol.getUid());
                    importedCol.setAliasUid(row.getUid());

                    addItem(row, true);
                    addItem(new DSMItem(importedCol), false);
                }
            }
            for(DSMItem col : colsToDelete) {  // delete the remaining items to finish the merge
                deleteItem(col);
            }


            // merge the connections
            for(DSMConnection conn : asymmetricMatrix.getConnections()) {
                DSMItem connRowItem = getItem(conn.getRowUid());
                DSMItem connColItem = getItem(conn.getColUid());
                assert connRowItem != null && connColItem != null : "Merging rows from symmetric DSM failed";

                modifyConnection(conn.getRowUid(), conn.getColUid(), conn.getConnectionName(), conn.getWeight(), conn.getInterfaces());
            }

        }

    }


    /**
     * Takes a matrix and creates a breakout view from it
     *
     * @param fromGroup  the domain that defines the row items
     * @param toGroup    the domain that defines the column items
     * @return           the matrix object that is a breakout view
     */
    @Override
    public AbstractDSMData exportZoom(Grouping fromGroup, Grouping toGroup) {
        if(fromGroup.equals(toGroup)) {
            // add the groupings
            ArrayList<Grouping> groupings = new ArrayList<>();
            for(Grouping grouping : getDomainGroupings(fromGroup)) {
                groupings.add(new Grouping(grouping));
            }

            SymmetricDSMData exportMatrix = new SymmetricDSMData(groupings);


            // find the row and column items
            for(DSMItem item : rows) {
                if(item.getGroup2().equals(fromGroup)) {
                    exportMatrix.addItem(new DSMItem(item), true);
                }
            }

            for(DSMItem item : cols) {
                if(item.getGroup2().equals(fromGroup)) {
                    exportMatrix.addItem(new DSMItem(item), false);
                }
            }

            // find the connections
            for(DSMConnection conn : connections) {
                if(exportMatrix.getRows().stream().anyMatch(r -> r.getUid() == conn.getRowUid()) && exportMatrix.getCols().stream().anyMatch(c -> c.getUid() == conn.getColUid())) {
                    exportMatrix.modifyConnection(conn.getRowUid(), conn.getColUid(), conn.getConnectionName(), conn.getWeight(), conn.getInterfaces());
                }
            }


            return exportMatrix;
        } else {
            // add the groupings
            ArrayList<Grouping> rowGroupings = new ArrayList<>();
            for(Grouping grouping : getDomainGroupings(fromGroup)) {
                rowGroupings.add(new Grouping(grouping));
            }
            ArrayList<Grouping> colGroupings = new ArrayList<>();
            for(Grouping grouping : getDomainGroupings(toGroup)) {
                colGroupings.add(new Grouping(grouping));
            }

            AsymmetricDSMData exportMatrix = new AsymmetricDSMData(rowGroupings, colGroupings);


            // find the row and column items
            for(DSMItem item : rows) {
                if(item.getGroup2().equals(fromGroup)) {
                    exportMatrix.addItem(new DSMItem(item), true);
                }
            }

            for(DSMItem item : cols) {
                if(item.getGroup2().equals(toGroup)) {
                    exportMatrix.addItem(new DSMItem(item), false);
                }
            }

            // find the connections
            for(DSMConnection conn : connections) {
                if(exportMatrix.getRows().stream().anyMatch(r -> r.getUid() == conn.getRowUid()) && exportMatrix.getCols().stream().anyMatch(c -> c.getUid() == conn.getColUid())) {
                    exportMatrix.modifyConnection(conn.getRowUid(), conn.getColUid(), conn.getConnectionName(), conn.getWeight(), conn.getInterfaces());
                }
            }


            return exportMatrix;
        }
    }

}
