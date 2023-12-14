package edu.trinity.cpsc215f23.treemap;

import edu.trinity.cpsc215f23.map.Entry;
import edu.trinity.cpsc215f23.map.Map;
import edu.trinity.cpsc215f23.tree.LinkedBinaryTree;
import edu.trinity.cpsc215f23.tree.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Realization of a map by means of a binary search tree.
 *
 * @author Takunari Miyazaki
 */
public class BinarySearchTreeMap<K, V> extends LinkedBinaryTree<Entry<K, V>> implements Map<K, V> {

    /**
     * The comparator used for key comparison in the binary search tree.
     */
    protected final Comparator<K> comparator; // comparator

    /**
     * A variable representing a node within the tree
     */
    protected Position<Entry<K, V>> actionPos; // a node variable

    /**
     * Creates a BinarySearchTreeMap with a default comparator.
     */
    public BinarySearchTreeMap() {
        comparator = new DefaultComparator<>();
        addRoot(null);
    }

    public BinarySearchTreeMap(Comparator<K> comparator) {
        this.comparator = comparator;
        addRoot(null);
    }

    /**
     * In this collection only internal nodes have entries.
     *
     * @return the number of entries in the map.
     */
    public int size(){
        return (super.size()-1)/2;
    }

    /**
     * Extracts the key of the entry at a given node of the tree.
     */
    protected K key(Position<Entry<K, V>> position) {
        return position.getElement().getKey();
    }

    /**
     * Extracts the value of the entry at a given node of the tree.
     */
    protected V value(Position<Entry<K, V>> position) {
        return position.getElement().getValue();
    }

    /**
     * Extracts the entry at a given node of the tree.
     */
    protected Entry<K, V> entry(Position<Entry<K, V>> position) {
        return position.getElement();
    }

    /**
     * Replaces an entry with a new entry (and reset the entry's location)
     */
    protected V replaceEntry(Position<Entry<K, V>> position, Entry<K, V> entry) {
        ((BSTEntry<K, V>) entry).position = position;
        return set(position, entry).getValue();
    }

    /**
     * Checks whether a given key is valid.
     */
    protected void checkKey(K key) throws IllegalArgumentException {
        if (key == null) // just a simple test for now
        {
            throw new IllegalArgumentException("Key is null.");
        }
    }

    /**
     * Checks whether a given entry is valid.
     */
    protected void checkEntry(Entry<K, V> entry) throws IllegalArgumentException {
        if (!(entry instanceof BSTEntry)) {
            throw new IllegalArgumentException("Entry is invalid, expecting type BSTEntry.");
        }
    }

    /**
     * Auxiliary method for inserting an entry at an external node. Inserts a given entry at a given external position,
     * expanding the external node to be internal with empty external children, and then returns the inserted entry.
     * (page 11.1.2)
     *
     * @param position The external position
     * @param entry    The entry to add to the external
     * @return The entry added
     */
    protected Entry<K, V> insertAtExternal(Position<Entry<K, V>> position, Entry<K, V> entry) {
            set(position, entry);
            addLeft(position, null);
            addRight(position, null);
            return entry; // Return the inserted entry

    }

    /**
     * Auxiliary method for removing an external node and its parent. Removes a given external node and its
     * parent, replacing external's parent with external's sibling.
     *
     * @param external The position to remove.
     */
    protected void removeExternal(Position<Entry<K, V>> external) {

        if (isExternal(external)) {
            Position<Entry<K, V>> parent = parent(external);
            remove(external);
            remove(parent);
        }
    }

    /**
     * Search the tree, starting at the root.
     *
     * @param key The node to search for.
     * @return The found node position
     */
    private Position<Entry<K, V>> treeSearch(K key) {
        return treeSearch(key, root());
    }


    /**
     * An auxiliary method used by get, put, and remove.
     *
     * @param key      The node to search for.
     * @param position The starting tree position
     * @return The found node position
     */
    protected Position<Entry<K, V>> treeSearch(K key, Position<Entry<K, V>> position) {
        if (isExternal(position)) {
            return position; // key not found; return external node
        }
        K curKey = key(position);
        int comp = comparator.compare(key, curKey);
        if (comp < 0) {
            return treeSearch(key, left(position)); // search left subtree
        } else if (comp > 0) {
            return treeSearch(key, right(position)); // search right subtree
        }
        return position; // return internal node where key is found
    }

