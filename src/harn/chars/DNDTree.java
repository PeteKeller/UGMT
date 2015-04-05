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

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import rpg.SortedTreeModel;

/**
 * A drag-and-drop tree.
 * @author Michael Jung
 */
public class DNDTree extends JTree implements DragGestureListener, DropTargetListener, DragSourceListener {
    /** Transferable flavor */
    static private DataFlavor[] myFlavor;

    /** Main reference */
    private Main main;

    /** Drag source */
    private DragSource dragSource;

    /** Drag context */
    private DragSourceContext dragSourceContext;

    /** Constructor */
    DNDTree(DefaultTreeModel model, Main aMain) {
        super(model);
        main = aMain;
        dragSource = DragSource.getDefaultDragSource() ;

        DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer
            (this, DnDConstants.ACTION_MOVE, this);
        DropTarget dropTarget = new DropTarget(this, this);
    }

    /** from DragGestureListener */
    public void dragGestureRecognized(DragGestureEvent e) {
        Main.TreeNode node = (Main.TreeNode) getLastSelectedPathComponent();
        if (node != null && e.getDragAction() == DnDConstants.ACTION_MOVE) {
            Transferable transfer =
                new MyTransferable(node);

            dragSource.startDrag(e, DragSource.DefaultMoveDrop, transfer, this);
        }
    }

    /** from DropTargetListener */
    public void drop(DropTargetDropEvent e) {
        Transferable tr = e.getTransferable();

        if (!tr.isDataFlavorSupported(myFlavor[0])) {
            e.rejectDrop();
            return;
        }

        // Get new parent node
        Point loc = e.getLocation();
        TreePath dest = getPathForLocation(loc.x, loc.y);

        TreePath src = getSelectionPath();
        if (!valid(dest, src) ||
            (e.getDropAction() != DnDConstants.ACTION_MOVE)) {
            e.rejectDrop();
            return;
        }

        e.acceptDrop (DnDConstants.ACTION_MOVE);

        Main.TreeNode newParent = (Main.TreeNode) dest.getLastPathComponent();
        Main.TreeNode child = (Main.TreeNode) src.getLastPathComponent();

        ((SortedTreeModel)getModel()).removeNodeFromParent
            ((MutableTreeNode)child);
        ((SortedTreeModel)getModel()).insertNodeSorted(child, newParent);

        main.getState().setDirty(true);
        e.getDropTargetContext().dropComplete(true);
    }

    /** Drop validity check. */
    private boolean valid(TreePath dest, TreePath src) {
        if (dest == null || src == null || src == dest) return false;

        Main.TreeNode node = (Main.TreeNode) dest.getLastPathComponent();
        if (node.isLeaf()) return false;

        return true;
    }

    /** from DragSourceListener */
    public void dragOver(DragSourceDragEvent e) {
        Point loc = e.getLocation();
        SwingUtilities.convertPointFromScreen(loc,this);
        TreePath dest = getPathForLocation(loc.x, loc.y);

        if (!valid(dest, getSelectionPath()))
            e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
        else
            e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
    }

    /** from DragSourceListener */
    public void dragDropEnd(DragSourceDropEvent e) {}

    /** from DragSourceListener */
    public void dragEnter(DragSourceDragEvent e) {}

    /** from DragSourceListener */
    public void dropActionChanged(DragSourceDragEvent e) {}

    /** from DragSourceListener */
    public void dragExit(DragSourceEvent e) {}

    /** from DropTargetListener */
    public void dragOver(DropTargetDragEvent e) {}

    /** from DropTargetListener */
    public void dragEnter(DropTargetDragEvent e) {}

    /** from DropTargetListener */
    public void dragExit(DropTargetEvent e) {}

    /** from DropTargetListener */
    public void dropActionChanged(DropTargetDragEvent e) {}

    public class MyTransferable implements Transferable {
        Main.TreeNode _data;
        MyTransferable(Object data) { _data = (Main.TreeNode) data; }
        public Object getTransferData(DataFlavor flavor) { return _data; }
        public DataFlavor[] getTransferDataFlavors() { return myFlavor; }
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(myFlavor[0]);
        }
    }

    static {
        myFlavor = new DataFlavor[] {
            new DataFlavor(harn.chars.Main.TreeNode.class, DataFlavor.javaJVMLocalObjectMimeType)
        };
    }
}
