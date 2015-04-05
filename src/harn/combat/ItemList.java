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

import harn.repository.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.tree.*;
import rpg.*;

/**
 * The main class for the skills plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class ItemList extends JPanel {
    /** Framework reference */
    private Main main;

    /** Structure panels */
    private JPanel[] structure;

    /** Maximum fields allowed/used */
    private static int MAX_STRUCT = 8;

    /** List of all items */
    protected HashSet all;

    /** Constraints */
    private GridBagConstraints constr;

    /** Constructor */
    public ItemList(Main aMain) {
        main = aMain;
        all = new HashSet();
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        constr = new GridBagConstraints();
        constr.gridx = constr.gridy = 0;
        constr.fill = GridBagConstraints.BOTH;
        constr.weightx = 1;
        constr.weighty = 1;

        // Armour usage / Weapon usage
        structure = new JPanel[MAX_STRUCT];

        constr.weightx = 0;
        for (int i = 0; i < MAX_STRUCT; i++) {
            structure[i] = new JPanel(new GridBagLayout());
            structure[i].setBackground(Color.WHITE);
            constr.gridx = i;
            if (i > 0) constr.weightx = 1;
            add(structure[i], constr);
        }
    }

    /** common to all lines */
    public void addItem(Line item) {
        all.add(item);
        constr.gridx = 0;
        structure[0].add(item.usedF(), constr);

        JComponent[] fields = item.getFields();
        for (int i = 0; fields != null && i < fields.length; i++)
            structure[i+1].add(fields[i], constr);

        constr.gridy++;
        revalidate();
        repaint();
    }

    /** Remove an item */
    public void removeItem(Line item) {
        all.remove(item);
        JComponent[] fields = item.getFields();
        structure[0].remove(item.usedF());

        for (int i = 0; fields != null && i < fields.length; i++)      
            structure[i+1].remove(fields[i]);

        //Can't do this: constr.gridy--;
        revalidate();
        repaint();
    }
    /** Clear all */
    public void clearAll() {
        all.clear();
        for (int i = 0; i < MAX_STRUCT; i++)
            structure[i].removeAll();
        constr.gridy = 0;
    }
}
