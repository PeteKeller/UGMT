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
package harn.combat;

import harn.repository.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import rpg.*;

/**
 * The main class for the skills plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {
    /** Framework reference */
    Framework myFrw;

    /** GUI references */
    private JPanel myPanel;

    /** My properties */
    Properties myProps;

    /** Path to my data */
    String myPath;

    /** Character data entry object */
    private Data myData;

    /** Character state entry object */
    private State myState;
    private Printer myPrint;

    // Upper GUI items
    private JPanel btPanel;
    private JPanel mapPanel;
    JButton bSave;
    JButton bDist;
    JButton bToggle;
    JButton bGrid;
    JButton bUpdate;
    JButton bFow;
    private JMenuItem miExport;
    private JMenuItem miPrint;
    JSpinner bScaleSpin;
    JSpinner bFowRadius;

    private JPopupMenu pMenu;

    MapHolder map;

    /** Tree model for combatant list */
    JTree tree;
    JTable table;

    // Lower GUI items

    /** Armour Item list */
    ItemList aitems;

    /** Weapon Item list */
    ItemList witems;

    /** Wound Item list */
    ItemList vitems;

    /** Armour table */
    ArmourAnalysis armour;

    /** Lower left corner */
    private JPanel llc;

    /** mouse pressed coordinates */
    private int gx, gy;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     * @return version
     */
    public float version() { return 0.4f; }

    /**
     * Init method. called by framework. Init all objects and then create
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

        // Data holder
        myData = new Data(this);
        myPrint = new Printer(this);

        // create GUI
        createGUI();
        map.listen();

        // State object
        myState = new State(this);

        // Register listeners
        frw.listen(myState, Char.TYPE, null);
        frw.listen(myState, Equipment.TYPE, null);
        frw.listen(myState, TimeFrame.TYPE, null);
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
     * Called by the framework when about to terminate.
     * @return state's dirty flag
     */
    public boolean hasUnsavedParts() { return false; }

    /**
     * Create (and show) GUI.
     */
    private void createGUI() {
        myPanel.setLayout(new GridBagLayout());

        bScaleSpin = new JSpinner(Framework.getScaleSpinnerModel(17));
        bScaleSpin.setValue(new Integer(100));
        ((JSpinner.DefaultEditor)bScaleSpin.getEditor()).getTextField().setEditable(false);
        bScaleSpin.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) { map.redo(); }
            });
        bScaleSpin.setToolTipText("Map scale");
        
        bFowRadius = new JSpinner();
        bFowRadius.setValue(new Integer(30));
        bFowRadius.setToolTipText("Fog of war radius (ft)");

        btPanel = new JPanel(new GridBagLayout());
        mapPanel = new JPanel(new GridBagLayout());

	bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
	bSave.setEnabled(false);
	bSave.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    myState.save();
		}
	    });

        bDist = new JButton(" - ");
        bDist.setEnabled(false);
        bDist.setToolTipText("Shows distance between clicked locations");

        bGrid = new JButton("Toggle Grid");
        bGrid.setMnemonic(KeyEvent.VK_G);
        bGrid.setToolTipText("Toggle Grid on map");
	bGrid.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    map.grid = !map.grid;
                    map.repaint();
                }
            });

        bToggle = new JButton("Toggle A/W/W");
        bToggle.setMnemonic(KeyEvent.VK_T);
        bToggle.setToolTipText("Toggle Armour, Weapons, and Wounds display");
	bToggle.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    GridBagConstraints constr = new GridBagConstraints();
                    constr.gridx = 0;
                    constr.fill = GridBagConstraints.BOTH;
                    constr.weighty = constr.weightx = 1;
                    if (llc.getComponent(0) == aitems) {
                        llc.remove(aitems);
                        llc.add(witems, constr);
                    }
                    else if (llc.getComponent(0) == witems) {
                        llc.remove(witems);
                        llc.add(vitems, constr);
                    }
                    else {
                        llc.remove(vitems);
                        llc.add(aitems, constr);
                    }
                    llc.revalidate();
                    llc.repaint();
		}
	    });

        bUpdate = new JButton("Update wounds");
        bUpdate.setMnemonic(KeyEvent.VK_U);
        bUpdate.setToolTipText("Update all wounds. Use with caution");
	bUpdate.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    myState.updateWounds();
                }
            });

        bFow = new JButton("Toggle FOW");
        bFow.setMnemonic(KeyEvent.VK_F);
        bFow.setToolTipText("Toggle Fog of war for selected combatant");
        bFow.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    map.fow = !map.fow;
                    map.repaint();
                }
            });

        // character tree
        tree = new JTree();
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    TreePath p = e.getNewLeadSelectionPath();
                    if (p == null) return;
                    TreeNode tn = (TreeNode) p.getLastPathComponent();
                    if (tn == null || !tn.isLeaf()) return;

                    myState.select(tn.toString());
                    armour.recalc();
                }
            });

        pMenu = new JPopupMenu();

        JMenuItem mi1 = new JMenuItem("Create Combatant");
        pMenu.add(mi1);
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    myState.newCombatant(null);
		}
	    });
        JMenuItem mi2 = new JMenuItem("Remove Combatant");
        pMenu.add(mi2);
        mi2.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    if (tp == null) return;
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (!tn.isLeaf()) return;
                    myState.removeCombatant((MutableTreeNode)tn);
		}
	    });
        JMenuItem mi3 = new JMenuItem("Combatant off field");
        pMenu.add(mi3);
        mi3.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    if (tp == null) return;
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (!tn.isLeaf()) return;
                    String name = tn.toString();
                    double[] pos = map.getTokenPos(name);
                    if (pos == null) return;
                    pos[0] = pos[1] = -1;
                    myState.setDirty(true);
                    map.announceToken(name);
                    map.repaint();
		}
	    });
        miExport = new JMenuItem("Export HTML");
        pMenu.add(miExport);
        miExport.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    if (tp == null) return;
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (!tn.isLeaf()) return;
                    myState.export(tn.toString());
                }
            });
        miExport.setToolTipText("Write (Web) character sheet");

        miPrint = new JMenuItem("Print");
        pMenu.add(miPrint);
        miPrint.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    if (tp == null) return;
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (!tn.isLeaf()) return;
                    myState.export(tn.toString());
                    myPrint.print(tn.toString());
                }
            });

        JMenuItem mi5 = new JMenuItem("Add Armour");
        pMenu.add(mi5);
        mi5.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    if (tp == null) return;
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (!tn.isLeaf()) return;
                    myState.newArmour
                        (tn.toString(),
                         tn == tree.getLastSelectedPathComponent());
                }
	    });

        JMenuItem mi4 = new JMenuItem("Add Weapon");
        pMenu.add(mi4);
        mi4.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    if (tp == null) return;
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (!tn.isLeaf()) return;
                    myState.newWeapon
                        (tn.toString(),
                         tn == tree.getLastSelectedPathComponent());
                }
	    });

        JMenuItem mi6 = new JMenuItem("Add Wound");
        pMenu.add(mi6);
        mi6.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    if (tp == null) return;
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (!tn.isLeaf()) return;
                    myState.newWound
                        (tn.toString(),
                         tn == tree.getLastSelectedPathComponent());
                }
	    });
        JMenuItem mi7= new JMenuItem("Clone Combatant");
        pMenu.add(mi7);
        mi7.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    if (tp == null) return;
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (!tn.isLeaf()) return;
                    myState.cloneCombatant(tn.toString());
		}
	    });

        tree.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX(); gy = e.getY();
                        TreePath tp = tree.getPathForLocation(gx,gy);
                        if (tp == null) return;
                        boolean sel = (tree.getLastSelectedPathComponent() == tp.getLastPathComponent());
                        miExport.setEnabled(sel);
                        miPrint.setEnabled(sel);
                        pMenu.show(e.getComponent(), gx, gy);
                    }
                }
            });

        JScrollPane tsp = new JScrollPane(tree);

        // Item list
        JPanel ip = new JPanel();
        ip.setLayout(new GridBagLayout());
        aitems = new ItemList(this);
        witems = new ItemList(this);
        vitems = new ItemList(this);
        map = new MapHolder(this);
	map.addMouseListener(new MoveByMouse
                             (this, Integer.parseInt(myProps.getProperty("distance.feet"))));
        map.setToolTipText
            ("<html>left-click to select combatant from map at coordinates<br> " +
             "shift-left-click to place combatant on map at " + 
             "coordinates<br>" +
             "right-click to rotate combatant clockwise<br>" +
             "shift-right-click to measure distances</html>");

	// Dummy panels
	JPanel pnms = new JPanel(new GridBagLayout());
	JPanel pn = new JPanel();
	JPanel ps = new JPanel();
	JPanel mapFlow = new JPanel(new GridBagLayout());
	JPanel pe = new JPanel();
	JPanel pw = new JPanel();

	Dimension zero = new Dimension(0,0);
	pn.setPreferredSize(zero);
	ps.setPreferredSize(zero);
	pe.setPreferredSize(zero);
	pw.setPreferredSize(zero);

        GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = 0;
	constr3.gridy = GridBagConstraints.RELATIVE;
	constr3.fill = GridBagConstraints.BOTH;
	constr3.weighty = 1;

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

        JScrollPane mp = new JScrollPane(mapFlow);
        JScrollBar sb = mp.getVerticalScrollBar();
        sb.setUnitIncrement(10);
        sb = mp.getHorizontalScrollBar();
        sb.setUnitIncrement(10);

        // Armour analysis
        armour = new ArmourAnalysis(this);
        table = new JTable(armour);
        table.setDefaultRenderer(String.class, new TableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    if (column == 0) {
                        TableCellRenderer renderer =
                            table.getTableHeader().getDefaultRenderer();
                        return renderer.getTableCellRendererComponent
                            (table, value, isSelected, hasFocus, row, column);
                    }
                    JLabel lab = new JLabel((String)value, SwingConstants.CENTER);
                    return lab;
                }
            });
        JScrollPane aap = new JScrollPane(table);

        GridBagConstraints constr = new GridBagConstraints();
        constr.gridx = 0;
        constr.fill = GridBagConstraints.BOTH;
        constr.weighty = constr.weightx = 1;
        mapPanel.add(mp, constr);
        llc = new JPanel(new GridBagLayout());
        llc.add(aitems, constr);

        constr.weighty = 0;
        constr.gridy = 0;
        ip.add(llc, constr);
        constr.weighty = 1;
        constr.gridy = 1;
        JPanel dp = new JPanel();
        dp.setBackground(Color.WHITE);
        ip.add(dp, constr);
    
        constr.weighty = 0;
        constr.gridy = 0;
        btPanel.add(bSave, constr);
        constr.gridy = 1;
        btPanel.add(bDist, constr);
        constr.gridy = 2;
        btPanel.add(bToggle, constr);
        constr.gridy = 3;
        btPanel.add(bGrid, constr);
        constr.gridy = 4;
        btPanel.add(bUpdate, constr);
        constr.gridy = 5;
        btPanel.add(bFowRadius, constr);
        constr.gridy = 6;
        btPanel.add(bFow, constr);
        constr.gridy = 7;
        btPanel.add(bScaleSpin, constr);
        constr.weighty = 1;
        constr.gridy = 8;
        btPanel.add(tsp, constr);

        JSplitPane sp1 =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, btPanel, mapPanel);
        sp1.setResizeWeight(.2);

        JSplitPane sp2 =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(ip), aap);
        sp2.setResizeWeight(.5);

        JSplitPane sp3 =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, sp1, sp2);
        sp3.setResizeWeight(.5);

        myPanel.add(sp3, constr);
    }

    /** Select tree node from token name */
    public void treeSelect(String name) {
        TreeNode root = (TreeNode)tree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode tn = root.getChildAt(i);
            if (((DefaultMutableTreeNode)tn).getUserObject().equals(name))
                tree.setSelectionPath(new TreePath(new Object[] { root, tn }));
        }
    }

    /** Late addition after model is prepared */
    public void addTreeModelListener() {
        tree.getModel().addTreeModelListener(new TreeModelListener() {
                public void treeNodesChanged(TreeModelEvent e) {
                    String newname = e.getChildren()[0].toString();
                    if (((TreeNode)e.getChildren()[0]).isLeaf())
                        myState.setName(((MutableTreeNode)e.getChildren()[0]), newname); 
                    myState.setDirty(true);
                }
                public void treeNodesInserted(TreeModelEvent e) {}
                public void treeNodesRemoved(TreeModelEvent e) {}
                public void treeStructureChanged(TreeModelEvent e) {}
            });
    }

    /**
     * Returns the state object.
     * @return state object
     */
    public State getState() { return myState; }

    /**
     * Get export map.
     */
    public BufferedImage getCombatMap() {
        Dimension d = map.getPreferredSize(false);
        BufferedImage img = new BufferedImage
            (d.width, d.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gc = (Graphics2D) img.getGraphics();
        gc.setColor(Color.BLACK);
        map.paintComponent(gc);
        return img;
    }

    /**
     * Returns the data object.
     * @return data object
     */
    public Data getData() { return myData; }

    /**
     * Each plugin must provide an array of objects it requires.
     * @return list of imports
     */
    public String[] requires() { return new String[0]; }

    /**
     * Each plugin must provide an array of objects it uses.
     * @return list of imports
     */
    public String[] uses() {
        return new String[] { Equipment.TYPE, Char.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() { return new String[] { Token.TYPE }; }
}
