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
package harn.magic;

import harn.repository.Char;
import java.io.*;
import java.util.*;
import javax.swing.JPanel;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import rpg.*;

/**
 * The main class for the export plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class State implements IListener {
    /** Holds all item holders */
    protected Hashtable holderList;

    /** Main reference */
    private Main main;

    /** Spell caster constant */
    private String[] spellers = { "Cleric", "Priest", "Shaman", "Mage", "Shek P'var" };

    /** Statefiles already merged */
    private String merged;

    /**
     * Constructor
     */
    public State(Main aMain) {
        main = aMain;
        holderList = new Hashtable();
        merged = "state.xml";
        Document stateDoc = Framework.parse(main.myPath + "state.xml", "magic");
        loadDoc(stateDoc.getDocumentElement());

        String[] charl = Framework.getStateFiles(main.myPath, merged);
        for (int i = 1; charl != null && i < charl.length; i++) {
            stateDoc = Framework.parse(main.myPath + charl[i], "magic");
            loadDoc(stateDoc.getDocumentElement());
        }
    }

    /**
     * Method to load the state.
     * @param doc (XML) document to load
     */
    private void loadDoc(Node doc) {
        NodeList tree = doc.getChildNodes();
        for (int i = 0; i < tree.getLength(); i++) {
            Node child = tree.item(i);
            NamedNodeMap attr = child.getAttributes();
            if (child.getNodeName().equals("char")) {
                String name = attr.getNamedItem("name").getNodeValue();
                Holder holder = new Holder(null, name, main);
                holderList.put(name, holder);
                NodeList children = child.getChildNodes();
                for (int j = 0; children != null && j < children.getLength(); j++) {
                    Node child1 = children.item(j);
                    attr = child1.getAttributes();
                    if (attr != null) {
                        String spname =
                            Framework.getXmlAttrValue(attr, "name", null);
                        String ml = Framework.getXmlAttrValue(attr, "ml", "0");
                        String adj1 = Framework.getXmlAttrValue(attr, "adj1", "");
                        String adj2 = Framework.getXmlAttrValue(attr, "adj2", "");
                        String adj3 = Framework.getXmlAttrValue(attr, "adj3", "");

                        Spell sp = new Spell(holder, spname, ml, adj1, adj2, adj3, main);
                        main.getModel().insertNodeSorted(sp, holder);
                    }
                }
            }
            if (child.getNodeName().equals("merged") &&
                attr.getNamedItem("list") != null) {
                merged = attr.getNamedItem("list").getNodeValue();
            }
        }
    }

    /**
     * Interface to listen for groups and chars
     * @param obj informed object
     */
    public void inform(IExport obj) {
        String occ = ((Char) obj).getAttribute("Own Occupation");
        if (occ == null) return;
        boolean speller = false;
        for (int i = 0; i < spellers.length; i++) {
            if (occ.indexOf(spellers[i]) > -1) {
                speller = true;
                break;
            }
        }
        if (!speller) return;

        Holder h = (Holder) holderList.get(obj.getName());
        if (h == null)
            h = new Holder((Char)obj, obj.getName(), main);
        else {
            h.mychar = (Char)obj;
            for (int i = 0; i < h.getChildCount(); i++)
                main.announce((Spell)h.getChildAt(i));
        }
        holderList.put(obj.getName(), h);       
    }

    /**
     * Save the stuff
     */
    public void save() {
        if (!main.bSave.isEnabled()) return;

        Document stateDoc = Framework.newDoc();
        Node root = stateDoc.createElement("magic");
        stateDoc.insertBefore(root, stateDoc.getLastChild());
        Framework.setMergedStates(stateDoc, main.myPath, merged);

        Iterator iter1 = holderList.keySet().iterator();
        while (iter1.hasNext()) {
            String mychar = (String) iter1.next();
            Holder holder = (Holder) holderList.get(mychar);

            Element celem = stateDoc.createElement("char");
            celem.setAttribute("name", mychar);
            root.insertBefore(celem, null);

            for (int i = 0; i < holder.getChildCount(); i++) {
                Spell spell = (Spell) holder.getChildAt(i);

                Element elem = stateDoc.createElement("spell");
                celem.insertBefore(elem, null);
                elem.setAttribute("name", spell.name());
                elem.setAttribute("ml", spell.getML());
                elem.setAttribute("adj1", spell.adj1);
                elem.setAttribute("adj2", spell.adj2);
                elem.setAttribute("adj3", spell.adj3);

                celem.insertBefore(elem, null);
            }
        }

        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Framework.transform(stateDoc, new StreamResult(file), null);

        main.bSave.setEnabled(false);
    }
}
