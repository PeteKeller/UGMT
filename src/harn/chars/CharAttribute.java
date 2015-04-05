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
import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.Collator;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import org.w3c.dom.*;

/**
 * Class to hold attributes at the GUI. Any changes in an attribute here wil
 * be reflected at the state. Determination (load or roll) is also handled
 * here.
 * @author Michael Jung
 */
public class CharAttribute {

    /** Main reference */
    private Main main;
    /** Table or conditional table (if required for roll) */
    private Object table;
    /** Field (editable attribute) */
    private JTextField field;
    /** Name of attribute */
    private String name;

    // random determination characteristic
    /** Roll dice */
    private String dice;
    /** Set to equal other attribute */
    private String set;
    /** Adjustments */
    private Hashtable cond2adj;
    /** Late adjustments */
    private Hashtable cond2lateadj;
    /** function */
    private String function;
    /** argument */
    private String arg;
    /** minutes per hour */
    final public static int HOUR = 60;
    /** minutes per day */
    final public static int DAY = 24 * HOUR;
    /** minutes per year */
    final public static int YEAR = 360 * DAY;
    /** ParseFormula */
    final private ParseFormula pf = new ParseFormula();

    /**
     * Constructor.
     * @param aMain main reference
     * @param ew entry width
     * @param idx index of entry
     * @param node node with children
     * @param attr attributes already determined
     * @param panel to add Gui items to
     */
    public CharAttribute(Main aMain, int ew, int idx, Node node, NamedNodeMap attr, JPanel panel) {
        main = aMain;

        cond2adj = new Hashtable();
        cond2lateadj = new Hashtable();

        // Get name, create button
        name = attr.getNamedItem("name").getNodeValue();
        JButton lab = new JButton(name);
        int h = lab.getPreferredSize().height;
        lab.setPreferredSize(new Dimension(ew, h));

        // add (PC) listener
        Node pc = attr.getNamedItem("pc");
        lab.addActionListener(new AttrActionListener(pc, this));
        lab.setToolTipText("Reroll attribute" + (pc != null ? "; <shift> use PC rule" : ""));

        // Edit field
        field = new JTextField();
        field.getDocument().addDocumentListener(new MyDocListener(this));

        // Add to panel
        GridBagConstraints constr = new GridBagConstraints();
        constr.gridx = 2 * (idx / 4);
        constr.gridy = idx % 4;
        constr.insets = new Insets(3, 3, 3, 3);

        panel.add(lab, constr);
        constr.gridx++;
        field.setPreferredSize(new Dimension(ew, h));
        panel.add(field, constr);

        // Get characteristic from XML
        Node a;
        a = attr.getNamedItem("roll");
        if (a != null) {
            dice = a.getNodeValue();
        }

        a = attr.getNamedItem("function");
        if (a != null) {
            function = a.getNodeValue();
        }

        a = attr.getNamedItem("arg");
        if (a != null) {
            arg = a.getNodeValue();
        }

        a = attr.getNamedItem("set");
        if (a != null) {
            set = a.getNodeValue();
        }

        // All possible outcomes
        TreeSet results = new TreeSet(Collator.getInstance(Locale.FRANCE));

        // Get table & adjustments (more characteristic)
        NodeList tree = node.getChildNodes();
        for (int i = 0; i < tree.getLength(); i++) {
            Node child = tree.item(i);
            NamedNodeMap cattr = child.getAttributes();
            if (cattr != null && child.getNodeName().equals("table")) {
                if (table == null) {
                    table = new RandTable(main);
                }
                ((RandTable) table).addRow(child, cattr, results);
            }
            if (cattr != null && child.getNodeName().equals("adjust")) {
                String val = cattr.getNamedItem("value").getNodeValue();
                String test = cattr.getNamedItem("test").getNodeValue();
                cond2adj.put(test, val);
            }
            if (cattr != null && child.getNodeName().equals("lateadjust")) {
                String ladj = cattr.getNamedItem("value").getNodeValue();
                Integer val = new Integer(ladj.substring(0, ladj.length() - 1));
                String test = cattr.getNamedItem("test").getNodeValue();
                cond2lateadj.put(test, val);
            }
            if (cattr != null && child.getNodeName().equals("use")) {
                if (table == null) {
                    table = new CondTable(main);
                }
                ((CondTable) table).addRow(child, cattr, results);
            }
        }

        if (results.size() == 0) {
            return;
        }

        // Create selection popup
        JPopupMenu pop = new MyPopupMenu();
        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                MyMenuItem item = (MyMenuItem) e.getSource();
                String txt =
                        (item.heritage.length() != 0 ? item.heritage + "/" : "") + item.getText();
                field.setText(txt);
            }
        };

        Iterator iter = results.iterator();
        while (iter.hasNext()) {
            String[] split = ((String) iter.next()).split("/");
            JMenuItem mi = null;
            for (int i = 0; i < split.length; i++) {
                JMenuItem menu = getMenuItem(pop, (MyMenu) mi, split[i], i == split.length - 1);
                mi = menu;
            }
            mi.addActionListener(al);
        }
        lab.addMouseListener(new MyMouseAdapter(pop));
    }

    /**
     * Create or use a new MenuItem.
     * @param root item to insert into
     * @param next string to create menu
     * @param last last item
     * @return added MenuItem
     */
    private JMenuItem getMenuItem(JPopupMenu pop, MyMenu root, String next,
            boolean last) {
        if (root == null) {
            MenuElement el[] = pop.getSubElements();
            for (int i = 0; i < el.length; i++) {
                if (((JMenuItem) el[i]).getText().equals(next)) {
                    return (JMenuItem) el[i];
                }
            }
            JMenuItem mi = (last ? new MyMenuItem("", next) : (JMenuItem) new MyMenu("", next));
            pop.add(mi);
            return mi;
        }
        else {
            for (int i = 0; i < root.getItemCount(); i++) {
                if (((JMenuItem) root.getItem(i)).getText().equals(next)) {
                    return (JMenuItem) root.getItem(i);
                }
            }
            String heritage =
                    root.heritage + (root.heritage.length() == 0 ? "" : "/") + root.getText();
            JMenuItem mi = (last ? new MyMenuItem(heritage, next) : (JMenuItem) new MyMenu(heritage, next));
            root.add(mi);
            return mi;
        }
    }

    /**
     * Get name.
     * @return name of this attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Roll an attribute.
     * @param rule rule with which to roll. (n<em>d</em>d<em>m</em>m)
     */
    public void roll(String rule) {
        String myArg = null;

        if (dice != null || set != null) {
            int roll = 0;
            // Roll standard
            if (rule == null && dice != null) {
                String myRoll = pf.eval(dice);
                try {
                    roll = Integer.parseInt(myRoll);
                }
		catch (NumberFormatException nfe) {
		    nfe.printStackTrace();                
                }
            } // Use rule instead
            else if (rule != null) {
                String myRoll = pf.eval(rule);
                try {
                    roll = Integer.parseInt(myRoll);
                }
                catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
            }
            else { // Use set
                roll = Integer.parseInt(main.getState().getAttribute(set));
            }

            // Preliminary setting of attribute
            main.getState().setAttribute(name, Integer.toString(roll));

            // Adjustements
            Iterator iter = cond2adj.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                if (evalCond(null, key, main.getState())) {
                    String val = (String) cond2adj.get(key);
                    try {
                        roll += Integer.parseInt(val);
                    }
                    catch (Exception e) {
                        // Parsing of right side
                        if (val.indexOf("-") >= 1) {
                            String tmp[] = val.split("-");
                            val = Integer.toString(Integer.parseInt(main.getState().getAttribute(tmp[0])) -
                                    Integer.parseInt(tmp[1]));
                        }
                        if (val.indexOf("+") >= 1) {
                            String tmp[] = val.split("\\+");
                            val = Integer.toString(Integer.parseInt(main.getState().getAttribute(tmp[0])) +
                                    Integer.parseInt(tmp[1]));
                        }
                        roll = Integer.parseInt(val);
                    }
                }
            }

            // Late adjustements
            iter = cond2lateadj.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                if (evalCond(null, key, main.getState())) {
                    int adj = ((Integer) cond2lateadj.get(key)).intValue();
                    roll = (roll * (100 + adj)) / 100;
                }
            }

            // Have result
            myArg = Integer.toString(roll > 0 ? roll : 0);
        }

        // an argument supersedes the roll
        if (arg != null) {
            String args[] = arg.split(",");
            if (args.length == 1) {
                myArg = main.getState().getAttribute(args[0]);
            }
            else {
                int sum = 0;
                for (int i = 0; i < args.length; i++) {
                    sum += Integer.parseInt(main.getState().getAttribute(args[i]));
                }
                myArg = Integer.toString((int) (sum / (double) args.length + .5));
            }
        }

        if (table != null) { // Result from table
            // Determine numbers to roll in table
            //TODO: FIX NEXT LINES TO USE PARSE FORMULA ??
            String r[] = dice.split("d");
            int n = Integer.parseInt(r[0]);
            int d = Integer.parseInt(r[1]);
            if (table instanceof RandTable) {
                myArg = ((RandTable) table).get(Integer.parseInt(myArg), n, d);
            }
            else {
                myArg = ((CondTable) table).get(Integer.parseInt(myArg), n, d);
            }
        }
        else if (function != null) { // Result from function
            myArg = evalFunc(function, myArg, null, main.getState().getCal(),
                    main.getState().getAct(), main.getState().getCurrent());
        }

        // Adapt state
        if (myArg != null) {
            field.setText(myArg);
            main.getState().setAttribute(name, myArg);
        }

        // Something was done
        main.getState().setDirty(true);
    }

    /**
     * Method to clear an attributes.
     */
    public void clear() {
        field.setText("");
    }

    /**
     * Set attribute value.
     * @param value value to set
     */
    public void set(String value) {
        field.setText(value);
    }

    /**
     * Static way to test a condition. Parses formulas of types: "a<b", "a>b",
     * "a=b", and "a!b", which can be "and"ed through "|". Numerical
     * comparison is obvious. String equality means right is contained in
     * left. "<" for strings means left is contained in the comma-separated
     * right list. "a" is taken to be an attribute, "b" a value. If "a" is
     * "$value", the skill in test is taken instead. If "$var" is specified,
     * not it's value is taken but its name instead.
     * @param var current skill being tested (if being tested)
     * @param test condition to test
     * @param state reference to get atttributes/skill from
     */
    public static boolean evalCond(String var, String test, State state) {
        if (test == null) {
            return true;
        }

        String[] cond = test.split("[|]");
        boolean met = true;
        for (int i = 0; (i < cond.length) && met; i++) {
            String[] sub = cond[i].split("[=<!>]");

            // Parsing of left side
            String lval;
            if (sub[0].equals("$var")) {
                lval = var;
            }
            else if (var != null && sub[0].equals("$value")) {
                lval = state.getPrelimSkill(var);
            }
            else {
                lval = state.getAttribute(sub[0]);
            }

            // Normalize emptieness
            if (lval == null || lval.length() == 0) {
                lval = "0";
            }

            // Parsing of right side
            String rval = sub[1];
            try {
                if (sub[1].indexOf("-") >= 1) {
                    String tmp[] = sub[1].split("-");
                    rval = Integer.toString(Integer.parseInt(state.getAttribute(tmp[0])) -
                            Integer.parseInt(tmp[1]));
                }
                if (sub[1].indexOf("+") >= 1) {
                    String tmp[] = sub[1].split("\\+");
                    rval = Integer.toString(Integer.parseInt(state.getAttribute(tmp[0])) +
                            Integer.parseInt(tmp[1]));
                }
            }
            catch (NumberFormatException e) {
                // Ignore, keep sub[1]
            }

            try {
                // Numeric "=", "<", ">", and "!"
                if (cond[i].indexOf("=") > -1) {
                    met &= (Integer.parseInt(lval) == Integer.parseInt(rval));
                }
                else if (cond[i].indexOf("<") > -1) {
                    met &= (Integer.parseInt(lval) < Integer.parseInt(rval));
                }
                else if (cond[i].indexOf(">") > -1) {
                    met &= (Integer.parseInt(lval) > Integer.parseInt(rval));
                }
                else if (cond[i].indexOf("!") > -1) {
                    met &= (Integer.parseInt(lval) != Integer.parseInt(rval));
                }
            }
            catch (Exception e) {
                // String "=", "<", and "!"
                if (cond[i].indexOf("=") > -1) {
                    met &= (lval.indexOf(rval) == 0);
                }
                else if (cond[i].indexOf("!") > -1) {
                    met &= (lval.indexOf(rval) != 0);
                }
                else if (cond[i].indexOf("<") > -1) {
                    String[] all = rval.split(",");
                    boolean lmet = false;
                    for (int j = 0; (j < all.length) && !lmet; j++) {
                        lmet |= (lval.indexOf(all[j]) == 0);
                    }
                    met &= lmet;
                }
            }
        }
        return met;
    }

    /**
     * Evaluate a function. Functiosn are special strings or a term to be
     * evaluated for SB.
     * @param function to eval
     * @param arg to the function
     * @param arg2 second args to the function
     * @param cal calendar reference to convert date formats and sunsign
     */
    public static String evalFunc(String function, String arg, String arg2, TimeFrame cal, CharGroup act, State.MyCharacter mychar) {
        if (function.equals("date")) {
            int t0 = 720;
            if (act != null) {
                t0 = (int) act.getDate() / YEAR;
            }
            // In pathological cases (arg < 0 and group date close to 0) this
            // can yield garbage.
            return cal.date2string((t0 * 360 + Integer.parseInt(arg)) * DAY);
        }
        else if (function.equals("sunsign")) {
            return cal.getSunsign(cal.string2datetime(arg != null ? arg : "1-Nuz-720"));
        }
        else if (function.equals("subtract_year")) // In pathological cases (net years before 0) this can yield
        // garbage.
        {
            return cal.date2string(cal.string2datetime(arg2) - Integer.parseInt(arg) * YEAR);
        }
        else if (function.equals("average")) {
            return arg;
        }
        else if (function.equals("today_year")) {
            return Long.toString((act.getDate() - cal.string2datetime(arg)) / YEAR);
        }
        else if (function.equals("calc_new_age")) {
            String bds = mychar.getAttribute("Birthdate");
            long bd = cal.string2datetime(bds != null && bds.length() != 0 ? bds : "1-Nuz-720");
            long age = Integer.parseInt(arg != null && arg.length() != 0 ? arg : "0");
            return Long.toString((act.getDate() - bd - age * YEAR) / YEAR);
        }
        else // Assume a + b x arg
        {
            return Integer.toString(CharSkill.evalTerm(0, function + "SB", Integer.parseInt(arg), null));
        }
    }

    /**
     * Action listener class for attributes.
     */
    class AttrActionListener implements ActionListener {

        /** attribute reference */
        CharAttribute attr;
        /** PC rule applies */
        String rule;

        /**
         * Constructor.
         * @param pc is key attribute for PCs
         * @param anAttr character attribute
         */
        AttrActionListener(Node pcrule, CharAttribute anAttr) {
            attr = anAttr;
            if (pcrule != null) {
                rule = pcrule.getNodeValue();
            }
        }

        /**
         * ActionListener method.
         * @param e event
         */
        public void actionPerformed(ActionEvent e) {
            if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                attr.roll(rule);
            }
            else {
                attr.roll(null);
            }
        }
    }

    /**
     * Text field listener class for attributes.
     */
    class MyDocListener implements DocumentListener {

        /** attribute reference */
        CharAttribute attr;

        /**
         * Constructor.
         * @param anAttr character attribute
         */
        MyDocListener(CharAttribute anAttr) {
            attr = anAttr;
        }

        /**
         * DocumentListener method
         * @param e event
         */
        public void removeUpdate(DocumentEvent e) {
            playBack(e);
        }

        /**
         * DocumentListener method
         * @param e event
         */
        public void insertUpdate(DocumentEvent e) {
            playBack(e);
        }

        /**
         * DocumentListener method
         * @param e event
         */
        public void changedUpdate(DocumentEvent e) {
            playBack(e);
        }

        /**
         * Play field to state.
         * @param e event
         */
        private void playBack(DocumentEvent e) {
            try {
                javax.swing.text.Document doc = e.getDocument();
                String txt = doc.getText(0, doc.getLength());
                main.getState().setAttribute(attr.name, txt);
                main.getState().setDirty(State.edit);
            }
            catch (BadLocationException ex) {
                // Can't happen
            }
        }
    }

    /**
     * Used for starting menus.
     */
    class MyMouseAdapter extends MouseAdapter {

        /** Popuop to popup */
        JPopupMenu pop;

        /** Constructor */
        MyMouseAdapter(JPopupMenu aPop) {
            pop = aPop;
        }

        /** IF method */
        public void mousePressed(MouseEvent e) {
            popup(e);
        }

        /** IF method */
        public void mouseReleased(MouseEvent e) {
            popup(e);
        }

        /** generic method */
        private void popup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                pop.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    /**
     * Used for wrapping menu items.
     */
    class MyPopupMenu extends JPopupMenu {

        MyPopupMenu() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            int sh = Toolkit.getDefaultToolkit().getScreenSize().height;
            if (d.height > sh) {
                return new Dimension(d.width, sh);
            }
            return d;
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
