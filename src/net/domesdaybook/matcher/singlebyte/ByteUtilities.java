/*
 * Copyright Matt Palmer 2009-2011, All rights reserved.
 *
 */

package net.domesdaybook.matcher.singlebyte;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Some useful bit-twiddling utilities to count bits in a byte,
 * and the bytes which match those bits.
 *
 * @author Matt Palmer
 */
public class ByteUtilities {

    private static final int QUOTE_CHARACTER_VALUE = 39;
    private static final int START_PRINTABLE_ASCII = 32;
    private static final int END_PRINTABLE_ASCII = 126;
    private static int[] MASK = {0x55, 0x33, 0x0F};

    /**
     * Private constructor for static utility class.
     */
    private ByteUtilities() {
    }


    /**
     * Returns the number of bits set in a given byte.
     * 
     * Algorithm taken from:
     * http://www-graphics.stanford.edu/~seander/bithacks.html#CountBitsSetParallel
     * 
     * @param b The byte to count the set bits.
     * @return The number of bits set in the byte.
     */
    public static int countSetBits(byte b) {
        int bits = (int) b;
        int result = bits - ((bits >>> 1) & MASK[0]);
        result = ((result >>> 2) & MASK[1]) + (result & MASK[1]);
        result = ((result >>> 4) + result) & MASK[2];
        return result;
    }


    /**
     * Returns the number of unset bits in a given byte.
     *
     * @param b The byte to count the unset bits.
     * @return The number of bits unset in the byte.
     */
    public static int countUnsetBits(byte b) {
        return 8 - countSetBits(b);
    }


    /**
     * Returns the number of bytes which would match all the bits
     * in a given bitmask.
     *
     * @param bitmask The bitmask.
     * @return The number of bytes matching all the bits in the bitmask.
     */
    public static int countBytesMatchingAllBits(byte bitmask) {
        // 00000000 - 1 << 8 = 256
        // 00000001 - 1 << 7 = 128
        // 00000011 - 1 << 6 = 64
        // 00000111 - 1 << 5 = 32
        // 00001111 - 1 << 4 = 16
        // 00011111 - 1 << 3 = 8
        // 00111111 - 1 << 2 = 4
        // 01111111 - 1 << 1 = 2
        // 11111111 - 1 << 0 = 1
        // which particular bits are set or unset does not affect the calculation.
        return 1 << countUnsetBits(bitmask);
    }

    /**
     * Returns the number of bytes which would match any of the bits
     * in a given bitmask.
     * 
     * @param bitmask The bitmask.
     * @return The number of bytes matching any of the bits in the bitmask.
     */
    public static int countBytesMatchingAnyBit(byte bitmask) {
        // 00000000 - 256 - 256 = 0    (no match: zero).
        // 00000001 - 256 - 128 = 128  (no match: half the bytes where that bit is not set)
        // 00000011 - 256 - 64  = 192  (no match: zero & 63 other possible values)
        // 00000111 - 256 - 32  = 224  (no match: zero & 31 other possible values)
        // 00001111 - 256 - 16  = 240  (no match: zero & 15 other possible values)
        // 00011111 - 256 - 8   = 248  (no match: zero & 7 other possible values)
        // 00111111 - 256 - 4   = 252  (no match: zero, 10000000, 11000000, 01000000)
        // 01111111 - 256 - 2   = 254  (no match: zero and 10000000)
        // 11111111 - 256 - 1   = 255  (no match: zero)
        // which particular bits are set or unset does not affect the calculation.
        return 256 - countBytesMatchingAllBits(bitmask);
    }



    /**
     * Returns a list of bytes which would match all the bits in a given bitmask.
     *
     * @param bitMask The bitmask to match.
     * @return A list of bytes matching the bitmask.
     */
    public static List<Byte> getBytesMatchingAllBitMask(final byte bitMask) {
        final List<Byte> bytes = new ArrayList<Byte>();
        for (int byteIndex = 0; byteIndex < 256; byteIndex++) {
            final byte byteValue = (byte) byteIndex;
            if ((byteValue & bitMask) == bitMask) {
                bytes.add((byte) byteIndex);
            }
        }
        return bytes;
    }


    public static Byte getAllBitMaskForBytes(final byte[] bytes) {
        return getAllBitMaskForBytes(toSet(bytes));
    }


    public static Set<Byte> toSet(final byte[] bytes) {
        Set<Byte> setOfBytes = new HashSet<Byte>();
        final int size = bytes.length;
        for (int count = 0; count < size; count++) {
            setOfBytes.add(bytes[count]);
        }
        return setOfBytes;
    }

    
    public static byte[] toArray(final List<Byte> list) {
        final byte[] result = new byte[list.size()];
        int position = 0;
        for (Byte b : list) {
            result[position++] = b;
        }
        return result;
    }


