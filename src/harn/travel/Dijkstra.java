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

import java.util.*;

/**
 * An implementation of Dijkstra's shortest path algorithm. It computes the
 * shortest path to all locations. The output of the algorithm is the shortest
 * distance from a start location to every other location, and the shortest
 * path from the start location to every other.
 *
 * <p> Upon calling execute, the results of the algorithm are made available
 * by calling getPredecessor and getMinDist.
 * 
 * <p> Originally from Renaud Waldura &lt;renaud+tw@waldura.com&gt;
 * @author Michael Jung
 */
class Dijkstra {
    /**
     * This comparator orders vertices according to their shortest
     * distances, in ascending fashion. If two vertices have the same
     * shortest distance, we compare the vertices themselves.
     */
    private Comparator shortestDistance = new Comparator() {
            public int compare(Object left, Object right) {
                // This doesn't work for distances close to MAX_VALUE
                int result = getMinDist((Integer)left) -
                    getMinDist((Integer)right);
                return (result == 0) ? ((Integer)left).compareTo((Integer)right) : result;
            }
        };
    
    /** Working set of vertices, kept ordered by shortest distance. */
    private SortedSet unsettledNodes = new TreeSet(shortestDistance);
    
    /**
     * Vertices for which the shortest distance to the source has been
     * found.
     */
    private Set settledNodes = new HashSet();
    
    /**
     * The currently known shortest distance for all locs.
     */
    private Map shortestDistances = new Hashtable();
    
    /**
     * Predecessors list: maps a vertex to its predecessor in the
     * spanning tree of shortest paths.
     */
    private Map predecessors = new Hashtable();
    
    /**
     * Distance array of arrays.
     */
    private ArrayList distances = new ArrayList();

    /**
     * Run Dijkstra's shortest path algorithm on the map.  The results of
     * the algorithm are available through getPredecessor and
     * getMinDist upon completion of this method.
     * @param start start vertex
     * @param destination destination vertex.
     */
    public void execute(Integer start, Integer destination) {
        settledNodes.clear();
        unsettledNodes.clear();
        
        shortestDistances.clear();
        predecessors.clear();
        
        // add source
        setMinDist(start, 0);
        unsettledNodes.add(start);
    
        // Current vertex
        Integer u;
        
        // Extract the vertex with the shortest distance
        while ((u = extractMin()) != null) {
            // destination reached, stop
            if (u == destination) break;
            settledNodes.add(u);
            relaxNeighbors(u);
        }
    }
    
    /**
     * Extract the vertex with the currently shortest distance, and
     * remove it from the priority queue.
     * @return minimum vertex, or null if queue is empty.
     */
    private Integer extractMin() {
        if (unsettledNodes.isEmpty()) return null;
        Integer min = (Integer) unsettledNodes.first();
        unsettledNodes.remove(min);
        return min;
    }
    
    /**
     * Compute new shortest distance for neighboring vertices and update
     * if a better distance is found.
     * @param u the vertex
     */
    private void relaxNeighbors(Integer u) {
        Iterator i = getDestinations(u.intValue()).iterator();
        while(i.hasNext()) {
            Integer v = (Integer) i.next();
            // skip vertex already settled
            if (settledNodes.contains(v)) continue;
            
            if (getMinDist(v) > getMinDist(u) +
                getDistance(u.intValue(), v.intValue())) {
                
                // assign new shortest distance and mark unsettled
                setMinDist
                    (v, getMinDist(u) +
                     getDistance(u.intValue(), v.intValue()));
                
                // assign predecessor in shortest path
                setPredecessor(v, u);
            }
        }
    }
    
    /**
     * @return shortest distance from the source to the given vertex, or
     * Integer.MAX_VALUE if there is no route to the destination.
     */
    private int getMinDist(Integer idx) {
        Integer d = (Integer) shortestDistances.get(idx);
        return (d == null) ? Integer.MAX_VALUE : d.intValue();
    }
    
    /**
     * Set the new shortest distance for the given vertex,
     * and re-balance the set according to new shortest distances.
     * @param idx the vertex to set
     * @param distance new shortest distance value
     */        
    private void setMinDist(Integer idx, int distance) {
        // this crucial step ensure no duplicates will be created in the
        // queue when an existing unsettled vertex is updated with a new
        // shortest distance
        unsettledNodes.remove(idx);
        shortestDistances.put(idx, new Integer(distance));
        
        // re-balance the sorted set according to the new shortest
        // distance found (see the comparator the set was initialized
        // with)
        unsettledNodes.add(idx);
    }
    
    /**
     * @return vertex leading to the given vertex on the shortest path, or
     * null, if there is no route to the destination.
     */
    public Integer getPredecessor(Integer idx) {
        return (Integer) predecessors.get(idx);
    }

    /**
     * @return route leading to the given vertex on the shortest path, or
     * null, if there is no route to the destination.
     */
    public Data.Route getShortestRouteTo(Integer idx) {
        if (getPredecessor(idx) == null) return null;
        return (Data.Route) getRoute
            (getPredecessor(idx).intValue(), idx.intValue());
    }
    
    /** Setter */
    private void setPredecessor(Integer a, Integer b) {
        predecessors.put(a, b);
    }

    /**
     * Link two vertices by a direct route with the given distance.
     */
    public void addDirectRoute(int start, int end, Data.Route route) {
        // Fill empty
        for (int i = distances.size(); i <= start; i++)
            distances.add(null);
        // Get second array
        ArrayList al = (ArrayList)distances.get(start);
        if (al == null) {
            al = new ArrayList();
            distances.set(start, al);
        }
        // Fill empty
        for (int i = al.size(); i <= end; i++)
            al.add(null);
        al.set(end, route);
    }
    
    /**
     * @return the distance between the two vertices, or 0 if no path
     * exists.
     */
    private int getDistance(int start, int end) {
        Data.Route route = getRoute(start, end);
        if (route == null) return 0;
        return route.distance();
    }

    /**
     * @return the route between the two vertices, or null if no path
     * exists.
     */
    private Data.Route getRoute(int start, int end) {
        if (distances.size() <= start) return null;
        ArrayList al = (ArrayList)distances.get(start);
        if (al == null) return null;
        if (al.size() <= end) return null;
        Data.Route route = (Data.Route) al.get(end);
        if (route == null) return null;
        return route;
    }

    /**
     * @return the list of all valid destinations from the given loc.
     */
    private ArrayList getDestinations(int idx) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < distances.size(); i++) {
            if (getDistance(idx, i) > 0) {
                list.add(new Integer(i));
            }
        }
        return list;
    }
    
    /**
     * @return the list of all vertices leading to the given vertex.
     */
    private ArrayList getPredecessors(int idx) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < distances.size(); i++) {
            if (getDistance(i, idx) > 0) {
                list.add(new Integer(i));
            }
        }
        return list;
    }
}
