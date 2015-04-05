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
package harn.equipment;

import javax.swing.tree.*;
import java.io.*;
import java.util.*;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import rpg.*;

/**
 * The main class for the export plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class State implements IListener {
    /** Main reference */
    private Main main;

    /** Statefiles already merged */
    private String merged;

    /**
     * Constructor
     */
    public State(Main aMain) {
        main = aMain;
        Document stateDoc = Framework.parse(main.myPath + "state.xml", "items");
        merged = "state.xml";
        loadDoc(stateDoc.getDocumentElement());

        String[] charl = Framework.getStateFiles(main.myPath, merged);
        for (int i = 1; charl != null && i < charl.length; i++) {
            stateDoc = Framework.parse(main.myPath + charl[i], "items");
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
            if (child.getNodeName().equals("merged") &&
                attr.getNamedItem("list") != null) {
                merged = attr.getNamedItem("list").getNodeValue();
            }
        }

        for (int i = 0; i < tree.getLength(); i++) {
	    Node child = tree.item(i);
            if (child.getNodeName().equals("item")) {
                NamedNodeMap attr = child.getAttributes();
                if (attr != null) {
                    String name = attr.getNamedItem("name").getNodeValue();
                    SortedTreeModel model;
                    if (attr.getNamedItem("main") != null &&
                        attr.getNamedItem("main").getNodeValue().equals("true"))
                        model = (SortedTreeModel)main.treetable.getTree().getModel();
                    else
                        model = (SortedTreeModel)main.remainder.getTree().getModel();
                    Item.create
                        (true, name, child, main, model, (DefaultMutableTreeNode)model.getRoot());
                }
            }
        }
    }

    /**
     * Interface to listen for groups and chars
     * @param obj informed object
     */
    public void inform(IExport obj) {
        if (!Item.find(obj.getName())) {
            SortedTreeModel model = (SortedTreeModel)main.remainder.getTree().getModel();
            Item.create
                (false, obj.getName(), null, main, model, (DefaultMutableTreeNode)model.getRoot());
        }
    }

    /**
     * Save the stuff
     */
    public void save() {
        // Create technical objects
        Document stateDoc = Framework.newDoc();
        Element father = stateDoc.createElement("items");
        stateDoc.appendChild(father);

        Framework.setMergedStates(stateDoc, main.myPath, merged);

        // Normal list
        Enumeration children =
            ((DefaultMutableTreeNode)main.treetable.getTree().getModel().getRoot()).children();
        while (children.hasMoreElements())
            save(stateDoc, (Item)children.nextElement(), father, true);

        // Hidden list
        children =
            ((DefaultMutableTreeNode)main.remainder.getTree().getModel().getRoot()).children();
        while (children.hasMoreElements())
            save(stateDoc, (Item)children.nextElement(), father, false);

        // Write the document to the file
        File file = new File(main.myPath + "state.xml");
        Framework.backup(main.myPath + "state.xml");
        Framework.transform(stateDoc, new StreamResult(file), null);
    }

    /**
     * Internal save
     */
    private void save(Document stateDoc, Item item, Element father, boolean inmain) {
        Element child = stateDoc.createElement("item");
        child.setAttribute("name", item.getName());
        child.setAttribute("main", Boolean.toString(inmain));
        if (item.getSelfWeight() != null) {
            child.setAttribute("weight", item.getSelfWeight());
        }
        father.insertBefore(child, null);

        for (int i = 0; i < item.getChildCount(); i++) {
            save(stateDoc, (Item) item.getChildAt(i), child, inmain);
        }
    }
}
