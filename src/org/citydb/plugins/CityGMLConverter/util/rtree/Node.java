package org.citydb.plugins.CityGMLConverter.util.rtree;


import com.vividsolutions.jts.geom.Envelope;

import org.geotools.index.TreeException;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


/**
 * DOCUMENT ME!
 *
 * @author Tommaso Nolli
 * @source $URL$
 */
public abstract class Node implements EntryBoundsChangeListener {
    private boolean leaf;
    protected int entriesCount = 0;
    protected int maxNodeEntries;
    protected Envelope bounds;
    protected Entry[] entries;
    protected boolean isChanged;

    public Node(int maxNodeEntries) {
        this.maxNodeEntries = maxNodeEntries;
        this.entries = new Entry[maxNodeEntries + 1];
        this.bounds = null;
    }

    /**
     * Adds an <code>Entry</code> to this <code>Node</code>
     *
     * @param entry
     */
    public final void addEntry(Entry entry) {
        this.entries[this.entriesCount++] = entry;
        entry.setListener(this);

        if (this.bounds == null) {
            this.bounds = new Envelope(entry.getBounds());
        } else {
            this.bounds.expandToInclude(entry.getBounds());
        }

        this.isChanged = true;
    }

    /**
     * Removes an <code>Entry</code> from this <code>Node</code>
     *
     * @param entry The <code>Entry</code> to remove
     */
    public final void removeEntry(Entry entry) {
        Entry[] newEntries = new Entry[this.entries.length];
        Envelope newBounds = null;
        int newSize = 0;

        for (int i = 0; i < this.entriesCount; i++) {
            if (!this.entries[i].equals(entry)) {
                newEntries[newSize++] = this.entries[i];

                if (newBounds == null) {
                    newBounds = new Envelope(this.entries[i].getBounds());
                } else {
                    newBounds.expandToInclude(this.entries[i].getBounds());
                }
            }
        }

        this.entries = newEntries;
        this.entriesCount = newSize;
        this.bounds = newBounds;
        this.isChanged = true;
    }

    /**
     * Removes all <code>Entry</code>s from this <code>Node</code>
     */
    public void clear() {
        Arrays.fill(this.entries, null);
        this.entriesCount = 0;
        this.bounds = null;
        this.isChanged = true;
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean isLeaf() {
        return leaf;
    }

    /**
     * DOCUMENT ME!
     *
     * @param b
     */
    public void setLeaf(boolean b) {
        leaf = b;
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public int getEntriesCount() {
        return this.entriesCount;
    }

    /**
     * Gets the n<i>th</i> Element
     *
     * @param n
     *
     * @return
     */
    public Entry getEntry(int n) {
        return (Entry) this.entries[n];
    }

    public Collection getEntries() {
        ArrayList ret = new ArrayList(this.entriesCount);

        for (int i = 0; i < this.entriesCount; i++) {
            ret.add(this.entries[i].clone());
        }

        return ret;
    }

    /**
     * DOCUMENT ME!
     *
     * @return The bounds
     */
    public Envelope getBounds() {
        return bounds;
    }

    /**
     * @see org.geotools.index.rtree.EntryBoundsChangeListener#boundsChanged(org.geotools.index.rtree.Entry)
     */
    public void boundsChanged(Entry e) {
        this.bounds = new Envelope(this.entries[0].getBounds());

        for (int i = 1; i < this.entriesCount; i++) {
            this.bounds.expandToInclude(this.entries[i].getBounds());
        }
    }

    /**
     * Saves this <code>Node</code>; this method calls doSave()
     *
     * @throws TreeException
     */
    public final void save() throws TreeException {
        this.doSave();
        this.isChanged = false;
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     *
     * @throws TreeException DOCUMENT ME!
     */
    public abstract Node getParent() throws TreeException;

    /**
     * Sets the parent of this <code>Node</code>
     *
     * @param node The parent <code>Node</code>
     */
    public abstract void setParent(Node node);

    /**
     * Returns the Entry pointing the specified <code>Node</code>
     *
     * @param node The <code>Node</code>
     *
     * @return The <code>Entry</code>
     */
    protected abstract Entry getEntry(Node node);

    /**
     * Saves this <code>Node</code>; called from save()
     *
     * @throws TreeException
     */
    protected abstract void doSave() throws TreeException;
}
