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
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

/**
 * Class for an armour line on the GUI and representing a piece of
 * armour.
 */
class WeaponLine extends Line {
    /** name of item */
    private JTextField name;

    /** Quality */
    private JComboBox quality;
    
    /** Damage */
    private JTextField damage;

    /** ML modifier */
    private JTextField mlmod;

    /** Handmodifier */
    private JTextField handmod;

    /** Quality numbers (hard-coded) */
    private String[] numbers = {
        "3", "4", "5", "6", "7", "8", "9", "10",
        "11", "12", "13", "14", "15", "16", "17", "18"
    };

    /** Weight */
    private String weight;
       
    /** Skill to use */
    String skill;

    /** Constructor */
    WeaponLine(Main aMain, String aName, String aDamage, String aQuality, String aHandmod, String anMlmod, String aSkill, String aWeight, boolean aUsed) {
        super(aMain, aUsed);

        // Name
        name = new JTextField(aName);
        name.getDocument().addDocumentListener(new MyDocListener());
        ArrayList al = main.getData().getWeapons();
        registerPopup(name, al);

        // Damage
        damage = new JTextField(aDamage);
        damage.getDocument().addDocumentListener(new MyDocListener());

        // Quality
        quality = new JComboBox(numbers);
        quality.setSelectedItem(aQuality);

        // Handmod
        handmod = new JTextField(aHandmod);
        handmod.setEditable(false);

        // ML mod
        mlmod = new JTextField(anMlmod);
        mlmod.setEditable(false);

        // Skill & weight
        skill = aSkill;
        weight = aWeight;
    }
    
    /** IF from Line */
    public JComponent[] getFields() {
        return new JComponent[] {
            itemF(), qualityF(), damageF(), handmodF(), mlmodF()
        };
    }

    /** register menu listener */
    private void registerPopup(final JTextField comp, ArrayList al) {
        final JPopupMenu menu = new JPopupMenu();
        for (int i = 0; i < al.size(); i++) {
            JMenuItem mi = new JMenuItem((String)al.get(i));
            mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        comp.setText(ev.getActionCommand());
                    }
                });
            menu.add(mi);
        }

        comp.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
    }

    /** Get name */
    public String item() { return name.getText(); }

    /** Get damage */
    public String damage() { return damage.getText(); }

    /** Get quality */
    public String quality() { return (String) quality.getSelectedItem(); }

    /** Get Hand modifier */
    public String handmod() { return handmod.getText(); }

    /** Get Hand modifier */
    public String mlmod() { return mlmod.getText(); }

    /** Get weight */
    public double weight() {
        try {
            return Double.parseDouble(weight);
        }
        catch (Exception e) {
            return 0;
        }
    }

    /** Get name */
    public JTextField itemF() { return name; }

    /** Get damage */
    public JTextField damageF() { return damage; }

    /** Get quality */
    public JComboBox qualityF() { return quality; }

    /** Get Hand modifier */
    public JTextField handmodF() { return handmod; }

    /** Get Hand modifier */
    public JTextField mlmodF() { return mlmod; }

    /** Get AML */
    public String aml(Char ch) {
        String[] mod = mlmod.getText().split("/");
        String ml = ch.getSkill("Weapon/" + skill);
        if (ml == null || ml.length() == 0) return "-";
        return Integer.toString(Integer.parseInt(ml) + Integer.parseInt(mod[0]));
    }

    /** Get DML */
    public String dml(Char ch) {
        String[] mod = mlmod.getText().split("/");
        String ml = ch.getSkill("Weapon/" + skill);
        if (ml == null || ml.length() == 0 || mod.length != 2) return "-";
        return Integer.toString(Integer.parseInt(ml) + Integer.parseInt(mod[1]));
    }

    /**
     * Class to listen to changes in weapon items and progate to recalc.
     */
    class MyDocListener implements DocumentListener {
        Thread t = null;
        long doIn = 0;
        public void removeUpdate(DocumentEvent e) { setField(e); }
        public void insertUpdate(DocumentEvent e) { setField(e); }
        public void changedUpdate(DocumentEvent e) { setField(e); }
        private void setField(DocumentEvent e) {
            // To increase GUI speed we only accept input after 100ms
            // passed.
            doIn = System.currentTimeMillis() + 100;
            if (t == null) {
                t = new Thread() {
                        public void run() {
                            while (System.currentTimeMillis() < doIn)
                                try {
                                    Thread.sleep(100);
                                }
                                catch (Exception ex) {
                                }
                            t = null;
                        }
                    };
                t.start();
                main.getState().setDirty(true);
            }
        }
    }
}
