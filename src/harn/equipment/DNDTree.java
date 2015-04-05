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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import rpg.SortedTreeModel;

/**
 * A drag-and-drop tree. The two right views are drag-and-drog, while the left
 * is drag only, but duplicates items.
 * @author Michael Jung
 */
public class DNDTree extends rpg.DNDTree {
    /** Main reference */
    private Main main;

    /** Kill popup */
    private JPopupMenu pMenu;

    /** mouse pressed coordinates */
    private int gx, gy;

    /** Constructor */
    DNDTree(DefaultTreeModel model, Main aMain) {
        super(model);
        main = aMain;

        pMenu = new JPopupMenu();
        JMenuItem mi1 = new JMenuItem("Delete");
        pMenu.add(mi1);
        mi1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = getPathForLocation(gx, gy);
                    if (tp == null) return;
                    MutableTreeNode tn = (MutableTreeNode) tp.getLastPathComponent();
                    if (tn == getModel().getRoot()) return;
                    if (tn instanceof Item) {
                        Item it = (Item)tn;
                        it.equipment.setInvalid();
                        main.myFrw.announce(it.equipment);
                    }
                    ((SortedTreeModel)getModel()).removeNodeFromParent(tn);
                    main.bSave.setEnabled(true);
                }
            });
        JMenuItem mi2 = new JMenuItem("Clone");
        pMenu.add(mi2);
        mi2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TreePath tp = getPathForLocation(gx, gy);
                    if (tp == null) return;
                    MutableTreeNode tn = (MutableTreeNode) tp.getLastPathComponent();
                    if (tn == getModel().getRoot()) return;
                    if (tn instanceof Item && tn.getParent() instanceof Item) {
                        Item it = (Item)tn;
                        Item nit = it.cloneItem
                            ((SortedTreeModel)getModel(),
                             (DefaultMutableTreeNode)it.getParent(), true);
                        main.myFrw.announce(nit.equipment);
                        main.bSave.setEnabled(true);
                    }
                }
            });

        addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX(); gy = e.getY();
                        pMenu.show(e.getComponent(), gx, gy);
                    }
                }
            });
    }

    /** from DropTargetListener */
    public void drop(DropTargetDropEvent e) {
        Transferable tr = e.getTransferable();

        if (!tr.isDataFlavorSupported(getFlavor()[0])) {
            e.rejectDrop();
            return;
        }

        // Get new parent node
        Point loc = e.getLocation();
        TreePath dest = getPathForLocation(loc.x, loc.y);
        TreePath src = null;
        TreeModel srcM = null;
        try {
            Item child;
            child = (Item) tr.getTransferData(getFlavor()[0]);

            TreeNode root = child.getRoot();
            if (main.treetable.getTree().getModel().getRoot() == root) {
                srcM = main.treetable.getTree().getModel();
                src = main.treetable.getTree().getSelectionPath();
            }
            else if (main.remainder.getTree().getModel().getRoot() == root) {
                srcM = main.remainder.getTree().getModel();
                src = main.remainder.getTree().getSelectionPath();
            }
            else {
                srcM = main.dataModel;
                src = main.dataList.getSelectionPath();
            }

            if (!valid(dest, src) ||
                ((e.getDropAction() & 0x03) == 0)) {
                e.rejectDrop();
                return;
            }
            e.acceptDrop(DnDConstants.ACTION_MOVE);

            DefaultMutableTreeNode newParent =
                (DefaultMutableTreeNode) dest.getLastPathComponent();

            if (srcM != main.dataModel) {
                ((SortedTreeModel)srcM).removeNodeFromParent(child);
                ((SortedTreeModel)getModel()).insertNodeSorted(child, newParent);
            }
            else { // New clone, auto insert into model
                child = child.cloneItem((SortedTreeModel)getModel(), newParent, false);
            }

            main.bSave.setEnabled(true);
            e.getDropTargetContext().dropComplete(true);
            main.myFrw.announce(child.equipment);
        }
        catch (Exception ex) {
            // Debug
            ex.printStackTrace();
        }
    }

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
}
