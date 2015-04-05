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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

//import com.oyoaha.swing.plaf.oyoaha.*;

/**
 * The main singleton class. It constructs the GUI and passes all plugins to
 * the PluginLoader. The framework also manages the listeners and
 * announcements of exported objects.<br> Some sample "look and feels" are put
 * here for reference.  Plugin writers should consult the public methods for
 * reference.
 * @author Michael Jung
 */
public class Framework extends WindowAdapter {
    /** Look and feel samples */
    private final static String[] looknfeel = new String[] {
	"com.incors.plaf.kunststoff.KunststoffLookAndFeel",
        "com.birosoft.liquid.LiquidLookAndFeel",
        "com.jgoodies.looks.plastic.Plastic3DLookAndFeel",
        "org.compiere.plaf.CompierePLAF",
	"com.sun.java.swing.plaf.windows.WindowsLookAndFeel",
        "com.oyoaha.swing.plaf.oyoaha.OyoahaLookAndFeel",
        "de.muntjak.tinylookandfeel.TinyLookAndFeel",
	"javax.swing.plaf.metal.MetalLookAndFeel",
	"com.sun.java.swing.plaf.motif.MotifLookAndFeel",
	"com.sun.java.swing.plaf.gtk.GTKLookAndFeel",
	"com.sun.java.swing.plaf.mac.MacLookAndFeel"
    };

    /** get $HOME */
    private final static String HOME = System.getProperty("user.home");

    /** OS dependent file seperator. Use for convenience */
    public final static char SEP = System.getProperty("file.separator").charAt(0);

    /** The (static) loader */
    private PluginLoader loader;

    /** The listener hash maps */
    private HashMap type2name2listener;

    /** announced objects during startup */
    private HashSet exports;

    /** overall frames */
    private JFrame[] frame;

    /** plugins per frame */
    private ArrayList[] plugins;

    /** plugins */
    private Hashtable allPlugins;

    /** popup tabs */
    private boolean popup;

    /**
     * During start up listeners will not be informed. Since object creation
     * may take place before listeners have a chance to register, we stack
     * them until start up has terminated. Once general startup ic finished,
     * we keep stacking (and informing) until the stack is empty.
     */
    private int startup; // 0 = stack, 1 = stack/inform, 2 = inform

    /** version information */
    private static boolean version = false;

    /** editor */
    private static boolean edit = false;

    /** Random generator */
    private static Random rand = new Random();

    /** Window size properties */
    private static Properties sizes;

    /** Document builder */
    private static DocumentBuilder docBuilder;

    /** XML transforming */
    private static TransformerFactory transFact;

    /** XML transfoprming */
    private static Transformer transFormer;

    /** Main method */
    public static void main(String[] args) {
	if (args.length > 0) {
            if (args[0].equals("-version")) version = true;
            if (args[0].equals("-edit")) edit = true;
        }

        
        try {
            UIManager.setLookAndFeel
                (UIManager.getSystemLookAndFeelClassName());
            //(looknfeel[0]);

            /*
            File file =
                new File(System.getProperty("user.dir"), "slushy10.zotm");
            OyoahaLookAndFeel lnf = new OyoahaLookAndFeel();
            if(file.exists()) lnf.setOyoahaTheme(file);
            UIManager.setLookAndFeel(lnf);
            */
        }
        catch (Exception e) {
            System.out.println("WARNING: Unable to load native look and feel");
        }

        try {
            Framework gui = new Framework();
        }
        catch (Throwable e) {
            // Debug
            e.printStackTrace();
        }
    }