    /**
     * Returns a value whose associated key is k.
     */
    public V get(K key) throws IllegalArgumentException {
        checkKey(key); // may throw an InvalidKeyException
        Position<Entry<K, V>> currentPos = treeSearch(key);
        actionPos = currentPos; // node where the search ended

        return isInternal(currentPos) ? value(currentPos) : null;
    }

    /**
     * Inserts an entry with a given key and value v into the map, returning
     * the old value whose associated key is key if it exists.
     */
    public V put(K key, V value) throws IllegalArgumentException {
        checkKey(key); // may throw an IllegalArgumentException
        Position<Entry<K, V>> insPos = treeSearch(key);
        BSTEntry<K, V> entry = new BSTEntry<>(key, value, insPos);
        actionPos = insPos; // node where the entry is being inserted
        if (isExternal(insPos)) { // we need a new node, key is new
            insertAtExternal(insPos, entry).getValue();
            return null;
        }

        return replaceEntry(insPos, entry); // key already exists
    }

    /**
     * Removes from the map the entry whose key is k, returning the value of
     * the removed entry.
     */
    public V remove(K key) throws IllegalArgumentException {
        checkKey(key); // may throw an IllegalArgumentException
        Position<Entry<K, V>> remPos = treeSearch(key);
        if (isExternal(remPos)) {
            return null; // key not found
        }
        Entry<K, V> toReturn = entry(remPos); // old entry
        if (isExternal(left(remPos))) {
            remPos = left(remPos); // left case
        } else if (isExternal(right(remPos))) {
            remPos = right(remPos); // right case
        } else { // entry is at a node with internal children
            Position<Entry<K, V>> swapPos = remPos; // find node for moving entry
            remPos = left(swapPos);
            do {
                remPos = right(remPos);
            } while (isInternal(remPos));
            replaceEntry(swapPos, parent(remPos).getElement());
        }
        actionPos = sibling(remPos); // sibling of the leaf to be removed
        removeExternal(remPos);

        return toReturn.getValue();
    }

    /**
     * Returns an iterable collection of the keys of all entries stored in the
     * map.
     */
    public Iterable<K> keySet() {

        List<K> keys = new ArrayList<>();

        for (Entry<K, V> entry : entrySet()) {
            keys.add(entry.getKey());
        }

        return keys;

    }

    /**
     * Returns an iterable collection of the values of all entries stored in
     * the map.
     */
    public Iterable<V> values() {

        List<V> values = new ArrayList<>();

        for (Entry<K, V> entry : entrySet()) {
            values.add(entry.getValue());
        }

        return values;
    }

    /**
     * Returns an iterable collection of all entries stored in the map. The sentinels are excluded.
     */
    public Iterable<Entry<K, V>> entrySet() {

        List<Entry<K, V>> entries = new ArrayList<>(size);

        for (Entry<K, V> entry : inorderElements()) {
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }


    /**
     * Returns a string representation of the BinarySearchTreeMap, providing a formatted list of key-value pairs.
     * The format is as follows: "(key1, value1), (key2, value2), ...".
     *
     * @return A string representing the key-value pairs in the map.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<K, V> entry : entrySet()) {

            sb.append("(").append(entry.getKey()).append(", ").append(entry.getValue()).append("), ");
        }
        if(!sb.isEmpty()){
            sb.setLength(sb.length()-2);
        }

        return sb.toString();
    }

    /**
     * Nested class for location-aware binary search tree entries
     */
    protected static class BSTEntry<K, V> implements Entry<K, V> {
        /**
         *  The key associated with this entry.
         */
        protected final K key;
        /**
         * The value associated with this entry.
         */
        protected final V value;
        /**
         * The position of this entry within the binary search tree.
         */
        protected Position<Entry<K, V>> position;

        /**
         * Constructs a BSTEntry with the given key, value, and position.
         *
         * @param key      The key for the entry.
         * @param value    The value associated with the key.
         * @param position The position of the entry in the binary search tree.
         */
        BSTEntry(K key, V value, Position<Entry<K, V>> position) {
            this.key = key;
            this.value = value;
            this.position = position;
        }

        /**
         * Returns the key associated with this entry.
         *
         * @return The key of the entry.
         */
        public K getKey() {
            return key;
        }

        /**
         * Returns the value associated with this entry.
         *
         * @return The value of the entry.
         */
        public V getValue() {
            return value;
        }

        /**
         * Returns the position of this entry in the binary search tree.
         *
         * @return The position of the entry.
         */
        public Position<Entry<K, V>> position() {
            return position;
        }
    }
}
