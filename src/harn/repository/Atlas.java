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
 * Object that represents a set of maps. It can be queried for specific
 * maps. While there is little chance that this object will change, it should
 * be stored for further reference by those plugins that will later access
 * maps but do not want to keep references to each single map. Moreover single
 * maps may contains large data blocks for their respective images and these
 * should be garbage-collectable. It is therefore strongly suggested not to
 * keep references to many individual maps but rather an atlas.
 * @author Michael Jung
 */
public abstract class Atlas implements IExport {
    /** type constant */
    public final static String TYPE = "Atlas";

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
     * Implement as speciied in superclass.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * Return a map with the specified name. If an atlas does not contain a
     * map with that name, it returns a null.
     * @param name name of the map requested
     * @return map requested or null if not present
     */
    public abstract ScaledMap getMap(String name);
}
