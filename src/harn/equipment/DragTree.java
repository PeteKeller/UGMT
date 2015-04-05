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

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.Point;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import rpg.SortedTreeModel;

/**
 * A drag-and-drop tree. The two right views are drag-and-drog, while the left
 * is drag only, but duplicates items.
 * @author Michael Jung
 */
public class DragTree extends rpg.DNDTree {
    /** Main reference */
    private Main main;

    /** Kill popup */
    private JPopupMenu pMenu;

    /** mouse pressed coordinates */
    private int gx, gy;

    /** Constructor */
    DragTree(DefaultTreeModel model, Main aMain) {
        super(model);
        main = aMain;
        setEditable(false);
    }

    /** from DropTargetListener */
    public void drop(DropTargetDropEvent e) { e.rejectDrop(); }

    /** Drop validity check. src may not be null. */
    protected boolean valid(TreePath dest, TreePath src) {
        if (dest == null || src == dest) return false;

        // Don't drop in drag tree
        if (main.dataList.getModel().getRoot() == dest.getPathComponent(0))
            return false;

        Object noded = dest.getLastPathComponent();
        TreeNode nodes = (TreeNode) src.getLastPathComponent();

        // Root Element can only take direct children of root elements
        if (!(noded instanceof Item))
            return !(nodes.getParent() instanceof Item);
        // Non-roots take all
        else
            return (nodes.getParent() instanceof Item);
    }

    /** from DragGestureListener */
    public void dragGestureRecognized(DragGestureEvent e) {
        if (!((TreeNode)getLastSelectedPathComponent()).isLeaf()) return;
        super.dragGestureRecognized(e);
    }
}
