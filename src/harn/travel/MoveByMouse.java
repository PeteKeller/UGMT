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
package harn.travel;

import java.awt.event.*;

/**
 * The class that handles mouse button clicks, left and right.
 * @author Michael Jung
 */
public class MoveByMouse extends MouseAdapter {
    /** Root reference */
    private Main main;

    /**
     * Constructor.
     * @param main root reference
     */
    MoveByMouse(Main aMain) { main = aMain; }

    /**
     * Method of the mouse adapter, called when a click on the map
     * occured. Prepares information and executes move if required.
     * @param e event that caused the call
     */
    public void mouseClicked(MouseEvent e) {
	// Get coordinates
	MapHolder m = main.getMap();
	int x = e.getX();
	int y = e.getY();
	int wm = m.getMyWidth();
	int hm = m.getMyHeight();

	// Out of bounds
	if (x < 0 || y < 0 || x > wm || y > hm) return;

	// Calculate distance
	int delta = main.getState().calcDist(x, y, main.bFree.getText());
	// If Button1, travel
	if (delta >= 0 &&
            ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) &&
            ((e.getModifiers() & InputEvent.CTRL_MASK) == 0) ) {
	    m.move(x, y, delta);
            main.contTravel.setEnabled(false);
	}
    }
}
