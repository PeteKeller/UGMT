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
package harn.index;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.Position;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import rpg.Framework;
import harn.repository.*;
import javax.swing.tree.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import rpg.*;

/**
 * Class that represents the loaded locations
 * @author Michael Jung
 */
class MyLocation extends Location {
    /** Main reference */
    Main main;

    /** name of location */
    String name;

    /** list of maps this is on */
    Hashtable maps;

    /** is this object valid */
    boolean valid;

    /** Locatable flag, only true shortly */
    boolean locatable;

    /**
     * Constructor
     * @param name name
     * @param aList list
     */
    MyLocation(Main aMain, String aName, Hashtable aList) {
        main = aMain;
        name = aName;
        maps = aList;
        valid = true;
        locatable = false;
        main.myFrw.announce(this);
    }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public String getName() { return name; }

    /**
     * Several locations may be active at a given time. Returns the state
     * of this location. A location must be valid to be active.
     * @return true if an active location
     */
    public boolean isActive() { return false; }

    /**
     * Several locations may be valid at a given time. Returns the state
     * of this location. A valid location is a location that the plugin
     * knows about.  In abstract terms, an invalid object was released to
     * garbage collection by the implementing plugin.
     * @return true if a valid location
     */
    public boolean isValid() { return valid; }

    /**
     * Locate this object on a map, if a map plugin is listening.
     */
    public void locate() {
        locatable = true;
        main.myFrw.announce(this);
        locatable = false;
        main.myFrw.announce(this);
    }

    /**
     * Test whether a given location shall be located on a map. This is
     * useful for a mapping plugin that wants to ensure a location to be
     * visible.
     * @return whether to locate this object
     */
    public boolean getLocatable() {
        return locatable;
    }

    /**
     * For this object return the set of shapes on the given map. This
     * list may be empty or null.
     * @return list of shapes on the map
     */
    public Shape[] getShapes(String map) {
        if (maps == null) return null;
        ArrayList list = (ArrayList) maps.get(map);
        if (list == null) return null;

        Shape[] shapes = new Shape[list.size()];
        for (int i = 0; i < shapes.length; i++)
            shapes[i] = (Shape) list.get(i);
        return shapes;
    }

    /**
     * Use this method to add a map/shape combination to a location.
     * @param map name of map
     * @param shape shape to add
     */
    public void addShape(String map, Shape shape) {
        if (main.editor == null || main.editor.editLoc != this) return;
        Editor.EditLocation el = (Editor.EditLocation) this;
        main.myData.renameShape
            (el.current, el.map, map, shape, el.idx, el.entry, el.index);
    }

    /**
     * The location is turned. We need to run through the whole sequence,
     * so it is not sufficient to just set the flag (and announce).
     */
    public void setActive(boolean myActive) {}

    /**
     * Inform that the state has changed.
     */
    public void inform() {
        if (!valid) return;
        main.myFrw.announce(this);
    }

    /**
     * The location is selected.
     */
    public void setSelected() {
        if (!valid) return;
        TreeModel tree = main.getIndexModel();
        for (int i = 0; i < tree.getChildCount(tree.getRoot()); i++) {
            Object child = tree.getChild(tree.getRoot(), i);
            for (int j = 0; j < tree.getChildCount(child); j++) {
                Object grandchild = tree.getChild(child, j);
                for (int k = 0; k < tree.getChildCount(grandchild); k++) {
                    Object o = tree.getChild(grandchild, k);
                    if (name.equals
                        (child.toString() + "/" + grandchild.toString() +
                         "/" + o.toString())) {
                        main.getIndex().setSelectionPath
                            (new TreePath(new Object[] { tree.getRoot(), child, grandchild, o }));
                        main.myFrw.announce(this);
                        main.myFrw.raisePlugin("Index");
                        return;
                    }
                }
            }
        }
    }

    /**
     * Test, whether the location is selected. (This method only works for
     * valid locations.)
     * @return selection state
     */
    public boolean getSelected() {
        if (!valid) return false;
        if (main.getIndex().getLastSelectedPathComponent() == null)
            return false;
        return name.equals(main.getIndex().getLastSelectedPathComponent().toString());
    }

    /**
     * Sets the active flag and announce this object in case of a change.
     * @param active new active flag
     */
    public void setInvalid() {
        valid = false;
        main.myFrw.announce(this);
    }

    /**
     * Return the full page as string. Usually a HTML page.
     * @return HTML page
     */
    public String getPage() {
        StringBuffer ret = new StringBuffer();
        String res = null;
        try {
            String fn = main.myData.getFile(name);
            String lnk = "";
            int idx = fn.indexOf("#");
            if (idx > 0) {
                lnk = fn.substring(idx + 1);
                fn = fn.substring(0, idx);
            }
            BufferedReader br = new BufferedReader
                (new FileReader(main.myPath + fn));
            String str;
            while ((str = br.readLine()) != null)
                ret.append(str + " ");
            br.close();
            if (idx > 0) {
                res = ret.substring(ret.indexOf("<a name=\""+lnk+"\""));
                int ei = res.substring(1).indexOf("<a name=");
                if (ei > 0)
                    res = "<html>" + res.substring(0, ei+1);
            }
            else
                res = ret.toString();
        }
        catch (Exception e) {
            // Debug
            e.printStackTrace();
        }
        return res;
    }
}
