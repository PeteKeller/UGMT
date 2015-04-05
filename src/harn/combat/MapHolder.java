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
package harn.combat;

import harn.repository.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.*;
import java.io.*;
import javax.swing.tree.*;
import rpg.*;
import java.awt.image.*;

/**
 * Class that represents the Map GUI.
 * @author Michael Jung
 */
public class MapHolder extends JComponent implements IListener {
    /** 2/Sqrt(3) */
    private static double WSTRETCH = 1/Math.sqrt(3);

    /** Revert 1/1000 pixel so hex below is 1 unit across */
    private static AffineTransform hexscale =
        AffineTransform.getScaleInstance(.0001,.0001);

    /** Hex (1000 across) */
    private static Shape hex = hexscale.createTransformedShape
        (new Polygon
         (new int[] {
             0, (int)(10000*WSTRETCH), (int)(15000*WSTRETCH),
             (int)(10000*WSTRETCH), 0, -(int)(5000*WSTRETCH)
          },
          new int[] {0, 0, 5000, 10000, 10000, 5000}, 6)
         );
    /** root reference */
    private Main main;

    /** current image */
    private BufferedImage img;

    /** current scale */
    protected int truescale;

    /** Draw grid */
    boolean grid = false;

    /** Draw FOW */
    boolean fow = false;

    /** Maps */
    private Hashtable maps;

    /** Marker */
    private BufferedImage marker;

    /** Active group */
    protected CharGroup act;

    /** Current tokens */
    private Hashtable tokens;

    /** minimum marker size (in pixel) */
    private int minSz;

    /** marker scale (in feet) */
    private int mScale;

    /** Number of feet before measuring leagues instead of feet */
    private int propD;

