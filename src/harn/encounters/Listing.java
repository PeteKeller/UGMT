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
package harn.encounters;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.text.*;
import rpg.*;
import java.util.*;
import org.w3c.dom.*;

/**
 * Listing of all events rolled at the GUI.
 * @author Michael Jung
 */
public class Listing extends JPanel {
    /** Main reference */
    private Main main;

    /** idx to Gui items */
    private Hashtable list;

    /** Constraints */
    private GridBagConstraints constr;

    /** Date text dimension */
    public static Dimension tdim;

    /** toggle hide pseudo-events */
    public boolean hidePseudos;

    /** Constructor */
    public Listing(Main aMain) {
	super();
        main = aMain;
        list = new Hashtable();
        hidePseudos = false;

	setLayout(new GridBagLayout());

        constr = new GridBagConstraints();
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.weightx = 1;

	FontMetrics fm = getFontMetrics(getFont());
	int th = fm.getHeight();
        int tw = fm.stringWidth("99-WWW-9999 9:99");
        tdim = new Dimension(tw, th);
    }

    /**
     * Add an event.
     */
    public void setEvent(int idx, EncounterEvent ev) {
        String idxs = Integer.toString(idx);
        GuiLine line = (GuiLine) list.get(idxs);
        if (line == null) {
            line = new GuiLine(main, ev);
            list.put(idxs, line);
        }
        else {
            line.setEvent(ev);
        }
        if (!main.myState.bulkchange) reorder();
    }

    /**
     * Toggle the hiding of pseudo events.
     */
    public void toggle() {
        hidePseudos = !hidePseudos;
        reorder();
    }

    /**
     * Reorder all events on GUI.
     */
    public void reorder() {
        Iterator iter = new TreeSet(list.keySet()).iterator();
        removeAll();
        int i = constr.gridy = 0;
        while (iter.hasNext()) {
            String key = (String) iter.next();
            int keyi = Integer.parseInt(key) * Main.WATCH;
            if (!main.myState.isTime(keyi)) {
                list.remove(key);
                continue;
            }
            GuiLine line = (GuiLine) list.get(key);
            if (!hidePseudos || line.isActive(Integer.parseInt(key))) {
                constr.gridy = i++;
                add(line, constr);
            }
        }
        revalidate();
        repaint();
    }

    /**
     * Redo groups.
     */
    public void redoGroup() {
        Iterator iter = list.keySet().iterator();
        while (iter.hasNext()) {
            ((GuiLine)list.get(iter.next())).reset();
        }
    }
}
