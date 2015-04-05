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
package rpg;

/**
 * This interface must be implemented by all objects that are to be announced
 * throughout the tool. Other plugins may react to the presence and changed of
 * such objects. For specific instance see the repository package. Any objects
 * that are not to remain private to a few plugins should be registered at the
 * repository, so foreign plugin writers see them.<br>
 * @author Michael Jung
 */
public interface IExport {
    /**
     * This is a type information. It may be considered a stringified class
     * name. Repository objects have final versions of this method.
     * @return type attribute
     */
    public String getType();

    /**
     * Returns the instance id or name. Different objects registered must use
     * different names for different objects, otherwise one will take
     * precedence. If different plugins attempt to register the same type,
     * they should use their own namespace in the instance for the same
     * reason.
     * @return instance name
     */
    public String getName();

    /**
     * Plugins that require a certain type object will find the necessity to
     * use more methods from that type as time passes. The providing plugin
     * will need to add these methods. To allow older plugins to interoperate
     * correctly, a version attribute all old methods must be used under the
     * same type name. The new requiring plugin will test the version to see
     * whether the new methods are provided. The new object type will probably
     * inherit from the old.
     * @return version of the type object
     */
    public double version();
}
