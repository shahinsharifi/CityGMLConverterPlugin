package org.citydb.plugins.CityGMLConverter.util.rtree;

import java.util.ArrayList;

import org.geotools.index.Data;
import org.geotools.index.DataDefinition;
import org.geotools.index.TreeException;

public class CityObjectData extends Data{
	
	private ArrayList values;
	private DataDefinition def;

	
	public CityObjectData(DataDefinition def) {
		super(def);
		this.def = def;
		this.values = new ArrayList(100);
	}

	@Override
	public Data addValue(Object val) throws TreeException {
		if (this.values.size() == def.getFieldsCount()) {
			throw new TreeException("Max number of values reached!");
		}
		int pos = this.values.size();
	/*	if (!val.getClass().equals(def.getField(pos).getFieldClass())) {
			throw new TreeException("Wrong class type, was expecting "
					+ def.getField(pos).getFieldClass());
		}*/
		this.values.add(val);
		return this;
	}

	@Override
	public DataDefinition getDefinition() {
		return this.def;
	}

	@Override
	public Object getValue(int i) {
		return this.values.get(i);
	}

	@Override
	public int getValuesCount() {
		// TODO Auto-generated method stub
		return this.values.size();
	}

	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < this.values.size(); i++) {
		if (i > 0) {
		ret.append(" - ");
		}
		ret.append(this.def.getField(i).getFieldClass());
		ret.append(": ");
		ret.append(this.values.get(i));
		}
		return ret.toString();
	}
	

}
