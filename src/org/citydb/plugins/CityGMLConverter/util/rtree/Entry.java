package org.citydb.plugins.CityGMLConverter.util.rtree;

import com.vividsolutions.jts.geom.Envelope;

import org.geotools.index.Data;



/**
 * DOCUMENT ME!
 *
 * @author Tommaso Nolli
 * @source $URL$
 */
public class Entry implements Cloneable {
    private Envelope bounds;
    private Object data;
    private EntryBoundsChangeListener listener;

    public Entry(Envelope e, Object data) {
        this.bounds = e;
        this.data = data;
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public Envelope getBounds() {
        return bounds;
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public Object getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        Entry e = (Entry) obj;

        return this.bounds.equals(e.getBounds())
        && this.data.equals(e.getData());
    }

    /**
     * DOCUMENT ME!
     *
     * @param envelope
     */
    void setBounds(Envelope envelope) {
        bounds = envelope;

        if (this.listener != null) {
            this.listener.boundsChanged(this);
        }
    }

    /**
     * @see java.lang.Object#clone()
     */
    protected Object clone() {
        Entry ret = new Entry(new Envelope(this.bounds), this.data);
        ret.setListener(this.listener);

        return ret;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Entry --> " + this.bounds + " - key: " + this.data;
    }

    /**
     * DOCUMENT ME!
     *
     * @param listener
     */
    public void setListener(EntryBoundsChangeListener listener) {
        this.listener = listener;
    }
}