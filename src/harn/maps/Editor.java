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

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import rpg.*;

/**
 * The main edit class for the maps plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class Editor extends MouseAdapter {
    /** Main reference */
    private Main main;

    /** Drawing starts */
    private int sx = -1;    
    private int sy = -1;

    /** Points of polygons (if used) */
    private int[] px;
    private int[] py;

    /** motion listener */
    MouseMotionListener ml;

    /** Editing shape */
    Shape currShape;

    /** Constructor */
    Editor(Main aMain) {
        main = aMain;
        ml = new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    // Get coordinates
                    int x = e.getX();
                    int y = e.getY();
                    if (main.bRect.isSelected()) {
                        currShape = new Rectangle
                            (Math.min(x, sx), Math.min(y,sy),
                             Math.abs(x - sx), Math.abs(y - sy));
                    }
                    if (main.bCirc.isSelected()) {
                        if ((y - sy)*(x - sx) < 0)
                            currShape = new Ellipse2D.Float
                                (Math.min(sx, sx - (y - sy)), Math.min(y,sy),
                                 Math.abs(y - sy), Math.abs(y - sy));
                        else
                            currShape = new Ellipse2D.Float
                                (Math.min(sx, sx - (sy - y)), Math.min(y,sy),
                                 Math.abs(y - sy), Math.abs(y - sy));
                    }
                    if (main.bPoly.isSelected()) {
                        pointToPolygon(x, y, true);
                    }
                    main.map.repaint();
                }
            };
    }

    /**
     * Method of the mouse adapter, called when a click on the map
     * occured. Prepares information and executes move if required.
     * @param e event that caused the call
     */
    public void mouseClicked(MouseEvent e) {
	// Get coordinates
	int x = e.getX();
	int y = e.getY();
	// If Button1 start
        // If Button2 end Ctrl-Button1 end
	if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 &&
            (e.getModifiers() & InputEvent.CTRL_MASK) == 0) {
            if (sx == -1) {
                sx = x;
                sy = y;
                px = new int[] { sx, sx };
                py = new int[] { sy, sy };
                main.map.addMouseMotionListener(ml);
            }
            else if (main.bPoly.isSelected()) {
                pointToPolygon(x, y, false);
            }
        }
        else { // Button2
            main.map.removeMouseMotionListener(ml);
            sx = sy = -1;
        }
    }

    /**
     * Puts a point to the current polygon.
     * @param x x coord
     * @param y y coord
     * @param replace replace the last point instead of adding
     */
    private void pointToPolygon(int x, int y, boolean replace) {
        if (!replace) {
            int[] tpx = new int[px.length + 1];
            int[] tpy = new int[py.length + 1];
            for (int i = 0; i < px.length; i++) {
                tpx[i] = px[i];
                tpy[i] = py[i];
            }
            px = tpx;
            py = tpy;
        }
        px[px.length - 1] = x;
        py[py.length - 1] = y;
        currShape = new Polygon(px, py, px.length);
        main.map.repaint();
    }
}
