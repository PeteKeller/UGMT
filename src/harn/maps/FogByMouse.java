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
import harn.repository.*;
import java.awt.image.*;
import java.awt.*;

/**
 * The class that handles mouse button clicks, left and right.
 * @author Michael Jung
 */
public class FogByMouse extends MouseAdapter {
    /** Root reference */
    private Main main;

    /** Points of polygons (if used) */
    private int[] px;
    private int[] py;

    /** Running marker */
    boolean inUse = false;

    /** Points of markings */
    private int ox, oy;

    /** Editing shape */
    Shape currShape;

    /** paint motion listener */
    MouseMotionListener paintMove;

    /** paint motion listener */
    MouseMotionListener polyMove;

    /**
     * Constructor.
     * @param main root reference
     */
    FogByMouse(Main aMain) {
        main = aMain;
        px = new int[0];
        py = new int[0];
        ox = oy = -1;
        polyMove = new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    // Get coordinates
                    int x = e.getX();
                    int y = e.getY();
                    pointToPolygon(x, y, 0);
                }
            };

        paintMove = new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    mouseClicked(e);
                }
            };
        Thread t = new Thread() {
                public void run() {
                    while(true) {
                        try { sleep(200); } catch(Exception e) {}
                        if (!inUse) ox = oy = -1;
                        inUse = false;
                    }
                }
            };
        t.start();
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
	int wm = main.map.getPreferredSize().width;
	int hm = main.map.getPreferredSize().height;

	// Out of bounds
	if (x < 0 || y < 0 || x > wm || y > hm) return;

        Location[] locs = main.map.getLocationsAt(x,y);

        int layer = main.getFogLayer();
            
        // If a polygon is not in progress
        if (px.length == 0 &&
            (e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
            ox = oy = -1;
            // If Button1 hide
            // If Button2 or Ctrl-Button1 unhide
            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 &&
                (e.getModifiers() & InputEvent.CTRL_MASK) == 0) {
                main.map.stain(layer, x, y, true);
            }
            else {
                main.map.stain(layer, x, y, false);
            }
        }
        else if (px.length == 0 &&
                 (e.getModifiers() & InputEvent.SHIFT_MASK) != 0 &&
                 (e.getModifiers() & InputEvent.BUTTON1_MASK) == 0) {
            if (ox > -1 && oy > -1) {
                main.map.stain(layer, x, y, ox, oy);
            }
            ox = x; oy = y;
            inUse = true;
        }
        else {
            ox = oy = -1;
            main.map.addMouseMotionListener(polyMove);
            // Start/continue drawing
            if (px.length == 0) {
                px = new int[] { x, x };
                py = new int[] { y, y };
            }
            else {
                // If Shift-Button1 hide
                // If Shift-Button2 unhide
                if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 &&
                    (e.getModifiers() & InputEvent.CTRL_MASK) == 0) {
                    pointToPolygon(x, y, 1);
                }
                if ((((e.getModifiers() & InputEvent.BUTTON1_MASK) == 0 ||
                      ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 &&
                       (e.getModifiers() & InputEvent.CTRL_MASK) != 0))) &&
                    (e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
                    pointToPolygon(x, y, -1);
                }
                if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 &&
                    (e.getModifiers() & InputEvent.SHIFT_MASK) != 0 &&
                    (e.getModifiers() & InputEvent.CTRL_MASK) == 0 &&
                    px.length > 2) {
                    main.map.stain(layer, currShape, true);
                    clean();
                }
                if (((e.getModifiers() & InputEvent.BUTTON1_MASK) == 0 ||
                     ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 &&
                      (e.getModifiers() & InputEvent.CTRL_MASK) != 0)) &&
                    (e.getModifiers() & InputEvent.SHIFT_MASK) != 0 &&
                    px.length > 2) {
                    main.map.stain(layer, currShape, false);
                    clean();
                }
            }
        }
    }

    /**
     * Puts a point to the current polygon.
     * @param x x coord
     * @param y y coord
     * @param dir direction in which point is to be had
     */
    private void pointToPolygon(int x, int y, int dir) {
        int[] tpx = null;
        int[] tpy = null;
        if (currShape != null) {
            Rectangle r = currShape.getBounds();
            r.grow(1,1);
            main.map.repaint(r, null);
        }
        switch (dir) {
        case 1:
            tpx = new int[px.length + 1];
            tpy = new int[py.length + 1];
            for (int i = 0; i < px.length; i++) {
                tpx[i] = px[i];
                tpy[i] = py[i];
            }
            tpx[px.length] = x;
            tpy[py.length] = y;
            px = tpx;
            py = tpy;
            break;
        case -1 :
            if (px.length > 2) {
                tpx = new int[px.length - 1];
                tpy = new int[py.length - 1];
                for (int i = 0; i < px.length - 1; i++) {
                    tpx[i] = px[i];
                    tpy[i] = py[i];
                }
                tpx[px.length - 2] = px[px.length - 1];
                tpy[py.length - 2] = py[px.length - 1];
                px = tpx;
                py = tpy;
            }
            else {
                clean();
            }
            break;
        case 0:
            if (px.length > 0) {
                px[px.length - 1] = x;
                py[py.length - 1] = y;
            }
            break;
        }
        if (px.length > 0) {
            currShape = new Polygon(px, py, px.length);
            main.map.repaint(currShape.getBounds(), null);
        }
    }

    /**
     * Clean out.
     */
    public void clean() {
        main.map.removeMouseMotionListener(polyMove);
        px = new int[0];
        py = new int[0];
        currShape = null;
    }
}
