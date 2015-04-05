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

import rpg.*;
import org.w3c.dom.*;
import java.util.*;

/**
 * Class to hold random tables.
 * @author Michael Jung
 */
class RandTable {
    /** Main reference */
    private Main main;

    /** Contains "<=" numbers (int) */
    private ArrayList rand;

    /** Contains result String or RandTable */
    private ArrayList results;

    /** Adjustments */
    private Hashtable cond2adj;

    /** Corollary attributes */
    private HashSet attributes;
    
    /**
     * Constructor.
     */
    public RandTable(Main aMain) {
        rand = new ArrayList();
        results = new ArrayList();
        main = aMain;
        cond2adj = new Hashtable();
    }

    /**
     * Add a row to table.
     * @param node node to parse
     * @param attr attributes already determined
     * @param result list that contains all possible results
     */
    public void addRow(Node node, NamedNodeMap attr, Set extresults) {
        // Append element
        String res = attr.getNamedItem("result").getNodeValue();
        String val = attr.getNamedItem("value").getNodeValue();
        rand.add(new Integer(Integer.parseInt(val)));

        // Subtable ?
        if (res.equals("table")) {
            // Get table
            RandTable subtable = null;
            NodeList tree = node.getChildNodes();
            for (int i = 0; i < tree.getLength(); i++) {
                Node child = tree.item(i);
                if (child.getNodeName().equals("table")) {
                    if (subtable == null) subtable = new RandTable(main);
                    subtable.addRow(child, child.getAttributes(), extresults);
                }
                if (child.getNodeName().equals("adjust")) {
                    Integer vals = new Integer
                        (child.getAttributes().getNamedItem("value").getNodeValue());
                    String test = child.getAttributes().getNamedItem("test").getNodeValue();
                    cond2adj.put(test, vals);
                }
            }
            results.add
                (new Object[] {
                    attr.getNamedItem("roll").getNodeValue(), subtable
                });
        }
        else {
            // Get attributes
            NodeList tree = node.getChildNodes();
            for (int i = 0; i < tree.getLength(); i++) {
                Node child = tree.item(i);
                NamedNodeMap cattr = child.getAttributes();
                if (child.getNodeName().equals("attribute")) {
                    if (attributes == null) attributes = new HashSet();
                    attributes.add(new DevAttribute(main, child, cattr));
                }
            }
            results.add(res);
            if (extresults != null && !res.matches("[0-9]*"))
                extresults.add(res);
        }
    }

    /**
     * Get the result for the random number supplied. Reroll means reroll
     * twice. Results are concatenated with "," as seperator.
     * @param r random number
     * @param n number of dice (for reroll)
     * @param m number of dice-sides (for reroll)
     * @return resulting value
     */
    public String get(int r, int n, int m) {
        String result = null;

        int i = 0;

        int[] lrand = new int[rand.size()];
        for (i = 0; i < rand.size(); i++)
            lrand[i] = ((Integer)rand.get(i)).intValue();

        i = 0;
        while (i < lrand.length - 1 && r > lrand[i]) i++;

        // Immediate result
        if (results.get(i) instanceof String) {
            String res = (String) results.get(i);
            if (res.equals("reroll")) {
                String res1 = get(DiceRoller.d(n,m),n,m);
                String res2 = get(DiceRoller.d(n,m),n,m);
                if (res1.equals("None")) return res2;
                if (res2.equals("None")) return res1;
                return res1 + "," + res2;
            }
            result = res;
        }
        else { 
            Object[] o = (Object[]) results.get(i);
            String dice[] = ((String)o[0]).split("d");
            int nn = Integer.parseInt(dice[0]);
            int dd = Integer.parseInt(dice[1]);
            int roll = DiceRoller.d(nn, dd);

            // Adjustements
            Iterator iter = cond2adj.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                if (CharAttribute.evalCond(null, key, main.getState())) {
                    roll += ((Integer) cond2adj.get(key)).intValue();
                }
            }

            result = ((RandTable) o[1]).get(roll, nn, dd);
        }
        if (attributes == null) return result;

        // Do the corollary attributes
        Iterator iter = attributes.iterator();
        while (iter.hasNext()) {
            DevAttribute attr = (DevAttribute)iter.next();
            attr.adjust();
        }
        return result;
    }
}
