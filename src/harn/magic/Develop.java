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
package harn.magic;

import harn.repository.Char;
import java.util.*;
import rpg.SortedTreeModel;

/**
 * Development is handled in this class.
 * @author Michael Jung
 */
public class Develop {
    /** Main reference */
    private Main main;

    /** State */
    protected String developing;

    /** List of (base) spells that costs */
    Hashtable costs;

    /** Current option points */
    int opt;

    /** Constructor */
    public Develop(Main aMain) {
        main = aMain;
        costs = new Hashtable();
    }

    /** Start to develop */
    public void start(String name) {
        developing = name;
        Holder holder = (Holder) main.state.holderList.get(name);
        if (holder == null || holder.mychar == null) return;
        costs = new Hashtable();

        opt = 0;
        // Skills
        Iterator iter = main.data.si.keySet().iterator();
        while (iter.hasNext()) {
            String group = (String) iter.next();
            String[] skills = ((String)main.data.si.get(group)).split("[|]");
            for (int i = 0; i < skills.length; i++) {
                String skill = holder.mychar.getSkill(skills[i]);
                if (skill == null) continue;
                int si = Integer.parseInt(skill)/10;
                if (si > opt) opt = si;
            }
            // Check all tests
            Hashtable ht = (Hashtable) main.data.cond.get(group);
            if (ht == null) { opt = 0; continue; }
        }
        
        // Attributes
        iter = main.data.attr.keySet().iterator();
        while (iter.hasNext()) {
            String group = (String) iter.next();
            String[] attrs = ((String)main.data.attr.get(group)).split("[|]");
            for (int i = 0; i < attrs.length; i++) {
                String attr = holder.mychar.getAttribute(attrs[i]);
                if (attr == null) continue;
                int at = Integer.parseInt(attr);
                if (at > opt) opt = at;
            }
        }

        main.bOptions.setText("CHOICE (" + opt + ")");

        // All groups
        iter = main.data.cond.keySet().iterator();
        while (iter.hasNext()) {
            String group = (String) iter.next();
            Iterator iter2 = main.data.full.iterator();
            while (iter2.hasNext()) {
                String spell = (String) iter2.next();
                if (spell.indexOf(group) == 0) {
                    Hashtable tests = (Hashtable) main.data.cond.get(group);
                    Iterator iter3 = tests.keySet().iterator();
                    while (iter3.hasNext()) {
                        String test = (String) iter3.next();
                        boolean ok = evalCond(spell, test, holder.mychar);
                        if (ok) {
                            String sh[] = spell.split("/");
                            String str = (String)tests.get(test);
                            int cost = Integer.parseInt(str);
                            String pre = (String) costs.get(sh[sh.length-1]);
                            if (pre == null || Integer.parseInt(pre.substring(pre.indexOf("/")+1)) > cost)
                                costs.put
                                    (sh[sh.length-1], sh[sh.length-2] + "/" + cost);
                        }
                    }
                }
            }
        }

        // Do freebies and too costly
        iter = costs.keySet().iterator();
        HashSet rem = new HashSet();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String val = (String) costs.get(key);
            int cost = Integer.parseInt(val.substring(val.indexOf("/")+1));
            if (cost == 0) {
                Spell sp =
                    new Spell(holder, key + "(" + val.substring(0, val.indexOf("/")) + ")", main);
                main.getModel().insertNodeSorted(sp, holder);

            }
            if (cost == 0 || cost > opt) rem.add(key);
        }
        costs.keySet().removeAll(rem);
        main.tree.revalidate();
        main.tree.repaint();
    }
    
    /** Stop to develop */
    public void stop() {
        opt = 0;
        developing = null;
        costs = new Hashtable();
        main.bOptions.setText("-");
        main.tree.revalidate();
        main.tree.repaint();
    }

    /**
     * Remove spells too expensive.
     */
    protected void proceed(String name) {
        // Reduce options
        String val = (String) costs.get(name.substring(0, name.indexOf("(")));
        int cost = Integer.parseInt(val.substring(val.indexOf("/")+1));

        opt -= cost;
        if (opt == 0) {
            stop();
            return;
        }

        main.bOptions.setText("CHOICE (" + opt + ")");

        // Weed options too expensive
        Iterator iter = costs.keySet().iterator();
        HashSet rem = new HashSet();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String val1 = (String) costs.get(key);
            cost = Integer.parseInt(val1.substring(val1.indexOf("/")+1));
            if (cost > opt) rem.add(key);
        }
        costs.keySet().removeAll(rem);
        main.tree.revalidate();
        main.tree.repaint();
    }

    /**
     * Static way to test a condition. Parses formulas of types: "a<b", "a>b",
     * "a=b", and "a!b", which can be "and"ed through "|". Numerical
     * comparison is obvious. String equality means right is contained in
     * left. "<" for strings means left is contained in the comma-separated
     * right list. "a" is taken to be an attribute, "b" a value. A non
     * condition means the current spell to test. (Test from end.)
     * @param var current skill being tested (if being tested)
     * @param test condition to test
     * @param state reference to get atttributes/skill from
     */
    private static boolean evalCond(String spell, String test, Char mychar) {
        if (test == null) return true;

        String[] cond = test.split("[|]");
        boolean met = true;
        for (int i = 0; (i < cond.length) && met; i++) {
            String[] sub = cond[i].split("[=<!>]");

            // Parsing of left side
            String lval;
            if (!sub[0].equals("$value"))
                lval = mychar.getAttribute(sub[0]);
            else
                lval = spell;

            // Normalize emptieness
            if (lval == null || lval.length() == 0) {
                lval = "0";
            }

            try {
                // Numeric "=", "<", ">", and "!"
                if (cond[i].indexOf("=") > -1)
                    met &= (Integer.parseInt(lval) == Integer.parseInt(sub[1]));

                else if (cond[i].indexOf("<") > -1)
                    met &= (Integer.parseInt(lval) < Integer.parseInt(sub[1]));

                else if (cond[i].indexOf(">") > -1)
                    met &= (Integer.parseInt(lval) > Integer.parseInt(sub[1]));

                else if (cond[i].indexOf("!") > -1)
                    met &= (Integer.parseInt(lval) != Integer.parseInt(sub[1]));
            }
            catch (Exception e) {
                // String "=", "<", and "!"
                if (cond[i].indexOf("=") > -1) {
                    met &= (lval.indexOf(sub[1]) == 0);
                }
                else if (cond[i].indexOf("!") > -1)
                    met &= (lval.indexOf(sub[1]) != 0);

                else if (cond[i].indexOf("<") > -1) {
                    String[] all = sub[1].split(",");
                    boolean lmet = false;
                    for (int j = 0; (j < all.length) && !lmet; j++)
                        lmet |= (lval.indexOf(all[j]) == 0);
                    met &= lmet;
                }
            }
        }
        return met;
    }
}
