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
package harn.chars;

import harn.repository.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;
import javax.swing.tree.*;
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
    JPanel myPanel;

    /** My properties */
    Properties myProps;

    /** Path to my data */
    String myPath;

    /** Character data entry object */
    private Data myData;

    /** Character state entry object */
    private State myState;

    /** development button list */
    private ArrayList devButtonList;

    /** Send to active group menu item */
    private JMenuItem miActive;

    /** Send to combat */
    private JMenuItem miCombat;

    /** Export to harnmaker */
    private JMenuItem miHMExport;

    /** Print character sheet */
    private JMenuItem miPrint;

    /** Button to export character to HTML */
    private JMenuItem miExport;

    /** Print sysnopsis */
    private JMenuItem miSPrint;

    // Right GUI items

    /** Print helper */
    private Printer myPrint;

    /** Standard display */
    private JPanel myDisplay;

    /** Read only display */
    private PrintPane myRODisplay;

    /** Panel that holds displays */
    private JScrollPane view;

    // Left GUI items

    /** Compound panel */
    private JPanel leftPanel;

    /** Save button */
    JButton bSave;

    /** create/remove menu */
    private JPopupMenu pMenu;

    /** synopsis menu */
    private JPopupMenu vMenu;

    /** mouse pressed coordinates */
    private int gx, gy;

    /** Button to roll char attributes */
    private JButton bRoll;

    /** Button to clear all attributes */
    private JButton bClearAt;

    /** Button to clear all skills */
    private JButton bClearSk;

    /** Button to show information about development stage */
    JButton bOptions;

    /** Button to show current character code */
    JButton bCode;

    /** Button to start development */
    private JButton bDevelop;

    /** Button to finalize (and recalc SBs for) skills */
    JButton bFinal;

    /** Button to import character from HarnMaker */
    private JButton bImportHM;

    /** Toggle Read only display */
    private JButton bToggle;

    /** Tree model for character list */
    TreeNode chartree;

    /** Tree for character list */
    DNDTree guitree;

    /** Hide branches in skill tree */
    boolean hideb;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     * @return version
     */
    public float version() { return 0.11f; }

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
        hideb = "true".equals(myProps.getProperty("branch.hide"));

        // create GUI
        createGUI();

        // State object
        myState = new State(this);
        myPrint = new Printer(this);

        // Data holder
        myData = new Data(this);
        myState.load();

        // Register listeners
        frw.listen(myState, TimeFrame.TYPE, null);
        frw.listen(myState, CharGroup.TYPE, null);
        frw.listen(myState, Equipment.TYPE, null);
        frw.listen(myState, Spell.TYPE, null);

        myState.setDirty(false);
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
        if (save && myState.getDirty())
            myState.save(chartree, null, null);
    }

    /**
     * Called by the framework when about to terminate.
     * @return state's dirty flag
     */
    public boolean hasUnsavedParts() { return myState.getDirty(); }

    /**
     * Create (and show) GUI.
     */
    private void createGUI() {
        // The left panel is filled with buttons
        devButtonList = new ArrayList();

        myPanel.setLayout(new GridBagLayout());
        leftPanel = new JPanel(new GridBagLayout());

        bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
        bSave.setEnabled(false);
        bSave.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myState.save(chartree, null, null);
                }
            });
        
        pMenu = new JPopupMenu();
        JMenuItem mi1 = new JMenuItem("Create Char");
        pMenu.add(mi1);
        mi1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (tn != null) {
                        if (tn.isLeaf()) tn = (TreeNode) tn.getParent();
                        myState.create(tn);
                        myState.setDirty(true);
                    }
                }
            });
        JMenuItem mi2 = new JMenuItem("Create Group");
        pMenu.add(mi2);
        mi2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (tn != null) {
                        if (tn.isLeaf()) tn = (TreeNode) tn.getParent();
                        myState.setDirty(true);
                        TreeNode newNode = new TreeNode("New Group", false);
                        ((SortedTreeModel)guitree.getModel()).insertNodeSorted
                            (newNode, tn);
                        guitree.scrollPathToVisible(new TreePath(newNode.getPath()));
                    }
                }
            });
        miActive = new JMenuItem("Add to active");
        pMenu.add(miActive);
        miActive.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    myState.getAct().addMember(tn.toString());
                }
            });
        miHMExport = new JMenuItem("Save as Harnmaker file");
        pMenu.add(miHMExport);
        miHMExport.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    myState.exportHM(tn.toString());
                }
            });
        miPrint = new JMenuItem("Print");
        pMenu.add(miPrint);
        miPrint.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    myState.export(tn.toString());
                    myPrint.print(tn.toString());
                }
            });
        miCombat = new JMenuItem("Send to combat");
        pMenu.add(miCombat);
        miCombat.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (tn == null || !tn.isLeaf()) return;
                    myState.makeCombatant(tn.toString());
                }
            });
        miExport = new JMenuItem("Export HTML");
        miExport.setToolTipText("Write (Web) character sheet");
        pMenu.add(miExport);
        miExport.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (tn == null || !tn.isLeaf()) return;
                    myState.export(tn.toString());
                }
            });

        JMenuItem mi3 = new JMenuItem("Clone");
        pMenu.add(mi3);
        mi3.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (tn == null || !tn.isLeaf()) return;
                    myState.clone(tn);
                    
                }
            });
        pMenu.addSeparator();
        JMenuItem mi4 = new JMenuItem("Remove");
        pMenu.add(mi4);
        mi4.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = guitree.getPathForLocation(gx,gy);
                    TreeNode tn = (TreeNode) tp.getLastPathComponent();
                    if (tn != null) {
                        if (tn.isLeaf()) myState.remove(tn.toString());
                        myState.setDirty(true);
                        // Update GUI
                        ((DefaultTreeModel)guitree.getModel()).removeNodeFromParent
                            ((MutableTreeNode)tn);
                    }
                }
            });

        vMenu = new JPopupMenu();
        miSPrint = new JMenuItem("Print");
        vMenu.add(miSPrint);
        miSPrint.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPrintable(myRODisplay);
                    try {
                        if (job.printDialog()) job.print();
                    }
                    catch (PrinterException ex) {
                        // Debug
                        ex.printStackTrace();
                    }
                }
            });


        bRoll = new JButton("Roll Attributes");
        bRoll.setMnemonic(KeyEvent.VK_R);
        bRoll.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myState.enableAnnounce(false);
                    myData.roll();
                    myState.enableAnnounce(true);
                    myState.setDirty(true);
                }
            });
        bRoll.setToolTipText("Random roll for all attributes");
        bRoll.setEnabled(false);

        bClearAt = new JButton("Clear Attributes");
        bClearAt.setMnemonic(KeyEvent.VK_A);
        bClearAt.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myState.enableAnnounce(false);
                    myData.clearAttrs();
                    myState.clearAttributes();
                    myState.enableAnnounce(true);
                }
            });
        bClearAt.setToolTipText("Clear all attribute sections");
        bClearAt.setEnabled(false);

        bClearSk = new JButton("Clear Skills");
        bClearSk.setMnemonic(KeyEvent.VK_C);
        bClearSk.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myState.enableAnnounce(false);
                    myData.clearSkills();
                    myState.clearSkills();
                    myState.enableAnnounce(true);
                }
            });
        bClearSk.setToolTipText("Clear all skill sections");
        bClearSk.setEnabled(false);

        bImportHM = new JButton("Import HarnMaker");
        bImportHM.setMnemonic(KeyEvent.VK_H);
        bImportHM.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myState.importHM();
                }
            });
        bImportHM.setToolTipText("Import a HarnMaker character");

        bToggle = new JButton("Toggle Synopsis");
        bToggle.setMnemonic(KeyEvent.VK_T);
        bToggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (guitree.getLastSelectedPathComponent() != null &&
                        ((TreeNode)guitree.getLastSelectedPathComponent()).isLeaf()) {
                        if (view.getViewport().getView() == myDisplay) {
                            String txt = myState.exportMini();
                            myRODisplay.setText(txt);
                            view.getViewport().setView(myRODisplay);
                            myRODisplay.setCaretPosition(0);
                            view.getVerticalScrollBar().setValue(0);
                        }
                        else
                            view.getViewport().setView(myDisplay);
                        view.getVerticalScrollBar().setValue(0);
                    }
                }
            });
        bToggle.setToolTipText("Switch view: full numbers or selected overview");
        bToggle.setEnabled(false);

        bCode = new JButton("-");
        bCode.setEnabled(false);
        bCode.setToolTipText("Code for exported character sheets");

        bOptions = new JButton("-");
        bOptions.setEnabled(false);
        bOptions.setToolTipText("Status/Options during development");

        bDevelop = new JButton("Develop");
        bDevelop.setMnemonic(KeyEvent.VK_D);
        bDevelop.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (devButtonList.size() > 0) {
                        bOptions.setText("- FATE -");
                        Object[] ba = (Object[])devButtonList.get(0);
                        ((AbstractButton)ba[0]).setEnabled(true);
                        myData.resetDevelopment();
                        bDevelop.setEnabled(false);
                        bFinal.setEnabled(true);
                    }
                }
            });
        bDevelop.setToolTipText("Develop the character");

        bFinal = new JButton("Finalize");
        bFinal.setMnemonic(KeyEvent.VK_F);
        bFinal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myState.enableAnnounce(false);
                    myData.finish();
                    myState.enableAnnounce(true);
                    bDevelop.setEnabled(true);
                    bFinal.setEnabled(false);
                    bOptions.setText(" - ");
                    for (int i = 0; i < devButtonList.size(); i++) {
                        Object[] ba = (Object[])devButtonList.get(i);
                        if (!((Boolean)ba[1]).booleanValue()) {
                            ((AbstractButton)ba[0]).setEnabled(false);
                        }
                        ((AbstractButton)ba[0]).setSelected(false);
                    }
                }
            });
        bFinal.setEnabled(false);
        bFinal.setToolTipText("Finalize development");

        chartree = new TreeNode("Character List", false);
        guitree = new DNDTree(new SortedTreeModel(chartree), this);
        guitree.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
        guitree.setEditable(true);
        guitree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    TreePath p = e.getNewLeadSelectionPath();
                    TreeNode tn = (p != null ? (TreeNode) p.getLastPathComponent() : null);
                    if (tn != null && tn.isLeaf()) {
                        myState.select(tn.toString());
                        myRODisplay.setCaretPosition(0);
                        view.getVerticalScrollBar().setValue(0);
                    }
                }
            });
        guitree.getModel().addTreeModelListener(new TreeModelListener() {
                public void treeNodesChanged(TreeModelEvent e) {
                    String newname = e.getChildren()[0].toString();
                    if (((TreeNode)e.getChildren()[0]).isLeaf())
                        myState.setName(newname); 
                    myState.setDirty(true);
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
                        TreePath tp =
                            guitree.getClosestPathForLocation(gx, gy);
                        boolean isLeaf =
                            ((DefaultMutableTreeNode)tp.getLastPathComponent()).isLeaf();
                        miActive.setEnabled(isLeaf);
                        miCombat.setEnabled(isLeaf);
                        miHMExport.setEnabled(isLeaf);
                        miPrint.setEnabled(isLeaf);
                        miExport.setEnabled(isLeaf);
                        miActive.setEnabled(isLeaf);
                        pMenu.show(e.getComponent(), gx, gy);
                    }
                }
            });

	// Index
        guitree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    bToggle.setEnabled(false);
                    bRoll.setEnabled(false);
                    bClearAt.setEnabled(false);
                    bClearSk.setEnabled(false);

                    TreePath p = e.getNewLeadSelectionPath();
                    if (p == null) return;
                    MutableTreeNode tn = (MutableTreeNode) p.getLastPathComponent();
                    if (tn == null || !tn.isLeaf()) return;

                    bToggle.setEnabled(true);
                    bRoll.setEnabled(true);
                    bClearAt.setEnabled(true);
                    bClearSk.setEnabled(true);
                }
            });

        GridBagConstraints constr2 = new GridBagConstraints();
        constr2.gridx = 0;
        constr2.weightx = 1;
        constr2.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(bToggle, constr2);
        leftPanel.add(bRoll, constr2);
        leftPanel.add(bClearAt, constr2);
        leftPanel.add(bClearSk, constr2);
        leftPanel.add(bImportHM, constr2);
        leftPanel.add(bSave, constr2);
        leftPanel.add(bCode, constr2);

        // We add an expanding DNDTree.
        GridBagConstraints constr3 = new GridBagConstraints();
        constr3.gridx = 0;
        constr3.fill = GridBagConstraints.BOTH;
        constr3.weighty = constr3.weightx = 1;
        leftPanel.add(new JScrollPane(guitree), constr3);
        // Option info
        leftPanel.add(bOptions, constr2);
        leftPanel.add(bDevelop, constr2);
        leftPanel.add(bFinal, constr2);

        GridBagConstraints constr4 = new GridBagConstraints();
        constr4.fill = GridBagConstraints.BOTH;
        constr4.weightx = constr4.weighty = 1;
        constr4.insets = new Insets(6,5,6,5);

        myDisplay = new JPanel(new GridBagLayout());
        try {
            myRODisplay = new PrintPane();
            myRODisplay.setContentType("text/html");
            myRODisplay.setMargin(new Insets(10,10,10,10));
            HTMLDocument doc = (HTMLDocument) myRODisplay.getDocument();
            doc.setBase(new URL("file:" + Framework.osName(myPath + "../")));
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
        myRODisplay.setEditable(false);

        myRODisplay.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger())
                        vMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            });

        view = new JScrollPane(myDisplay);
        view.getVerticalScrollBar().setUnitIncrement(10);
        view.getHorizontalScrollBar().setUnitIncrement(10);

        JSplitPane sp =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, view);
        sp.setResizeWeight(.2);
        myPanel.add(sp, constr4);

        // If someone turned these value into non-Integers, we throw up
        int w = Integer.parseInt(myProps.getProperty("display.width"));
        int h = Integer.parseInt(myProps.getProperty("display.height"));
        view.setPreferredSize(new Dimension(w, h));
    }

    /**
     * Returns the state object.
     * @return state object
     */
    public State getState() { return myState; }

    /**
     * Returns the data object.
     * @return data object
     */
    public Data getData() { return myData; }

    /**
     * Returns the display object.
     * @return display object
     */
    public JPanel getDisplay() { return myDisplay; }

    /**
     * Returns the RO display object.
     * @return RO display object
     */
    public JEditorPane getRODisplay() { return myRODisplay; }

    /**
     * Returns the view object.
     * @return view object
     */
    public JScrollPane getView() { return view; }

    /**
     * Add a button.
     * @param b button to add
     */
    public void addButton(AbstractButton b, boolean a) {
        GridBagConstraints constr2 = new GridBagConstraints();
        constr2.gridx = 0;
        constr2.fill = GridBagConstraints.HORIZONTAL;
        devButtonList.add(new Object[] { b, new Boolean(a) });
        leftPanel.add(b, constr2);
        if (!a) b.setEnabled(false);
    }

    /**
     * Enable next development button.
     */
    public void enableNextButton() {
        for (int i = 0; i < devButtonList.size(); i++) {
            Object[] ba = (Object[])devButtonList.get(i);
            if (((AbstractButton)ba[0]).isSelected() ||
                ((AbstractButton)ba[0]).isEnabled()) {
                enableNextButton((AbstractButton)ba[0]);
                return;
            }
        }

        // Finalize, if all buttons are done
        bDevelop.doClick();
        bOptions.setText("-");        
    }

    /**
     * Enable next development button.
     */
    public void disableThisButton(AbstractButton b) {
        for (int i = 0; i < devButtonList.size() - 1; i++) {
            Object[] ba = (Object[])devButtonList.get(i);
            if (b == ba[0]) {
                b.setSelected(true);
                if (!((Boolean)ba[1]).booleanValue()) b.setEnabled(false);
                return;
            }
        }
    }

    /**
     * Enable next development button.
     * @param b button to disable and take next from
     */
    public void enableNextButton(AbstractButton b) {
        b.setSelected(false);
        for (int i = 0; i < devButtonList.size() - 1; i++) {
            Object[] ba = (Object[])devButtonList.get(i);
            if (b == ba[0]) {
                bOptions.setText("- FATE -");
                ba = (Object[])devButtonList.get(i+1);
                ((AbstractButton)ba[0]).setEnabled(true);
                return;
            }
        }

        // Finalize, if all buttons are done
        bDevelop.doClick();
        bOptions.setText("-");
    }

    /**
     * Provide Framework.
     * @return framework reference
     */
    public Framework getFramework() { return myFrw; }

    /**
     * Each plugin must provide an array of objects it requires.
     * @return list of imports
     */
    public String[] requires() { return new String[] { TimeFrame.TYPE }; }

    /**
     * Each plugin must provide an array of objects it uses.
     * @return list of imports
     */
    public String[] uses() { return new String[] { Equipment.TYPE, Sketch.TYPE, Spell.TYPE }; }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() { return new String[] { Char.TYPE }; }

    static class TreeNode extends DefaultMutableTreeNode {
        /** Character? */
        boolean isChar;
        /** Constructor */
        TreeNode(String name, boolean newIsChar) {
            super(name);
            isChar = newIsChar;
        }
        /** return whether character = leaf or not */
        public boolean isLeaf() { return isChar; }
        public boolean getAllowsChildren() { return !isChar; }
    }

    class PrintPane extends JEditorPane implements Printable {
        public int print (Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            double sh = pf.getImageableHeight() / getSize().height;
            double sw = pf.getImageableWidth() / getSize().width;

            Graphics2D g2 = (Graphics2D)g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.scale(Math.min(sw,sh), Math.min(sw,sh));

            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            paint(g);
            return Printable.PAGE_EXISTS;
        }
    }
}
