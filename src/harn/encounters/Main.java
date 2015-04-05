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
package harn.encounters;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import javax.swing.*;
import rpg.*;

/**
 * The main class for the encounter plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {
    /** Framework reference */
    Framework myFrw;

    /** GUI references */
    JPanel myPanel;

    /** My properties */
    Properties myProps;

    /** Path to my data */
    String myPath;

    /** Data */
    Data myData;

    /** Encounter Display */
    JEditorPane myDisplay;

    /** List of encounters */
    JScrollPane rightPanel;

    /** Group state object */
    State myState;

    /** GUI listing of events */
    Listing list;

    /** Value menu */
    ValuMenu pMenu;

    /** Times */
    static public final int HOUR = 60;
    static public final int WATCH = 4 * HOUR;
    static public final int DAY = 6 * WATCH;

    /** Left GUI items */
    JButton bSave;
    private JButton bRemove;
    private JButton bRemove1;
    private JButton bHide;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.4f; }

    /**
     * Init method. Called by framework. Init all objects and then create
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
	myPath = path + frw.SEP;
	myData = new Data(this, frw);

	// State object
	myState = new State(this);

	// create GUI
	createGUI();

        // Listeners
        frw.listen(myState, CharGroup.TYPE, null);
        frw.listen(myState, Weather.TYPE, null);
        frw.listen(myState, Atlas.TYPE, null);
        frw.listen(myState, TimeFrame.TYPE, null);
        frw.listen(myState, Location.TYPE, null);
        frw.listen(myState, Sketch.TYPE, null);
        frw.listen(pMenu, Sketch.TYPE, null);
        frw.listen(pMenu, Location.TYPE, null);
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
    public void stop(boolean save) { if (bSave.isEnabled()) myState.save(); }

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
	// The left panel is filled with buttons, some of which are disabled
	// and only show information. Default information seen below will be
	// replaced as soon as the appropriate objects, which contain such
	// values, become known.

	GridBagConstraints constr1;
	myPanel.setLayout(new GridBagLayout());

	constr1 = new GridBagConstraints();
	constr1.fill = GridBagConstraints.VERTICAL;
	constr1.insets = new Insets(5, 5, 5, 5);
	JPanel leftPanel = new JPanel(new GridBagLayout());

	bRemove = new JButton("Dump some events");
        bRemove.setMnemonic(KeyEvent.VK_D);
	bRemove.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    myState.dump(false);
		}
	    });
        bRemove.setToolTipText
            ("Remove encounters not on screen - if you don't need them");

	bRemove1 = new JButton("Dump all events");
        bRemove1.setMnemonic(KeyEvent.VK_A);
	bRemove1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    myState.dump(true);
		}
	    });
        bRemove1.setToolTipText("Remove all encounters - performs a reroll");

        bHide = new JButton("Hide Non-events");
        bHide.setMnemonic(KeyEvent.VK_H);
	bHide.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    list.toggle();
                    bHide.setText
                        ((list.hidePseudos ? "Show" : "Hide") +
                         " Non-events");
		}
	    });
        bHide.setToolTipText
            ("<html>Some watches have no events. But a time slot is still<br>" +
             "rolled and an event assigned. Show/Hide these \"Non-events\"</html>");
	bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
	bSave.setEnabled(false);
	bSave.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    myState.save();
		}
	    });

        GridBagConstraints constr2 = new GridBagConstraints();
	constr2.gridx = 0;
        constr2.weightx = 1;
	constr2.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(bSave, constr2);
        leftPanel.add(bRemove, constr2);
        leftPanel.add(bRemove1, constr2);
        leftPanel.add(bHide, constr2);

	myDisplay = new JEditorPane();
        myDisplay.setEditable(false);

        GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = 0;
	constr3.gridy = GridBagConstraints.RELATIVE;
	constr3.fill = GridBagConstraints.BOTH;
	constr3.weighty = 1;
        leftPanel.add(myDisplay, constr3);

	GridBagConstraints constr4 = new GridBagConstraints();
	constr4.fill = GridBagConstraints.BOTH;
	constr4.weightx = constr4.weighty = 1;
	constr4.insets = new Insets(6,5,6,5);

        GridBagConstraints constr5 =  new GridBagConstraints();
        constr5.fill = GridBagConstraints.BOTH;
        constr5.gridy = 0;
        constr5.weightx = 1;
        JPanel tmp = new JPanel(new GridBagLayout());
        list = new Listing(this);
        tmp.add(list, constr5);
        constr5.weighty = 1;
        constr5.gridy = 1;
        tmp.add(new JLabel(), constr5);

	rightPanel = new JScrollPane(tmp);
        rightPanel.getVerticalScrollBar().setUnitIncrement(10);
        rightPanel.getHorizontalScrollBar().setUnitIncrement(10);

	JSplitPane sp = new JSplitPane
            (JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
	myPanel.add(sp, constr4);

        pMenu = new ValuMenu(this);
    }

    /**
     * Each plugin must provide an array of objects it requires.
     * @return list of imports
     */
    public String[] requires() {
        return new String[] { TimeFrame.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it uses.
     * @return list of imports
     */
    public String[] uses() {
        return new String[] { CharGroup.TYPE, Weather.TYPE, Atlas.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() { return new String[] { TimeEvent.TYPE }; }
}