    /**
     * Constructor. Load mark = group cursor.
     * @param main root reference
     */
    MapHolder(Main aMain) {
	super();
	main = aMain;
        tokens = new Hashtable();
        minSz = Integer.parseInt(main.myProps.getProperty("marker.min_size"));
        mScale = Integer.parseInt(main.myProps.getProperty("marker.size"));
        String f = main.myProps.getProperty("marker.file");
        try {
            marker =
                ImageIO.read(new File(main.myPath + f));
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
        maps = new Hashtable();
    }

    /** Get Marker Width */
    public int getMarkerWidth() { return marker.getWidth(); }

    /** Get Marker Height */
    public int getMarkerHeight() { return marker.getHeight(); }

    /** Get Token x-coordinate */
    public int getTokenPosX(String name) {
        MyToken token = (MyToken) tokens.get(name);
        if (token == null || token.pos == null) return -1;
        return (int)(token.pos[0]*getScale());
    }

    /** Get Token y-coordinate */
    public int getTokenPosY(String name) {
        MyToken token = (MyToken) tokens.get(name);
        if (token == null || token.pos == null) return -1;
        return (int)(token.pos[1]*getScale());
    }

    /** Get scaling transform */
    public AffineTransform getMarkerScaling() {
        float guiscale = truescale*getScale()/13200f;
        int dw = (int)Math.max(minSz, mScale*guiscale);
        int dh = (int)Math.max(minSz, mScale*guiscale);
        int mw = marker.getWidth();
        int mh = marker.getHeight();
        return AffineTransform.getScaleInstance
            (dw/(double)mw, dh/(double)mh);
    }

    /** force repaint */
    public void redo() { repaint(); revalidate(); }

    /**
     * Returns current dimension.
     */
    public Dimension getPreferredSize() { return getPreferredSize(true); }

    /**
     * Returns current dimension.
     */
    public Dimension getPreferredSize(boolean gui) {
        if (img == null) return super.getPreferredSize();
        int w = (int)(img.getWidth(null)*(gui ? getScale() : 1));
        int h = (int)(img.getHeight(null)*(gui ? getScale() : 1));
        return new Dimension(w,h);
    }
    
    /** Get the proper scale */
    public float getScale() {
        return ((Integer)main.bScaleSpin.getValue()).intValue()/100f;
    }

    /** Get the proper scale */
    private int getFowRadius() {
        return ((Integer)main.bFowRadius.getValue()).intValue();
    }

    /**
     * Callback for the system GUI manager. Redraw map.
     * @param g graphics object
     */
    public void paintComponent(Graphics g) { paintComponent(g, true); }

    /**
     * Callback for the redraw.
     * @param g graphics object
     * @param gui used by gui
     */
    public void paintComponent(Graphics g, boolean gui) {
        img = getMap();
        float scale = gui ? getScale() : 1;
        AffineTransform trans = AffineTransform.getScaleInstance(scale, scale);
        ((Graphics2D)g).setRenderingHint
            (RenderingHints.KEY_RENDERING,
             RenderingHints.VALUE_RENDER_QUALITY);
        float guiscale = truescale*scale/13200f;

        int dw = (int)Math.max(minSz, mScale*guiscale);
        int dh = (int)Math.max(minSz, mScale*guiscale);

        if (img != null) {
            // Draw map
            int w = img.getWidth(null);
            int h = img.getHeight(null);
            ((Graphics2D)g).drawImage(img, trans, this);

            if (grid) {
                AffineTransform gscale =
                    AffineTransform.getScaleInstance(dw, dh);
                Shape lhex = gscale.createTransformedShape(hex);

                for (float i = 0; i < scale*w; i += 3*dw*WSTRETCH) {
                    for (int j = 0; j < 2*scale*h/(float)dh; j++) {
                        AffineTransform trafo =
                            AffineTransform.getTranslateInstance
                            (i + (j%2)*3*dw*WSTRETCH/2f, j*dh/2f);
                        Shape tmp = trafo.createTransformedShape(lhex);
                        ((Graphics2D)g).draw(tmp);
                    }
                }
            }
        }

        int mw = marker.getWidth();
        int mh = marker.getHeight();
        int fh = dh/2;
	Font f = new Font("SansSerif", Font.BOLD, fh);
        FontMetrics fm = getFontMetrics(f);
        g.setFont(f);

        AffineTransform cT =
            AffineTransform.getTranslateInstance(-mw/2, -mh/2);
        AffineTransform sT =
            AffineTransform.getScaleInstance
            (dw/(double)mw, dh/(double)mh);

        Iterator iter = tokens.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String)iter.next();
            MyToken token = (MyToken) tokens.get(name);
            if (token == null || token.pos == null) continue;
            int px = (int)(token.pos[0]*scale);
            int py = (int)(token.pos[1]*scale);
            if (px < 0 || py < 0) continue;

            AffineTransform rT =
                AffineTransform.getRotateInstance(token.pos[2]);
            AffineTransform pT =
                AffineTransform.getTranslateInstance(px, py);

            // pT * sT * rT * cT
            pT.concatenate(sT);
            pT.concatenate(rT);
            pT.concatenate(cT);
            ((Graphics2D)g).drawImage(marker, pT, this);
                
            String init = name.substring(0,1);
            g.drawString
                (init, px - fm.stringWidth(init)/2, py + fh/2);
        }

