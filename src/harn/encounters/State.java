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
package harn.encounters;

import harn.repository.*;
import rpg.*;
import org.w3c.dom.*;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.awt.*;
import java.util.*;

/**
 * State object. This represents the encounter states. It therefore
 * listens to "Groups" and updates the events accordingly.
 * @author Michael Jung
 */
public class State implements IListener {
    /** reference to the act object */
    CharGroup act;

    /** reference to the weather object */
    private Weather weath;

    /** reference to the weather object */
    TimeFrame cal;

    /** List of locations */
    Hashtable locs;

    /** List of sketches */
    private Hashtable notes;

    /** reference to atlanti */
    private ArrayList atlas;

    /** Root reference */
    private Main main;

    /** current display time */
    private long tDisplay = Long.MIN_VALUE;

    /** indicates whether anything is to save */
    private boolean dirty = false;

    /** Encounters already determined (group -> idx -> Event) */
    private Hashtable availEnc;

    /** Representation of the state.xml file (and changes) */
    private Document stateDoc;

    /** tomorrows to show */
    private int front;

    /** backwards to show */
    private int back;

    /** Default length of events */
    static int evlen;

    /** Suppress redraw while bulk changes */
    boolean bulkchange;

    /** List of currently valid active/inactive encounters */
    private Set actEncounters;
    private Set inactEncounters;

    /**
     * Constructor. Loads the state of the weather (predetermined). This is
     * not a simple hasmap of hashmaps.
     * @param aMain root reference
     */
    State(Main aMain) {
	main = aMain;
        atlas = new ArrayList();
        availEnc = new Hashtable();
        locs = new Hashtable();
        notes = new Hashtable();
        actEncounters = new HashSet();
        inactEncounters = new HashSet();

        bulkchange = false;

        front = Integer.parseInt(main.myProps.getProperty("tomorrows"));
        back = Integer.parseInt(main.myProps.getProperty("yesterdays"));
        evlen = Integer.parseInt(main.myProps.getProperty("encounter.length"));

        stateDoc = Framework.parse(main.myPath + "state.xml", "state");
    }

    /**
     * Determine whether a given time is within scope.
     * @param t tiem to check
     */
    public boolean isTime(int t) {
        return (t >= act.getDate() - back * Main.DAY) &&
            (t <= act.getDate() + front * Main.DAY);
    }

    /**
     * Put an event in this list according to it's last announcement.
     */
    public void put(EncounterEvent ev) {
        actEncounters.remove(ev);
        inactEncounters.remove(ev);
        if (ev.isValid() && ev.isActive())
            actEncounters.add(ev);
        if (ev.isValid() && !ev.isActive())
            inactEncounters.add(ev);
    }

    /**
     * This method drops all or invisible encounter entries, if all=false
     * those that are older than "back" or further than "front".
     * @param all all or just invisible?
     */
    void dump(boolean all) {
        // For XML update
        Node tree = stateDoc.getDocumentElement();
        NodeList children = tree.getChildNodes();

        long first = 0;
        long end = 0;
        if (act != null) {
            // first & last date to keep
	    first = act.getDate() - back * Main.DAY;
	    end = act.getDate() + front * Main.DAY;
        }

        // Note that we will keep all GuiLines in order to avoid the costly
        // reorganization of all GUI sub-tems.
        for (int i = 0; i < children.getLength(); i++) {
            NamedNodeMap attr = children.item(i).getAttributes();
            if (attr != null) {
                String name = attr.getNamedItem("date").getNodeValue();
                if (all || first > cal.string2datetime(name) ||
                    end < cal.string2datetime(name)) {
                    tree.removeChild(children.item(i));
                }
                Long idx = new Long(cal.string2datetime(name)/Main.WATCH);
                Hashtable ht = (Hashtable) availEnc.get(act.getName());
                EncounterEvent enc = (EncounterEvent) ht.get(idx);
                if (enc != null) {
                    enc.destroy();
                    ht.remove(idx);
                }
            }
        }

        // Redraw
        tDisplay = Integer.MIN_VALUE;
        inform(act);
    }

