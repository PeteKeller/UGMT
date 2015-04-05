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
import javax.swing.*;
import org.w3c.dom.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;

/**
 * Class to hold Skills at the GUI. Any changes in an attribute here wil be
 * reflected at the state. Determination (load or roll) is also handled here.
 * The action buttons work as follows:
 * <table><tr>             <th>Normal</th>      <th>Options</th></tr>
 * <tr><td>Leaf</td>       <td>Recalc/Final</td><td>shift:Choose</td></tr>
 * <tr><td>Branched</td>   <td>(Un)fold</td>    <td>-</td></tr>
 * <tr><td>Specialized</td><td>(Un)fold</td>    <td>shift:choose</td></tr>
 * </table>
 * @author Michael Jung
 */
public class CharSkill {
    /** Implied values for superskills */
    private static Hashtable imply = new Hashtable();

    /** State reference */
    private State state;

    /** Name of skill */
    private String name;

    /** Attributes (for SB) label */
    private JComponent lattr;

    /** List of attributes for SB */
    private String[] calcattr;

    /** Sunsign label */
    private JComponent lsun;

    /** List of sunsigns for SB */
    private String[] calcsun;

    /** ML */
    private JTextField ml;

    /** OML */
    private JComponent oml;

    /** Prelim ML */
    private JTextField pml;

    /** Branch list (also skills) */
    private ArrayList branches;

    /** Specialisation list (also skills) */
    private ArrayList specials;

    /** Action button */
    private JButton act;

    /** Standard color of the button */
    private Color oldcol;

    /** Option tests (and values). Used by the action listener */
    private ArrayList options;

    /** Supress duplicate warnings */
    static boolean warn = true;

    /** Tooltips */
    final static public String[] tooltips = {
        "<html>Click to recalculate SB and copy PML to ML (finalize)<br>" +
        "While deploying options, use &lt;shift&gt;-click for an option here</html>",

        "Fold/Unfold subskills",

        "<html>Fold/Unfold specialisation skills<br>" +
        "While deploying options, use &lt;shift&gt;-click for an option here</html>"
    };

    /**
     * Constructor for headers only.
     * @param ew entry width
     * @param panel to add Gui items to
     */
    public CharSkill(JPanel panel, int ew) {
        GridBagConstraints constr = new GridBagConstraints();
        constr.gridx = constr.gridy = 0;
        constr.gridwidth = 1;
        constr.weightx = 1;
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.insets = new Insets(3,3,3,3);

        // Names
        String heads[] = new String[]
            { "Skill", "Attributes", "Sunsign", "Preliminary ML", "OML", "ML" };

        for (int i = 0; i < heads.length; i++) {
            JLabel head = new JLabel(heads[i]);
            int h = head.getPreferredSize().height;
            head.setPreferredSize(new Dimension(ew, h));
            panel.add(head, constr);
            constr.gridx++;
        }
    }

    /**
     * Constructor.
     * @param hide hide subskills on GUI?
     * @param aState state object reference
     * @param ew entry width
     * @param idx index of entry
     * @param node node with children
     * @param attr attributes already determined
     * @param panel to add Gui items to
     */
    public CharSkill(boolean hide, State aState, int ew, int idx, Node node, NamedNodeMap attr, JPanel panel) {
        this(null, '/', hide, aState, ew, idx, node, attr, panel, null);
    }

