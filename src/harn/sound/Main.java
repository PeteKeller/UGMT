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
package harn.sound;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import rpg.*;
import javax.sound.sampled.*;

/**
 * The main class for the sound plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {
    /** GUI references */
    JPanel myPanel;

    /** My properties */
    Properties myProps;

    /** Framework reference */
    Framework myFrw;

    /** Path to my data */
    String myPath;

    /** Path to my sound data */
    static String mySoundPath;

    /** Group state object */
    private State state;

    /** My data */
    Data myData;

    /** Left GUI items */
    JButton bDate;
    JButton bAdd;
    JButton bAddCF;
    JButton bSave;

    /** Right GUI items */
    JScrollPane rightPanel;
    LineMixer mixer;

    /** Sound bank proxy */
    private MySounds sounds;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.6f; }

    /**
     * Return bits per second
     */
    public AudioInputStream getExportStream() { return null; }

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
        myPanel = panel;
	myProps = props;
	myPath = path + frw.SEP;
	mySoundPath = path + frw.SEP + "sounds" + frw.SEP;
	myData = new Data(this, frw);
        myFrw = frw;

	// Create GUI
	createGUI();

	// State object
	state = new State(this);
        sounds = new MySounds();

	// Register listeners
	frw.listen(mixer.ctrl, TimeEvent.TYPE, null);
	frw.listen(mixer.ctrl, Location.TYPE, null);
	frw.listen(mixer.ctrl, CharGroup.TYPE, null);
	frw.listen(mixer.ctrl, Weather.TYPE, null);

        // Announce
        frw.announce(sounds);
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
	if (save && state.getDirty()) state.save();
    }

    /**
     * Called by the framework when about to terminate.
     * @return  whether to save or not
     */
    public boolean hasUnsavedParts() { return state.getDirty(); }

    /**
     * Create (and show) GUI.
     */
    private void createGUI() {
	// The left panel is filled with buttons, some of which are disabled
	// and only show information.

	GridBagConstraints constr1;
	myPanel.setLayout(new GridBagLayout());
	constr1 = new GridBagConstraints();
	constr1.fill = GridBagConstraints.VERTICAL;
	constr1.insets = new Insets(5, 5, 5, 5);
	JPanel leftPanel = new JPanel(new GridBagLayout());

	bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
	bSave.setEnabled(false);
	bSave.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    state.save();
		}
	    });

        bDate = new JButton("1.1.720 (1) 0:00");
	bDate.setEnabled(false);
        bDate.setToolTipText("Current date");

        bAdd = new JButton("Add Top Line");
        bAdd.setMnemonic(KeyEvent.VK_T);
	bAdd.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    GuiLine gl =
                        mixer.addTopLine("", null);
                    gl.myNode = getState().addNode(gl);
                    state.setDirty(gl);
		}
	    });
        bAdd.setToolTipText("Add a conditional sound line");

        bAddCF = new JButton("Add Bottom Line");
        bAddCF.setMnemonic(KeyEvent.VK_B);
	bAddCF.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    GuiLine gl =
                        mixer.addBottomLine();
                    gl.myNode = getState().addNode(gl);
                    state.setDirty(gl);
		}
	    });
        bAddCF.setToolTipText("Add an unconditional sound line");

        GridBagConstraints constr2 = new GridBagConstraints();
	constr2.gridx = 0;
	constr2.fill = GridBagConstraints.HORIZONTAL;
        constr2.weightx = 1;
	leftPanel.add(bDate, constr2);
        leftPanel.add(bAdd, constr2);
        leftPanel.add(bAddCF, constr2);
        leftPanel.add(bSave, constr2);

	// We add dummies at the bottom of the left pane
        GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = 0;
	constr3.gridy = GridBagConstraints.RELATIVE;
	constr3.fill = GridBagConstraints.BOTH;
	constr3.weighty = 1;
	JPanel dummy = new JPanel();
        leftPanel.add(dummy, constr3);

	// The right panel contains the line list and is displayed in a
	// scrollable area.

	GridBagConstraints constr4 = new GridBagConstraints();
	constr4.fill = GridBagConstraints.BOTH;
	constr4.weightx = constr4.weighty = 1;

	mixer = new LineMixer(myProps, this);

        GridBagConstraints constr5 = new GridBagConstraints();
	constr5.gridy = 0;
	constr5.gridx = GridBagConstraints.RELATIVE;

	rightPanel = new JScrollPane(mixer);
        rightPanel.getVerticalScrollBar().setUnitIncrement(10);
        rightPanel.getHorizontalScrollBar().setUnitIncrement(10);
	constr4.insets = new Insets(6,5,6,5);

        JSplitPane sp =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
	myPanel.add(sp, constr4);

	// If someone turned these value into non-Integers, we throw up
	int w = Integer.parseInt(myProps.getProperty("display.width"));
	int h = Integer.parseInt(myProps.getProperty("display.height"));
	rightPanel.setPreferredSize(new Dimension(w, h));
    }

    /**
     * Returns the state object.
     * @return state object
     */
    State getState() { return state; }

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
	return new String[0];
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() { return new String[0]; }

    /**
     * Proxy for sound bank.
     */
    class MySounds extends SoundBank {
        /**
         * Implement as specified in superclass.
         * @return name attribute
         */
        public String getName() { return "Soundplugin " + version(); }

        /**
         * Add a location trigger.
         * @param loc location trigger
         */
        public void addLocation(Location loc) {
            if (loc == null) return;
            GuiLine gl =
                mixer.addTopLine("Location", loc.getName());
            gl.myNode = getState().addNode(gl);
            state.setDirty(gl);
            myFrw.raisePlugin("Sound");
        }

        /**
         * Add a (calendar) event trigger.
         * @param ev event trigger
         */
        public void addEvent(TimeEvent ev) {
            GuiLine gl =
                mixer.addTopLine("Event", ev.getName());
            gl.myNode = getState().addNode(gl);
            state.setDirty(gl);
            myFrw.raisePlugin("Sound");
        }
    }
}
