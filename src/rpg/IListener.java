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
 * This interface is implemented by all objects that are registered as
 * listeners for exported objects from other plugins. Once registered, the
 * object is informed every time a change is announced by the providing
 * plugin. During startup, all announced objects are collected and notified
 * after all plugins are running to avoid missed objects due to the start
 * sequence.<br> Registering is explained in the Framework.
 * @author Michael Jung
 */
public interface IListener {
    /**
     * Called by the framework to notify a new/changed object to the listener.
     * Take care that this is actually the object you are looking
     * for. Sometimes, for instance when listening to types, you will be
     * informed about objects aou have no interest in. Do not update your
     * plugin state everytime with data you probably already have.<br> Also be
     * aware that objects may become invalid. To inform about this state
     * change, the object must either provide an isValid method, or announce
     * another as isActive, in the hope that other plugins will invalidate
     * non-active objects themselves.
     * @param obj the notified object
     */
    public void inform(IExport obj);
}
