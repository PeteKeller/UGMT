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
import org.w3c.dom.*;
import javax.swing.*;
import java.util.Hashtable;

/**
 * The encounter object that is announced. An encounter may be incomplete, then the
 * variable "tableref" holds the currently valid table.  Once the encounter
 * has been completed, one of "index", "note", or "name" will show a non-null
 * answer. Exactly one of the four is always non-null, unless the event is a non-event.
 * Validity is false, if this event is for another group.
 * @author Michael Jung
 */
public class EncounterEvent extends TimeEvent {
    /** Main reference */
    private Main main;

    /** table reference */
    private String tableref;

    /** index reference */
    private String index;

    /** notes reference */
    private String note;

    /** name */
    private String name;

    /** prefix */
    private String prefix;

    /** from/to */
    protected long[] time;

    /** Validity flag; invalid is dead */
    private boolean valid = true;

    /** State node reference */
    private Element node;

    /** Group for which this holds */
    private CharGroup act;

    /**
     * Constructor with table and group.
     */
    EncounterEvent(Main aMain, String aTable, long from, long to, CharGroup group) {
        main = aMain;
        tableref = aTable;
        time = new long[] { from, to };
        act = group;
        valid = true;
        prefix = "";
        announce();
    }

    /** Destroy (destructor) */
    public void destroy() { valid = false; announce(); }

    /** IF method */
    public boolean isValid() { return valid; }

    /** set validity */
    public void setValid() { valid = true; announce(); }

    /** Get table */
    public String getTable() { return tableref; }

    /** Reset prefix */
    public void resetPrefix() { prefix = ""; }

    /** Announce, if non-null */
    public void announce() {
        main.myFrw.announce(this);
        main.myState.put(this);
    }

    /**
     * Setting an entry from a state.xml or data.xml entry.
     * @param attr the attributes from the entry
     */
    public void setStateNode(Node aNode) {
        node = (Element) aNode;
        NamedNodeMap attr = node.getAttributes();

        if (attr.getNamedItem("tableref") != null)
            tableref = attr.getNamedItem("tableref").getNodeValue();
        if (attr.getNamedItem("prefix") != null)
            prefix = attr.getNamedItem("prefix").getNodeValue();
        if (attr.getNamedItem("name") != null)
            name = attr.getNamedItem("name").getNodeValue();
        if (attr.getNamedItem("note") != null)
            note = attr.getNamedItem("note").getNodeValue();
        if (attr.getNamedItem("index") != null)
            index = attr.getNamedItem("index").getNodeValue();

        announce();
    }

    /**
     * Setting an entry from a data.xml entry.
     * @param aNode the attributes from the entry
     */
    public void setDataNode(Node aNode) {
        NamedNodeMap attr = aNode.getAttributes();

        name = null;
        note = null;
        index = null;
        tableref = null;
        node.removeAttribute("prefix");
        node.removeAttribute("note");
        node.removeAttribute("index");
        node.removeAttribute("tableref");

        if (attr.getNamedItem("tableref") != null) {
            tableref = attr.getNamedItem("tableref").getNodeValue();
            node.setAttribute("tableref", tableref); 
        }
        if (attr.getNamedItem("prefix") != null) {
            prefix += attr.getNamedItem("prefix").getNodeValue();
        }
        if (attr.getNamedItem("name") != null) {
            name = prefix + attr.getNamedItem("name").getNodeValue();
            node.setAttribute("name", name); 
        }
        if (attr.getNamedItem("note") != null) {
            note = attr.getNamedItem("note").getNodeValue();
            node.setAttribute("note", note); 
        }
        if (attr.getNamedItem("index") != null) {
            index =  attr.getNamedItem("index").getNodeValue();
            node.setAttribute("index", index); 
        }

        main.bSave.setEnabled(true);
        announce();
    }

