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
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import rpg.*;

/**
 * MapHolder holds OriginalMap, and BufferedMap.
 *
 * OriginalMap contains the mask and original both at the size of the original
 * file.  BufferedMap is used as a drawing canvas for the appropriate scales.
 *
 * Class that represents the Map GUI. Array elements of currentImg shall never
 * modified, only the array as a whole.
 * @author Michael Jung
 */
public class MapHolder extends JComponent implements IListener {
    /** Show foggy borders (view) or not */
    private boolean foggy = false;

    /** Name for current map */
    private String currentName;

    /** Name for active map */
    private String activeName;

    /** Fog icon */
    private ImageIcon fogIcon;

    /** root reference */
    private Main main;

    /** Highlighting color */
    private Color high;

    /** Link color */
    private Color link;

    /** list of locations */
    private HashSet locs;

    /** drawing width */
    private int epwidth;

    /** fog drawing background */
    private Color bgCol;

    /** Announce thread to save GUI speed */
    private Thread announceThread;

    /** stall time */
    private long doIn = 0;

    /** Highlighted shape */
    private Shape[] highShape;

    /** Highlight time */
    private int hightime;

    /** Original map */
    private OriginalMap origMap;

    /** While we redraw */
    private boolean redo = false;

    /** Local variable to hide cursor or not */
    private boolean hideCursor = true;

    /**
     * Constructor. Load mark = group cursor.
     * @param props properties
     * @param main root reference
     */
    MapHolder(Properties props, Main aMain) {
	super();
	main = aMain;
        highShape = null;
        locs = new HashSet();
	fogIcon = new ImageIcon(main.myPath + props.getProperty("fog"));

        // Color with alpha
        int col =
            (int)Long.parseLong(props.getProperty("highlight.color"), 16);
        high = new Color(col, true);
        col = (int)Long.parseLong(props.getProperty("hyperlink.color"), 16);
        link = new Color(col, true);
        hightime = Integer.parseInt(props.getProperty("highlight.time"), 16);
        epwidth = (int)Long.parseLong(props.getProperty("editor.pen.width"));
        origMap = new OriginalMap(main, this);
        setDoubleBuffered(true);
        expire(true);
    }

    /** Get current name */
    String getCurrentName() { return currentName; }

    /** Get foggy flag */
    boolean getFoggy() { return foggy; }

    /** Set foggy flag */
    void setFoggy(boolean newFog) { foggy = newFog; }

    /**
     * Set current icon only (check boxes may change the visibility mode).
     */
    void revalidateCurrentIcon() {
        if (currentName != null)
            origMap.set(Data.getMap(currentName));
        // Adapt the display
        expire(false);
    }

    /**
     * Setting the icons. If a name is null, nothing is changed. Only a true
     * map may be operated on. The method is called twice. Once from many
     * external places, this will update the tree. The tree update itself will
     * call this method and do the rest. Actually these two things should be
     * two methods.
     * @param name for current map
     * @param updateTree tree update?
     */
    void setIcons(String current, boolean updateTree) {
	if (current == null) return;
        if (current.equals(currentName) && !origMap.isEmpty()) return;

        Data.MyMap map = (Data.MyMap)Data.getMap(current);
        if (map == null && currentName != null)
            return;

        main.moveMenu.reset();

        if (updateTree) {
            // Adapt tree selection
            String path[] = current.split("/");
            DefaultMutableTreeNode parent = main.maptree;
            for (int i = 0; i < path.length; i++) {
                Enumeration iter = parent.children();
                while (iter.hasMoreElements()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        iter.nextElement();
                    if (!node.toString().equals(path[i])) continue;
                    parent = node;
                    break;
                }
            }

            main.tree.addSelectionPath(new TreePath(parent.getPath()));
        }
        else {
            // GUI update
            Data.MyMap dmap = (Data.MyMap) Data.getMap(current);
            ArrayList layers = dmap != null ? dmap.layers : null;
            main.setLayers(layers);

            currentName = current;
            origMap.set(map);

            expire(true);
        }
    }
    
    /**
     * Return the first location known.
     * @return first location
     */
    public Location getFirstLocation() {
        if (locs == null || locs.size() == 0) return null;
        Iterator iter = locs.iterator();
        return (Location) iter.next();
    }

    /**
     * Returns the locations that are valid for the given coordinates
     * @param x x-coordinate
     * @param y y-coordinate
     */
    Location[] getLocationsAt(int x, int y) {
        float scale = getScale();
        x = (int)(x/scale);
        y = (int)(y/scale);

        ArrayList al = new ArrayList();
        if (currentName != null) {
            Iterator iter = locs.iterator();
            while (iter.hasNext()) {
                Location loc = (Location) iter.next();
                Shape[] myShapes = loc.getShapes(currentName);
                for (int i = 0; myShapes != null && i < myShapes.length; i++) {
                    if (myShapes[i].contains(x,y))
                        al.add(loc);
                }
            }
        }

        Location[] ret = new Location[al.size()];
        for (int i = 0; i < al.size(); i++)
            ret[i] = (Location) al.get(i);
        return ret;
    }

