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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

/**
 * Class for an armour line on the GUI and representing a piece of
 * armour.
 */
class ArmourLine extends Line {
    /** name */
    private JTextField name;

    /** material */
    private JTextField material;

    /** Quality */
    private JComponent quality;
    
    /** Constructor */
    ArmourLine(Main aMain, String aName, String aMaterial, String aQuality, boolean aUsed) {
        super(aMain, aUsed);

        // Name
        name = new JTextField(aName);
        name.getDocument().addDocumentListener(new MyDocListener());
        ArrayList al = main.getData().getArmours();
        registerPopup(name, al);

        // Material
        material = new JTextField(aMaterial);
        material.getDocument().addDocumentListener(new MyDocListener());
        al = main.getData().getMaterials();
        registerPopup(material, al);

        // Quality
        quality =
            new JComboBox(Data.qualities);
        if (aQuality.length() < 4) {
            ((JComboBox)quality).setSelectedItem(aQuality);
            ((JComboBox)quality).addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        main.armour.recalc();
                        main.getState().setDirty(true);
                    }
                });
        }
        else {
            Dimension d = quality.getPreferredSize();
            quality =
                new JTextField(aQuality.toUpperCase());
            ((JTextField)quality).setEditable(false);
            quality.setPreferredSize(d);
        }
    }
    
    /** IF from Line */
    public JComponent[] getFields() {
        return new JComponent[] { itemF(), materialF(), qualityF() };
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

    /** Get weight */
    public double weight() { return main.getData().getWeight(item(), material()); }

    /** Get material */
    public String material() { return material.getText(); }

    /** Get quality */
    public String quality() {
        if (quality instanceof JComboBox)
            return (String)((JComboBox)quality).getSelectedItem();
        else
            return ((JTextField)quality).getText();
    }

    /** Get name field */
    public JComponent itemF() { return name; }

    /** Get material field*/
    public JComponent materialF() { return material; }

    /** Get quality field */
    public JComponent qualityF() { return quality; }

    /**
     * Class to listen to changes in armour items and progate to recalc.
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
                            if (used.isSelected()) main.armour.recalc();
                            t = null;
                        }
                    };
                t.start();
                main.getState().setDirty(true);
            }
        }
    }
}
