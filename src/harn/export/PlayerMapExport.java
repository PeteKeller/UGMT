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

import harn.repository.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;
import rpg.*;
import org.w3c.dom.*;

/**
 * Class that contains the GUI references to a map plugin export, which need
 * to be queried to determine output. This class also handles the export.
 * @author Michael Jung
 */
class PlayerMapExport extends HttpExport {
    /** Scaling */
    private JSpinner bScaleSpin;

    /** References to maps */
    private Hashtable maps;

    /** Current group coordinates */
    private CharGroup act;

    /**
     * Constructor
     * @param aMain main reference
     * @param gridy y coordinate on GUI
     * @param panel panel to add this object's GUI to
     */
    PlayerMapExport(Main aMain, int gridy, JPanel panel, String[] exp) {
        super(aMain, "map", "The current map", panel, gridy);
        maps = new Hashtable();

        bScaleSpin = new JSpinner(Framework.getScaleSpinnerModel(17));
        bScaleSpin.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    main.bSave.setEnabled(true);
                }
            });
        bScaleSpin.setValue(new Integer(100));
        ((JSpinner.DefaultEditor)bScaleSpin.getEditor()).getTextField().setEditable(false);

        constrBox.gridx = 2;
        panel.add(bScaleSpin, constrBox);
    }

    /**
     * Called by the exporting thread upon a request for index export.
     * @param query queried export
     * @param os output stream for direct access (not used)
     * @param osr output writer for writing
     */
    public void export(String query, OutputStream os, PrintWriter osr) throws IOException {
        BufferedImage bi = getMap();
        if (bi != null) {
            osr.println("Content-type: image/png");
            osr.println();
            osr.flush();
            ImageIO.write(bi, "png", os);
        }
    }

    /**
     * Called by the framework to notify a new/changed object to the listener.
     * @param obj the notified object
     */
    public void inform(IExport obj) {
        if (obj instanceof ScaledMap)
            maps.put(((ScaledMap)obj).getName(), obj);

        if (obj instanceof CharGroup) {
            CharGroup cg = (CharGroup) obj;
            if (cg.isActive()) {
                act = cg;
            }
        }
    }

    /**
     * Get the current active map.
     * @param sz size of the image
     */
    private BufferedImage getMap() {
        ImageIcon src = null;
        // Map
        if (act != null) {
            ScaledMap map = (ScaledMap)maps.get(act.getMap());
            if (map != null) {
                BufferedImage bi = map.getBufferedImage(false, false);
                float scale = ((Integer)bScaleSpin.getValue()).intValue()/100f;
                AffineTransform trans = AffineTransform.getScaleInstance
                    (scale, scale);

                // Draw map
                int w = bi.getWidth(null);
                int h = bi.getHeight(null);
                BufferedImage ret = new BufferedImage
                    ((int)(w*scale), (int)(h*scale), BufferedImage.TYPE_INT_ARGB);
                Graphics g = ret.getGraphics();
                
                ((Graphics2D)g).setRenderingHint
                    (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                ((Graphics2D)g).drawImage(bi, trans, null);
                g.dispose();
                return ret;
            }
        }
        return null;
    }

    /**
     * Called by the main class to register this object with the framework.
     */
    public void listen() {
        main.myFrw.listen(this, ScaledMap.TYPE, null);
        main.myFrw.listen(this, CharGroup.TYPE, null);
    }

    /** Save the export in XML document */
    public void save(Element child) {
        super.save(child);
        child.setAttribute("mapscale", bScaleSpin.getValue().toString());
    }

    /** Load the export from XML document */
    public void load(NamedNodeMap cattr) {
        super.load(cattr);
        if (cattr.getNamedItem("mapscale") != null)
            bScaleSpin.setValue
                (new Integer(Integer.parseInt(cattr.getNamedItem("mapscale").getNodeValue())));
    }
}
