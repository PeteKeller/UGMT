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
package harn.equipment;

import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.event.*;
import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
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
    private JPanel myPanel;

    /** Display of important characters */
    TreeTable treetable;

    /** Display of everything else*/
    TreeTable remainder;

    /** My properties */
    Properties myProps;

    /** Path to my data */
    String myPath;

    /** Framework reference */
    Framework myFrw;

    /** State object */
    State state;

    /** Data object */
    Data data;

    /** Data GUI */
    DragTree dataList;

    /** Data model */
    SortedTreeModel dataModel;

    /** Left panel */
    JButton bSave;

    /** Search field */
    JTextField search;

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

        data = new Data(this);

	// Create GUI
	createGUI();

        state = new State(this);

        // Listener
	frw.listen(state, CharGroup.TYPE, null);
	frw.listen(state, Char.TYPE, null);
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
    public void stop(boolean save) { if (save) state.save(); }

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

        bSave = new JButton("Save");
        bSave.setEnabled(false);
        bSave.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    state.save();
                    ((JButton)e.getSource()).setEnabled(false);
                }
            });

        dataList = new DragTree(dataModel, this);
 	dataList.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
        dataList.setEditable(false);

        search = new JTextField();
        final IncSearchDocListener dl = new IncSearchDocListener(search, dataList);
        search.getDocument().addDocumentListener(dl);
        search.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dl.increment();
                    dl.work(search.getDocument());
                }
            });

        dataList.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    TreePath p = e.getNewLeadSelectionPath();
                    DefaultMutableTreeNode tn = null;
                    if (p != null)
                        tn = (DefaultMutableTreeNode) p.getLastPathComponent();
                    if (tn != null) {
                        dl.setStart(tn.getFirstLeaf());
                    }
                }
            });

        int cellw =
            Integer.parseInt(myProps.getProperty("display.cell.width"));
        int cellh =
            Integer.parseInt(myProps.getProperty("display.cell.height"));

        EquipTableModel model = new EquipTableModel(this);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Other Holders");
        DNDTree display = new DNDTree(new SortedTreeModel(root), this);
        remainder = new TreeTable(display, model, cellh);
        model.ttref = remainder;
        for (int i = 0; i < model.getColumnCount(); i++)
            remainder.getTable().getColumnModel().getColumn(i).setPreferredWidth
                (cellw);

        model = new EquipTableModel(this);
        root = new DefaultMutableTreeNode("Main Holders");
        display = new DNDTree(new SortedTreeModel(root), this);
        treetable = new TreeTable(display, model, cellh);
        model.ttref = treetable;
        for (int i = 0; i < model.getColumnCount(); i++)
            treetable.getTable().getColumnModel().getColumn(i).setPreferredWidth
                (cellw);

        JScrollPane scroll1 = new JScrollPane(treetable);
        scroll1.getVerticalScrollBar().setUnitIncrement(10);
        scroll1.getHorizontalScrollBar().setUnitIncrement(10);

        JScrollPane scroll2 = new JScrollPane(remainder);
        scroll2.getVerticalScrollBar().setUnitIncrement(10);
        scroll2.getHorizontalScrollBar().setUnitIncrement(10);

        JPanel tmp = new JPanel();
        tmp.setLayout(new GridBagLayout());

        GridBagConstraints constr = new GridBagConstraints();
        constr.weightx = 1;
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.gridy = 0;
        tmp.add(bSave, constr);
        constr.gridy = 1;
        constr.weighty = 1;
        constr.fill = GridBagConstraints.BOTH;
        tmp.add(new JScrollPane(dataList), constr);
        constr.gridy = 2;
        constr.weighty = 0;
        constr.fill = GridBagConstraints.HORIZONTAL;
        tmp.add(search, constr);

        JSplitPane sp2 =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll1, scroll2);
        sp2.setResizeWeight(.5);

        JSplitPane sp1 =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tmp, sp2);
        sp1.setResizeWeight(.2);

        constr.gridy = 1;
        constr.weighty = 1;
        constr.fill = GridBagConstraints.BOTH;
        myPanel.add(sp1, constr);
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
	return new String[] { CharGroup.TYPE, Char.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() {
	return new String[0];
    }
}
