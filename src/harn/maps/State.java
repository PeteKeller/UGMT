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
package harn.maps;

import rpg.*;
import harn.repository.CharGroup;
import org.w3c.dom.*;
import java.util.StringTokenizer;
import java.io.*;
import javax.xml.transform.stream.StreamResult;

/**
 * State object. This contains the active group reference.  It also listens to
 * "Groups" and applies any changes to such objects. It updates all simple GUI
 * objects.
 * @author Michael Jung
 */
public class State implements IListener {
    /** Root reference */
    Main main;

    /** reference to the active object */
    CharGroup act;

    /** dirty flag */
    boolean dirty;

    Document stateDoc;

    /**
     * Constructor,
     * @param aMain root reference
     */
    State(Main aMain) {
	main = aMain;
        stateDoc = Framework.parse(main.myPath + "state.xml", "maps");
        NodeList tree = stateDoc.getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            NamedNodeMap cattr = child.getAttributes();
            if (child instanceof Element) {
                main.bSuppExp.setSelected
                    (cattr.getNamedItem("export") != null);
                main.bFrames.setSelected
                    (cattr.getNamedItem("frames") != null);
                main.bFullInfo.setSelected
                    (cattr.getNamedItem("info") != null);
                main.bShowFog.setSelected
                    (cattr.getNamedItem("fog") != null);
            }
        }
        main.bSave.setEnabled(false);
    }

    /** getter */
    public boolean getDirty() { return dirty; }

    /** Setter */
    public void setDirty() { dirty = true; main.bSave.setEnabled(true); }

    /** Save (GUI) state */
    public void save() {
        NodeList tree = stateDoc.getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child instanceof Element) {
                ((Element)child).removeAttribute("export");
                ((Element)child).removeAttribute("frames");
                ((Element)child).removeAttribute("info");
                ((Element)child).removeAttribute("fog");
                if (main.bSuppExp.isSelected())
                    ((Element)child).setAttribute("export", "true");
                if (main.bFrames.isSelected())
                    ((Element)child).setAttribute("frames", "true");
                if (main.bFullInfo.isSelected())
                    ((Element)child).setAttribute("info", "true");
                if (main.bShowFog.isSelected())
                    ((Element)child).setAttribute("fog", "true");
            }
        }
        // Write the document to the file
        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Framework.transform(stateDoc, new StreamResult(file), null);
        
        main.bSave.setEnabled(false);
    }

    /**
     * This method is called by the framework to inform about a changed or new
     * object. It initiates a full redraw of the GUI if the active object
     * changed.
     * @param obj the changed or new object
     */
    public void inform(IExport obj) {
	CharGroup cg  = (CharGroup) obj;
	if (cg.isActive()) {
	    if (act != cg) {
		// Set new state
		act = cg;
                //main.map.setIcons(act.getMap(), true);
	    }
	}
	// Check all border regions for overlapping
	NodeList nl = main.myData.getNodeList(cg.getMap());
	for (int i = 0; nl != null && i < nl.getLength(); i++) {
	    // Check for borders
	    NamedNodeMap attr = nl.item(i).getAttributes();
	    if (nl.item(i).getNodeName().equals("border")) {
		int x = cg.getX();
		int y = cg.getY();

		// Check if in this border region
		String x1s = attr.getNamedItem("x1").getNodeValue();
		int x1 = Integer.parseInt(x1s);
		if (x < x1) continue;
		String x2s = attr.getNamedItem("x2").getNodeValue();
		int x2 = Integer.parseInt(x2s);
		if (x > x2) continue;
		String y1s = attr.getNamedItem("y1").getNodeValue();
		int y1 = Integer.parseInt(y1s);
		if (y < y1) continue;
		String y2s = attr.getNamedItem("y2").getNodeValue();
		int y2 = Integer.parseInt(y2s);
		if (y > y2) continue;
		
		// Get transform in border region
		String trf = attr.getNamedItem("transform").getNodeValue();
		StringTokenizer tok = new StringTokenizer(trf, ";");
		float a1 = Float.parseFloat(tok.nextToken());
		float b1 = Float.parseFloat(tok.nextToken());
		float c1 = Float.parseFloat(tok.nextToken());
		float a2 = Float.parseFloat(tok.nextToken());
		float b2 = Float.parseFloat(tok.nextToken());
		float c2 = Float.parseFloat(tok.nextToken());
		
		// Apply transform
		int xn = (int)(a1 * x + b1 * y + c1);
		int yn = (int)(a2 * x + b2 * y + c2);
		String mn = attr.getNamedItem("map").getNodeValue();

		// Full name
		Node parent = nl.item(i).getParentNode();
                while (!parent.getNodeName().equals("atlas"))
                    parent = parent.getParentNode();
		attr = parent.getAttributes();
		String atlas = attr.getNamedItem("name").getNodeValue();
		String fullName = atlas + "/" + mn;

		// Adjust chargroup (if map exists locally)
                if (main.myData.getMap(fullName) != null)
                    cg.setLocation(fullName, xn, yn, cg.getDate());
                else {
                    // Adjust chargroup (if map exists globally)
                    if (main.myData.getMap(mn) != null)
                        cg.setLocation(mn, xn, yn, cg.getDate());
                }

		// That's it; don't test for more borders
		break;
	    }
	}
    }

    /**
     * Return the active group for direct access.
     * @return active chargroup
     */
    CharGroup getActive() { return act; }

    /**
     * Set the coordinates and map of the given chargroup. If null, the
     * current map is centered.
     * @param group group to set the map for
     */
    void setCurrent(CharGroup group) {
	int x,y;

	if (group != null) {
	    main.map.setIcons(group.getMap(), true);

	    // Get position from group
            int scale = ((Integer)main.bScaleSpin.getValue()).intValue();
	    x = (group.getX() * scale / 100);
	    y = (group.getY() * scale / 100);
	}
	else {
	    // Get center
	    x = main.map.getPreferredSize().width/2;
	    y = main.map.getPreferredSize().height/2;
	}
	// Redraw
	main.setViewport(x, y);
    }
}
