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
 * Object that represents a character group. A group travels together and is
 * considered a unit on a map in time. Only one can be active at a given
 * time. If and only if a change in the results a method below would yield
 * occurs, announce this object. If a group is to be scheduled for different
 * times and maps, this must be realized with a subclass of this.
 * @author Michael Jung
 */
public abstract class CharGroup implements IExport {
    /** type constant */
    public final static String TYPE = "CharGroup";

    /**
     * Final implementation. subclasses are differentiated viy version.
     * @return type attribute
     */
    public final String getType() { return TYPE; }

    /**
     * This is the first version of this interface.
     * @return version (1.0)
     */
    public double version() { return 1.1; }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * Only one group may be active at a given time. It is used by plugins
     * which can handle only one group at a time or need to designate a
     * special group.
     * @return true if the one active group
     */
    public abstract boolean isActive();
    
    /**
     * Called by plugins that move a group. The map attribute may not be
     * meaningful to a provider, but a location only makes sense in that
     * reference frame. The time may also changed. All changes are combined to
     * avoid redundant announcements.<br> If only partial location information
     * is set, use the getters below for the unchanged parts.
     * @param map the map identifier for the location
     * @param x map coordinate
     * @param y map coordinate
     */
    public abstract void setLocation(String map, int x, int y, long date);

    /**
     * Return current map. If the group could technically be located on
     * several maps, choose one but be consistent.
     * @return map identifier
     */
    public abstract String getMap();

    /**
     * Return the current x coordinate on the map the group is located on.
     * @return x coordinate
     */
    public abstract int getX();

    /**
     * Return the current y coordinate on the map the group is located on.
     * @return y coordinate
     */
    public abstract int getY();

    /**
     * Gets the date of the current group belonging to the location given.
     * @return current date of group
     */
    public abstract long getDate();    

    /**
     * Gets the travel modes the group currently uses. A travel plugin must
     * know these and will probably take the minimum of those available. In
     * case a providing plugin doesn't have a clue as to what is expected
     * here, it may provide a km/h fouble instead. A requiring plugin may use
     * the number instead, if no matching modes are found
     * @return mode list
     */
    public abstract String[] getTravelModes();

    /**
     * Get the inherent speed of the group in km/h (leagues/watch). This is
     * generally the minimum of the group members and proportional to the
     * Agility. Sometimes the group speed is not adequately covered by terrain
     * and vegetation, mostly tactical environs. This speed can be used
     * instead.
     * @return tactical speed of group
     */
    public abstract float getSpeed();

    /**
     * Add a member to the group. It is assumed that the name is
     * unique.
     * @param name name of the member.
     */
    public abstract void addMember(String name);

    /**
     * Remove a member from the group. It is assumed that the name is
     * unique.
     * @param name name of the member.
     */
    public abstract void removeMember(String name);
}
