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
package org.citydb.plugins.CityGMLConverter.gui.preferences;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.citydb.config.project.general.PathMode;
import org.citydb.gui.factory.PopupMenuDecorator;
import org.citydb.plugins.CityGMLConverter.CityKMLExportPlugin;
import org.citydb.plugins.CityGMLConverter.config.Balloon;
import org.citydb.plugins.CityGMLConverter.config.BalloonContentMode;
import org.citydb.plugins.CityGMLConverter.util.Util;





@SuppressWarnings("serial")
public class WaterBodyBalloonPanel extends AbstractPreferencesComponent {

	protected static final int BORDER_THICKNESS = 5;
	protected static final int MAX_TEXTFIELD_HEIGHT = 20;

	private JCheckBox includeDescription = new JCheckBox();
	private JPanel contentSourcePanel;
	private JRadioButton genAttribRadioButton = new JRadioButton("");
	private JRadioButton fileRadioButton = new JRadioButton("");
	private JRadioButton genAttribAndFileRadioButton = new JRadioButton("");
	private JTextField browseText = new JTextField("");
	private JButton browseButton = new JButton("");
	private JCheckBox contentInSeparateFile = new JCheckBox();
	private JLabel warningLabel = new JLabel();

	private Balloon UtilBalloon = new Balloon();

	public WaterBodyBalloonPanel(CityKMLExportPlugin plugin) {
		super(plugin);
		initGui();
	}

	private Balloon getConfigBalloon() {
		return plugin.getConfig().getWaterBodyBalloon();
	}

	@Override
	public String getTitle() {
		return Util.I18N.getString("pref.tree.kmlExport.waterBodyBalloon");
	}

	@Override
	public boolean isModified() {
		setUtilBalloonValues();
		if (!getConfigBalloon().equals(UtilBalloon)) return true;
		return false;
	}