    /**
     * Constructor.
     * @param prefix prefix to skills (used with subskills)
     * @param hide hide subskills on GUI?
     * @param aState state object reference
     * @param ew entry width
     * @param idx index of entry
     * @param node node with children
     * @param attr attributes already determined
     * @param panel to add Gui items to
     * @param superskill parent skill
     */
    private CharSkill(String prefix, char sep, boolean hide, State aState, int ew, int idx, Node node, NamedNodeMap attr, JPanel panel, CharSkill superskill) {
        state = aState;

        GridBagConstraints constr = new GridBagConstraints();
        constr.gridx = 0;
        constr.gridy = idx;
        constr.gridwidth = GridBagConstraints.REMAINDER;
        constr.insets = new Insets(0,0,0,0);

        JPanel cpanel  = new JPanel(new GridBagLayout());
        panel.add(cpanel, constr);

        constr.insets = new Insets(3,3,3,3);
        constr.gridwidth = 1;
        constr.gridy = 0;

        String subname = attr.getNamedItem("name").getNodeValue();
        name = (prefix != null ? prefix + sep : "") + subname;

        act = new JButton(subname);
        int h = act.getPreferredSize().height;
        act.setPreferredSize(new Dimension(ew, h));
        oldcol = act.getBackground();
        cpanel.add(act, constr);

        // Branch button
        if (attr.getNamedItem("branch") != null &&
            attr.getNamedItem("branch").getNodeValue().equals("true")) {
            branches = new ArrayList();

            act.setToolTipText(tooltips[1]);
            Font of = act.getFont();
            Font nf = new Font
                (of.getName(), of.getStyle() + Font.ITALIC, of.getSize());
            act.setFont(nf);
            // Add action listener later, so it can hide subskills in
            // constructor
        }
        // Specialisation
        else if (attr.getNamedItem("special") != null &&
                 attr.getNamedItem("special").getNodeValue().equals("true")) {
            specials = new ArrayList();
            act.setToolTipText(tooltips[2]);
            Font of = act.getFont();
            Font nf = new Font
                (of.getName(), of.getStyle() + Font.ITALIC, of.getSize());
            act.setFont(nf);
            // Add action listener later, so it can hide specialised skills in
            // constructor
        }
        // Fixed button
        else {
            act.setToolTipText(tooltips[0]);
            act.addActionListener(new SkillActionListener(this));
        }

        // Attributes for SB
        constr.gridx++;
        if (superskill != null) calcattr = superskill.calcattr;
        if (attr.getNamedItem("attr") != null) {
            String str = attr.getNamedItem("attr").getNodeValue();
            calcattr = str.split("/");
        }
        lattr = new JLabel();
        lattr.setPreferredSize(new Dimension(ew, h));
        cpanel.add(lattr, constr);

        // Sunsign
        constr.gridx++;
        if (superskill != null) calcsun = superskill.calcsun;
        if (attr.getNamedItem("sun") != null) {
            String str = attr.getNamedItem("sun").getNodeValue();
            calcsun = str.split("/");
        }
        lsun = new JLabel();
        lsun.setPreferredSize(new Dimension(ew, h));
        cpanel.add(lsun, constr);

        // PML dummy for branches
        constr.gridx++;
        if (branches != null) {
            JLabel dum = new JLabel();
            dum.setPreferredSize(new Dimension(ew, h));
            cpanel.add(dum, constr);
        }
        else {
            pml = new JTextField();
            pml.setPreferredSize(new Dimension(ew, h));
            pml.getDocument().addDocumentListener(new MyDocListener(this, true));
            cpanel.add(pml, constr);
        }

        // OML
        constr.gridx++;
        String str = null;
        if (superskill != null && superskill.oml != null)
            str = getText(superskill.oml);
        else if (attr.getNamedItem("oml") != null)
            str = attr.getNamedItem("oml").getNodeValue();

        if (str != null) {
            oml = new JLabel(str);
            oml.setPreferredSize(new Dimension(ew, h));
            cpanel.add(oml, constr);
        }
        else {
            JLabel dum = new JLabel();
            dum.setPreferredSize(new Dimension(ew, h));
            cpanel.add(dum, constr);
        }

        // ML
        constr.gridx++;
        if (branches == null) {
            ml = new JTextField();
            ml.setPreferredSize(new Dimension(ew, h));
            ml.getDocument().addDocumentListener(new MyDocListener(this, false));
            cpanel.add(ml, constr);
        }
        else {
            JLabel dum = new JLabel();
            dum.setPreferredSize(new Dimension(ew, h));
            cpanel.add(dum, constr);
        }

        // Branches
        if (attr.getNamedItem("branch") != null &&
            attr.getNamedItem("branch").getNodeValue().equals("true")) {

            int cidx = 1;
            NodeList tree = node.getChildNodes();
            for (int i = 0; tree != null && i < tree.getLength(); i++) {
                Node child = tree.item(i);
                NamedNodeMap cattr = child.getAttributes();
                if (cattr != null) {
                    branches.add
                        (new CharSkill
                         (name, '/', hide, state, ew, cidx++, child, cattr,
                          cpanel, this));
                }
            }
            // Add action listener here so it can hide subskills in
            // constructor
            act.addActionListener
                (new BranchActionListener(hide, this, cpanel));
        }

        // Specialisation
        if (attr.getNamedItem("special") != null &&
            attr.getNamedItem("special").getNodeValue().equals("true")) {
            int cidx = 1;
            NodeList tree = node.getChildNodes();
            for (int i = 0; tree != null && i < tree.getLength(); i++) {
                Node child = tree.item(i);
                NamedNodeMap cattr = child.getAttributes();
                if (cattr != null) {
                    specials.add
                        (new CharSkill
                         (name, '#', hide, state, ew, cidx++, child, cattr,
                          cpanel, this));
                }
            }
            // Add action listener here so it can hide specialised skills in
            // constructor
            act.addActionListener
                (new SpecialActionListener(hide, this, cpanel));
        }

        // Imply
        if (attr.getNamedItem("imply") != null)
            CharSkill.setImply(name,  attr.getNamedItem("imply").getNodeValue());
    }

