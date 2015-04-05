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
package harn.combat;

import java.awt.event.*;
import java.util.*;
import java.awt.geom.*;
import java.awt.*;

/**
 * The class that handles mouse button clicks, left and right.
 * @author Michael Jung
 */
public class MoveByMouse extends MouseAdapter {
    /** Root reference */
    private Main main;

    /** Number of turns */
    private int turns;

    /** first position for measurement */
    private int ox, oy;

    /** Number of feet before measuring leagues instead of feet */
    private int propD;

    /**
     * Constructor.
     * @param main root reference
     */
    MoveByMouse(Main aMain, int aPropD) {
        main = aMain;
        propD = aPropD;
        turns = Integer.parseInt(main.myProps.getProperty("rotate.turns"));
        ox = oy = -1;
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

        Object o = main.tree.getLastSelectedPathComponent();
        int mask = e.getModifiers();

        // If Button1 select
        if ((mask & InputEvent.BUTTON1_MASK) != 0 &&
            (mask & InputEvent.SHIFT_MASK) == 0 &&
            (mask & InputEvent.CTRL_MASK) == 0) {

            int mw = main.map.getMarkerWidth();
            int mh = main.map.getMarkerHeight();

            AffineTransform cT =
                AffineTransform.getTranslateInstance(-mw/2, -mh/2);
            AffineTransform sT = main.map.getMarkerScaling();

            Iterator iter = main.map.getTokenIterator();
            while (iter.hasNext()) {
                String name = (String)iter.next();
                int px = main.map.getTokenPosX(name);
                int py = main.map.getTokenPosY(name);
                if (px < 0 || py < 0) continue;

                AffineTransform pT =
                    AffineTransform.getTranslateInstance(px, py);

                // pT * sT * rT * cT
                pT.concatenate(sT);
                pT.concatenate(cT);

                if (pT.createTransformedShape(new Rectangle(mw, mh)).contains
                    (e.getX(), e.getY())) {
                    main.treeSelect(name);
                    return;
                }
            }
        }
        // If Shift-Button1 set location
        if ((mask & InputEvent.BUTTON1_MASK) != 0 &&
            (mask & InputEvent.SHIFT_MASK) != 0 &&
            (mask & InputEvent.CTRL_MASK) == 0) {
            if (o == null) return;

            String name = o.toString();
            double[] pos = main.map.getTokenPos(name);

            if (pos == null) return;
            pos[0] = x/main.map.getScale();
            pos[1] = y/main.map.getScale();
            main.getState().setDirty(true);
            main.map.announceToken(name);
            main.map.repaint();
        }
        // If Button2 or Ctrl-Button1 rotate
        if ((mask & InputEvent.BUTTON1_MASK) == 0 &&
            (mask & InputEvent.SHIFT_MASK) == 0 &&
            (mask & InputEvent.CTRL_MASK) == 0 ||
            (mask & InputEvent.BUTTON1_MASK) != 0 &&
            (mask & InputEvent.SHIFT_MASK) == 0 &&
            (mask & InputEvent.CTRL_MASK) != 0) {
            if (o == null) return;

            String name = o.toString();
            double[] pos = main.map.getTokenPos(name);

            if (pos == null) return;
            pos[2] += 8*Math.atan(1)/turns;
            main.map.announceToken(name);
            main.getState().setDirty(true);
            main.map.repaint();
        }
        // If Shift-Button2 or Shift-Ctrl-Button1 display distance
        if ((mask & InputEvent.BUTTON1_MASK) == 0 &&
            (mask & InputEvent.SHIFT_MASK) != 0 &&
            (mask & InputEvent.CTRL_MASK) == 0 ||
            (mask & InputEvent.BUTTON1_MASK) != 0 &&
            (mask & InputEvent.SHIFT_MASK) != 0 &&
            (mask & InputEvent.CTRL_MASK) != 0) {
            if (ox == -1) {
                ox = e.getX();
                oy = e.getY();
            }
            else {
                String txt;
                int nx = e.getX();
                int ny = e.getY();
                double distp = (ox - nx)*(ox - nx) + (oy - ny)*(oy - ny);
                double dist =
                    Math.sqrt(distp)/main.map.getScale()/main.map.truescale;
                if (dist > propD/13200) txt = (int)(dist + .5) + " leagues";
                else txt = (int)(13200 * dist + .5) + " feet";
                main.bDist.setText(txt);
                ox = nx; oy = ny;
            }
        }
    }
}
