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
import java.awt.image.BufferedImage;

/**
 * Object that represents weather. Weather may be restricted to maps, certain
 * locations and time, but need not be. Only one can be active at a given
 * time. (Non-active weather may be kept.) If and only if a change in the
 * results a method below would yield occurs, announce this object. If a group
 * is to be scheduled for different times and maps, this must be realized with
 * a subclass of this
 * @author Michael Jung
 */
public abstract class Weather implements IExport {
    /** type constant */
    public final static String TYPE = "Weather";

    /**
     * Final implementation. subclasses are differentiated via version.
     * @return type attribute
     */
    public final String getType() { return TYPE; }

    /**
     * This is the first version of this interface.
     * @return version (1.0)
     */
    public double version() { return 1.2; }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * Only one weather object may be active at a given time. It is used by
     * plugins which will handle only one weather at a time or need to
     * designate special weather. This is the normal case.
     * @return true if the one active weather
     */
    public abstract boolean isActive();

    /**
     * Gives a weather condition. This may be an arbitrary string but should
     * be considered carefully as other plugins may react to it. E.g. if it is
     * "muddy", a travel plugin might choose a different movement rate. If
     * nothing special can be said, an empty string may be returned. A null
     * may be returned if the weather object is not responsible for this
     * location/time. A weather plugin should make sure there are objects
     * available for every reasonable location/time.
     * @param map map name
     * @param x coordinate on map
     * @param y coordinate on map
     * @param date time for weather
     * @return condition as string
     */
    public abstract String condition(String map, int x, int y, long date);

    /**
     * Gets a weather event. This is different from a condition.  This may be
     * "rain", while the condition might be muddy (or not).  The same caution
     * that applies to conditions should be applied to events. A null may be
     * returned if there is no event or the weather object is not responsible
     * for this location/time. A weather plugin should make sure there are
     * objects available for every reasonable location/time.
     * @param map map name
     * @param x coordinate on map
     * @param y coordinate on map
     * @param date time for weather
     * @return event as string
     */
    public abstract String event(String map, int x, int y, long date);

    /**
     * Provide a weather export in image form. The provider decides size and
     * contents of image.
     * @return image
     */
    public abstract BufferedImage getExport();
}
