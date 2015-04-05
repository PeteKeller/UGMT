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
package harn.repository;

import rpg.IExport;
import javax.swing.ImageIcon;
import java.io.File;

/**
 * Object that represents a PC or NPC. This object groups attributes (such as
 * strength), skills (such as Fishing), and a heraldic icon. It also allows
 * for a HTML representation of the character. (This may be null, if a
 * seperate creation process is required.)
 * @author Michael Jung
 */
public abstract class Char implements IExport {
    /** type constant */
    public final static String TYPE = "Character";

    /**
     * Final implementation. subclasses are differentiated via version.
     * @return type attribute
     */
    public final String getType() { return TYPE; }

    /**
     * This is the first version of this interface.
     * @return version (1.0)
     */
    public double version() { return 1.1; }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * This method provides something of a PIN for a character. This should be
     * provided by external agents when accessing this character. (The code
     * may be null.)
     * @return the PIN code
     */
    public abstract String getCode();

    /**
     * Get an attribute.
     * @param key attribute name
     * @return attribute value
     */
    public abstract String getAttribute(String key);

    /**
     * Get a skill.
     * @param key skill name
     * @return skill value
     */
    public abstract String getSkill(String key);

    /**
     * Get the attribute names list.
     * @return attribute names list
     */
    public abstract String[] getAttributeList();

    /**
     * Get the skill names list.
     * @return skill names list
     */
    public abstract String[] getSkillList();

    /**
     * Get the heraldry icon
     * @return heraldry icon
     */
    public abstract ImageIcon getHeraldry();

    /**
     * Return the full character HTML page.
     * @return HTML file
     */
    public abstract File getPage();

    /**
     * Several characters may be valid at a given time. Returns the state of
     * this character. A valid character is a character that the plugin knows
     * about.  In abstract terms, an invalid object was released to garbage
     * collection by the implementing plugin.
     * @return true if a valid character
     */
    public abstract boolean isValid();

    /**
     * Set the character active. The meaning of this is dependant on
     * plugin. It may mean that the caharcter is shown on a GUI.
     */
    public abstract void setActive();

    /**
     * Is the character a combatant?
     * @return combatant status
     */
    public abstract boolean isCombatant();

    /**
     * Make the character a combatant or remove him from list.
     * @param status combatant status
     */
    public abstract void makeCombatant(boolean status);
}
