package com.app.godo.utils;

import java.util.HashMap;
import java.util.Map;

public class SerbianNormalizer {

    private static final Map<Character, String> CYRILLIC_TO_LATIN = new HashMap<>();

    static {
        // Uppercase Cyrillic → Latin
        CYRILLIC_TO_LATIN.put('А', "A");  CYRILLIC_TO_LATIN.put('Б', "B");
        CYRILLIC_TO_LATIN.put('В', "V");  CYRILLIC_TO_LATIN.put('Г', "G");
        CYRILLIC_TO_LATIN.put('Д', "D");  CYRILLIC_TO_LATIN.put('Ђ', "Dj");
        CYRILLIC_TO_LATIN.put('Е', "E");  CYRILLIC_TO_LATIN.put('Ж', "Z");
        CYRILLIC_TO_LATIN.put('З', "Z");  CYRILLIC_TO_LATIN.put('И', "I");
        CYRILLIC_TO_LATIN.put('Ј', "J");  CYRILLIC_TO_LATIN.put('К', "K");
        CYRILLIC_TO_LATIN.put('Л', "L");  CYRILLIC_TO_LATIN.put('Љ', "Lj");
        CYRILLIC_TO_LATIN.put('М', "M");  CYRILLIC_TO_LATIN.put('Н', "N");
        CYRILLIC_TO_LATIN.put('Њ', "Nj"); CYRILLIC_TO_LATIN.put('О', "O");
        CYRILLIC_TO_LATIN.put('П', "P");  CYRILLIC_TO_LATIN.put('Р', "R");
        CYRILLIC_TO_LATIN.put('С', "S");  CYRILLIC_TO_LATIN.put('Т', "T");
        CYRILLIC_TO_LATIN.put('Ћ', "C");  CYRILLIC_TO_LATIN.put('У', "U");
        CYRILLIC_TO_LATIN.put('Ф', "F");  CYRILLIC_TO_LATIN.put('Х', "H");
        CYRILLIC_TO_LATIN.put('Ц', "C");  CYRILLIC_TO_LATIN.put('Ч', "C");
        CYRILLIC_TO_LATIN.put('Џ', "Dz"); CYRILLIC_TO_LATIN.put('Ш', "S");

        // Lowercase Cyrillic → Latin
        CYRILLIC_TO_LATIN.put('а', "a");  CYRILLIC_TO_LATIN.put('б', "b");
        CYRILLIC_TO_LATIN.put('в', "v");  CYRILLIC_TO_LATIN.put('г', "g");
        CYRILLIC_TO_LATIN.put('д', "d");  CYRILLIC_TO_LATIN.put('ђ', "dj");
        CYRILLIC_TO_LATIN.put('е', "e");  CYRILLIC_TO_LATIN.put('ж', "z");
        CYRILLIC_TO_LATIN.put('з', "z");  CYRILLIC_TO_LATIN.put('и', "i");
        CYRILLIC_TO_LATIN.put('ј', "j");  CYRILLIC_TO_LATIN.put('к', "k");
        CYRILLIC_TO_LATIN.put('л', "l");  CYRILLIC_TO_LATIN.put('љ', "lj");
        CYRILLIC_TO_LATIN.put('м', "m");  CYRILLIC_TO_LATIN.put('н', "n");
        CYRILLIC_TO_LATIN.put('њ', "nj"); CYRILLIC_TO_LATIN.put('о', "o");
        CYRILLIC_TO_LATIN.put('п', "p");  CYRILLIC_TO_LATIN.put('р', "r");
        CYRILLIC_TO_LATIN.put('с', "s");  CYRILLIC_TO_LATIN.put('т', "t");
        CYRILLIC_TO_LATIN.put('ћ', "c");  CYRILLIC_TO_LATIN.put('у', "u");
        CYRILLIC_TO_LATIN.put('ф', "f");  CYRILLIC_TO_LATIN.put('х', "h");
        CYRILLIC_TO_LATIN.put('ц', "c");  CYRILLIC_TO_LATIN.put('ч', "c");
        CYRILLIC_TO_LATIN.put('џ', "dz"); CYRILLIC_TO_LATIN.put('ш', "s");

        // Latin diacritical marks → plain Latin
        CYRILLIC_TO_LATIN.put('Š', "S");  CYRILLIC_TO_LATIN.put('š', "s");
        CYRILLIC_TO_LATIN.put('Ć', "C");  CYRILLIC_TO_LATIN.put('ć', "c");
        CYRILLIC_TO_LATIN.put('Č', "C");  CYRILLIC_TO_LATIN.put('č', "c");
        CYRILLIC_TO_LATIN.put('Ž', "Z");  CYRILLIC_TO_LATIN.put('ž', "z");
        CYRILLIC_TO_LATIN.put('Đ', "Dj"); CYRILLIC_TO_LATIN.put('đ', "dj");
    }

    public static String normalize(String input) {
        if (input == null) return null;

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            String replacement = CYRILLIC_TO_LATIN.get(c);
            if (replacement != null) {
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }
        return sb.toString().toLowerCase();
    }
}
