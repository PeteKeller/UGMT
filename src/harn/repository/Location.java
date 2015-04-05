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
import java.awt.Shape;

/**
 * Object that represents a Location on a map or several maps. Such items are
 * announced as they are turned active or inactive or come into existence.
 * Receiving plugins might display these locations. Invalid locations should
 * not be used. A location's active or selected state need not have a
 * relation.
 * @author Michael Jung
 */
public abstract class Location implements IExport {
    /** type constant */
    public final static String TYPE = "Location";

    /**
     * Final implementation. subclasses are differentiated viy version.
     * @return type attribute
     */
    public final String getType() { return TYPE; }

    /**
     * This is the first version of this interface.
     * @return version (1.0)
     */
    public double version() { return 1.3; }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * Several locations may be active at a given time. Returns the state of
     * this location. A location must be valid to be active.
     * @return true if an active location
     */
    public abstract boolean isActive();

    /**
     * The location is turned active/inactive.  (This method only works for
     * valid locations.)
     * @param activate or deactivate
     */
    public abstract void setActive(boolean active);

    /**
     * The location is selected. (This method only works for valid locations.)
     * Note that you cannot unselect a location.
     */
    public abstract void setSelected();

    /**
     * Test, whether the location is selected. (This method only works for
     * valid locations.)
     * @return selection state
     */
    public abstract boolean getSelected();

    /**
     * Test whether a given location shall be located on a map. This is useful
     * for a mapping plugin that wants to ensure a location to be visible.
     * @return whether to locate this object
     */
    public abstract boolean getLocatable();

    /**
     * Several locations may be valid at a given time. Returns the state of
     * this location. A valid location is a location that the plugin knows
     * about.  In abstract terms, an invalid object was released to garbage
     * collection by the implementing plugin.
     * @return true if a valid location
     */
    public abstract boolean isValid();

    /**
     * For this object return the set of shapes on the given map. The union of
     * these shapes depict the location on the map for this object. This list
     * may be empty or null.
     * @return list of polygons on the map
     */
    public abstract Shape[] getShapes(String map);

    /**
     * Use this method to add a map/shape combination to a location.
     * @param map name of map
     * @param shape shape to add
     */
    public abstract void addShape(String map, Shape shape);

    /**
     * Return the full page as string. Usually a HTML page.
     * @return HTML page
     */
    public abstract String getPage();

    /**
     * Returns the fact that this location is used for editing. Defaults to
     * false.
     */
    public boolean isEditing() { return false; }
}
