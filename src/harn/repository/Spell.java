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
 * Object that represents a spell. Contains the name of the spell, ML and a
 * reference to the owning character. Also contains the isValid flag.
 * @author Michael Jung
 */
public abstract class Spell implements IExport {
    /** type constant */
    public final static String TYPE = "Spell";

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
     * Implement as specified in superclass.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * Get the owning character. Never null if valid.
     * @return owner
     */
    public abstract Char getOwner();

    /**
     * Get ML.
     * @return ML
     */
    public abstract int getML();

    /**
     * Whether this spell is still valid.
     * @return validity
     */
    public abstract boolean isValid();
}
