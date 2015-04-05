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
package harn.weather;

import java.awt.event.*;
import javax.swing.*;

/**
 * The class that handles mouse button clicks, left and right.
 * @author Michael Jung
 */
public class MoveByMouse extends JPopupMenu {
    /** main reference */
    private Main main;

    /**
     * Constructor
     * @param main main reference
     */
    MoveByMouse(Main aMain) {
        main = aMain;
        JMenuItem mi1 = new JMenuItem("Locate active group here");
        add(mi1);
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    work(true);
		}
	    });
        JMenuItem mi2 = new JMenuItem("Measure distance");
        add(mi2);
        mi2.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    work(false);
		}
	    });
    }

    /**
     * Work method of the menu, called when a click occured. Prepares
     * information and executes move if required.
     * @param move move or just measure
     */
    public void work(boolean move) {
	// Get coordinates
	WeatherDisplay w = main.getDisplay();
	int wm = w.getPreferredSize().width;
	int hm = w.getPreferredSize().height;
	int x = main.gx;
	int y = main.gy;

	// Out of bounds
	if (x < 0 || y < 0 || x > wm || y > hm) return;

	// Calculate distance
	long delta = main.getState().calcDist(x,y);

	// If Button1, move
	if (delta >= 0 && move)
            main.getState().addDate(delta);
    }
}
