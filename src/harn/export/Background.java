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
package harn.export;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The class that controls the socket. It listens and pipes out appropriate
 * HTTP answers.
 * @author Michael Jung
 */
public class Background {

    /** List of all exportables */
    ArrayList exports;
    
    /** listener port */
    final int port;

    /** Main reference */
    Main main;

    /**
     * Constructor
     * @param al list of exportables
     */
    Background(ArrayList al, int aPort, Main aMain) {
        main = aMain;
        exports = al;
        port = aPort;

        // Start socket
        Thread main = new Thread() {
                public void run() {
                    try {
                        ServerSocket servsock = new ServerSocket(port);
                        Socket sock;
                        while ((sock = servsock.accept()) != null) {
                            Thread t = new MyThread(sock);
                            t.start();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        main.start();
    }

    /**
     * This class is used to asynchronously answer a HTTP request for an
     * exportable.
     */
    class MyThread extends Thread {
        /** socket for communication */
        Socket sock;

        /**
         * Constructor.
         * @param aSock socket for communication.
         */
        MyThread(Socket aSock) { sock = aSock; }

        /**
         * Main thread mehod. Reads from socket and Writes to socket.
         */
        public void run() {
            OutputStream os = null;
            PrintWriter osr = null;
            try {
                BufferedReader bsr = new BufferedReader
                    (new InputStreamReader(sock.getInputStream()));
                String in = null;
                String query = "";
                char version = '1';
                while ((in = bsr.readLine()) != null) {
                    String[] res = in.split("  *");
                    if (res.length > 0 && res[0].equals("GET")) {
                        query = res[1].substring(1);
                        version = in.charAt(in.length() - 1);
                        break;
                    }
                }
                HttpExport gl = null;
                for (int i = 0; (gl == null) && (i < exports.size()); i++) {
                    gl = (HttpExport) exports.get(i);
                    if (!gl.choose(query))
                        gl = null;
                }
                
                // Simple Security 
                if (query.indexOf("..") > -1) gl = null;
                os = sock.getOutputStream();
                osr = new PrintWriter(os);
                
                if (query.length() == 0) {
                    osr.println("HTTP/1." + version + " 200 OK");
                    printTop(osr);
                }
                else {
                    if (gl != null) {
                        osr.println("HTTP/1." + version + " 200 OK");
                        gl.export(query, os, osr);
                    }
                    else {
                        // Error
                        osr.println("HTTP/1." + version + " 403 FORBIDDEN");
                        osr.println();
                        osr.println("<html><head><body>Forbidden</body></html>");
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try { osr.close(); } catch(Exception e) {}
                try { os.close(); } catch(Exception e) {}
                try { sock.close(); } catch(Exception e) {}
            }
        }
    }

    /** Top index */
    private void printTop(PrintWriter osr) {
        osr.println("Content-type: text/html\n");
        osr.println("<html><head><body>");
        for (int i = 0; i < exports.size(); i++) {
            String name = ((HttpExport)exports.get(i)).name();
            if (((HttpExport)exports.get(i)).choose(name))
                osr.println("<a href=\"/" + name + "\">" + name + "</a><br>");
        }
    }
}
