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
package harn.index;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import java.net.URL;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import rpg.*;
import harn.repository.*;

/**
 * The main class for the index plugin. Takes care of the GUI and
 * instantiates all subordinate objects.
 * @author Michael Jung
 */
public class SearchEngine {
    /** Back ref */
    private Main main;

    /** Constraints for buttons and rows */
    private GridBagConstraints constr;

    /** select button pressed; used for highlighting */
    private boolean select = false;

    /** Currently searched text */
    private String txt;

    /** Color value used for highlighting */
    private String selCol;

    /** Constructor */
    SearchEngine(Main aMain) {
        main = aMain;
        selCol = main.myProps.getProperty("search.highlight");
        constr = new GridBagConstraints();
        constr.fill = GridBagConstraints.BOTH;
        constr.gridx = 0;
        constr.weighty = 1;
        constr.gridy = 0;
    }

    /** Main method */
    public void search() {
	GridBagConstraints constr3 = new GridBagConstraints();
	constr3.gridx = constr3.gridy = 0;
	constr3.weightx = 1;
	constr3.gridy = 2;
        constr3.weighty = 1;
	constr3.fill = GridBagConstraints.BOTH;
        constr.gridy = 0;
        main.results.removeAll();
        main.results.revalidate();

        Thread t = new MyThread();
        t.start();
    }

    /** Get a reader (possible preselected) */
    public InputStream getStream(URL url) throws IOException {
        InputStream is = url.openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuffer buf = new StringBuffer();
        String in;
        String srch = "(^|[ >])" + txt + "($|[< ,\\.])";
        String repl = "$1<b><font color=\"#" + selCol + "\">"
            + txt + "</font></b>$2";
        while ((in = br.readLine()) != null) {
            if (select)
                in = in.replaceAll(srch,repl);
            buf.append(in);
        }
        select = false;
        return new ByteArrayInputStream(buf.toString().getBytes());
    }

    /** Thread that searches */
    class MyThread extends Thread {
        MyThread() {
        }
        public void run () {
            main.search.setEnabled(false);
            txt = main.search.getText();
            if (txt.length() < 2) return;

            Enumeration list =
                ((DefaultMutableTreeNode)main.getIndexModel().getRoot()).depthFirstEnumeration();
            Hashtable file2anch2node = new Hashtable();
            while (list.hasMoreElements()) {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode)list.nextElement();
                if (!node.isLeaf()) continue;
                TreeNode[] p = node.getPath();
                String key = "";
                for (int i = 1; i < p.length; i++)
                    key += "/" +
                        ((DefaultMutableTreeNode)p[i]).getUserObject().toString();
                key = key.substring(1);
                String fname = main.myData.getFile(key);
                String file = fname;
                String anch = "";
                int idx = -1;
                if ((idx = fname.indexOf("#")) > -1) {
                    file = fname.substring(0,idx);
                    anch = fname.substring(idx+1);
                }
                Hashtable anch2node = (Hashtable)file2anch2node.get(file);
                if (anch2node == null) {
                    anch2node = new Hashtable();
                    file2anch2node.put(file, anch2node);
                }
                anch2node.put(anch, node);
            }

            Pattern panch = Pattern.compile(".*<a name=\"([^\"]+)\".*");
            Iterator iter = file2anch2node.keySet().iterator();
            while (iter.hasNext()) {
                try {
                    String file = (String) iter.next();
                    Hashtable anch2node = (Hashtable)file2anch2node.get(file);
                    BufferedReader bsr = new BufferedReader
                        (new FileReader(new File(main.myPath + file)));
                    String in = null;
                    String anch = "";
                    int ln = 0;
                    HashSet nodes = new HashSet();
                    while ((in = bsr.readLine()) != null) {
                        Matcher manch = panch.matcher(in);
                        if (manch.matches())
                            anch = manch.group(1);
                        in = in.replaceAll("\\<[^>]*\\>","");
                        in = in.replaceAll(".*\\>","");
                        in = in.replaceAll("\\<.*","");
                        in = in.replaceAll("^ *","");
                        if (in.indexOf(txt) > -1) {
                            DefaultMutableTreeNode tn =
                                (DefaultMutableTreeNode) anch2node.get(anch);
                            if (nodes.contains(tn)) continue;
                            nodes.add(tn);
                            if (tn != null)
                                SwingUtilities.invokeLater
                                    (new MyThread2(ln, tn, in));
                        }
                        ln++;
                    }
                    bsr.close();
                }
                catch (IOException e) {
                    // Debug
                    e.printStackTrace();
                }
            }
            main.search.setEnabled(true);
        }
    }

    class MyThread2 extends Thread {
        String in;
        DefaultMutableTreeNode tn;
        int line;
        MyThread2(int ln, DefaultMutableTreeNode ptn, String p) {
            in = p;
            tn = ptn;
            line = ln;
        }
        public void run() {
            JButton b = new JButton(tn.getUserObject().toString());
            b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        select = true;
                        main.view.addPropertyChangeListener(new PropertyChangeListener() {
                                public void propertyChange(PropertyChangeEvent evt) {
                                    main.view.getCaret().setSelectionVisible
                                        (true);
                                }
                            });
                        main.getIndex().addSelectionPath
                            (new TreePath(tn.getPath()));
                    }
                });
            Object[] tp = tn.getPath();
            String full = "";
            for (int i = 0; i< tp.length; i++)
                full += "/" + tp[i].toString();
            b.setToolTipText
                ("<html>Click to go select index entry<br>" +
                 full.substring(1) + "</html>");
            JTextField tf = new JTextField(in);
            tf.setEnabled(false);

            constr.gridx = 0;
            main.results.add(b, constr);
            constr.gridx = 1;
            main.results.add(tf, constr);
            constr.gridy++;
            main.sresults.revalidate();
        }
    }
}
