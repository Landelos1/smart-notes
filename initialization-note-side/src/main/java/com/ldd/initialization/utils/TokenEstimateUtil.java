package com.ldd.initialization.utils;

public class TokenEstimateUtil {

    private static final double ENGLISH_CHAR_TOKEN_RATIO = 0.3d;
    private static final double CHINESE_CHAR_TOKEN_RATIO = 0.6d;

    private TokenEstimateUtil() {
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        int chineseCount = 0;
        int totalLength = text.length();
        for (int i = 0; i < totalLength; i++) {
            char ch = text.charAt(i);
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                chineseCount++;
            }
        }

        int otherCount = totalLength - chineseCount;
        double estimated = chineseCount * CHINESE_CHAR_TOKEN_RATIO + otherCount * ENGLISH_CHAR_TOKEN_RATIO;
        return (int) Math.ceil(estimated);
    }
}
