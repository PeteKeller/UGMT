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
package harn.skills;

import rpg.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Class for a skill line on the GUI.
 * @author Michael Jung
 */
class GuiLine {
    /** Main reference */
    private Main main;

    /** Editable Text field: name */
    JTextField name;

    /** Editable Text field: ML */
    private JTextField ml;

    /** Editable Text field: dice */
    private JTextField d;

    /** Editable Text field: adj-1 */
    private JTextField adj1;

    /** Editable Text field: adj-2 */
    private JTextField adj2;

    /** Editable Text field: adj-3 */
    private JTextField adj3;

    /** Button to roll */
    private JButton b;

    /** Result field: roll */
    private JTextField roll;

    /** Result field: success */
    private JTextField success;

    /** Result field: end result */
    private JTextField result;

    /** Select */
    private JComboBox cbox;

    /** current random number */
    private int rand = 0;

    /** Item constraints */
    private GridBagConstraints constrItem;

    /** Top panel */
    private JPanel top;

    /** Section */
    State.Section sect;

    /** Point of popup creation */
    private int gx, gy;

    /**
     * Constructor. A Gui line is always added to display.
     * @param aSect section this line relates to
     * @param aTop top panel
     * @param aMain main reference
     */
    GuiLine(State.Section aSect, JPanel aTop, Main aMain) {
        sect = aSect;
        main = aMain;
        top = aTop;
	constrItem = new GridBagConstraints();
	constrItem.weightx = constrItem.weighty = 1;
        constrItem.anchor = GridBagConstraints.NORTH;
	constrItem.fill = GridBagConstraints.BOTH;
        constrItem.insets = new Insets(2, 2, 2, 2);
        constrItem.gridx = 0;

        name = new JTextField(sect.name);
        name.getDocument().addDocumentListener
            (new MyDocListener(sect,0,this));
        if (sect.readonly) name.setEnabled(false);

        name.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        gx = e.getX(); gy = e.getY();
                        main.lineMenu.setLine(GuiLine.this);
                        if (sect.readonly) main.lineMenu.mi2.setEnabled(false);
                        main.lineMenu.show(e.getComponent(), gx, gy);
                    }
                }
            });

        constrItem.gridx = 1;
        ml = new JTextField(sect.ml);
        ml.getDocument().addDocumentListener
            (new MyDocListener(sect,1,this));
        if (sect.readonly) ml.setEnabled(false);

        constrItem.gridx = 2;
        d = new JTextField(sect.d);
        d.getDocument().addDocumentListener
            (new MyDocListener(sect,2,this));

        constrItem.gridx = 3;
        adj1 = new JTextField(sect.adj1);
        adj1.getDocument().addDocumentListener
            (new MyDocListener(sect,4,this));

        constrItem.gridx = 4;
        adj2 = new JTextField(sect.adj2);
        adj2.getDocument().addDocumentListener
            (new MyDocListener(sect,5,this));

        constrItem.gridx = 5;
        adj3 = new JTextField(sect.adj3);
        adj3.getDocument().addDocumentListener
            (new MyDocListener(sect,6,this));

        constrItem.gridx = 6;
        b = new JButton("Roll");
        b.addActionListener(new MyActionListener(this));

        constrItem.gridx = 7;
        roll = new JTextField("-");
        roll.setEnabled(false);

        constrItem.gridx = 8;
        success = new JTextField("-");
        success.setEnabled(false);

        constrItem.gridx = 9;
        cbox = new JComboBox(new String[] { "---" });
        cbox.addActionListener(new MyActionListener(this));

        constrItem.gridx = 10;
        result = new JTextField("-");

        testCombo(sect.name, this);
    }

    /** Set GUI y */
    public void setY(int y) { constrItem.gridy = y; }

    /**
     * GUI-validate this object
     */
    public void revalidate() {
        if (top.isAncestorOf(name)) {
            name.setEnabled(!sect.readonly);
            ml.setEnabled(!sect.readonly);

            name.setText(sect.name);
            ml.setText(sect.ml);
            d.setText(sect.d);
            adj1.setText(sect.adj1);
            adj2.setText(sect.adj2);
            adj3.setText(sect.adj3);

            testCombo(sect.name, this);
            calc();
        }
    }

    /**
     * Remove from display.
     */
    public void undisplay() {
        main.getState().setDirty(true);
        top.remove(name);
        top.remove(ml);
        top.remove(d);
        top.remove(adj1);
        top.remove(adj2);
        top.remove(adj3);
        top.remove(b);
        top.remove(roll);
        top.remove(success);
        top.remove(cbox);
        top.remove(result);
        top.revalidate();
    }

    /**
     * Redisplay this object.
     */
    public void display() {
        revalidate();

        constrItem.gridx = 0;
        top.add(name, constrItem);
        constrItem.gridx = 1;
        top.add(ml, constrItem);
        constrItem.gridx = 2;
        top.add(d, constrItem);
        constrItem.gridx = 3;
        top.add(adj1, constrItem);
        constrItem.gridx = 4;
        top.add(adj2, constrItem);
        constrItem.gridx = 5;
        top.add(adj3, constrItem);
        constrItem.gridx = 6;
        top.add(b, constrItem);
        constrItem.gridx = 7;
        top.add(roll, constrItem);
        constrItem.gridx = 8;
        top.add(success, constrItem);
        constrItem.gridx = 9;
        top.add(cbox, constrItem);
        constrItem.gridx = 10;
        top.add(result, constrItem);
    }

    /**
     * Test a skillname for combobox relevance.
     * @param skill skill name
     * @param y level in display
     */
    public void testCombo(String skill, GuiLine line) {
        if (!main.getData().getSkillList().containsKey(skill) &&
            !line.cbox.getSelectedItem().equals("---")) {
            line.cbox.removeAllItems();
            line.cbox.addItem("---");
        }
        if (main.getData().getSkillList().containsKey(skill) &&
            line.cbox.getSelectedItem().equals("---")) {
            line.cbox.removeAllItems();
            Hashtable ht =
                (Hashtable) main.getData().getSkillList().get(skill);
            Iterator iter = ht.keySet().iterator();
            while (iter.hasNext()) {
                line.cbox.addItem(iter.next());
            }
        }
    }

    /**
     * Recalculates the line. Randomness is supplied elsewhere.
     */
    private void calc() {
        try {
            if (rand == 0) return;

            // Get Die
            int post = Integer.parseInt(d.getText().split("d")[1]);

            // Roll
            // Apply adjustments to ML
            int mli = Integer.parseInt(ml.getText());
            mli = adjust(mli, adj1.getText());
            mli = adjust(mli, adj2.getText());
            mli = adjust(mli, adj3.getText());

            if (post == 100 && mli < 5) mli = 5;
            if (post == 100 && mli > 95) mli = 95;

            // Set roll
            roll.setText
                (Integer.toString(mli) + "/" + Integer.toString(rand));

            // Set success level
            String suc = (rand <= mli ? "S" : "F");
            if (post == 100) {
                if (rand%5 == 0) suc = "C" + suc;
                else suc = "M" + suc;
            }
            success.setText(suc);

            // Set result
            if (!main.getData().getSkillList().containsKey(name.getText()))
                return;

            Hashtable ht = (Hashtable) main.getData().getSkillList().get
                (name.getText());
            Hashtable res = (Hashtable) ht.get(cbox.getSelectedItem());

            String resStr = (String) res.get(suc);

            // Do some magic
            resStr = resStr.replaceAll("EML",ml.getText());
            resStr = resStr.replaceAll("ML",ml.getText());
            resStr = eval(resStr);

            result.setText(resStr);
        }
        catch (Throwable t) {
            // Don't debug faulty entries, nil result
            success.setText("-");
            result.setText("-");
            roll.setText("-");
        }
    }

    /**
     * Evaluate formula. Magic:-)
     */
    //TODO: Magic Formula Evaluation Here!!!
    public String eval(String formula) {
        char[] list = formula.toCharArray();
        String remain = "";

        int res = 0;
        int current = 0;
        char op = ' ';
        for (int i = 0; i < list.length; i++) {
            if (Character.isDigit(list[i]))
                current = current * 10 +
                    Integer.parseInt(Character.toString(list[i]));
            else {
                if (i == 0) return formula;
                switch(op) {
                    case '*': res = res * current; current = 0; break;
                    case '/': res = res / current; current = 0; break;
                    case '+': res = res + current; current = 0; break;
                    case '-': res = res - current; current = 0; break;
                    case 'd': res = DiceRoller.d(res,current); current = 0; break;
                    default: res = current; current = 0; break;
                }
                switch (list[i]) {
                    case '*': case '/': case '+': case '-': case 'd':
                        op = list[i]; break;
                    default : remain = new String(list, i, list.length - i);
                }
            }
            if (remain.length() != 0) break;
        }
        if (remain.length() == 0) {
            switch(op) {
                case '*': res = res * current; break;
                case '/': res = res / current; break;
                case '+': res = res + current; break;
                case '-': res = res - current; break;
                case 'd': res = DiceRoller.d(res,current); break;
                default: res = current; current = 0; break;
            }
        }
        return ((int)res) + remain;
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
     * Class that does the dirty thing.
     */
    class MyDocListener implements DocumentListener {
        /** back reference to state */
        private State.Section section;
        /** idx in section fields */
        private int idx;
        /** Gui line */
        private GuiLine line;
        /**
         * Constructor.
         * @param aSection back refernce to state
         * @param anIdx index in section fields
         */
        MyDocListener(State.Section aSection, int anIdx, GuiLine aLine) {
            section = aSection;
            idx = anIdx;
            line = aLine;
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
         * Play field to state and dirty.
         */
        private void playBack(DocumentEvent e) {
            Document doc = e.getDocument();
            try {
                String txt = doc.getText(0, doc.getLength());
                main.getState().setDirty(true);
                switch (idx) {
                    case 0 : section.name = txt; testCombo(txt,line); break;
                    case 1 : section.ml = txt; break;
                    case 2 : section.d = txt; break;
                    case 4 : section.adj1 = txt; break;
                    case 5 : section.adj2 = txt; break;
                    case 6 : section.adj3 = txt; break;
                }
                calc();
            }
            catch (BadLocationException ex) {
                // Can't happen
            }
        }
    }

    /**
     * Class that does the rolling.
     */
    class MyActionListener implements ActionListener {
        /** GUI line reference */
        GuiLine line;
        /**
         * Constructor.
         * @param anIdx index in panel field
         */
        MyActionListener(GuiLine aLine) { line = aLine; }
        /**
         * IF method.
         * @param e event
         */
        public void actionPerformed(ActionEvent e) {
            // Get Dice
            String[] dice = line.d.getText().split("d");
            int pre = Integer.parseInt(dice[0]);
            int post = Integer.parseInt(dice[1]);

            // Roll
            if (e.getSource() != line.cbox)
                line.rand = DiceRoller.d(pre, post);

            // Recalc
            calc();
        }
    }
}
