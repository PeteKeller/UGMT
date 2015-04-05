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
package harn.chars;

import java.util.*;
import org.w3c.dom.*;

/**
 * Class to hold conditional table references.
 * @author Michael Jung
 */
class CondTable {
    /** main reference */
    private Main main;

    /** Conditions map to table */
    private Hashtable cond2table;

    /**
     * Constructor.
     * @param aMain main reference
     */
    public CondTable(Main aMain) { main = aMain; cond2table = new Hashtable(); }

    /**
     * Add a row to this table.
     * @param node node to parse
     * @param attr attributes already determined
     */
    public void addRow(Node node, NamedNodeMap attr, Set results) {
        RandTable table = new RandTable(main);

        String test = attr.getNamedItem("test").getNodeValue();
        NodeList tree = node.getChildNodes();
        for (int i = 0; i < tree.getLength(); i++) {
            Node child = tree.item(i);
            if (child.getNodeName().equals("table"))
                table.addRow(child, child.getAttributes(), results);
        }

        cond2table.put(test, table);
    }

    /**
     * Get the table result for the random numbers supplied. Consider that the
     * predetermined number was adjusted.
     * @param r prerolled/predetermined number
     * @param n number of dice
     * @param d number of dice-sides
     * @return result value
     */
    public String get(int r, int n, int d) {
        Iterator iter = cond2table.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            if (CharAttribute.evalCond(null, key, main.getState())) {
                RandTable tab = (RandTable) cond2table.get(key);
                return tab.get(r, n, d);
            }
        }
        return null;
    }
}