    public static byte[] getAllByteValues() {
        return getBytesInRange(0, 255);
    }


    public static byte[] getBytesInRange(int from, int to) {
        byte[] range = new byte[to-from+1];
        int position = 0;
        for (int value = from; value <= to; value++) {
            range[position++] = (byte) value;
        }
        return range;
    }
    
    
    public static Set<Byte> invertedSet(final Set<Byte> bytes) {
        Set<Byte> invertedSet = new HashSet<Byte>();
        for (int value = 0; value < 256; value++) {
            if (!bytes.contains((byte) value)) {
                invertedSet.add((byte) value);
            }
        }
        return invertedSet;
    }

    
    /**
     *
     * @param bytes A set of bytes to find an all bitmask to match.
     * @return A bitmask to match the set with, or null if no bitmask exists for that set of bytes.
     */
    public static Byte getAllBitMaskForBytes(final Set<Byte> bytes) {
        Byte allBitMask = null;
        // Build a candidate bitmask from the bits all the bytes have in common.
        int bitsInCommon = getBitsInCommon(bytes);
        if (bitsInCommon > 0) {
            // If the number of bytes in the set is the same as the number of bytes
            // which would match the bitmask, then the set of bytes can be matched
            // by that bitmask.
            byte mask = (byte) bitsInCommon;
            if (bytes.size() == countBytesMatchingAllBits(mask)) {
                allBitMask = new Byte(mask);
            }
        }
        return allBitMask;
    }



    /**
     *
     * @param bytes A set of bytes to find an any bitmask to match.
     * @return A bitmask to match the set with, or null if no bitmask exists for that set of bytes.
     */
    public static Byte getAnyBitMaskForBytes(final Set<Byte> bytes) {
        Byte allBitMask = null;
        // Build a candidate bitmask from the bits all the bytes have in common.
        int bitsInCommon = getBitsInCommon(bytes);
        if (bitsInCommon > 0) {
            // If the number of bytes in the set is the same as the number of bytes
            // which would match the bitmask, then the set of bytes can be matched
            // by that bitmask.
            byte mask = (byte) bitsInCommon;
            if (bytes.size() == countBytesMatchingAnyBit(mask)) {
                allBitMask = new Byte(mask);
            }
        }
        return allBitMask;
    }


    public static Byte getAnyBitMaskForBytes(final byte[] bytes) {
        return getAnyBitMaskForBytes(toSet(bytes));
    }
    
    /**
     *
     * @param bytes A set of bytes to find the bits in common.
     * @return An integer mask containing only the bits in common.
     */
    public static int getBitsInCommon(final Set<Byte> bytes) {
        int bitsinCommon = 0xFF;
        for (Byte b : bytes) {
            bitsinCommon = bitsinCommon & b;
        }
        return bitsinCommon;
    }



    /**
     * Returns a list of bytes which would match any of the bits in a given bitmask.
     * @param bitMask The bitmask to match.
     * @return A list of all the bytes matching the any bitmask.
     */
    public static List<Byte> getBytesMatchingAnyBitMask(final byte bitMask) {
        final List<Byte> bytes = new ArrayList<Byte>();
        // start loop at one - any bitmask matchers can never match the zero byte.
        for (int byteIndex = 1; byteIndex < 256; byteIndex++) {
            final byte byteValue = (byte) byteIndex;
            if ((byteValue & bitMask) > 0) {
                bytes.add((byte) byteIndex);
            }
        }
        return bytes;
    }


    /**
     * Returns a byte value as either a 2-char hex string, or if
     * pretty printing, and the byte value is a printable ASCII
     * character, as a quoted ASCII char, unless it is a single quote
     * character itself, in which case it will still be represented as
     * a hex byte.
     * 
     * @param prettyPrint Whether to pretty print the byte value.
     * @param value The byte value to represent as a string.
     * @return A string containing the byte value as a string.
     */
    public static String byteToString(final boolean prettyPrint, int byteValue) {
        String result = null;
        if (prettyPrint) {
            if (byteValue >= START_PRINTABLE_ASCII &&
                byteValue <= END_PRINTABLE_ASCII &&
                byteValue != QUOTE_CHARACTER_VALUE) {
                result = String.format(" '%c' ", byteValue);
            } else {
                result = String.format(" %02x ", byteValue);
            }
        } else {
            result = String.format("%02x", byteValue);
        }
        return result;
    }
    
}