    /**
     * Get scale.
     */
    public float getScale() {
        return ((Integer)main.bScaleSpin.getValue()).intValue() / 100.0f;
    }

    /**
     * Returns current dimension.
     */
    public Dimension getPreferredSize() {
        if (origMap.isEmpty()) return super.getPreferredSize();
        float scale = getScale();
        int w = origMap.getWidth();
        int h = origMap.getHeight();
        return new Dimension((int)(scale * w), (int)(scale * h));
    }
    
    /**
     * Listen to a location.
     * @param obj the location
     */
    public void inform(IExport obj) {
        Location loc = (Location) obj;

        // Enable editing
        if (main.editor != null) {
            main.bExport.setEnabled(true);
            locs = new HashSet();
        }

        if (loc.isValid())
            locs.add(loc);
        else {
            locs.remove(loc);
            return;
        }

        // Locate on map
        if (loc.getLocatable()) {
            Shape[] myShapes = setIconForLocation(loc);
            if (myShapes != null && myShapes.length > 0) {
                Thread t = new Thread() {
                        public void run() {
                            try {
                                while (redo)
                                    sleep(hightime);
                                sleep(hightime);
                            }
                            catch (Exception e) {
                            }
                            highShape = null;
                            repaint(null, null);
                        }
                    };
                highShape = myShapes;
                t.start();
                main.myFrw.raisePlugin("Maps");
            }

            // Editing
            if (loc.isEditing() && main.editor != null) {
                main.editor.currShape = myShapes[0];
            }
        }
    }

    /**
     * Paint this component for export
     */
    public BufferedImage paintMap() {
        // Create Image (original size)

        if (origMap.isEmpty()) return null;
        BufferedImage ret = new BufferedImage
            (origMap.getWidth(), origMap.getHeight(), BufferedImage.TYPE_INT_ARGB);
        origMap.setColor(getBackground());
        ((Graphics2D)ret.getGraphics()).setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        origMap.draw((Graphics2D)ret.getGraphics(), 1);
        return ret;
    }

    /**
     * Use fast buffer if fbShape = null, if not redraw fast buffer with
     * fbShape.
     */
    public void repaint(Rectangle shape, Shape fbShape) {
        origMap.useFastBuffer(fbShape);
        if (shape == null) repaint(); else repaint(shape);
    }

    /**
     * Callback for the system GUI manager. Redraw map.
     * @param g graphics object
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        redo = true;
        ((Graphics2D)g).setRenderingHint
            (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (origMap.isEmpty()) return;
        main.myFrw.setBusy(hideCursor && true);

        // Basics
        ((Graphics2D)g).setStroke(new BasicStroke(epwidth));
        float scale = getScale();

	// Draw map
        origMap.setColor(getBackground());
        origMap.draw((Graphics2D)g, scale);

        // Draw editing shape (already GUI size)
        if (main.editor != null && main.editor.currShape != null) {
            g.setColor(high);
            ((Graphics2D)g).draw(main.editor.currShape);
        }

        // Draw fog editing shape (already GUI size)
        if (main.fogMouse.currShape != null) {
            g.setColor(high);
            ((Graphics2D)g).draw(main.fogMouse.currShape);
        }

        // Draw Fog
        if (foggy) {
            // Viewport
            JViewport vp = main.rightPanel.getViewport();
            int x = vp.getViewPosition().x;
            int y = vp.getViewPosition().y;
            int w = vp.getExtentSize().width;
            int h = vp.getExtentSize().height;
            int sw = (int) (scale * w);
            int sh = (int) (scale * h);

            if (sw < w) x -= (w - sw)/2;
            if (sh < h) y -= (h - sh)/2;

            g.drawImage(fogIcon.getImage(), x, y, w, h, this);
        }

        AffineTransform trans = AffineTransform.getScaleInstance(scale, scale);

        // Draw locations
        if (main.bFrames.isSelected()) {
            Iterator iter = locs.iterator();
            if (currentName != null) {
                while (iter.hasNext()) {
                    Location loc = (Location) iter.next();
                    Shape[] myShapes = loc.getShapes(currentName);
                    boolean act = loc.getSelected();
                    g.setColor(high);
                    for (int i = 0; myShapes != null && i < myShapes.length; i++) {
                        Shape shape = trans.createTransformedShape(myShapes[i]);
                        if (act)
                            ((Graphics2D)g).fill(shape);
                        else
                            ((Graphics2D)g).draw(shape);
                    }
                }
            }

            // Draw Links
            if (main.myData.getCurrent() != null) {
                Hashtable links = main.myData.getCurrent().maps;
                g.setColor(link);
                iter = links.keySet().iterator();
                while (iter.hasNext()) {
                    Shape shape = trans.createTransformedShape((Shape)iter.next());
                    ((Graphics2D)g).draw(shape);
                }
            }

            // Highlight shape
            Shape[] shape = highShape;
            if (shape != null) {
                g.setColor(high);
                for (int i = 0; i < shape.length; i++) {
                    Shape transshape = trans.createTransformedShape(shape[i]);
                    ((Graphics2D)g).fill(transshape);
                }
            }
        }
        // We have fixed the fast buffer by now
        origMap.useFastBuffer(null);

        // To increase GUI speed we only announce 250ms passed. (Stalling this
        // too long will cause seemingly spurious lags on the GUI after action
        // was taken, because actions can also take time.)
        doIn = System.currentTimeMillis() + 250;
        if (announceThread == null) {
            announceThread = new Thread() {
                    public void run() {
                        while (System.currentTimeMillis() < doIn)
                            try { Thread.sleep(100); } catch (Exception ex) {}
                        if (currentName != null)
                            main.myFrw.announce
                                ((Data.MyMap)Data.getMap(currentName));
                        announceThread = null;
                    }
                };
            announceThread.start();
        }
        main.myFrw.setBusy(false);
        hideCursor = true;
        redo = false;
    }

    /**
     * Redo the image. Create a new fast image only, when sizes might have
     * changed.
     * @param rmmasks remove masks
     */
    private void expire(boolean rmmasks) {
        if (origMap.isEmpty()) return;
        main.myFrw.setBusy(true);

        // Base
        if (rmmasks) origMap.resetMask();
        origMap.setMasks();
        repaint(null, origMap.getBounds()); // reset and set do not use fastBuffer
        main.myFrw.setBusy(false);
    }