    /**
     * Use this method to save the data in this object to file. After saving
     * this object is "clean" once more.
     */
    void save() {
        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Framework.transform(stateDoc, new StreamResult(file), null);
        main.bSave.setEnabled(false);
    }
    
    /**
     * Get a location from list.
     */
    public Location getLoc(String name) { return (Location) locs.get(name); }

    /**
     * Get a sketch from list.
     */
    public Sketch getNote(String name) {
        String[] sk = name.split("/");
        // Dump leading emptiness
        Sketch note = (Sketch) notes.get(sk[1]);
        for (int i = 2; i < sk.length; i++) {
            Sketch[] subs = note.getSubSketches();
            for (int j = 0; j < subs.length; j++)
                if (subs[j].getName().equals(sk[i])) {
                    note = subs[j];
                    break;
                }
        }
        return note;
    }

    /**
     * IF method.
     */
    public void inform(IExport obj) {
        if (obj instanceof CharGroup) {
            CharGroup cg  = (CharGroup) obj;
            if (!cg.isActive()) return;
            if (act != cg && act != null) invalidateEvents();
            act = cg;
            if (cal == null) return;
            long t0 = act.getDate()/main.DAY * main.DAY;
            long t00 = act.getDate()/main.WATCH * main.WATCH;
            bulkchange = true;
            // If the new time requires an event
            for (int i = 0; i < (front + back); i++) {
                for (int j = 0; j < 6; j++) {
                    long t1 = t0 + ((i - back) * 6 + j) * Main.WATCH;
                    EncounterEvent ev = findEvent(t1/Main.WATCH);
                    if (ev == null) {
                        if (t1 < 0) continue;
                        int off = DiceRoller.d(Main.WATCH);
                        ev = new EncounterEvent
                            (main, "General", t1 + off, t1 + off + evlen, act);
                        putEvent(t1/Main.WATCH, ev);
                        main.bSave.setEnabled(true);
                        main.list.setEvent((int)t1/Main.WATCH, ev);
                        if (!ev.isFinal())
                            roll(ev.time[0], ev, ev.isForced(), ev.getTable());
                    }
                    else {
                        if (t00 == t1 && !ev.isFinal())
                            roll(ev.time[0], ev, ev.isForced(), ev.getTable());
                    }
                }
            }
            bulkchange = false;
            main.list.reorder();

            // Announce active/inactive changes
            Set delta = new HashSet();
            Iterator iter = actEncounters.iterator();
            while (iter.hasNext()) {
                EncounterEvent e = (EncounterEvent)iter.next();
                if (!e.isActive()) delta.add(e);
            }
            iter = inactEncounters.iterator();
            while (iter.hasNext()) {
                EncounterEvent e = (EncounterEvent)iter.next();
                if (e.isActive()) delta.add(e);
            }
            iter = delta.iterator();
            while (iter.hasNext()) {
                ((EncounterEvent)iter.next()).announce();
            }
        }

        if (obj instanceof Weather) {
            Weather w  = (Weather) obj;
            if (w.isActive()) weath = w;
        }
        if (obj instanceof Atlas) {
            atlas.add(obj);
        }
        if (obj instanceof TimeFrame) {
            cal = (TimeFrame) obj;
            if (act != null) inform(act);
        }
        if (obj instanceof Location) {
            Location loc = (Location) obj;
            if (loc.isValid())
                locs.put(loc.getName(), loc);
            else
                locs.remove(loc.getName());
        }
        if (obj instanceof Sketch) {
            Sketch sk = (Sketch) obj;
             if (sk.isValid())
                notes.put(sk.getName(), sk);
            else
                notes.remove(sk.getName());
        }
    }

