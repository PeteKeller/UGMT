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
 * Class for note display. Any changes here wil be reflected at the
 * state. Determination (load) is also handled here.
 * @author Michael Jung
 */
class CharNote {
    /** Name of note */
    private JTextField name;

    /** Value of note */
    private JTextField value;

    /** Back reference */
    private Main main;

    /** State reference */
    private Section section;

    /** Constructor */
    public CharNote(Main aMain, int ew, int idx, NamedNodeMap cattr, JPanel panel, Section sect) {
        super();
        main = aMain;
        section = sect;

        GridBagConstraints constr = new GridBagConstraints();
        constr.gridx = 0;
        constr.gridy = idx;
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.weightx = 1;
        constr.insets = new Insets(0,0,0,0);

        JPanel cpanel  = new JPanel(new GridBagLayout());
        panel.add(cpanel, constr);
        int h = main.bSave.getPreferredSize().height;

        constr.insets = new Insets(3,3,3,3);
        constr.gridy = 0;
        constr.weightx = 0;

        name = new JTextField();
        name.setPreferredSize(new Dimension(ew, h));
        name.setEnabled(false);
        name.setText(cattr.getNamedItem("name").getNodeValue());
        cpanel.add(name, constr);

        constr.gridx = 1;
        constr.fill = GridBagConstraints.HORIZONTAL;
        constr.weightx = 1;

        value = new JTextField();
        value.setPreferredSize(new Dimension(0, h));
        cpanel.add(value, constr);

        value.getDocument().addDocumentListener(new DocumentListener() {
                public void removeUpdate(DocumentEvent e) { set(); }
                public void insertUpdate(DocumentEvent e) { set(); }
                public void changedUpdate(DocumentEvent e) { set(); }
                private void set() {
                    main.getState().setNote(name.getText(), value.getText());
                    main.getState().setDirty(State.edit);
                }
            });
    }

    /** Method to clear all notes */
    public void clear() {
        if (name != null) name.setText("");
        if (value != null) value.setText("");
    }

    /** Get name */
    public String getName() { return name.getText(); }

    /** Set Note */
    public void set() {
        String strvalue = main.getState().getNote(name.getText());
        if (value != null) value.setText(strvalue);
    }
}
