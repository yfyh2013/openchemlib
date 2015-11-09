/*
 * Project: DD_core
 * @(#)String2DoubleArray.java
 *
 * Copyright (c) 1997- 2015
 * Actelion Pharmaceuticals Ltd.
 * Gewerbestrasse 16
 * CH-4123 Allschwil, Switzerland
 *
 * All Rights Reserved.
 *
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.
 *
 * Author: MvK
 */

package com.actelion.research.util.convert;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class String2DoubleArray {

    private static final String DELIMITER = " \t\n\r\f,;";


    public static double [] convert(String sLine) throws Exception{

        StringTokenizer st = new StringTokenizer(sLine, DELIMITER);

        double[] dArray = new double[st.countTokens()];

        int ii = 0;
        while (st.hasMoreTokens()) {
            String sNumber = st.nextToken();
            // The formatting sign "'" is not recocnized by the Double.valueOf(...)
            // function.
            sNumber = sNumber.replaceAll("'", "");
            try {
                dArray[ii] = Double.parseDouble(sNumber);
            }
            catch (NumberFormatException ex1) {
                throw new  NumberFormatException("No number: " + sNumber + ".");
            }
            ii++;
        }

        return dArray;
    }

    public static double[] convert(ArrayList sArrList) throws Exception{

        ArrayList dArrList = new ArrayList();
        int iSize = 0;
        for (int ii = 0; ii < sArrList.size(); ii++) {
            double[] arr = convert( (String) sArrList.get(ii));
            dArrList.add(arr);
            iSize += arr.length;
        }
        double[] dArrAll = new double[iSize];
        int index = 0;
        for (int ii = 0; ii < dArrList.size(); ii++) {
            double[] arr = (double[]) dArrList.get(ii);
            for (int jj = 0; jj < arr.length; jj++) {
                dArrAll[index++] = arr[jj];
            }
        }
        return dArrAll;
    }

    public static void main(String [] args) {
        String str = "1 2 3 ";
        try {
            double[] arr = convert(str);
            for (int ii = 0; ii < arr.length; ii++) {
              System.out.println(arr[ii]);
            }

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}