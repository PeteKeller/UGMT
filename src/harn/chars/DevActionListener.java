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
 * Action listener class for development buttons. This class exists for each
 * development button.  Every skill reachable has a test condition associated
 * with it.  The options are also listed and are worked out after "Fate"
 * development has taken place. Certain skills are determined during the
 * development phase (e.g. Psionics), which require table lookup. The last
 * pahse for development consists in a post-development step for attributes.
 * @author Michael Jung
 */
public class DevActionListener implements ActionListener {
    /** Main reference */
    private Main main;

    /** map skill to OptionSetting */
    private HashSet skills;

    /** development options */
    private ArrayList options;

    /** attribute adjustements */
    private ArrayList attradj;

    /** button always active */
    private boolean active;

    /** Repeating */
    private MyRepeat repeat;

    // Table section
    
    /** table to determine skills */
    private RandTable table;

    /** count of skills to determine */
    private String count;

    /** roll which dice to determine skills */
    private String dice;

    /** OML for determined skills */
    private String oml;

    /**
     * Constructor.
     * @param aMain main reference
     * @param node node to parse
     * @param anActive button is always active
     */
    public DevActionListener(Main aMain, Node node, boolean anActive) {
        main = aMain;
        skills = new HashSet();
        options = new ArrayList();
        active = anActive;

        // Previous option; options are chained.
        DevOption prev = null;

        NodeList tree = node.getChildNodes();
        for (int i = 0; i < tree.getLength(); i++) {
            Node child = tree.item(i);
            NamedNodeMap cattr = child.getAttributes();
            if (child.getNodeName().equals("option")) {
                prev = new DevOption(prev, main, child, cattr);
                options.add(prev);
            }
            if (child.getNodeName().equals("attribute")) {
                if (attradj == null) attradj = new ArrayList();
                attradj.add(new DevAttribute(main, child, cattr));
            }
            if (child.getNodeName().equals("repeat")) {
                repeat = new MyRepeat(child, cattr);
            }
            if (child.getNodeName().equals("skill")) {
                // Name
                Node nnode = cattr.getNamedItem("name");
                if (nnode != null) {
                    String name = nnode.getNodeValue();

                    // default OML
                    String oml = null;
                    if (cattr.getNamedItem("oml") != null)
                        oml = cattr.getNamedItem("oml").getNodeValue();
                        
                    // All tests
                    NodeList tests = child.getChildNodes();
                    for (int j = 0; j < tests.getLength(); j++) {
                        Node subchild = tests.item(j);
                        NamedNodeMap scattr = subchild.getAttributes();
                        if (scattr != null) {
                            String test = scattr.getNamedItem("test").getNodeValue();
                            String myoml = oml;
                            if (scattr.getNamedItem("oml") != null)
                                myoml = scattr.getNamedItem("oml").getNodeValue();
                            skills.add(new OptionSetting(name, test, myoml));
                        }
                    }
                    if (tests.getLength() == 0)
                        skills.add(new OptionSetting(name, null, oml));
                }
                else { // "name" not present
                    count = cattr.getNamedItem("count").getNodeValue();
                    dice = cattr.getNamedItem("roll").getNodeValue();
                    oml = cattr.getNamedItem("oml").getNodeValue();

                    // Table
                    NodeList rows = child.getChildNodes();
                    for (int j = 0; j < rows.getLength(); j++) {
                        Node sub = rows.item(j);
                        if (sub.getNodeName().equals("table")) {
                            if (table == null) table = new RandTable(main);
                            table.addRow(sub, sub.getAttributes(), null);
                        }
                    }
                }
            }
        }
    }

    /**
     * ActionListener method. When activated, all "Fate" skills are
     * determined. Options are then started. (The fatal actions are presented
     * through the previous activated button or the initial button.) As it
     * stands, table skills are alway automatic and do not give a choice
     * afterwards.
     * @param e event
     */
    public void actionPerformed(ActionEvent e) {
        if (active) {
            actionPerformed();
            adjust();
        }
        else {
            // Set the active development phase
            main.getState().setCurrentDev(this);
            main.disableThisButton((JToggleButton)e.getSource());

            boolean proceed = actionPerformed();

            // Proceed (devlopment section internal)
            if (!proceed)
                main.enableNextButton((JToggleButton)e.getSource());
        }
    }

    /**
     * The proper action for development.
     * @param e event
     * @return proceed (internal to development section)
     */
    public boolean actionPerformed() {
        if (repeat != null) repeat.repeat();

        if (table == null) {
            // Non-choice skills
            Iterator iter = skills.iterator();
            while (iter.hasNext()) {
                OptionSetting tv = (OptionSetting) iter.next();
                if (CharAttribute.evalCond(tv.name, tv.test, main.getState()))
                    if (tv.name != null) main.getData().setPML(tv.name, tv.oml);
            }

            // Option choice setup
            if (options.size() > 0) {
                DevOption dev = (DevOption) options.get(0);
                boolean on = dev.enable();
                if (on) return true; // else proceed
            }
        }
        else {
            // Tables are non-choice only
            //TODO: FIX THIS TO USE PARSE FORMULA
            int nn = Integer.parseInt(main.getState().getAttribute(count));
            String[] r = dice.split("d");
            int n = Integer.parseInt(r[0]);
            int d = Integer.parseInt(r[1]);
            for (int i = 0; i < nn; i++) {
                int roll = DiceRoller.d(n, d);
                String res = table.get(roll, n, d);
                String all[] = res.split(",");
                for (int j = 0; j < all.length; j++) {
                    main.getData().setPML(all[j], oml);
                }
            }
        }
        return false; // Don't proceed
    }

    /**
     * Rest the development (option) sections.
     */
    public void resetDevelopment() {
        for (int i = 0; i < options.size(); i++)
            ((DevOption)options.get(i)).reset();
    }

    /**
     * Finish attribute adjustments
     */
    public void adjust() {
        // Finish attribute adjustments
        for (int i = 0; (attradj != null) && i < attradj.size(); i++) {
            ((DevAttribute)attradj.get(i)).adjust();
        }
    }

    /**
     * Class to hold repeater. Needs a function/arg pair.
     */
    class MyRepeat {
        /** Attributes affected */
        private HashSet attributes;
        /** function */
        private String function;
        /** argument */
        private String arg;
        /** attribute adjustements */
        private ArrayList attradj;
        /**
         * Constructor.
         * @param node node with children
         * @param attr attributes already determined
         */
        MyRepeat(Node node, NamedNodeMap attr) {
            attributes = new HashSet();
            function = attr.getNamedItem("function").getNodeValue();
            arg = attr.getNamedItem("arg").getNodeValue();
            // Get attributes
            NodeList tree = node.getChildNodes();
            for (int i = 0; i < tree.getLength(); i++) {
                Node child = tree.item(i);
                NamedNodeMap cattr = child.getAttributes();
                if (child.getNodeName().equals("attribute")) {
                    if (attradj == null) attradj = new ArrayList();
                    attradj.add(new DevAttribute(main, child, cattr));
                }
            }
        }
        /**
         * Repeat. Called when button is pressed
         */
        void repeat() {
            State state = main.getState();
            String myArg = state.getAttribute(arg);
            int num = Integer.parseInt(CharAttribute.evalFunc
                (function, myArg, null, state.getCal(), state.getAct(),
                 state.getCurrent()));
            for (int j = 0; j < num; j++) {
                for (int i = 0; (attradj != null) && i < attradj.size(); i++) {
                    ((DevAttribute)attradj.get(i)).adjust();
                }
            }
        }
    }
}
