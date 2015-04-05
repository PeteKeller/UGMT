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
import java.text.*;
import java.util.*;

/**
 * This class provides an alphabetical insert to a default tree model
 * @author Michael Jung
 */
public class SortedTreeModel extends DefaultTreeModel {
    /** Default constructor */
    public SortedTreeModel(TreeNode root) { super(root); }

    /** insert ordered */
    public void insertNodeSorted(DefaultMutableTreeNode newChild, DefaultMutableTreeNode parent) {
        Collator col = Collator.getInstance(Locale.FRANCE);
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) parent.getChildAt(i);
            String a = newChild.getUserObject().toString();
            String b = node.getUserObject().toString();
            if (col.compare(a,b) < 0) {
                insertNodeInto(newChild, parent, i);
                return;
            }
        }
        insertNodeInto(newChild, parent, parent.getChildCount());
    }
}

