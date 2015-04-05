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

import java.net.*;
import java.io.*;
import java.util.Properties;
import javax.swing.JPanel;

/**
 * This singleton loads all plugins into memory. It is called from the
 * framework main class. It loads the jar file that is contained in the
 * plugin's directory, loads the properties and then starts it.<br>
 * This is not an interface availabel to plugins.
 * @author Michael Jung
 */
public class PluginLoader extends URLClassLoader {
    /** reference to the singleton framework */
    private Framework frw;

    /** Properties */
    private Properties global;

    /**
     * Constructor.
     * @param aFrw framework reference
     */
    PluginLoader(Framework aFrw, Properties props) {
        super(new URL[0]);
        frw = aFrw;
        global = props;
    }

    /**
     * Load the plugin and pass it some GUI space.
     * @param pi the directory of the plugin
     * @param frm the GUI space available for the plugin.
     * @param edit edit mode
     */
    IPlugin add(File pi, JPanel frm, boolean edit) throws Exception {
	String path = pi.getAbsolutePath() + frw.SEP;

	// Get jar/classes
	String jar = pi.toURI().toURL().toString() + pi.getName() + ".jar";
	addURL(new URL(jar));
	// Get properties
	Properties props = new Properties(global);
	props.load(new FileInputStream(path + "properties"));

	// Init
	Class pc = loadClass("harn." + pi.getName() + ".Main");
	IPlugin po = (IPlugin) pc.newInstance();
	if (edit) {
            if (po.startEdit(frw, frm, props, path))
                return po;
            else
                return null;
        }
        else {
            po.start(frw, frm, props, path);
            return po;
        }
    }
}
