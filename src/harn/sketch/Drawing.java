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
package harn.sketch;

import harn.repository.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import rpg.*;
import java.io.*;
import javax.imageio.*;

/**
 * Class that contains the sketch area
 * @authot Michael Jung
 */
public class Drawing extends JTextArea {
    /** Last Point */
    private int lx, ly;

    /** Default size */
    int width, height;

    /** Draw buffer */
    private BufferedImage buf;

    /** Main reference */
    private Main main;

    /** May draw */
    private boolean drawable;

    /** Tree reference */
    TreeNode treeref;

    /** are document changes from the user */
    private boolean user;

    /**
     * Constructor
     * @param x width
     * @param y height
     */
    Drawing(Main aMain, int w, int h) {
        super();
        main = aMain;

        setOpaque(false);
        setMargin(new Insets(5,5,5,5));
        setLineWrap(true);
        setColumns(0);
        setRows(0);

        user = true;
        lx = ly = 0;

        buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = buf.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, buf.getWidth(), buf.getHeight());
        width = w;
        height = h;

        addMouseListener(new MouseAdapter() { 
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    lx = e.getX();
                    ly = e.getY();
                }
            });

        addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (!drawable) return;
                    Graphics g = buf.getGraphics();
                    int x = e.getX();
                    int y = e.getY();
                    if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0)
                        return;
                    setSelectionStart(0);
                    setSelectionEnd(0);
                    if (((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) &&
                        ((e.getModifiers() & InputEvent.CTRL_MASK) == 0)) {
                        g.setColor(Color.BLACK);
                        g.drawLine(lx, ly, x, y);
                    }
                    else {
                        g.setColor(Color.WHITE);
                        g.fillOval(lx - 7, ly - 7, 14, 14);
                        g.fillOval(x - 7, y - 7, 14, 14);
                        double l = Math.sqrt((x-lx)*(x-lx) + (y-ly)*(y-ly));
                        if (l != 0) {
                            int x1 = x - (int)(7*(y-ly)/l);
                            int y1 = y + (int)(7*(x-lx)/l);
                            int x2 = x + (int)(7*(y-ly)/l);
                            int y2 = y - (int)(7*(x-lx)/l);
                            int x3 = lx + (int)(7*(y-ly)/l);
                            int y3 = ly - (int)(7*(x-lx)/l);
                            int x4 = lx - (int)(7*(y-ly)/l);
                            int y4 = ly + (int)(7*(x-lx)/l);
                            g.fillPolygon
                                (new int[] {x1, x2, x3, x4},
                                 new int[] {y1, y2, y3, y4}, 4); 
                        }
                    }
                    lx = x;
                    ly = y;
                    main.myData.setDirty(true);
                    repaint();
                }
            });

        DocumentListener dl = new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    if (user) main.myData.setDirty(true);
                }
                public void removeUpdate(DocumentEvent e) {
                    if (user) main.myData.setDirty(true);
                }
                public void changedUpdate(DocumentEvent e) {
                    if (user) main.myData.setDirty(true);
                }
            };

        getDocument().addDocumentListener(dl);
    }

    /**
     * paint Methods
     */
    public void paintComponent(Graphics g) {
        if (buf != null)
            g.drawImage(buf, 0, 0, buf.getWidth(), buf.getHeight(), this);
        super.paintComponent(g);
    }

    /**
     * Clear.
     */
    public void clear() {
        user = false;
        buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = buf.getGraphics();
        g.setPaintMode();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, buf.getWidth(), buf.getHeight());
        setText(null);
        lx = ly = 0;
        repaint();
        user = true;
    }

    /**
     * Display a new Note.
     */
    public void set(TreeNode aRef) {
        clear();
        treeref = aRef;
        if (aRef == null) return;
        File txt = aRef.getTxt();
        File png = aRef.getPng();

        setEditable(false);
        drawable = false;

        try {
            if (txt != null) {
                BufferedReader br = new BufferedReader(new FileReader(txt));
                StringBuffer total = new StringBuffer();
                String str;
                while ((str = br.readLine()) != null)
                    total.append(str + "\n");
                br.close();
                user = false;
                setText(total.toString());
                setEditable(txt.canWrite());
                user = true;
            }
            if (png != null) {
                buf = ImageIO.read(png);
                drawable = png.canWrite();
                if (drawable && buf == null)
                    buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                repaint();
            }
        }
        catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Save to a file. Assumes files are writable.
     */
    public void save() {
        if (treeref == null) return;
        File txt = treeref.getTxt();
        File png = treeref.getPng();
        if (txt != null && !txt.canWrite()) txt = null;
        if (png != null && !png.canWrite()) png = null;

        try {
            if (txt != null) {
                FileWriter w = new FileWriter(txt);
                w.write(getText(), 0, getText().length());
                w.close();
            }
            if (png != null) {
                ImageIO.write(buf, "png", png);
            }
        }
        catch (IOException e) {
            // Debug
            e.printStackTrace();
        }
    }

    /** Overridden IF method */
    public Dimension getPreferredSize() {
        Dimension other = super.getPreferredSize();
        int sw = (other.width > width) ? other.width : width;
        int sh = (other.height > height) ? other.height : height;
        return new Dimension(sw,sh);
    }

    /** Overridden IF method */
    public boolean getScrollableTracksViewportWidth() { return false; }
}
