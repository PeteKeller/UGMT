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
package harn.maps;

import java.awt.event.*;
import javax.swing.tree.*;
import java.util.Enumeration;

/**
 * The class that handles mouse button clicks, left and right.
 * @author Michael Jung
 */
public class LinkByMouse extends MouseAdapter {
    /** Root reference */
    private Main main;

    /**
     * Constructor.
     * @param main root reference
     */
    LinkByMouse(Main aMain) { main = aMain; }

    /**
     * Method of the mouse adapter, called when a click on the map
     * occured. Prepares information and executes move if required.
     * @param e event that caused the call
     */
    public void mouseClicked(MouseEvent e) {
        // If not Button1 return
	if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == 0)
            return;

	// Get coordinates
	int x = e.getX();
	int y = e.getY();
	int wm = main.dirMap.getMyWidth();
	int hm = main.dirMap.getMyHeight();

	// Out of bounds
	if (x < 0 || y < 0 || x > wm || y > hm) return;

        String map = main.dirMap.getLinkAt(x,y);

        if (map == null) return;
        // Adapt tree selection
        String path[] = map.split("/");
        DefaultMutableTreeNode parent = main.maptree;
        for (int i = 0; i < path.length; i++) {
            Enumeration iter = parent.children();
            while (iter.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    iter.nextElement();
                if (!node.toString().equals(path[i])) continue;
                parent = node;
                break;
            }
        }
        main.tree.addSelectionPath(new TreePath(parent.getPath()));
    }
}