    /**
     * Constructor. Builds GUI. Loads plugins. Informs initial listeners. Get
     * the property file, which contains the layout. Then lapses into standard
     * passive GUI mode. The GUI consists of <n> windows or panels, each
     * contains a set of plugins.
     */
    private Framework() throws Exception {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("properties"));
            sizes = new Properties();
            File sf = new File(".state");
            if (sf.exists())
                sizes.load(new FileInputStream(sf));
        }
        catch(Exception e) {
            // Debug
            e.printStackTrace();
        }

        // Popup?
        popup = "true".equals(props.getProperty("tabs.popup"));

        // Get window list
        ArrayList wl = new ArrayList();
        for (int i = 1; props.getProperty("window." + i) != null; i++) {
            String[] plugs = props.getProperty("window." + i).split(",");
            wl.add(plugs);
        }
        if (wl.size() == 0) wl.add(new String[0]);

        // GUI Frame
        frame = new JFrame[wl.size()];
        plugins = new ArrayList[wl.size()];
        for (int i = 0; i < frame.length; i++) {
            frame[i] = new JFrame("Universal Gamemaster Tool");
            frame[i].getContentPane().setLayout(new BorderLayout());
            // Register quit operation
            frame[i].setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame[i].addWindowListener(this);
            JTabbedPane tabpane = new JTabbedPane();
            frame[i].getContentPane().setLayout(new BorderLayout());
            frame[i].getContentPane().add(tabpane, BorderLayout.CENTER);
            if (edit) {
                JLabel lab1 = new JLabel("EDIT MODE");
                lab1.setHorizontalAlignment(SwingConstants.CENTER);
                frame[i].getContentPane().add(lab1, BorderLayout.SOUTH);
            }
            plugins[i] = new ArrayList();

            tabpane.getActionMap().put("next", new MyAction(tabpane, true));
            tabpane.getActionMap().put("prev", new MyAction(tabpane, false));
            tabpane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put
                (KeyStroke.getKeyStroke("ctrl shift LESS"), "next");
            tabpane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put
                (KeyStroke.getKeyStroke("ctrl LESS"), "prev");
        }

        boolean useLast = false;

	if (version) System.err.println("Framework: 0.11");

        // Load plugins. We silently do nothing in case no plugins are
        // available or the main directory does not exist.
        File dir = new File(".");
        String[] all = (dir != null ? dir.list() : null);
        //all = new String[] { "maps", "combat", "groups" };

	// Splash
        final JFrame splash = Splash.createSplash(all);

        // Helpers
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        docBuilder = f.newDocumentBuilder();
        docBuilder.setErrorHandler(new ErrorHandler() {
                public void error(SAXParseException e) {
                    System.err.println
                        ("line:" + e.getLineNumber() +
                         ";column:" + e.getColumnNumber());
                }
                public void fatalError(SAXParseException e) { error(e); }
                public void warning(SAXParseException e) { error(e); }
            });
        transFact = TransformerFactory.newInstance();
        transFormer = transFact.newTransformer();
        transFormer.setOutputProperty(OutputKeys.INDENT, "yes");
        transFormer.setOutputProperty
            ("{http://xml.apache.org/xslt}indent-amount", "2");

        // Load
        loader = new PluginLoader(this, props);
        allPlugins = new Hashtable();

        TreeSet sort = new TreeSet();
        for (int i = 0; all != null && i < all.length; i++) sort.add(all[i]);
        Iterator iter = sort.iterator();
        
        // Primary load
        startup = 0;
        while (iter.hasNext()) {
            String curr = (String) iter.next();

            File pf = new File(curr);
            if (pf == null || !pf.isDirectory() || curr.equals("lib")) continue;
            Splash.loadlabel.setText("Loading " + curr + " ...");

            try {
                JPanel plugPanel = new JPanel();
                IPlugin plug = loader.add(pf, plugPanel, edit);

                // Find proper pane
                int found = 0;
                for (int j = 0; j < wl.size(); j++) {
                    String[] list = (String[])wl.get(j);
                    for (int k = 0; k < list.length; k++) {
                        if (list[k].equals(curr)) {
                            found = j;
                            break;
                        }
                    }
                }

                Component cmp = frame[found].getContentPane().getComponent(0);
                if (plug != null)
                    ((JTabbedPane)cmp).add(capitalize(pf.getName()), plugPanel);

                // Add to list
                if (plug != null) {
                    if (version) {
                        versionInfo(curr, plug);
                    }
                    else {
                        plugins[found].add(plug);
                        allPlugins.put(pf.getName(), plug);
                    }
                }
            }
            catch (Throwable t) {
                // Debugging
                t.printStackTrace();
            }
        }
	if (version) System.exit(0);

	startup = 1;

	// Inform listeners
	while (exports != null && !exports.isEmpty()) {
	    IExport exp = (IExport) exports.iterator().next();
	    exports.remove(exp);
            try {
                Splash.loadlabel.setText("Connecting... " + exp.getType());
                announceCond(exp);
            }
            catch (Throwable t) {
                // Debugging
                t.printStackTrace();
            }
	}

	startup = 2;

        // Show it off
        for (int i = 0; i < frame.length; i++)
            SwingUtilities.invokeLater(new MyThread(frame[i], 0));
            SwingUtilities.invokeLater(new Thread() {
                    public void run() {
                        splash.dispose();
                    }
                });
        for (int i = 0; i < frame.length; i++) {
            if (sizes.getProperty("window." + i) != null) {
                String xy[] = sizes.getProperty("window." + i).split(",");
                if (xy[0].startsWith("MAX")) {
                    frame[i].setExtendedState(JFrame.MAXIMIZED_HORIZ);
                    xy[0] = xy[0].substring(3);
                }
                if (xy[1].startsWith("MAX")) {
                    frame[i].setExtendedState(JFrame.MAXIMIZED_VERT);
                    xy[1] = xy[1].substring(3);
                }
                frame[i].setSize
                    (Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
            }
            SwingUtilities.invokeLater(new MyThread(frame[i], 1));
        }
    }

    /**
     * This method is called by plugins to announce new objects, types and
     * changes to these. During startup this is buffered to allow late
     * registration.
     * @param e the object to be notified
     */
    public void announce(IExport e) {
	// Stacking...
	if (startup == 0 || startup == 1) {
	    if (exports == null) exports = new HashSet();
	    exports.add(e);
	    return;
	}
        announceCond(e);
    }

    /**
     * Announce bypass when stacking/informing
     */
    private void announceCond(IExport e) {
        // No type
	if (e.getType() == null) return;

	// No listeners at all
	if (type2name2listener == null) return;

	// Get specific type listeners; if none return
	HashMap name2listener = (HashMap) type2name2listener.get(e.getType());
	if (name2listener == null) return;

	// Get specific name listeners
	HashSet listenerSet = (HashSet) name2listener.get(e.getName());
	if (listenerSet == null) listenerSet = new HashSet();

	// Add general name listeners
	listenerSet.addAll((HashSet) name2listener.get(null));

	// Inform them all
	Object[] list = listenerSet.toArray();
	for (int i = 0; list != null && i < list.length; i++) {
	    IListener lnr = (IListener) (list[i]);
	    if (lnr != null) lnr.inform(e);
	}
    }

    /**
     * This method is called to register listeners on IExport objects. name
     * may be null, which means to be informed for all objects of type type.
     * @param listener listener to be informed
     * @param type object type
     * @param name object instance's name
     */
    public void listen(IListener listener, String type, String name) {
	// Ignore me...
	if (type == null || listener == null) return;

	// No listeners at all yet
	if (type2name2listener == null) type2name2listener = new HashMap();

	// Add type-map if not present
	if (!type2name2listener.containsKey(type))
	    type2name2listener.put(type,new HashMap());

	// Add name-map if not present
	HashMap name2listener = (HashMap) type2name2listener.get(type);
	if (!name2listener.containsKey(name))
	    name2listener.put(name,new HashSet());

	// Add listener to the map he belongs to
	HashSet listenerSet = (HashSet) name2listener.get(name);
	listenerSet.add(listener);
    }

    /**
     * Call this method to remove a listener. Call it for each place this
     * listener was registered, there is no method to remove a specific
     * listener from all lists.
     * @param listener listener to be removed
     * @param type object type
     * @param name object instance's name
     */
    public void deafen(IListener listener, String type, String name) {
	// Ignore me...
	if (type == null) return;
	if (!type2name2listener.containsKey(type)) return;

	// Get the name listener map; if none return
	HashMap name2listener = (HashMap) type2name2listener.get(type);
	if (!name2listener.containsKey(name)) return;

	// Get the listener set and remove as asked
	HashSet listenerSet = (HashSet) name2listener.get(name);
	listenerSet.remove(listener);

	// Clean out those maps that are not needed. Keep the top map.
	if (listenerSet.isEmpty()) name2listener.remove(name);
	if (name2listener.isEmpty()) type2name2listener.remove(type);
    }

    /**
     * The exit handler. In case the user requests quiting, we ask all
     * plugins, whether there remains any unsaved data and in that case ask
     * the user whethe to cancel, save or quit without saving.
     * @param e window event
     */
    public void windowClosing(WindowEvent e) {
	boolean dirty = false;

        // Get source
        int src = 0;
        while (e.getSource() != frame[src]) src++;

	// Get all plugins' dirty state
        ArrayList al = plugins[src];
	for (int i = 0; i < al.size(); i++)
	    dirty |= ((IPlugin) al.get(i)).hasUnsavedParts();
	int opt;

        Dimension d = frame[src].getSize();
        String w = ((frame[src].getExtendedState() & JFrame.MAXIMIZED_HORIZ) > 0 ? "MAX" : "") + d.width;
        String h = ((frame[src].getExtendedState() & JFrame.MAXIMIZED_VERT) > 0 ? "MAX" : "") + d.height;
        sizes.setProperty("window." + src, w + "," + h);

	// Preselect the options
	if (dirty) {
	    // Show dialog
            Object[] options = { "YES/BACKUP", "YES", "NO" };
            int ret = JOptionPane.showOptionDialog
                (frame[src], "Save plugin states?", "Confirm",
                 JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                 null, options, options[0]);

	    // React (Fall through: backup -> save -> end)
	    switch (ret) {
            case 0: backup();
            case 1: for (int i = 0; i < al.size(); i++)
                ((IPlugin)al.get(i)).stop(true);
            case 2:
                ((Window)e.getSource()).dispose();
                frame[src] = null;
	    }
	}
	else {
	    opt = JOptionPane.OK_CANCEL_OPTION;

	    // Show dialog
	    int ret = JOptionPane.showConfirmDialog
		(frame[src], "Really quit?", "Quit", opt);
	    
	    // React
	    switch (ret) {
            case JOptionPane.OK_OPTION:
                for (int i = 0; i < al.size(); i++)
                    ((IPlugin)al.get(i)).stop(false);
                ((Window)e.getSource()).dispose();
                frame[src] = null;
	    }
	}

        for (int i = 0; i < frame.length; i++)
            if (frame[i] != null) return;

        try {
            sizes.store(new FileOutputStream(".state"), null);
        }
        catch(Exception ex) {
            // Debug
            ex.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * This allows inquisitive plugins to obtain a list of plugins. The
     * returned array list contains the name to plugin map.
     * @return a list of all plugins
     */
    public Hashtable getPlugins() { return allPlugins; }

    /**
     * This allows inquisitive plugins to obtain a certain plugin. A null may
     * be returned if the plugin is not available.
     * @param name name of plugin
     * @return the requested plugin
     */
    public IPlugin getPlugins(String name) {
	return (IPlugin)allPlugins.get(name);
    }

    /**
     * This will raise the tab of the plugin. Nothing happens, if the plugin
     * isn't present.
     * @param name plugin name
     */
    public void raisePlugin(String name) {
        if (!popup) return;
        for (int i = 0; i < frame.length; i++) {
            JTabbedPane pane =
                (JTabbedPane) frame[i].getContentPane().getComponent(0);
            for (int j = 0; j < pane.getTabCount(); j++)
                if (pane.getTitleAt(j).equals(name)) {
                    pane.setSelectedIndex(j);
                    return;
                }
        }
    }

    /**
     * Allows plugins to set and reset a busy cursor, in case lengthy loading
     * calculations take place.
     * @param yes busy?
     */
    public void setBusy(boolean yes) {
        if (yes)
            for (int i = 0; i < frame.length; i++)
                frame[i].setCursor
                    (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        else
            for (int i = 0; i < frame.length; i++)
                frame[i].setCursor(Cursor.getDefaultCursor());
    }

//    /**
//     * This method rolls an n-sided die. Invalid entries return themselves.
//     * @param n maximum number to roll
//     * @return random number in 1..n
//     */
//    public static int d(int n) {
//	if (n > 1) return 1 + Math.abs(rand.nextInt()) % n;
//	return n;
//    }
//
//    /**
//     * This method rolls m n-sided dice. Invalid entries return themselves,
//     * i.e. m*n.
//     * @param n maximum number to roll
//     * @return random number in 1..n
//     */
//    public static int d(int m, int n) {
//	if (m < 1) return m*n;
//	int ret = 0;
//	for (int i = 0; i < m; i++)
//	    ret += d(n);
//	return ret;
//    }

    /**
     * Versioning information to help debugging a multi-plugin environment.
     * @param name name of plugin
     * @param p plugin to print version information about
     */
    private void versionInfo(String name, IPlugin p) {
	System.err.println(name + ": " + p.version());

	String[] prov = p.provides();
	System.err.print(" - provides: ");
	for (int i = 0; i < prov.length - 1; i++)
	    System.err.print(prov[i] + ", ");
	if (prov.length > 0)
	    System.err.print(prov[prov.length - 1]);
	System.err.println();
	    
	System.err.print(" - requires: ");
	String[] req = p.requires();
	for (int i = 0; i < req.length - 1; i++)
	    System.err.print(req[i] + ", ");
	if (req.length > 0)
	    System.err.print(req[req.length - 1]);
	System.err.println();
    }

    /**
     * Helper to capitalize a string.
     * @param str string to capitalize
     * @return capitalized string
     */
    public static String capitalize(String str) {
	char c[] = str.toCharArray();
 	c[0] = Character.toUpperCase(c[0]);
 	return new String(c);
    }

    /**
     * Helper to OS-ify a filename
     * @param str string to OS-ify
     * @return OS-ify string
     */
    public static String osName(String str) { return str.replace('/',SEP); }

    /**
     * Make a backup of a file.
     * @param src source
     */
    public static void backup(String src) {
        try {
            FileChannel srcChannel =
                new FileInputStream(src).getChannel();
            FileChannel dstChannel =
                new FileOutputStream(src + ".back").getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        }
        catch (FileNotFoundException e) {
            // No backup then
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
    }

    /**
     * Parse a document. If document cannot be loaded, print a message and
     * then return a doc with empty root element.
     * @param name file name
     * @param root root element
     * @return document parsed
     */
    public static Document parse(String name, String root) {
        try {
            return docBuilder.parse(new File(osName(name)));
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
            Document doc = newDoc();
            Element elem = doc.createElement(root);
            doc.appendChild(elem);
            return doc;
        }
    }

    /**
     * Return new document.
     * @return new document
     */
    public static Document newDoc() {
        return docBuilder.newDocument();
    }

    /**
     * XML transform utility
     */
    public static void transform(Document src, Result dest, Document xslt) {
        Transformer localFormer = transFormer;
        try {
            if (xslt != null) {
                localFormer = transFact.newTransformer(new DOMSource(xslt));
            }
            localFormer.transform(new DOMSource(src), dest);
        }
        catch (Exception e) {
            // Debug
            e.printStackTrace();
        }
    }

    /**
     * Return all state.xml and all unmerged state*.xml files, state.xml in
     * pole position.
     * @param path path to search
     * @param merged list of names to ignore
     */
    public static String[] getStateFiles(String path, final String merged) {
        final String[] ignore = merged.split(",");
        File pf = new File(path);
        String[] list = pf.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    boolean accept = 
                        name.endsWith(".xml") && name.startsWith("state");
                    if (!accept) return false;
                    for (int i = 0; ignore != null && i < ignore.length; i++)
                        if (ignore[i].equals(name)) return false;
                    return true;
                }
            });
        for (int i = 0; list != null && i < list.length; i++) {
            if (list[i].equals("state.xml")) {
                list[i] = list[0];
                list[0] = "state.xml";
                break;
            }
        }
        // No state.xml file found
        String[] ret = list;
        if (list == null || list.length == 0 || !list[0].equals("state.xml")) {
            ret = new String[list != null ? list.length + 1 : 1];
            ret[0] = "state.xml";
            for (int i = 1; i < ret.length; i++)
                ret[i] = list[i-1];
        }
        return ret;
    }

    /**
     * Add all current state files to the merged list.
     * @param doc document to add merged attribute to
     * @param path path of the callling plugin
     * @param merged previsouly merged list (non-null and non-empty)
     */
    public static void setMergedStates(Document doc, String path, final String merged) {
        File pf = new File(path);
        String[] list = pf.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml") && name.startsWith("state");
                }
            });

        // Take old merged, add new
        StringBuffer buf = new StringBuffer(merged);
        for (int i = 0; i < list.length; i++) {
            if (merged.indexOf(list[i]) < 0)
                buf.append(",").append(list[i]);
        }
        String newmerged = buf.toString();
        // Set or create and add newly merged
        Element elmerge = null;
        NodeList nodelist = doc.getDocumentElement().getElementsByTagName("merged");
        if (nodelist != null && nodelist.getLength() > 0)
            elmerge = (Element)nodelist.item(0);

        if (elmerge != null) {
            elmerge.getAttributes().getNamedItem("list").setNodeValue(newmerged);
        }
        else {
            elmerge = doc.createElement("merged");
            elmerge.setAttribute("list", newmerged);
            doc.getDocumentElement().insertBefore(elmerge, null);
        }
    }

    /**
     * Get a reasonable model for scale spinners.
     */
    public static SpinnerListModel getScaleSpinnerModel(int sz) {
        Object[] ret = new Object[sz];
        for (int i = 0; i < 10 && i < sz; i++)
            ret[i] = new Integer(10 + 10*i);

        for (int i = 10; i < sz; i++) {
            int prev = ((Integer)ret[i-1]).intValue();
            ret[i] = new Integer((int)((1 + .1*(i-9)) * prev));
        }
        return new SpinnerListModel(ret);
    }

    /**
     * Static helper for attribute getter
     * @param attributes attributes to look in (non-null)
     * @param attribute name to look for
     * @param def default value, if name not found
     */
    public static String getXmlAttrValue(NamedNodeMap attributes, String name, String def) {
        if (attributes.getNamedItem(name) != null)
            return attributes.getNamedItem(name).getNodeValue();
        return def;
    }

    /**
     * Method to backup "state.xml" and "*.html" files in plugin root
     * directories.
     */
    private void backup() {
        // Inputs
        File dir = new File(".");
        String[] all = (dir != null ? dir.list() : null);

        try {
            // Output
            ZipOutputStream out = new ZipOutputStream
                (new FileOutputStream
                 (new Date().toString().replaceAll(" |:","-") + ".zip"));
            byte[] buf = new byte[1024];

            for (int i = 0; i < all.length; i++) {
                File pf = new File(all[i]);

                if (pf == null || !pf.isDirectory() || all[i].equals("lib")
                    || !new File(all[i] + SEP + "state.xml").exists())
                    continue;
            
                
                FileInputStream fis = new FileInputStream
                    (all[i] + SEP + "state.xml");

                // Add ZIP entry
                out.putNextEntry(new ZipEntry(all[i] + SEP + "state.xml"));

                // Add ZIP content
                int len = 0;
                while ((len = fis.read(buf)) > 0)
                    out.write(buf, 0, len);

                // Close
                out.closeEntry();
                fis.close();

                String[] html = pf.list(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".html");
                        }
                    });
                for (int j = 0; j < html.length; j++) {
                    fis = new FileInputStream
                        (all[i] + SEP + html[j]);

                    // Add ZIP entry
                    out.putNextEntry(new ZipEntry(all[i] + SEP + html[j]));

                    // Add ZIP content
                    len = 0;
                    while ((len = fis.read(buf)) > 0)
                        out.write(buf, 0, len);

                    // Close
                    out.closeEntry();
                    fis.close();
                }
            }

            out.close();
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
    }

    /**
     * Class for tabbed pane traversal
     */
    class MyAction extends AbstractAction {
        /** father object */
        private JTabbedPane myPane;
        /** forward ? */
        private boolean forward;
        /** Constructor */
        MyAction(JTabbedPane pane, boolean aForward) {
            myPane = pane;
            forward = aForward;
        }
        /** IF method */
        public void actionPerformed(ActionEvent e) {
            int i = myPane.getSelectedIndex();
            int n = myPane.getTabCount();
            myPane.setSelectedIndex(forward ? (i + 1)%n : (i + n - 1)%n);
        }
    };

    /**
     * Class for packing
     */
    class MyThread extends Thread {
        JFrame frame;
        int method;
        MyThread(JFrame aFrame, int aMethod) {
            super();
            frame = aFrame;
            method = aMethod;
        }
        public void run() {
            switch(method) {
                case 0: frame.pack(); break;
                case 1: frame.setVisible(true); break;
            }
        }
    }
}
