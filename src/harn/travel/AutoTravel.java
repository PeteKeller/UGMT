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
package harn.travel;

import harn.repository.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.tree.*;

/**
 * The class that handles automatic travel. Route must be preselected.
 * @author Michael Jung
 */
public class AutoTravel implements ActionListener {
    /** State from main */
    private Main main;

    /** Remaining route */
    private ArrayList remainRoute;

    /** Constructor */
    public AutoTravel(Main aMain) {
        main = aMain;
    }

    /** IF method */
    public void actionPerformed(ActionEvent e) {
        String map = main.getState().getActive().getMap();

        // Get the end destination
        TreePath path = main.tree.getPathForLocation(main.gx, main.gy);
        if (map == null || path == null) return;
        String dest = path.getLastPathComponent().toString();
        if (path.getPathCount() == 3)
            dest = path.getPathComponent(1) + "/" + dest;

        // Get the source
        int cx = main.getState().getX();
        int cy = main.getState().getY();
        String cm = main.getState().getMap();

        // Get all vertices (locations)
        Hashtable str2loc = new Hashtable();
        ArrayList allRoutes = (ArrayList) main.myData.getRoutes(map);
        for (int i = 0; i < allRoutes.size(); i++) {
            Data.Route rout = (Data.Route) allRoutes.get(i);
            Location loc1 = new Location(rout.loc1(), rout.x(0), rout.y(0));
            Location loc2 = new Location(rout.loc2(), rout.x(-1), rout.y(-1));
            str2loc.put(rout.loc1(), loc1);
            str2loc.put(rout.loc2(), loc2);
        }

        // Get all vertex indices and keep destination and source
        Integer destI = null;
        Integer srcI = null;
        double dist = Double.MAX_VALUE;
        Location src = null;
        Hashtable ht = new Hashtable();
        Object[] idx = str2loc.keySet().toArray();
        for (int i = 0; i < idx.length; i++) {
            Integer idxI = new Integer(i);
            ht.put(idx[i], idxI);
            // Destination (equivalent on any map)
            String fullname = (String)idx[i];
            if (fullname.substring(fullname.lastIndexOf("//")+2).equals(dest))
                destI = idxI;

            // Source
            Location test = (Location)str2loc.get(idx[i]);
            AffineTransform nm2cm =
                main.map.getMap(test.getMap()).getTransform(cm);
            Point2D omp = nm2cm.transform
                (new Point2D.Double(test.x,test.y), null);
            double testd = (cx - omp.getX())*(cx - omp.getX()) +
                                (cy - omp.getY())*(cy - omp.getY());
            if (testd < dist) {
                dist = testd;
                src = test;
                srcI = idxI;
            }
        }

        // Dijkstra engine
        Dijkstra algo = new Dijkstra();

        // Add all "planar" routes
        for (int i = 0; i < allRoutes.size(); i++) {
            Data.Route rout = (Data.Route) allRoutes.get(i);
            algo.addDirectRoute
                (((Integer)ht.get(rout.loc1())).intValue(),
                 ((Integer)ht.get(rout.loc2())).intValue(),
                 rout);
            algo.addDirectRoute
                (((Integer)ht.get(rout.loc2())).intValue(),
                 ((Integer)ht.get(rout.loc1())).intValue(),
                 main.myData.invert(rout));
        }
        // Add all "vertical" routes
        for (int i = 0; i < idx.length; i++) {
            Location lloc = (Location)str2loc.get(idx[i]);
            String lmap = lloc.getMap(); 
            String loc = lloc.getId();
            ScaledMap smap = main.map.getMap(lmap);
            String[] names = smap.getReachableMaps();
            for (int j = 0; j < names.length; j++) {
                Location nloc = (Location)str2loc.get(names[j]+"//"+loc);
                if (nloc != null && !names[j].equals(lmap)) {
                    int[] x = new int[2];
                    int[] y = new int[2];
                    x[0] = lloc.x; x[1] = nloc.x;
                    y[0] = lloc.y; y[1] = nloc.y;
                    Data.Route rout = main.myData.getReflexive
                        (lmap+"//"+loc, names[j]+"//"+loc, x, y);
                    algo.addDirectRoute
                        (((Integer)ht.get(lmap+"//"+loc)).intValue(),
                         ((Integer)ht.get(names[j]+"//"+loc)).intValue(),
                         rout);
                    algo.addDirectRoute
                        (((Integer)ht.get(names[j]+"//"+loc)).intValue(),
                         ((Integer)ht.get(lmap+"//"+loc)).intValue(),
                         main.myData.invert(rout));
                }
            }
        }

        algo.execute(srcI, destI);
        // No way
        if (algo.getPredecessor(destI) == null) return;

        // Obtain proper route
        ArrayList xy = new ArrayList();
        for (Integer step = destI; !step.equals(srcI); step = algo.getPredecessor(step)) {
            Data.Route leg = algo.getShortestRouteTo(step);
            for (int i = leg.length(); i > 0; i--) {
                String loce = leg.loc2();
                if (i == 1) loce = leg.loc1();
                Location loc = new Location(loce, leg.x(i-1), leg.y(i-1));
                xy.add(loc);
            }
        }

        // Travel on route
        main.getMap().autotravel = true;
        travel(xy);
        main.getMap().autotravel = false;
        main.getMap().repaint();
    }

