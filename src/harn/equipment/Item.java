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
package harn.equipment;

import harn.repository.Equipment;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.tree.*;
import org.w3c.dom.*;
import rpg.SortedTreeModel;

/**
 * Item class, represents data and Gui
 * @author Michael Jung
 */
class Item extends DefaultMutableTreeNode implements rpg.DNDTree.DragNode {
    /** List of all root objects */
    private static HashSet all = new HashSet();

    /** Main reference */
    private Main main;

    /** Equipment reference - exported object */
    protected MyEquipment equipment;

    /** (Material) Prefix */
    private String prefix;

    /** Noderef */
    private NamedNodeMap myRef;

    /**
     * Find a root group/char by name.
     * @param name of group/char
     * @return whether found or not
     */
    public static boolean find(String name) { return all.contains(name); }

    /**
     * Is the itema container
     */
    public boolean isLeaf() {
        if (getChildCount() != 0 ||
            getParent() == null ||
            getParent().getParent() == null)
            return false;
        return true;
    }

    /**
     * Create a new item.
     */
    public static void create(boolean startup, String aName, Node child, Main aMain, SortedTreeModel aModel, DefaultMutableTreeNode father) {
        all.add(aName);
        new Item(startup, aName, aMain, child, aModel, father);
    }

    /** Clone this item */
    public Item cloneItem (SortedTreeModel model, DefaultMutableTreeNode parent, boolean cloneChildren) {
        Item opitem = (Item)getParent();

        // Recalc name
        String add = "";
        if (opitem.prefix != null)
            add = opitem.getUserObject().toString() + opitem.prefix;

        Item item = new Item(this, add);
        model.insertNodeSorted(item, parent);

        if (cloneChildren) {
            Enumeration list = children();
            while (list.hasMoreElements()) {
                Item child = (Item)list.nextElement();
                model.insertNodeSorted(child.cloneItem(model, this, true), item);
            }
        }
        new Exception().printStackTrace();

        // As last action we calculate weight of the parent. Children have
        // already been added in at this point.
        if (parent instanceof Item)
            ((Item)item.getParent()).recalcWeight();

        return item;
    }

    /** Clone constructor */
    private Item(Item item, String add) {
        main = item.main;
        if (item.myRef == null) {
            setUserObject(add + item.getName());
            equipment = new MyEquipment(this, item.equipment.selfweight);
            return;
        }
        setUserObject(add + item.myRef.getNamedItem("name").getNodeValue());
        double weight = -1;
        if (item.myRef != null) {
            Node node = item.myRef.getNamedItem("weight");
            if (node != null)
                weight = main.data.calcWeight(node.getNodeValue());
        }
        equipment = new MyEquipment(this, weight);

        // Announcement handled elsewhere
    }

    /** Main constructor */
    public Item(boolean startup, String aName, Main aMain, Node child, SortedTreeModel aModel, DefaultMutableTreeNode father) {
        super();
        main = aMain;
        String name = aName;

        // Price and weight
        double weight = -1;
        if (child != null) {
            myRef = child.getAttributes();
            if (myRef != null) {
                Node price = myRef.getNamedItem("price");
                Node weightn = myRef.getNamedItem("weight");
                if (aModel == main.dataModel) {
                    if (price != null || weightn != null) name += " (";
                    if (price != null) name += price.getNodeValue();
                    if (price != null && weightn != null) name += "/";
                    if (weightn != null) {
                        name += weightn.getNodeValue();
                    }
                    if (price != null || weightn != null) name += ")";
                }
                if (weightn != null)
                    weight = Data.calcWeight(weightn.getNodeValue());
                Node node = myRef.getNamedItem("prefix");
                if (node != null)
                    prefix = node.getNodeValue();
            }
        }
        equipment = new MyEquipment(this, weight);
        setUserObject(name);

        if (aModel != null) aModel.insertNodeSorted(this, father);

        NodeList children = null;
        if (child != null) children = child.getChildNodes();

        // Add children
        for (int j = 0; children != null && j < children.getLength(); j++) {
            NamedNodeMap cattr = children.item(j).getAttributes();
            if (cattr != null) {
                Item sub = new Item
                    (startup, cattr.getNamedItem("name").getNodeValue(), main, children.item(j), aModel, this);
            }
        }

        // We calculate weight of the parent. Children have already been added
        // in at this point.
        if (father != null && father instanceof Item)
            ((Item)father).recalcWeight();

        // We announce this after children are created. In that case all
        // children are accessible when they get announced.  We don't announce
        // the templates and at startup only roots.
        if (aModel != main.dataModel && !(startup && father instanceof Item))
            main.myFrw.announce(equipment);
    }

    /** Recalc weight from all children */
    public void recalcWeight() {
        if (equipment.selfweight < 0) return;
        equipment.totalweight = equipment.selfweight;
        for (int j = 0; j < getChildCount(); j++) {
            double weight = ((Item)getChildAt(j)).equipment.getWeight();
            if (weight > 0) equipment.totalweight += weight;
        }
    }

    /**
     * Get name.
     */
    public String getName() { return getUserObject().toString(); }

    /**
     * Set weight.
     */
    public void setWeight(String ws) {
        double w = -1;
        try {
            if (getParent() instanceof Item) {
                equipment.selfweight = Double.parseDouble(ws);
                ((Item)getParent()).recalcWeight();
            }
        }
        catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Get self weight.
     */
    public String getSelfWeight() {
        if (equipment.selfweight < 0)
            return null;
        String ret = Double.toString(equipment.selfweight);
        return ret.substring(0, Math.min(ret.length(), 10)) + "lbs";
    }

    /**
     * Exported class
     */
    class MyEquipment extends Equipment {
        /** reference to Item */
        private Item item;
        /** total weight */
        private double totalweight;
        /** self weight */
        private double selfweight;
        /** Validity */
        private boolean valid;
        /** Constructor */
        public MyEquipment(Item anItem, double aWeight) {
            item = anItem; totalweight = selfweight = aWeight; valid = true;
        }
        /**
         * Implement as specified in superclass.
         * @return name attribute
         */
        public String getName() { return item.getName(); }
        /**
         * Return the unique father (equipment) object.
         * @return father object (/null for root)
         */
        public Equipment getFather() {
            if (item.getParent() != null && item.getParent() instanceof Item)
                return ((Item)item.getParent()).equipment;
            else
                return null;
        }
        /**
         * Return the subitems this equipment "contains". This may be abstract
         * containment.
         * @return child equipment
         */
        public Equipment[] getChildren() {
            Equipment[] ret = new Equipment[item.getChildCount()];
            for (int i = 0; i < ret.length; i++)
                ret[i] = ((Item)item.getChildAt(i)).equipment;
            return ret;
        }
        /**
         * Add a child. This addition will be made the responsibility of the
         * component that own the father item.
         * @param child new child to add.
         */
        public void addChild(String child) {
            TreeModel model = null;
            if (getRoot().equals(main.treetable.getTree().getModel()))
                model = main.treetable.getTree().getModel();
            else
                model = main.remainder.getTree().getModel();
            new Item(false, child, main, null, (SortedTreeModel)model, item);
        }
        /**
         * Return the weight of the object in (Harnic) pounds.  A negative
         * weight specifies an unknown weight, a 0 is possible.
         */
        public double getWeight() { return totalweight; }
        /** Validity */
        public boolean isValid() { return valid; }
        /** Validity */
        public void setInvalid() { valid = false; }
    }

    /** dummy */
    public void update(int c_row) {}
}
