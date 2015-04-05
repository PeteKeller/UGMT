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
import javax.swing.*;
import java.awt.image.*;
import java.awt.*;

/**
 * The class that handles mouse button clicks, left and right.
 * @author Michael Jung
 */
public class MoveByMenu extends JPopupMenu {
    /** Root reference */
    private Main main;

    /** Locate active group */
    private JMenuItem locAct;

    /** Choose submap */
    private JMenuItem subMap;

    /** Show index entry */
    private JMenuItem showEntry;

    /** Show distance */
    private JMenuItem showDist;

    /** first points for distance */
    private int ox = -1, oy = -1;

    /** Number of feet before measuring leagues instead of feet */
    private int propD;

    /**
     * Constructor.
     * @param main root reference
     */
    MoveByMenu(Main aMain, int aPropD) {
        main = aMain;
        propD = aPropD;
        locAct = new JMenuItem("Locate active group here");
        add(locAct);
        locAct.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int wm = main.map.getPreferredSize().width;
                    int hm = main.map.getPreferredSize().height;

                    // Out of bounds
                    if (main.gx < 0 || main.gy < 0 ||
                        main.gx > wm || main.gy > hm) return;

                    float scale = main.map.getScale();
                    CharGroup cg = main.getState().getActive();
                    if (cg != null)
                        cg.setLocation
                            (main.map.getCurrentName(), (int)(main.gx/scale),
                             (int)(main.gy/scale), cg.getDate());
                }
            });
        subMap = new JMenuItem("Go to Submap");
        subMap.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int wm = main.map.getPreferredSize().width;
                    int hm = main.map.getPreferredSize().height;

                    // Out of bounds
                    if (main.gx < 0 || main.gy < 0 ||
                        main.gx > wm || main.gy > hm) return;

                    float scale = main.map.getScale();
                    main.myData.setCurrent
                        ((int)(main.gx/scale),(int)(main.gy/scale));
                }
            });
        showEntry = new JMenuItem("Go to Index entry");
        showEntry.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int wm = main.map.getPreferredSize().width;
                    int hm = main.map.getPreferredSize().height;

                    // Out of bounds
                    if (main.gx < 0 || main.gy < 0 ||
                        main.gx > wm || main.gy > hm) return;

                    float scale = main.map.getScale();
                    Location[] locs = main.map.getLocationsAt(main.gx,main.gy);
                    for (int i = 0; i < 1 && i < locs.length; i++) {
                        locs[i].setSelected();
                    }
                }
            });
        showDist = new JMenuItem("Measure distance");
        add(showDist);
        showDist.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int wm = main.map.getPreferredSize().width;
                    int hm = main.map.getPreferredSize().height;

                    // Out of bounds
                    if (main.gx < 0 || main.gy < 0 ||
                        main.gx > wm || main.gy > hm) return;

                    float scale = main.map.getScale();
                    if (ox == -1) {
                        ox = (int)(main.gx/scale);
                        oy = (int)(main.gy/scale);
                    }
                    else {
                        String txt;
                        int nx = (int)(main.gx/scale);
                        int ny = (int)(main.gy/scale);
                        double distp = (ox - nx)*(ox - nx) + (oy - ny)*(oy - ny);
                        double dist =
                            Math.sqrt(distp) / main.myData.getCurrent().getScale();
                        if (dist > propD/13200)
                            txt = (int)(dist + .5) + " leagues";
                        else
                            txt = (int)(13200 * dist + .5) + " feet";
                        main.bDist.setText(txt);
                        ox = nx; oy = ny;
                    }
                }
            });
    }

    public void setLocs(Location[] locs) {
        if (locs == null || locs.length == 0)
            remove(showEntry);
        else
            add(showEntry);
    }

    public void setMaps(boolean set) {
        if (set) add(subMap); else remove(subMap);
    }

    /**
     * Reset the distance ruler.
     */
    public void reset() { ox = oy = -1; main.bDist.setText(" - "); }
}