    /** rescale and repaint */
    public void setScale() {
        if (origMap.isEmpty()) return;
        main.myFrw.setBusy(true);
        revalidate();
        main.myFrw.setBusy(false);
    }

    /**
     * Locate a map for an index (location). Set the icon.
     * @param loc location to locate a map for
     * @return shapes on this map
     */
    private Shape[] setIconForLocation(Location loc) {
        Data data = main.myData;
        Iterator iter = data.getMapNames().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Shape[] sh = loc.getShapes(key);
            if (sh != null && sh.length > 0) {
                Rectangle r = sh[0].getBounds();
                setIcons(key, true);
                Runnable vp = new MyRunnable(r);
                SwingUtilities.invokeLater(vp);

                // Get center
                return sh;
            }
        }
        return null;
    }

    /**
     * Clear out map
     */
    void clear() { origMap.set(null); }

    /**
     * Save masks.
     */
    public void saveMasks(File f) { origMap.saveMasks(f); }
    
    /**
     * Load masks.
     */
    public void loadMasks(File f) {
        origMap.loadMasks(f, getScale());
        // load mask does not update fastBuffer
        repaint(getVisibleRect(), origMap.getBounds());
    }

    /**
     * Stains a shape with sharp borders.
     */
    public void stain(int layer, Shape shape, boolean hide) {
        origMap.stain(layer, shape, hide);
        repaint(shape.getBounds(), shape); // stain does not update fastBuffer
    }

    /**
     * Redraw.
     */
    public void hideAll(int layer, boolean hide) {
        origMap.hideAll(layer, hide);
        repaint(getVisibleRect(), origMap.getBounds()); // stain does not update fastBuffer
    }

    /**
     * Marks a line in black.
     */
    public void stain(int layer, int x0, int y0, int x1, int y1) {
        origMap.stain(layer, x0, y0, x1, y1);
        Rectangle r = new Rectangle
            (Math.min(x0,x1), Math.min(y0,y1),
             Math.max(x0,x1) - Math.min(x0,x1), Math.max(y0,y1) - Math.min(y0,y1));
        repaint(r, r); // stain does not update fastBuffer
    }

    /**
     * Stains a point with smooth  borders.
     */
    public void stain(int layer, int xc, int yc, boolean hide) {
        Shape r = origMap.stain(layer, xc, yc, hide);
        // stain does not update fastBuffer
        hideCursor = false;
        if (r != null) repaint(r.getBounds(), r.getBounds());
    }

    /**
     * Runnable to center Map at a later time. Somehow there are still
     * resizing events coming after this one. I cannot make any sense of it.
     */
    class MyRunnable implements Runnable {
        Rectangle rr;
        MyRunnable(Rectangle r) { rr = r; }
        public void run() {
            main.setViewport(rr.x + rr.width/2, rr.y + rr.height/2);
        }
    }
}
