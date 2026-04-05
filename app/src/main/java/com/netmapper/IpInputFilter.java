package com.netmapper;

import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Input filter and validator for IP addresses and subnet masks.
 */
public class IpInputFilter implements InputFilter {

    private final boolean isSubnetPrefix; // true for "192.168.1", false for full IP

    public IpInputFilter(boolean isSubnetPrefix) {
        this.isSubnetPrefix = isSubnetPrefix;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        // Build the resulting string
        StringBuilder builder = new StringBuilder(dest);
        builder.replace(dstart, dend, source.subSequence(start, end).toString());
        String result = builder.toString();

        // Allow only digits and dots
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                return "";
            }
        }

        // Validate the format
        if (!isValidPartialIp(result, isSubnetPrefix)) {
            return "";
        }

        return null; // Accept the input
    }

    private boolean isValidPartialIp(String ip, boolean isPrefix) {
        if (ip.isEmpty()) return true;

        // No consecutive dots
        if (ip.contains("..")) return false;

        // No leading dot
        if (ip.startsWith(".")) return false;

        String[] parts = ip.split("\\.", -1);
        int maxParts = isPrefix ? 3 : 4;

        if (parts.length > maxParts) return false;

        for (String part : parts) {
            if (part.isEmpty()) continue; // Allow trailing dot while typing

            // No leading zeros (except "0" itself)
            if (part.length() > 1 && part.startsWith("0")) return false;

            // Must be a valid number 0-255
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }

            // Max 3 digits
            if (part.length() > 3) return false;
        }

        return true;
    }

    /**
     * Validate a complete IP address (4 octets).
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) return false;

        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        for (String part : parts) {
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate a subnet prefix (3 octets like "192.168.1").
     */
    public static boolean isValidSubnetPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return false;

        String[] parts = prefix.split("\\.");
        if (parts.length != 3) return false;

        for (String part : parts) {
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate a subnet mask (like "255.255.255.0").
     */
    public static boolean isValidSubnetMask(String mask) {
        if (!isValidIp(mask)) return false;

        // Check it's a valid mask (contiguous 1s followed by 0s)
        String[] parts = mask.split("\\.");
        int[] values = new int[4];
        for (int i = 0; i < 4; i++) {
            values[i] = Integer.parseInt(parts[i]);
        }

        // Build 32-bit mask
        long maskValue = ((long) values[0] << 24) | ((long) values[1] << 16) |
                         ((long) values[2] << 8) | values[3];

        // Valid masks: must be contiguous 1s then 0s
        // e.g., 255.255.255.0 = 11111111.11111111.11111111.00000000
        if (maskValue == 0) return true; // /0

        // Find first 0 bit, then verify all remaining bits are 0
        long inverted = ~maskValue & 0xFFFFFFFFL;
        return (inverted & (inverted + 1)) == 0;
    }

    /**
     * Apply IP filter to an EditText for subnet prefix.
     */
    public static void applySubnetFilter(EditText editText) {
        editText.setFilters(new InputFilter[]{new IpInputFilter(true)});
    }

    /**
     * Apply IP filter to an EditText for full IP address.
     */
    public static void applyIpFilter(EditText editText) {
        editText.setFilters(new InputFilter[]{new IpInputFilter(false)});
    }

    /**
     * Apply IP filter to an EditText for subnet mask.
     */
    public static void applyMaskFilter(EditText editText) {
        editText.setFilters(new InputFilter[]{new IpInputFilter(false)});
    }

    /**
     * TextWatcher that auto-inserts dots after valid octets.
     */
    public static TextWatcher createAutoFormatWatcher(EditText editText, boolean isPrefix) {
        return new TextWatcher() {
            private boolean isFormatting = false;
            private int maxOctets = isPrefix ? 3 : 4;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                String text = s.toString();
                String[] parts = text.split("\\.", -1);

                // Auto-add dot after complete octet (3 digits or value > 25)
                if (parts.length < maxOctets) {
                    String lastPart = parts[parts.length - 1];
                    if (lastPart.length() == 3 ||
                        (lastPart.length() == 2 && !lastPart.isEmpty() &&
                         Integer.parseInt(lastPart) > 25)) {
                        isFormatting = true;
                        s.append(".");
                        isFormatting = false;
                    }
                }
            }
        };
    }
}