    /**
     * Return a calculation (used for HarnMaker import/export).
     */
    public int calcSB(State.MyCharacter c) { return calcSB(c, calcattr, calcsun); }

    /**
     * Return a calculation (used for HarnMaker import/export).
     */
    public static int calcSB(State.MyCharacter c, String[] mycalcattr, String[] mycalcsun) {
        if (mycalcattr == null) return 0;

        float sum = 0;
        for (int i = 0; i < mycalcattr.length; i++) {
            String val = c.getAttribute(mycalcattr[i]);
            if (val != null)
                sum += Integer.parseInt(val);
        }
        
        int tmp = (int)(sum/mycalcattr.length + .5);

        if (mycalcsun == null) return tmp;

        sum = 0;
        String sun = c.getAttribute("Sunsign");
        if (sun != null) {
            String[] sunpair = sun.split("/");
            for (int j = 0; j < sunpair.length; j++) {
                for (int i = 0; i < mycalcsun.length; i++) {
                    String sign = mycalcsun[i].substring(0,3);
                    if (sign.equals(sunpair[j]))
                        sum += (mycalcsun[i].charAt(3) == '+' ? 1 : -1) *
                            Integer.parseInt(mycalcsun[i].substring(4));
                }
            }
        }
        return tmp + (int)sum;
    }

    /**
     * Redo contents: recalculate sunsigns and attributes
     */
    public void redo() {
        if (calcattr != null && lattr != null) {
            float sum = 0;
            for (int i = 0; i < calcattr.length; i++) {
                String val = state.getAttribute(calcattr[i]);
                if (val != null)
                    sum += Integer.parseInt(val);
            }
            setText
                (lattr, Integer.toString((int)(sum/calcattr.length + .5)));
        }

        if (calcsun != null && lsun != null) {
            int sum = 0;
            String sun = state.getAttribute("Sunsign");
            if (sun != null) {
                String[] sunpair = sun.split("/");
                for (int j = 0; j < sunpair.length; j++) {
                    for (int i = 0; i < calcsun.length; i++) {
                        String sign = calcsun[i].substring(0,3);
                        if (sign.equals(sunpair[j]))
                            sum += (calcsun[i].charAt(3) == '+' ? 1 : -1) *
                                Integer.parseInt(calcsun[i].substring(4));
                    }
                }
            }
            setText(lsun, Integer.toString(sum));
        }

        for (int i = 0; branches != null && i < branches.size(); i++)
            ((CharSkill)branches.get(i)).redo();

        for (int i = 0; specials != null && i < specials.size(); i++)
            ((CharSkill)specials.get(i)).redo();
    }

    /**
     * Method to find a skill within this section.
     * @param skill skill to search
     * @return true if found
     */
    public CharSkill findSubSkill(String skill) {
        if (name.equals(skill)) return this;

        // Check subskills
        for (int i = 0; (branches != null) && (i < branches.size()); i++) {
            CharSkill sk = (CharSkill) branches.get(i);

            if (sk.getName().equals(skill)) return sk;

            CharSkill ssk = sk.findSubSkill(skill);
            if (ssk != null) return ssk;
        }

        // Check specialisation
        for (int i = 0; (specials != null) && (i < specials.size()); i++) {
            CharSkill sk = (CharSkill) specials.get(i);
            if (sk.getName().equals(skill)) return sk;
        }

        return null;
    }

