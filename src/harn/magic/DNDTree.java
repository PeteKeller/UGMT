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
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import rpg.SortedTreeModel;
import java.util.EventObject;

/**
 * A drag-and-drop tree.
 * @author Michael Jung
 */
public class DNDTree extends rpg.DNDTree {
    /** Main reference */
    private Main main;

    /** Constructor */
    DNDTree(DefaultTreeModel model, Main aMain) {
        super(model);
        main = aMain;
    }

    /** from DropTargetListener */
    public void drop(DropTargetDropEvent e) {
        Transferable tr = e.getTransferable();

        if (!tr.isDataFlavorSupported(getFlavor()[0]) &&
            !tr.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            e.rejectDrop();
            return;
        }

        // Get new parent node
        Point loc = e.getLocation();
        TreePath dest = getPathForLocation(loc.x, loc.y);
        TreePath src = null;
        TreeModel srcM = null;
        try {
            Spell child = null;
            if (tr.isDataFlavorSupported(getFlavor()[0])) {
                child = (Spell) tr.getTransferData(getFlavor()[0]);
                src = getSelectionPath();
                srcM = getModel();
            }

            if (!valid(dest, src) ||
                ((e.getDropAction() & 0x03) == 0)) {
                e.rejectDrop();
                return;
            }

            if (!tr.isDataFlavorSupported(getFlavor()[0])) {
                child = new Spell
                    ((Holder) dest.getLastPathComponent(),
                     (String) tr.getTransferData(DataFlavor.stringFlavor), main);
                if (main.develop.developing != null)
                    main.develop.proceed((String)((DefaultMutableTreeNode)child).getUserObject());
            }

            e.acceptDrop(DnDConstants.ACTION_MOVE);

            DefaultMutableTreeNode newParent =
                (DefaultMutableTreeNode) dest.getLastPathComponent();

            try {
                if (srcM != null) ((SortedTreeModel)srcM).removeNodeFromParent
                                      ((DefaultMutableTreeNode)child);
            }
            catch (NullPointerException ex) {
                // Bug in (Linux only?) Swing Plaf
            }
            ((SortedTreeModel)getModel()).insertNodeSorted
                ((DefaultMutableTreeNode)child, newParent);

            main.bSave.setEnabled(true);
            e.getDropTargetContext().dropComplete(true);
        }
        catch (Exception ex) {
            // Debug
            ex.printStackTrace();
        }
    }

    /** Drop validity check. */
    protected boolean valid(TreePath dest, TreePath src) {
        if (dest == null || src == dest) return false;
        if (dest.getPathCount() != 2) return false;
        return (src == null || src.getPathCount() == 3);
    }
}
