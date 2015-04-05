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
import rpg.*;
import javax.swing.*;
import java.awt.*;
import org.w3c.dom.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

/**
 * Class for a wound line on the GUI and representing a wound.
 */
class WoundLine extends Line {
    /** Fields */
    protected JComponent[] fields;

    /** Name */
    protected String name;

    /** Constructor */
    WoundLine(Main aMain, String aName, String[] values, Class[] types, String[][] choices, String[] helps, boolean aUsed) {
        super(aMain, aUsed);
        name = aName;
        fields = new JComponent[types.length + 1];
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(JTextField.class)) {
                fields[i] = new JTextField();
                if (i < values.length && values[i] != null)
                    ((JTextField)fields[i]).setText(values[i]);
            }
            if (types[i].equals(JComboBox.class)) {
                fields[i] = new JComboBox(choices[i]);
                if (i < values.length && values[i] != null)
                    ((JComboBox)fields[i]).setSelectedItem(values[i]);
            }
            fields[i].setToolTipText(helps[i]);
        }
        JButton b = new JButton("Roll");
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    roll();
                }
            });
        b.setToolTipText("Advance wound one time step");
        fields[types.length] = b;
    }

    /** IF from Line */
    public JComponent[] getFields() { return fields; }

    /**
     * Roll this wound
     * @return a change was made/was possible
     */
    public boolean roll() {
        Char ch = main.getState().getChar(name);
        if (ch == null || !used()) return false;

        ArrayList rules = main.getData().rules;
        NamedNodeMap crule = null;

        // Get last rule to be match condition
        for (int i = 0; i < rules.size(); i++) {
            // cond period test ms, cs, mf, cf, condi, resulti
            NamedNodeMap att = (NamedNodeMap)rules.get(i);
            String cond = att.getNamedItem("cond").getNodeValue();
            if (evalCond(ch, cond, this)) crule = att;
        }
        if (crule == null || main.getState().cal == null) return false;

        // Check/Set date
        long lastdate = main.getState().cal.string2datetime
            (getValue(ch, "Rolled"));
        long period = Long.parseLong
            (crule.getNamedItem("period").getNodeValue());
        if (main.map.act == null || main.map.act.getDate() + period < lastdate)
            return false;
        setValue
            ("Rolled", main.getState().cal.datetime2string(lastdate + period));

        //TODO FIX THIS TO USE parseFormula
        String[] r = crule.getNamedItem("roll").getNodeValue().split("d");
        int n = Integer.parseInt(r[0]);
        int d = Integer.parseInt(r[1]);
        int roll = DiceRoller.d(n, d);

        // Check test
        int test = Integer.parseInt
            (evalTerm(ch, crule.getNamedItem("test").getNodeValue(), this));
        String attr;
        if (roll > test)
            attr = (roll%5 == 0 ? "cf" : "mf");
        else
            attr = (roll%5 == 0 ? "cs" : "ms");
        if (crule.getNamedItem(attr) == null) return true;

        // Calculate result
        String[] result = crule.getNamedItem(attr).getNodeValue().split("=");
        setValue(result[0], evalTerm(ch, result[1], this));

        // Check end of wound (infection)
        if (crule.getNamedItem("exit") == null)
            return true;
        String cond = crule.getNamedItem("exit").getNodeValue();
        if (evalCond(ch, cond, this)) {
            String eresult[] = crule.getNamedItem("result").getNodeValue().split("[|]");
            for (int i = 0; i < eresult.length; i++) {
                result = eresult[i].split("=");
                setValue
                    (result[0], evalTerm(ch, result[1], this));
            }
        }
        return true;
    }

    /** Return the field/char value */
    private void setValue(String name, String value) {
        String[] names = main.getData().getWoundFields();
        for (int i = 0; i < names.length; i++)
            if (names[i].equals(name)) {
                if (fields[i] instanceof JTextField)
                    ((JTextField)fields[i]).setText(value);
                if (fields[i] instanceof JComboBox)
                    ((JComboBox)fields[i]).setSelectedItem(value);
                return;
            }
    }

    /** Return the field/char value */
    protected String getValue(Char ch, String name) {
        // Fields
        String[] names = main.getData().getWoundFields();
        for (int i = 0; i < names.length; i++)
            if (names[i].equals(name)) {
                if (fields[i] instanceof JTextField)
                    return ((JTextField)fields[i]).getText();
                if (fields[i] instanceof JComboBox)
                    return (String)((JComboBox)fields[i]).getSelectedItem();
            }

        // Attributes
        if (ch == null) return null;
        String[] list = ch.getAttributeList();
        for (int i = 0; i < list.length; i++)
            if (list[i].equals(name))
                return ch.getAttribute(name);

        return null;
    }

    /*
     * Static way to test a condition. Parses formulas of types: "a<b", "a>b",
     * "a=b", and "a!b", which can be "and"ed through "|". Numerical
     * comparison is obvious. String equality means right is contained in
     * left. "<" for strings means left is contained in the comma-separated
     * right list. "a" is taken to be a field, "b" a value.
     * @param ch character to take attributes from
     * @param test condition to test
     * @param wl reference to get values from
     */
    public static boolean evalCond(Char ch, String test, WoundLine wl) {
        if (test == null) return true;

        String[] cond = test.split("[|]");
        boolean met = true;
        for (int i = 0; (i < cond.length) && met; i++) {
            String[] sub = cond[i].split("[=<!>]");

            // Parsing of left side
            String lval = wl.getValue(ch, sub[0]);

            // Normalize emptieness
            if (lval == null || lval.length() == 0) {
                lval = "0";
            }

            // Parsing of right side
            String rval = sub[1];

            try {
                // Numeric "=", "<", ">", and "!"
                if (cond[i].indexOf("=") > -1)
                    met &= (Integer.parseInt(lval) == Integer.parseInt(rval));

                else if (cond[i].indexOf("<") > -1)
                    met &= (Integer.parseInt(lval) < Integer.parseInt(rval));

                else if (cond[i].indexOf(">") > -1)
                    met &= (Integer.parseInt(lval) > Integer.parseInt(rval));

                else if (cond[i].indexOf("!") > -1)
                    met &= (Integer.parseInt(lval) != Integer.parseInt(rval));
            }
            catch (Exception e) {
                // String "=", "<", and "!"
                if (cond[i].indexOf("=") > -1)
                    met &= (lval.indexOf(rval) == 0);

                else if (cond[i].indexOf("!") > -1)
                    met &= (lval.indexOf(rval) != 0);

                else if (cond[i].indexOf("<") > -1) {
                    String[] all = rval.split(",");
                    boolean lmet = false;
                    for (int j = 0; (j < all.length) && !lmet; j++)
                        lmet |= (lval.indexOf(all[j]) == 0);
                    met &= lmet;
                }
            }
        }
        return met;
    }

    /**
     * Static. Evaluates terms of the form "a-b", "a+b", "a*b" and
     * "a", where a, and b may be numbers or attributes/fields.
     * @param term term to evaluate
     * @param wl place to get values from
     */
    static private String evalTerm(Char ch, String term, WoundLine wl) {
        int iplus = term.indexOf('+');
        int iminus = term.indexOf('-');
        int ix = term.indexOf('*');
        int imax = Math.max(iplus,Math.max(iminus,ix));

        if (term.equals("*")) return term;

        int rres = 0;
        int lres = 0;

        // Use first term as start
        if (imax > 0) {
            String var = term.substring(0, imax);
            String val = wl.getValue(ch, var);
            if (val != null)
                lres = Integer.parseInt(val);
            else
                lres = Integer.parseInt(var);

            var = term.substring(imax + 1);
            val = wl.getValue(ch, var);
            if (val != null)
                rres = Integer.parseInt(val);
            else
                rres = Integer.parseInt(var);
        }
        else {
            String val = wl.getValue(ch, term);
            if (val != null) return val;
            return term;
        }

        if (imax == ix)
            return Integer.toString(lres * rres);
        if (imax == iplus)
            return Integer.toString(lres + rres);
        return Integer.toString(lres - rres);
    }
}
