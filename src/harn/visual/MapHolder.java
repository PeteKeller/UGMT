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
package harn.visual;

import rpg.*;
import harn.repository.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import java.awt.*;
import java.util.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import javax.imageio.*;

/**
 * Class that represents the Map GUI.
 * @author Michael Jung
 */
public class MapHolder extends JComponent implements GLEventListener, AncestorListener, IListener {

    /** Back reference */
    private Main main;

    /** Triangles for terrain */
    private float[][] tri;

    /** Colors for triangles */
    private float[][] txt;

    /** Max terrain grid size for Jogl */
    int max_size;

    /** Max height for Jogl */
    int max_hght;

    /** Handles for graphic card procedures */
    private int h_lscape, h_token;

    /** display size for ease of reference */
    private float display_w, display_h;

    /** Scale texture to board */
    private float scale;

    /** Scale pixel per league of map */
    float ppl;

    /** Helper constant */
    static final int[] MASK = new int[] { 0xff, 0xff00, 0xff0000 };

    /** Helper constant */
    static final float DEG2RAD = (float)Math.atan(1)/45;

    /** All (valid) tokens */
    private HashSet tokens;

    /** Export image */
    private BufferedImage expImg;

    /** height field distort for auto-heightfield */
    private float distort;

    /** Tilt */
    float tilt;

    /** Zoom */
    float zoom;

    /** Rotate */
    float rotate;

    /** Translate */
    float movx, movy;

    /** Base Image */
    BufferedImage texture;

    /** Base heightfield */
    BufferedImage heightfield;

    /** JOGL Container */
    private GLCanvas drawable;

    /** Redo all */
    private boolean redoAll;

    /** Marker color */
    private float[] marker_col;

    /** force an internal reshape event */
    private boolean forceReshape;

    /** Constructor */
    public MapHolder(Main aMain) {
        main = aMain;
        tokens = new HashSet();
        max_hght = Integer.parseInt
            (main.myProps.getProperty("terrain.height"));
        setMSScale(100);
        distort = Float.parseFloat
            (main.myProps.getProperty("autoheight.distort"));
        distort = Math.min(1, Math.max(0, distort));
        String col = main.myProps.getProperty("marker.color");
        marker_col = new float[3];
        marker_col[0] = Integer.parseInt(col.substring(0,2), 16)/255f;
        marker_col[1] = Integer.parseInt(col.substring(2,4), 16)/255f;
        marker_col[2] = Integer.parseInt(col.substring(4,6), 16)/255f;

        setLayout(new BorderLayout());

        addAncestorListener(this);
        tilt = rotate = 0;
        zoom = max_size/2f;
        redoAll = true;
    }

    public void redo(boolean all) {
        redoAll |= all;
        if (drawable != null) drawable.repaint();
    }

    /** Set the scale multiplier */
    public void setMSScale(int mult) {
        int nmax_size = (Integer.parseInt
            (main.myProps.getProperty("terrain.size")) * mult)/100;
        if (nmax_size < 1) nmax_size = 1;
        zoom = zoom * nmax_size / max_size;
        max_size = nmax_size;
        forceReshape = true;
        redo(true);
    }

    /**
     * AncestorListener IF; needed because not all visibility notifies reach
     * GLEventListeners.
     */
    public void ancestorAdded(AncestorEvent event) {
        if (drawable == null) {
            // Set up JOGL
            GLCapabilities caps = new GLCapabilities();
            caps.setDoubleBuffered(true);
            caps.setHardwareAccelerated(true);
            drawable = new GLCanvas(caps);
            drawable.addGLEventListener(this);
            add(drawable, BorderLayout.CENTER);
        }
        drawable.repaint();
        repaint();
        revalidate();
    }

    /**
     * AncestorListener IF; needed because not all visibility notifies reach
     * GLEventListeners.
     */
    public void ancestorMoved(AncestorEvent event) {
        if (drawable != null) drawable.repaint();
    }

    /**
     * Export method.
     */
    public BufferedImage getImage() {
        return expImg;
    }

    /**
     * AncestorListener IF; needed because not all visibility notifies reach
     * GLEventListeners.
     */
    public void ancestorRemoved(AncestorEvent event) {
        if (drawable != null) drawable.repaint();
    }

