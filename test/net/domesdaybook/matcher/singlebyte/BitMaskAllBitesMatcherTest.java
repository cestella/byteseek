/*
 * Copyright Matt Palmer 2009-2011, All rights reserved.
 *
 */

package net.domesdaybook.matcher.singlebyte;

import net.domesdaybook.bytes.ByteUtilities;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matt
 */
public class BitMaskAllBitesMatcherTest {

    public BitMaskAllBitesMatcherTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }


    /**
     * Test of matches method, of class BitMaskAllBitsMatcher.
     */
    @Test
    public void testMatches_byte() {
        BitMaskAllBitsMatcher matcher = new BitMaskAllBitsMatcher(b(255));
        validateMatchInRange(matcher, 255, 255);
        validateNoMatchInRange(matcher, 0, 254);

        SimpleTimer.timeMatcher("Bitmask All 255", matcher);

        matcher = new BitMaskAllBitsMatcher(b(0));
        validateMatchInRange(matcher, 0, 256);

        SimpleTimer.timeMatcher("Bitmask All 0", matcher);

        matcher = new BitMaskAllBitsMatcher(b(254));
        validateMatchInRange(matcher, 254, 255);
        validateNoMatchInRange(matcher, 0, 253);

        SimpleTimer.timeMatcher("Bitmask All 254", matcher);

        matcher = new BitMaskAllBitsMatcher(b(128));
        validateMatchInRange(matcher, 128, 255);
        validateNoMatchInRange(matcher, 0, 127);

        SimpleTimer.timeMatcher("Bitmask All 128", matcher);
        
        // test all bit masks using different methods.
        for (int mask = 0; mask < 256; mask++) {
            matcher = new BitMaskAllBitsMatcher(b(mask));
            validateMatchBitsSet(matcher, b(mask));
            validateNoMatchBitsNotSet(matcher, b(mask));
        }
    }

    private void validateNoMatchInRange(BitMaskAllBitsMatcher matcher, int from, int to) {
        for (int count = from; count <= to; count++) {
            assertEquals(false, matcher.matches(b(count)));
        }
    }

    private void validateMatchInRange(BitMaskAllBitsMatcher matcher, int from, int to) {
        for (int count = from; count <= to; count++) {
            assertEquals(true, matcher.matches(b(count)));
        }
    }

    private void validateMatchBitsSet(BitMaskAllBitsMatcher matcher, int bitmask) {
        String description = String.format("0x%02x", bitmask);
        for (int count = 0; count < 256; count++) {
            byte value = (byte) (count | bitmask);
            assertEquals(description, true, matcher.matches(value));
        }
    }
    
    private void validateNoMatchBitsNotSet(BitMaskAllBitsMatcher matcher, int bitmask) {
        if (bitmask > 0) { // This test won't work for a zero bitmask.
            String description = String.format("0x%02x", bitmask);
            final int invertedMask = bitmask ^ 0xFF;
            for (int count = 0; count < 256; count++) { // zero byte matches everything.
                byte value = (byte) (count & invertedMask);
                assertEquals(description, false, matcher.matches(value));
            }
        }
    }
    

    /**
     * Test of toRegularExpression method, of class BitMaskAllBitsMatcher.
     */
    @Test
    public void testToRegularExpression() {
        for (int count = 0; count < 256; count++) {
            BitMaskAllBitsMatcher matcher = new BitMaskAllBitsMatcher(b(count));
            String expected = String.format("&%02x", count);
            assertEquals(expected, matcher.toRegularExpression(false));
        }
    }



    /**
     * Test of getMatchingBytes method, of class BitMaskAllBitsMatcher.
     */
    @Test
    public void testGetMatchingBytes() {
        BitMaskAllBitsMatcher matcher = new BitMaskAllBitsMatcher(b(255));
        assertArrayEquals("0xFF matches [0xFF] only",
                new byte[] {b(255)},
                matcher.getMatchingBytes());

        matcher = new BitMaskAllBitsMatcher(b(0));
        assertArrayEquals("0x00 matches all bytes", ByteUtilities.getAllByteValues(), matcher.getMatchingBytes());

        matcher = new BitMaskAllBitsMatcher(b(254));
        byte[] expected = new byte[] {b(254), b(255)};
        assertArrayEquals("0xFE matches 0xFF and 0xFE only", expected, matcher.getMatchingBytes());

        matcher = new BitMaskAllBitsMatcher(b(3));
        expected = new byte[64];
        for (int count = 0; count < 64; count++) {
            expected[count] = (byte)((count << 2) | 3);
        }
        assertArrayEquals("0x03 matches 64 bytes with first two bits set", expected, matcher.getMatchingBytes());

        matcher = new BitMaskAllBitsMatcher(b(128));
        expected = ByteUtilities.getBytesInRange(128, 255);
        assertArrayEquals("0x80 matches all bytes from 128 to 255", expected, matcher.getMatchingBytes());
    }

    /**
     * Test of getNumberOfMatchingBytes method, of class BitMaskAllBitsMatcher.
     */
    @Test
    public void testGetNumberOfMatchingBytes() {
        BitMaskAllBitsMatcher matcher = new BitMaskAllBitsMatcher(b(255));
        assertEquals("0xFF matches one byte", 1, matcher.getNumberOfMatchingBytes());

        matcher = new BitMaskAllBitsMatcher(b(0));
        assertEquals("0x00 matches 256 bytes", 256, matcher.getNumberOfMatchingBytes());

        matcher = new BitMaskAllBitsMatcher(b(254));
        assertEquals("0xFE matches 2 bytes", 2, matcher.getNumberOfMatchingBytes());

        matcher = new BitMaskAllBitsMatcher(b(3));
        assertEquals("0x03 matches 64 bytes", 64, matcher.getNumberOfMatchingBytes());
        
        matcher = new BitMaskAllBitsMatcher(b(128));
        assertEquals("0x80 matches 128 bytes", 128, matcher.getNumberOfMatchingBytes());
    }

    private byte b(int i) {
        return (byte) i;
    }

}