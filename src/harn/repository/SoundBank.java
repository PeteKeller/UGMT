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
 * Object that represents a sound system, where triggers can be
 * registered. Calling add<something> will cfeate a trigger in the sound
 * plugin that must be filled with sound specific details there.
 * @author Michael Jung
 */
public abstract class SoundBank implements IExport {
    /** type constant */
    public final static String TYPE = "SoundBank";

    /**
     * Final implementation. subclasses are differentiated viy version.
     * @return type attribute
     */
    public final String getType() { return TYPE; }

    /**
     * This is the first version of this interface.
     * @return version (1.0)
     */
    public double version() { return 1.0; }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * Add a location trigger.
     * @param loc location trigger
     */
    public abstract void addLocation(Location loc);

    /**
     * Add a (calendar) event trigger.
     * @param ev event trigger
     */
    public abstract void addEvent(TimeEvent ev);
}
