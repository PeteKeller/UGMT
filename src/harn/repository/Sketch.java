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
import java.io.File;

/**
 * Object that represents a sketch or a note. It may either be a directory or
 * contain (graphical) content. Note that a directory strucure is reachable by
 * publishing the root element. It is not necessary to export all sketches. In
 * that case the validity must be checked by walking the tree. Changes in the
 * content are not announced.
 * @author Michael Jung
 */
public abstract class Sketch implements IExport {
    /** type constant */
    public final static String TYPE = "Sketch";

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
     * Returns the validity of a sketch. I.e. a sketch may have been removed
     * and becomes invalid. This is announced.
     * @return validity of sketch
     */
    public abstract boolean isValid();

    /**
     * Return a list of subsketches. If this is non-null, the image is null.
     * @return sub-sketch list
     */
    public abstract Sketch[] getSubSketches();

    /**
     * Return the content of this sketch. If this is non-null, subsketches are
     * null.
     * @return content image
     */
    public abstract File getContent();

    /**
     * If exported, this may be seen by players. It may be published
     * externally.
     * @return exportable or not
     */
    public abstract boolean isExported();

    /**
     * The sketch is selected. (This method only works for valid sketches.)
     * Note that you cannot unselect a sketch.
     */
    public abstract void setSelected();
}
