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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.tree.*;
import rpg.DiceRoller;
import rpg.Framework;

/**
 * Spell class, represents data and Gui
 * @author Michael Jung
 */
class Spell extends DefaultMutableTreeNode implements rpg.TreeTable.ButtonNode { 
    /** Main reference */
    private Main main;

    /** Adj-1 */
    String adj1;

    /** Adj-2 */
    String adj2;

    /** Adj-3 */
    String adj3;

    /** Result */
    String result;

    /** Result */
    String success;

    /** current random number */
    int rand = 0;

    /** Father */
    private Holder father;

    /** Ref to the exported object */
    MySpell mySpell;

    /** Init constructor */
    Spell(Holder aFather, String name, Main aMain) {
        this(aFather, name, "5", "0", "0", "0", aMain);
        if (aFather.mychar == null) return;

        String type =
            (String) main.data.spells.get(name.substring(0, name.indexOf("(")));
        if (type == null) return;
        String stat[] = type.split(":");
        String newml = "5";
        String[] allstats = stat[1].split("[|]");

        for (int i = 0; i < allstats.length; i++) {
            String tmp = "0";
            if (stat[0].equals("skill"))
                tmp = aFather.mychar.getSkill(allstats[i]);
            if (stat[0].equals("attr"))
                tmp = aFather.mychar.getAttribute(allstats[i]);
            if (stat[0].equals("oml"))
                tmp = stat[1];

            if (tmp != null &&
                Integer.parseInt(tmp) > Integer.parseInt(newml))
                newml = tmp;
        }
        mySpell.ml = newml;
    }    
    
    /** Load constructor */
    Spell(Holder aFather, String name, String mls, String adj1s, String adj2s, String adj3s, Main aMain) {
        super(name);
        main = aMain;
        mySpell = new MySpell(mls, adj1s, adj2s, adj3s);
        father = aFather;

        if (father != null && father.mychar != null)
            main.announce(this);
    }

    public void update(int row) {
        rand = DiceRoller.d(1,100);
        calc(row);
    }

    /** Get ML */
    public String getML() { return mySpell.ml; }

    /** Get exportable */
    public harn.repository.Spell getExport() { return mySpell; }

    /** Get name */
    public String name() { return getUserObject().toString(); }

    /**
     * Recalculates the line. Randomness is supplied elsewhere.
     */
    public void calc(int row) {
        String newsuccess;
        String newresult;
        try {
            if (rand == 0) return;

            // Apply adjustments to ML
            int mli = Integer.parseInt(mySpell.ml);
            mli = adjust(mli, adj1);
            mli = adjust(mli, adj2);
            mli = adjust(mli, adj3);

            if (mli < 5) mli = 5;
            if (mli > 95) mli = 95;

            // Set roll
            newresult = Integer.toString(mli) + "/" + Integer.toString(rand);

            // Set success level
            String suc = (rand <= mli ? "S" : "F");
            if (rand%5 == 0) suc = "C" + suc;
            else suc = "M" + suc;
            newsuccess = suc;
        }
        catch (Throwable t) {
            // Don't debug faulty entries, nil result
            newsuccess = "-";
            newresult = "-";
        }
        if (!newresult.equals(result)) main.modsModel.setValueAt(newresult, row, 5);
        if (!newsuccess.equals(success)) main.modsModel.setValueAt(newsuccess, row, 6);
    }

    /**
     * Parse and apply adjustments
     * @param val current value
     * @param adj adjustment to make
     * @return value after application
     */
    static private int adjust(int val, String adj) {
        if (adj.length() == 0) return val;
        if (adj.charAt(0) == '+')
            return val + Integer.parseInt(adj.substring(1));
        if (adj.charAt(0) == '-')
            return val - Integer.parseInt(adj.substring(1));
        if (adj.charAt(0) == '*')
            return (int)(val * Float.parseFloat(adj.substring(1)));
        if (adj.charAt(0) == '/')
            return (int)(val / Float.parseFloat(adj.substring(1)));
        return val + Integer.parseInt(adj);
    }

    /**
     * Class used for export.
     */
    class MySpell extends harn.repository.Spell {
        /** ML */
        String ml;
        /** Constructor */
        MySpell(String aMl, String anAdj1, String anAdj2, String anAdj3) {
            ml = aMl; adj1 = anAdj1; adj2 = anAdj2; adj3 = anAdj3;
        }
        /**
         * Implement as specified in superclass.
         * @return name attribute
         */
        public String getName() { return name(); }
        /**
         * Get the owning character. Never null.
         * @return owner
         */
        public Char getOwner() { return father.mychar; }
        /**
         * Get ML.
         * @return ML
         */
        public int getML() { return Integer.parseInt(ml); }
        /**
         * Whether this spell is still valid.
         * @return validity
         */
        public boolean isValid() {
            return father != null && father.mychar != null;
        }
    }
}
