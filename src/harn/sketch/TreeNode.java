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

import javax.swing.tree.*;
import javax.swing.event.*;
import java.io.*;
import java.beans.*;
import javax.imageio.*;
import javax.swing.*;
import rpg.Framework;
import harn.repository.Sketch;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.net.*;

/**
 * Tree node - instances represents directory structure.
 * @author Michael Jung
 */
public class TreeNode extends DefaultMutableTreeNode {
    /** Base files */
    protected File png;
    protected File txt;
    protected File dir;
    protected File html;

    /** back reference */
    protected Main main;

    /** Proxied sketch object */
    protected PSketch sketch;

    /** Constructor */
    public TreeNode(Main aMain, String name, File f) {
        super(name);
        main = aMain;
        sketch = new PSketch(this, name);
        if (f.isDirectory()) {
            dir = f;
            return;
        }
        if (f.getName().endsWith(".png")) png = f;
        if (f.getName().endsWith(".txt")) txt = f;
        if (f.getName().endsWith(".html")) html = f;
    }

    /** Determine modifiable */
    public boolean isReadOnly() {
        return (txt != null && !txt.canWrite() ||
                png != null && !png.canWrite() ||
                html != null && !html.canWrite() ||
                dir != null && !dir.canWrite());
    }

    /** Set the txt file */
    public void setTxt(File f) { txt = f; }

    /** Set the html file */
    public void setHtml(File f) { html = f; }

    /** Set the png file */
    public void setPng(File f) { png = f; }

    /** Get the directory */
    public File getDir() { return dir; }

    /** Get the text */
    public File getTxt() { return txt; }

    /** Get the image */
    public File getPng() { return png; }

    /** Get the Html */
    public File getHtml() { return html; }

    /** Overwritten Tree Node interface */
    public boolean isLeaf() { return dir == null; }

    /** Overwritten Tree Node interface */
    public boolean getAllowsChildren() { return dir != null; }

    /** Delete this object */
    public boolean delete() {
        boolean ret = true;
        if (txt != null) ret &= txt.delete();
        if (png != null) ret &= png.delete();
        if (html != null) ret &= html.delete();
        if (dir != null) ret &= dir.delete();
        if (ret) sketch.valid = false;
        return ret;
    }

    /** Rename this object */
    public String rename(String name) {
        // Do not rename root!
        if (this == main.sketchtree) return dir.getName();

        boolean invalid = (name.length() == 0) || (name.indexOf('/') > -1);
        if (txt != null) {
            File f = new File(txt.getParent() + Framework.SEP + name + ".txt");
            if (!txt.renameTo(f) || invalid)
                return txt.getName().replaceFirst(".txt$","");
            txt = f;
        }

        if (html != null) {
            File f = new File(html.getParent() + Framework.SEP + name + ".html");
            if (!html.renameTo(f) || invalid)
                return html.getName().replaceFirst(".html$","");
            html = f;
        }

        if (png != null) {
            File f = new File(png.getParent() + Framework.SEP + name + ".png");
            if (!png.renameTo(f) || invalid)
                return png.getName().replaceFirst(".png$","");
            png = f;
        }

        if (dir != null) {
            File f = new File(dir.getParent() + Framework.SEP + name);
            if (!dir.renameTo(f) || invalid)
                return dir.getName();
            dir = f;
        }

        sketch.name = name;
        return null;
    }
}
