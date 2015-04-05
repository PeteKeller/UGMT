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

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import rpg.*;

/**
 * A drag-and-drop tree.
 * @author Michael Jung
 */
public class DNDTree extends JTree implements DragGestureListener, DropTargetListener, DragSourceListener {
    /** Transferable flavor */
    static DataFlavor[] myFlavor;

    /** Main reference */
    Main main;

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
        TreeNode node = (TreeNode) getLastSelectedPathComponent();
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

        TreeNode newParent = (TreeNode) dest.getLastPathComponent();
        TreeNode child = (TreeNode) src.getLastPathComponent();

        // Old files
        File txt = child.getTxt();
        File png = child.getPng();
        File dir = child.getDir();

        // New files
        String newPath = newParent.getDir().getAbsolutePath() + Framework.SEP;
        File txtN = new File(newPath + txt.getName());
        File pngN = new File(newPath + png.getName());
        File dirN = new File(newPath + png.getName());

        // Check for overwrite
        boolean mv = (txt == null) || txtN.exists();
        mv &= (png == null) || pngN.exists();
        mv &= (dir == null) || dirN.exists();

        // Check for copy problems
        if (txt != null && mv) mv &= txt.renameTo(new File(newPath + txt.getName()));
        if (png != null && mv) mv &= png.renameTo(new File(newPath + png.getName()));
        if (dir != null && mv) mv &= dir.renameTo(new File(newPath + png.getName()));

        if (!mv) {
            e.rejectDrop();
            return;
        }
        
        e.acceptDrop (DnDConstants.ACTION_MOVE);

        ((SortedTreeModel)getModel()).removeNodeFromParent(child);
        ((SortedTreeModel)getModel()).insertNodeSorted(child, newParent);

        e.getDropTargetContext().dropComplete(true);
    }

    /** Drop validity check. */
    private boolean valid(TreePath dest, TreePath src) {
        if (dest == null || src == null || src == dest) return false;

        TreeNode node = (TreeNode) dest.getLastPathComponent();
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
        TreeNode _data;
        MyTransferable(Object data) { _data = (TreeNode) data; }
        public Object getTransferData(DataFlavor flavor) { return _data; }
        public DataFlavor[] getTransferDataFlavors() { return myFlavor; }
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(myFlavor[0]);
        }
    }

    static {
        myFlavor = new DataFlavor[] {
            new DataFlavor(harn.sketch.TreeNode.class, DataFlavor.javaJVMLocalObjectMimeType)
        };
    }
}
