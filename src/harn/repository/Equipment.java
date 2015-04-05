/*
 * UGMT : Unversal Gamemaster tool
 * Copyright (c) 2004 Michael Jung
 *
 * Any party obtaining a copy of these files is granted, free of charge, a
 * full and unrestricted irrevocable, world-wide, paid up, royalty-free,
 * nonexclusive right and license to deal in this software and
 * documentation files (the "Software"), including without limitation the
 * rights to use, copy, modify, merge, publish and/or distribute copies of
 * the Software, and to permit persons who receive copies from any such 
 * party to do so, with the only requirement being that this copyright 
 * notice remain intact.
 */
package harn.repository;

import rpg.IExport;

/**
 * This interface is implemented by equipment items. All equipment is
 * organized in a tree, where the roots have no father. The first level
 * objects are characters and groups and the like, each having a unique
 * name. Any lower level object will then be either a container of a basic
 * item. Note that items need not be physical, they can represent ideal
 * groupings, i.e. "daily" worn items or "combat" equipment. The sheer amount
 * of these objects does not allow to announce all these objects. It is
 * therefore recommended to announce these objects only when they change. At
 * startup the first level object should be announced only.
 * @author Michael Jung
 */
public abstract class Equipment implements IExport {
    /** type constant */
    public final static String TYPE = "Equipment";

    /**
     * Final implementation. subclasses are differentiated via version.
     * @return type attribute
     */
    public final String getType() { return TYPE; }

    /**
     * This is the first version of this interface.
     * @return version (1.1)
     */
    public double version() { return 1.1; }

    /**
     * Implement as speciied in superclass.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * Return the unique father (equipment) object.
     * @return father object (/null for root)
     */
    public abstract Equipment getFather();

    /**
     * Return the subitems this equipment "contains". This may be abstract
     * containment.
     * @return child equipment
     */
    public abstract Equipment[] getChildren();

    /**
     * Add a child. This addition will be made the responsibility of the
     * component that own the father item.
     * @param child new child to add.
     */
    public abstract void addChild(String child);

    /**
     * Return the weight of the object in (Harnic) pounds. A negative weight
     * specifies an unknown weight, a 0 is possible. For containers, this
     * includes all contained items.
     */
    public abstract double getWeight();

    /**
     * Returns the state of this equipment. A valid equipment is equipment
     * that the plugin knows about.  In abstract terms, an invalid object was
     * released to garbage collection by the implementing plugin.
     * @return true if a valid equipment
     */
    public abstract boolean isValid();
}
