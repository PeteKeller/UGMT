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
 * The class that handles mouse movements to show short information lines.
 * @author Michael Jung
 */
public class InfoByMouse extends MouseMotionAdapter {
    /** Root reference */
    private Main main;

    /** info max size */
    private int iwdth, ihght;

    /**
     * Constructor.
     * @param main root reference
     */
    InfoByMouse(Main aMain) {
        main = aMain;
        ihght = Integer.parseInt(main.myProps.getProperty("info.height"));
        iwdth = Integer.parseInt(main.myProps.getProperty("info.width"));
    }

    /**
     * IF method
     */
    public void mouseMoved(MouseEvent e) {
        mouseMoved(e.getX(),e.getY());
    }

    /** Convenient method */
    public void mouseMoved(int x, int y) {
        Location[] locs = main.map.getLocationsAt(x,y);
        String tooltip = null;
        if (main.bToggle.getText().equals("<html><i>Navigate")) {
            for (int i = 0; i < 1 && i < locs.length; i++) {
                int idx = locs[i].getName().lastIndexOf("/");
                if (main.bFullInfo.isSelected())
                    tooltip = format(locs[i].getPage());
                else
                    tooltip = locs[i].getName().substring(idx+1);
            }
            main.map.setToolTipText(tooltip);
        }
        main.moveMenu.setLocs(locs);
        main.moveMenu.setMaps(main.myData.checkForSubmap(x,y));
    }

    /** Format the text from index plugin */
    public String format(String txt) {
        txt = txt.substring(0, Math.min(ihght*iwdth, txt.length()));
        txt = txt.replaceAll("<[^>]*>","");
        StringBuffer buf = new StringBuffer(txt);
        for (int i = 0, j = 0; i < txt.length(); i++, j++) {
            if (j > iwdth && Character.isSpaceChar(buf.charAt(i))) {
                buf.insert(i+1, "<br>");
                j = 0;
                i += 3;
            }
        }
        txt = buf.toString();
        return "<html>" + txt + "...";
    }
}
