import java.io.*;
import java.net.*;
import java.beans.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.regex.*;
import javax.imageio.*;
import javax.imageio.spi.*;
import java.awt.geom.*;
import javax.swing.*;
import org.apache.batik.bridge.*;
import org.apache.batik.dom.svg.*;
import org.apache.batik.gvt.*;
import org.apache.batik.gvt.renderer.*;
import org.apache.batik.util.*;
import org.apache.batik.ext.awt.image.*;
import org.w3c.dom.svg.*;

class Test {
	public static void main(String[] args) {
		String srch = "(^| |>)Abriel(<| |,|\\.|$)";
		String repl = "$1<b><font color=#11111>Abriel</font></b>$2";
		System.out.println(srch + " ::: " + repl);
		String in = "ough <a href=\"riel/Abriel.html\">Abriel Abbey</a> la";
		in = in.replaceAll(srch, repl);
		System.out.println(in);
	}
}
