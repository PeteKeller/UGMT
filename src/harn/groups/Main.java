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
package harn.groups;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import rpg.*;

/**
 * The main class for the groups plugin. Takes care of the GUI and
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
    private Data data;

    /** Group state object */
    private State state;

    /** Left GUI items */
    JButton bSave;
    JPopupMenu pMenu;
    JMenuItem miActive;
    DefaultMutableTreeNode grouptree;
    JTree tree;

    /** mouse pressed coordinates */
    private int gx, gy;

    /** Right GUI items */
    GroupDisplay display;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.8f; }

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

	// Data holder
	data = new Data(this);

	// State object
	state = new State(this);

	// create GUI
	createGUI();

	// register listeners
	frw.listen(data, TimeFrame.TYPE, null);
	frw.listen(data, Char.TYPE, null);
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
    public void stop(boolean save) {
	if (save && state.getDirty()) state.save();
    }

    /**
     * Called by the framework when about to terminate. We never save
     * anything. See above.
     * @return false
     */
    public boolean hasUnsavedParts() { return state.getDirty(); }

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
		}
	    });

	tree = new JTree(new SortedTreeModel(grouptree));
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
                Font bf = null;
                Font of = null;
                public Font getFont() {
                    if (bf == null) {
                        of = super.getFont();
                        if (of != null)
                            bf = new Font
                                (of.getName(), Font.BOLD, of.getSize());
                    }
                    if (of != null && getState().getActive(getText()))
                        return bf;
                    else
                        return of;
                }
            });
	tree.getSelectionModel().setSelectionMode
	    (TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setEditable(true);
	tree.addTreeSelectionListener(new TreeSelectionListener() {
		public void valueChanged(TreeSelectionEvent e) {
		    TreePath p = e.getNewLeadSelectionPath();
		    if (p != null && p.getPathCount() == 2) {
			getState().setCurrent
                            (p.getPathComponent(1).toString());
		    }
		}
	    });
        tree.getModel().addTreeModelListener(new TreeModelListener() {
                public void treeNodesChanged(TreeModelEvent e) {
                    String newname = e.getChildren()[0].toString();
                    getState().setName(e.getChildIndices()[0], newname); 
                }
                public void treeNodesInserted(TreeModelEvent e) {}
                public void treeNodesRemoved(TreeModelEvent e) {}
                public void treeStructureChanged(TreeModelEvent e) {}
            });

        tree.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX(); gy = e.getY();
                        pMenu.show(e.getComponent(), gx, gy);
                    }
                }
            });

        pMenu = new JPopupMenu();
        JMenuItem mi1 = new JMenuItem("Create Group");
        pMenu.add(mi1);
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    getState().newGroup();
		}
	    });

        JMenuItem mi2 = new JMenuItem("Remove Group");
        pMenu.add(mi2);
        mi2.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    MutableTreeNode tn =
                        (MutableTreeNode) tp.getLastPathComponent();
		    if (tn != null && tn.isLeaf())
                        getState().removeGroup(tn.toString());
		}
	    });

        miActive = new JMenuItem("Activate");
        pMenu.add(miActive);
        miActive.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = tree.getPathForLocation(gx,gy);
                    MutableTreeNode tn =
                        (MutableTreeNode) tp.getLastPathComponent();
		    if (tn != null && tn.isLeaf())
                        getState().setActive(tn.toString());
		}
	    });



        GridBagConstraints constr2 = new GridBagConstraints();
	constr2.gridx = 0;
	constr2.fill = GridBagConstraints.HORIZONTAL;
	constr2.weightx = 1;
        leftPanel.add(bSave, constr2);

	// We add an expanding JTree.
        GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = 0;
	constr3.fill = GridBagConstraints.BOTH;
	constr3.weighty = 1;
        leftPanel.add(new JScrollPane(tree), constr3);

	GridBagConstraints constr4 = new GridBagConstraints();
	constr4.fill = GridBagConstraints.BOTH;
	constr4.weightx = constr4.weighty = 1;
        constr4.insets = new Insets(6,5,6,5);

	display = new GroupDisplay(myProps, this);
	JScrollPane view = new JScrollPane(display);

        JSplitPane sp =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, view);
        sp.setResizeWeight(.2);
        myPanel.add(sp, constr4);

        view.getVerticalScrollBar().setUnitIncrement(10);
        view.getHorizontalScrollBar().setUnitIncrement(10);

        // Set the first group active; default
        tree.addSelectionInterval(1,1);
    }

    /**
     * Returns the state object.
     * @return state object
     */
    State getState() { return state; }

    /**
     * Returns the data object.
     * @return data object
     */
    Data getData() { return data; }

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
    public String[] uses() { return new String[] { Char.TYPE }; }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() {
	return new String[] { CharGroup.TYPE };
    }
}
