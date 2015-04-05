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
package harn.sketch;

import javax.swing.tree.*;
import javax.swing.event.*;
import java.io.*;
import java.beans.*;
import javax.imageio.*;
import javax.swing.*;
import rpg.Framework;
import harn.repository.Sketch;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.net.*;

/**
 * Class to export the sketch.  Object that represents a sketch or a
 * note. It may either be a directory or contain (graphical) content. Note
 * that a directory strucure is reachable by publishing the root
 * element. It is not necessary to export all.
 */
class PSketch extends Sketch {
    boolean valid = true;

    String name;

    boolean export = false;

    TreeNode father;

    /** Constructor */
    PSketch(TreeNode aFather, String aName) {
        father = aFather;
        name = aName;
    }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public String getName() { return name; }

    /**
     * Returns the validatiy of a sketch. I.e. a sketch may have been
     * removed and becomes invalid. This is announced.
     * @return validity of sketch
     */
    public boolean isValid() { return valid; }

    /**
     * Return a list of subsketches. If this is non-null, the image is
     * null.
     * @return sub-sketch list
     */
    public Sketch[] getSubSketches() {
        if (father.dir == null) return null;
        int i = 0;
        Enumeration list = father.children();
        while (list.hasMoreElements()) {
            list.nextElement();
            i++;
        }
        Sketch[] ret = new Sketch[i];
        i = 0;
        list = father.children();
        while (list.hasMoreElements())
            ret[i++] = ((TreeNode)list.nextElement()).sketch;
        return ret;
    }

    /**
     * Return the content of this sketch. If this is non-null, subsketches
     * are null.
     * @return content image
     */
    public File getContent() {
        if (father.dir != null) return null;

        int w = father.main.myDrawing.width;
        int h = father.main.myDrawing.height;
        if (father.html != null) {
            w = father.main.myDrawingHtml.getPreferredSize().width;
            h = father.main.myDrawingHtml.getPreferredSize().height;
        }

        try {
            if (father.png != null) return father.png;
            if (father.txt != null) return father.txt;
            if (father.html != null) return father.html;
        }
        catch (Exception e) {
            // Debug
            e.printStackTrace();
        }
        return null;
    }

    /**
     * If exported, this may be seen by players. It may be published
     * externally.
     * @return exportable or not
     */
    public boolean isExported() { return export; }

    /**
     * The sketch is selected. (This method only works for valid sketches.)
     * Note that you cannot unselect a sketch.
     */
    public void setSelected() {
        father.main.guitree.addSelectionPath(new TreePath(father.getPath()));
        father.main.myFrw.raisePlugin("Sketch");
    }
}