    /** Init called as canvas is created */
    public void init(GLAutoDrawable drawable) {
        String col = main.myProps.getProperty("background.color");
        float r = Integer.parseInt(col.substring(0,2), 16)/255f;
        float g = Integer.parseInt(col.substring(2,4), 16)/255f;
        float b = Integer.parseInt(col.substring(4,6), 16)/255f;

        GL gl = drawable.getGL();
        gl.glShadeModel(GL.GL_SMOOTH);
        gl.glClearColor(r, g, b, 0);
        gl.glClearDepth(1.0f);
	gl.glEnable(GL.GL_DEPTH_TEST);
	gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);

        // Prepare
        h_token = gl.glGenLists(1);
        h_lscape = gl.glGenLists(1);

        compileToken(gl);
    }

    /** Resize listener method */
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        display_w = w;
        display_h = h;
        GL gl = drawable.getGL();
        GLU glu = new GLU();

        // Set up viewport
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective
            (60f, display_w/(float)display_h, 10, 3*max_size/2f);
    }

    /** Rare or dummy callback */
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    /**
     * paintComponent equivalent.
     */
    public void display(GLAutoDrawable drawable) {
        if (!isShowing()) return;

        // Graphics equivalents
        GL gl = drawable.getGL();
        GLU glu = new GLU();
        GLUT glut = new GLUT();

        if (forceReshape) {
            forceReshape = false;
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glLoadIdentity();
            glu.gluPerspective
                (60f, display_w/(float)display_h, 10, 3*max_size/2f);
        }

        // Set up camera
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        int rad = (int)zoom;

        glu.gluLookAt
            (0, rad*Math.sin(tilt*DEG2RAD), rad*Math.cos(tilt*DEG2RAD),
             0,0,0,
             0,1,0);  
        gl.glTranslatef(movx, movy, 0);

        if (redoAll) {
            h_lscape = gl.glGenLists(1);
            main.myFrw.setBusy(true);
            compileTerrain(gl);
            main.myFrw.setBusy(false);
            redoAll = false;
        }

        // This is necessary in order to make loading simple
        gl.glRotatef(180 + rotate, 0, 0, 1);
        gl.glRotatef(180, 0, 1, 0);
        gl.glCallList(h_lscape);

        if (main.act != null && texture != null) {
            int tw = texture.getWidth();
            int th = texture.getHeight();
            gl.glTranslatef(-tw/2*scale, -th/2*scale, 0);
            float fscale = Math.max(1, ppl/13200f*scale);
            Iterator iter = tokens.iterator();
            while (iter.hasNext()) {
                Token tok = (Token) iter.next();
                double[] pos = tok.getCoordinates(main.act.getMap());
                if (pos == null || pos[0] < 0 || pos[1] < 0) continue;
                gl.glTranslatef
                    ((float)pos[0]*scale, (float)pos[1]*scale, 0);
                gl.glScalef(fscale, fscale, 1);
                gl.glCallList(h_token);
                drawLetter(glut, gl, tok.getName().charAt(0), pos[2]/DEG2RAD);
                gl.glScalef(1/fscale, 1/fscale, 1);
                gl.glTranslatef
                    (-(float)pos[0]*scale, -(float)pos[1]*scale, 0);
            }
            gl.glTranslatef(tw/2*scale, th/2*scale, 0);
        }

        gl.glRotatef(180, 0, 1, 0);
        gl.glRotatef(180, 0, 0, 1);

        gl.glFlush();
        expImg = Screenshot.readToBufferedImage((int)display_w, (int)display_h);
    }

    /** Draw letter */
    private void drawLetter(GLUT glut, GL gl, char tok, double rotdeg) {
        gl.glTranslatef(0, 0, -max_hght - .1f);
        gl.glColor3f(0, 0, 0);

        gl.glRotatef(180 + (float)rotdeg, 0, 0, 1);

        gl.glScalef(1/60f, 1/60f, 1f);
        int w = glut.glutStrokeWidth(GLUT.STROKE_ROMAN, tok);
        gl.glRotatef(180, 0, 1, 0);
        gl.glTranslatef(-w/2, 0, 0);
        glut.glutStrokeCharacter(GLUT.STROKE_ROMAN, tok);
        gl.glTranslatef(-w/2, 0, 0);
        gl.glRotatef(180, 0, 1, 0);
        gl.glScalef(60f, 60f, 1f);

        gl.glRotatef(180 - (float)rotdeg, 0, 0, 1);

        gl.glTranslatef(0, 0, max_hght + .1f);
    }

    /** Compile the token */
    private void compileToken(GL gl) {
        // Todo: color
        gl.glNewList(h_token, GL.GL_COMPILE);

        gl.glColor3f(marker_col[0], marker_col[1], marker_col[2]);
        gl.glScalef(1.25f, 1.25f, 1);

        gl.glBegin(GL.GL_QUAD_STRIP);
        gl.glVertex3f(-2, 0, 0);
        gl.glVertex3f(-2, 0, -max_hght);
        gl.glVertex3f(-1, -1.76f, 0);
        gl.glVertex3f(-1, -1.76f, -max_hght);
        gl.glVertex3f(1, -1.76f, 0);
        gl.glVertex3f(1, -1.76f, -max_hght);
        gl.glVertex3f(2, 0, 0);
        gl.glVertex3f(2, 0, -max_hght);
        gl.glVertex3f(1, 1.76f, 0);
        gl.glVertex3f(1, 1.76f, -max_hght);
        gl.glVertex3f(-1, 1.76f, 0);
        gl.glVertex3f(-1, 1.76f, -max_hght);
        gl.glVertex3f(-2, 0, 0);
        gl.glVertex3f(-2, 0, -max_hght);
        gl.glEnd();

        gl.glBegin(GL.GL_POLYGON);
        gl.glVertex3f(-2, 0, -max_hght);
        gl.glVertex3f(-1, -1.76f, -max_hght);
        gl.glVertex3f(1, -1.76f, -max_hght);
        gl.glVertex3f(2, 0, -max_hght);
        gl.glVertex3f(1, 1.76f, -max_hght);
        gl.glVertex3f(-1, 1.76f, -max_hght);
        gl.glEnd();

        gl.glScalef(.8f, .8f, 1);
        gl.glEndList();
    }

    private void compileTerrain(GL gl) {
        boolean flat = !main.bAutoDetect.isSelected();

        // Prepare images sizes
        if (texture == null) return;
        int tw = texture.getWidth();
        int th = texture.getHeight();

        BufferedImage img =
            (heightfield != null ? heightfield : texture);

        int hw = img.getWidth();
        int hh = img.getHeight();

        int aw = tw;
        int ah = th;

        if (aw > max_size) { aw = max_size; ah = (aw*th)/tw; }
        if (ah > max_size) { ah = max_size; aw = (ah*tw)/th; }

        scale = aw/(float)tw;

        // Triangles and colors
        tri = new float[aw*ah][];
        txt = new float[aw*ah][];

        // Fill arrays
        for (int ax = 0; ax < aw; ax++) {
            for (int ay = 0; ay < ah; ay++) {
                int rgb[] = new int[4];

                // Get color from surrounding points
                int tx = (ax*tw)/aw;
                int ty = (ay*th)/ah;
                float dx = (ax*tw)/(float)aw - tx;
                float dy = (ay*th)/(float)ah - ty;
                rgb[3] = rgb[2] = rgb[1] = rgb[0] = texture.getRGB(tx, ty);

                // This and the following commented out code provide correcter
                // (anti-aliasing) colors, but also takes 1.5 as much time to
                // execute. Maybe we can put thisinto a different thread some time and have it execute in the background

                /*
                if (tx + 1 < tw)
                    rgb[1] = texture.getRGB(tx + 1, ty);
                if (tx + 1 < tw && ty + 1 < th)
                    rgb[2] = texture.getRGB(tx + 1, ty + 1);
                if (ty + 1 < th)
                    rgb[3] = texture.getRGB(tx, ty + 1);
                */
                float[] col = new float[3];

                // Mix colors
                for (int i = 0; i < 3; i++)
                    col[2-i] = /*(dx)*(dy)**/((rgb[0] & MASK[i]) >> (8*i)) / 256f;
                        //+ (1-dx)*(dy)*((rgb[1] & MASK[i]) >> (8*i)) / 256f
                        //+ (1-dx)*(1-dy)*((rgb[2] & MASK[i]) >> (8*i)) / 256f
                        //+ (dx)*(1-dy)*((rgb[3] & MASK[i]) >> (8*i)) / 256f;

                txt[ay*aw + ax] = col;

                // Get heightfield from surrounding points
                int hx = (ax*hw)/aw;
                int hy = (ay*hh)/ah;

                rgb[3] = rgb[2] = rgb[1] = rgb[0] = img.getRGB(hx, hy);

                /*
                if (hx + 1 < hw)
                    rgb[1] = img.getRGB(hx + 1, hy);
                if (hx + 1 < hw && hy + 1 < hh)
                    rgb[2] = img.getRGB(hx + 1, hy + 1);
                if (hy + 1 < hh)
                    rgb[3] = img.getRGB(hx, hy + 1);
                */
                col = new float[3];

                // Mix heightfield
                for (int i = 0; i < 3; i++)
                    col[i] = /*(dx)*(dy)**/((rgb[0] & MASK[i]) >> (8*i)) / 256f;
                        //+ (1-dx)*(dy)*((rgb[1] & MASK[i]) >> (8*i)) / 256f
                        //+ (1-dx)*(1-dy)*((rgb[2] & MASK[i]) >> (8*i)) / 256f
                        //+ (dx)*(1-dy)*((rgb[3] & MASK[i]) >> (8*i)) / 256f;


                // Create height from heightfield
                float gray = (col[0] + col[1] + col[2])/3;

                // Gray distortion
                gray = 1 - distort*(float)Math.pow(1 - gray, 3)
                    - (1 - distort)*(1 - gray);

                // Check for consistency with previous points
                float[] res = new float[12];
                res[2] = res[5] = res[8] = res[11] = 
                    scale * max_hght * (heightfield != null ? -gray : (flat ? 0 : gray-1));

                // Top left point
                res[0] = ax; res[1] = ay;
                if (ax*ay > 0) res[2] = tri[ay*aw - aw + ax - 1][8];
                // Top right point
                res[3] = ax+1; res[4] = ay;
                if (ay > 0) res[5]= tri[ay*aw - aw + ax][8];
                // Lower right (this) Point
                res[6] = ax+1; res[7] = ay+1;
                // Lower Left point
                res[9] = ax; res[10]= ay+1;
                if (ax > 0) res[11]= tri[ay*aw + ax - 1][8];

                tri[ay*aw + ax] = res;
            }
        }

        // Fill Graphics
        gl.glNewList(h_lscape, GL.GL_COMPILE);
        gl.glTranslated(-aw/2, -ah/2, 0);

        for (int i = 0; i < ah; i++) {
            float[] pt = (float[]) tri[i*aw+0];
            float[] col = (float[]) txt[i*aw+0];

            gl.glBegin(GL.GL_TRIANGLE_STRIP);

            gl.glColor3f(col[0], col[1], col[2]);

            gl.glVertex3f(pt[9], pt[10], pt[11]);
            gl.glVertex3f(pt[0], pt[1], pt[2]);
            gl.glVertex3f(pt[6], pt[7], pt[8]);
            gl.glVertex3f(pt[3], pt[4], pt[5]);

            for (int j = 1; j < aw; j++) {
                pt = (float[]) tri[i*aw+j];
                col = (float[]) txt[i*aw+j];

                gl.glColor3f(col[0], col[1], col[2]);

                gl.glVertex3f(pt[6], pt[7], pt[8]);
                gl.glVertex3f(pt[3], pt[4], pt[5]);

            }
            gl.glEnd();
	}
        gl.glTranslated(aw/2, ah/2, 0);
        gl.glEndList();
    }

    /**
     * Listen to groups and maps.
     * @param obj the location
     */
    public void inform(IExport obj) {
        Token tok = (Token)obj;
        if (tok.isValid()) tokens.add(tok); else tokens.remove(tok);
    }
}
