/**
 * UGMT : Universal Gamemaster tool
 * Copyright (c) 2009 Michael Jung
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

package rpg;

//import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class DiceRollerTest {
    DiceRoller instance;

    public DiceRollerTest() {
        instance = new DiceRoller();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new DiceRoller();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of d method, of class Dice.
     */
    @Test
    public void testD_int() {
        System.out.println("dY");
        long seed = 12344321;
        instance.setSeed(seed);
        int n = 20;
        int expResult = 11;
        int result = DiceRoller.d(n);
        assertEquals(expResult, result);
        System.out.println("  Testing negative input");
        n = -1;
        result = DiceRoller.d(n);
        assertEquals(n, result);
        System.out.println("  Testing input of Zero");
        n = 0;
        result = DiceRoller.d(n);
        assertEquals(n, result);
        System.out.println("  Testing input of One");
        n = 1;
        result = DiceRoller.d(n);
        assertEquals(n, result);
    }

    /**
     * Test of d method, of class Dice.
     */
    @Test
    public void testD_int_int() {
        System.out.println("XdY");
        long seed = 1234554321;
        instance.setSeed(seed);
        int numDice = 3;
        int numSides = 6;
        int expResult = 10;
        int result = DiceRoller.d(numDice, numSides);
        assertEquals(expResult, result);
        System.out.println("  Number of dice = -1");
        numDice = -1;
        numSides = 6;
        expResult = -6;
        result = DiceRoller.d(numDice, numSides);
        assertEquals(expResult, result);
        System.out.println("  Number of dice = -5");
        numDice = -5;
        numSides = 6;
        expResult = -30;
        result = DiceRoller.d(numDice, numSides);
        assertEquals(expResult, result);
        System.out.println("  Number of dice = 3, Number of sides = 0");
        numDice = 3;
        numSides = 0;
        expResult = 0;
        result = DiceRoller.d(numDice, numSides);
        assertEquals(expResult, result);
        System.out.println("  Number of dice = 3, Number of sides = -2");
        numDice = 3;
        numSides = -2;
        expResult = -6;
        result = DiceRoller.d(numDice, numSides);
        assertEquals(expResult, result);
    }

    /**
     * Test of rollMax method, of class Dice.
     */
    @Test
    public void testRollMax() {
        System.out.println("rollMax");
        long seed = 569;
//        instance.setSeed(seed);
//        System.out.println("  " + Dice.d(6));
//        System.out.println("  " + Dice.d(6));
//        System.out.println("  " + Dice.d(6));
//        System.out.println("  " + Dice.d(6));
        instance.setSeed(seed);
        int n = 4;
        int d = 6;
        int m = 3;
        int expResult = 15;
        int result = DiceRoller.rollMax(n, d, m);
        assertEquals(expResult, result);
    }


//    /**
//     * Method used to find proper seed to ensure correct results.
//     */
//    private void findSeed() {
//        int roll1;
//        int roll2;
//        int roll3;
//        int roll4;
//        Random rand = new Random();
//
//        for (long i = 0; i <= Long.MAX_VALUE; i++) {
//            rand.setSeed(i);
//            roll1 = rand.nextInt(6) + 1;
//            roll2 = rand.nextInt(6) + 1;
//            roll3 = rand.nextInt(6) + 1;
//            roll4 = rand.nextInt(6) + 1;
//            if ((roll1 == 5) && (roll2 == 4) && (roll3 == 1) && (roll4 == 6)) {
//                System.out.println(i);
//                break;
//            }
//        }
//    }
}
