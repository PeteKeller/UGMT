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
import java.awt.print.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.util.regex.*;
import javax.imageio.*;
import javax.print.attribute.*;
import rpg.*;
import javax.print.attribute.standard.*;

/**
 * This class capsulates the main printing facility.
 * @author Michael Jung
 */
public class Printer implements Printable {
    /** Main reference */
    private Main main;

    /** local reference of char to print */
    private String mychar;

    /**
     * Constructor.
     * @param aMain main reference
     */
    Printer(Main aMain) {
        main = aMain;
    }

    /**
     * Method to call for printing.
     * @param achar character name to print
     */
    public void print(String achar) {
        mychar = achar;

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        PageFormat pf = job.defaultPage();
        HashPrintRequestAttributeSet attSet = new HashPrintRequestAttributeSet();
        attSet.add(new MediaPrintableArea(0f, 0f, (float)pf.getWidth()*72, (float)pf.getHeight()*72, MediaPrintableArea.INCH));
        try {
            if (job.printDialog(attSet)) job.print();
        }
        catch (Exception ex) {
            // Debug
            ex.printStackTrace();
        }
    }

    /**
     * Callback for system printing.
     */
    public int print (Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
        Hashtable ccs = new Hashtable();
        
        if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

        double sw = pf.getImageableWidth()/850;
        double sh = pf.getImageableHeight()/1100;
        ((Graphics2D)g).translate(pf.getImageableX(), pf.getImageableY());
        ((Graphics2D)g).scale(sw, sh);

        try {
            BufferedReader br = new BufferedReader
                (new FileReader
                 (new File(main.myPath + main.getState().getCode(mychar) + ".html")));
            StringBuffer buf = new StringBuffer();
            String str;
            while ((str = br.readLine()) != null) {
                // CCS
                Matcher m = Pattern.compile
                    (".*div\\.(\\w+) \\{ (.*) \\}.*").matcher(str);
                if (m.matches()) ccs.put(m.group(1), m.group(2));

                // Img line
                m = Pattern.compile
                    ("<img style=\"(.*)\" src=\"(.*)\">").matcher(str);
                if (m.matches()) {
                    int x = 0;
                    int y = 0;
                    int w = 0;
                    int h = 0;
                    String[] format =
                        (m.group(1)).split("(:|;) *");
                    for (int i = 0; i < format.length; i += 2) {
                        if (format[i].equals("left")) {
                            x = Integer.parseInt
                                (format[i+1].substring(0,format[i+1].length() - 2));
                        }
                        else if (format[i].equals("top")) {
                            y = Integer.parseInt
                                (format[i+1].substring(0,format[i+1].length() - 2));
                        }
                        else if (format[i].equals("width")) {
                            w = Integer.parseInt
                                (format[i+1].substring(0,format[i+1].length() - 2));
                        }
                        else if (format[i].equals("height")) {
                            h = Integer.parseInt
                                (format[i+1].substring(0,format[i+1].length() - 2));
                        }
                    }
                    BufferedImage bi = ImageIO.read(new File(main.myPath + m.group(2)));
                    g.drawImage
                        (bi, x, y, x + w, y + h, 0, 0, bi.getWidth(), bi.getHeight(), null);
                }

                // Div
                m = Pattern.compile
                    ("<div class=\"(\\w+)\" style=\"(.*)\">(.*)</div>").matcher(str);
                if (m.matches()) {
                    int x = 0;
                    int y = 0;
                    Font of = g.getFont();
                    String[] format =
                        (ccs.get(m.group(1)) + m.group(2)).split("(:|;) *");
                    for (int i = 0; i < format.length; i += 2) {
                        if (format[i].equals("font-size")) {
                            int sz = Integer.parseInt
                                (format[i+1].substring(0,format[i+1].length() - 2));
                            Font nf = new Font
                                (of.getName(), of.getStyle(), sz);
                            g.setFont(nf);
                        }
                        else if (format[i].equals("font-weight:")) {
                            if (format[i+1].equals("bold")) {
                                Font nf = new Font
                                    (of.getName(), of.getStyle() | Font.BOLD, of.getSize());
                                g.setFont(nf);
                            }
                            else if ((of.getStyle() & Font.BOLD) != 0) {
                                    Font nf = new Font
                                        (of.getName(), of.getStyle() - Font.BOLD, of.getSize());
                                    g.setFont(nf);
                            }
                                
                        }
                        else if (format[i].equals("left")) {
                            x = Integer.parseInt
                                (format[i+1].substring(0,format[i+1].length() - 2));
                        }
                        else if (format[i].equals("top")) {
                            y = Integer.parseInt
                                (format[i+1].substring(0,format[i+1].length() - 2));
                        }
                        of = g.getFont();
                    }
                    g.drawString(m.group(3), x, y + of.getSize());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return Printable.PAGE_EXISTS;
    }
}
