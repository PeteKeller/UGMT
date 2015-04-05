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
package harn.chars;

import javax.swing.*;
import org.w3c.dom.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Class to hold the heraldry icon. The icon is only updated when a character
 * is loaded to the GUI.
 * @author Michael Jung
 */
class CharHeraldry extends JButton {
    /** Main reference */
    private Main main;

    /** Icon name */
    private String iconName;

    /** Test list */
    private ArrayList tests;

    /**
     * Constructor
     */
    public CharHeraldry(Main aMain, Node node, NamedNodeMap attr, JPanel panel) {
        super();
        setHorizontalAlignment(JButton.CENTER);

        main = aMain;

        // Add to parent
        GridBagConstraints constr = new GridBagConstraints();
        constr.gridx = 4;
        constr.gridheight = 4;
        constr.fill = GridBagConstraints.BOTH;
        constr.weightx = constr.weighty = 1;
        panel.add(this, constr);

        // Load tests from XML
        tests = new ArrayList();
        NodeList tree = node.getChildNodes();
        for (int i = 0; i < tree.getLength(); i++) {
            Node child = tree.item(i);
            NamedNodeMap cattr = child.getAttributes();
            if (cattr != null) {
                String test = cattr.getNamedItem("test").getNodeValue();
                String value = cattr.getNamedItem("value").getNodeValue();
                tests.add(new MyTest(test, value));
            }
        }

        // button pressed
        addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setIcon((String)null, true); // changes iconName
                    if (iconName != null && iconName.length() != 0) {
                        main.getState().setHeraldry(iconName);
                        main.getState().setDirty(true);
                    }
                }
            });

        // Create selection popup
        JPopupMenu pop = new JPopupMenu();
        ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JMenuItem item = (JMenuItem)e.getSource();
                    setIcon(item.getText(), true);
                    main.getState().setHeraldry(iconName);
                    main.getState().setDirty(true);
                }
            };

        Iterator iter = tests.iterator();
        while (iter.hasNext()) {
            JMenuItem mi = new JMenuItem(((MyTest)iter.next()).value);
            pop.add(mi);
            mi.addActionListener(al);
        }
        addMouseListener(new MyMouseAdapter(pop));
    }

    /**
     * Clear icon.
     */
    public void clear() {
        iconName = null;
        setIcon((Icon)null);
    }

    /**
     * Set the icon (and revalidate).
     */
    public void setIcon(String heraldry, boolean calc) {
        ImageIcon icon = null;
        if (heraldry != null) {
            iconName = heraldry;
            icon = new ImageIcon(main.myPath + heraldry);
        }
        else if (calc) {
            for (int i = 0; i < tests.size(); i++) {
                MyTest t = (MyTest) tests.get(i);
                if (CharAttribute.evalCond(null, t.test, main.getState())) {
                    iconName = t.value;
                    icon = new ImageIcon(main.myPath + t.value);
                    break;
                }
            }
        }
        setIcon(icon);
    }
    /**
     * Class to hold tests. Only used within CharHeraldry.
     */
    class MyTest {
        /** test string */
        String test;
        /** value if test succeeds */
        String value;
        /**
         * Constructor
         * @param aTest test
         * @param aValue value
         */
        MyTest(String aTest, String aValue) { test = aTest; value = aValue; }
    }
    /**
     * Used for starting menus.
     */
    class MyMouseAdapter extends MouseAdapter {
        /** Popuop to popup */
        JPopupMenu pop;
        /** Constructor */
        MyMouseAdapter(JPopupMenu aPop) { pop = aPop; }
        /** IF method */
        public void mousePressed(MouseEvent e) { popup(e); }
        /** IF method */
        public void mouseReleased(MouseEvent e) { popup(e); }
        /** generic method */
        private void popup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                pop.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
