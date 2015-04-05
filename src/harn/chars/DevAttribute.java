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
import java.util.*;
import javax.xml.parsers.*;
import java.io.File;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.w3c.dom.*;

/**
 * Class to hold attribute adjustments during development. Generally needs a
 * function/arg pair or set to be meaningful. Changes will be modifying,
 * seldom setting.
 * @author Michael Jung
 */
class DevAttribute {
    /** Main reference */
    private Main main;

    /** Adjustments */
    private Hashtable cond2adj;

    /** Name of attribute */
    private String name;

    /** function */
    private String function;

    /** argument */
    private String arg;

    /** set */
    private String set;

    /** dice */
    private String dice;

    /** table */
    private RandTable table;

    /**
     * Constructor.
     * @param node node with children
     * @param attr attributes already determined
     */
    DevAttribute(Main aMain, Node node, NamedNodeMap attr) {
        main = aMain;
        cond2adj = new Hashtable();

        // Get name
        name = attr.getNamedItem("name").getNodeValue();

        // Get attribute characteristic
        Node a;
        a = attr.getNamedItem("function");
        if (a != null) function = a.getNodeValue();

        a = attr.getNamedItem("arg");
        if (a != null) arg = a.getNodeValue();

        a = attr.getNamedItem("set");
        if (a != null) set = a.getNodeValue();

        a = attr.getNamedItem("roll");
        if (a != null) dice = a.getNodeValue();

        // All possible outcomes; not used in this context
        TreeSet results = new TreeSet();

        // Get table & adjustments
        NodeList tree = node.getChildNodes();
        for (int i = 0; i < tree.getLength(); i++) {
            Node child = tree.item(i);
            NamedNodeMap cattr = child.getAttributes();
            if (child.getNodeName().equals("adjust")) {
                String val = cattr.getNamedItem("value").getNodeValue();
                String test = cattr.getNamedItem("test").getNodeValue();
                cond2adj.put(test, val);
            }
            if (child.getNodeName().equals("table")) {
                if (table == null) table = new RandTable(main);
                ((RandTable)table).addRow(child, cattr, results);
            }
        }
    }

    /**
     * Adjust. Called when an attribute needs to be adjusted. Without a
     * function/arg pair, this is meaningless.
     */
    void adjust() {
        State state = main.getState();

        // Test and compute numerical adjustment
        int num = 0;
        Iterator iter = cond2adj.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (CharAttribute.evalCond(null, key, state)) {
                String val = (String) cond2adj.get(key);
                try {
                    num += Integer.parseInt(val);
                }
                catch (Exception e) {
                    // Parsing of right side
                    if (val.indexOf("-") >= 1) {
                        String tmp[] = val.split("-");
                        val = Integer.toString
                            (Integer.parseInt(state.getAttribute(tmp[0])) -
                             Integer.parseInt(tmp[1]));
                    }
                    else if (val.indexOf("+") >= 1) {
                        String tmp[] = val.split("\\+");
                        val = Integer.toString
                            (Integer.parseInt(state.getAttribute(tmp[0])) +
                             Integer.parseInt(tmp[1]));
                    }
                    else {
                        val = state.getAttribute(val);
                    }
                    num = Integer.parseInt(val);
                }
            }
        }

        // Function needs argument arg-attribute
        String adj = null;
        if (arg != null) {
            adj = state.getAttribute(arg);
        }

        // The argument is numerically adjusted or the adjustment is taken
        // as the numerical argument itself, in case arg-attribute is not
        // used.
        if (num != 0) {
            if (adj != null)
                adj = Integer.toString(Integer.parseInt(adj) + num);
            else
                adj = Integer.toString(num);
        }

        // Get the value to be adjusted and adjustable (arg)
        String myVal = state.getAttribute(name);
        String myArg = myVal;

        // Evaluate a function, if given. The arguments are the value to
        // be adjusted and the adjustment. The result is the final value.
        if (function != null && adj != null && myArg != null) {
            myArg = CharAttribute.evalFunc
                (function, adj, myArg, state.getCal(),
                 state.getAct(), state.getCurrent());
        }
        if (set != null) {
            myArg = state.getAttribute(set);
            if (myArg == null) myArg = "0";
            if (adj != null)
                myArg = Integer.toString
                    (Integer.parseInt(myArg) + Integer.parseInt(adj));
        }
        if (dice != null) {
            //TODO: Fix this to use parse value
            String[] d = dice.split("d");
            int r = DiceRoller.d
                (Integer.parseInt(d[0]),Integer.parseInt(d[1]));
            r += num;
            // Result from table
            if (table != null) {
                String tres = ((RandTable)table).get
                    (r, Integer.parseInt(d[0]), Integer.parseInt(d[1]));
                try {
                    myArg = Integer.toString
                        (Integer.parseInt(myArg) + Integer.parseInt(tres));
                }
                catch (Exception e) {
                    if (tres != null && tres.length() != 0 &&
                        myArg.indexOf(tres) < 0)
                        myArg += "," + tres;
                }
            }
            else
                myArg = Integer.toString(r);
        }

        // Set the adjusted attribute. 
        if (myArg != null && !myArg.equals(myVal))
            main.getData().setAttribute(name, myArg);
    }
}
