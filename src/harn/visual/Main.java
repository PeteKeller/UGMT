/**
 * UGMT : Universal Gamemaster tool
 * Copyright (c) 2004 Michael Jung
 * miju@phantasia.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package harn.visual;

import harn.repository.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import rpg.*;

/**
 * The main class for the maps plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin, IListener {
    /** GUI references */
    JPanel myPanel;

    /** My properties */
    Properties myProps;

    /** Maps */
    private Hashtable maps;

    /** Active group */
    CharGroup act;

    /** Path to my data */
    static String myPath;

    /** Framework reference */
    Framework myFrw;

    /** Left (main) GUI items */
    private JSlider slideLR;
    private JSlider slideTD;
    private JSlider slideRotate;
    private JSlider slideTilt;
    private JSlider slideZoom;
    private JButton bReset;
    JCheckBox bAutoDetect;

    /** Right GUI items */
    private MapHolder map;

    /** Redo GL canvas */
    private boolean redo;

    /** Details on GUI */
    private JSpinner bScaleSpin;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.2f; }

    /**
     * Init method called by Framework. Init all objects and then create
     * GUI. Register listeners.
     * @param frw framework
     * @param panel my GUI root object
     * @param props my properties
     * @param path path to my directory
     */
    public void start(Framework frw, JPanel panel, Properties props, String path) {
        redo = true;
        maps = new Hashtable();

        // Useful references
        myFrw = frw;
        myPanel = panel;
        myProps = props;
        myPath = path;

        // Create GUI
        createGUI();

        myFrw.listen(this, ScaledMap.TYPE, null);
        myFrw.listen(map, Token.TYPE, null);
        myFrw.listen(this, CharGroup.TYPE, null);
    }

    public boolean startEdit(Framework frw, JPanel panel, Properties props, String path) { return false; }

    /**
     * Called by framework when terminating. We never save anything. We'Re
     * stateless - the state object immediately externalizes all state
     * changes.
     * @param save whether to save or not
     */
    public void stop(boolean save) {}

    /**
     * Called by the framework when about to terminate. We never save
     * anything. See above.
     * @return false
     */
    public boolean hasUnsavedParts() { return false; }

    /**
     * Create (and show) GUI.
     */
    private void createGUI() {
	GridBagConstraints constr1 = new GridBagConstraints();
	myPanel.setLayout(new GridBagLayout());
	constr1.gridx = 0;
	constr1.weightx = 1;
	constr1.anchor = GridBagConstraints.CENTER;
	constr1.fill = GridBagConstraints.HORIZONTAL;

        JPanel leftPanel = new JPanel(new GridBagLayout());

        bScaleSpin = new JSpinner(Framework.getScaleSpinnerModel(17));
        bScaleSpin.setValue(new Integer(100));
        ((JSpinner.DefaultEditor)bScaleSpin.getEditor()).getTextField().setEditable(false);

        bScaleSpin.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    map.setMSScale(((Integer)bScaleSpin.getValue()).intValue());
                }
            });

        slideLR = new JSlider(JSlider.HORIZONTAL);
        slideLR.setToolTipText("Move terrain left or right");
        TitledBorder b = BorderFactory.createTitledBorder("Left - Right");
        b.setTitleJustification(TitledBorder.CENTER);
        slideLR.setBorder(b);
        slideLR.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    map.movx = map.max_size * (slideLR.getValue()-50)/100f;
                    if (redo) map.redo(false);
                }
            });

        slideRotate = new JSlider(JSlider.HORIZONTAL);
        slideRotate.setToolTipText("Rotate terrain");
        b = BorderFactory.createTitledBorder("Rotate");
        b.setTitleJustification(TitledBorder.CENTER);
        slideRotate.setBorder(b);
        slideRotate.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    map.rotate = 180 * (slideRotate.getValue() - 50)/50f;
                    if (redo) map.redo(false);
                }
            });

        slideZoom = new JSlider(JSlider.HORIZONTAL);
        slideZoom.setToolTipText("Zoom in and out");
        b = BorderFactory.createTitledBorder("Zoom");
        b.setTitleJustification(TitledBorder.CENTER);
        slideZoom.setBorder(b);
        slideZoom.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    map.zoom = map.max_size * (100-slideZoom.getValue())/100f;
                    if (redo) map.redo(false);
                }
            });

        slideTD = new JSlider(JSlider.HORIZONTAL);
        slideTD.setToolTipText("Move terrain up or down");
        b = BorderFactory.createTitledBorder("Up - Down");
        b.setTitleJustification(TitledBorder.CENTER);
        slideTD.setBorder(b);
        slideTD.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    map.movy = map.max_size * (50-slideTD.getValue())/100f;
                    if (redo) map.redo(false);
                }
            });

        slideTilt = new JSlider(JSlider.HORIZONTAL);
        slideTilt.setToolTipText("Tilt terrain");
        b = BorderFactory.createTitledBorder("Tilt");
        b.setTitleJustification(TitledBorder.CENTER);
        slideTilt.setBorder(b);
        slideTilt.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    map.tilt = 1.79f * (slideTilt.getValue() - 50);
                    if (redo) map.redo(false);
                }
            });

        bReset = new JButton("Reset");
        bReset.setMnemonic(KeyEvent.VK_R);
        bReset.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    redo = false;
                    slideLR.setValue(50);
                    slideRotate.setValue(50);
                    slideZoom.setValue(50);
                    slideTD.setValue(50);
                    slideTilt.setValue(50);
                    map.redo(false);
                    redo = true;
                }
            });
        bAutoDetect = new JCheckBox("Autodetect Elevation", true);
        bAutoDetect.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    map.redo(true);
                }
            });
        

        leftPanel.add(slideLR, constr1);
        leftPanel.add(slideRotate, constr1);
        leftPanel.add(slideZoom, constr1);
        leftPanel.add(slideTD, constr1);
        leftPanel.add(slideTilt, constr1);
        leftPanel.add(bReset, constr1);
        leftPanel.add(bAutoDetect, constr1);

	constr1.fill = GridBagConstraints.BOTH;
	constr1.weighty = 1;
        leftPanel.add(new JPanel(), constr1);
	constr1.fill = GridBagConstraints.HORIZONTAL;
	constr1.weighty = 0;
	leftPanel.add(bScaleSpin, constr1);

        map = new MapHolder(this);
	int w = Integer.parseInt(myProps.getProperty("display.width"));
	int h = Integer.parseInt(myProps.getProperty("display.height"));
        map.setPreferredSize(new Dimension(w, h));
        
	GridBagConstraints constr2 = new GridBagConstraints();
	constr2.fill = GridBagConstraints.BOTH;
	constr2.weightx = constr2.weighty = 1;
        JSplitPane sp =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, map);
	myPanel.add(sp, constr2);
    }

    /**
     * Export method.
     */
    public BufferedImage getImage() { return map.getImage(); }

    /**
     * Each plugin must provide an array of objects it requires.
     * @return list of imports
     */
    public String[] requires() {
	return new String[] { CharGroup.TYPE, ScaledMap.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it uses.
     * @return list of imports
     */
    public String[] uses() {
        return new String[] {};
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() {
	return new String[] {};
    }

    /**
     * Listen to groups and maps.
     * @param obj the location
     */
    public void inform(IExport obj) {
        if (obj instanceof ScaledMap)
            maps.put(((ScaledMap)obj).getName(), obj);

        if (obj instanceof CharGroup) {
            CharGroup cg = (CharGroup) obj;
            if (cg.isActive()) {
                act = cg;
            }
        }
        if (act != null) {
            ScaledMap actmap = (ScaledMap)maps.get(act.getMap());
            if (actmap != null && (obj == act || obj == actmap)) {
                map.ppl = actmap.getScale();
                map.max_hght = Integer.parseInt
                    (myProps.getProperty("terrain.height"));
                map.texture = actmap.getBufferedImage(true, false);
                map.heightfield = actmap.getHeightField();
                if (actmap.getHeight() > 0)
                    map.max_hght =
                        (int)(actmap.getHeight() * map.ppl *
                              map.max_size / map.texture.getWidth() / 13200);
                map.redo(true);
            }
        }
    }
}
