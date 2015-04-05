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
package harn.calendar;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import rpg.*;

/**
 * The main class for the calendar plugin. Takes care of the GUI and
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

    /** Group state object */
    private State state;

    /** Weather Display */
    private CalendarDisplay display;

    /** Data */
    Data myData;

    /** Left GUI items */
    JButton bSave;
    JButton bDate;
    JButton bDusk;
    JButton bDist;
    JButton bStay;
    JButton bMoon;
    JButton bSunsign;
    JButton bSuppress;
    JButton bBeginNext;
    JButton bEndNext;

    JPopupMenu pStayMenu;
    MoveByMouse pSoundMenu;
    JPopupMenu pSuppressMenu;

    /** Right GUI items */
    JScrollPane rightPanel;
    JPanel calFlow;

    /** GUI state */
    private long stay = 4 * Data.HOUR;

    /** coordinates of popup */
    int gx, gy;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.9f; }

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
	state = new State(this);

	// create GUI
	createGUI();

	// register listeners
	frw.listen(state, CharGroup.TYPE, null);
	frw.listen(state, TimeEvent.TYPE, null);
        frw.listen(myData, SoundBank.TYPE, null);
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
    public void stop(boolean save) { state.save(); }

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

        bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
        bSave.setEnabled(false);
        bSave.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    state.save();
                    bSave.setEnabled(false);
		}
	    });

        bDate = new JButton("1.1.720 (1) 0:00");
	bDate.setEnabled(false);
        bDate.setToolTipText("Current date");
	bSunsign = new JButton("Sunsign");
	bSunsign.setEnabled(false);
        bSunsign.setToolTipText("Current Sunsign");

	bMoon = new JButton("Moon");
	bMoon.setEnabled(false);
        bMoon.setToolTipText("Current moon phase");

        bStay = new JButton("Stay (4:00)");
        bStay.setMnemonic(KeyEvent.VK_T);
	bStay.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0)
                        state.addDate(-stay);
                    else
                        state.addDate(stay);
		}
	    });
        bStay.setToolTipText("Wait for the given time (Shift: go back in time)");

        pStayMenu = new JPopupMenu();

        String[] times = myProps.getProperty("timesteps").split(",");

        for (int i = 0; i < times.length; i++) {
            JMenuItem mi = new JMenuItem(times[i]);
            pStayMenu.add(mi);
            mi.addActionListener(new MyPopUpActionListener(times[i]));
        }

        bStay.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        pStayMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

        bDusk = new JButton("Dusk");
        bDusk.setMnemonic(KeyEvent.VK_U);
	bDusk.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    long now = state.getDate();
		    long dawn = myData.getDawn(now);
		    long dusk = myData.getDusk(now);
		    long nextDawn = myData.getDawn(now + Data.DAY);

		    // Go to the next dawn/dusk. We add one minute to avoid
		    // rounding problems.
		    if (dawn > now)
			state.addDate(dawn - now + 1);
		    else if (dusk > now) 
			state.addDate(dusk - now + 1);
		    else 
			state.addDate(nextDawn - now + 1);
                    redo();
		}
	    });
        bDusk.setToolTipText("Wait for next dusk/dawn (for time in parenthesis)");

        bDist = new JButton("Distance: 0:00");
	bDist.setEnabled(false);
        bDist.setToolTipText("Last leg or potential next leg");

        pSuppressMenu = new JPopupMenu();
        addType(null);

        bSuppress = new JButton("Suppress");
	bSuppress.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        pSuppressMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        bSuppress.setToolTipText("Suppress selectable event types");

        bBeginNext = new JButton("Begin Next");
        bBeginNext.setMnemonic(KeyEvent.VK_N);
        bBeginNext.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    state.gotoNext(0);
                    redo();
                }
            });
        bBeginNext.setToolTipText("Go to next beginning of an event");

        bEndNext = new JButton("End Next");
        bEndNext.setMnemonic(KeyEvent.VK_E);
        bEndNext.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    state.gotoNext(1);
                    redo();
                }
            });
        bEndNext.setToolTipText("Go to next end of an event");

	// Prepare button width (assumption made on display style)
	Font f = bDate.getFont();
	int bw = bDate.getFontMetrics(f).stringWidth("88-WWW-8888 8:88")
	    // Consider margins
	    + bDate.getMargin().left + bDate.getMargin().right;

	// Set button width
	int bh = bDate.getPreferredSize().height;
	bDate.setPreferredSize(new Dimension(bw, bh));

        GridBagConstraints constr2 = new GridBagConstraints();
	constr2.gridx = 0;
	constr2.weightx = 1;
	constr2.fill = GridBagConstraints.HORIZONTAL;
	leftPanel.add(bSave, constr2);
	leftPanel.add(bDate, constr2);
        leftPanel.add(bSunsign, constr2);
        leftPanel.add(bMoon, constr2);
        leftPanel.add(bDist, constr2);
        leftPanel.add(bStay, constr2);
        leftPanel.add(bDusk, constr2);
        leftPanel.add(bSuppress, constr2);
        leftPanel.add(bBeginNext, constr2);
        leftPanel.add(bEndNext, constr2);

	// We stuff everything at the top by inserting an expanding dummy.
	JPanel dummy = new JPanel();
        GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = 0;
	constr3.gridy = GridBagConstraints.RELATIVE;
	constr3.fill = GridBagConstraints.BOTH;
	constr3.weighty = 1;
        leftPanel.add(dummy, constr3);

	// The dates are displayed in a scrollable area.

	GridBagConstraints constr4 = new GridBagConstraints();
	constr4.fill = GridBagConstraints.BOTH;
	constr4.weightx = constr4.weighty = 1;

	display = new CalendarDisplay(myProps, this);

        pSoundMenu = new MoveByMouse(this);
        
        display.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX();
                        gy = e.getY();
                        TimeEvent ev = display.findEvent(gx,gy);
                        if (ev != null)
                            pSoundMenu.show(e.getComponent(), gx, gy);
                    }
                }
            });
        
	// Dummy panels
	JPanel pnms = new JPanel(new GridBagLayout());
	JPanel pn = new JPanel();
	JPanel ps = new JPanel();
	calFlow = new JPanel(new GridBagLayout());
	JPanel pe = new JPanel();
	JPanel pw = new JPanel();

	Dimension zero = new Dimension(0,0);
	pn.setPreferredSize(zero);
	ps.setPreferredSize(zero);
	pe.setPreferredSize(zero);
	pw.setPreferredSize(zero);

	pnms.add(pn, constr3);
	pnms.add(display);
	pnms.add(ps, constr3);

        GridBagConstraints constr5 = new GridBagConstraints();
	constr5.gridy = 0;
	constr5.gridx = GridBagConstraints.RELATIVE;
	constr5.fill = GridBagConstraints.BOTH;
	constr5.weightx = 1;

	calFlow.add(pe, constr5);
	calFlow.add(pnms);
	calFlow.add(pw, constr5);

	rightPanel = new JScrollPane(calFlow);
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
     * This method is called by other objects, which require a redraw of the
     * screen. This includes the map and informational buttons. We assume
     * everything has changed.
     */
    void redo() {
	// Date and condition information
	if (getState() != null) {
	    bDate.setText(myData.datetime2string(getState().getDate()));
	}

	// Moon
	int m = myData.getMoon(state.getDate());
	if (m == 0) bMoon.setText("New Moon");
	else if (m == 100) bMoon.setText("Full moon");
	else if (m > 0) bMoon.setText("Waxing (" + m + ")");
	else  bMoon.setText("Waning (" + (-m) + ")");

	// Dusk/dawn
	long now = state.getDate();
	long dawn = myData.getDawn(now);
	long dusk = myData.getDusk(now);
	long nextDawn = myData.getDawn(now + Data.DAY);
	if (dawn > now)
	    bDusk.setText("Dawn (" + myData.time2string(dawn - now) + ")");
	else if (dusk > now)
	    bDusk.setText("Dusk (" + myData.time2string(dusk - now) + ")");
	else
	    bDusk.setText("Dawn (" + myData.time2string(nextDawn - now) + ")");

	// Sunsign
	bSunsign.setText
            ("Sunsign: " + myData.getSunsign(getState().getDate()));

        // Get viewport dimension
        JViewport vp = rightPanel.getViewport();
        int w = vp.getExtentSize().width;
        int h = vp.getExtentSize().height;

        // Get center from state
        int cw = calFlow.getSize().width;
        int ch = calFlow.getSize().height;

        int x = (int)((now % Data.DAY) * cw / Data.DAY);
        int y = (int)(state.back + .5 * ch/(state.front + state.back));

        if (x - w/2 < 0) x = w/2;
        if (y - h/2 < 0) y = h/2;
        if (x + w/2 > cw) x = cw - w/2;
        if (y + h/2 > ch) y = ch - h/2;

        // Set viewport center to hotspot
        vp.setViewPosition(new Point(x - w/2, y - h/2));
    }

    /**
     * Add a event type to suppress. A null is not added, but does a recalc of
     * the menu.
     * @param type type to suppress 
     */
    public void addType(String type) {
        if (myData == null)  return;
        if (type != null) myData.addType(type, false);
        Set s = myData.getTypes();
        pSuppressMenu = new JPopupMenu();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            JCheckBoxMenuItem mi = new JCheckBoxMenuItem((String)me.getKey());
            mi.setSelected(((Boolean)me.getValue()).booleanValue());
            mi.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
			String txt = ((JCheckBoxMenuItem)e.getItem()).getText();
                        myData.addType(txt, e.getStateChange() == ItemEvent.SELECTED);
                        state.inform(null);
                        bSave.setEnabled(true);
		    }
                });
            pSuppressMenu.add(mi);
        }
    }

    /**
     * Returns the weather display object.
     * @return weather display object
     */
    CalendarDisplay getDisplay() { return display; }

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
	return new String[] { CharGroup.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it uses.
     * @return list of imports
     */
    public String[] uses() { return new String[0]; }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() {
	return new String[] { TimeFrame.TYPE, TimeEvent.TYPE };
    }

    class MyPopUpActionListener implements ActionListener {
        String time;
        long aStay;
        MyPopUpActionListener(String aTime) {
            time = aTime;
            if (time.indexOf(":") > 0)
                aStay = Long.parseLong(time.substring(0,1)) * Data.HOUR +
                    Long.parseLong(time.substring(2));
            else
                aStay = Long.parseLong
                    (time.substring(0,time.indexOf(" "))) * Data.DAY;
        }
        public void actionPerformed(ActionEvent e) {
            bStay.setText("Stay (" + time + ")");
            stay = aStay;
        }
    }
}
