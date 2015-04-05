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

import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;
import java.util.regex.*;

/**
 * The class that models the armour analysis display.
 * @author Michael Jung
 */
public class ArmourAnalysis implements TableModel {
    /** main reference */
    private Main main;

    /** column names */
    private String[] colNames;

    /** row names */
    private LinkedList rowNames;

    /** total armour values (int[]) */
    private Hashtable values;

    /** Constructor */
    public ArmourAnalysis(Main aMain) {
        main = aMain;
        colNames = main.getData().columnNames();
        rowNames = main.getData().rowNames();
        values = new Hashtable();
    }

    /** Recalc all values */
    public void recalc() {
        rowNames = main.getData().rowNames();
        Hashtable nvalues = new Hashtable();
        Iterator iter = main.aitems.all.iterator();
        while (iter.hasNext()) {
            ArmourLine item = (ArmourLine) iter.next();
            if (item.used()) {
                Hashtable tmp =
                    main.getData().getCoverage(item.item(), item.material());
                Iterator iter2 = tmp.keySet().iterator();
                while (iter2.hasNext()) {
                    String key = (String) iter2.next();
                    int[] vavals = (int[]) nvalues.get(key);
                    int[] tavals = (int[]) tmp.get(key);
                    if (vavals == null)
                        vavals = new int[tavals.length];
                    nvalues.put(key, vavals);
                    for (int i = 0; i < vavals.length; i++) {
                        String q = item.quality();
                        if (q.length() < 4) {
                            if (tavals[i] > 0)
                                vavals[i] += prefunc
                                    (Math.max(1, tavals[i] + State.intQ(q)));
                        }
                        else {
                            Pattern p = Pattern.compile
                                ("(?:\\D+(\\d+))?(?:\\D+(\\d+))?(?:\\D+(\\d+))?(?:\\D+(\\d+)).*");
                            Matcher m = p.matcher(q);
                            m.matches();
                            vavals[i] += prefunc
                                (m.groupCount() > i && m.group(i+1) != null ?
                                 Integer.parseInt(m.group(i+1)) :
                                 tavals[i]);
                                
                        }
                    }
                }
            }
        }

        // Add common to r/l/f/b
        iter = nvalues.keySet().iterator();
        values = new Hashtable();
        while (iter.hasNext()) {
            String key = (String) iter.next();

            // Basic
            values.put(key, nvalues.get(key));
            if (key.indexOf('/') < 0) continue;

            // Do the dual part
            String prefix = key.substring(0,key.indexOf("/"));
            int[] orig = (int[]) nvalues.get(key);
            int[] base = (int[]) nvalues.get(prefix);
            int[] dual = null;

            String postfix = null;
            if (key.endsWith("/l")) postfix = "/r";
            if (key.endsWith("/r")) postfix = "/l";
            if (key.endsWith("/f")) postfix = "/b";
            if (key.endsWith("/b")) postfix = "/f";

            dual = (int[]) nvalues.get(prefix + postfix);
            if (dual == null) dual = new int[orig.length];
            values.put(prefix + postfix, dual);

            for (int i = 0; base != null && i < orig.length; i++)
                dual[i] += base[i];
            values.remove(prefix);
        }

        // Redo row names
        iter = values.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (key.indexOf('/') < 0) continue;

            String prefix = key.substring(0,key.indexOf("/"));
            String postfix = null;
            if (key.endsWith("/l")) postfix = "/r";
            if (key.endsWith("/r")) postfix = "/l";
            if (key.endsWith("/f")) postfix = "/b";
            if (key.endsWith("/b")) postfix = "/f";

            for (int i = 0; i < rowNames.size(); i++) {
                if (rowNames.get(i).equals(prefix)) {
                    rowNames.set(i, key);
                    rowNames.add(i, prefix + postfix);
                    break;
                }
            }
        }
        main.table.revalidate();
        main.table.repaint();
    }

    /** Get row name */
    public String getRowName(int ri) {
        return (String)rowNames.get(ri);
    }

    /** Get materials for row */
    public Object[] getRowMaterial(int ri) {
        HashSet ret = new HashSet();
        Iterator iter = main.aitems.all.iterator();
        while (iter.hasNext()) {
            ArmourLine item = (ArmourLine) iter.next();
            if (item.used()) {
                Hashtable tmp =
                    main.getData().getCoverage(item.item(), item.material());
                Iterator iter2 = tmp.keySet().iterator();
                while (iter2.hasNext()) {
                    String key = (String) iter2.next();
                    if (getRowName(ri).equals(key))
                        ret.add(item.material());
                }
            }
        }
        return ret.toArray();
    }

    /** More than standard addition? */
    public int prefunc(int x) {
        if (main.getData().getNorm() == null) return x;
        if (main.getData().getNorm().startsWith("L2")) {
            return x*x;
        }
        return x;
    }

    /** More than standard addition? */
    public int postfunc(int x) {
        String norm = main.getData().getNorm();
        if (norm == null) return x;
        if (norm.equals("L2max")) {
            return (int) Math.ceil(Math.sqrt(x));
        }
        if (norm.equals("L2min")) {
            return (int) Math.floor(Math.sqrt(x));
        }
        if (norm.equals("L2")) {
            return (int) Math.round(Math.sqrt(x));
        }
        return x;
    }

    /** IF method */
    public String getColumnName(int ci) {
        if (ci == 0) return "-";
        return colNames[ci - 1];
    }
    /** IF method */
    public Object getValueAt(int ri, int ci) {
        if (ci == 0) return rowNames.get(ri);
        if (rowNames.get(ri).equals("-")) return null;
        int[] res = (int[])values.get(rowNames.get(ri));
        if (res == null) return "0";
        return Integer.toString(postfunc(res[ci - 1]));
    }

    /** IF method */
    public int getRowCount() { return rowNames.size(); }

    /** IF method */
    public int getColumnCount() { return colNames.length + 1; }

    /** IF method */
    public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }

    /** IF method */
    public void removeTableModelListener(TableModelListener l) {}

    /** IF method */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}

    /** IF method */
    public void	addTableModelListener(TableModelListener l) {}

    /** IF method */
    public Class getColumnClass(int columnIndex) { return String.class; }
}
