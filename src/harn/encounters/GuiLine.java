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
package harn.encounters;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import rpg.*;
import java.util.*;
import org.w3c.dom.*;

/**
 * Class to hold gui items.
 * @author Michael Jung
 */
public class GuiLine extends JPanel {

    /** event reference */
    private EncounterEvent ev;

    /** Main reference */
    private Main main;

    /** Times */
    private JTextField from;
    private JTextField to;

    /** types */
    JComboBox types;

    /** value */
    private JTextField value;

    /** Internal setting of value */
    static boolean intern;

    /** Constructor */
    GuiLine(Main aMain, EncounterEvent anEv) {
        main = aMain;
        ev = anEv;

        setLayout(new GridBagLayout());
        GridBagConstraints constr2 = new GridBagConstraints();
        constr2.fill = GridBagConstraints.BOTH;
        constr2.weightx = 1;
        constr2.weighty = 1;

        constr2.gridx = 0;
        constr2.weightx = 0;
        from = new JTextField();
        from.setDocument(new DateDoc(true));
        from.setText(main.myState.datetime2string(ev.time[0]));
        from.setPreferredSize(Listing.tdim);
        from.setEnabled(ev.isEnabled());
        add(from, constr2);

        constr2.gridx = 1;
        constr2.weightx = 0;
        to = new JTextField();
        to.setDocument(new DateDoc(false));
        to.setText(main.myState.datetime2string(ev.time[1]));
        to.setPreferredSize(Listing.tdim);
        to.setEnabled(ev.isEnabled());
        add(to, constr2);

        JButton but = new JButton("Reroll");
        but.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    main.myState.roll
                        (ev.time[0], ev, ev.isForced(), "General");
                    // Update GUI (only)
                    intern = true;
                    String v = null;
                    String t = (String)types.getSelectedItem();
                    types.setSelectedItem(ev.getType0());
                    if (t.equals("note")) v = ev.getNote();
                    if (t.equals("index")) v = ev.getIndex();
                    if (t.equals("name")) v = ev.getName0();
                    value.setText(v != null ? v : "");
                    intern = false;
                }
            });
        but.setToolTipText("Reroll this event (from scratch)");
        constr2.gridx = 2;
        add(but, constr2);

        constr2.gridx = 3;
        types = new JComboBox(new String[] { "name", "index", "note" });
        if (main.myState != null && main.myState.act != null)
            types.setSelectedItem(ev.getType0());
        types.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // Update GUI (only)
                    intern = true;
                    String g = main.myState.act.getName();
                    String t = (String)types.getSelectedItem();
                    String v = null;
                    if (t.equals("note")) v = ev.getNote();
                    if (t.equals("index")) v = ev.getIndex();
                    if (t.equals("name")) v = ev.getName0();
                    value.setText(v != null ? v : "");
                    intern = false;
                }
            });
        add(types, constr2);

        constr2.gridx = 5;
        constr2.weightx = 1;
        value = new JTextField("");
        value.getDocument().addDocumentListener(new DocumentListener() {
                Thread t = null;
                long doIn = 0;
                DocumentEvent src;
                public void removeUpdate(DocumentEvent e) { setField(e); }
                public void insertUpdate(DocumentEvent e) { setField(e); }
                public void changedUpdate(DocumentEvent e) { setField(e); }
                private void setField(DocumentEvent e) {
                    // To increase GUI speed we only accept input after 100ms
                    // passed.
                    doIn = System.currentTimeMillis() + 100;
                    src = e;
                    if (intern) return;
                    if (t == null) {
                        t = new Thread() {
                                public void run() {
                                    while (System.currentTimeMillis() < doIn)
                                        try {
                                            Thread.sleep(100);
                                        }
                                        catch (Exception ex) {
                                        }
                                    set();
                                    t = null;
                                }
                            };
                        t.start();
                    }
                }
            });
        value.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { popup(e); }
                public void mouseReleased(MouseEvent e) { popup(e); }
                private void popup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        main.pMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        add(value, constr2);
        reset();
    }

    /**
     * Set the textfield according to field value.
     */
    private void set() {
        String t = (String) types.getSelectedItem();
        String v = (String) value.getText();
        ev.setValue(t,v);
        from.setEnabled(ev.isEnabled());
        to.setEnabled(ev.isEnabled());
    }

    /**
     * Gets active status from event.
     */
    public boolean isActive(int idx) {
        float[] res = ev.isActive(idx * Main.WATCH, (idx + 1) * Main.WATCH);
        return (res != null && res[0] != res[1]);
    }

    /**
     * Set an event as new reference.
     */
    public void setEvent(EncounterEvent anEv) {
        if (ev == anEv) return;
        ev = anEv;
        // Update GUI (only)
        intern = true;
        from.setText(main.myState.datetime2string(ev.time[0]));
        to.setText(main.myState.datetime2string(ev.time[1]));
        reset();
        intern = false;
    }

    /**
     * Set the combobox and the textfield according to event values.
     */
    public void reset() {
        // Update GUI (only)
        intern = true;
        from.setEnabled(ev.isEnabled());
        to.setEnabled(ev.isEnabled());
        String t = ev.getType0();
        String v = null;
        if (t.equals("note")) v = ev.getNote();
        if (t.equals("index")) v = ev.getIndex();
        if (t.equals("name")) v = ev.getName0();
        types.setSelectedItem(t);
        value.setText(v != null ? v : "");
        intern = false;
    }

    /**
     * Validating dates text field.
     */
    class DateDoc extends PlainDocument {
        boolean from;
        DateDoc(boolean aFrom) {
            from = aFrom;
        }
        public void insertString(int off, String str, AttributeSet a) {
            try {
                check(off, str, off);
                super.insertString(off, str, a);
            }
            catch (Exception e) {
                // Don't allow
            }
        }
        public void remove(int off, int len) {
            try {
                check(off, "", off + len);
                super.remove(off, len);
            }
            catch (Exception e) {
                // Don't allow
            }
        }
 	public void replace(int off, int len, String str, AttributeSet a) {
            try {
                check(off, str, off + len);
                super.replace(off, len, str, a);
            }
            catch (Exception e) {
                // Don't allow
            }
        }
        private void check(int off1, String str, int off2) throws Exception {
            if (intern) return;
            String test =
                getText(0, off1) + str + getText(off2, getLength() - off2);
            long t = main.myState.cal.string2datetime(test);
            if (t < 0) throw new Exception("t < 0");
            if (from) ev.setTime(t,ev.time[1]); else ev.setTime(ev.time[0],t);
        }
    }
}