	private void initGui() {
		setLayout(new GridBagLayout());

		ButtonGroup contentSourceRadioGroup = new ButtonGroup();
		contentSourceRadioGroup.add(genAttribRadioButton);
		contentSourceRadioGroup.add(fileRadioButton);
		contentSourceRadioGroup.add(genAttribAndFileRadioButton);

		includeDescription.setIconTextGap(10);
		add(includeDescription, Util.setConstraints(0,1,2,1,1.0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,0,BORDER_THICKNESS,0));

		contentSourcePanel = new JPanel();
		contentSourcePanel.setLayout(new GridBagLayout());
		contentSourcePanel.setBorder(BorderFactory.createTitledBorder(""));
		add(contentSourcePanel, Util.setConstraints(0,2,2,1,1.0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,0,BORDER_THICKNESS,0));

		genAttribRadioButton.setIconTextGap(10);
		fileRadioButton.setIconTextGap(10);
		genAttribAndFileRadioButton.setIconTextGap(10);

		GridBagConstraints garb = Util.setConstraints(0,0,0.0,1.0,GridBagConstraints.BOTH,0,BORDER_THICKNESS,0,BORDER_THICKNESS);
		garb.gridwidth = 2;
		contentSourcePanel.add(genAttribRadioButton, garb);
		GridBagConstraints frb = Util.setConstraints(0,1,0.0,1.0,GridBagConstraints.BOTH,0,BORDER_THICKNESS,0,BORDER_THICKNESS);
		frb.gridwidth = 2;
		contentSourcePanel.add(fileRadioButton, frb);
		browseText.setPreferredSize(browseText.getSize());
		contentSourcePanel.add(browseText, Util.setConstraints(0,2,1.0,1.0,GridBagConstraints.BOTH,0,BORDER_THICKNESS * 6,0,0));
		contentSourcePanel.add(browseButton, Util.setConstraints(1,2,0.0,1.0,GridBagConstraints.BOTH,0,BORDER_THICKNESS * 2,0,BORDER_THICKNESS));
		GridBagConstraints gaafrb = Util.setConstraints(0,3,0.0,1.0,GridBagConstraints.BOTH,BORDER_THICKNESS,BORDER_THICKNESS,0,BORDER_THICKNESS);
		gaafrb.gridwidth = 2;
		contentSourcePanel.add(genAttribAndFileRadioButton, gaafrb);

		contentInSeparateFile.setIconTextGap(10);
		add(contentInSeparateFile, Util.setConstraints(0,3,2,1,1.0,0.0,GridBagConstraints.BOTH,BORDER_THICKNESS,0,0,0));
		add(warningLabel, Util.setConstraints(0,4,2,1,1.0,0.0,GridBagConstraints.BOTH,0,BORDER_THICKNESS * 6,0,0));

		PopupMenuDecorator.getInstance().decorate(browseText);

		includeDescription.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEnabledComponents();
			}
		});

		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadFile();
			}
		});

		genAttribRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEnabledComponents();
			}
		});

		fileRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEnabledComponents();
			}
		});

		genAttribAndFileRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEnabledComponents();
			}
		});
	}

	@Override
	public void doTranslation() {
		((TitledBorder)contentSourcePanel.getBorder()).setTitle(Util.I18N.getString("pref.kmlexport.balloon.contentSource.border"));	

		includeDescription.setText(Util.I18N.getString("pref.kmlexport.balloon.label.includeDescription"));
		genAttribRadioButton.setText(Util.I18N.getString("pref.kmlexport.balloon.label.genAttrib"));
		fileRadioButton.setText(Util.I18N.getString("pref.kmlexport.balloon.label.file"));
		genAttribAndFileRadioButton.setText(Util.I18N.getString("pref.kmlexport.balloon.label.genAttribAndFile"));
		browseButton.setText(Util.I18N.getString("common.button.browse"));
		contentInSeparateFile.setText(Util.I18N.getString("pref.kmlexport.balloon.label.contentInSeparateFile"));
		warningLabel.setText(Util.I18N.getString("pref.kmlexport.balloon.label.warningLabel"));
	}

	@Override
	public void loadSettings() {
		Balloon configBalloon = getConfigBalloon();
		copyBalloonContents(configBalloon, UtilBalloon);

		includeDescription.setSelected(configBalloon.isIncludeDescription());
		switch (configBalloon.getBalloonContentMode()) {
			case GEN_ATTRIB:
				genAttribRadioButton.setSelected(true);
				break;
			case FILE:
				fileRadioButton.setSelected(true);
				break;
			case GEN_ATTRIB_AND_FILE:
				genAttribAndFileRadioButton.setSelected(true);
				break;
		}
		browseText.setText(configBalloon.getBalloonContentTemplateFile());
		contentInSeparateFile.setSelected(configBalloon.isBalloonContentInSeparateFile());
		setEnabledComponents();
	}

	private void setUtilBalloonValues() {
		UtilBalloon.setIncludeDescription(includeDescription.isSelected());
		if (genAttribRadioButton.isSelected()) {
			UtilBalloon.setBalloonContentMode(BalloonContentMode.GEN_ATTRIB);
		}
		else if (fileRadioButton.isSelected()) {
			UtilBalloon.setBalloonContentMode(BalloonContentMode.FILE);
		}
		else if (genAttribAndFileRadioButton.isSelected()) {
			UtilBalloon.setBalloonContentMode(BalloonContentMode.GEN_ATTRIB_AND_FILE);
		}
		UtilBalloon.getBalloonContentPath().setLastUsedPath(browseText.getText().trim());
		UtilBalloon.setBalloonContentTemplateFile(browseText.getText().trim());
		UtilBalloon.setBalloonContentInSeparateFile(contentInSeparateFile.isSelected());
	}

	@Override
	public void setSettings() {
		setUtilBalloonValues();
		Balloon configBalloon = getConfigBalloon();
		copyBalloonContents(UtilBalloon, configBalloon);
	}

	private void loadFile() {
		JFileChooser fileChooser = new JFileChooser();

		FileNameExtensionFilter filter = new FileNameExtensionFilter("HTML Files (*.htm, *.html)", "htm", "html");
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.addChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.setFileFilter(filter);

		if (UtilBalloon.getBalloonContentPath().isSetLastUsedMode()) {
			fileChooser.setCurrentDirectory(new File(UtilBalloon.getBalloonContentPath().getLastUsedPath()));
		} else {
			fileChooser.setCurrentDirectory(new File(UtilBalloon.getBalloonContentPath().getStandardPath()));
		}
		int result = fileChooser.showSaveDialog(getTopLevelAncestor());
		if (result == JFileChooser.CANCEL_OPTION) return;
		try {
			String exportString = fileChooser.getSelectedFile().toString();
			browseText.setText(exportString);
			UtilBalloon.getBalloonContentPath().setLastUsedPath(fileChooser.getCurrentDirectory().getAbsolutePath());
			UtilBalloon.getBalloonContentPath().setPathMode(PathMode.LASTUSED);
		}
		catch (Exception e) {
			//
		}
	}

	private void setEnabledComponents() {
		genAttribRadioButton.setEnabled(includeDescription.isSelected());
		fileRadioButton.setEnabled(includeDescription.isSelected());
		genAttribAndFileRadioButton.setEnabled(includeDescription.isSelected());
		contentInSeparateFile.setEnabled(includeDescription.isSelected());
		warningLabel.setEnabled(includeDescription.isSelected());
		
		boolean browseEnabled = (fileRadioButton.isEnabled() && fileRadioButton.isSelected()) ||
								(genAttribAndFileRadioButton.isEnabled() && genAttribAndFileRadioButton.isSelected());

		browseText.setEnabled(browseEnabled);
		browseButton.setEnabled(browseEnabled);
	}

	private void copyBalloonContents(Balloon sourceBalloon, Balloon targetBalloon) {
		targetBalloon.setBalloonContentInSeparateFile(sourceBalloon.isBalloonContentInSeparateFile());
		targetBalloon.setBalloonContentMode(sourceBalloon.getBalloonContentMode());
		targetBalloon.setBalloonContentTemplateFile(sourceBalloon.getBalloonContentTemplateFile());
		targetBalloon.setIncludeDescription(sourceBalloon.isIncludeDescription());
		targetBalloon.getBalloonContentPath().setPathMode(sourceBalloon.getBalloonContentPath().getPathMode());
		targetBalloon.getBalloonContentPath().setLastUsedPath(sourceBalloon.getBalloonContentPath().getLastUsedPath());
		targetBalloon.getBalloonContentPath().setStandardPath(sourceBalloon.getBalloonContentPath().getStandardPath());
	}
	
}
