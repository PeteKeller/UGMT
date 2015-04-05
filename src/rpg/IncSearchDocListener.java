/*
 * UGMT : Unversal Gamemaster tool
 * Copyright (c) 2004 Michael Jung
 *
 * Any party obtaining a copy of these files is granted, free of charge, a
 * full and unrestricted irrevocable, world-wide, paid up, royalty-free,
 * nonexclusive right and license to deal in this software and
 * documentation files (the "Software"), including without limitation the
 * rights to use, copy, modify, merge, publish and/or distribute copies of
 * the Software, and to permit persons who receive copies from any such 
 * party to do so, with the only requirement being that this copyright 
 * notice remain intact.
 */
package rpg;

import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;
import javax.swing.*;

/**
 * Text field listener class for incremental searching.
 */
public class IncSearchDocListener implements DocumentListener {
    /** Search iteration */
    private DefaultMutableTreeNode start;

    /** Field containing the text to search */
    private JTextField search;

    /** Tree to search */
    private JTree tree;

    /** Color */
    private Color bg;

    /** Constructor */
    public IncSearchDocListener(JTextField pSearch, JTree pTree) {
        bg = pSearch.getBackground();
        search = pSearch;
        tree = pTree;
    }

    /** IF method */
    public void removeUpdate(DocumentEvent e) { work(e.getDocument()); }

    /** IF method */
    public void insertUpdate(DocumentEvent e) { work(e.getDocument()); }

    /** IF method */
    public void changedUpdate(DocumentEvent e) {}

    /** Set start */
    public void setStart(DefaultMutableTreeNode aStart) { start = aStart; }

    /** Increment search node */
    public void increment() {
        if (start == null)
            start = ((DefaultMutableTreeNode)tree.getModel().getRoot()).getFirstLeaf();
        else
            start = start.getNextLeaf();
    }

    /** work */
    public void work(Document doc) {
        if (start == null)
            start = ((DefaultMutableTreeNode)tree.getModel().getRoot()).getFirstLeaf();
        try {
            search.setBackground(bg);
            String txt = doc.getText(0, doc.getLength());
            DefaultMutableTreeNode tn = start;
            int i = 0;
            while (tn != null) {
                if (tn.getUserObject().toString().toLowerCase().indexOf(txt.toLowerCase()) > -1) {
                    TreeNode[] p = ((DefaultTreeModel)tree.getModel()).getPathToRoot(tn);
                    TreePath tp = new TreePath(p);
                    tree.setSelectionPath(tp);
                    tree.scrollPathToVisible(tp);
                    return;
                }
                tn = tn.getNextLeaf();
            }
            // Blink
            search.setBackground(Color.RED);
        }
        catch (BadLocationException ex) {
            // Can't happen
        }
    }
}