    /**
     * Fix the preliminary skills and turn them final, i.e. finalize.
     */
    public void fixPrelim() {
        act.setEnabled(true);
        if (pml != null && ml != null) {
            ml.setText(pml.getText());
        }
        if (branches != null) {
            for (int i = 0; i < branches.size(); i++)
                ((CharSkill)branches.get(i)).fixPrelim();
        }
        if (specials != null) {
            for (int i = 0; i < specials.size(); i++)
                ((CharSkill)specials.get(i)).fixPrelim();
        }
    }

    /**
     * Method to clear all skills
     */
    public void clear() {
        if (ml != null) ml.setText("");
        if (pml != null) pml.setText("");
        for (int i = 0; branches != null && i < branches.size(); i++)
            ((CharSkill)branches.get(i)).clear();
        for (int i = 0; specials != null && i < specials.size(); i++)
            ((CharSkill)specials.get(i)).clear();
    }

    /**
     * Get name.
     * @return name for skill
     */
    public String getName() { return name; }
        
    /**
     * Enable the skill by changing the button's color. Used by options during
     * development.
     * @param skills skill list (null if all)
     * @param ops list of OptionSettings
     * @return whether something was actually done
     */
    public boolean enable(String[] skills, ArrayList ops) {
        boolean ret = false;

        // Is this skill contained in the parameter list
        boolean contains = false;
        if (skills != null)
            for (int i = 0; !contains && (i < skills.length); i++)
                contains = (skills[i].equals(name));
        else
            contains = true;

        // If not don't worry
        if (contains) {
            options = ops;
            if (options != null) {
                for (int i = 0; !ret && (i < options.size()); i++) {
                    OptionSetting tv = (OptionSetting)options.get(i);
                    String val = (pml != null ? getText(pml) : null);
                    ret |= (CharAttribute.evalCond(name, tv.test, state) && 
                            tv.costsMet(name));
                }
            }
        }

        for (int i = 0; branches != null && i < branches.size(); i++)
            ret |= ((CharSkill)branches.get(i)).enable(skills, ops);
        for (int i = 0; specials != null && i < specials.size(); i++)
            ret |= ((CharSkill)specials.get(i)).enable(skills, ops);

        if (!ret) {
            act.setEnabled(false);
        }
        else {
            act.setEnabled(true);
        }
        return ret;
    }

    /**
     * Set skill value. Used when loading a character. Cleans PML as well.
     * @param skilllist list of available skills.
     */
    public void set() {
        String value = state.getSkill(name);
        if (pml != null) pml.setText("");
        if (ml != null) ml.setText(value != null ? value : "");

        for (int i = 0; branches != null && i < branches.size(); i++)
            ((CharSkill)branches.get(i)).set();

        for (int i = 0; specials != null && i < specials.size(); i++)
            ((CharSkill)specials.get(i)).set();

        // Recalcultate SBs and sunsigns. Since this recurses as well, we do
        // this only for leaves. Any NumberFormatEceptions at this point are
        // ignored for incomplete characters
        try {
            if (branches == null) redo();
        }
        catch (NumberFormatException e) {
            if (warn) {
                System.err.println("Example warning: " + name + " incomplete");
                Thread t = new Thread() {
                        public void run() {
                            try { sleep(100); } catch (Exception e) {}
                            warn = true;
                        }
                    };
                warn = false;
                t.start();
            }
            
        }
    }

    /**
     * Set PML of a skill. The passed value may be zero in which case the
     * standard is tried.
     * @param skill skill name
     * @param value skill value
     */
    public void setPML(String skill, String value) {
        if (lattr != null && lsun != null && pml != null && skill.equals(name)) {
            // Get SB
            int sb = 0;

            String s1 = getText(lattr);
            String s2 = getText(lsun);
            if (s1.length() != 0) sb += Integer.parseInt(s1);
            if (s2.length() != 0) sb += Integer.parseInt(s2);

            // Get OML formula
            String myeval = (oml != null ? getText(oml) : "0xSB");

            // Passed "value" overrides OML
            if (value != null) myeval = value;

            // Check the previous PML
            int old = 0;
            if (pml.getText().length() > 0)
                old = Integer.parseInt(pml.getText());

            // Evaluate the formula (note that OML may recursively be
            // present).
            int myml = evalTerm
                (old, myeval, sb, (oml != null ? getText(oml) : "0xSB"));

            // Change if it makes a difference. Otherwise increase by SB.
            if (old < myml) {
                pml.setText(Integer.toString(myml));
            }
            else {
                pml.setText(Integer.toString(old + sb));
            }
        }

        if (branches != null && !skill.equals(name)) {
            for (int i = 0; i < branches.size(); i++)
                ((CharSkill)branches.get(i)).setPML(skill, value);
        }
    }