        if (fow && img != null) {
            int fowr = (int)(getFowRadius()*guiscale);
            Object o = main.tree.getLastSelectedPathComponent();
            if (fowr > 0 && o != null) {
                String name = o.toString();
                double[] pos = main.map.getTokenPos(name);

                BufferedImage mask = createFog(img, pos, fowr);
                ((Graphics2D)g).drawImage(mask, 0, 0, this);
            }
        }
        revalidate();
    }

    /**
     * Create Fog of war.
     * @param img image to look over
     * @param pos position of looker
     */
    private BufferedImage createFog(BufferedImage img, double[] pos, int r) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D big = (Graphics2D) bi.getGraphics();
        big.setColor(Color.BLACK);
        int x1 = Math.max((int)pos[0] - r, 0);
        int y1 = Math.max((int)pos[1] - r, 0);
        int x2 = Math.min((int)pos[0] + r, w);
        int y2 = Math.min((int)pos[1] + r, h);
        int x21 = x2 - x1;
        int y21 = y2 - y1;
        big.fillRect(0, 0, w, h);

        int[] mask_rgba = new int[x21*y21];
        int[] fog_rgba = new int[x21*y21];

        img.getRGB(x1, y1, x21, y21, mask_rgba, 0, x21);

        // Left
        for (int i = 0; i <= 2*r; i++) {
            int i0 = (int)pos[0];
            int j0 = (int)pos[1];
            fog_rgba[(i0 - x1) + (j0 - y1)*x21] = 0xff;
            for (double j = 0; j <= r; j++) {
                int i1 = (int)(pos[0] + j*(0 - r)/r);
                int j1 = (int)(pos[1] + j*(i - r)/r);
                if (i1 >= x1 && j1 >= y1 && i1 < x2 && j1 < y2) {
                    int idx1 = (i1 - x1) + (j1 - y1)*x21;
                    int idx0 = (i0 - x1) + (j0 - y1)*x21;
                    fog_rgba[idx1] = Math.min(0xff & mask_rgba[idx1], fog_rgba[idx0]);
                    fog_rgba[idx1] =
                        Math.min(dist(i1, j1, pos[0], pos[1], r), fog_rgba[idx1]);
                    i0 = i1; j0 = j1;
                }
            }
        }
        // Right
        for (int i = 0; i <= 2*r; i++) {
            int i0 = (int)pos[0];
            int j0 = (int)pos[1];
            fog_rgba[(i0 - x1) + (j0 - y1)*x21] = 0xff;
            for (double j = 0; j <= r; j++) {
                int i1 = (int)(pos[0] + j*(2*r - r)/r);
                int j1 = (int)(pos[1] + j*(i - r)/r);
                if (i1 >= x1 && j1 >= y1 && i1 < x2 && j1 < y2) {
                    int idx1 = (i1 - x1) + (j1 - y1)*x21;
                    int idx0 = (i0 - x1) + (j0 - y1)*x21;
                    fog_rgba[idx1] = Math.min(0xff & mask_rgba[idx1], fog_rgba[idx0]);
                    fog_rgba[idx1] =
                        Math.min(dist(i1, j1, pos[0], pos[1], r), fog_rgba[idx1]);
                    i0 = i1; j0 = j1;
                }
            }
        }
        // Top
        for (int i = 0; i <= 2*r; i++) {
            int i0 = (int)pos[0];
            int j0 = (int)pos[1];
            fog_rgba[(i0 - x1) + (j0 - y1)*x21] = 0xff;
            for (double j = 0; j <= r; j++) {
                int j1 = (int)(pos[1] + j*(0 - r)/r);
                int i1 = (int)(pos[0] + j*(i - r)/r);
                if (i1 >= x1 && j1 >= y1 && i1 < x2 && j1 < y2) {
                    int idx1 = (i1 - x1) + (j1 - y1)*x21;
                    int idx0 = (i0 - x1) + (j0 - y1)*x21;
                    fog_rgba[idx1] = Math.min(0xff & mask_rgba[idx1], fog_rgba[idx0]);
                    fog_rgba[idx1] =
                        Math.min(dist(i1, j1, pos[0], pos[1], r), fog_rgba[idx1]);
                    i0 = i1; j0 = j1;
                }
            }
        }
        // Bottom
        for (int i = 0; i <= 2*r; i++) {
            int i0 = (int)pos[0];
            int j0 = (int)pos[1];
            fog_rgba[(i0 - x1) + (j0 - y1)*x21] = 0xff;
            for (double j = 0; j <= r; j++) {
                int j1 = (int)(pos[1] + j*(2*r - r)/r);
                int i1 = (int)(pos[0] + j*(i - r)/r);
                if (i1 >= x1 && j1 >= y1 && i1 < x2 && j1 < y2) {
                    int idx1 = (i1 - x1) + (j1 - y1)*x21;
                    int idx0 = (i0 - x1) + (j0 - y1)*x21;
                    fog_rgba[idx1] = Math.min(0xff & mask_rgba[idx1], fog_rgba[idx0]);
                    fog_rgba[idx1] =
                        Math.min(dist(i1, j1, pos[0], pos[1], r), fog_rgba[idx1]);
                    i0 = i1; j0 = j1;
                }
            }
        }

        for (int i = 0; i < y21; i++) {
            for (int j = 0; j < x21; j++) {
                mask_rgba[i*x21 + j] = ((0xff - fog_rgba[i*x21 + j]) << 24);
            }
        }
        bi.setRGB(x1, y1, x21, y21, mask_rgba, 0, x21);
        return bi;
    }

    /**
     * Distance between x,y and a,b, where r shall be normed to 0xff.
     */
    private int dist(int x, int y, double a, double b, int r) {
        int ret = (int)(Math.sqrt((x-a)*(x-a) + (y-b)*(y-b))/r * 255);
        return 0xff - Math.min(ret, 255);
    }

    /**
     * Listen to maps and groups. Scales of maps do not change! Therefore an
     * announcement of a map doesn't require a redraw.
     * @param obj the location
     */
    public void inform(IExport obj) {
        if (obj instanceof ScaledMap)
            maps.put(((ScaledMap)obj).getName(), obj);

        if (obj instanceof CharGroup) {
            CharGroup cg = (CharGroup) obj;
            if (cg.isActive() && cg != act) {
                act = cg;
            }
            if (img == null) img = getMap();
        }
    }
    /**
     * Get the current active map in the dimension desired.
     * @param sz size of the image
     */
    private BufferedImage getMap() {
        ImageIcon src = null;
        // Map
        if (act != null) {
            ScaledMap map = (ScaledMap)maps.get(act.getMap());
            if (map != null) {
                truescale = map.getScale();
                return map.getBufferedImage(true, false);
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

    /**
     * Remove a token.
     */
    public void removeToken(String name) {
        MyToken tok = (MyToken)tokens.get(name);
        tok.valid = false;
        tokens.remove(name);
        repaint();
        main.myFrw.announce(tok);
    }

    /**
     * Get a token.
     */
    public double[] getTokenPos(String name) {
        MyToken tok = (MyToken)tokens.get(name);
        if (tok != null) return tok.pos;
        return null;
    }

    /**
     * Get token iterator.
     */
    public Iterator getTokenIterator() { return tokens.keySet().iterator(); }

    /**
     * Add a token.
     */
    public void addToken(String name) {
        if (!tokens.containsKey(name)) {
            MyToken tok = new MyToken(name, new double[] { -1, -1, 0});
            tokens.put(name, tok);
            main.getState().setDirty(true);
            main.myFrw.announce(tok);
            repaint();
        }
    }

    /** Announce a token */
    public void announceToken(String name) {
        MyToken tok = (MyToken)tokens.get(name);
        if (tok != null) main.myFrw.announce(tok);
    }

    /**
     * Add a token.
     */
    public void addToken(String name, double[] token) {
        MyToken tok = new MyToken(name, token);
        tokens.put(name, tok);
        main.myFrw.announce(tok);
    }

    /** Rename token. */
    public void renameToken(String oldName, String newName) {
        MyToken tok = (MyToken) tokens.get(oldName);
        tokens.remove(oldName);
        tokens.put(newName, tok);
        tok.name = newName;
        repaint();
    }

    class MyToken extends Token {
        /** Name */
        private String name;
        /** Validity */
        private boolean valid;
        /** Position */
        private double[] pos;
        /** Constructor */
        MyToken(String aName, double[] aPos) {
            name = aName;
            valid = true;
            pos = aPos;
        }
        /** IF method */
        public String getName() { return name; }
        /** IF method */
        public boolean isValid() { return valid; }
        /** IF method */
        public double[] getCoordinates(String map) {
            if (map.equals(act.getMap())) {
                return pos;
            }
            return null;
        }
    }
}
