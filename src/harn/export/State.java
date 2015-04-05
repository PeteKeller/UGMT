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
package harn.export;

import rpg.*;
import org.w3c.dom.*;
import java.io.*;
import javax.xml.transform.stream.StreamResult;

/**
 * State object.
 * @author Michael Jung
 */
public class State {
    /** Root reference */
    private Main main;

    /** dirty flag */
    private boolean dirty;

    /** state document */
    private Document stateDoc;

    /**
     * Constructor,
     * @param aMain root reference
     */
    State(Main aMain) {
	main = aMain;
        stateDoc = Framework.parse(main.myPath + "state.xml", "export");
        NodeList tree = stateDoc.getDocumentElement().getChildNodes();

        boolean found = false;
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            NamedNodeMap cattr = child.getAttributes();
            // Only one element!
            if (child instanceof Element) {
                found = true;
                for (int j = 0; j < main.exports.size(); j++) {
                    HttpExport exp = (HttpExport)main.exports.get(j);
                    exp.load(cattr);
                }
            }
        }
        if (!found) {
            Node child = stateDoc.createElement("entry");
            stateDoc.getDocumentElement().insertBefore(child, null);
            NamedNodeMap cattr = child.getAttributes();
            for (int j = 0; j < main.exports.size(); j++) {
                HttpExport exp = (HttpExport)main.exports.get(j);
                exp.load(cattr);
            }
        }
    }

    /** getter */
    public boolean getDirty() { return dirty; }

    /** Setter */
    public void setDirty() { dirty = true; main.bSave.setEnabled(true); }

    /** Save (GUI) state */
    public void save() {
        NodeList tree = stateDoc.getDocumentElement().getChildNodes();
	for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            // Only one element!
            if (child instanceof Element) {
                for (int j = 0; j < main.exports.size(); j++) {
                    HttpExport exp = (HttpExport)main.exports.get(j);
                    ((Element)child).removeAttribute(exp.name());
                    exp.save((Element)child);
                }
            }
        }
        // Write the document to the file
        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Framework.transform(stateDoc, new StreamResult(file), null);
        
        main.bSave.setEnabled(false);
    }
}