    /**
     * Travel the route. Take into travel time per day, breaks for stalling,
     * breaks for weather, breaks for encounters. Also consider map
     * equivalence.
     * @route contains the (backward) route
     */
    private void travel(ArrayList route) {
        int stall = 0;
        int i = route.size();

        if (i > 0) main.contTravel.setEnabled(true);

        for (;i > 0; i--) {
            // Break if appropriate
            if (stall > Integer.parseInt(main.eStall.getText()))
                break;

            // Calculate distance
            String om = main.getState().getMap();
            Location loc = (Location) route.get(i-1);
            String nm = loc.getMap();
            AffineTransform nm2om =
                main.map.getMap(nm).getTransform(om);
            Point2D omp = nm2om.transform
                (new Point2D.Double(loc.x,loc.y), null);
            int delta = main.getState().calcDist
                ((int)omp.getX(), (int)omp.getY(), main.bFree.getText());

            // Intersperse some sleep
            String tmp = main.bMove.getText().substring("Move: ".length());
            long moved = !tmp.equals("-") ? Data.cal.string2datetime(tmp) : 0;
            long allowed = Integer.parseInt(main.eTravel.getText())*Main.HOUR;
            if (delta > allowed) delta = 0;
            if (moved + delta > allowed) {
                main.map.move
                    (main.getState().getX(), main.getState().getY(),
                     Main.DAY - moved);
                i++; continue;
            }                

            // Break for encounters now
            if (main.getState().activeEncounter())
                break;

            // Try and move (take weather into account)
            String cond = main.getWeather().getCondition((int)omp.getX(),(int)omp.getY());
            if (delta >= 0 && !main.getState().getTypes().contains(cond)) {
                if ((int)omp.getX() != main.getState().getX() ||
                    (int)omp.getY() != main.getState().getY()) {
                    stall = 0;
                }
                else {
                    stall++;
                }
                main.map.move((int)omp.getX(), (int)omp.getY(), delta);
            }
            else {
                stall++;
                main.map.move
                    (main.getState().getX(), main.getState().getY(),
                     Main.stay);
                i++;
            }
        }
        if (i == 0) {
            remainRoute = null;
            main.contTravel.setEnabled(false);
        }
        else {
            remainRoute = new ArrayList();
            for (int j = 0; j < i; j++)
                remainRoute.add(route.get(j));
        }
    }

    /** Continue travel */
    public void continueTravel() {
        if (remainRoute == null) return;

        // Travel on route
        main.getMap().autotravel = true;
        travel(remainRoute);
        main.getMap().autotravel = false;
        main.getMap().repaint();
    }

    class Location {
        /** name of location */
        String name;
        /** coords of location */
        int x,y;
        /** Constructor */
        Location(String aName, int anX, int anY) {
            name = aName; x = anX; y = anY;
        }
        /** get map part of name */
        public String getMap() {
            return name.substring(0,name.lastIndexOf("//"));
        }
        /** get id part of name */
        public String getId() {
            return name.substring(name.lastIndexOf("//")+2);
        }
    }
}
