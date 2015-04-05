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
package harn.magic;

import harn.repository.Char;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.awt.datatransfer.*;
import javax.swing.tree.*;
import rpg.SortedTreeModel;

/**
 * Spell holder class, represents data and GUI.
 * @author Michael Jung
 */
class Holder extends DefaultMutableTreeNode {
    /** Main reference */
    private Main main;

    /** Character ref */
    Char mychar;

    /** Main constructor */
    Holder(Char aChar, String name, Main aMain) {
        super(name);
        main = aMain;
        mychar = aChar;
        main.getModel().insertNodeSorted
            (this, (DefaultMutableTreeNode)main.getModel().getRoot());
    }

    /** IF method */
    public boolean isLeaf() { return false; }
}
