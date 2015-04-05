/*
 * UGMT : Unversal Gamemaster tool
 * Copyright (c) 2004 Michael Jung
 *
 * Any party obtaining a copy of these files is granted, free of charge, a
 * full and unrestricted irrevocable, world-wide, paid up, royalty-free,
 * nonexclusive right and license to deal in this software and
 * documentation files (the "Software"), including without limitation the
 * rights to use, copy, modify, merge, publish and/or distribute copies of
 * the Software, and to permit persons who receive copies from any such 
 * party to do so, with the only requirement being that this copyright 
 * notice remain intact.
 */
package rpg;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * The Splash screen class. Used to show the copyrights.
 * @author Michael Jung
 */
public class Splash {
    static String[] copy = new String[] {
        "Various authors have contributed and claim copyright for material ",
        "contained in the UGMT. Following is an alphabetical list. ",
        "Please read the COPYRIGHT.* files in the plugins' directories for details."
    };

    static public JLabel loadlabel;

    static JFrame createSplash(String[] all) throws IOException {
        GridBagConstraints constr = new GridBagConstraints();
        constr.insets = new Insets(10,10,10,10);
        constr.gridx = constr.gridy = 0;
        constr.gridwidth = 2;

        // Frame
        JLabel splabel;
        final JFrame splash = new JFrame("UGMT");
        JPanel spanel = new JPanel(new GridBagLayout());
        splash.getContentPane().add(spanel);

        // Title
        splabel = new JLabel("COPYRIGHT");
        spanel.add(splabel, constr);

        constr.insets.top = constr.insets.bottom = 1;

        for (int i = 0; i < copy.length; i++) {
            constr.gridy++;
            if (i == copy.length - 1) constr.insets.bottom = 10;
            splabel = new JLabel(copy[i]);
            spanel.add(splabel, constr);
        }

        constr.insets.bottom = 1;

        HashSet holders = new HashSet();

        for (int i = 0; all != null && i < all.length; i++) {
            // All plug-ins
            File pf = new File(all[i]);
            if (pf != null && pf.isDirectory() && !all[i].equals("lib")) {
                String[] copy = pf.list(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.startsWith("COPYRIGHT");
                        }
                    });

                // All copyright notices
                for (int j = 0; copy != null && j < copy.length; j++) {
                    File af = new File(pf.getAbsolutePath(), copy[j]);
                    BufferedReader bsr = new BufferedReader
                        (new InputStreamReader(new FileInputStream(af)));
                    String in = null;
                    if (bsr.ready()) {
                        in = bsr.readLine();
                        String[] names = in.split("(,? +and +)|([:/,]+ *)");
                        for (int k = 1; k < names.length; k++) {
                            holders.add(names[k]);
                        }
                    }
                }
            }
        }

        TreeSet sort = new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2) {
                    String s1 = ((String) o1).replaceFirst(".* ","") + (String)o1;
                    String s2 = ((String) o2).replaceFirst(".* ","") + (String)o2;
                    return s1.compareTo(s2);
                }
            });
        sort.addAll(holders);
        Iterator iter = sort.iterator();
        String hold = "";
        while (iter.hasNext()) {
            hold += (String) iter.next() + (iter.hasNext() ? ", " : "");
            if (hold.length() > 50) hold = addLabel(spanel, constr, hold);
        }
        if (hold.length() > 0) addLabel(spanel, constr, hold);

        constr.insets.top = 10;
        constr.insets.bottom = 10;
        // End
        loadlabel = new JLabel("Loading ...");
        constr.gridy = constr.gridy + 1;
        spanel.add(loadlabel, constr);

        SwingUtilities.invokeLater(new Thread() {
                public void run() { splash.pack(); }
            });
        Dimension scrsize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension mysize = splash.getSize();
        splash.setLocation
            ((scrsize.width - mysize.width)/2, 
             (scrsize.height - mysize.height)/2);
        splash.setVisible(true);
        return splash;
    }

    private static String addLabel(JPanel spanel, GridBagConstraints constr, String hold) {
        JLabel splabel = new JLabel(hold);
        Font of = splabel.getFont();
        Font nf = new Font
            (of.getName(),
             of.getStyle() + Font.ITALIC,
             of.getSize());
        splabel.setFont(nf);
        constr.gridy++;
        spanel.add(splabel, constr);
        return "";
    }
}
