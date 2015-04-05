/**
 * UGMT : Universal Gamemaster tool
 * Copyright (c) 2004 Michael Jung
 * miju@phantasia.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package harn.log;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import rpg.*;

/**
 * The main class for the log plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class State implements IListener {
    /** length of hour */
    final public static int HOUR = 60;

    /** length of day */
    final public static int DAY = 24 * HOUR;

    /** current logged group */
    protected CharGroup act;

    /** types of entries (checkboxes) */
    private HashSet types;

    /** Main reference */
    private Main main;

    /** Time reference */
    private TimeFrame tf;

    /** Current time events */
    private ArrayList events;

    /** Location list */
    private Hashtable allLocs;

    /** Last printed date */
    private String lastDate;

    /** Weather reference */
    private Weather weather;
    
    /** Current Weather effect/condition */
    private String currentWeather;
    
    /** Current Locations */
    private ArrayList currentLocs;
    
    /** Constructor */
    public State(Main aMain) {
        main = aMain;
        events = new ArrayList();
        allLocs = new Hashtable();
        types = new HashSet();

        org.w3c.dom.Document dataDoc = Framework.parse
            (main.myPath + "state.xml", "calendar");
        loadDoc(dataDoc.getDocumentElement());
    }

    /**
     * Method to load the state.
     * @param doc (XML) document to load
     */
    private void loadDoc(Node doc) {
        NodeList tree = doc.getChildNodes();
        String[] typeL = new String[0];
	for (int i = 0; i < tree.getLength(); i++) {
            if (!tree.item(i).getNodeName().equals("types")) continue;
            typeL =
                tree.item(i).getAttributes().getNamedItem("list").getNodeValue().split(",");
        }
        for (int i = 0; (typeL != null) && (i < typeL.length); i++)
            if (typeL[i].length() != 0)  types.add(typeL[i]);
    }

    /**
     * IF method from listener.
     * @param obj changed object
     */
    public void inform(IExport obj) {
        if (obj instanceof CharGroup) {
            CharGroup cg = (CharGroup) obj;
            if (cg.isActive()) {
                if (act == null || !cg.getName().equals(act.getName())) {
                    act = cg;
                    load();
                }
                if (tf == null || weather == null) return;

                long date = act.getDate();
                String map = act.getMap();
                int x = act.getX();
                int y = act.getY();

                if (main.bWeather.isSelected()) {
                    String eff1 = weather.event(map, x, y, date);
                    String eff2 = weather.condition(map, x, y, date);
                    String effect = "";
                    if (eff1 != null && eff1.length() != 0)
                        effect = Framework.capitalize(eff1);
                    if (eff2 != null && eff2.length() != 0 && !eff2.equals(eff1)) {
                        if (effect.length() != 0)
                            effect += ", " + eff2;
                        else
                            effect = Framework.capitalize(eff2);
                    }
                    insertWeather(effect);
                }
                checkAllLocs();
            }
        }
        if (obj instanceof TimeFrame) {
            tf = (TimeFrame) obj;
        }
        // Events should only be anounced after a calendar!
        if (obj instanceof TimeEvent) {
            TimeEvent ev = (TimeEvent)obj;
            if (!ev.isActive() || !main.bCalendar.isSelected()) return;
            insertTimeEvent(ev.getName());
        }
        if (obj instanceof Weather) {
            Weather w = (Weather) obj;
            if (w.isActive()) weather = w;
        }
        if (obj instanceof Location) {
            Location loc = (Location)obj;
            if (loc.isValid())
                allLocs.put(loc.getName(), obj);
            else
                allLocs.remove(loc.getName());
            checkAllLocs();
        }
    }

    /**
     * Load the log of current group
     */
    private void load() {
        try {
            File file = new File(main.myPath + act.getName() + ".html");
            Document doc = main.display.getDocument();
            doc.putProperty(Document.StreamDescriptionProperty, null);
            if (file.exists())
                main.display.setPage(file.toURL());
            else {
                HTMLDocument tmp =
                    (HTMLDocument)new HTMLEditorKit().createDefaultDocument();
                main.display.setContentType("text/html");
                main.display.setDocument(tmp);
            }
            main.display.getDocument().addDocumentListener
                (new DocumentListener() {
                        public void insertUpdate(DocumentEvent e) { dirty(); }
                        public void removeUpdate(DocumentEvent e) { dirty(); }
                        public void changedUpdate(DocumentEvent e) { dirty(); }
                        private void dirty() { main.bSave.setEnabled(true); }
                    });
        }
        catch (Exception e) {
            // Debug
            e.printStackTrace();
        }
    }

    /**
     * Save the log of current group and state.
     */
    public void save() {
        try {
            Framework.backup(main.myPath + act.getName() + ".html");
            FileWriter fw =
                new FileWriter(main.myPath + act.getName() + ".html");
            String txt = main.display.getText();
            fw.write(txt, 0, txt.length());
            fw.close();

            saveDoc();
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
    }

    /**
     * Use this method to save the data in this object to file. After saving,
     * this object is "clean" once more.
     */
    public void saveDoc() {
        try {
            // Create technical objects
            org.w3c.dom.Document doc = Framework.newDoc();

            // Root
            Node root = doc.createElement("calendar");
            doc.insertBefore(root, doc.getLastChild());

            // List
            String list = "";
            if (!main.bWeather.isSelected()) list += "Log Weather,";
            if (!main.bLocs.isSelected()) list += "Log Locations,";
            if (!main.bCalendar.isSelected()) list += "Log Events,";
            if (!main.bDate.isSelected()) list += "Log Date,";
            if (!main.bTime.isSelected()) list += "Log Time,";
            if (list.length() > 0) {
                org.w3c.dom.Element eltypes = doc.createElement("types");
                eltypes.setAttribute("list", list.substring(0,list.length() - 1));
                org.w3c.dom.Element eltype = doc.createElement("test");
                root.insertBefore(eltypes, null);
                eltypes.insertBefore(eltype, null);
            }

            // Write the document to the file
            File file = new File(main.myPath + "state.xml");
            Framework.backup(main.myPath + "state.xml");
            Result result = new StreamResult(file);

            Framework.transform(doc, result, null);
        }
	catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Insert a time event.
     * @param effect event
     */
    public void insertTimeEvent(String effect) {
        if (effect.length() == 0) return;

        boolean found = false;
        for (int i = 0; !found && (i < events.size()); i++)
            found = events.get(i).equals(effect);

        insertElement(effect, !found);
        if (!found) events.add(effect);
    }

    /**
     * Insert a weather event
     * @param effect event
     */
    public void insertWeather(String effect) {
        if (effect.equals(currentWeather)) return;
        currentWeather = effect;
        insertElement(effect, true);
    }

    /**
     * Insert a location event
     * @param effect event
     */
    public void insertLocationEvent(ArrayList toCheck) {
        String str = "";

        // Entering
        String enter = "Entering: ";
        for (int i = 0; (toCheck != null) && i < toCheck.size(); i++) {
            boolean alreadyIn = (currentLocs == null);
            for (int j = 0; !alreadyIn && (j < currentLocs.size()); j++)
                if (currentLocs.get(j).equals(toCheck.get(i)))
                    alreadyIn = true;
            if (!alreadyIn) {
                String loc = toCheck.get(i).toString();
                enter += loc.substring(loc.lastIndexOf("/") + 1) + ",";
            }
        }

        // Leaving
        String leave = "Leaving: ";
        for (int i = 0; (currentLocs != null) && i < currentLocs.size(); i++) {
            boolean stillIn = (toCheck == null);
            for (int j = 0; !stillIn && (j < toCheck.size()); j++)
                if (toCheck.get(j).equals(currentLocs.get(i)))
                    stillIn = true;
            if (!stillIn) {
                String loc = currentLocs.get(i).toString();
                leave += loc.substring(loc.lastIndexOf("/") + 1) + ",";
            }
        }

        currentLocs = toCheck;
        boolean doEnter = false;
        if (enter.length() != "Entering: ".length()) {
            str += enter.substring(0,enter.length()-1);
            doEnter = true;
        }
        if (leave.length() != "Leaving: ".length()) {
            if (doEnter) str += ", ";
            str += leave.substring(0,leave.length()-1);
        }

        insertElement(str, true);
    }

    /**
     * Enter a string into the document at the current date.
     * @param effect string to enter
     * @param force force display even if date hasn't changed
     */
    protected void insertElement(String effect, boolean force) {
        if (act == null || tf == null) return;
        String str = getTimeString(act.getDate());
        try {
            HTMLDocument doc = ((HTMLDocument)main.display.getDocument());
            Element el = doc.getElement
                (doc.getDefaultRootElement(), StyleConstants.NameAttribute,
                 HTML.Tag.BODY);

            // If date changed or a forced entry occurred
            boolean oldDate = str.equals(lastDate);
            if (oldDate && !force) return;
            if (effect == null || (!oldDate && effect.length() != 0)) {
                if (!oldDate) events = new ArrayList();
                lastDate = str;
                str += (effect != null ? effect : "") + "</p>";
                // Java bug #4496801
                if (el.getElementCount() > 0)
                    doc.insertAfterEnd
                        (el.getElement(el.getElementCount()-1), str);
                else
                    doc.insertBeforeEnd
                        (el, str);
            }
            else {
                if (effect.length() == 0) return;
                doc.insertBeforeEnd
                    (el.getElement(el.getElementCount()-2), ", " + effect);
            }
        }
        catch (Exception e) {
            // Debug
            e.printStackTrace();
        }
    }

    /**
     * Get the current time string as requested by GUI.
     * @param date non-negative date
     */
    private String getTimeString(long date) {
        String str = "<p><b>";
        if (main.bDate.isSelected() && main.bTime.isSelected())
            str += tf.datetime2string(date);
        if (main.bDate.isSelected() && !main.bTime.isSelected())
            str += tf.date2string(date);
        if (!main.bDate.isSelected() && main.bTime.isSelected())
            str += tf.time2string(date % DAY);
        str += "</b>&nbsp;";
        return str;
    }

    /**
     * Check all locations, whether the active group is inside one. Also
     * remove locations left. Only do so when time frame is available.
     */
    private void checkAllLocs() {
        if (act == null || tf == null) return;
        if (!main.bLocs.isSelected()) return;

        ArrayList toCheck = new ArrayList();
        Iterator iter = allLocs.keySet().iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            Location loc = (Location)allLocs.get(o);

            // Only check existing locations
            if (loc == null) continue;
            Shape[] sh = loc.getShapes(act.getMap());
            if (sh == null || sh.length == 0) continue;

            boolean inside = false;
            // Test all shapes
            for (int i = 0; (i < sh.length) && !inside; i++)
                if (sh[i].contains(act.getX(), act.getY())) inside = true;
            
            if (inside) toCheck.add(o);
        }

        insertLocationEvent(toCheck);
    }

    /**
     * Is event type contained?
     * @return whether containstype or not
     */
    public boolean containsType(String type) { return types.contains(type); }
}
