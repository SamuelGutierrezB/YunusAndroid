package com.abzikel.yunus.utils;

import android.text.InputFilter;
import android.text.Spanned;

// InputFilter to limit EditText input to two decimal places
public class DecimalDigitsInputFilter implements InputFilter {
    private final int decimalPlaces;

    public DecimalDigitsInputFilter(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int digit_start, int digit_end) {
        String input = dest.toString() + source.toString();
        if (input.contains(".")) {
            int index = input.indexOf(".");
            int decimals = input.length() - index - 1;
            if (decimals > decimalPlaces) return "";
        }
        return null;
    }
}
