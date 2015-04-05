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
package harn.magic;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import rpg.*;

/**
 * The main class for the export plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {
    /** GUI references */
    private JPanel myPanel;

    /** Tree and modifiers */
    protected TreeTable displaymods;

    /** Display of modifiers */
    protected SpellTableModel modsModel;

    /** Display of spells */
    protected JEditorPane otherdisplay;

    /** My properties */
    private Properties myProps;

    /** scrollpane */
    private JScrollPane scroll;

    /** Path to my data */
    protected String myPath;

    /** Framework reference */
    private Framework myFrw;

    /** State object */
    protected State state;

    /** Data object */
    protected Data data;

    /** Development */
    protected Develop develop;

    /** Data GUI */
    protected DragTree tree;

    /** Menu to start development */
    private JPopupMenu pDevelop;

    /** Menu to stop development */
    private JPopupMenu pAbort;

    /** Menu for deletion */
    private JPopupMenu pDelete;

    /** Menu to start adaption of spells to caster */
    private AdaptMenu pAdapt;

    /** Saving */
    protected JButton bSave;

    /** Toggle spell */
    private JButton bToggle;

    /** Button to show information about development stage */
    protected JButton bOptions;

    /** Location of popup */
    private int gx,gy;

    /** Search field */
    private JTextField search;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.4f; }

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
        develop = new Develop(this);

	// Create GUI
	createGUI();

        state = new State(this);

        // Listener
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
     * Called by framework when terminating. We never save anything. We're
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
        bSave.setMnemonic(KeyEvent.VK_S);
        bSave.setEnabled(false);
        bSave.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    state.save();
                    ((JButton)e.getSource()).setEnabled(false);
                }
            });

        bToggle = new JButton("Toggle");
        bToggle.setMnemonic(KeyEvent.VK_T);
        bToggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (scroll.getViewport().getView() == displaymods) {
                        scroll.getViewport().setView(otherdisplay);
                        otherdisplay.setCaretPosition(0);
                    }
                    else
                        scroll.getViewport().setView(displaymods);
                    scroll.getVerticalScrollBar().setValue(0);
                }
            });
        bToggle.setToolTipText("Switch between spell descriptions and Character list");

        search = new JTextField();
        final IncSearchDocListener dl = new IncSearchDocListener(search, tree);
        search.getDocument().addDocumentListener(dl);
        search.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dl.increment();
                    dl.work(search.getDocument());
                }
            });

        tree.addTreeSelectionListener(new TreeSelectionListener() {
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


        pDevelop = new JPopupMenu();
        JMenuItem mi1 = new JMenuItem("Develop");
        pDevelop.add(mi1);
        mi1.addActionListener(new ActionListener() {
                boolean enable = false;
		public void actionPerformed(ActionEvent e) {
                    TreePath tp =
                        displaymods.getTree().getClosestPathForLocation(gx, gy);
                    develop.start
                        ((String)((Holder)tp.getLastPathComponent()).getUserObject());
                }
            });

        pAbort = new JPopupMenu();
        mi1 = new JMenuItem("Abort Develop");
        pAbort.add(mi1);
        mi1.addActionListener(new ActionListener() {
                boolean enable = false;
		public void actionPerformed(ActionEvent e) {
                    TreePath tp =
                        displaymods.getTree().getClosestPathForLocation(gx, gy);
                    develop.stop();
                }
            });

        pDelete = new JPopupMenu();
        JMenuItem mi2 = new JMenuItem("Delete");
        pDelete.add(mi2);
        mi2.addActionListener(new ActionListener() {
                boolean enable = false;
		public void actionPerformed(ActionEvent e) {
                    TreePath tp =
                        displaymods.getTree().getClosestPathForLocation(gx, gy);
                    getModel().removeNodeFromParent((Spell)tp.getLastPathComponent());
                }
            });

        pAdapt = new AdaptMenu(this);

        bOptions = new JButton("-");
        bOptions.setEnabled(false);
        bOptions.setToolTipText("Spell options points left during development");

        otherdisplay = new JEditorPane();
	otherdisplay.setEditable(false);
        otherdisplay.setMargin(new Insets(10,10,10,10));
        otherdisplay.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        pAdapt.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });

        DNDTree display = new DNDTree
            (new SortedTreeModel(new DefaultMutableTreeNode("Casters")), this);
        display.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX();
                        gy = e.getY();
                        TreePath tp =
                            displaymods.getTree().getClosestPathForLocation(gx, gy);
                        if (tp.getLastPathComponent() instanceof Spell) {
                            pDelete.show(e.getComponent(), gx, gy);
                        }
                        if (tp.getLastPathComponent() instanceof Holder) {
                            Holder h = (Holder)tp.getLastPathComponent();
                            if (develop == null || develop.developing == null) {
                                pDevelop.show(e.getComponent(), gx, gy);
                            }
                            else {
                                if (h.getUserObject().equals(develop.developing))
                                    pAbort.show(e.getComponent(), gx, gy);
                            }
                        }
                    }
                }
            });

        modsModel = new SpellTableModel(this);
        int cellh = Integer.parseInt(myProps.getProperty("display.cell.height"));
        displaymods = new TreeTable(display, modsModel, cellh);

        int totw = Integer.parseInt(myProps.getProperty("display.width"));
        for (int i = 0; i < modsModel.getColumnCount(); i++)
            displaymods.getTable().getColumnModel().getColumn(i).setPreferredWidth
                (totw/modsModel.getColumnCount());

        scroll = new JScrollPane(displaymods);
        scroll.getVerticalScrollBar().setUnitIncrement(10);
        scroll.getHorizontalScrollBar().setUnitIncrement(10);

        tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    TreePath p = e.getNewLeadSelectionPath();
                    if (p == null) return;
                    MutableTreeNode tn = (MutableTreeNode) p.getLastPathComponent();
                    if (tn != null && tn.isLeaf()) {
                        String key = tn.toString();
                        String page = ((Data.DataTreeNode)tn).getLink();
                        if (page == null) return;
                        try {
                            URL url = new URL("file:" + myPath + page);
                            otherdisplay.setPage(url);
                            pAdapt.recalc(key);
                        }
                        catch (Exception ex) {
                            // Debug
                            ex.printStackTrace();
                        }
                    }
                }
            });

        JPanel tmp = new JPanel();
        tmp.setLayout(new GridBagLayout());
        
        GridBagConstraints constr = new GridBagConstraints();
        constr.weightx = 1;
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.gridy = 0;
        tmp.add(bSave, constr);
        constr.gridy = 1;
        tmp.add(bToggle, constr);

        constr.gridy = 2;
        constr.weighty = 1;
        constr.fill = GridBagConstraints.BOTH;
        tmp.add(new JScrollPane(tree), constr);

        constr.gridy = 3;
        constr.weighty = 0;
        tmp.add(search, constr);

        constr.gridy = 4;
        tmp.add(bOptions, constr);

        constr.weighty = 1;
        JSplitPane sp =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tmp, scroll);
        sp.setResizeWeight(.2);
        myPanel.add(sp, constr);
    }

    /**
     * Get caster tree root.
     */
    protected SortedTreeModel getModel() {
        return (SortedTreeModel)displaymods.getTree().getModel();
    }

    /**
     * Allow announcement.
     */
    public void announce(Spell spell) {
        myFrw.announce(spell.getExport());
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
	return new String[] { harn.repository.Spell.TYPE } ;
    }
}
