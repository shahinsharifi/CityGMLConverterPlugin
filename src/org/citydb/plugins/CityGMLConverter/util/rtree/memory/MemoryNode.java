package org.citydb.plugins.CityGMLConverter.util.rtree.memory;

import org.citydb.plugins.CityGMLConverter.util.rtree.Entry;
import org.citydb.plugins.CityGMLConverter.util.rtree.Node;
import org.geotools.index.TreeException;



/**
 * DOCUMENT ME!
 *
 * @author Tommaso Nolli
 * @source $URL$
 */
public class MemoryNode extends Node {
    private Node parent;

    /**
     * DOCUMENT ME!
     *
     * @param maxNodeEntries
     */
    public MemoryNode(int maxNodeEntries) {
        super(maxNodeEntries);
    }

    /**
     * @see org.geotools.index.rtree.Node#getParent()
     */
    public Node getParent() throws TreeException {
        return this.parent;
    }

    /**
     * @see org.geotools.index.rtree.Node#setParent(org.geotools.index.rtree.Node)
     */
    public void setParent(Node node) {
        this.parent = node;
    }

    /**
     * @see org.geotools.index.rtree.Node#getEntry(org.geotools.index.rtree.Node)
     */
    protected Entry getEntry(Node node) {
        Entry ret = null;
        Node n = null;

        for (int i = 0; i < this.entries.length; i++) {
            n = (Node) this.entries[i].getData();

            if (n == node) {
                ret = this.entries[i];

                break;
            }
        }

        return ret;
    }

    /**
     * @see org.geotools.index.rtree.Node#doSave()
     */
    protected void doSave() throws TreeException {
        // does nothing....
    }
}