    /**
     * Getter for implied values of superskills.
     * @param name name of skill
     * @return implied value
     */
    static public String getImply(String name) { return (String) imply.get(name); }

    /**
     * Setter for implied values of superskills.
     * @param name name of skill
     * @param value implied value
     */
    static public void setImply(String name, String value) { imply.put(name, value); }

    /**
     * Static. Evaluates terms of the form "a-bx...", "a+bx...", "bx..." and
     * "a", where a, and b may be numbers and a also 'OML' and ''.
     * @param oldval previous value
     * @param term term to evaluate
     * @param sb substitute for ...
     * @param oml term for OML (evaluated recursively)
     */
    static int evalTerm(int oldval, String term, int sb, String oml) {
        int iplus = term.indexOf('+');
        int iminus = term.indexOf('-');
        int ix = term.indexOf('x');
        int ioml = term.indexOf("OML");

        int res = 0;

        // If OML present start with it
        if (ioml > -1) {
            res = evalTerm(oldval, oml, sb,  null);
        }
        // Otherwise use first term as start
        else if (iplus > -1) {
            if (iplus > 0)
                res = Integer.parseInt(term.substring(0, iplus));
            else
                res = oldval;
        }
        else if (iminus > -1) {
            if (iminus > 0)
                res = Integer.parseInt(term.substring(0, iminus));
            else
                res = oldval;
        }

        if (ix > -1) {
            int f1;
            if (iplus > -1)
                f1 = Integer.parseInt(term.substring(iplus + 1, ix));
            else // iminus
                f1 = Integer.parseInt(term.substring(iminus + 1, ix));
            res += f1*sb;
        }

        if (ix == -1 && iplus == -1 && ioml == -1 && iminus == -1)
            return Integer.parseInt(term);

        return res;
    }

    /**
     * Static Getter for JLabel and JTextField
     * @param f component
     * @return text value of f
     */
    public static String getText(JComponent f) {
        if (f instanceof JLabel)
            return ((JLabel)f).getText(); 
        return ((JTextField)f).getText();
    }

    /**
     * Static Setter for JLabel and JTextField
     * @param f component
     * @param v text value to set
     */
    public static void setText(JComponent f, String v) {
        if (f instanceof JLabel)
            ((JLabel)f).setText(v); 
        else
            ((JTextField)f).setText(v); 
    }

