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
package harn.index;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.URL;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import rpg.*;
import harn.repository.*;

/**
 * The main edit class for the index plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Editor {
    /** Activated link constant */
    final private HyperlinkEvent.EventType ACTIVATED =
        HyperlinkEvent.EventType.ACTIVATED;

    /** Main reference */
    private Main main;

    /** GUI reference */
    private JPanel myPanel;

    /** Index entries */
    private JTree indexTree;

    /** Model for tree */
    DefaultMutableTreeNode contentTree;
    
    /** Entry save button */
    private JButton bSave;

    /** Menus */
    private JPopupMenu pMenuView;
    private JPopupMenu pMenuIndex;

    /** selected in index - this is ONLY used for editing in the tree. */
    private String currentSel;

    /** selected in index */
    EditLocation editLoc;

    /** Menu entries */
    private JMenuItem miCE; // create entry
    private JMenuItem miRE; // remove entry
    private JMenuItem miIR; // import reference

    private JMenuItem miCSI; // create sub-index
    private JMenuItem miRSI; // remove sub-index

    private JMenuItem miCML; // create map link
    private JMenuItem miRML; // remove map link
    private JMenuItem miLML; // locate map link

    /** Right side */
    JTextField title;
    JEditorPane view;

    /** mouse pressed coordinates */
    private int gx, gy;

    /** Constructor */
    Editor(Main aMain, JPanel aPanel) {
        main = aMain;
        myPanel = aPanel;
    }

    /**
     * Create (and show) Edit GUI.
     */
    public void createGUI() {
        myPanel.setLayout(new GridBagLayout());

        // Save
        bSave = new JButton("Save Entry");
        bSave.setEnabled(false);
        bSave.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        FileWriter w =
                            new FileWriter(view.getPage().getFile());
                        w.write(view.getText(), 0, view.getText().length());
                        w.close();
                    }
                    catch (IOException ex) {
                        // Debug
                        ex.printStackTrace();
                    }
                    setDirty(false);
                }
            });
        bSave.setToolTipText("Save changes to entry");

	// View
	view = new JEditorPane();
        view.registerEditorKitForContentType("text/html", "HTMLEditorKit2"); 
        view.setEditorKitForContentType("text/html", new HTMLEditorKit2());
        view.setMargin(new Insets(10,10,10,10));
	view.addHyperlinkListener(new HyperlinkListener() {
		public void hyperlinkUpdate(HyperlinkEvent e) {
		    if (e.getEventType() == ACTIVATED) {
			try {
                            URL url = view.getPage();
                            if (url != null)
                                main.history.add(url);
                            main.future.clear();
                            String key = main.myData.getKey
                                (e.getURL().toString());
                            if (key.equals("---")) {
                                view.setPage(e.getURL());
                                title.setText(key);
                            }
                            else {
                                TreePath tp = getTreePath(key);
                                indexTree.addSelectionPath(tp);
                                view.setPage(e.getURL());
                            }
                            listen();
			}
			catch(Exception ex) {
			    ex.printStackTrace();
			}
		    }
		}
	    });
        view.setEditable(true);

        pMenuView = new JPopupMenu();
        JMenuItem mi1 = new JMenuItem("Boldface");
        pMenuView.add(mi1);
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    int b = view.getSelectionStart();
                    String txt = view.getSelectedText();
                    view.replaceSelection("");
                    try {
                        HTMLDocument doc = (HTMLDocument)view.getDocument();
                        ((HTMLEditorKit)view.getEditorKit()).insertHTML
                            (doc, b, "<b>" + txt + "</b>", 0, 0, HTML.Tag.B);
                    }
                    catch (Exception ex) {
                        // Debug
                        ex.printStackTrace();
                    }
                }
            });
        JMenuItem mi2 = new JMenuItem("Italics");
        pMenuView.add(mi2);
        mi2.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    int b = view.getSelectionStart();
                    String txt = view.getSelectedText();
                    view.replaceSelection("");
                    try {
                        HTMLDocument doc = (HTMLDocument)view.getDocument();
                        ((HTMLEditorKit)view.getEditorKit()).insertHTML
                            (doc, b, "<i>" + txt + "</i>", 0, 0, HTML.Tag.I);
                    }
                    catch (Exception ex) {
                        // Debug
                        ex.printStackTrace();
                    }
                }
            });

        view.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        pMenuView.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

	// Index
        indexTree = new JTree();
        indexTree.setCellEditor(new MyCellEditor(indexTree));
        indexTree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    TreePath p = e.getNewLeadSelectionPath();
                    if (p == null) return;
                    Object oa[] = p.getPath();
                    MutableTreeNode tn =
                        (MutableTreeNode) oa[oa.length - 1];
                    currentSel = tn.toString();
                    if (oa.length == 4 || oa.length == 5) {
                        String key = "";
                        for (int i = 1; i < 4; i++)
                            key += "/" + oa[i].toString();
                        key = key.substring(1);
                        String s = main.myData.getFile(key);
                        if (s == null) return;
                        try {
                            URL url = view.getPage();
                            view.setPage
                                (new URL("file:" + main.myPath + s));
                            title.setText(key);
                            listen();
                        }
                        catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (oa.length == 5) {
                        TreeNode entry = tn.getParent();
                        TreeNode index = entry.getParent();
                        String tmp = currentSel;
                        String map = tmp.substring(tmp.indexOf(":") + 2);
                        int idx = Integer.parseInt
                            (tmp.substring(0, tmp.indexOf(":") - 1));
                        editLoc = getTempLocation
                            (index.getParent().toString(), index.toString(),
                             entry.toString(), map, idx);
                        main.myFrw.announce(editLoc);
                    }
                }
            });
	indexTree.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
        indexTree.setEditable(true);
        indexTree.setExpandsSelectedPaths(true);
        indexTree.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX(); gy = e.getY();
                        TreePath tp =
                            indexTree.getPathForLocation(gx, gy);
                        if (tp == null) return;
                        int len = tp.getPathCount();
                        miCSI.setEnabled(len == 2); // create sub-index
                        miRSI.setEnabled(len == 3); // remove sub-index
                        miCE.setEnabled(len == 3); // create entry
                        miRE.setEnabled(len == 4); // remove entry
                        miIR.setEnabled(len == 4); // import reference
                        miCML.setEnabled(len == 4); // create map link
                        miRML.setEnabled(len == 5); // remove map link
                        miLML.setEnabled(len == 5); // locate map link
                        pMenuIndex.show(e.getComponent(), gx, gy);
                    }
                }
            });

        pMenuIndex = new JPopupMenu();
        miCE = new JMenuItem("Create Entry");
        pMenuIndex.add(miCE);
        miCE.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath p = indexTree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) p.getLastPathComponent();
                    main.myData.importFile
                        (tn.getParent().toString(), tn.toString(),
                         "New Entry", null, p);
                }
            });
        miRE = new JMenuItem("Remove Entry");
        pMenuIndex.add(miRE);
        miRE.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath p = indexTree.getPathForLocation(gx,gy);
                    Object[] tn = p.getPath();

                    // Create new selection path
                    Object[] tnn = new Object[tn.length - 1];
                    for (int i = 0; i < tnn.length; i++) tnn[i] = tn[i];

                    main.myData.removeEntry
                        (tn[1].toString(), tn[3].toString(), tn[2].toString(),
                         new TreePath(tnn));
                }
            });
        miCSI = new JMenuItem("Create Subindex");
        pMenuIndex.add(miCSI);
        miCSI.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath p = indexTree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) p.getLastPathComponent();
                    int errno = main.myData.createIndex
                        (tn.toString(), "New sub-index", p);
                    switch (errno) {
                        case -1 : error("Subindex exists!"); break;
                        case -2 : error("Cannot write subindex!"); break;
                    }
                }
            });
        miRSI = new JMenuItem("Remove Subindex");
        pMenuIndex.add(miRSI);
        miRSI.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath p = indexTree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) p.getLastPathComponent();
                    main.myData.removeIndex
                        (tn.getParent().toString(), tn.toString(),
                         p.getParentPath());
                }
            });
        miIR = new JMenuItem("Import Reference");
        pMenuIndex.add(miIR);
        miIR.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath p = indexTree.getPathForLocation(gx,gy);
                    Object[] tn = p.getPath();
                    JFileChooser choose = new JFileChooser();
                    choose.setMultiSelectionEnabled(false);
                    int res = choose.showOpenDialog(myPanel);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        File file = choose.getSelectedFile();
                        main.myData.importFile
                            (tn[1].toString(), tn[2].toString(),
                             tn[3].toString(), file, p);
                    }
                }
            });
        miRML = new JMenuItem("Remove map link");
        pMenuIndex.add(miRML);
        miRML.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath p = indexTree.getPathForLocation(gx,gy);
                    Object[] tn = p.getPath();
                    main.myData.removeShape
                        (tn[1].toString(),
                         ((TreeNode)tn[3]).getIndex((TreeNode)tn[4]),
                         tn[3].toString(), tn[2].toString(),
                         p.getParentPath());
                }
            });
        miCML = new JMenuItem("Create map link");
        pMenuIndex.add(miCML);
        miCML.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath p = indexTree.getPathForLocation(gx,gy);
                    Object[] tn = p.getPath();
                    main.myData.createShape
                        (tn[1].toString(), tn[3].toString(), tn[2].toString(),
                         p);
                }
            });
        miLML = new JMenuItem("Locate map link");
        pMenuIndex.add(miLML);
        miLML.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath p = indexTree.getPathForLocation(gx,gy);
                    Object[] tn = p.getPath();
                    String tmp = tn[4].toString();
                    String map = tmp.substring(tmp.indexOf(":") + 2);
                    int idx = Integer.parseInt
                        (tmp.substring(0, tmp.indexOf(":") - 1));
                    editLoc = getTempLocation
                        (tn[1].toString(), tn[2].toString(), tn[3].toString(), map, idx);
                    editLoc.locate();
                }
            });

        // Panels
	JScrollPane sview = new JScrollPane(view);
        sview.getVerticalScrollBar().setUnitIncrement(10);
        sview.getHorizontalScrollBar().setUnitIncrement(10);
	JScrollPane sindex = new JScrollPane(indexTree);
        sindex.getVerticalScrollBar().setUnitIncrement(10);
        sindex.getHorizontalScrollBar().setUnitIncrement(10);

	GridBagConstraints constr1 = new GridBagConstraints();
	constr1.gridx = 0;
	constr1.weightx = 1;
	constr1.fill = GridBagConstraints.BOTH;

	JPanel p1 = new JPanel(new GridBagLayout());
        p1.add(bSave, constr1);
	constr1.weighty = 1;
        p1.add(sindex, constr1);

	GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = constr3.gridy = 0;
	constr3.weightx = 1;
        constr3.weighty = 0;
	constr3.fill = GridBagConstraints.HORIZONTAL;

        JPanel tp = new JPanel(new GridBagLayout());
        title = new JTextField();
        title.setEnabled(false);
        tp.add(title, constr3);
	constr3.gridy = 1;
        constr3.weighty = 1;
	constr3.fill = GridBagConstraints.BOTH;
        tp.add(sview, constr3);

	JSplitPane sp4 = new JSplitPane
            (JSplitPane.HORIZONTAL_SPLIT, p1, tp);
        sp4.setResizeWeight(.2);
	
	myPanel.add(sp4, constr1);

	// If someone turned these value into non-Integers, we throw up
	int w = Integer.parseInt(main.myProps.getProperty("display.width"));
	int h = Integer.parseInt(main.myProps.getProperty("display.height"));
	view.setPreferredSize(new Dimension(w, h));
    }

    /**
     * Create a temporary location for reference of the map editing plugin.
     * @param current index name
     * @param entry entry name
     * @param idx index of shape
     */
    public EditLocation getTempLocation(String current, String index, String entry, String map, int idx) {
        MyLocation loc = main.myData.getLocation
            (current + "/" + index + "/" + entry);
        ArrayList al = new ArrayList();
        al.add(loc.getShapes(map)[idx]);
        Hashtable ht = new Hashtable();
        ht.put(map,al);
        return new EditLocation(main, current, loc, ht, idx, index, entry);
    }

    /**
     * Get index.
     * @return index tree
     */
    public JTree getIndex() {
        return indexTree;
    }

    /**
     * Get index.
     * @return get index tree model
     */
    public TreeModel getIndexModel() {
        return indexTree.getModel();
    }

    /**
     * Set index tree model.
     * @param model model to set
     */
    public void setIndexModel(TreeModel m) {
        indexTree.setModel(m);
        m.addTreeModelListener(new TreeModelListener() {
                public void treeNodesChanged(TreeModelEvent e) {
                    Object[] tn = e.getPath();
                    String nname = e.getChildren()[0].toString();

                    if (tn.length == 2)
                        main.myData.renameIndex
                            (tn[1].toString(), currentSel, nname,
                             new TreePath(tn));
                    if (tn.length == 3)
                        main.myData.renameEntry
                            (tn[1].toString(), currentSel,
                             nname, tn[2].toString(), new TreePath(tn));
                    currentSel = nname;
                }
                public void treeNodesInserted(TreeModelEvent e) {}
                public void treeNodesRemoved(TreeModelEvent e) {}
                public void treeStructureChanged(TreeModelEvent e) {}
            });
    }

    /** Error box */
    private void error(String msg) {
        JOptionPane.showMessageDialog(myPanel, msg);
    }

    /**
     * Set the document.
     */
    public void listen() throws IOException {
        view.getDocument().addDocumentListener(new MyDocumentListener());
    }

    /**
     * Set the dirty flag
     */
    private void setDirty(boolean dirty) {
        bSave.setEnabled(dirty);
    }

    /**
     * Create a tree path to be selected.
     */
    TreePath getTreePath(String key) {
        String[] keys = key.split("/");
        TreePath tp = indexTree.getLeadSelectionPath();
        Object[] oa = tp.getPath();
        int idx = 0;
        for (int j = 0; j < oa.length - 1; j++) {
            TreeNode tn = (TreeNode) oa[j];
            for (int i = 0; i < tn.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode)tn.getChildAt(i);
                if (child.getUserObject().equals(keys[j])) {
                    oa[j + 1] = tn.getChildAt(i);
                    break;
                }
            }
        }
        return new TreePath(oa);
    }

    /**
     * Simple doc listener.
     */
    class MyDocumentListener implements DocumentListener {
        public void insertUpdate(DocumentEvent ev) { setDirty(true); }
        public void removeUpdate(DocumentEvent ev) { setDirty(true); }
        public void changedUpdate(DocumentEvent ev) { setDirty(true); }
    }

    /**
     * Simple editing.
     */
    class MyCellEditor extends DefaultTreeCellEditor {
        MyCellEditor(JTree tree) { super(tree, new DefaultTreeCellRenderer()); }
        public boolean isCellEditable(EventObject ev) {
            if (ev == null) return super.isCellEditable(ev);
            MouseEvent me = (MouseEvent) ev;
            TreePath tp = indexTree.getPathForLocation(me.getX(), me.getY());
            if (tp.getPathCount() == 5) return false;
            return super.isCellEditable(ev);
        }
    }

    /**
     * Synchronous loading.
     */
    class HTMLEditorKit2 extends HTMLEditorKit {
        public Document createDefaultDocument() {
            HTMLDocument doc = (HTMLDocument) super.createDefaultDocument(); 
            doc.setAsynchronousLoadPriority(-1);
            return doc; 
        }
    }

    /**
     * Temporary object for editing.
     */
    class EditLocation extends MyLocation {
        MyLocation ref;
        int idx;
        String index;
        String entry;
        String map;
        String current;
        EditLocation(Main main, String aCurrent, MyLocation aRef, Hashtable ht, int anIdx, String anIndex, String anEntry) {
            super(main, "Edit", ht);
            current = aCurrent;
            ref = aRef;
            idx = anIdx;
            index = anIndex;
            entry = anEntry;
            map = (String)ht.keys().nextElement();
        }
        public boolean isEditing() { return true; }
    }
}
