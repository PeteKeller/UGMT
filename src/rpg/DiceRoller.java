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
package rpg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class DiceRoller {

    /** Random generator */
    private static Random rand = new Random();

    /**
     * This method rolls an n-sided die. Invalid entries return themselves.
     * @param n maximum number to roll
     * @return random number in 1..n
     */
    public static int d(int n) {
        if (n > 0) {
            return 1 + rand.nextInt(n);
        }
        return n;
    }

    /**
     * This method rolls m n-sided Dice. Invalid entries return themselves,
     * i.e. m*n.
     * @param numDice Number of dice to roll
     * @param numSides Number of sides per die
     * @return random number in 1..n
     */
    public static int d(int numDice, int numSides) {
        if (numDice < 1) {
            return numDice * numSides;
        }
        int ret = 0;
        for (int i = 0; i < numDice; i++) {
            ret += d(numSides);
        }
        return ret;
    }


    /**
     * Roll n times dd and sum maximum m rolls.
     * @param numDice
     * @param sides
     * @param maxDice
     * @return
     */
    static public int rollMax(int numDice, int sides, int maxDice) {
        if (maxDice > numDice) {
            return d(numDice, sides);
        }

        ArrayList<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < numDice; i++) {
            l.add(d(sides));
        }
        Collections.sort(l, Collections.reverseOrder());

        int sum = 0;
        for (int i = 0; i < maxDice; i++) {
            sum += l.get(i);
        }

        return sum;
    }




    /**
     * Sets the seed for the Random Number Generator.  This ensures that the
     *     results are the same for each iteration of the testing.
     *     NOTE: USED FOR TESTING ONLY!
     *
     * @param seed Long number used to seed the Random Number Generator.
     */
    void setSeed(long seed) {
         rand.setSeed(seed);
     }
}
