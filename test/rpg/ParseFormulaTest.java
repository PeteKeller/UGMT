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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ParseFormulaTest {
    ParseFormula instance;
    int expMod;
    int expMax;
    int expDice;
    int expSides;
    String result;
    String formula;


    public ParseFormulaTest() {
        instance = new ParseFormula();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new ParseFormula();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of eval method, of class ParseFormula.
     */
    @Test
    public void failures() {
        System.out.println("failures");

        formula = "1q10";
        System.out.println("  " + formula);
        result = instance.eval(formula);
        assertEquals("Failed - illegal operator: ", "ERROR: " + formula, result);

        formula = "q10";
        System.out.println("  " + formula);
        result = instance.eval(formula);
        assertEquals("Failed - illegal operator: ", "ERROR: " + formula, result);

        formula = "bored";
        System.out.println("  " + formula);
        result = instance.eval(formula);
        assertEquals("Failed - text only: ", "ERROR: " + formula, result);

        formula = "1d20d10";
        System.out.println("  " + formula);
        result = instance.eval(formula);
        assertEquals("Failed - double D: ", "ERROR: " + formula, result);

        formula = "1m20m10";
        System.out.println("  " + formula);
        result = instance.eval(formula);
        assertEquals("Failed - double M: ", "ERROR: " + formula, result);

        formula = "4m20d3";
        System.out.println("  " + formula);
        expMax = 3;
        expMod = 0;
        expDice = 4;
        expSides = 20;
        result = instance.eval(formula);
        assertEquals("Failed - M and D reversed: ", "ERROR: " + formula, result);

        formula = "1d20+10+2";
        System.out.println("  " + formula);
        result = instance.eval(formula);
        assertEquals("Failed - double Plus: ", "ERROR: " + formula, result);

        formula = "1d20+10-2";
        System.out.println("  " + formula);
        result = instance.eval(formula);
        assertEquals("Failed - double Plus: ", "ERROR: " + formula, result);

    }

    /**
     * Test of eval method, of class ParseFormula.
     */
    @Test
    public void xDy() {
        System.out.println("xDy");
        formula = "d6";
        System.out.println("  " + formula);
        expMax = -1;
        expMod = 0;
        expDice = 1;
        expSides = 6;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);

        formula = "2d6";
        System.out.println("  " + formula);
        expMax = -1;
        expMod = 0;
        expDice = 2;
        expSides = 6;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);

        formula = "12d20";
        System.out.println("  " + formula);
        expMax = -1;
        expMod = 0;
        expDice = 12;
        expSides = 20;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);

        formula = "1d20";
        System.out.println("  " + formula);
        expMax = -1;
        expMod = 0;
        expDice = 1;
        expSides = 20;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);
    }
    /**
     * Test of eval method, of class ParseFormula.
     */
    @Test
    public void xDyMz() {
        System.out.println("eval");
        String expResult = "";

        formula = "4d20m3";
        System.out.println("  " + formula);
        expMax = 3;
        expMod = 0;
        expDice = 4;
        expSides = 20;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);

        formula = "d20m3";
        System.out.println("  " + formula);
        expMax = 3;
        expMod = 0;
        expDice = 1;
        expSides = 20;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);
    }

    /**
     * Test of eval method, of class ParseFormula.
     */
    @Test
    public void xDyMzQa() {
        System.out.println("xDyMzQa");

        formula = "4d20m3+1";
        System.out.println("  " + formula);
        expMax = 3;
        expMod = 1;
        expDice = 4;
        expSides = 20;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);

        formula = "4d20m3-5";
        System.out.println("  " + formula);
        expMax = 3;
        expMod = -5;
        expDice = 4;
        expSides = 20;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);

    }

    /**
     * Test of eval method, of class ParseFormula.
     */
    @Test
    public void xDyQa() {
        System.out.println("xDyQa");

        formula = "5d6+1";
        System.out.println("  " + formula);
        expMax = -1;
        expMod = 1;
        expDice = 5;
        expSides = 6;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);

        formula = "3d12-5";
        System.out.println("  " + formula);
        expMax = -1;
        expMod = -5;
        expDice = 3;
        expSides = 12;
        result = instance.eval(formula);
        assertEquals("Max", expMax, instance.maxNum);
        assertEquals("Mod", expMod, instance.numMod);
        assertEquals("Dice", expDice, instance.numDice);
        assertEquals("Sides", expSides, instance.numSides);

    }

}