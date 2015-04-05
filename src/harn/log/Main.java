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
package harn.log;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.text.html.*;
import rpg.*;

/**
 * The main class for the log plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {
    /** GUI references */
    private JPanel myPanel;

    /** My properties */
    private Properties myProps;

    /** Path to my data */
    protected String myPath;

    /** Framework reference */
    private Framework myFrw;

    /** State object */
    private State myState;

    /** Button panel */
    private JPanel leftPanel;

    /** Buttons for logging options */
    protected JCheckBox bCalendar;
    protected JCheckBox bWeather;
    protected JCheckBox bLocs;
    protected JCheckBox bDate;
    protected JCheckBox bTime;


    /** Save */
    protected JButton bSave;

    /** Insert Date/Time */
    private JButton bInsert;

    /** Display of Log */
    protected JEditorPane display;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.5f; }

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

        myState = new State(this);

	// Create GUI
	createGUI();

        // Listeners
        myFrw.listen(myState, CharGroup.TYPE, null);
        myFrw.listen(myState, TimeFrame.TYPE, null);
        myFrw.listen(myState, TimeEvent.TYPE, null);
        myFrw.listen(myState, Weather.TYPE, null);
        myFrw.listen(myState, Location.TYPE, null);
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
     * Called by framework when terminating.
     * @param save whether to save or not
     */
    public void stop(boolean save) {
        if (save) {
            myState.save();
            bSave.setEnabled(false);
        }
    }

    /**
     * Called by the framework when about to terminate. We never save
     * anything. See above.
     * @return false
     */
    public boolean hasUnsavedParts() { return bSave.isEnabled(); }

    /**
     * Create (and show) GUI.
     */
    private void createGUI() {
        myPanel.setLayout(new GridBagLayout());

	// The left panel is filled with buttons
	leftPanel = new JPanel(new GridBagLayout());

        bWeather = new JCheckBox("Log Weather");
        bWeather.setSelected(!myState.containsType("Log Weather"));
        bLocs = new JCheckBox("Log Locations");
        bLocs.setSelected(!myState.containsType("Log Locations"));
        bCalendar = new JCheckBox("Log Events");
        bCalendar.setSelected(!myState.containsType("Log Events"));
        bDate = new JCheckBox("Log Date");
        bDate.setSelected(!myState.containsType("Log Date"));
        bTime = new JCheckBox("Log Time");
        bTime.setSelected(!myState.containsType("Log Time"));


	bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
        bSave.setEnabled(false);
	bSave.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    myState.save();
                    bSave.setEnabled(false);
		}
	    });
 
        bInsert = new JButton("Insert Date/Time");
        bInsert.setMnemonic(KeyEvent.VK_I);
        bInsert.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    myState.insertElement(null, true);
		}
	    });

        display = new JEditorPane();
	display.setEditable(true);
        display.setMargin(new Insets(10,10,10,10));

        GridBagConstraints constr2 = new GridBagConstraints();
	constr2.gridx = 0;
	constr2.weightx = 1;
	constr2.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(bWeather, constr2);
        leftPanel.add(bCalendar, constr2);
        leftPanel.add(bLocs, constr2);
        leftPanel.add(bDate, constr2);
        leftPanel.add(bTime, constr2);
        leftPanel.add(bInsert, constr2);
        leftPanel.add(bSave, constr2);

	// We stuff everything at the top by inserting an expanding dummy.
	JPanel dummy = new JPanel();
        GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = 0;
	constr3.gridy = GridBagConstraints.RELATIVE;
	constr3.fill = GridBagConstraints.BOTH;
	constr3.weighty = 1;
        leftPanel.add(dummy, constr3);

	GridBagConstraints constr4 = new GridBagConstraints();
	constr4.fill = GridBagConstraints.BOTH;
	constr4.weightx = constr4.weighty = 1;
        constr4.insets = new Insets(6,5,6,5);

	JScrollPane stree = new JScrollPane(display);
        stree.getVerticalScrollBar().setUnitIncrement(10);
        stree.getHorizontalScrollBar().setUnitIncrement(10);

        JSplitPane sp =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, stree);
        sp.setResizeWeight(.1);
        myPanel.add(sp, constr4);

    }

    /**
     * Each plugin must provide an array of objects it requires.
     * @return list of imports
     */
    public String[] requires() {
        return new String[] { CharGroup.TYPE, TimeFrame.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it uses.
     * @return list of imports
     */
    public String[] uses() {
	return new String[] { Location.TYPE, Weather.TYPE, TimeEvent.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() {
	return new String[0];
    }
}
