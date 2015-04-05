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

import harn.repository.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import java.text.Collator;
import javax.swing.tree.*;
import rpg.*;

/**
 * Class that represents the Map GUI.
 * @author Michael Jung
 */
public class DirMapHolder extends JComponent {
    /** Name for current map */
    private String currentName;

    /** root reference */
    private Main main;

    /** list of linked (ref,map)s indexed by grid-id */
    private Hashtable mapList;

    /**
     * Constructor.
     * @param props properties
     * @param main root reference
     */
    DirMapHolder(Properties props, Main aMain) {
	super();
        setLayout(new GridBagLayout());
	main = aMain;
    }

    /** Get current name */
    String getCurrentName() { return currentName; }

    /**
     * Setting the icons. If a name is null, nothing is changed. Only a true
     * map may be operated on.
     * @param name for current map
     */
    void setIcon(String current) {
	if (current != null && (!current.equals(currentName) || mapList == null)) {
            mapList = main.myData.getGridLinks(current);

            removeAll();
            if (mapList == null) {
                revalidate();
                return;
            }

	    currentName = current;

            TreeSet ts = new TreeSet(Collator.getInstance(Locale.FRANCE));
            ts.addAll(mapList.keySet());
            Iterator iter = ts.iterator();
            ArrayList panels = new ArrayList();
            int idx = 0;
            int x = 0;
            int y = 0;
            while (iter.hasNext()) {
                String gridid = (String) iter.next();
                String[] refmap = (String[]) mapList.get(gridid);
                ImagePanel p = new ImagePanel(refmap);

                if (gridid.matches("[0-9],[0-9]")) {
                    x = Integer.parseInt
                        (gridid.substring(0, gridid.indexOf(',')));
                    y = Integer.parseInt
                        (gridid.substring(gridid.indexOf(',')+1));
                }
                else {
                    x = idx%4;
                    y = idx/4;
                    idx++;
                }

                // Prepare y-panels
                for (int i = panels.size(); i <= y; i++)
                    panels.add(new ArrayList());

                // Prepare x-panels
                ArrayList al = (ArrayList) panels.get(y);
                for (int i = al.size(); i <= x; i++)
                    al.add(null);

                al.set(x, p);
            }

            GridBagConstraints constr = new GridBagConstraints();
            constr.fill = GridBagConstraints.BOTH;
            constr.weightx = constr.weighty = 1;
            constr.insets = new Insets(5,5,5,5);
            for (int i = 0; i < panels.size(); i++) {
                ArrayList al = (ArrayList) panels.get(i);
                for (int j = 0; j < al.size(); j++) {
                    JComponent px = new JPanel();
                    constr.gridx = j;
                    constr.gridy = i;
                    if (al.get(j) != null)
                        px = (ImagePanel)al.get(j);
                    add(px, constr);
                }
            }

	    // Adapt the display
	    revalidate();
	}
    }
    
    /**
     * Returns our width.
     * @return width     
     */
    int getMyWidth() { return getPreferredSize().width; }

    /**
     * Returns our height.
     * @return height
     */
    int getMyHeight() { return getPreferredSize().height; }

    /**
     * Returns the linked map valid for the given coordinates
     * @param x x-coordinate
     * @param y y-coordinate
     */
    String getLinkAt(int x, int y) {
        Component c = findComponentAt(x,y);
        if (c instanceof JPanel && c.getParent() instanceof ImagePanel) {
            return ((ImagePanel)c.getParent()).map;
        }
        return null;
    }

    /**
     * Clear out map
     */
    void clear() { mapList = null; }

    /**
     * Class to hold a displayed image.
     */
    class ImagePanel extends JPanel {
        Image image;
        String map; // full name
        int w, h, w0;
        public ImagePanel(String[] refs) {
            super(new GridBagLayout());
            map = refs[1];
            String name = refs[2]; // short name

            try {
                image = ImageIO.read(new File(Framework.osName(refs[0])));
                w0 = image.getWidth(null);
                h = image.getHeight(null);
            }
            catch (IOException e) {
                // Debug
                e.printStackTrace();
            }

            w = Math.min(200,w0);

            JButton b = new JButton(name);
            b.setEnabled(false);
            b.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
            b.setAlignmentX(CENTER_ALIGNMENT);

            JPanel p = new JPanel() {
                    public Dimension getPreferredSize() { return new Dimension(w, h); }
                    public void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Dimension d = getSize();
                        int dx = Math.max(d.width/2 - w/2, 0);
                        int dy = Math.max(d.height/2 - h*w/(2*w0), 0);
                        g.drawImage(image, dx, dy, dx + w, dy + h*w/w0, 0, 0, w0, h, this);
                    }
                };
            p.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
            p.setBackground(Color.WHITE);

            GridBagConstraints constr = new GridBagConstraints();
            constr.fill = GridBagConstraints.HORIZONTAL;
            constr.weightx = 1;
            add(b, constr);

            constr.fill = GridBagConstraints.BOTH;
            constr.weighty = constr.gridy = 1;
            add(p, constr);

        }
    }
}
