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
package harn.export;

import rpg.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.w3c.dom.*;

/**
 * Class that contains the GUI references to a particular export, which needs
 * to be queried to determine output. Also does the exporting itself.
 * @author Michael Jung
 */
abstract class HttpExport implements IListener {
    /** Name of export */
    private String name;

    /** Name of export */
    private String desc;

    /** Active or not */
    private JCheckBox button;

    /** main reference */
    protected Main main;

    /** Gui constraints */
    protected GridBagConstraints constrBox;

    /** Constructor */
    protected HttpExport(Main aMain, String aName, String aDesc, JPanel panel, int gridy) {
        main = aMain;
        name = aName;
        desc = aDesc;
        constrBox = new GridBagConstraints();
        constrBox.fill = GridBagConstraints.HORIZONTAL;
        constrBox.weighty = 0;
        constrBox.gridy = gridy;
        constrBox.insets = new Insets(1, 3, 1, 3);

        // Export or not
        button = new JCheckBox(aName);
        button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    main.bSave.setEnabled(true);
                }
            });

        // Description
        JLabel ef = new JLabel(desc);

        // Add to GUI
        constrBox.gridx = 0;
        panel.add(button, constrBox);
        constrBox.gridx = 1;
        panel.add(ef, constrBox);
    }

    /**
     * The name for which the exporter queries  
     */
    final public String name() { return name; }

    /**
     * Called by the exporting Background thread upon a request.
     * @param query queried export
     * @param os output stream for direct access
     * @param osr output writer for writing
     */
    public abstract void export(String query, OutputStream os, PrintWriter osr) throws IOException;

    /**
     * Called by the main class to register this object with the framework.
     */
    public abstract void listen();

    /**
     * Called by the exporting Background thread to determine, whether request
     * is handled by this plugin.
     * @param query queried export
     * @return true if this class handles the export
     */
    final public boolean choose(String query) {
        return (query.indexOf(name()) == 0 && button.isSelected());
    }

    /**
     * Select (through state import)
     */
    final public void select(boolean select) {
        button.setSelected(select);
        button.revalidate();
        button.repaint();
    }

    /** Save the export in XML document */
    public void save(Element child) {
        if (button.isSelected())
            child.setAttribute(name(), "true");
    }

    /** Load the export from XML document */
    public void load(NamedNodeMap cattr) {
        select(cattr.getNamedItem(name()) != null);
    }

    /**
     * Select (through state import)
     */
    final public boolean isSelected() { return button.isSelected(); }
}
