package org.citydb.plugins.CityGMLConverter.util.rtree.memory;

import org.citydb.plugins.CityGMLConverter.util.rtree.Entry;
import org.citydb.plugins.CityGMLConverter.util.rtree.Node;
import org.citydb.plugins.CityGMLConverter.util.rtree.PageStore;
import org.geotools.index.DataDefinition;
import org.geotools.index.LockManager;
import org.geotools.index.TreeException;



/**
 * DOCUMENT ME!
 *
 * @author Tommaso Nolli
 * @source $URL$
 */
public class MemoryPageStore extends PageStore {
    private static final int DEF_MAX = 50;
    private static final int DEF_MIN = 25;
    private static final short DEF_SPLIT = SPLIT_QUADRATIC;
    private LockManager lockManager = new LockManager();
    private Node root = null;

    public MemoryPageStore(DataDefinition def) throws TreeException {
        this(def, DEF_MAX, DEF_MIN, DEF_SPLIT);
    }

    public MemoryPageStore(DataDefinition def, int max, int min, short split)
        throws TreeException {
        super(def, max, min, split);

        this.root = new MemoryNode(max);
        this.root.setLeaf(true);
    }

    /**
     * @see org.geotools.index.rtree.PageStore#getRoot()
     */
    public Node getRoot() {
        return this.root;
    }

    /**
     * @see org.geotools.index.rtree.PageStore#setRoot(org.geotools.index.rtree.Node)
     */
    public void setRoot(Node node) throws TreeException {
        this.root = node;
        this.root.setParent(null);
    }

    /**
     * @see org.geotools.index.rtree.PageStore#getEmptyNode(boolean)
     */
    public Node getEmptyNode(boolean isLeaf) {
        MemoryNode ret = new MemoryNode(this.maxNodeEntries);
        ret.setLeaf(isLeaf);

        return ret;
    }

    /**
     * @see org.geotools.index.rtree.PageStore#getNode(org.geotools.index.rtree.Entry,
     *      org.geotools.index.rtree.Node)
     */
    public Node getNode(Entry parentEntry, Node parent)
        throws TreeException {
        Node ret = (Node) parentEntry.getData();
        ret.setParent(parent);

        return ret;
    }

    /**
     * @see org.geotools.index.rtree.PageStore#createEntryPointingNode(org.geotools.index.rtree.Node)
     */
    public Entry createEntryPointingNode(Node node) {
        return new Entry(node.getBounds(), node);
    }

    /**
     * @see org.geotools.index.rtree.PageStore#free(org.geotools.index.rtree.Node)
     */
    public void free(Node node) {
        // Does nothing
    }

    /**
     * @see org.geotools.index.rtree.PageStore#close()
     */
    public void close() throws TreeException {
        this.root = null;
    }
}