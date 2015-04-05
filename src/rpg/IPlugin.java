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

import javax.swing.JPanel;
import java.util.Properties;

/**
 * This interface must be implemented by all plugins.
 * @author Michael Jung
 */
public interface IPlugin {
    /**
     * This method is called when the plugin is loaded. Do not assume any
     * other plugin is loaded before or after yours. Use the listener
     * interface for that. Any initialization should take place here. Objects
     * being announced are stalled until all plugins are loaded, at which
     * point they are announced.<br> Presently all plugins are loaded at the
     * same time, no delayed loading is possible, nor is unloading.
     * @param frw framework reference
     * @param frm the GUI frame that belongs to the plugin
     * @param props elementary configuration
     * @param path path to the plugin. Use for loading additional data and
     * state information
     */
    public void start(Framework frw, JPanel frm, Properties props, String path);
    /**
     * Provide a version information of a format "x.y", where x is a major and
     * y is a minor version number. Queried by the framework.
     */
    public float version();

    /**
     * This method will be called when the editable part of a plugin is
     * loaded.  Do not assume any other plugin is loaded before or after
     * yours. Use the listener interface for that. Any initialization should
     * take place here. Objects being announced are stalled until all plugins
     * are loaded, at which point they are announced.<br> Presently all
     * plugins are loaded at the same time, no delayed loading is possible,
     * nor is unloading. Do not assume any correspondence with the
     * <em>start</em> method. If the plugin does not support editing, return
     * false.  (The JPanel will be reused then.)
     * @param frw framework reference
     * @param frm the GUI frame that belongs to the plugin
     * @param props elementary configuration
     * @param path path to the plugin. Use for loading data information
     * @return whether this plugin suport editing at all
     */
    public boolean startEdit(Framework frw, JPanel frm, Properties props, String path);

    /**
     * The same caution that applies to start applies here. Do elementary
     * clean-up.  If save is true, the user has requested state information to
     * be saved. If false, do not save. Try avoid popping up save dialog boxes
     * here - use the defaults or inputs already provided. Since order is not
     * guaranteed several such save dialogs may be confusing. If you need to,
     * don't forget to identify it as yours.
     * @param save save information to disk
     */
    public void stop(boolean save);

    /**
     * This method is called by the framework to decide whether any components
     * have unsaved data and it needs to query the user for immediate or
     * save/safe exit.
     * @return whether the componets has unsevd parts
     */
    public boolean hasUnsavedParts();

    /**
     * Each plugin must provide an array of objects it requires. This method
     * is not used by the framework presently. It may be used by an
     * administrative plugin to find out whether an object was not announced
     * that is needed by a plugin that appearantly is not functioning
     * correctly.
     * @return list of imports
     */
    public String[] requires();

    /**
     * Each plugin must provide an array of objects it may use but does not
     * require. This method is not used by the framework presently. It may be
     * used by an administrative plugin to find out whether an object was not
     * announced that would be useful to a plugin that appearantly is not
     * functioning satisfactory.
     * @return list of imports
     */
    public String[] uses();

    /**
     * Each plugin must provide an array of objects it announces. This method
     * is not used by the framework presently. It may be used by an
     * administrative plugin to find out whether an object was not announced
     * that is needed by a plugin that appearantly is not functioning
     * correctly.
     * @return list of exports
     */
    public String[] provides();
}
