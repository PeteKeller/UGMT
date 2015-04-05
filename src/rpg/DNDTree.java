/*
 * UGMT : Universal Gamemaster tool
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

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

/**
 * A drag-and-drop tree. It implements all the necessary formal interfaces and
 * realizes drag and drop through the DragSource interface, which means that
 * the drag source will supervise dragging.  Only allows dragging of objects
 * implementing the internal Interface <em>DragNode</em>.  Eligibility for
 * dropping is decided by evaluation of the <em>valid</em> method on the tree
 * paths.
 * @author Michael Jung
 */
public abstract class DNDTree extends JTree implements DragGestureListener, DropTargetListener, DragSourceListener {
    /** Drag source */
    private DragSource dragSource;

    /** Drag context */
    private DragSourceContext dragSourceContext;

    /** DND Flavor */
    private static DataFlavor[] myFlavor;

    /**
     * Constructor.
     * @param model usually a SortedTreeModel
     */
    public DNDTree(DefaultTreeModel model) {
        super(model);
        // Defaults
        setEditable(true);

        dragSource = DragSource.getDefaultDragSource() ;
        DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer
            (this, DnDConstants.ACTION_MOVE, this);
        DropTarget dropTarget = new DropTarget(this, this);
    }

    /** From DragGestureListener */
    public void dragGestureRecognized(DragGestureEvent e) {
        TreePath tp =
            getClosestPathForLocation(e.getDragOrigin().x, e.getDragOrigin().y);
        setSelectionPath(tp);
        TreeNode tn = (TreeNode) tp.getLastPathComponent();
        if (tn instanceof DragNode) {
            DragNode node = (DragNode) tn;
            if (node != null && e.getDragAction() == DnDConstants.ACTION_MOVE) {
                Transferable transfer = new MyTransferable(node);
                dragSource.startDrag
                    (e, DragSource.DefaultMoveDrop, transfer, this);
            }
        }
    }

    /** From DragSourceListener */
    public void dragOver(DragSourceDragEvent e) {
        // Convert point to local coordinates of dragged-over object
        Point loc = e.getLocation();
        SwingUtilities.convertPointFromScreen(loc, SwingUtilities.getRoot(this));
        Component c = SwingUtilities.getDeepestComponentAt
            (SwingUtilities.getRoot(this), loc.x, loc.y);

        // Drop only on other DNDTrees
        if (!(c instanceof DNDTree)) {
            e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
            return;
        }

        // Get dragged over tree path
        loc = e.getLocation();
        DNDTree target = (DNDTree) c;
        SwingUtilities.convertPointFromScreen(loc, target);
        TreePath dest = target.getPathForLocation(loc.x, loc.y);

        try {
            // Get dragging tree path
            Transferable tr = e.getDragSourceContext().getTransferable();
            TreePath src = null;
            if (tr.isDataFlavorSupported(myFlavor[0]))
                src = getSelectionPath();

            // Set cursor appropriately
            if (target.valid(dest, src))
                e.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
            else
                e.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
        }
        catch (Exception ex) {
            // Debug
            ex.printStackTrace();
        }
    }

    /**
     * Drop validity check. This needs to be implemented by subclasses
     * according to their needs.
     * @param src source tree path
     * @param dest destination tree path
     * @return drop possible or not
     */
    protected abstract boolean valid(TreePath dest, TreePath src);

    /** From DragSourceListener */
    public void dragDropEnd(DragSourceDropEvent e) {}

    /** From DragSourceListener */
    public void dragEnter(DragSourceDragEvent e) {}

    /** From DragSourceListener */
    public void dropActionChanged(DragSourceDragEvent e) {}

    /** From DragSourceListener */
    public void dragExit(DragSourceEvent e) {}

    /** From DropTargetListener */
    public void dragOver(DropTargetDragEvent e) {}

    /** From DropTargetListener */
    public void dragEnter(DropTargetDragEvent e) {}

    /** From DropTargetListener */
    public void dragExit(DropTargetEvent e) {}

    /** From DropTargetListener */
    public void dropActionChanged(DropTargetDragEvent e) {}

    /** Interface to implement for draggable objects */
    public interface DragNode {}

    /** Flavor used in DNDTrees */
    final static public DataFlavor[] getFlavor() { return myFlavor; }

    /**
     * Transferable wrapping class.
     */
    class MyTransferable implements Transferable {
        /** wrapped object */
        DragNode _data;

        /**
         * Constructor.
         * @param data object to wrap
         */
        MyTransferable(Object data) { _data = (DragNode) data; }

        /** From Transferable */
        public Object getTransferData(DataFlavor flavor) { return _data; }

        /** From Transferable */
        public DataFlavor[] getTransferDataFlavors() { return myFlavor; }

        /** From Transferable */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(myFlavor[0]);
        }
    }

    static {
        try {
            myFlavor = new DataFlavor[] {
                new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType)
            };
        }
        catch (ClassNotFoundException e) {
            // Debug
            e.printStackTrace();
        }
    }
}
