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
import javax.swing.ImageIcon;

/**
 * Object that represents an event. Such items are announced as they are
 * turned active or inactive. They may also be announced at other instances,
 * e.g. if their description changes.  Receiving plugins can take action upon
 * these events. Events are not programmatic events but events that occur as a
 * given group passes through time.
 * @author Michael Jung
 */
public abstract class TimeEvent implements IExport {
    /** type constant */
    public final static String TYPE = "Event";

    /**
     * Final implementation. subclasses are differentiated via version.
     * @return type attribute
     */
    public final String getType() { return TYPE; }

    /**
     * This is the first version of this interface.
     * @return version (1.0)
     */
    public double version() { return 1.0; }

    /**
     * Implement as specified in superclass. This is also the full description
     * of the event.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * Several events may be active at a given time. Returns the state of
     * this event.
     * @return true if an active event
     */
    public abstract boolean isActive();

    /**
     * Returns a general information string. Useful for grouping types of
     * events. E.g. "holiday".
     * @return textual description of event
     */
    public abstract String type();

    /**
     * Check whether this is event is active for the given time interval.
     * Returns the array containing the relative starting and end points
     * (i.e. within [0,1]) in the interval [from, to] or null, if no
     * intersection is present. If from = to, and the intersection is
     * non-empty return the array {0,1}.
     * @param from start of interval
     * @param to end of interval
     * @return start and end.
     */
    public abstract float[] isActive(long from, long to);

    /**
     * This method is called, when the user asks for details of a specific
     * event. Such details may be displayed by the calling plugin.
     * @return displayed string.
     */
    public abstract String details();

    /**
     * Several events may be valid at a given time. Returns the state of this
     * event. A valid event is an event that the plugin knows about.  In
     * abstract terms, an invalid object was released to garbage collection by
     * the implementing plugin.
     * @return true if a valid event
     */
    public abstract boolean isValid();

    /**
     * Get a small icon for this event. Can be used for display, but the
     * displaying plugin must be aware of null pointers, in case an event does
     * not have an icon.
     */
    public abstract ImageIcon getIcon();
}
