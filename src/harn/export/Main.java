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
package harn.export;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import rpg.*;

/**
 * The main class for the export plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {
    /** GUI references */
    JPanel myPanel;

    /** save button */
    JButton bSave;

    /** My properties */
    Properties myProps;

    /** Path to my data */
    String myPath;

    /** State object */
    State myState;

    /** Framework reference */
    Framework myFrw;

    /** List of all exportables */
    ArrayList exports;
    
    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.6f; }

    /**
     * Init method called by Framework. Init all objects and then create
     * GUI. Register listeners.
     * @param frw framework
     * @param panel my GUI root object
     * @param props my properties
     * @param path path to my directory
     */
    public void start(Framework frw, JPanel panel, Properties props, String path) {
	// Useful references
	myFrw = frw;
        myPanel = panel;
	myProps = props;
	myPath = path;

        // Backgroundtask
        exports = new ArrayList();

        Background task = new Background
            (exports, Integer.parseInt(props.getProperty("port")), this);
        
	// Create GUI
	createGUI();

        myState = new State(this);

        // Listeners
        for (int i = 0; i < exports.size(); i++)
            ((HttpExport)exports.get(i)).listen();

        bSave.setEnabled(false);
    }

    /**
     * Init edit method. Called by framework. Init all objects and then create
     * GUI. Register listeners.
     * @param frw framework
     * @param panel my GUI root object
     * @param props my properties
     * @param path path to my directory
     * @return whether this plugin suport editing at all
     */
    public boolean startEdit(Framework frw, JPanel frm, Properties props, String path) {
        return false;
    }

    /**
     * Called by framework when terminating. We never save anything. We'Re
     * stateless - the state object immediately externalizes all state
     * changes.
     * @param save whether to save or not
     */
    public void stop(boolean save) { if (save) myState.save(); }

    /**
     * Called by the framework when about to terminate. We never save
     * anything. See above.
     * @return false
     */
    public boolean hasUnsavedParts() { return myState.getDirty(); }

    /**
     * Create (and show) GUI.
     */
    private void createGUI() {
        myPanel.setLayout(new GridBagLayout());

	// Constraints for boxes
	GridBagConstraints constrBox = new GridBagConstraints();
        constrBox.fill = GridBagConstraints.BOTH;
        constrBox.weighty = 0;
        constrBox.gridy = 0;
        constrBox.insets = new Insets(1, 1, 1, 1);

        // Headers
        constrBox.gridx = 0;
        constrBox.weightx = .1;
        JComponent tmp = new JPanel();
        tmp.add(new JLabel("Export name"));
        tmp.setBackground(Color.lightGray);
        myPanel.add(tmp, constrBox);

        constrBox.gridx = 1;
        constrBox.weightx = 1;
        tmp = new JPanel();
        tmp.add(new JLabel("Description"));
        tmp.setBackground(Color.lightGray);
        myPanel.add(tmp, constrBox);

        constrBox.gridx = 2;
        constrBox.weightx = 0;
        bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
        bSave.setEnabled(false);
        bSave.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myState.save();
                }
            });

        myPanel.add(bSave, constrBox);

        String[] exp = myProps.getProperty("export").split(",");

        // Player map
        exports.add
            (new PlayerMapExport(this, ++constrBox.gridy, myPanel, exp));

        // Player information at index plugin
        exports.add
            (new IndexExport(this, ++constrBox.gridy, myPanel, exp));

        // Character html pages
        exports.add
            (new CharSheetsExport(this, ++constrBox.gridy, myPanel, exp));

        // Character html pages / combat
        exports.add
            (new CombatExport(this, ++constrBox.gridy, myPanel, exp));

        // Sketches
        exports.add
            (new SketchExport(this, ++constrBox.gridy, myPanel, exp));

        // Weather
        exports.add
            (new WeatherExport(this, ++constrBox.gridy, myPanel, exp));

        // Weather
        exports.add
            (new LogExport(this, ++constrBox.gridy, myPanel, exp));

        // Visual
        exports.add
            (new VisualExport(this, ++constrBox.gridy, myPanel, exp));

        // Sound (not working)
//         exports.add
//             (new SoundExport(this, ++constrBox.gridy, myPanel, exp));

        // Dummy
        constrBox.weighty = 1;
        constrBox.gridy++;
        myPanel.add(new JPanel(), constrBox);
    }

    /**
     * Each plugin must provide an array of objects it requires.
     * @return list of imports
     */
    public String[] requires() {
        return new String[0];
    }

    /**
     * Each plugin must provide an array of objects it uses.
     * @return list of imports
     */
    public String[] uses() {
	return new String[] { CharGroup.TYPE, Location.TYPE, ScaledMap.TYPE, Char.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() {
	return new String[0];
    }
}
