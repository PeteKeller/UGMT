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
import harn.repository.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import org.w3c.dom.*;

/**
 * Class that contains the GUI references to a 3D visual export.
 * @author Michael Jung
 */
class VisualExport extends HttpExport {
    /** Scaling */
    private JSpinner bScaleSpin;

    /** Sound queue */
    private harn.visual.Main plugin;

    /**
     * Constructor
     * @param aMain main reference
     * @param gridy y coordinate on GUI
     * @param panel panel to add this object's GUI to
     */
    VisualExport(Main aMain, int gridy, JPanel panel, String[] exp) {
        super(aMain, "visual", "3D visualization of maps", panel, gridy);

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
     * Called by the exporting Background thread upon a request.
     * @param query queried export
     * @param os output stream for direct access
     * @param osr output writer for writing
     */
    public void export(String query, OutputStream os, PrintWriter osr) throws IOException {
        if (plugin == null) {
            Object o = main.myFrw.getPlugins("visual");
            // If o = null, the next cast will probably horribly fail, which
            // is why this is not a one-liner
            if (o != null) plugin = (harn.visual.Main) o;
        }

        // Localize query 
        query = query.substring(query.indexOf('/')+1);

        if (plugin == null) return;
        osr.println("Content-type: image/png");
        osr.println();
        osr.flush();
        ImageIO.write(getImage(), "png", os);
    }

    /** Get the map */
    private BufferedImage getImage() {
        BufferedImage bi = plugin.getImage();
        float scale = ((Integer)bScaleSpin.getValue()).intValue()/100f;
        int w = (int)(bi.getWidth()*scale);
        int h = (int)(bi.getHeight()*scale);
        BufferedImage ret = new BufferedImage
            (w, h, BufferedImage.TYPE_INT_ARGB);
        ret.getGraphics().drawImage
            (bi, 0, 0, w, h, 0, 0, bi.getWidth(), bi.getHeight(), null);
        return ret;
    }

    /** 
     * Called by the framework to notify a new/changed object to the listener.
     * @param obj the notified object
     */
    public void inform(IExport obj) {}

    /**
     * Called by the main class to register this object with the framework.
     */
    public void listen() {}

    /** Save the export in XML document */
    public void save(Element child) {
        super.save(child);
        child.setAttribute("visualscale", bScaleSpin.getValue().toString());
    }

    /** Load the export from XML document */
    public void load(NamedNodeMap cattr) {
        super.load(cattr);
        if (cattr.getNamedItem("visualscale") != null)
            bScaleSpin.setValue
                (new Integer(Integer.parseInt(cattr.getNamedItem("visualscale").getNodeValue())));
    }
}
