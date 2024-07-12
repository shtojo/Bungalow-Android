package com.smj.bungalow;

class Tools {
    static final String[] weekDays = { "Sunday", "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday" };

    static final String[] months = { "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December" };

    private static final String B80L =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+-<>(){}[]#&%@!*^/";

    private static final char[] B80A = B80L.toCharArray();

    /**
     * Encode integer to base 80 character
     * @param n integer
     * @return base80 character
     */
    static char encodeB80(int n) {
        if (n > 79) {
            return '?';
        }
        return B80A[n];
    }

    /**
     * Decode B80 character to integer (range 0-79)
     * @param c Character to decode
     * @return Integer value (range 0-79) or -1 on error
     */
    static int decodeB80(char c) {
        int pos = B80L.indexOf(c);
        if (pos < 0) {
            return -1;
        }
        return pos;
    }
}
