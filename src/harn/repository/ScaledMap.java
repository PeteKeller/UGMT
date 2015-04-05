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
import java.awt.geom.AffineTransform;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

/**
 * Object that represents a map to travel on. Do not hold all maps in memory,
 * only load those that are requested and maybe unload others after some time.
 * @author Michael Jung
 */
public abstract class ScaledMap implements IExport {
    /** type constant */
    public final static String TYPE = "ScaledMap";

    /**
     * Final implementation. subclasses are differentiated viy version.
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
     * Get the image of the map; to be displayed. Does not includes display
     * scale. The delivering plugin may return null. The force parameter is an
     * indication that it should try harder.
     * @param force force image return
     * @param raw raw or modified image
     * @return image to be displayed
     */
    public abstract BufferedImage getBufferedImage(boolean force, boolean raw);

    /**
     * Return the scale of the map. The scale is pixels / 4 kilometres.
     * @return scale
     */
    public abstract int getScale();

    /**
     * Return the terrain feature of the given location. This may be an
     * arbitrary string but should be considered carefully as other plugins
     * may react to it. E.g. if you return "mountains", a travel plugin might
     * choose a different movement rate. If nothing special can be said, an
     * empty string may be returned. A null may be returned if the map
     * object is not responsible for this location.
     * @return terrain identifier
     */
    public abstract String getTerrain(int x, int y);

    /**
     * Return the terrain feature of the given location. This may be an
     * arbitrary string but should be considered carefully as other plugins
     * may react to it. E.g. if you return "mountains", a travel plugin might
     * choose a different movement rate. If nothing special can be said, an
     * empty string may be returned. A null may be returned if the map object
     * is not responsible for this location. The map shall look within a
     * reasonable distance for the closest land (non-water) terrain spot and
     * return it.
     * @return terrain identifier
     */
    public abstract String getLandTerrain(int x, int y);

    /**
     * Return the vegetation feature of the given location. This may be an
     * arbitrary string but should be considered carefully as other plugins
     * may react to it. E.g. if you return "woods", a travel plugin might
     * choose a different movement rate. If nothing special can be said, an
     * empty string may be returned. A null may be returned if the map
     * object is not responsible for this location.
     * @return terrain identifier
     */
    public abstract String getVegetation(int x, int y);

    /**
     * Return the vegetation feature of the given location. This may be an
     * arbitrary string but should be considered carefully as other plugins
     * may react to it. E.g. if you return "woods", a travel plugin might
     * choose a different movement rate. If nothing special can be said, an
     * empty string may be returned. A null may be returned if the map
     * object is not responsible for this location. The map shall look within a
     * reasonable distance for the closest land (non-water) terrain spot and
     * return it.
     * @return terrain identifier
     */
    public abstract String getLandVegetation(int x, int y);

    /**
     * Get the part (0-1) of tactical speed that is used on this map. A
     * travelling group has inherent speed. Sometimes the group speed is not
     * adequately covered by terrain and vegetation, mostly tactical
     * environs. Should not be used for water travel.
     * @return tactical speed part of total speed
     */
    public abstract float getTacticalSpeed();

    /**
     * Get a transform which transforms coordinates of this map into
     * coordinates of the map called "map".  This is always an affine
     * transform. In case such a transform does not exist, such as when a map
     * belongs to a different atlas (isn't interconnected) a null is returned.
     * @param map name of the other map
     * @return the transform converting coordinates from this' to map's
     */
    public abstract AffineTransform getTransform(String map);

    /**
     * Get the list of all transformable or reachable maps, i.e. all those
     * interconnected with this map. The returned array includes the map
     * itself.
     * @return list of (full) names of maps reachable.
     */
    public abstract String[] getReachableMaps();

    /**
     * Get a height field for the map. Height is given as color value: the
     * whiter, the higher.
     * @return heightfield image
     */
    public abstract BufferedImage getHeightField();

    /**
     * Get the maximum height in feet of the heightfield.
     * @return maximum height
     */
    public abstract int getHeight();
}
