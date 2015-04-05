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

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.*;

/**
 * A drag tree (without drop).
 * @author Michael Jung
 */
public class DragTree extends JTree implements DragGestureListener, DropTargetListener, DragSourceListener {
    /** Main reference */
    private Main main;

    /** Drag source */
    private DragSource dragSource;

    /** Drag context */
    private DragSourceContext dragSourceContext;

    /** DND Flavor */
    private static DataFlavor[] myFlavor = new DataFlavor[] { DataFlavor.stringFlavor };

    /** Constructor */
    DragTree(DefaultTreeModel model, Main aMain) {
        super(model);
        main = aMain;
        dragSource = DragSource.getDefaultDragSource();

        DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer
            (this, DnDConstants.ACTION_MOVE, this);
        DropTarget dropTarget = new DropTarget(this, this);

        setCellRenderer(new DefaultTreeCellRenderer() {
                public Component getTreeCellRendererComponent
                    (JTree tree, Object value, boolean sel, boolean expanded,
                     boolean leaf, int row, boolean hasFocus) {

                    super.getTreeCellRendererComponent
                        (tree, value, sel, expanded, leaf, row, hasFocus);

                    if (leaf && main.develop != null && main.develop.developing != null &&
                        !((Data.DataTreeNode)value).getEnabled())
                        setForeground(Color.GRAY);
                    return this;
                }
            });
    }

    /** from DragGestureListener */
    public void dragGestureRecognized(DragGestureEvent e) {
        Data.DataTreeNode node = (Data.DataTreeNode) getLastSelectedPathComponent();
        if (node == null || !node.isLeaf()) return;
        TreeNode parent = node.getParent();
        if (e.getDragAction() == DnDConstants.ACTION_MOVE && main.develop != null &&
            (main.develop.developing == null||
             main.develop.costs.keySet().contains(node.getUserObject()))) {
            String sel = node.toString() + "(" + parent.toString() + ")";
            Transferable transfer = new MyTransferable(sel);
            dragSource.startDrag
                (e, DragSource.DefaultMoveDrop, transfer, main.displaymods.getTree());
        }
    }

    /** from DropTargetListener */
    public void drop(DropTargetDropEvent e) { e.rejectDrop(); }

    /** from DragSourceListener */
    public void dragOver(DragSourceDragEvent e) {}

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

    /** Transferable dummy class */
    public class MyTransferable implements Transferable {
        String _data;
        MyTransferable(Object data) { _data = (String) data; }
        public Object getTransferData(DataFlavor flavor) { return _data; }
        public DataFlavor[] getTransferDataFlavors() { return myFlavor; }
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(myFlavor[0]);
        }
    }
}
