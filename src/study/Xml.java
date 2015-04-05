/*
Skills?
16" NAME="Weapon" (already in equip)
16" NAME="Weapon ML"
*/
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import org.w3c.dom.*;

public class Xml {
    static Document doc;
    public Xml() {
        try {
            // Rules
	    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder b = f.newDocumentBuilder();
            doc = b.parse(new File("/home/harntool/hc.xml"));
        }
        catch (Exception e) {
	    // Debugging
            e.printStackTrace();
        }
    }
    public static void main(String[] argv) {
        Hashtable attr = new Hashtable();
        Hashtable skill = new Hashtable();
        HashSet equip = new HashSet();
        String[] nspells = new String[100]; // psi, ritual, spells
        String[] mspells = new String[100];

        long cur = System.currentTimeMillis();
        Xml xml = new Xml();

        Hashtable htn = new Hashtable();
        NodeList tree = doc.getDocumentElement().getChildNodes();
        int fidx = 0;
	for (int i = 0; i < tree.getLength(); i++) {
            NodeList tree2 = tree.item(i).getChildNodes();
            for (int j = 0; j < tree2.getLength(); j++) {
                Node child = tree2.item(j);
                NamedNodeMap catt = child.getAttributes();
                if (child.getNodeName().equals("FIELD")) {
                    htn.put
                        (new Integer(fidx++),
                         catt.getNamedItem("NAME").getNodeValue());
                }
                if (child.getNodeName().equals("ROW")) {
                    Hashtable ht = new Hashtable();
                    String bd = null;
                    String bm = null;
                    String by = null;
                    String cv = null;
                    String cn = null;
                    NodeList tree3 = tree2.item(j).getChildNodes();
                    int idx = 0;
                    for (int k = 0; tree3 != null && k < tree3.getLength(); k++) {
                        child = tree3.item(k);
                        if (child.getNodeName().equals("COL")) {
                            Integer iidx = new Integer(idx++);
                            Object o = htn.get(iidx);
                            NodeList tree4 = child.getChildNodes();
                            for (int l = 0; tree4 != null && l < tree4.getLength(); l++) {
                                if (tree4.item(l).getNodeName().equals("DATA")) {
                                    Node data = tree4.item(l);
                                    if (data.getChildNodes() != null && data.getChildNodes().getLength() != 0) {
                                        String val = data.getChildNodes().item(0).toString();
                                        if (val == null || val.length() == 0) continue;
                                        if (o.equals("Character Name")) attr.put("Name", val);
                                        if (o.equals("Age")) attr.put("Age", val);
                                        if (o.equals("Attr: AGL")) attr.put("Agility", val);
                                        if (o.equals("Attr: AUR")) attr.put("Aura", val);
                                        if (o.equals("Attr: CML")) attr.put("Comeliness", val);
                                        if (o.equals("Attr: DEX")) attr.put("Dexterity", val);
                                        if (o.equals("Attr: EYE")) attr.put("Eyesight", val);
                                        if (o.equals("Attr: HRG")) attr.put("Hearing", val);
                                        if (o.equals("Attr: INT")) attr.put("Intelligence", val);
                                        if (o.equals("Attr: MOR")) attr.put("Morality", val);
                                        if (o.equals("Attr: SML")) attr.put("Smell", val);
                                        if (o.equals("Attr: STA")) attr.put("Stamina", val);
                                        if (o.equals("Attr: STR")) attr.put("Strength", val);
                                        if (o.equals("Attr: VOI")) attr.put("Voice", val);
                                        if (o.equals("Attr: WIL")) attr.put("Will", val);
                                        if (o.equals("Complexion")) attr.put("Complexion", val);
                                        if (o.equals("Culture")) attr.put(o, val);
                                        if (o.equals("Deity")) attr.put(o, val);
                                        if (o.equals("Endurance")) attr.put(o, val);
                                        if (o.equals("Eye color")) attr.put("Eye Color", val);
                                        if (o.equals("Hair color")) attr.put("Hair Color", val);
                                        if (o.equals("Height")) attr.put(o, val);
                                        if (o.equals("Sex")) attr.put(o, val);
                                        if (o.equals("Social class")) attr.put("Social Class", val);
                                        if (o.equals("Species")) attr.put(o, val);
                                        if (o.equals("Voice")) attr.put(o, val);
                                        if (o.equals("Weight")) attr.put(o, val);
                                        if (o.equals("Size")) attr.put(o, val);
                                        if (o.equals("Piety")) attr.put(o, val);
                                        if (o.equals("Birthplace")) attr.put(o, val);
                                        if (o.equals("Clanhead")) attr.put(o, val);
                                        if (o.equals("Estrangement")) attr.put(o, val);
                                        if (o.equals("Frame")) attr.put(o, val);
                                        if (o.equals("Dodge")) skill.put(o, val);
                                        if (o.equals("Character title")) attr.put("Own Occupation", val);
                                        if (o.equals("Parent title")) attr.put("Parent Occupation", val);
                                        if (o.equals("Parent relation")) attr.put("Parent", val);
                                        if (o.equals("Equipment list 1")) equip.add(val);
                                        if (o.equals("Equipment list 2")) equip.add(val);
                                        if (o.equals("Equipment list 3")) equip.add(val);
                                        if (o.equals("Weapon")) equip.add(val);
                                        if (o.equals("Armour")) equip.add(val.replaceAll("(.*) \\((.*)\\)","$2 $1"));
                                        if (o.equals("Spell name")) nspells[l] = val;
                                        if (o.equals("Spell ML")) mspells[l] = val;
//if (o.equals("Psionic talent")) mspells[80 + l] = val;
//if (o.equals("Psionic talent ML")) mspells[80 + l] = val;
                                        if (o.equals("Ritual Name")) nspells[40 + l] = val;
                                        if (o.equals("Ritual ML")) mspells[40 + l] = val;
                                        if (o.equals("Medical comma separated")) attr.put(o, val);
//if (o.equals("Import field from old database")) System.out.println(val);
//if (o.equals("Skill list 1 Combat")) System.out.println(val);
//if (o.equals("Skill list 1 Psionics")) System.out.println(val);
//?if (o.equals("Skill list 2 Comb ML")) System.out.println(val);
//?if (o.equals("Skill list 2 Comb name")) System.out.println(val);
//?if (o.equals("Skill list 2 Comm ML")) System.out.println(val);
//?if (o.equals("Skill list 2 Comm name")) System.out.println(val);
//?if (o.equals("Skill list 2 Lore ML")) System.out.println(val);
//?if (o.equals("Skill list 2 Lore name")) System.out.println(val);
//?if (o.equals("Skill list 2 Phys ML")) System.out.println(val);
//?if (o.equals("Skill list 2 Phys name")) System.out.println(val);
//?if (o.equals("Skill list 2 Psi ML")) System.out.println(val);
//?if (o.equals("Skill list 2 Psi name")) System.out.println(val);
//if (o.equals("80" NAME="Skill ML")) System.out.println(val);
//if (o.equals("80" NAME="Skill multiplier")) System.out.println(val);
//if (o.equals("Skill name")) System.out.println("1."+val);
//if (o.equals("Skill name 2")) System.out.println("2."+val);
//if (o.equals("80" NAME="Skill OML")) System.out.println(val);
                                        if (o.equals("Sibling rank")) {
                                            int iof = val.indexOf("of");
                                            if (iof > 0) {
                                                attr.put("Sibling Rank", val.substring(0,iof));
                                                attr.put("Family Size", val.substring(iof + 3));
                                            }
                                            else
                                                attr.put("Sibling Rank", val);
                                        }
                                        if (o.equals("Age: Expected"))
                                            if (!val.equals("n/a"))
                                                attr.put("Degeration Age",
                                                         Integer.toString(Integer.parseInt(val) - 10));
                                        if (o.equals("Psyche"))
                                            if (!val.equals("No mental disorders"))
                                                attr.put("Psyche", val);
                                        if (o.equals("Convocation name")) {
                                            cn = val;
                                            if (cv != null)
                                                skill.put(cn, cv);
                                        }
                                        if (o.equals("Convocation CML")) {
                                            cv = val;
                                            if (cn != null)
                                                skill.put(cn, cv);
                                        }
                                        if (o.equals("Birthdate: Day")) {
                                            bd = val;
                                            if (bm != null & by != null)
                                                attr.put("Birthdate", bd+"-"+bm+"-"+by);
                                        }
                                        if (o.equals("Birthdate: Month")) {
                                            bm = month(val);
                                            if (bd != null & by != null)
                                                attr.put("Birthdate", bd+"-"+bm+"-"+by);
                                        }
                                        if (o.equals("Birthdate: Year")) {
                                            by = val;
                                            if (bm != null & bd != null)
                                                attr.put("Birthdate", bd+"-"+bm+"-"+by);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    static String month(String mm) {
        if (mm.equals("1")) return "Nuz";
        if (mm.equals("2")) return "Peo";
        if (mm.equals("3")) return "Kel";
        if (mm.equals("4")) return "Nol";
        if (mm.equals("5")) return "Lar";
        if (mm.equals("6")) return "Agr";
        if (mm.equals("7")) return "Azu";
        if (mm.equals("8")) return "Hal";
        if (mm.equals("9")) return "Sav";
        if (mm.equals("10")) return "Ilv";
        if (mm.equals("11")) return "Nav";
        return "Mor";
    }
}
