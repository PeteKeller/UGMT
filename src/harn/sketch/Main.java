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
package harn.sketch;

import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import rpg.*;
import org.w3c.dom.*;
import javax.xml.transform.stream.StreamResult;

/**
 * The main class for the sketch plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {
    /** GUI references */
    JPanel myPanel;

    /** My properties */
    private Properties myProps;

    /** Path to my data */
    String myPath;

    /** My data */
    Data myData;

    /** Drawable */
    Drawing myDrawing;
    DrawingHTML myDrawingHtml;
    private JScrollPane scrollDrawing;

    /** Framework reference */
    Framework myFrw;

    /** Left GUI items */
    JButton bSave;
    private JButton bSaveExp;
    JButton bSaveSyn;
    private JButton bClear;
    TreeNode sketchtree;
    DNDTree guitree;
    private JPopupMenu pMenu;

    /** mouse pressed coordinates */
    private int gx, gy;

    /** Right GUI items */
    private JScrollPane draw;
    private JCheckBox cbExport;
    private JTextField tfFullname;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.7f; }

    /**
     * Init method called by Framework. Init all objects and then create
     * GUI. Register listeners.
     * @param frw framework
     * @param panel my GUI root object
     * @param props my properties
     * @param path path to my directory
     */
    public void start(Framework frw, JPanel panel, Properties props, String path) {
        //JEditorPane.registerEditorKitForContentType
        //("text/html", "MyEditorKit");

	// Useful references
	myFrw = frw;
        myPanel = panel;
	myProps = props;
	myPath = path;
	myData = new Data(this);

	// Create GUI
	createGUI();

        // Set the first sketch active; default
        guitree.addSelectionInterval(1,1);
        frw.announce(sketchtree.sketch);
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
	if (save && myData.getDirty())
            myData.save();
    }

    /**
     * Called by the framework when about to terminate. We never save
     * anything. See above.
     * @return false
     */
    public boolean hasUnsavedParts() { return myData.getDirty(); }

    /**
     * Create (and show) GUI.
     */
    private void createGUI() {
	// The left panel is filled with buttons

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
		    myData.save();
		}
	    });

        bSaveExp = new JButton("Save Exports");
        bSaveExp.setMnemonic(KeyEvent.VK_E);
        bSaveExp.setEnabled(false);
	bSaveExp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    Document doc = Framework.newDoc();
                    doc.appendChild(doc.createElement("sketch"));
                    myData.saveExports
                        (sketchtree, doc.getDocumentElement(), doc);
                    File file = new File(myPath + "state.xml");
                    Framework.backup(myPath + "state.xml");
                    Framework.transform(doc, new StreamResult(file), null);
                    bSaveExp.setEnabled(false);
		}
	    });

        bSaveSyn = new JButton("Save Synopsis");
        bSaveSyn.setMnemonic(KeyEvent.VK_Y);
        bSaveSyn.setEnabled(false);
	bSaveSyn.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    myData.saveSynopsis(sketchtree);
                    bSaveSyn.setEnabled(false);
		}
	    });

        bClear = new JButton("Clear");
        bClear.setMnemonic(KeyEvent.VK_C);
	bClear.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    myDrawing.clear();
                    myDrawingHtml.clear();
		}
	    });
        bClear.setToolTipText("Clear the current sketch entry if writable");

	guitree = new DNDTree(new SortedTreeModel(sketchtree), this);
	guitree.getSelectionModel().setSelectionMode
	    (TreeSelectionModel.SINGLE_TREE_SELECTION);
        guitree.setEditable(true);
	guitree.addTreeSelectionListener(new TreeSelectionListener() {
		public void valueChanged(TreeSelectionEvent e) {
                    TreePath p = e.getNewLeadSelectionPath();
                    if (p == null) return;
		    TreeNode tn = (TreeNode) p.getLastPathComponent();
                    if (tn.getTxt() != null) {
                        myDrawingHtml.set(null);
                        myDrawing.set(tn);
                        scrollDrawing.setViewportView(myDrawing);
                    }
                    else {
                        myDrawing.set(null);
                        myDrawingHtml.set(tn);
                        scrollDrawing.setViewportView(myDrawingHtml);
                    }
                    cbExport.setSelected(tn.sketch.export);
                    tfFullname.setText(fullName(tn));
		}
	    });
        guitree.getModel().addTreeModelListener(new TreeModelListener() {
                public void treeNodesChanged(TreeModelEvent e) {
                    TreeNode tn = (TreeNode)e.getChildren()[0];
                    String newname = tn.toString();
                    String oldname = tn.rename(newname);
                    if (oldname != null && !oldname.equals(newname)) {
                        tn.setUserObject(oldname);
                    }
                    tfFullname.setText(fullName(tn));
                }
                public void treeNodesInserted(TreeModelEvent e) {}
                public void treeNodesRemoved(TreeModelEvent e) {}
                public void treeStructureChanged(TreeModelEvent e) {}
            });

        guitree.addMouseListener(new MouseAdapter() {
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
        JMenuItem mi1 = new JMenuItem("Add Note");
        pMenu.add(mi1);
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
		    TreeNode tn = (TreeNode) tp.getLastPathComponent();
		    if (tn != null && tn.isLeaf()) tn = (TreeNode) tn.getParent();
                    if (tn != null) myData.createNote(tn);
                }
            });

        JMenuItem mi2 = new JMenuItem("Remove");
        pMenu.add(mi2);
        mi2.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
		    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    // Don't kill the root!
		    if (tn != sketchtree && tn != null) myData.remove(tn);
		}
	    });

        JMenuItem mi3 = new JMenuItem("Add Group");
        pMenu.add(mi3);
        mi3.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
		    TreeNode tn = (TreeNode) tp.getLastPathComponent();
		    if (tn != null && tn.isLeaf()) tn = (TreeNode) tn.getParent();
                    if (tn != null) myData.createDirectory(tn);
                }
            });

        GridBagConstraints constr2 = new GridBagConstraints();
	constr2.gridx = 0;
	constr2.weightx = 1;
	constr2.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(bSave, constr2);
        leftPanel.add(bSaveExp, constr2);
        leftPanel.add(bSaveSyn, constr2);
        leftPanel.add(bClear, constr2);

	// We add an expanding JTree.
        GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = 0;
	constr3.gridy = GridBagConstraints.RELATIVE;
	constr3.fill = GridBagConstraints.BOTH;
	constr3.weighty = 1;
        leftPanel.add(new JScrollPane(guitree), constr3);

	// The right panel only contains a drawable.  The sketchpad is
	// displayed in a scrollable area.

	GridBagConstraints constr4 = new GridBagConstraints();
	constr4.fill = GridBagConstraints.BOTH;
	constr4.gridwidth = 2;
	constr4.weightx = constr4.weighty = 1;

	// Dummy panels
	// If someone turned these value into non-Integers, we throw up
	int w = Integer.parseInt(myProps.getProperty("sketch.width"));
	int h = Integer.parseInt(myProps.getProperty("sketch.height"));

	JPanel rightPanel = new JPanel(new GridBagLayout());

	myDrawing = new Drawing(this, w, h);
        myDrawing.setToolTipText
            ("<html>shift-left-click/drag draws<br>" + 
             "shift-right-click/drag erases<br>"+ 
             "enter text by typing</html>");
	myDrawingHtml = new DrawingHTML(this, w, h);
        scrollDrawing = new JScrollPane(myDrawing);
        scrollDrawing.getVerticalScrollBar().setUnitIncrement(10);
        scrollDrawing.getHorizontalScrollBar().setUnitIncrement(10);

        // Export box
        cbExport = new JCheckBox("Export path");
        cbExport.setHorizontalAlignment(SwingConstants.RIGHT);
        cbExport.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (myDrawing.treeref != null)
                        myDrawing.treeref.sketch.export =
                            ((JCheckBox)e.getSource()).isSelected();
                    if (myDrawingHtml.treeref != null)
                        myDrawingHtml.treeref.sketch.export =
                            ((JCheckBox)e.getSource()).isSelected();
                    bSaveExp.setEnabled(true);
                    bSaveSyn.setEnabled(true);
                }
            });
        JPanel tmp = new JPanel(new GridBagLayout());

        rightPanel.add(scrollDrawing, constr4);
	constr2.gridy = 1;
        rightPanel.add(tmp, constr2);

        // Full name
        tfFullname = new JTextField();
        tfFullname.setEditable(false);

        JLabel phys = new JLabel("True path ");
        phys.setHorizontalAlignment(SwingConstants.RIGHT);

	constr2.fill = GridBagConstraints.HORIZONTAL;
	constr2.gridy = 0;
	constr2.gridx = 0;
	constr2.weightx = 0;
        tmp.add(cbExport, constr2);
	constr2.gridx = 1;
	constr2.weightx = 1;
        tmp.add(tfFullname, constr2);

	constr4.insets = new Insets(6,5,6,5);
        JSplitPane sp =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        sp.setResizeWeight(.2);
	myPanel.add(sp, constr4);

	w = Integer.parseInt(myProps.getProperty("display.width"));
	h = Integer.parseInt(myProps.getProperty("display.height"));
	rightPanel.setPreferredSize(new Dimension(w, h));
    }

    /**
     * Stringify a path.
     */
    public String fullName(TreeNode tn) {
        if (tn == null) return "";
        return fullName((TreeNode)tn.getParent()) + "/" +
            tn.getUserObject().toString();
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
        return new String[0];
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() {
	return new String[0];
    }
}
