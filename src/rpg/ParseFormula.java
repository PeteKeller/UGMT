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

import java.util.Locale;

public class ParseFormula {
    int numDice = 1;
    int numSides = 1;
    int numMod = 0;
    int maxNum = -1;
    int res;
    int current;
    char op;
    String errorCode;
    String formula;
    boolean foundD;
    boolean foundM;
    boolean foundPlusMinus;

    /**
     * Evaluate formula. Magic:-)
     */
    public String eval(String inFormula) {
        this.formula = inFormula;
        char[] list = formula.toUpperCase(Locale.ENGLISH).toCharArray();
        errorCode = "";
        res = 0;
        current = 0;
        op = ' ';
        foundD = false;
        foundM = false;
        foundPlusMinus = false;

        for (int i = 0; i < list.length; i++) {
            if (Character.isDigit(list[i]))
                current = current * 10 +
                    Integer.parseInt(Character.toString(list[i]));
            else {
                switch(op) {
                    case '+':
                        processPlus();
                        break;
                    case '-':
                        processMinus();
                        break;
                    case 'D':
                        processD();
                        break;
                    case 'M':
                        processM();
                        break;
                    case ' ':
                        res = current;
                        current = 0;
                        break;
                }
                switch (list[i]) {
                    case '+':
                    case '-':
                    case 'D':
                    case 'M':
                        op = list[i];
                        break;
                    default :
                        errorCode = formula;
                        break;
                }
            }
        }
        switch(op) {
            case '+':
                processPlus();
                break;
            case '-':
                processMinus();
                break;
            case 'D':
                processD();
                break;
            case 'M':
                processM();
                break;
            default:
                errorCode = formula;
                break;
            }
        if ("".equals(errorCode)) {
            int myRoll;

            if (maxNum == -1) {
                myRoll = DiceRoller.d(numDice, numSides);
            } else {
                myRoll = DiceRoller.rollMax(numDice, numSides, maxNum);
            }
            myRoll += numMod;
            return Integer.toString(myRoll);
        } else {
            return "ERROR: " + errorCode;
        }
    }

    private boolean isDGood() {
        return !foundD;
    }

    private void processD() {
        if (isDGood()){

            if (res == 0) {
                res = 1;
            }
            numDice = res;
            numSides = current;
            current = 0;
            foundD = true;
        } else {
            errorCode = formula;
        }
    }

    private boolean isMGood() {
        return (foundD && !foundM);
    }

    private void processM() {
        if (isMGood()){

            maxNum = current;
            current = 0;
            foundM = true;
        } else {
            errorCode = formula;
        }
    }

    private boolean isPlusMinusGood() {
        return (!foundPlusMinus);
    }

    private void processPlus() {
        if (isPlusMinusGood()){

            numMod = current;
            current = 0;
            foundPlusMinus = true;
        } else {
            errorCode = formula;
        }
    }


    private void processMinus() {
        if (isPlusMinusGood()){

            numMod = -1 * current;
            current = 0;
            foundPlusMinus = true;
        } else {
            errorCode = formula;
        }
    }
}
