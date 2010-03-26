package edu.ohsu.cslu.util;

public class Arrays {

    public static void insertGaps(final Object[] oldArray, final int[] gapIndices, final Object[] newArray, final Object gap) {
        int currentGap = 0;
        int oldJ = 0;
        for (int newJ = 0; newJ < newArray.length; newJ++) {
            if (currentGap < gapIndices.length && oldJ == gapIndices[currentGap]) {
                newArray[newJ] = gap;
                currentGap++;
            } else {
                newArray[newJ] = oldArray[oldJ++];
            }
        }
    }

    public static void insertGaps(final Object[] oldArray, final int[] gapIndices, final Object[] newArray, final Object[] gaps) {
        int currentGap = 0;
        int oldJ = 0;
        for (int newJ = 0; newJ < newArray.length; newJ++) {
            if (currentGap < gapIndices.length && oldJ == gapIndices[currentGap]) {
                newArray[newJ] = gaps[currentGap];
                currentGap++;
            } else {
                newArray[newJ] = oldArray[oldJ++];
            }
        }
    }
}
