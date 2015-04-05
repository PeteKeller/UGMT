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
package harn.travel;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import rpg.*;

/**
 * The main class for the travel plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {

    /** Some date constants */
    static long HOUR = 60;
    static long WATCH = 4*HOUR;
    static long DAY = 6*WATCH;

    /** coords of mouse for popup */
    public int gx, gy;

    /** GUI references */
    JPanel myPanel;

    /** My properties */
    Properties myProps;

    /** Path to my data */
    String myPath;

    /** Group state object */
    private State state;

    /** Weather */
    private WeatherEffects weather;

    /** Auto travel object */
    private AutoTravel autoTravel;

    /** My data */
    Data myData;

    /** Left GUI items */
    JButton bDate;
    JButton bSave;
    JButton bDist;
    JButton bMove;
    JButton bStay;
    JButton bFree;
    JButton bTerr;
    JButton bMode;
    JButton bStall;
    JButton bSpeed;
    JButton bCond;
    JFormattedTextField eSpeed;
    JFormattedTextField eTravel;
    JFormattedTextField eStall;
    DefaultMutableTreeNode treeRoot;
    JTree tree;

    JPopupMenu pTravel;
    JPopupMenu pModes;
    JPopupMenu pStayMenu;
    JPopupMenu pStall;

    JMenuItem contTravel;

    /** Right GUI items */
    JScrollPane rightPanel;
    JPanel mapFlow;
    MapHolder map;

    // GUI state items

    /** time to stay (bStay) */
    static long stay = WATCH;

    /** First time visibility */
    static private boolean firstVisible = false;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.8f; }

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
	myData = new Data(this);

	// State object
	state = new State(this);
	//  object
	weather = new WeatherEffects(this);

	// Create GUI
	createGUI();

	// Register listeners
	frw.listen(state, CharGroup.TYPE, null);
	frw.listen(state, TimeEvent.TYPE, null);
	frw.listen(map, Atlas.TYPE, null);
	frw.listen(weather, Weather.TYPE, null);
	frw.listen(myData, TimeFrame.TYPE, null);
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
	// The left panel is filled with buttons, some of which are disabled
	// and only show information. Default information seen below will be
	// replaced as soon as the appropriate objects, which contain such
	// values, become known.

	GridBagConstraints constr1;
	myPanel.setLayout(new GridBagLayout());
	constr1 = new GridBagConstraints();
	constr1.fill = GridBagConstraints.VERTICAL;
	constr1.insets = new Insets(5, 5, 5, 5);

        bDate = new JButton("1.1.720 (1) 0:00");
	bDate.setEnabled(false);
        bDate.setToolTipText("Current date");

        bDist = new JButton("Distance: 0:00");
	bDist.setEnabled(false);
        bDist.setToolTipText("Last leg or potential next leg");

        bMove = new JButton("Move: 0:00");
	bMove.setEnabled(false);
        bMove.setToolTipText("Distance travelled without rest");

        bCond = new JButton("Dry");
	bCond.setEnabled(false);
        bCond.setToolTipText("Current weather/terrain condition");

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

	bFree = new JButton("Normal");
        bFree.setMnemonic(KeyEvent.VK_R);
	bFree.addActionListener(new ActionListener() {
		// 0=normal, 1=free, 2=forced
		int cost;
		public void actionPerformed(ActionEvent e) {
		    // We cycle through the three movement types
		    cost = (cost+1)%3;
		    switch (cost) {
			case 0 : bFree.setText("Normal"); break;
			case 1 : bFree.setText("Free"); break;
			case 2 : bFree.setText("Forced"); break;
		    }
		}
	    });
        bFree.setToolTipText("Free/Normal/Forced movement (costs)");

        bTerr = new JButton("water");
	bTerr.setEnabled(false);
        bTerr.setToolTipText("Current terrain/terrain aimed at");

        pModes = new JPopupMenu();

        bMode = new JButton("Modes");
	bMode.setEnabled(false);
        bMode.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        pModes.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        bMode.setToolTipText("Show travelling modes");
        pModes = new JPopupMenu();

        bMode = new JButton("Modes");
	bMode.setEnabled(false);
        bMode.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        pModes.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        bMode.setToolTipText("Show travelling modes");

        bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
        bSave.setEnabled(false);
        bSave.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    state.save();
                    bSave.setEnabled(false);
		}
	    });

        bSpeed = new JButton("Auto speed");
        bSpeed.setMnemonic(KeyEvent.VK_U);
	bSpeed.addActionListener(new ActionListener() {
		boolean auto = true;
		public void actionPerformed(ActionEvent e) {
		    if (auto)
			bSpeed.setText("Manual speed");
		    else
			bSpeed.setText("Auto speed");
		    eSpeed.setEnabled(auto);
		    auto = !auto;
		}
	    });
        bSpeed.setToolTipText("Auto select speed or manually determine it");

	prepareButtonWidth();

	eSpeed = new JFormattedTextField(NumberFormat.getNumberInstance());
        eSpeed.setValue(new Double(0));
        eSpeed.setEnabled(false);
        eSpeed.setToolTipText("Travelling speed");

        eTravel = new JFormattedTextField(NumberFormat.getIntegerInstance());
        eTravel.setValue(new Integer(8));
        eTravel.setToolTipText("Travel time (hours/day)");

	eStall = new JFormattedTextField(NumberFormat.getIntegerInstance());
        eStall.setValue(new Integer(2));
        eStall.setToolTipText("Consecutive unsuccessful waits, after which auto-travel is aborted");

        pTravel = new JPopupMenu();
        JMenuItem mi = new JMenuItem("move to destination");
        pTravel.add(mi);
        autoTravel = new AutoTravel(this);
        mi.addActionListener(autoTravel);

	tree = new JTree();
        tree.setEditable(false);
	tree.getSelectionModel().setSelectionMode
	    (TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX();
                        gy = e.getY();
                        TreePath path =
                            tree.getPathForLocation(gx, gy);
                        if (path == null) return;
                        pTravel.show(e.getComponent(), gx, gy);
                    }
                }
            });

        JScrollPane scp = new JScrollPane(tree);
        scp.getVerticalScrollBar().setUnitIncrement(10);
        scp.getHorizontalScrollBar().setUnitIncrement(10);

        GridBagConstraints constr2 = new GridBagConstraints();
	constr2.gridx = 0;
	constr2.weightx = 1;
	constr2.fill = GridBagConstraints.HORIZONTAL;
        JPanel leftPanel = new JPanel(new GridBagLayout());
	leftPanel.add(bDate, constr2);
        leftPanel.add(bDist, constr2);
        leftPanel.add(bMove, constr2);
        leftPanel.add(bTerr, constr2);
        leftPanel.add(bCond, constr2);
        leftPanel.add(bStay, constr2);
        leftPanel.add(bFree, constr2);
        leftPanel.add(bMode, constr2);
        leftPanel.add(bSpeed, constr2);
        leftPanel.add(bSave, constr2);

        Box duml = new Box(BoxLayout.Y_AXIS);
        Box dumr = new Box(BoxLayout.Y_AXIS);

        JButton b1 = new JButton("Speed:");
        b1.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        b1.setEnabled(false);
        duml.add(b1);
        dumr.add(eSpeed);

        b1 = new JButton("Travel:");
        b1.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        b1.setEnabled(false);
        duml.add(b1);
        dumr.add(eTravel);
        
        b1 = new JButton("Stall:");
        b1.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        b1.setEnabled(false);
        duml.add(b1);
        dumr.add(eStall);

        Box dummy = new Box(BoxLayout.X_AXIS);
        dummy.add(duml);
        dummy.add(dumr);

        pStall = new JPopupMenu();
        contTravel = new JMenuItem("<html><i>Continue</i>");
        contTravel.setEnabled(false);
        contTravel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    autoTravel.continueTravel();
                }
            });
        pStall.add(contTravel);
        pStall.addSeparator();

        mi = new JCheckBoxMenuItem("Encounters");
        mi.setSelected(state.getTypes().contains("Encounters"));
        mi.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    String txt = ((JCheckBoxMenuItem)e.getItem()).getText();
                    bSave.setEnabled(true);
                    state.toggleType(txt);
                }
            });
        pStall.add(mi);

        weatherStall();

        bStall = new JButton("Stall Conditions");
	bStall.setEnabled(false);
        bStall.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        pStall.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        bStall.setToolTipText("Show/Select conditions that stall auto-travel");

        GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = 0;
	constr3.weightx = 1;
	constr3.gridy = GridBagConstraints.RELATIVE;
	constr3.fill = GridBagConstraints.BOTH;
	constr3.weighty = 1;

        leftPanel.add(scp, constr3);
        leftPanel.add(dummy, constr2);
        leftPanel.add(bStall, constr2);

	// The right panel only contains the map.  The map is displayed in a
	// scrollable area. When the mouse is left-clicked here, movement to
	// that point takes place. Right-clicking leads only to a calculation.

	GridBagConstraints constr4 = new GridBagConstraints();
	constr4.fill = GridBagConstraints.BOTH;
	constr4.weightx = constr4.weighty = 1;

	map = new MapHolder(myProps, this);
        map.setToolTipText
            ("<html>left-click to travel to cursor<br>right-click to " +
             "estimate travel-time from cursor</html>");

	// Dummy panels
	JPanel pnms = new JPanel(new GridBagLayout());
	JPanel pn = new JPanel();
	JPanel ps = new JPanel();
	mapFlow = new JPanel(new GridBagLayout());
	JPanel pe = new JPanel();
	JPanel pw = new JPanel();

	Dimension zero = new Dimension(0,0);
	pn.setPreferredSize(zero);
	ps.setPreferredSize(zero);
	pe.setPreferredSize(zero);
	pw.setPreferredSize(zero);

	pnms.add(pn, constr3);
	pnms.add(map);
	pnms.add(ps, constr3);

        GridBagConstraints constr5 = new GridBagConstraints();
	constr5.gridy = 0;
	constr5.gridx = GridBagConstraints.RELATIVE;
	constr5.fill = GridBagConstraints.BOTH;
	constr5.weightx = 1;

	mapFlow.add(pe, constr5);
	mapFlow.add(pnms);
	mapFlow.add(pw, constr5);

	rightPanel = new JScrollPane(mapFlow);
        rightPanel.getVerticalScrollBar().setUnitIncrement(10);
        rightPanel.getHorizontalScrollBar().setUnitIncrement(10);
	constr4.insets = new Insets(6,5,6,5);

        JSplitPane sp =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
	myPanel.add(sp, constr4);

	map.addMouseListener(new MoveByMouse(this));

	// If someone turned these value into non-Integers, we throw up
	int w = Integer.parseInt(myProps.getProperty("display.width"));
	int h = Integer.parseInt(myProps.getProperty("display.height"));
	rightPanel.setPreferredSize(new Dimension(w, h));
	rightPanel.getViewport().addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    if (!firstVisible) redo(true);
		}
	    });
    }

    /**
     * This method is called by other objects, which require a redraw of the
     * screen. This includes the map and informational buttons. We assume
     * everything has changed.
     * @param all do the full redo or just the viewport
     */
    void redo(boolean all) {
	// State, Mode and Weather information
	if (getState() != null && all) {
	    bDate.setText(Data.stringDate(getState().getDate()));
            String[] modes = getState().getModes();
            pModes.removeAll();
            for (int i = 0; modes != null && i < modes.length; i++) {
                JMenuItem mi = new JMenuItem(modes[i]);
                mi.setEnabled(false);
                pModes.add(mi);
            }
        }

	if (getWeather() != null && all)
	    bCond.setText
		(getWeather().getCondition(getState().getX(),getState().getY()));

	// Get viewport dimension
	JViewport vp = rightPanel.getViewport();
	int w = vp.getExtentSize().width;
	int h = vp.getExtentSize().height;
	
	int cw = map.getPreferredSize().width;
	int ch = map.getPreferredSize().height;

	// Get center from active group
	int x = getState().getX();
	int y = getState().getY();

	if (x - w/2 < 0) x = w/2;
	if (y - h/2 < 0) y = h/2;
	if (x + w/2 > cw) x = cw - w/2;
	if (y + h/2 > ch) y = ch - h/2;

	// Set viewport center to hotspot
	firstVisible = (w + h != 0);
	vp.setViewPosition(new Point(x - w/2, y - h/2));
    }

    /**
     * Returns the map display.
     * @return map display
     */
    MapHolder getMap() { return map; }

    /**
     * Returns the state object.
     * @return state object
     */
    State getState() { return state; }

    /**
     * Returns the weather object.
     * @return weather object
     */
    WeatherEffects getWeather() { return weather; }

    /**
     * Prepares the button widths according to the widest terrain/vegetation
     * combination possible. This is gathered from the data holder.
     */
    private void prepareButtonWidth() {
	// Get the used font
	Font f = bTerr.getFont();
	FontMetrics fm = bTerr.getFontMetrics(f);

	// Iterate
	ArrayList atv = myData.getAllStrings();
	int w = 0;
	for (int i = 0; i < atv.size(); i++) {
	    int w0 = fm.stringWidth((String)atv.get(i));
	    if (w0 > w) w = w0;
	}
	// Consider the margins for the button width
	w += bTerr.getMargin().left + bTerr.getMargin().right;

	// Set the width
	int h = bTerr.getPreferredSize().height;
	bTerr.setPreferredSize(new Dimension(w, h));
    }

    /**
     * Reset all available routes (destinations)
     */
    public void setRoutes(String map) {
	treeRoot = new DefaultMutableTreeNode("Destinations");
        SortedTreeModel tm = new SortedTreeModel(treeRoot);
	tree.setModel(tm);
        ArrayList al = (ArrayList) myData.getRoutes(map);
        Hashtable nodes = new Hashtable();

        for (int i = 0; al != null && i < al.size(); i++) {
            Data.Route rout = (Data.Route) al.get(i);
            String[] locs = new String[] { rout.loc1(), rout.loc2() };
            for (int j = 0; j < 2; j++) {
                if (!myData.isHidden(locs[j])) {
                    String loc = locs[j].substring(locs[j].lastIndexOf("//")+2);
                    if (loc.indexOf("/") > 0) {
                        String group = loc.substring(0,loc.indexOf("/"));
                        loc = loc.substring(loc.indexOf("/")+1);
                        HashSet node = (HashSet) nodes.get(group);
                        if (node == null) {
                            node = new HashSet();
                            nodes.put(group, node);
                        }
                        node.add(loc);
                    }
                    else
                        nodes.put(loc, loc);
                }
            }
        }
        Iterator iter = nodes.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            Object val = nodes.get(key);
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(key);
            tm.insertNodeSorted(node, treeRoot);
            if (val instanceof HashSet) {
                Iterator iter2 = ((HashSet)val).iterator();
                while (iter2.hasNext()) {
                    tm.insertNodeSorted
                        (new DefaultMutableTreeNode(iter2.next()), node);
                }
            }
        }
    }

    /**
     * Set the weather stall menu.
     */
    public void weatherStall() {
        Set s = myData.getEffects();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            String me = (String) iter.next();
            JCheckBoxMenuItem mi = new JCheckBoxMenuItem(me.substring(1));
            mi.setSelected(me.charAt(0) == '+');
            mi.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
			String txt = ((JCheckBoxMenuItem)e.getItem()).getText();
                        bSave.setEnabled(true);
                        state.toggleType(txt);
		    }
                });
            pStall.add(mi);
        }
    }

    /**
     * Each plugin must provide an array of objects it requires.
     * @return list of imports
     */
    public String[] requires() {
	return new String[] {
            CharGroup.TYPE, TimeFrame.TYPE, Atlas.TYPE, ScaledMap.TYPE
	};
    }

    /**
     * Each plugin must provide an array of objects it uses.
     * @return list of imports
     */
    public String[] uses() {
	return new String[] { Weather.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() { return new String[0]; }

    class MyPopUpActionListener implements ActionListener {
        String time;
        long aStay;
        MyPopUpActionListener(String aTime) {
            time = aTime;
            if (time.indexOf(":") > 0)
                aStay = Long.parseLong(time.substring(0,1)) * HOUR +
                    Long.parseLong(time.substring(2));
            else
                aStay = Long.parseLong(time.substring(0,time.indexOf(" "))) * DAY;
        }
        public void actionPerformed(ActionEvent e) {
            bStay.setText("Stay (" + time + ")");
            stay = aStay;
        }
    }
}
