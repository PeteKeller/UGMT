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
package harn.magic;

import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import javax.swing.text.Document;
import java.util.regex.*;
import java.util.Iterator;
import java.net.URL;

/**
 * This class shows the adaptation options to the spell on display
 * @author Michael Jung
 */
public class AdaptMenu extends JPopupMenu {
    /** fall back menu entry */
    private JMenuItem mi1;

    /** main reference */
    private Main main;

    /** Operation for evalTerm */
    static public final String ops[] = { "/", "x", "\\+", "-" };

    /**
     * Constructor
     */
    public AdaptMenu(Main aMain) {
        main = aMain;
        mi1 = new JMenuItem("Standard");
        mi1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    MutableTreeNode tn =
                        (MutableTreeNode) main.tree.getLastSelectedPathComponent();
                    if (tn != null && tn.isLeaf()) {
                        String key = tn.toString();

                        String page = ((Data.DataTreeNode)tn).getLink();
                        if (page == null) return;
                        try {
                            Document doc = main.otherdisplay.getDocument();
                            doc.putProperty(Document.StreamDescriptionProperty, null);
                            URL url = new URL("file:" + main.myPath + page);
                            main.otherdisplay.setPage(url);
                        }
                        catch (Exception ex) {
                            // Debug
                            ex.printStackTrace();
                        }
                    }
                    
                }
            });

        add(mi1);
        addSeparator();
    }

    /**
     * Adapts the menu structure to a new spell
     * @param name name of new spell on display
     */
    public void recalc(String sName) {
        removeAll();

        add(mi1);
        addSeparator();
        
        if (main.state == null || main.state.holderList == null) return;
        sName = sName.replaceAll("\\([IV]*\\)", "");

        Iterator iter = main.state.holderList.keySet().iterator();
        while (iter.hasNext()) {
            String hName = (String) iter.next();
            Holder holder = (Holder) main.state.holderList.get(hName);
            for (int i = 0; i < holder.getChildCount(); i++) {
                Spell sp = (Spell)holder.getChildAt(i);
                String lsName = sp.name().replaceAll("\\([IV]*\\)", "");
                if (sName.equals(lsName)) {
                    JMenuItem mi = new JMenuItem(hName);
                    mi.addActionListener(new MyActionListener(sp));
                    add(mi);
                }
            }
        }
    }

    /**
     * Evaluate math terms
     */
    static public String evalTerm(String term, Spell spell) {
        String ret = term;
        ret = ret.replaceAll("[RC]?ML(\\s*[^0-9\\s])", spell.getML() + "$1");
        ret = ret.replaceAll
            ("[RC]?SI(\\s*[^0-9\\s])", Integer.toString(Integer.parseInt(spell.getML())/10) + "$1");
        for (int i = 0; i < ops.length; i++) {
            Pattern p = Pattern.compile
                ("([0-9]+)\\s*" + ops[i] + "\\s*([0-9]+)");
            Matcher m = p.matcher(ret);
            while (m.find()) {
                int f1 = Integer.parseInt(m.group(1));
                int f2 = Integer.parseInt(m.group(2));

                int r = 0;
                switch(i) {
                    case 0: r = f1/f2; break;
                    case 1: r = f1*f2; break;
                    case 2: r = f1+f2; break;
                    case 3: r = f1-f2; break;
                }
                ret = m.replaceFirst(Integer.toString(r));
                m = p.matcher(ret);
            }
        }
        return ret;
    }

    /**
     * Action Listener class.
     */
    class MyActionListener implements ActionListener {
        Spell spell;
        MyActionListener(Spell aSpell) { spell = aSpell; }
        public void actionPerformed(ActionEvent e) {
            JEditorPane ep = main.otherdisplay;
            Document doc = ep.getDocument();
            URL url = ep.getPage();
            doc.putProperty(Document.StreamDescriptionProperty, null);
            try {
                ep.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            JEditorPane ep = main.otherdisplay;
                            if (e.getPropertyName().equals("page")) {
                                ep.setText(evalTerm(ep.getText(), spell));
                                ep.removePropertyChangeListener(this);
                            }
                        }
                    });
                ep.setPage(url);
            }
            catch (Exception ex) {
                // Debug
                ex.printStackTrace();
            }
        }
    }
}