    /**
     * Setting the current attributes into the node (and keep the node).
     * @param attr the attributes from the entry
     */
    public void emptyStateNode(Node aNode) {
        node = (Element) aNode;
        node.setAttribute("group", act.getName());
        node.setAttribute("date", main.myState.datetime2string(time[0]));
        node.setAttribute
            ("length", main.myState.time2string(time[1] - time[0]));

        if (tableref != null)
            node.setAttribute("tableref", tableref);
        if (name != null)
            node.setAttribute("name", name);
        if (note != null)
            node.setAttribute("note", note);
        if (index != null)
            node.setAttribute("index", index);
        if (prefix != null)
            node.setAttribute("prefix", prefix);
    }

    /**
     * Is this event finalized.
     * @param group name of group to check
     * @return finalized
     */
    public boolean isFinal() {
        return (name != null || note != null || index != null) ||
            (name == null && note == null && index == null && tableref == null);
    }

    /**
     * Is this event finalized or incomplete?
     * @param group name of group to check
     * @return finalized
     */
    public boolean isEnabled() {
        return (name != null || note != null || index != null || tableref != null);
    }

    /**
     * Get type (defaults to "name).
     */
    public String getType0() {
        if (note != null) return "note";
        if (index != null) return "index";
        return "name";
    }

    /**
     * Set a value for a group.
     */
    public void setValue(String type, String value) {
        name = null;
        note = null;
        index = null;
        tableref = null;
        prefix = "";
        node.removeAttribute("name");
        node.removeAttribute("note");
        node.removeAttribute("index");
        node.removeAttribute("tableref");
        node.removeAttribute("prefix");

        if (value == null || value.length() == 0) {
            announce();
            main.bSave.setEnabled(true);
            return;
        }

        if (type.equals("name")) {
            name = value;
            node.setAttribute("name", value);
        }
        if (type.equals("note")) {
            note = value;
            node.setAttribute("note", value);
        }
        if (type.equals("index")) {
            index = value;
            node.setAttribute("index", value);
        }

        main.bSave.setEnabled(true);
        announce();
    }

    /**
     * Set time. to >= from >= 0
     */
    protected void setTime(long from, long to) {
        time[0] = from; time[1] = to;
        if ((node != null) && (to - from != State.evlen))
            node.setAttribute
                ("length", main.myState.time2string(to - from));
        announce();
    }

    /** Should be forced */
    public boolean isForced() {
        return (act.getDate() >= time[0] &&
                act.getDate() <= time[1]);
    }

    /** IF method TimeEvent */
    public String getName() {

        if (note != null)
            return note.substring(note.lastIndexOf("/") + 1);
        if (index != null)
            return index.substring(index.lastIndexOf("/") + 1);
        if (name != null)
            return name;

        return "???";
    }

    /** IF method TimeEvent */
    public String type() { return "Encounter"; }

    /** IF method TimeEvent */
    public boolean isActive() {
        if (!valid) return false;
        if (name == null && note == null &&
            tableref == null && index == null)
            return false;
        return (act.getDate() >= time[0] &&
                act.getDate() <= time[1]);
    }

    /** IF method TimeEvent */
    public float[] isActive(long from, long to) {
        // Prerequisites
        if (!valid) return null;

        if (name == null && note == null && tableref == null && index == null)
            return null;

        if (to < time[0]) return null;

        // from = to
        if (from == to) {
            if (time[1] >= to && time[0] <= from) return new float[]{0,1};
            return null;
        }

        // [t0,t1] intersect [f,t] != {}
        float f0 = ((float)(time[0] - from))/(to - from);
        float f1 = ((float)(time[1] - from))/(to - from);
        if (f0 < 0) f0 = 0;
        if (f1 > 1) f1 = 1;
        if (f1 < 0) return null;

        return new float[] { f0, f1 };
    }

    /** Get Note */
    public String getNote() { return note; }

    /** Get Index */
    public String getIndex() { return index; }

    /** Get Name */
    public String getName0() { return name; }

    /** IF method TimeEvent */
    public String details() {
        if (!valid) return null;
        main.myFrw.raisePlugin("Encounters");

        if (note != null)
            main.myState.getNote(note).setSelected();
        if (index != null)
            main.myState.getLoc(index).setSelected();
        // Name must be last
        if (name != null)
            main.myDisplay.setText(name);
        return null;
    }

    /** IF method TimeEvent */
    public ImageIcon getIcon() { return null; }
}
