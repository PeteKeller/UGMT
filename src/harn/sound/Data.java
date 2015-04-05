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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.*;

import org.w3c.dom.*;

import rpg.*;

/**
 * Data object. Here we collect all our data.
 * @author Michael Jung
 */
public class Data {
    /** Root reference */
    private Main main; 

    /** List of all sound files */
    private TreeSet allSource;

    /** Default sound */
    static public String defSound;

    /** List of weather keys */
    private ArrayList weathKeys;

    /**
     * Constructor.
     * @param aMain root reference
     * @param frw Framework reference for parsing the data file
     */
    Data(Main aMain, Framework frw) {
	main = aMain;
        weathKeys = new ArrayList();

        // All possible outcomes
        allSource = new TreeSet(Collator.getInstance(Locale.FRANCE));

        // All sounds
        File dir = new File(main.mySoundPath);
        getSubDir(dir.listFiles(), "");

        // All keys
        Document doc = Framework.parse(main.myPath + "data.xml", "sound");
        getDataKeys(doc.getDocumentElement().getChildNodes());
    }

    private void getSubDir(File[] all, String prefix) {
        for (int i = 0; i < all.length; i++) {
            if (all[i].isDirectory()) {
                getSubDir(all[i].listFiles(), prefix + all[i].getName() + "/");
            }
            else {
                allSource.add(prefix + all[i].getName());
                if (defSound == null)
                    defSound = prefix + all[i].getName();
            }
        }
    }

    /**
     * Parses a branch on the XML-tree.
     * @param tree current branch to parse
     * @param name current name on the stack
     */
    private void getDataKeys(NodeList tree) {
        for (int i = 0; tree != null && i < tree.getLength(); i++) {
            NamedNodeMap attr = tree.item(i).getAttributes();
            if (attr != null) {
                weathKeys.add(attr.getNamedItem("value").getNodeValue());
            }
        }
    }

    /**
     * Accessor to the list of sound files. Also add a delet item.
     * @return sound file list
     */
    JPopupMenu getSoundFiles(final GuiLine gl, final LineMixer lm) {
        // Create selection popup
        JPopupMenu pop = new JPopupMenu();
        
        ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MyMenuItem item = (MyMenuItem)e.getSource();
                    String txt =
                        (item.heritage.length() != 0 ? item.heritage + "/" : "") + item.getText();
                    lm.setSoundListText
                        (true, (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0, gl, txt);
                }
            };

        JMenuItem mi = new JMenuItem("<html><i>Delete</i>");
        pop.add(mi);
        pop.addSeparator();
        mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    lm.setSoundListText(false, false, gl, null);
                }
            });

        Iterator iter = allSource.iterator();
        while (iter.hasNext()) {
            String[] split = ((String)iter.next()).split("/");
            mi = null;
            for (int i = 0; i < split.length; i++) {
                JMenuItem menu = getMenuItem
                    (pop, (MyMenu)mi, split[i], i == split.length-1);
                mi = menu;
            }
            mi.addActionListener(al);
        }
        return pop;
    }

    /**
     * Accessor to the list of keys to which a sound can be tied.  We accept
     * only (hard-coded) "Location", "Event", and "Weather".  See also the
     * SoundControl.java method inform.
     * @return key list
     */
    String[] getKeys() {
        return new String[] { "Location", "Event", "Weather" };
    }

    /**
     * Accessor to the list of weather name keys to which a sound can be tied.
     * @return weather key list
     */
    String[] getWeatherKeys() {
        String[] ret = new String[weathKeys.size()];
        for (int i = 0; i < weathKeys.size(); i++)
            ret[i] = (String) weathKeys.get(i);
        return ret;
    }

    /**
     * Create or use a new MenuItem.
     * @param root item to insert into
     * @param next string to create menu
     * @param last last item
     * @return added MenuItem
     */
    private JMenuItem getMenuItem(JPopupMenu pop, MyMenu root, String next, boolean last) {
        if (root == null) {
            MenuElement el[] = pop.getSubElements();
            for (int i = 0; i < el.length; i++)
                if (((JMenuItem)el[i]).getText().equals(next)) return (JMenuItem) el[i];
            JMenuItem mi = (last ? new MyMenuItem("", next) : (JMenuItem)new MyMenu("", next));
            pop.add(mi);
            return mi;
        }
        else {
            for (int i = 0; i < root.getItemCount(); i++) {
                if (((JMenuItem)root.getItem(i)).getText().equals(next)) return (JMenuItem) root.getItem(i);
            }
            String heritage = root.heritage + "/" + root.getText();
            JMenuItem mi = (last ? new MyMenuItem(heritage, next) : (JMenuItem)new MyMenu(heritage, next));
            root.add(mi);
            return mi;
        }
    }

    /**
     * Used for menu cascading, because getParent does not work correctly.
     */
    class MyMenuItem extends JMenuItem {
        String heritage;
        MyMenuItem(String aHeritage, String txt) { super(txt); heritage = aHeritage; }
    }

    /**
     * Used for menu cascading, because getParent does not work correctly.
     */
    class MyMenu extends JMenu {
        String heritage;
        MyMenu(String aHeritage, String txt) { super(txt); heritage = aHeritage; }
    }

    /**
     * Used for wrapping menu items.
     */
    class MyPopupMenu extends JPopupMenu {
        MyPopupMenu() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            int sh = Toolkit.getDefaultToolkit().getScreenSize().height;
            if (d.height > sh) return new Dimension(d.width, sh);
            return d;
        }
    }
}
