package study;

import java.awt.print.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.util.regex.*;
import javax.imageio.*;

public class HPrint implements Printable {
    Hashtable ccs = new Hashtable();
    public static void main(String[] argv) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new HPrint());
        try {
            if (job.printDialog()) 
                job.print();
        }
        catch (Exception ex) {
            // Debug
            ex.printStackTrace();
        }
    }
    public int print (Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
        String dir = "/home/harntool/harntool/chars/";
        if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

        double sw = pf.getImageableWidth()/850;
        double sh = pf.getImageableHeight()/1100;
        ((Graphics2D)g).translate(pf.getImageableX(), pf.getImageableY());
        ((Graphics2D)g).scale(sw, sh);

        try {
            BufferedReader br = new BufferedReader
                (new FileReader(new File(dir + "4498.html")));
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
                    BufferedImage bi = ImageIO.read(new File(dir + m.group(2)));
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
                                
                        }
                        else if (format[i].equals("left")) {
                            x = Integer.parseInt
                                (format[i+1].substring(0,format[i+1].length() - 2));
                        }
                        else if (format[i].equals("top")) {
                            y = Integer.parseInt
                                (format[i+1].substring(0,format[i+1].length() - 2));
                        }
                        else {
                            //    font-weight:bold; font-variant:small-caps
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
