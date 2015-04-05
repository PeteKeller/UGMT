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
package harn.sound;

import java.util.*;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import rpg.*;

/**
 * State object. This represents the state of the mixer except the GUI.  It
 * updates all simple GUI objects.
 * @author Michael Jung
 */
public class State {
    /** Root reference */
    private Main main;

    /** indicates whether anything is to save */
    private boolean dirty = false;

    /** Representation of the state.xml file (and changes) */
    private Document stateDoc;

    /** List of all sound lines */
    LinkedList list;

    /**
     * Constructor,
     * @param aMain root reference
     */
    State(Main aMain) {
	main = aMain;
        list = new LinkedList();
        stateDoc = Framework.parse(main.myPath + "state.xml", "state");
        list = loadSoundsFromDoc();
    }

    /**
     * Load the sounds.
     * @return hashtable containing the groups.
     */
    private LinkedList loadSoundsFromDoc() {
	LinkedList ret = new LinkedList();

	NodeList tree = stateDoc.getDocumentElement().getChildNodes();
	for (int i = 0; tree != null && i < tree.getLength(); i++) {
	    Node node = tree.item(i);
	    NamedNodeMap attr = node.getAttributes();
	    if (attr != null) {
		// Get attributes
		String sound = attr.getNamedItem("name").getNodeValue();
		String cnd1 = Framework.getXmlAttrValue(attr, "primary", null);
		String cnd2 = Framework.getXmlAttrValue(attr, "secondary", null);
		String loop = Framework.getXmlAttrValue(attr, "loop", "yes");
		String gain = attr.getNamedItem("gain").getNodeValue();
                String play = Framework.getXmlAttrValue(attr, "play", "no");
                String pause = Framework.getXmlAttrValue(attr, "pause", "no");
                GuiLine gl = new GuiLine
                    (main.mixer, play.equals("yes"), sound, cnd1, cnd2,
                     Integer.parseInt(gain), loop.equals("yes"), pause.equals("yes"), (Element) node);
	    }
	}
	return ret;
    }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * is accessible by this method.
     * @return dirty flag
     */
    boolean getDirty() { return dirty; }

    /**
     * Is there anything to save? This is indicated by the dirty flag, which
     * can be changed here. The save button is enabled
     */
    void setDirty(GuiLine gl) {
	dirty = true;
	if (main.bSave != null)
	    main.bSave.setEnabled(true);
        if (gl == null) return;

        Element n = gl.myNode;
        n.setAttribute("name", (String)gl.soundlist.getValue());
	if (gl.oldC1 != null && gl.oldC1.length() != 0) {
            n.setAttribute("primary", gl.oldC1);
            n.setAttribute("secondary", gl.oldC2);
        }
        float gain = gl.gain.getValue()/(float)(gl.gain.getMaximum());
        n.setAttribute("gain", Integer.toString((int)(gain*100)));
        n.setAttribute("play", gl.play.isSelected()?"yes":"no");
        n.setAttribute("pause", gl.pause.isSelected()?"yes":"no");
	if (gl.loop != null) n.setAttribute("loop", (gl.loop.isSelected()?"yes":"no"));
    }

    /**
     * Use this method to save the data in this object to file. AFter saving
     * this object is "clean" once more.
     */
    void save() {
        try {
            File file = new File(main.myPath + "state.xml");
            Framework.backup(main.myPath + "state.xml");
            Result result = new StreamResult(file);

            // Write the document to the file
	    Framework.transform(stateDoc, result, null);
            main.bSave.setEnabled(false);
            dirty = false;
        }
	catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }

    /**
     * Add a node that corresponds to a GUI line
     * @param gl line that was added
     */
    Element addNode(GuiLine gl) {
        // Adjust DOM structures
        Node parent =  stateDoc.getDocumentElement();

        // Put item
        Element nn = stateDoc.createElement("sound");
        parent.insertBefore(nn, parent.getLastChild());

        nn.setAttribute("name", (String)gl.soundlist.getValue());
	if (gl.oldC1 != null && gl.oldC1.length() != 0) {
            nn.setAttribute("primary", gl.oldC1);
            nn.setAttribute("secondary", gl.oldC2);
        }
        float gain = gl.gain.getValue()/(float)(gl.gain.getMaximum());
        nn.setAttribute("gain", Integer.toString((int)(gain*100)));
	if (gl.loop != null) nn.setAttribute("loop", (gl.loop.isSelected()?"yes":"no"));

        return nn;
    }

    /**
     * Remove a node that corresponds to a GUI line
     * @param guiline line that was removed
     */
    void removeNode(GuiLine guiline) {
        if (guiline == null) return;

        Node parent =  stateDoc.getDocumentElement();
        parent.removeChild(guiline.myNode.getPreviousSibling());
        parent.removeChild(guiline.myNode);
    }
}
