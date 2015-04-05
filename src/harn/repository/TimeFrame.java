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

/**
 * Object that represents a calendar. This object can be queried to obtain
 * subunits of time (e.g. months) and string representations. It makes a few
 * common assumptions about the structure of time (e.g. equal length
 * years). This is not quite correct but an implementation of a java Calendar
 * object isn't quite up to the job either.
 * @author Michael Jung
 */
public abstract class TimeFrame implements IExport {
    /** type constant */
    public final static String TYPE = "TimeFrame";

    /**
     * Final implementation. subclasses are differentiated viy version.
     * @return type attribute
     */
    public final String getType() { return TYPE; }

    /**
     * This is the first version of this interface.
     * @return version (1.0)
     */
    public double version() { return 1.0; }

    /**
     * Implement as specified in superclass.
     * @return name attribute
     */
    public abstract String getName();

    /**
     * Pretty prints a (small) time. Prototypical is a "h:mm" format.
     * @param date date to print
     */
    public abstract String time2string(long date);

    /**
     * Pretty prints a (big) date. Prototypical is a "d.m.y" format.
     * @param date date to print
     */
    public abstract String date2string(long date);

    /**
     * Pretty prints date and time. Prototypical is a "d.m.y h:mm" format.
     * @param date date/time to print
     */
    public abstract String datetime2string(long date);

    /**
     * Utility that converts a string date format into a long date. This
     * utility must be able to parse all dates it prints with the three
     * methods above.
     * @param str string version of date
     * @return long date
     */
    public abstract long string2datetime(String str);

    /**
     * Get the moon phase as percentage value. Positive is waxing, negative is
     * waning, 100 is a full moon, 0 a new moon.
     * @param time time for which to determine moon phase
     * @return moon phase
     */
    public abstract int getMoon(long time);

    /**
     * Returns dawn of the day in which time lies.
     * @param time time in a day
     * @return dawn of the day
     */
    public abstract long getDawn(long time);

    /**
     * Returns dusk of the day in which time lies.
     * @param time time in a day
     * @return dawn of the day
     */
    public abstract long getDusk(long time);

    /**
     * Get Season.
     * @param time time to determine season for
     * @return season string
     */
    public abstract String getSeason(long date);

    /**
     * Calculate the sunsign for this month.
     * @param date time in minutes to determine sunsign for
     * @return sunsign
     */
    public abstract String getSunsign(long date);
}
