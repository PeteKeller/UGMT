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
import java.awt.print.*;
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
 * The main class for the index plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Main implements IPlugin {
    /** Activated link constant */
    final private HyperlinkEvent.EventType ACTIVATED = HyperlinkEvent.EventType.ACTIVATED;

    /** Editor for index entries */
    Editor editor;

    /** GUI reference */
    private JPanel myPanel;

    /** Left side */
    private JTree indexTree;

    JButton forward;
    JButton back;
    private JButton bSave;

    /** Menus */
    private JPopupMenu pMenuHI;
    private JPopupMenu pMenuV;
    private JPopupMenu pPrint;

    /** Right side */
    private JTextField title;
    PrintPane view;

    /** Current position of mouse for popup */
    private int gx, gy;

    /** Used for moving back/forth*/
    private boolean linear = false;

    /** Panel for results */
    JScrollPane sresults;
    JPanel results;

    /** Top panel (right side) */
    JPanel tp;

    /** Framework reference */
    Framework myFrw;

    /** My data */
    Data myData;

    /** My properties */
    Properties myProps;

    /** Path to my data */
    String myPath;

    /** history list of pages visited*/
    MyLinkedList history;

    /** future list of pages visited */
    MyLinkedList future;

    /** Maximum number of elements in the future/history lists */
    private int histSize;

    /** Search field */
    JTextField search;

    /** incremental index search listener */
    private IncSearchDocListener inindListener;

    /** Full search doc listener */
    private MyFullListener fullListener;

    /** Incremental page doc listener */
    private MyDocListener indocListener;

    /** Currently active listener */
    private DocumentListener curListener;

    /** Full Text search */
    private SearchEngine engine;

    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version() { return 0.9f; }

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
	myPath = path + frw.SEP;
        history = new MyLinkedList();
        future = new MyLinkedList();
        histSize = Integer.parseInt(props.getProperty("history.size"));

	// Create GUI
	createGUI();

	myData = new Data(this, frw);
        engine = new SearchEngine(this);

        // Register listeners
        myFrw.listen(myData, SoundBank.TYPE, null);
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
	myProps = props;
	myPath = path + frw.SEP;

	// Create GUI
	editor = new Editor(this, frm);
        editor.createGUI();

	myData = new Data(this, frw);

        return true;
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
    public boolean hasUnsavedParts() { return (bSave != null ? bSave.isEnabled() : false); }

    /**
     * Create (and show) GUI.
     */
    private void createGUI() {
        myPanel.setLayout(new GridBagLayout());

        // Save
        bSave = new JButton("Save");
        bSave.setMnemonic(KeyEvent.VK_S);
        bSave.setEnabled(false);
        bSave.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Document doc = view.getDocument();
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
        bSave.setToolTipText("Save changes");

	// View
	view = new PrintPane();
        view.setEditorKit(view.getEditorKitForContentType("text/html"));
        view.setEditable(false);
        view.setMargin(new Insets(10,10,10,10));
        view.addCaretListener(new CaretListener() {
                public void caretUpdate(CaretEvent e) {
                    if (!indocListener.inwork)
                        indocListener.setStart(e.getMark());
                }
            });
	view.addHyperlinkListener(new HyperlinkListener() {
		public void hyperlinkUpdate(HyperlinkEvent e) {
		    if (e.getEventType() == ACTIVATED) {
			try {
                            String key = myData.getKey(e.getURL().toString());
                            if (key.equals("---")) {
                                view.setPage(e.getURL());
                                title.setText(key);
                            }
                            else {
                                TreePath tp = getTreePath(key);
                                indexTree.addSelectionPath(tp);
                            }
			}
			catch(Exception ex) {
			    ex.printStackTrace();
			}
		    }
		}
	    });
        view.setToolTipText("Use popup to print page");

	// Index
        indexTree = new JTree();
        indexTree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    TreePath p = e.getNewLeadSelectionPath();
                    if (p == null) return;
                    Object oa[] = p.getPath();
                    MutableTreeNode tn = (MutableTreeNode) oa[oa.length - 1];
                    if (tn != null && tn.isLeaf()) {
                        String key = "";
                        for (int i = 1; i < oa.length; i++)
                            key += "/" + oa[i].toString();
                        key = key.substring(1);
                        String s = myData.getFile(key);
                        if (s == null) return;
                        try {
                            if (s.indexOf("pdf:") == 0) {
                                s = s.substring(4);
                                Runtime.getRuntime().exec
                                    (System.getProperty("pdf") + " " + myPath + s);
                            }
                            else {
                                URL url = view.getPage();
                                if (url != null && !myData.getKey(url.toString()).equals("---")) {
                                    history.add(url);
                                    if (!linear) future.clear();
                                }
                                view.setPage(new URL("file:" + myPath + s));
                                title.setText(key);
                            }
                        }
                        catch(Exception ex) {
                            ex.printStackTrace();
                        }
                        myData.getLocation(key).inform();
                        // Other selections
                        indexTree.scrollPathToVisible(p);
                    }
                }
            });
	indexTree.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
        indexTree.setEditable(false);

        search = new JTextField();
        inindListener = new IncSearchDocListener(search, indexTree);
        fullListener = new MyFullListener();
        indocListener = new MyDocListener();

        curListener = inindListener;
        search.getDocument().addDocumentListener(curListener);
        search.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (curListener == inindListener) {
                        inindListener.increment();
                        inindListener.work(search.getDocument());
                    }
                    if (curListener == fullListener) {
                        fullListener.work(search.getDocument());
                    }
                    if (curListener == indocListener) {
                        indocListener.increment();
                        indocListener.work(search.getDocument());
                    }
                }
            });

        indexTree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    TreePath p = e.getNewLeadSelectionPath();
                    DefaultMutableTreeNode tn = null;
                    if (p != null)
                        tn = (DefaultMutableTreeNode) p.getLastPathComponent();
                    if (tn != null) {
                        inindListener.setStart(tn.getFirstLeaf());
                        indocListener.setStart(0);
                    }
                }
            });

        // History buttons
        forward = new JButton("Forward");
        forward.setMnemonic(KeyEvent.VK_F);
        forward.setEnabled(false);
        forward.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (future.size() > 0) {
                        try {
                            String key = myData.getKey(future.getLast().toString());
                            TreePath tp = getTreePath(key);
                            linear = true;
                            indexTree.addSelectionPath(tp);
                            linear = false;
                            future.removeLast();
                        }
                        catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        forward.setToolTipText("Forward a page in view history");

        back = new JButton("Back");
        back.setMnemonic(KeyEvent.VK_B);
        back.setEnabled(false);
        back.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (history.size() > 0) {
                        try {
                            String key = myData.getKey(history.getLast().toString());
                            TreePath tp = getTreePath(key);
                            linear = true;
                            indexTree.addSelectionPath(tp);
                            linear = false;
                            future.add(history.getLast().toString());
                            history.removeLast();
                            history.removeLast();                            
                        }
                        catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        back.setToolTipText("Back a page in view history");

        pMenuHI = new JPopupMenu();
        JMenuItem mi1 = new JMenuItem("Locate");
        pMenuHI.add(mi1);
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = indexTree.getPathForLocation(gx, gy);
                    if (tp == null) return;
                    Object[] oa = tp.getPath();
                    if (tp.getPath().length < 4) return;
                    String entry = "";
                    for (int i = 1; i < 4; i++)
                        entry += "/" + oa[i].toString();
                    MyLocation loc = myData.getLocation(entry.substring(1));
                    loc.locate();
                }
	    });
        mi1 = new JMenuItem("Sound Trigger");
        pMenuHI.add(mi1);
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    TreePath tp = indexTree.getPathForLocation(gx, gy);
                    if (tp == null) return;
                    Object[] oa = tp.getPath();
                    String entry = oa[oa.length - 1].toString();
                    if (oa.length > 1)
                        entry = oa[oa.length - 2].toString() + "/" + entry;
                    if (oa.length > 2)
                        entry = oa[oa.length - 3].toString() + "/" + entry;
                    MyLocation loc = myData.getLocation(entry);
                    if (myData.getSoundBank() != null)
                        myData.getSoundBank().addLocation(loc);
                }
	    });

        indexTree.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX(); gy = e.getY();
                        pMenuHI.show(e.getComponent(), gx, gy);
                    }
                }
            });

        pMenuV = new JPopupMenu();
        mi1 = new JMenuItem("Boldface");
        pMenuV.add(mi1);
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
        pMenuV.add(mi2);
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
                        if (editor != null) {
                            gx = e.getX(); gy = e.getY();
                            pMenuV.show(e.getComponent(), gx, gy);
                        }
                        else {
                            pPrint.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }
            });
        pPrint = new JPopupMenu();
        mi1 = new JMenuItem("Print");
        pPrint.add(mi1);
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPrintable(view);
                    if (job.printDialog()) {
                        try {
                            job.print();
                        }
                        catch (PrinterException ex) {
                            // Debug
                            ex.printStackTrace();
                        }
                    }
                }
            });

        // Panels
	JScrollPane sview = new JScrollPane(view);
        sview.getVerticalScrollBar().setUnitIncrement(10);
        sview.getHorizontalScrollBar().setUnitIncrement(10);
	JScrollPane sindex = new JScrollPane(indexTree);
        sindex.getVerticalScrollBar().setUnitIncrement(10);
        sindex.getHorizontalScrollBar().setUnitIncrement(10);
	results = new JPanel(new GridBagLayout());
	sresults = new JScrollPane(results);
        sresults.getVerticalScrollBar().setUnitIncrement(10);
        sresults.getHorizontalScrollBar().setUnitIncrement(10);
        JSplitPane sp = new JSplitPane
            (JSplitPane.VERTICAL_SPLIT, sview, sresults);
        sp.setResizeWeight(.9);

        final JCheckBox full = new JCheckBox("Full");
        full.setToolTipText("Full database search (non-incremental)");
        final JCheckBox inind = new JCheckBox("Index");
        inind.setSelected(true);
        inind.setToolTipText("Index search (incremental)");
        final JCheckBox indoc = new JCheckBox("Page");
        indoc.setToolTipText("Search shown page (incremental)");
        Box three = new Box(BoxLayout.X_AXIS);
        three.add(full);
        three.add(inind);
        three.add(indoc);
        ButtonGroup bg = new ButtonGroup();
        bg.add(full);
        bg.add(inind);
        bg.add(indoc);
        ItemListener il = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() != ItemEvent.SELECTED) return;
                    String mod = ((JCheckBox)e.getItem()).getText();
                    search.getDocument().removeDocumentListener(curListener);
                    if (e.getItem() == full)
                        curListener = fullListener;
                    if (e.getItem() == inind)
                        curListener = inindListener;
                    if (e.getItem() == indoc)
                        curListener = indocListener;
                    results.removeAll();
                    results.repaint();
                    search.getDocument().addDocumentListener(curListener);
                }
            };
        full.addItemListener(il);
        inind.addItemListener(il);
        indoc.addItemListener(il);

	GridBagConstraints constr1 = new GridBagConstraints();
	constr1.gridx = 0;
	constr1.weightx = 1;
	constr1.fill = GridBagConstraints.BOTH;

	JPanel p1 = new JPanel(new GridBagLayout());
        p1.add(back, constr1);
        p1.add(forward, constr1);
        p1.add(bSave, constr1);
	constr1.weighty = 1;
        p1.add(sindex, constr1);
        constr1.weighty = 0;
        p1.add(three, constr1);
        p1.add(search, constr1);

	GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = constr3.gridy = 0;
	constr3.weightx = 1;
        constr3.weighty = 0;
	constr3.fill = GridBagConstraints.HORIZONTAL;

        tp = new JPanel(new GridBagLayout());
        title = new JTextField();
        title.setEnabled(false);
        tp.add(title, constr3);
	constr3.gridy = 1;
        constr3.weighty = 1;
	constr3.fill = GridBagConstraints.BOTH;
        tp.add(sp, constr3);

	JSplitPane sp4 = new JSplitPane
            (JSplitPane.HORIZONTAL_SPLIT, p1, tp);
        sp4.setResizeWeight(.2);
	
	constr1.weighty = 1;
	myPanel.add(sp4, constr1);

	// If someone turned these value into non-Integers, we throw up
	int w = Integer.parseInt(myProps.getProperty("display.width"));
	int h = Integer.parseInt(myProps.getProperty("display.height"));
	view.setPreferredSize(new Dimension(w, h));
    }

    /**
     * Get index.
     * @return index tree
     */
    public JTree getIndex() {
        if (editor != null)
            return editor.getIndex();
        return indexTree;
    }

    /**
     * Get index.
     * @return get index tree model
     */
    public TreeModel getIndexModel() {
        if (editor != null)
            return editor.getIndexModel();
        return indexTree.getModel();
    }

    /**
     * Set index tree model.
     * @param model model to set
     */
    public void setIndexModel(TreeModel m) {
        if (editor != null)
            editor.setIndexModel(m);
        if (indexTree != null)
            indexTree.setModel(m);
    }

    /**
     * Set title line text.
     * @param txt text for title line
     */
    public void setTitleText(String txt) {
        if (title != null)
            title.setText(txt);
        if (editor != null)
            editor.title.setText(txt);
    }

    /**
     * Set View documents.
     * @param txt text for title line
     */
    public void setViewDocument(HTMLDocument doc) {
        if (view != null)
            view.setDocument(doc);
        if (editor != null)
            editor.view.setDocument(doc);
    }

    /**
     * Get the editor (often null)
     * @return editor
     */
    public Editor getEditor() { return editor; }

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
     * Each plugin must provide an array of objects it requires.
     * @return list of imports
     */
    public String[] requires() { return new String[0]; }

    /**
     * Each plugin must provide an array of objects it uses.
     * @return list of imports
     */
    public String[] uses() { return new String[0]; }

    /**
     * Each plugin must provide an array of objects it announces.
     * @return list of exports
     */
    public String[] provides() { return new String[] { Location.TYPE }; }

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

    /** Full search listener */
    class MyFullListener implements DocumentListener {
        String curr;
        /** IF method */
        public void removeUpdate(DocumentEvent e) { work(e.getDocument()); }
        /** IF method */
        public void insertUpdate(DocumentEvent e) { work(e.getDocument()); }
        /** IF method */
        public void changedUpdate(DocumentEvent e) {}
        /** work */
        public void work(Document doc) {
            try {
                if (curr == null) {
                    curr = doc.getText(0, doc.getLength());
                    return;
                }
                String curr2 = doc.getText(0, doc.getLength());
                if (curr2.equals(curr))
                    engine.search();
                curr = curr2;
            }
            catch(BadLocationException e) {
                // Can't happen
            }
        }
    }

    /** Full search listener */
    class MyDocListener implements DocumentListener {
        /** for caret listeners */
        public boolean inwork = false;
        /** Color */
        private Color bg;
        /** position in page */
        private int docpos = 0;
        /** Constructor */
        public MyDocListener() { bg = search.getBackground(); }
        /** IF method */
        public void removeUpdate(DocumentEvent e) { work(e.getDocument()); }
        /** IF method */
        public void insertUpdate(DocumentEvent e) { work(e.getDocument()); }
        /** IF method */
        public void changedUpdate(DocumentEvent e) {}
        /** Set start */
        public void setStart(int n) { docpos = n; }
        /** Increment */
        public void increment() { docpos++; }
        /** work */
        public void work(Document srch) {
            search.setBackground(bg);
            try {
                HTMLDocument doc = (HTMLDocument)view.getDocument();
                if (doc == null) return;
                int dlen = doc.getLength();
                String txt = srch.getText(0, srch.getLength());
                int tlen = txt.length();
                int i = docpos;
                while (i + tlen < dlen) {
                    if (doc.getText(i, tlen).equals(txt)) {
                        inwork = true;
                        docpos = i;
                        view.select(docpos, docpos + tlen);
                        view.getCaret().setSelectionVisible(true);
                        inwork = false;
                        return;
                    }
                    i++;
                }
                // Blink
                search.setBackground(Color.RED);
            }
            catch(BadLocationException e) {
                // Can't happen
            }
        }
    }

    /**
     * Utility class for printing
     */
    class PrintPane extends JEditorPane implements Printable {
        /** IF method */
        public int print (Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            double sw = pf.getImageableWidth() / getSize().width;
            int nPages = (int) Math.ceil
                (sw * getSize().height / pf.getImageableHeight());

            if (pageIndex >= nPages) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D)g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.translate(0f, -pageIndex * pf.getImageableHeight());
            g2.scale(sw, sw);
            paint(g);

            return Printable.PAGE_EXISTS;
        }
        /** Helper for reading a stream */
        public InputStream getStream(URL url) throws IOException {
            return engine.getStream(url);
        }
    }
}
