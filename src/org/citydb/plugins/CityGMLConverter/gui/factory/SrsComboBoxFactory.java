/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2013
 * Institute for Geodesy and Geoinformation Science
 * Technische Universitaet Berlin, Germany
 * http://www.gis.tu-berlin.de/
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see 
 * <http://www.gnu.org/licenses/>.
 * 
 * The development of the 3D City Database Importer/Exporter has 
 * been financially supported by the following cooperation partners:
 * 
 * Business Location Center, Berlin <http://www.businesslocationcenter.de/>
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * Berlin Senate of Business, Technology and Women <http://www.berlin.de/sen/wtf/>
 */
package org.citydb.plugins.CityGMLConverter.gui.factory;

import java.awt.Component;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.citydb.api.database.DatabaseSrs;
import org.citydb.api.event.Event;
import org.citydb.api.event.EventHandler;
import org.citydb.api.event.global.GlobalEvents;
import org.citydb.api.gui.DatabaseSrsComboBox;
import org.citydb.api.registry.ObjectRegistry;
import org.citydb.config.Config;
import org.citydb.plugins.CityGMLConverter.config.ConfigImpl;
import org.citydb.plugins.CityGMLConverter.util.Util;





public class SrsComboBoxFactory {
	private static SrsComboBoxFactory instance = null;
	private final DatabaseSrs dbRefSys;
	private final List<WeakReference<SrsComboBox>> srsBoxes = new ArrayList<WeakReference<SrsComboBox>>();
	private final Config config;

	private SrsComboBoxFactory(Config config) {
		// just to thwart instantiation
		this.config = config;
		dbRefSys = DatabaseSrs.createDefaultSrs();
		dbRefSys.setSupported(true);
	}

	public static synchronized SrsComboBoxFactory getInstance(Config config) {
		if (instance == null) {
			instance = new SrsComboBoxFactory(config);
			instance.resetAll(true);
		}

		return instance;
	}

	public SrsComboBox createSrsComboBox(boolean onlyShowSupported) {
		SrsComboBox srsBox = new SrsComboBox(onlyShowSupported);
		srsBox.init();

		WeakReference<SrsComboBox> ref = new WeakReference<SrsComboBox>(srsBox);
		srsBoxes.add(ref);

		return srsBox;
	}

	public void updateAll(boolean sort) {
		processSrsComboBoxes(sort, true);
	}

	public void resetAll(boolean sort) {
		// by default, any reference system is not supported. In GUI mode we can
		// override this because the SRS combo boxes will take care.
		//	for (DatabaseSrs refSys : config.getProject().getDatabase().getReferenceSystems())
		//		refSys.setSupported(true);

		//	processSrsComboBoxes(sort, false);
	}

	private void processSrsComboBoxes(boolean sort, boolean update) {
		//if (sort)
		//	Collections.sort(config.getProject().getDatabase().getReferenceSystems());

		Iterator<WeakReference<SrsComboBox>> iter = srsBoxes.iterator();
		while (iter.hasNext()) {
			WeakReference<SrsComboBox> ref = iter.next();
			SrsComboBox srsBox = ref.get();

			if (srsBox == null) {
				iter.remove();
				continue;
			}

			if (update)
				srsBox.updateContent();
			else
				srsBox.reset();

			srsBox.repaint();
		}
	}

	@SuppressWarnings("serial")
	public class SrsComboBox extends DatabaseSrsComboBox implements EventHandler {
		private boolean showOnlySupported;
		private boolean showOnlySameDimension;

		private SrsComboBox(boolean onlyShowSupported) {
			this.showOnlySupported = onlyShowSupported;
			setRenderer(new SrsComboBoxRenderer(this, getRenderer()));

			ObjectRegistry.getInstance().getEventDispatcher().addEventHandler(GlobalEvents.SWITCH_LOCALE, this);
		}

		@Override
		public void setSelectedItem(Object anObject) {
			if (anObject instanceof DatabaseSrs) {
				DatabaseSrs refSys = (DatabaseSrs)anObject;

				DatabaseSrs cand = null;

				for (int i = 0; i < getItemCount(); i++) {
					DatabaseSrs item = getItemAt(i);
					if (item != null) {
						if (item.getId().equals(refSys.getId())) {
							super.setSelectedItem(item);
							cand = null;
							break;
						} else if (cand == null && item.getSrid() == refSys.getSrid())
							cand = refSys;
					}
				}

				if (cand != null)
					super.setSelectedItem(cand);

			}
		}

		@Override
		public void setShowOnlySupported(boolean show) {
			showOnlySupported = show;
		}

		@Override
		public void setShowOnlySameDimension(boolean show) {
			showOnlySameDimension = show;
		}

		public void setDBReferenceSystem() {
			setSelectedItem(dbRefSys);
		}

		public boolean isDBReferenceSystemSelected() {
			return getSelectedItem() == dbRefSys;
		}

		private void init() {

			DatabaseSrs srsWGS = DatabaseSrs.createDefaultSrs(); 
			srsWGS.setSrid(4326);
			srsWGS.setDescription("[Default] WGS84");

			
			addItem(dbRefSys);
			addItem(srsWGS);
		}

		private void reset() {
			removeAllItems();
			init();
		}

		private void updateContent() {
			DatabaseSrs selectedItem = getSelectedItem();
			if (selectedItem == null)
				selectedItem = dbRefSys;

			reset();
			setSelectedItem(selectedItem);
		}

		private void doTranslation() {
			dbRefSys.setDescription(Util.I18N.getString("common.label.boundingBox.crs.sameAsInSourceFile"));
			DatabaseSrs selectedItem = getSelectedItem();
			if (selectedItem == null)
				selectedItem = dbRefSys;

			removeItemAt(0);
			insertItemAt(dbRefSys, 0);

			if (selectedItem == dbRefSys)
				setSelectedItem(selectedItem);

			repaint();
			fireActionEvent();
		}

		@Override
		public void handleEvent(Event event) throws Exception {
			doTranslation();
		}
	}

	private class SrsComboBoxRenderer implements ListCellRenderer {
		final SrsComboBox box;
		final ListCellRenderer renderer;

		public SrsComboBoxRenderer(SrsComboBox box, ListCellRenderer renderer) {
			this.box = box;
			this.renderer = renderer;
		}

		@Override
		public Component getListCellRendererComponent(JList list, 
				Object value, 
				int index, 
				boolean isSelected, 
				boolean cellHasFocus) {
			Component c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value != null)
				box.setToolTipText(value.toString());

			return c;
		}
	}
}
