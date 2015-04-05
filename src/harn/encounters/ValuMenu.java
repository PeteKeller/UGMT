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

import rpg.*;
import harn.repository.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Class to menus in order to ease selection of sketches and index entries.
 * @author Michael Jung
 */
public class ValuMenu extends JPopupMenu implements IListener {
    /** Root sketch objects */
    private HashSet sketch;

    /** popup for sketches */
    private JPopupMenu skMenu;

    /** popup for index */
    private JPopupMenu indMenu;

    /** reference to value field */
    private JTextField value;

    /** Main reference */
    private Main main;

    /** Constructor */
    public ValuMenu(Main aMain) {
        main = aMain;
        sketch = new HashSet();
        skMenu = new JPopupMenu();
        indMenu = new JPopupMenu();
    }

    /** Override */
    public void show(Component invoker, int x, int y) {
        value = (JTextField) invoker;
        Object type = ((GuiLine)value.getParent()).types.getSelectedItem();

        if (type.equals("note")) {
            skMenu.removeAll();
            createSketchItems(skMenu, null, "/");
            skMenu.show(invoker, x, y);
        }
        else if (type.equals("index")) {
            indMenu.removeAll();
            createIndexItems(indMenu);
            indMenu.show(invoker, x, y);
        }
    }

    /** Build index menu */
    private void createIndexItems(JPopupMenu pop) {
        Iterator iter = main.myState.locs.keySet().iterator();
        while (iter.hasNext()) {
            // Split off only along the first two slashes. (Index and
            // subindex.)
            final String total = (String)iter.next();
            String[] split = total.split("/");
            for (int i = 3; i < split.length; i++)
                split[2] += "/" + split[i];

            JMenuItem mi = null;
            for (int i = 0; i < split.length; i++) {
                JMenuItem menu = getMenuItem
                    (pop, (MyMenu)mi, split[i], i == split.length - 1);
                mi = menu;
            }
            // Create listener
            ActionListener al = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JMenuItem item = (JMenuItem)e.getSource();
                        value.setText(total);
                    }
                };
            mi.addActionListener(al);
        }
    }

    /**
     * Create or use a new MenuItem.
     * @param root item to insert into
     * @param nxt string to create menu
     * @param last last item
     * @return added MenuItem
     */
    private JMenuItem getMenuItem(JPopupMenu pop, MyMenu root, String next, boolean last) {
        JMenuItem mi = null;
        if (root == null) {
            MenuElement el[] = pop.getSubElements();
            for (int i = 0; i < el.length; i++)
                if (((JMenuItem)el[i]).getText().equals(next))
                    return (JMenuItem) el[i];
            if (last)
                mi = new MyMenuItem("", next);
            else
                mi = (JMenuItem)new MyMenu("",next);

            pop.add(mi);
        }
        else {
            for (int i = 0; i < root.getItemCount(); i++) {
                if (((JMenuItem)root.getItem(i)).getText().equals(next))
                    return (JMenuItem) root.getItem(i);
            }
            String heritage = root.heritage + root.getText();
            if (last)
                mi = new MyMenuItem(heritage, next);
            else
                mi = (JMenuItem)new MyMenu(heritage, next);
            
            root.add(mi);
        }
        return mi;
    }

    /** Build sketch menu */
    private void createSketchItems(JComponent menu, Sketch root, final String prefix) {
        Object[] list = null;

        // Get recursive list
        if (root == null)
            list = sketch.toArray();
        else
            list = root.getSubSketches();
        if (list == null) return;

        // Iterate
        for (int i = 0; i < list.length; i++) {
            final Sketch sk = (Sketch) list[i];
            Object[] oa = sk.getSubSketches();
            JComponent item = null;

            // Create appropriate sub-item
            if (oa == null || oa.length == 0) {
                item = new JMenuItem(sk.getName());
                ActionListener al = new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            JMenuItem item = (JMenuItem)e.getSource();
                            String txt = prefix + sk.getName();
                            value.setText(txt);
                        }
                    };
                ((JMenuItem)item).addActionListener(al);
            }
            else {
                item = new JMenu(sk.getName());
            }

            // Recurse
            if (root == null)
                ((JPopupMenu)menu).add(item);
            else
                ((JMenu)menu).add(item);
            createSketchItems(item, sk, prefix + sk.getName() + "/");
        }
    }

    /**
     * IF method.
     */
    public void inform(IExport obj) {
        if (obj instanceof Sketch) {
            Sketch sk = (Sketch) obj;
            if (sk.isValid()) sketch.add(sk); else sketch.remove(sk);
        }
    }

    /**
     * Used for menu cascading, because getParent does not work correctly.
     */
    class MyMenuItem extends JMenuItem {
        String heritage;
        MyMenuItem(String aHeritage, String txt) {
            super(txt);
            heritage = aHeritage;
        }
    }

    /**
     * Used for menu cascading, because getParent does not work correctly.
     */
    class MyMenu extends JMenu {
        String heritage;
        MyMenu(String aHeritage, String txt) {
            super(txt);
            heritage = aHeritage;
        }
    }
}
