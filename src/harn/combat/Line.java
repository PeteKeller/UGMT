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
import rpg.*;
import javax.swing.*;
import java.awt.*;
import org.w3c.dom.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

/**
 * Base Class for a all types of GUI lines.
 */
abstract class Line {
    /** Main reference */
    protected Main main;

    /** Used */
    protected JCheckBox used;

    /** Constructor */
    Line(Main aMain, boolean aUsed) {
        main = aMain;

        // Check box
        used = new JCheckBox();
        used.setSelected(aUsed);
        used.setBackground(Color.WHITE);
        used.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (((e.getModifiers() & e.SHIFT_MASK) != 0) && !used.isSelected())
                        // This can happen only for the selected!
                        main.getState().removeLine
                            (main.tree.getLastSelectedPathComponent().toString(), Line.this);
                    main.getState().setDirty(true);
                    main.armour.recalc();
                }
            });
        used.setToolTipText("<shift>-clicking removes permanently");
    }

    /** Get used */
    public boolean used() { return used.isSelected(); }

    /** Get used field */
    public JComponent usedF() { return used; }

    public abstract JComponent[] getFields();
}
