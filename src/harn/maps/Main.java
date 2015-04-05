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
package harn.maps;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.io.File;
import rpg.*;

/**
 * The main class for the maps plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {
    /** GUI references */
    JPanel myPanel;

    /** My properties */
    Properties myProps;

    /** Path to my data */
    static String myPath;

    /** Simple state object */
    private State state;

    /** Random content exposition */
    private RandomContent randContent;

    /** My data */
    Data myData;

    /** Framework reference */
    Framework myFrw;

    /** Left (main) GUI items */
    private JButton bCenter;
    private JButton bActive;
    JCheckBox bSuppExp;
    JButton bFog;
    JButton bToggle;
    JSpinner bScaleSpin;
    JButton bSave;

    /** Left (navigate) GUI items */
    DefaultMutableTreeNode maptree;
    private JButton forward;
    private JButton back;
    JButton bDist;
    JTree tree;

    /** Left (manipulate) GUI items */
    private JButton bSaveFog;
    private JButton bLoadFog;
    JRadioButton bPoly;
    JRadioButton bCirc;
    JRadioButton bRect;
    private JButton bLayers;
    private JButton bFogs;
    private JPopupMenu pShowLayerMenu;
    private JPopupMenu pFogLayerMenu;
    private JPopupMenu pRandomMenu;
    JCheckBox bFrames;
    private JButton bFill;
    JCheckBox bFullInfo;
    JCheckBox bShowFog;

    /** Used for moving back/forth*/
    private boolean linear = false;

    /** Right GUI items */
    JScrollPane rightPanel;
    MapHolder map;
    DirMapHolder dirMap;

    /** Use in edit mode*/
    JButton bExport;
    Editor editor;
    JPopupMenu eMenu;
    JMenuItem miCD;
    JMenuItem miCM;

    /** regular move listener */
    MoveByMenu moveMenu;
    MouseAdapter moveMA;

    /** fog edit move listener */
    FogByMouse fogMouse;

    /** Information move listener */
    InfoByMouse infoMouse;
    
    /** popup coordinates */
    int gx,gy;

    /** history list of maps visited*/
    private MyLinkedList history;

    /** future list of maps visited */
    MyLinkedList future;

    /** Maximum number of elements in the future/history lists */
    private int histSize;

    /** selected in index - this is ONLY used for editing in the tree. */
    private String currentSel;

    /** incremental index search listener */
    private IncSearchDocListener sMapListener;
    private JTextField search;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.11f; }

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
        history = new MyLinkedList();
        future = new MyLinkedList();
        histSize = Integer.parseInt(props.getProperty("history.size"));

        // Create GUI
        createGUI();

        myData = new Data(this, frw);
        state = new State(this);
        randContent = new RandomContent(this);

        // Register listeners
        myFrw.listen(state, CharGroup.TYPE, null);
        myFrw.listen(map, Location.TYPE, null);

        state.setCurrent(null);
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
        // Unused
        history = new MyLinkedList();
        future = new MyLinkedList();

        // Useful references
        myFrw = frw;
        myPanel = frm;
        myProps = props;
        myPath = path;
        
        editor = new Editor(this);

        // Create GUI
        createGUI();
        
        myData = new Data(this, frw);
        
        // Register listeners
        myFrw.listen(map, Location.TYPE, null);
        
        return true;
    }

    /**
     * Get the tree model.
     * @return tree model
     */
    public SortedTreeModel getTreeModel() {
        return (SortedTreeModel) tree.getModel();
    }

    /**
     * Called by framework when terminating. We never save anything. We'Re
     * stateless - the state object immediately externalizes all state
     * changes.
     * @param save whether to save or not
     */
    public void stop(boolean save) {
        if (save && state.getDirty())
            state.save();
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
        bScaleSpin = new JSpinner(Framework.getScaleSpinnerModel(17));
        bScaleSpin.setValue(new Integer(100));
        ((JSpinner.DefaultEditor)bScaleSpin.getEditor()).getTextField().setEditable(false);

        bScaleSpin.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    map.setScale();
                }
            });

	// The left panel is filled with buttons

	GridBagConstraints constr1;
	myPanel.setLayout(new GridBagLayout());
	constr1 = new GridBagConstraints();
	constr1.fill = GridBagConstraints.VERTICAL;
	constr1.insets = new Insets(5, 5, 5, 5);
	final JPanel leftPanel = new JPanel(new GridBagLayout());
	final JPanel leftnPanel = new JPanel(new GridBagLayout());
	final JPanel leftmPanel = new JPanel(new GridBagLayout());

        bCenter = new JButton("Center current");
        bCenter.setMnemonic(KeyEvent.VK_C);
	bCenter.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    getState().setCurrent(null);
		}
	    });
        bCenter.setToolTipText("Center the current map");

        bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
        bSave.setEnabled(false);
        bSave.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (state != null) state.save();
                }
            });
        
        bSuppExp = new JCheckBox("Suppress Export");
        bSuppExp.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (state != null) state.setDirty();
                }
            });
        bSuppExp.setToolTipText("Suppress or enable player map export");

        bActive = new JButton("Locate active");
        bActive.setMnemonic(KeyEvent.VK_L);
	bActive.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    if (map.getParent() == null) {
                        dirMap.getParent().add(map);
                        dirMap.getParent().remove(dirMap);
                    }
                    dirMap.clear();
                    map.setIcons(getState().getActive().getMap(), true);
                    map.revalidate();
		    getState().setCurrent(getState().getActive());
                    // Enable/disable buttons
                    bCenter.setEnabled(true);
                    bFog.setEnabled(true);
                    bScaleSpin.setEnabled(true);
		}
	    });
        bActive.setToolTipText("Choose the map the active group is on");

        bFog = new JButton("Standard Fog");
        bFog.setMnemonic(KeyEvent.VK_T);
	bFog.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    map.setFoggy(!map.getFoggy());
		    if (map.getFoggy())
			bFog.setText("Full View");
		    else
			bFog.setText("Standard Fog");
                    fogMouse.clean();
		    map.repaint(null, null);
		}
	    });
        bFog.setToolTipText("Switch the Fading-out of map's boundary (standard fog)");

        bFill = new JButton("Hide all");
        bFill.setMnemonic(KeyEvent.VK_H);
        bFill.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    map.hideAll
                        (getFogLayer(),
                         (e.getModifiers() & InputEvent.SHIFT_MASK) == 0);
                }
            });

        bFill.setToolTipText("Click hides all, shift click erases all fog");

        bFrames = new JCheckBox("Show Wiring");
        bFrames.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    map.repaint(null, null);
                    if (state != null) state.setDirty();
                }
            });
        bFrames.setToolTipText("Show index and submap wiring");

        bShowFog = new JCheckBox("Show Free Fog");
        bShowFog.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    map.revalidateCurrentIcon();
                    if (state != null) state.setDirty();
                }
            });
        bShowFog.setToolTipText("Show free fog (or not)");

        bFullInfo = new JCheckBox("Index details");
        bFullInfo.setToolTipText("Show more text for the index entry at mouse position");
        bFullInfo.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (state != null) state.setDirty();
                }
            });

        bSaveFog = new JButton("Save Fog");
        bSaveFog.setMnemonic(KeyEvent.VK_A);
	bSaveFog.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser(myPath);
                    if (fc.showOpenDialog((Component)e.getSource()) !=
                        JFileChooser.APPROVE_OPTION)
                        return;
                    File f = fc.getSelectedFile();
                    if (f == null) return;
                    map.saveMasks(f);
                }
            });
        bSaveFog.setToolTipText("Save free fog from this map");

        bLoadFog = new JButton("Load Fog");
        bLoadFog.setMnemonic(KeyEvent.VK_L);
	bLoadFog.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser(myPath);
                    if (fc.showOpenDialog((Component)e.getSource()) !=
                        JFileChooser.APPROVE_OPTION)
                        return;
                    File f = fc.getSelectedFile();
                    if (f == null) return;
                    map.loadMasks(f);
                }
            });
        bLoadFog.setToolTipText("Load free fog for this map");

        bDist = new JButton(" - ");
        bDist.setEnabled(false);

	tree = new JTree();
        if (editor != null) tree.setEditable(true);
        resetTree();
	tree.getSelectionModel().setSelectionMode
	    (TreeSelectionModel.SINGLE_TREE_SELECTION);
        final TreeSelectionListener treeListener = new TreeSelectionListener() {
		public void valueChanged(TreeSelectionEvent e) {
                    TreePath p = tree.getSelectionPath();
                    if (p == null) return;
		    DefaultMutableTreeNode tn = (DefaultMutableTreeNode) p.getLastPathComponent();
                    currentSel = tn.toString();
                    if (tn != null)
                        sMapListener.setStart(tn.getFirstLeaf());

                    if (search.hasFocus()) return;

                    String mapname = "";
                    for (int i = 1; i < p.getPathCount(); i++)
                        mapname += "/" + p.getPathComponent(i);

                    if (tn.isLeaf()) {
                        if (map.getParent() == null) {
                            dirMap.getParent().add(map);
                            dirMap.getParent().remove(dirMap);
                            // History
                            if (dirMap.getCurrentName() != null)
                                history.add(dirMap.getCurrentName());
                        }
                        else {
                            // History
                            if (map.getCurrentName() != null)
                                history.add(map.getCurrentName());
                        }
                        dirMap.clear();
                        map.setIcons(mapname.substring(1), false);
                        map.revalidate();
                    }
                    else {
                        if (dirMap.getParent() == null) {
                            map.getParent().add(dirMap);
                            map.getParent().remove(map);
                            // History
                            if (map.getCurrentName() != null)
                                history.add(map.getCurrentName());
                        }
                        else {
                            // History
                            if (dirMap.getCurrentName() != null)
                                history.add(dirMap.getCurrentName());
                        }

                        map.clear();
                        if (p.getPathCount() > 1)
                            dirMap.setIcon(mapname.substring(1));
                        else
                            dirMap.setIcon("");
                    }
                    if (!linear) future.clear();

                    // Enable/disable buttons
                    bCenter.setEnabled(tn.isLeaf());
                    bFog.setEnabled(tn.isLeaf());
                    bToggle.setEnabled(tn.isLeaf());
                    bScaleSpin.setEnabled(tn.isLeaf());

                    // Center and redraw
                    if (editor == null) getState().setCurrent(null);
                    tree.scrollPathToVisible(p);
		}
	    };
	tree.addTreeSelectionListener(treeListener);

        pRandomMenu = new JPopupMenu();
        JMenuItem mi1 = new JMenuItem("New");
        pRandomMenu.add(mi1);
        mi1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    randContent.set();
                }
            });

        // History buttons
        forward = new JButton("Forward");
        forward.setMnemonic(KeyEvent.VK_F);
        forward.setEnabled(false);
        forward.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (future.size() > 0) {
                        TreePath tp = getTreePath((String)future.getLast());
                        linear = true;
                        tree.addSelectionPath(tp);
                        linear = false;
                        future.removeLast();
                    }
                }
            });
        forward.setToolTipText("View forward map");

        back = new JButton("Back");
        back.setMnemonic(KeyEvent.VK_B);
        back.setEnabled(false);
        back.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (history.size() > 0) {
                        TreePath tp = getTreePath((String)history.getLast());
                        linear = true;
                        tree.addSelectionPath(tp);
                        linear = false;
                        future.add(history.getLast().toString());
                        history.removeLast();
                        history.removeLast();
                    }
                }
            });
        back.setToolTipText("View previous map");

        bToggle = new JButton("<html><i>Navigate");
        bToggle.setMnemonic(KeyEvent.VK_N);
        bToggle.setMnemonic(KeyEvent.VK_M);
        bToggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = 0;
                    c.weightx = c.weighty = 1;
                    c.fill = GridBagConstraints.BOTH;
                    if (bToggle.getText().equals("<html><i>Navigate")) {
                        bToggle.setText("<html><i>Manipulate");
                        leftPanel.remove(leftnPanel);
                        leftPanel.add(leftmPanel, c);
                        map.removeMouseListener(moveMA);
                        map.addMouseListener(fogMouse);
                        map.addMouseMotionListener(fogMouse.paintMove);
                        bToggle.setToolTipText
                            ("<html>On map: press left button to create fog,<br>" +
                             "right button to erase fog,<br>" +
                             "shift-right button to mark;<br>" +
                             "shift-left button to start polygon,<br>" +
                             "left-button to add edge to polygon,<br>" +
                             "right-button to remove edge from polygon,<br>" +
                             "shift-left button to end and create fog in polygon,<br>" +
                             "shift-right button to end and erase fog");
                    }
                    else {
                        bToggle.setText("<html><i>Navigate");
                        leftPanel.remove(leftmPanel);
                        leftPanel.add(leftnPanel, c);
                        map.addMouseListener(moveMA);
                        map.removeMouseListener(fogMouse);
                        map.removeMouseMotionListener(fogMouse.paintMove);
                        bToggle.setToolTipText("On map: use popup for map actions");
                    }
                    leftPanel.revalidate();
                    leftPanel.repaint();
                }
            });
        bToggle.setEnabled(false);
        bToggle.setToolTipText("On map: use popup for map actions");

        GridBagConstraints constr2 = new GridBagConstraints();
	constr2.gridx = 0;
	constr2.weightx = 1;
	constr2.fill = GridBagConstraints.HORIZONTAL;
        leftnPanel.add(back, constr2);
        leftnPanel.add(forward, constr2);
	if (editor == null) leftPanel.add(bActive, constr2);
        leftPanel.add(bCenter, constr2);
        if (editor == null) leftPanel.add(bSave, constr2);
        if (editor == null) leftPanel.add(bSuppExp, constr2);
        if (editor == null) leftPanel.add(bFog, constr2);
        if (editor == null) leftmPanel.add(bFrames, constr2);
        if (editor == null) leftmPanel.add(bFullInfo, constr2);
        if (editor == null) leftmPanel.add(bShowFog, constr2);
        if (editor == null) leftmPanel.add(bSaveFog, constr2);
        if (editor == null) leftmPanel.add(bLoadFog, constr2);
        if (editor == null) leftmPanel.add(bFill, constr2);
        if (editor == null) leftnPanel.add(bDist, constr2);
        leftPanel.add(bScaleSpin, constr2);
        leftPanel.add(bToggle, constr2);

        // Layers
        bLayers = new JButton("Layers");
        if (editor == null) leftmPanel.add(bLayers, constr2);
	bLayers.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger() && pShowLayerMenu != null) {
                        pShowLayerMenu.show
                            (bLayers, bLayers.getWidth()/2, bLayers.getHeight()/2);
                    }
                }
            });
        bLayers.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    pShowLayerMenu.show
                        (bLayers, bLayers.getWidth()/2, bLayers.getHeight()/2);
                }
            });
        bLayers.setToolTipText("Show/Select layers switched on/off");

        // Masks
        bFogs = new JButton("Active Fog");
        if (editor == null) leftmPanel.add(bFogs, constr2);
	bFogs.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger() && pFogLayerMenu != null) {
                        pFogLayerMenu.show
                            (bFogs, bFogs.getWidth()/2, bFogs.getHeight()/2);
                    }
                }
            });
        bFogs.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    pFogLayerMenu.show
                        (bFogs, bFogs.getWidth()/2, bFogs.getHeight()/2);
                }
            });
        bFogs.setToolTipText("Show/Select fog layer edited");

        search = new JTextField();
        sMapListener = new IncSearchDocListener(search, tree);
        search.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    treeListener.valueChanged(null);
                }
            });
        search.getDocument().addDocumentListener(sMapListener);
        search.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    sMapListener.increment();
                    sMapListener.work(search.getDocument());
                }
            });

        if (editor != null) {
            ButtonGroup bg = new ButtonGroup();
            bRect = new JRadioButton("Rectangle");
            bRect.setSelected(true);
            leftmPanel.add(bRect, constr2); bg.add(bRect);
            bCirc = new JRadioButton("Circle");
            leftmPanel.add(bCirc, constr2); bg.add(bCirc);
            bPoly = new JRadioButton("Polygon");
            leftmPanel.add(bPoly, constr2); bg.add(bPoly);
            bExport = new JButton("Change Location");
            bExport.setEnabled(false);
            bExport.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Location loc = map.getFirstLocation();
                        if (loc != null && editor.currShape != null)
                            loc.addShape(map.getCurrentName(), editor.currShape);
                    }
                });
            leftPanel.add(bExport, constr2);

            eMenu = new JPopupMenu();
            miCD = new JMenuItem("Add directory");
            eMenu.add(miCD);
            miCD.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        TreePath tp = tree.getPathForLocation(gx, gy);
                        myData.addDir(tp.getPath());
                    }
                });

            miCM = new JMenuItem("Add Map");
            eMenu.add(miCM);
            miCM.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        TreePath tp = tree.getPathForLocation(gx, gy);
                        myData.addMap(tp.getPath());
                    }
                });
        }

        tree.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX(); gy = e.getY();
                        TreePath tp =
                            tree.getPathForLocation(gx, gy);
                        if (tp == null) return;
                        if (tp.getPathCount() == 1) return;
                        if (editor != null) {
                            boolean leaf =
                                ((TreeNode)tp.getLastPathComponent()).isLeaf();
                            if (leaf) return;
                            eMenu.show(e.getComponent(), gx, gy);
                        }
                        else {
                            String val = 
                                tp.getLastPathComponent().toString();
                            if (tp.getPathCount() != 2 ||
                                !val.equals("Random")) return;
                            pRandomMenu.show(e.getComponent(), gx, gy);
                        }
                    }
                }
            });

	// We add an expanding JTree.
        GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = 0;
	constr3.gridy = GridBagConstraints.RELATIVE;
	constr3.fill = GridBagConstraints.BOTH;
	constr3.weighty = 1;
        leftnPanel.add(new JScrollPane(tree), constr3);
        leftnPanel.add(search, constr2);
        leftmPanel.add(new JPanel(), constr3);

	// The right panel only contains a map.  The map is displayed in a
	// scrollable area.

	GridBagConstraints constr4 = new GridBagConstraints();
	constr4.fill = GridBagConstraints.BOTH;
	constr4.weightx = constr4.weighty = 1;

	map = new MapHolder(myProps, this);

        fogMouse = new FogByMouse(this);
        infoMouse = new InfoByMouse(this);

        // menu must be aware of infoMouse
        moveMA = new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX(); gy = e.getY();
                        moveMenu.show
                            (e.getComponent(), e.getX(), e.getY());
                    }
                }
            };

        moveMenu = new MoveByMenu
            (this, Integer.parseInt(myProps.getProperty("distance.feet")));

        if (editor == null) {
            map.addMouseListener(moveMA);
        }

        if (editor == null) {
            map.addMouseMotionListener(infoMouse);
        }
        else {
            map.setToolTipText
                ("<html>left-click starts/continues outline<br>" + 
                 "right-click ends outline</html>");
            map.addMouseListener(editor);
        }

	dirMap = new DirMapHolder(myProps, this);
        dirMap.setToolTipText("left-click to select sub map");
	dirMap.addMouseListener(new LinkByMouse(this));

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
        AdjustmentListener al = new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    if (e.getValueIsAdjusting() || !map.getFoggy())
                        return;
                    map.repaint(null, null);
                }
            };
        JScrollBar sb = rightPanel.getVerticalScrollBar();
        sb.setUnitIncrement(10);
        sb.addAdjustmentListener(al);
        sb = rightPanel.getHorizontalScrollBar();
        sb.setUnitIncrement(10);
        sb.addAdjustmentListener(al);

        myPanel.getActionMap().put("up", new MyAction(true, true));
        myPanel.getActionMap().put("down", new MyAction(true, false));
        myPanel.getActionMap().put("left", new MyAction(false, true));
        myPanel.getActionMap().put("right", new MyAction(false, false));
        myPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put
            (KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_MASK), "up");
        myPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put
            (KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK), "down");
        myPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put
            (KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK), "left");
        myPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put
            (KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK), "right");

        // Bypass tree default keystrokes
        tree.getInputMap(JComponent.WHEN_FOCUSED).put
            (KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_MASK), "none");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put
            (KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK), "none");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put
            (KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK), "none");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put
            (KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK), "none");

	constr4.insets = new Insets(6,5,6,5);

        constr2.fill = GridBagConstraints.BOTH;
        constr2.weighty = 1;
        leftPanel.add(leftnPanel, constr2);
        JSplitPane sp =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        sp.setResizeWeight(.2);
	myPanel.add(sp, constr4);

	// If someone turned these value into non-Integers, we throw up
	int w = Integer.parseInt(myProps.getProperty("display.width"));
	int h = Integer.parseInt(myProps.getProperty("display.height"));
	rightPanel.setPreferredSize(new Dimension(w, h));
    }

    /**
     * (Re)set tree model.
     */
    public void resetTree() {
	maptree = new DefaultMutableTreeNode("Atlas List");
        TreeModel tm = new SortedTreeModel(maptree);
	tree.setModel(tm);
        tm.addTreeModelListener(new TreeModelListener() {
                public void treeNodesChanged(TreeModelEvent e) {
                    Object[] tn = e.getPath();
                    String nname = e.getChildren()[0].toString();
                    if (!myData.rename(tn, nname, currentSel)) {
                        ((MutableTreeNode)e.getChildren()[0]).setUserObject
                            (currentSel);
                    }
                    else {
                        currentSel = nname;
                    }
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
    public String[] uses() {
        return new String[] { Location.TYPE };
    }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() {
	return new String[] { Atlas.TYPE, ScaledMap.TYPE };
    }

    /**
     * This method is called by other objects, which require a redraw of the
     * screen.
     * @param x coordinate to point to
     * @param y coordinate to point to
     */
    void setViewport(int x, int y) {
	// Get viewport dimension
	JViewport vp = rightPanel.getViewport();
	int w = vp.getExtentSize().width;
	int h = vp.getExtentSize().height;
	
	int cw = map.getPreferredSize().width;
	int ch = map.getPreferredSize().height;

	if (x - w/2 < 0) x = w/2;
	if (x - w/2 < 0) x = w/2;
	if (x - w/2 < 0) x = w/2;
	if (y - h/2 < 0) y = h/2;
	if (x + w/2 > cw) x = cw - w/2;
	if (y + h/2 > ch) y = ch - h/2;

	// Set viewport center to hotspot
	vp.setViewPosition(new Point(x - w/2, y - h/2));
    }

    /** Return number of layers */
    public int getNumLayers() { return pShowLayerMenu.getComponents().length; }

    /**
     * Show the layer or not. Returns false for out of bounds. i = 0 is the
     * base image.
     * @param i layer to check
     * @return show or not
     */
    public boolean showLayer(int i) {
        if (pShowLayerMenu == null ||
            i >= pShowLayerMenu.getComponents().length) return false;
        JCheckBoxMenuItem cb =
            (JCheckBoxMenuItem)pShowLayerMenu.getComponents()[i];
        return cb.isSelected() && cb.isEnabled();
    }

    /**
     * Return the current fog layer from the radiobutton group.
     * @return fog layer
     */
    public int getFogLayer() {
        for (int i = 0; i < pFogLayerMenu.getComponents().length; i++)
            if (((JRadioButton)pFogLayerMenu.getComponents()[i]).isSelected())
                return i;
        return 0;
    }

    /**
     * Enable layers.
     * @param list layers to check
     * @return show or not
     */
    public void setLayers(ArrayList list) {
        pShowLayerMenu = new JPopupMenu();
        pFogLayerMenu = new JPopupMenu();
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; list != null && i < list.size(); i++) {
            JCheckBoxMenuItem mi =
                new JCheckBoxMenuItem(Integer.toString(i));
            mi.setSelected(true);
            mi.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        map.revalidateCurrentIcon();
                    }
                });
            pShowLayerMenu.add(mi);

            JRadioButton rb = new JRadioButton(Integer.toString(i));
            bg.add(rb);
            if (i == 0) rb.setSelected(true);
            pFogLayerMenu.add(rb);

            if (list.get(i) == null) {
                mi.setEnabled(false);
                rb.setEnabled(false);
            }
        }
    }

    /**
     * Create a tree path to be selected.
     */
    private TreePath getTreePath(String key) {
        String[] keys = key.split("/");
        Object[] oa = new Object[keys.length + 1];
        oa[0] = tree.getModel().getRoot();
        int idx = 0;
        for (int j = 0; j < oa.length - 1; j++) {
            TreeNode tn = (TreeNode) oa[j];
            for (int i = 0; i < tn.getChildCount(); i++) {
                DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode)tn.getChildAt(i);
                if (child.getUserObject().equals(keys[j])) {
                    oa[j + 1] = tn.getChildAt(i);
                    break;
                }
            }
        }
        return new TreePath(oa);
    }

    /**
     * Key listener in scrollbars.
     */
    class MyAction extends AbstractAction {
        private boolean updown, upleft;
        MyAction(boolean anUpdown, boolean anUpleft) {
            updown = anUpdown;
            upleft = anUpleft;
        }
        public void actionPerformed(ActionEvent e) {
            JScrollBar sb =
                updown ? rightPanel.getVerticalScrollBar() : rightPanel.getHorizontalScrollBar();
            sb.setValue(sb.getValue() + (upleft ? -sb.getUnitIncrement() : sb.getUnitIncrement()));
        }
    }

    /**
     * Simple Queue implementation. It also manages the buttons for this list.
     */
    class MyLinkedList extends LinkedList {
        public boolean add(Object o) {
            // Buttons
            if (this == history) {
                if (back != null) back.setEnabled(true);
            }
            else {
                if (forward != null) forward.setEnabled(true);
            }

            // Implement queue
            if (size() > histSize)
                removeFirst();
            return super.add(o);
        }
        public Object removeLast() {
            // Buttons
            if (size() <= 1) {
                if (this == history) {
                    if (back != null)  back.setEnabled(false);
                }
                else {
                    if (forward != null) forward.setEnabled(false);
                }
            }
            return super.removeLast();
        }
        public void clear() {
            // Buttons
            if (this == history) {
                if (back != null) back.setEnabled(false);
            }
            else {
                if (forward != null) forward.setEnabled(false);
            }
            super.clear();
        }
    }
}