    /**
     * Put an event into the internal list and into the state file
     * representation.
     * @param idx time idx
     * @param ev event to note 
     */
    private void putEvent(long idx, EncounterEvent ev) {
        Hashtable ht = (Hashtable) availEnc.get(act.getName());
        if (ht == null) {
            ht = new Hashtable();
            availEnc.put(act.getName(), ht);
        }
        ht.put(new Long(idx), ev);

        // Get parent in DOM document
        Node parent = stateDoc.getDocumentElement();

        // Put into DOM document
        Element nn = stateDoc.createElement("item");
        parent.appendChild(nn);
        ev.emptyStateNode(nn);
    }

    /**
     * Invalidate all events. Done when active groups change.
     */
    private void invalidateEvents() {
        Hashtable ht = (Hashtable) availEnc.get(act.getName());
        if (ht == null) return;
        Iterator iter = ht.keySet().iterator();
        while (iter.hasNext())
            ((EncounterEvent)ht.get(iter.next())).destroy();
    }

    /**
     * Find an event.
     * @param idx time idx
     */
    private EncounterEvent findEvent(long idx) {
        Hashtable ht = (Hashtable) availEnc.get(act.getName());
        if (ht == null) {
            ht = new Hashtable();
            availEnc.put(act.getName(), ht);
        }
        EncounterEvent ev = (EncounterEvent) ht.get(new Long(idx));
        if (ev != null) {
            if (!ev.isValid()) ev.setValid();
            main.list.setEvent((int)idx, ev);
            return ev;
        }

        // Get parent in DOM document
        NodeList children = stateDoc.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            NamedNodeMap attr = node.getAttributes();
            if (attr != null) {
                String date = attr.getNamedItem("date").getNodeValue();
                String group = attr.getNamedItem("group").getNodeValue();
                if (cal.string2datetime(date) / Main.WATCH == idx &&
                    group.equals(act.getName())) {
                    long time = cal.string2datetime(date);
                    long len = evlen;
                    Node lenNode = attr.getNamedItem("length");
                    if (lenNode != null)
                        len = cal.string2datetime(lenNode.getNodeValue());
                    ev = new EncounterEvent
                        (main, null, time, time + len, act);
                    ev.setStateNode(node);
                    ht.put(new Long(idx), ev);
                    main.list.setEvent((int)idx, ev);
                    return ev;
                }
            }
        }
        return null;
    }

    /**
     * Get current terrain.
     */
    public String getTerrain() {
        if (act == null || atlas == null) return null;
        for (int i = 0; i < atlas.size(); i++) {
            ScaledMap map = ((Atlas)atlas.get(i)).getMap(act.getMap());
            if (map != null)
                return map.getTerrain(act.getX(), act.getY());
        }
        return null;
    }

    /**
     * Get current vegetation.
     */
    public String getVegetation() {
        if (weath == null || act == null || atlas == null) return null;
        for (int i = 0; i < atlas.size(); i++) {
            ScaledMap map = ((Atlas)atlas.get(i)).getMap(act.getMap());
            if (map != null)
                return map.getVegetation(act.getX(), act.getY());
        }
        return null;
    }

    /**
     * Get current weather condition.
     */
    public String getWeatherCondition() {
        if (weath == null || act == null || atlas == null) return null;
        return weath.condition(act.getMap(), act.getX(), act.getY(), act.getDate());
    }

    /**
     * Get current weather event.
     */
    public String getWeatherEvent() {
        if (weath == null || act == null || atlas == null) return null;
        return weath.event(act.getMap(), act.getX(), act.getY(), act.getDate());
    }

    /**
     * Get current daylight (or not).
     */
    public String getLight() {
        if (act == null) return null;
        long morn = cal.getDawn(act.getDate());
        long eve = cal.getDusk(act.getDate());
        if (morn <= act.getDate() && eve > act.getDate())
            return "day";
        return "night";
    }

    /**
     * Test current position for location.
     */
    public String getLocation(String map) {
        if (act == null || atlas == null) return null;
        if (!act.getMap().equals(map)) return null;
        Location loc = (Location) locs.get(map);
        Shape[] sh = loc.getShapes(map);
        for (int i = 0; i < sh.length; i++)
            if (sh[i].contains(act.getX(), act.getY())) return map;
        return null;
    }

    /**
     * Convert date to string representation.
     */
    public String datetime2string(long date) { return cal.datetime2string(date); }

    /**
     * Convert time to string representation.
     */
    public String time2string(long date) { return cal.time2string(date); }

    /**
     * Roll an encounter.
     * @param time time of event
     * @param ev event as already present; to be modified
     * @param force roll in spite of locale conditions?
     * @param table node to roll on
     */
    public void roll(long time, EncounterEvent ev, boolean force, String parent) {
        if (parent == null) return;
        if (parent.equals("General")) ev.resetPrefix();

        int sum = 0;
        ArrayList rand = new ArrayList();
        ArrayList tables = new ArrayList();
        boolean local = false;

        // Prepare table
	NodeList tree = findTable(parent).getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
            if (tree.item(i).getNodeName().equals("entry")) {
                // Check condition
                Node cond = tree.item(i).getAttributes().getNamedItem("cond");
                Node weight = tree.item(i).getAttributes().getNamedItem("weight");
                if (weight != null && cond != null) {
                    if (!evalCond(cond.getNodeValue()))
                        weight = null;
                    local |= evalLocal(cond.getNodeValue());
                    if (local && !force) break;
                }

                // Add to list
                if (weight != null) {
                    int add = Integer.parseInt(weight.getNodeValue());
                    sum += add;
                    rand.add(new Integer(sum));
                    tables.add(tree.item(i));
                }
            }
        }
        if (rand.size() == 0) return;

        if (!local || force) {
            // Roll
            //TODO: FIX NEXT LINE AS WELL
            int r =
                DiceRoller.d(((Integer)rand.get(rand.size() - 1)).intValue());
            int l = 0;
            while (((Integer)rand.get(l)).intValue() < r && l < rand.size())
                l++;
            Node node = (Node) tables.get(l);

            // Adjust encounter
            ev.setDataNode(node);

            // Recursive
            Node attr = node.getAttributes().getNamedItem("tableref");
            if (attr != null) {
                roll(time, ev, force, attr.getNodeValue());
            }
            // Announce, if first
            if (parent.equals("General")) ev.announce();
        }
    }

    /**
     * Find a table (node).
     * @param name name of table
     * @return table
     */
    public Node findTable(String name) { return main.myData.findTable(name); }

    /**
     * Test a condition for independence of local. Only dependence on time
     * attributes will continue rolling a random encounter.
     * @param condlist conditions to test
     * @return is locale important
     */
    public boolean evalLocal(String condlist) {
        String[] cond = condlist.split("[|]");
        for (int i = 0; i < cond.length; i++) {
            String[] sub = cond[i].split("[=!]");
            if (sub[0].equals("terrain") ||
                sub[0].equals("vegetation") ||
                sub[0].equals("location")) return true;
        }
        return false;
    }

    /**
     * Way to test a condition. Parses formulas of types: "a=b", and "a!b",
     * which can be "and"ed through "|".  "a" is taken to be a current
     * condition, "b" a value for such a condition.
     * @param condlist conditions to test
     */
    public boolean evalCond(String condlist) {
        boolean met = true;
        if (condlist == null) return met;

        String[] cond = condlist.split("[|]");
        for (int i = 0; (i < cond.length) && met; i++) {
            String[] sub = cond[i].split("[=!]");

            String test = null;
            if (sub[0].equals("terrain")) test = getTerrain();
            else if (sub[0].equals("vegetation")) test = getVegetation();
            else if (sub[0].equals("condition")) test = getWeatherCondition();
            else if (sub[0].equals("weather")) test = getWeatherEvent();
            else if (sub[0].equals("time")) test = getLight();
            else if (sub[0].equals("location")) test = getLocation(sub[1]);
            if (cond[i].indexOf("=") > -1)
                met &= (sub[1].equals(test));
            else // cond[i].indexOf("!") > -1
                met &= (!sub[1].equals(test));
        }
        return met;
    }
}