    /**
     * Action listener class for skills.
     */
    class SkillActionListener implements ActionListener {
        /** attribute reference */
        CharSkill skill;
        /**
         * Constructor.
         * @param aSkill character attribute
         */
        SkillActionListener(CharSkill aSkill) {
            skill = aSkill;
        }
        /**
         * ActionListener method.
         * @param e event
         */
        public void actionPerformed(ActionEvent e) {
            if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0 &&
                options != null) {
                int cost = 0;
                for (int i = 0; i < options.size(); i++) {
                    OptionSetting tv = (OptionSetting) options.get(i);
                    String val = (pml != null ? getText(pml) : null);
                    if (CharAttribute.evalCond(name, tv.test, state) && 
                        tv.costsMet(name)) {
                        tv.setCosts(name);
                        skill.setPML(skill.name, tv.oml);
                        break;
                    }
                }
                boolean on = state.getCurrentOpt().enable();
                if (!on) state.enableNextButton();
            }
            else {
                skill.fixPrelim();
                skill.redo();
            }
        }
    }

    /**
     * Action listener class for skills which have subskills. This class
     * removes invisible and shows visible subskills.
     */
    class BranchActionListener implements ActionListener {
        /** skill reference */
        CharSkill skill;
        /** panel to hide/show on */
        JPanel panel;
        /** arraylist of hidden items */
        ArrayList hidden;
        /**
         * Constructor.
         * @param hide hide skills on startup
         * @param aSkill skill reference
         * @param aPanel panel to hide/Show on
         */
        BranchActionListener(boolean hide, CharSkill aSkill, JPanel aPanel) {
            skill = aSkill;
            panel = aPanel;
            hidden = new ArrayList();
            if (hide) actionPerformed(null);
        }
        /**
         * ActionListener method.
         * @param e event
         */
        public void actionPerformed(ActionEvent e) {
            if (hidden.size() == 0) {
                // i = 0..5 is the first row button which stays
                Component[] cmp = panel.getComponents();
                for (int i = 6; i < cmp.length; i++) {
                    hidden.add(cmp[i]);
                    panel.remove(cmp[i]);
                }
            }
            else {
                GridBagConstraints constr = new GridBagConstraints();
                constr.gridx = 0;
                constr.gridwidth = GridBagConstraints.REMAINDER;
                for (int i = 0; i < hidden.size(); i++) {
                    constr.gridy = i+1;
                    panel.add((Component)hidden.get(i), constr);
                }
                hidden.clear();
            }
            panel.revalidate();
        }
    }

    /**
     * Action listener class for skills which have specialisation skills. This
     * class removes invisible and shows visible subskills.
     */
    class SpecialActionListener implements ActionListener {
        /** skill reference */
        CharSkill skill;
        /** panel to hide/show on */
        JPanel panel;
        /** arraylist of hidden items */
        ArrayList hidden;
        /**
         * Constructor.
         * @param hide hide skills on startup
         * @param aSkill skill reference
         * @param aPanel panel to hide/Show on
         */
        SpecialActionListener(boolean hide, CharSkill aSkill, JPanel aPanel) {
            skill = aSkill;
            panel = aPanel;
            hidden = new ArrayList();
            if (hide) actionPerformed(null);
        }
        /**
         * ActionListener method.
         * @param e event
         */
        public void actionPerformed(ActionEvent e) {
            if (e != null && (e.getModifiers() & InputEvent.SHIFT_MASK) != 0 &&
                options != null) {
                int cost = 0;
                for (int i = 0; i < options.size(); i++) {
                    OptionSetting tv = (OptionSetting) options.get(i);
                    String val = (pml != null ? getText(pml) : null);
                    if (CharAttribute.evalCond(name, tv.test, state) && 
                        tv.costsMet(name)) {
                        tv.setCosts(name);
                        skill.setPML(skill.name, tv.oml);
                        break;
                    }
                }
                boolean on = state.getCurrentOpt().enable();
                if (!on) state.enableNextButton();
            }
            else {
                if (hidden.size() == 0) {
                    // i = 0..5 is the first row button which stays
                    Component[] cmp = panel.getComponents();
                    for (int i = 6; i < cmp.length; i++) {
                        hidden.add(cmp[i]);
                        panel.remove(cmp[i]);
                    }
                }
                else {
                    GridBagConstraints constr = new GridBagConstraints();
                    constr.gridx = 0;
                    constr.gridwidth = GridBagConstraints.REMAINDER;
                    for (int i = 0; i < hidden.size(); i++) {
                        constr.gridy = i+1;
                        panel.add((Component)hidden.get(i), constr);
                    }
                    hidden.clear();
                }
                panel.revalidate();
            }
        }
    }

    /**
     * Text field listener class for skills. The OML and the PML fields are
     * monitored with this class.
     */
    class MyDocListener implements DocumentListener {
        /** attribute reference */
        CharSkill skill;
        /** prelim skill? */
        boolean prelim;
        /**
         * Constructor.
         * @param aSkill skill to monitor
         * @param aPrelim PML monitored (or OML)?
         */
        MyDocListener(CharSkill aSkill, boolean aPrelim) {
            prelim = aPrelim;
            skill = aSkill;
        }
        /**
         * DocumentListener method
         * @param e event
         */
        public void removeUpdate(DocumentEvent e) { playBack(e); }
        /**
         * DocumentListener method
         * @param e event
         */
        public void insertUpdate(DocumentEvent e) { playBack(e); }
        /**
         * DocumentListener method
         * @param e event
         */
        public void changedUpdate(DocumentEvent e) { playBack(e); }
        /**
         * Play field to state.
         */
        private void playBack(DocumentEvent e) {
            try {
                javax.swing.text.Document doc = e.getDocument();
                String txt = doc.getText(0, doc.getLength());

                if (prelim)
                    state.setPrelimSkill(skill.name, txt);
                else
                    state.setSkill(skill.name, txt);

                state.setDirty(State.edit);
            }
            catch (BadLocationException ex) {
                // Can't happen
            }
        }
    }
}
