/*
 * Copyright Matt Palmer 2013-14, All rights reserved.
 *
 * This code is licensed under a standard 3-clause BSD license:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 
 *  * The names of its contributors may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.byteseek.matcher.sequence;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import net.byteseek.utils.ByteUtils;
import net.byteseek.io.reader.ByteArrayReader;
import net.byteseek.io.reader.FileReader;
import net.byteseek.matcher.bytes.ByteMatcher;
import net.byteseek.matcher.bytes.OneByteMatcher;
import net.byteseek.matcher.bytes.TwoByteMatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests all the constructors and public methods of the ByteMatcherSequenceMatcher class for
 * both success and failure, and the associated ReverseByteMatcherSequenceMatcher class for all 
 * the same conditions.
 * 
 * In particular, it tests for out-of-bounds, next-to-boundary and boundary-crossing
 * conditions in the matching methods.
 * 
 * @author Matt Palmer
 */
public class ByteMatcherSequenceMatcherTest {

	//////////////////
	// test setup   //
	//////////////////

	private final static Random	rand	= new Random();

	private List<FileReader>    readers;
	private byte[]				bytes;

	public ByteMatcherSequenceMatcherTest() {
	}

	/**
	 * Generates a random number to use in randomising tests where complete
	 * coverage takes too long.  The seed is output to the console to give a
	 * fighting chance of replicating a failing test - but I'm not really 
	 * convinced this is a very pleasant way of testing.  Still, it gives more
	 * complete coverage of the code than purely fixed tests.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpClass() throws Exception {
		final long seed = System.currentTimeMillis();
		// final long seed = ?
		rand.setSeed(seed);
		System.out.println("Seeding random number generator with: " + Long.toString(seed));
		System.out.println("To repeat these exact tests, set the seed to the value above.");
	}

	/**
	 * Creates a file reader and a byte array from an ASCII test file.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		readers = getReaders("/TestASCII.txt", 1, 10, 4092, 4100);
		bytes = readers.get(14).getWindow(0).getArray();
	}

	@After
	public void tearDown() throws Exception {
		for (FileReader reader : readers) {
			reader.close();
		}
		readers.clear();
	}
	
    //TODO: tests for populate matchers and populate repeated matchers...

	
	///////////////////////////
	//   constructor tests   //
	///////////////////////////


	
	/**
	 * 
	 * Construct all possible single byte value sequences.  Tests are:
	 * 
	 * - the length is one.
	 * - the number of bytes matched by it is one.
	 * - the value of the byte matcher is the one it was constructed with.
	 * - the matcher matches that byte in a byte array and reader.
	 * - the matcher does not match a different byte value in a byte array and reader.
	 */
	@Test
	public void testConstructSingleByte() throws IOException {
		for (int byteValue = 0; byteValue < 256; byteValue++) {

			ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher((byte) byteValue);
			assertEquals("length:1, byte value:" + Integer.toString(byteValue), 1, matcher.length());

			byte[] matchingBytes = matcher.getMatcherForPosition(0).getMatchingBytes();
			assertEquals("number of bytes matched=1", 1, matchingBytes.length);
			assertEquals("byte value:" + Integer.toString(byteValue), byteValue,
					matchingBytes[0] & 0xFF);

			byte[] testArray = new byte[] { (byte) byteValue };
			assertTrue("matches that byte value in an array " + byteValue, matcher.matches(testArray, 0));

			ByteArrayReader wrapped = new ByteArrayReader(testArray);
			assertTrue("matches that byte value in a reader", matcher.matches(wrapped, 0));

			int differentValue = rand.nextInt(256);
			while (differentValue == byteValue) {
				differentValue = rand.nextInt(256);
			}
			byte[] different = new byte[] { (byte) differentValue };
			assertFalse("does not match a different byte value in an array",
					matcher.matches(different, 0));

			wrapped = new ByteArrayReader(different);
			assertFalse("does not match a different byte value in a reader",
					matcher.matches(wrapped, 0));
		}
	}


	/**
	 * Construct using random arrays of bytes, 100 times.  Tests are:
	 * 
	 * - the length is correct.
	 * - each position in the matcher only matches one byte.
	 * - each byte in the matcher is correct.
	 */
	@Test
	public void testConstructByteArray() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final byte[] array = createRandomArray(1024);
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(array);
			assertEquals("length:" + Integer.toString(array.length), array.length, matcher.length());

			for (int i = 0; i < array.length; i++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(i);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final byte matchingValue = array[i];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
			
			testDefensivelyCopied(array, matcher);
		}
	}
	
	
	private void testDefensivelyCopied(byte[] array, SequenceMatcher matcher) {
		changeArray(array);
		for (int pos = 0; pos < array.length; pos++) {
			final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
			final byte[] matchingBytes = sbm.getMatchingBytes();
			final byte matchingValue = array[pos];
			assertEquals("number of bytes matched=1", 1, matchingBytes.length);
			assertTrue("byte value not equals:" + Integer.toString(matchingValue), 
					matchingValue != matchingBytes[0]);
		}
	}
	
	private void changeArray(byte[] array) {
		for (int i = 0; i < array.length; i++) {
			int newvalue = ((array[i] & 0xFF) + 1) % 256;
			array[i] = (byte) newvalue;
		}
	}
	
	
	/**
	 * Construct using random repeated byte values for all byte values.  Tests are:
	 * 
	 * - length is correct
	 * - each position in the matcher only matches one byte.
	 * - each byte in the matcher is correct.
	 * 
	 */
	@Test
	public void testConstructRepeatedBytes() {
		for (int byteValue = 0; byteValue < 256; byteValue++) {
			final int repeats = rand.nextInt(1024) + 1;
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(repeats, (byte) byteValue);
			assertEquals(
					"length:" + Integer.toString(repeats) + ", byte value:"
							+ Integer.toString(byteValue), repeats, matcher.length());

			for (int pos = 0; pos < repeats; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(byteValue), byteValue,
						matchingBytes[0] & 0xFF);
			}
		}
	}
	
	
	@Test
	public void testRepeatedByteMatcher() {
		for (int byteValue = 0; byteValue < 256; byteValue++) {
			final int repeats = rand.nextInt(1024) + 1;
			ByteMatcher bytematcher = OneByteMatcher.valueOf((byte) byteValue);
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(repeats, bytematcher);
			assertEquals(
					"length:" + Integer.toString(repeats) + ", byte value:"
							+ Integer.toString(byteValue), repeats, matcher.length());

			for (int pos = 0; pos < repeats; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(byteValue), byteValue,
						matchingBytes[0] & 0xFF);
			}
		}		
	}
	
	
	/**
	 * Construct using a string and the default charset.
	 */
	@Test
	public void testConstructString() {
		testConstructString("1");
		testConstructString("abcdefghijklmnopqrstuvwxyz");
		testConstructString("0123456789" + '\t');
		testConstructString("0123456789" + "\n\r" + "MORE TEXT");
	}
	
	
	private void testConstructString(String string) {
		testConstructString(string, Charset.defaultCharset());
	}
	
	
	/**
	 * Construct using a string and the standard supported charsets.
	 */
	@Test
	public void testConstructStringCharset() {
		testConstructStringCharset("1");
		testConstructStringCharset("abcdefghijklmnopqrstuvwxyz");
		testConstructStringCharset("0123456789" + '\t');
		testConstructStringCharset("0123456789" + "\n\r" + "MORE TEXT");
	}
	
	
	private void testConstructStringCharset(String string) {
		testConstructString(string, Charset.defaultCharset());
		testConstructString(string, Charset.forName("US-ASCII"));
		testConstructString(string, Charset.forName("ISO-8859-1"));
		testConstructString(string, Charset.forName("UTF-8"));
		testConstructString(string, Charset.forName("UTF-16BE"));
		testConstructString(string, Charset.forName("UTF-16LE"));
		testConstructString(string, Charset.forName("UTF-16"));
	}
	

	private void testConstructString(String string, Charset charset) {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(string);
		byte[] array = string.getBytes();
		assertEquals("length:", array.length, matcher.length());
		for (int i = 0; i < array.length; i++) { 
			final ByteMatcher sbm2 = matcher.getMatcherForPosition(i);
			final byte[] matchingBytes = sbm2.getMatchingBytes();
			assertEquals("number of bytes matched source=1", 1, matchingBytes.length);
			assertEquals("byte value:" + Integer.toString(matchingBytes[0]),
					array[i], matchingBytes[0]);
			
		}
	}
	

	@Test
	public void testConstructSubsequence() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final byte[] array = createRandomArray(1024);
			final int startPos = rand.nextInt(array.length);
			final int endPos   = startPos + rand.nextInt(array.length-startPos) + 1;
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(array, startPos, endPos);
			assertEquals("length:" + Integer.toString(endPos - startPos), endPos - startPos, matcher.length());

			for (int pos = 0; pos < endPos - startPos; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final byte matchingValue = array[startPos + pos];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
			
			testDefensivelyCopied(array, new ByteMatcherSequenceMatcher(array, 0, array.length));
		}
	}
	
	
	@Test
	public void testConstructRepeatedSubsequence() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final byte[] array = createRandomArray(1024);
			final int startPos = rand.nextInt(array.length);
			final int endPos   = startPos + rand.nextInt(array.length-startPos) + 1;
			final int repeats = rand.nextInt(10) + 1;
			final int length = (endPos - startPos) * repeats;
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(repeats, array, startPos, endPos);
			assertEquals("length:" + length, length, matcher.length());

			for (int pos = 0; pos < length; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final int arrayPos = startPos + (pos % (endPos - startPos));
				final byte matchingValue = array[arrayPos];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
			
			testDefensivelyCopied(array, new ByteMatcherSequenceMatcher(array, 0, array.length));
		}
	}
	
	
	@Test
	public void testConstructSingleByteMatcher() {
		for (int i = 0; i < 256; i++) {
			ByteMatcher matcher = OneByteMatcher.valueOf((byte) i);
			ByteMatcherSequenceMatcher sequence = new ByteMatcherSequenceMatcher(matcher);
			assertEquals("Length is one", 1, sequence.length());
			byte[] test = new byte[] {(byte) i};
			assertTrue("Matches byte value correctly " + i, sequence.matches(test, 0));
			int val2 = (i + 1) % 256;
			byte[] test2 = new byte[] {(byte) val2};
			assertFalse("Does not match value " + val2, sequence.matches(test2, 0));
		}
	}
	
	
	@Test
	public void testConstructByteMatcherArray() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final ByteMatcher[] array = createRandomByteMatcherArray(1024);
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(array);
			assertEquals("length:" + Integer.toString(array.length), array.length, matcher.length());

			for (int pos = 0; pos < array.length; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final byte matchingValue = array[pos].getMatchingBytes()[0];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
		}
	}
	
	
	@Test
	public void testConstructRepeatedByteMatcherArray() {
		for (int byteValue = 0; byteValue < 256; byteValue++) {
			final int repeats = rand.nextInt(1024) + 1;
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(repeats, OneByteMatcher.valueOf((byte) byteValue));
			assertEquals(
					"length:" + Integer.toString(repeats) + ", byte value:"
							+ Integer.toString(byteValue), repeats, matcher.length());

			for (int pos = 0; pos < repeats; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(byteValue), byteValue,
						matchingBytes[0] & 0xFF);
			}
		}
	}
	
	
	@Test
	public void testConstructByteMatcherArraySubsequence() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final ByteMatcher[] array = createRandomByteMatcherArray(1024);
			final int startPos = rand.nextInt(array.length);
			final int endPos   = startPos + rand.nextInt(array.length-startPos) + 1;
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(array, startPos, endPos);
			assertEquals("length:" + Integer.toString(endPos - startPos), endPos - startPos, matcher.length());

			for (int pos = 0; pos < endPos - startPos; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final byte matchingValue = array[startPos + pos].getMatchingBytes()[0];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
		}
	}
	
	
	@Test
	public void testConstructRepeatedByteMatcherArraySubsequence() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final ByteMatcher[] array = createRandomByteMatcherArray(1024);
			final int startPos = rand.nextInt(array.length);
			final int endPos   = startPos + rand.nextInt(array.length-startPos) + 1;
			final int repeats = rand.nextInt(10) + 1;
			final int length = (endPos - startPos) * repeats;
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(repeats, array, startPos, endPos);
			assertEquals("length:" + length, length, matcher.length());

			for (int pos = 0; pos < length; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final int arrayPos = startPos + (pos % (endPos - startPos));
				final byte matchingValue = array[arrayPos].getMatchingBytes()[0];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
		}
	}
	
	
	@Test
	public void testConstructRepeatedByteMatcherSequenceMatcher() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final byte[] array = createRandomArray(1024);
			final ByteMatcherSequenceMatcher source = new ByteMatcherSequenceMatcher(array);
			final int repeats = rand.nextInt(10) + 1;
			final int length = array.length * repeats;
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(repeats, source);
			assertEquals("length:" + length, length, matcher.length());

			for (int pos = 0; pos < length; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final int arrayPos = (pos % array.length);
				final byte matchingValue = array[arrayPos];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
		}
	}
	
	
	@Test
	public void testConstructByteMatcherSequenceMatcherSubsequence() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final byte[] array = createRandomArray(1024);
			final ByteMatcherSequenceMatcher source = new ByteMatcherSequenceMatcher(array);
			final int startPos = rand.nextInt(array.length);
			final int endPos   = startPos + rand.nextInt(array.length-startPos) + 1;
			final int length = (endPos - startPos);
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(source, startPos, endPos);
			assertEquals("length:" + length, length, matcher.length());

			for (int pos = 0; pos < length; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final int arrayPos = startPos + (pos % (endPos - startPos));
				final byte matchingValue = array[arrayPos];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
		}
	}
	
	
	@Test
	public void testConstructRepeatedByteMatcherSequenceMatcherSubsequence() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final byte[] array = createRandomArray(1024);
			final ByteMatcherSequenceMatcher source = new ByteMatcherSequenceMatcher(array);
			final int startPos = rand.nextInt(array.length);
			final int endPos   = startPos + rand.nextInt(array.length-startPos) + 1;
			final int repeats = rand.nextInt(10) + 1;
			final int length = (endPos - startPos) * repeats;
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(repeats, source, startPos, endPos);
			assertEquals("length:" + length, length, matcher.length());

			for (int pos = 0; pos < length; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final int arrayPos = startPos + (pos % (endPos - startPos));
				final byte matchingValue = array[arrayPos];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
		}
	}
		
	@Test
	public void testConstructSingleByteMatcherSequenceMatcher() {
		byte[] array = createRandomArray(65);
		ByteMatcherSequenceMatcher source = new ByteMatcherSequenceMatcher(array);
		ByteMatcherSequenceMatcher result = new ByteMatcherSequenceMatcher(source);
		assertEquals("Lengths are the same: " + array.length, source.length(), result.length());
		for (int i = 0; i < array.length; i++) {
			final ByteMatcher sbm = result.getMatcherForPosition(i);
			final byte[] matchingBytes = sbm.getMatchingBytes();
			final byte matchingValue = array[i];
			assertEquals("number of bytes matched=1", 1, matchingBytes.length);
			assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
					matchingBytes[0]);
		}
	}

	
	@Test
	public void testConstructByteMatcherSequenceMatcherArray() {
		ByteMatcherSequenceMatcher[] matchers = new ByteMatcherSequenceMatcher[5];
		int totalLength = 0;
		for (int i = 0; i < 5; i++) {
			matchers[i] = new ByteMatcherSequenceMatcher(createRandomByteMatcherArray(129));
			totalLength += matchers[i].length();
		}
		ByteMatcherSequenceMatcher result = new ByteMatcherSequenceMatcher(matchers);
		assertEquals("Length is correct: " + totalLength, totalLength, result.length());
		
		int matcherPos = 0;
		for (int i = 0; i < 5; i++) {
			ByteMatcherSequenceMatcher component = matchers[i];
			for (int j = 0; j < component.length(); j++) {
				ByteMatcher sourceMatch = component.getMatcherForPosition(j);
				ByteMatcher resultMatch = result.getMatcherForPosition(matcherPos++);
				byte[] sourceBytes = sourceMatch.getMatchingBytes();
				byte[] resultBytes = resultMatch.getMatchingBytes();
				assertEquals("Same number of bytes match", sourceBytes.length, resultBytes.length);
				assertEquals("Matches the same bytes", sourceBytes[0], resultBytes[0]);
			}
		}
	}
	
	
	@Test
	public void testConstructSingleSequenceMatcher() {
		byte[] array = createRandomArray(342);
		ByteSequenceMatcher source = new ByteSequenceMatcher(array);
		ByteMatcherSequenceMatcher result = new ByteMatcherSequenceMatcher(source);
		assertEquals("length is correct " + array.length, source.length(), result.length());
		for (int i = 0; i < array.length; i++) {
			final ByteMatcher sbm = result.getMatcherForPosition(i);
			final byte[] matchingBytes = sbm.getMatchingBytes();
			final byte matchingValue = array[i];
			assertEquals("number of bytes matched=1", 1, matchingBytes.length);
			assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
					matchingBytes[0]);
		}
	}
	
	
	@Test
	public void testConstructSequenceMatcherArray() {
		ByteSequenceMatcher[] matchers = new ByteSequenceMatcher[5];
		int totalLength = 0;
		for (int i = 0; i < 5; i++) {
			matchers[i] = new ByteSequenceMatcher(createRandomArray(129));
			totalLength += matchers[i].length();
		}
		ByteMatcherSequenceMatcher result = new ByteMatcherSequenceMatcher(matchers);
		assertEquals("Length is correct: " + totalLength, totalLength, result.length());
		
		int matcherPos = 0;
		for (int i = 0; i < 5; i++) {
			ByteSequenceMatcher component = matchers[i];
			for (int j = 0; j < component.length(); j++) {
				ByteMatcher sourceMatch = component.getMatcherForPosition(j);
				ByteMatcher resultMatch = result.getMatcherForPosition(matcherPos++);
				byte[] sourceBytes = sourceMatch.getMatchingBytes();
				byte[] resultBytes = resultMatch.getMatchingBytes();
				assertEquals("Same number of bytes match", sourceBytes.length, resultBytes.length);
				assertEquals("Matches the same bytes", sourceBytes[0], resultBytes[0]);
			}
		}
	}
	
	
	/**
	 * Construct using random lists of byte sequence matchers.
	 * Tests are:
	 * 
	 * - the length of an assembled matcher is correct.
	 * - each position in the list of matchers matches only one byte.
	 * - each position in the assembled matcher matches only one byte.
	 * - each byte in the assembled matcher is correct.
	 */
	@Test
	public void testConstructByteSequenceMatcherList() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final List<ByteMatcherSequenceMatcher> list = createRandomList(32);
			int totalLength = 0;
			for (final SequenceMatcher matcher : list) {
				totalLength += matcher.length();
			}
			final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(list);
			assertEquals("length:" + Integer.toString(totalLength), totalLength, matcher.length());

			int localPos = -1;
			int matchIndex = 0;
			SequenceMatcher currentMatcher = list.get(matchIndex);
			for (int pos = 0; pos < totalLength; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				localPos++;
				if (localPos == currentMatcher.length()) {
					matchIndex++;
					currentMatcher = list.get(matchIndex);
					localPos = 0;
				}
				final ByteMatcher sbm2 = currentMatcher.getMatcherForPosition(localPos);
				final byte[] matchingBytes2 = sbm2.getMatchingBytes();
				assertEquals("number of bytes matched source=1", 1, matchingBytes2.length);
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingBytes2[0]),
						matchingBytes2[0], matchingBytes[0]);
			}
		}
	}
	
	


	
	@Test
	public void testConstructReverseByteArray() {
		for (int testNo = 0; testNo < 10; testNo++) {
			final byte[] array = createRandomArray(1024);
			final SequenceMatcher matcher = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(array);
			assertEquals("length:" + Integer.toString(array.length), array.length, matcher.length());

			for (int pos = 0; pos < array.length; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final byte matchingValue = array[array.length - pos - 1];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
			
			testDefensivelyCopied(ByteUtils.reverseArray(array), matcher);
		}
	}
	
	
	@Test
	public void testConstructRepeatedReverseByteArray() {
		int repeats = 1;
		for (int testNo = 0; testNo < 10; testNo++) {
			final byte[] array = createRandomArray(1024);
			final int startPos = rand.nextInt(array.length);
			final int endPos   = startPos + rand.nextInt(array.length-startPos) + 1;

			final int length = (endPos - startPos) * repeats;
			final ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(repeats, array, startPos, endPos);
			assertEquals("length:" + length, length, matcher.length());

			for (int pos = 0; pos < length; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final int arrayPos = endPos - (pos % (endPos - startPos)) - 1;
				final byte matchingValue = array[arrayPos];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
			
			testDefensivelyCopied(array, new ByteMatcherSequenceMatcher(array, 0, array.length));
			repeats = rand.nextInt(10) + 1;
		}
	}
	
	
	@Test
	public void testConstructReverseRepeatIndexedCopyConstructor() {
		int repeats = 1;
		for (int testNo = 0; testNo < 10; testNo++) {
			final byte[] array = createRandomArray(1024);
			final int startPos = rand.nextInt(array.length);
			final int endPos   = startPos + rand.nextInt(array.length-startPos) + 1;

			final int length = (endPos - startPos) * repeats;
			final ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher source = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(array);
			final ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(repeats, source, startPos, endPos);
			assertEquals("length:" + length, length, matcher.length());

			for (int pos = 0; pos < length; pos++) {
				final ByteMatcher sbm = matcher.getMatcherForPosition(pos);
				final byte[] matchingBytes = sbm.getMatchingBytes();
				final int arrayPos = endPos - (pos % (endPos - startPos)) - 1;
				final byte matchingValue = array[arrayPos];
				assertEquals("number of bytes matched=1", 1, matchingBytes.length);
				assertEquals("byte value:" + Integer.toString(matchingValue), matchingValue,
						matchingBytes[0]);
			}
			
			testDefensivelyCopied(array, new ByteMatcherSequenceMatcher(array, 0, array.length));
			repeats = rand.nextInt(10) + 1;
		}
	}
	
	//////////////////////////////////
	//  construction failure tests  //
	//////////////////////////////////

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testConstructNoRepeats() {
		new ByteMatcherSequenceMatcher(0, (byte) 0x8f);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testConstructNullArray() {
		new ByteMatcherSequenceMatcher((byte[]) null);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testConstructEmptyArray() {
		new ByteMatcherSequenceMatcher(new byte[0]);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testConstructNullList() {
		new ByteMatcherSequenceMatcher((ArrayList<ByteMatcherSequenceMatcher>) null);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testConstructEmptyList() {
		new ByteMatcherSequenceMatcher(new ArrayList<ByteMatcherSequenceMatcher>());
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testConstructNullElementList() {
		List<ByteMatcherSequenceMatcher> list = new ArrayList<ByteMatcherSequenceMatcher>();
		list.add(null);
		new ByteMatcherSequenceMatcher(list);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testNullByteArray() {
		new ByteMatcherSequenceMatcher((byte[]) null);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testEmptyByteArray() {
		new ByteMatcherSequenceMatcher(new byte[0]);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testNullByteMatcherArrayMatcher() {
		new ByteMatcherSequenceMatcher((ByteMatcherSequenceMatcher) null, 0, 0);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testByteMatcherArrayMatcherNegativeStart() {
		final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher((byte) 0x01, (byte) 0x02, (byte) 0x03);
		new ByteMatcherSequenceMatcher(matcher, -1, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testByteMatcherArrayMatcherEndPastLength() {
		final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher((byte) 0x01, (byte) 0x02, (byte) 0x03);
		new ByteMatcherSequenceMatcher(matcher, 0, 5);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testByteMatcherArrayMatcherStartNotSmallerThanEnd() {
		final ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher((byte) 0x01, (byte) 0x02, (byte) 0x03);
		new ByteMatcherSequenceMatcher(matcher, 2, 2);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructNullByteMatcherCollection() {
		new ByteMatcherSequenceMatcher((List<ByteMatcher>) null);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructEmptyByteMatcherCollection() {
		new ByteMatcherSequenceMatcher(new ArrayList<ByteMatcher>());
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructByteMatcherCollectionNullElement() {
		List<ByteMatcher> matchers = new ArrayList<ByteMatcher>();
		matchers.add(OneByteMatcher.valueOf((byte) 0x00));
		matchers.add(null);
		new ByteMatcherSequenceMatcher(matchers);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructNullRepeatedByteMatcher() {
		new ByteMatcherSequenceMatcher(3, (ByteMatcher) null);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructZeroRepeatedByteMatcher() {
		new ByteMatcherSequenceMatcher(0, OneByteMatcher.valueOf((byte) 0x00));
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructNegativeRepeatedByteMatcher() {
		new ByteMatcherSequenceMatcher(-1, OneByteMatcher.valueOf((byte) 0x00));
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testSubsequenceConstructorByteArrayNotNull() {
		new ByteMatcherSequenceMatcher((byte[]) null, 2, 2);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testSubsequenceConstructorByteArrayNotEmpty() {
		new ByteMatcherSequenceMatcher(new byte[0], 0, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructNullByteMatcher() {
		new ByteMatcherSequenceMatcher((ByteMatcher) null);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructNullByteMatcherArray() {
		new ByteMatcherSequenceMatcher((ByteMatcher[]) null);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructByteMatcherArrayStartLessThanZero() {
		new ByteMatcherSequenceMatcher(new ByteMatcher[] {OneByteMatcher.valueOf((byte) 0x01)},
		                               -1, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructByteMatcherArrayEndPastLength() {
		new ByteMatcherSequenceMatcher(new ByteMatcher[] {OneByteMatcher.valueOf((byte) 0x01)},
		                               0, 2);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructByteMatcherArrayStartPastEnd() {
		new ByteMatcherSequenceMatcher(new ByteMatcher[] {OneByteMatcher.valueOf((byte) 0x01)},
		                               1, 1);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedNullByteMatcherArray() {
		new ByteMatcherSequenceMatcher(1, (ByteMatcher[]) null);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructZeroRepeatedByteMatcherArray() {
		new ByteMatcherSequenceMatcher(0, new ByteMatcher[] {OneByteMatcher.valueOf((byte) 0x00)});
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructByteMatcherArrayNullElement() {
		new ByteMatcherSequenceMatcher(new ByteMatcher[] {null});
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructEmptyByteMatcherArray() {
		new ByteMatcherSequenceMatcher(new ByteMatcher[0]);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructNullRepeatedIndexedByteMatcherArray() {
		new ByteMatcherSequenceMatcher(1, (ByteMatcher[]) null, 0, 1);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructRepeatedIndexedByteMatcherArrayStartLessThanZero() {
		new ByteMatcherSequenceMatcher(1, new ByteMatcher[] {OneByteMatcher.valueOf((byte) 0x01)},
		                               -1, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructRepeatedIndexedByteMatcherArrayEndPastLength() {
		new ByteMatcherSequenceMatcher(1, new ByteMatcher[] {OneByteMatcher.valueOf((byte) 0x01)},
		                               0, 2);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructRepeatedIndexedByteMatcherArrayStartPastEnd() {
		new ByteMatcherSequenceMatcher(1, new ByteMatcher[] {OneByteMatcher.valueOf((byte) 0x01)},
		                               1, 1);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedIndexedNullByteMatcherArray() {
		new ByteMatcherSequenceMatcher(1, (ByteMatcher[]) null, 0, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructZeroRepeatedIndexedByteMatcherArray() {
		new ByteMatcherSequenceMatcher(0, new ByteMatcher[] {OneByteMatcher.valueOf((byte) 0x00)}, 0, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedIndexedByteMatcherArrayNullElement() {
		new ByteMatcherSequenceMatcher(1, new ByteMatcher[] {null}, 0, 1);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedIndexedEmptyByteMatcherArray() {
		new ByteMatcherSequenceMatcher(1, new ByteMatcher[0], 0, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedNullByteArray() {
		new ByteMatcherSequenceMatcher(5, (byte[]) null);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedEmptyByteArray() {
		new ByteMatcherSequenceMatcher(5, new byte[0]);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructZeroRepeatedByteArray() {
		new ByteMatcherSequenceMatcher(0, new byte[1]);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedNullIndexedByteArray() {
		new ByteMatcherSequenceMatcher(5, (byte[]) null, 0, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedEmptyIndexedByteArray() {
		new ByteMatcherSequenceMatcher(5, new byte[0], 0, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructZeroRepeatedByteIndexedArray() {
		new ByteMatcherSequenceMatcher(0, new byte[1], 0, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructNegativeStartRepeatedByteIndexedArray() {
		new ByteMatcherSequenceMatcher(1, new byte[1], -1, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructEndTooBigRepeatedByteIndexedArray() {
		new ByteMatcherSequenceMatcher(2, new byte[1], 0, 2);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructStartEqualsEndRepeatedByteIndexedArray() {
		new ByteMatcherSequenceMatcher(3, new byte[] {(byte) 0x01, (byte) 0x02, (byte) 0x03},
									   2, 2);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructStartPastEndRepeatedByteIndexedArray() {
		new ByteMatcherSequenceMatcher(4, new byte[] {(byte) 0x01, (byte) 0x02, (byte) 0x03},
									   2, 1);
	}

	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testSubsequenceConstructorStartNotNegative() {
		new ByteMatcherSequenceMatcher(new byte[] {(byte) 0x01}, -200, 1);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testSubsequenceConstructorEndPastLength() {
		new ByteMatcherSequenceMatcher(new byte[] {(byte) 0x01}, 0, 2);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testSubsequenceConstructorStartNotSmallerThanEnd() {
		new ByteMatcherSequenceMatcher(new byte[] {(byte) 0x01, (byte) 0x02}, 1, 1);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructNullString() {
		new ByteMatcherSequenceMatcher((String) null);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructNullCharset() {
		new ByteMatcherSequenceMatcher("A string", null);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructEmptyString() {
		new ByteMatcherSequenceMatcher("");
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedNullByteMatcherSequenceMatcher() {
		new ByteMatcherSequenceMatcher(1, (ByteMatcherSequenceMatcher) null);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructZeroRepeatedByteMatcherSequenceMatcher() {
		new ByteMatcherSequenceMatcher(0, new ByteMatcherSequenceMatcher((byte) 0));
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructReverseNullByteMatcherArrayMatcher() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher((ByteMatcherSequenceMatcher) null);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructReverseNullByteArray() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher((byte[]) null);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructReverseEmptyByteArray() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(new byte[0]);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructReverseSubsequenceNullByteArray() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher((byte[]) null);
	}

	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructReverseSubsequenceNegativeStart() {
		ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher original = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(createRandomArray(128));
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(original, -1, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructReverseSubsequenceEndPastLength() {
		ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher original = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(createRandomArray(128));
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(original, 0, original.length() + 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructReverseSubsequenceStartNotLessThanEnd() {
		ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher original = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(createRandomArray(128));
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(original, original.length(), original.length());
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedReverseNullByteArray() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(1, (byte[]) null, 0, 1);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedReverseEmptyByteArray() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(1, new byte[0], 0, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testConstructRepeatedReverseNoPositiveRepeat() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(0, new byte[] {(byte) 0x01}, 0, 1);
	}
	
	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructRepeatedReverseStartNegative() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(1, new byte[] {(byte) 0x01}, -1, 1);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructRepeatedReverseEndPastLength() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(1, new byte[] {(byte) 0x01}, 0, 2);
	}

	@SuppressWarnings("unused")
	@Test (expected=IndexOutOfBoundsException.class)
	public void testConstructRepeatedReverseStartNotLessThanEnd() {
		new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(1, new byte[] {(byte) 0x01, (byte) 0x02}, 1, 1);
	}


	// ///////////////////////////
	// matcher iteration tests //
	// ///////////////////////////

	@Test
	public void testByteMatcherIterator() {
		testByteMatcherIterator("1");
		testByteMatcherIterator("abcdefghijklmnopqrstuvwxyz");
		testByteMatcherIterator("atat\t\tn\n");
		
		testReverseByteMatcherIterator("1");
		testReverseByteMatcherIterator("abcdefghijklmnopqrstuvwxyz");
		testReverseByteMatcherIterator("atat\t\tn\n");
	}
	
	private void testByteMatcherIterator(String value) {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(value);
		testByteMatcherIterator(matcher, value);
		
		for (int i = 1; i < value.length() - 1; i++) {
			String newValue = value.substring(i);
			SequenceMatcher submatch = matcher.subsequence(i);
			testByteMatcherIterator(submatch, newValue);
		}
		
		for (int i = 1; i < value.length(); i++) {
			String newValue = value.substring(0, i);
			SequenceMatcher submatch = matcher.subsequence(0, i);
			testByteMatcherIterator(submatch, newValue);
		}
	}
	

	private void testByteMatcherIterator(SequenceMatcher value, String expected) {
		Iterator<ByteMatcher> iterator = value.iterator();
		assertNotNull("Byte matcher iterator is not null", iterator);
		
		try {
			iterator.remove();
			fail("expected an unsupported operation exception removing from the iterator");
		} catch (UnsupportedOperationException ex) {};
		
		assertTrue("Byte matcher iterator must always have at least one element [" + expected + ']', iterator.hasNext());
		int position = 0;
		while (iterator.hasNext()) {
			ByteMatcher bm = iterator.next();
			byte[] matchingbytes = bm.getMatchingBytes();
			assertEquals("Matching bytes has one byte", 1, matchingbytes.length);
			
			byte b = (byte) expected.charAt(position++);
			assertEquals("Bytes match for byte " + b, b, matchingbytes[0]);
		}
		
		assertEquals("Lengths match after iteration", position, value.length());
		
		try {
			iterator.next();
			fail("expected NoSuchElementException");
		} catch (NoSuchElementException ex) {};
	}

	
	private void testReverseByteMatcherIterator(String value) {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher(value);
		matcher = matcher.reverse();
		
		testReverseByteMatcherIterator(matcher, value);
		
		for (int i = 1; i < value.length() - 1; i++) {
			String newValue = value.substring(0, value.length() - i);
			SequenceMatcher submatch = matcher.subsequence(i);
			testReverseByteMatcherIterator(submatch, newValue);
		}

	}
	
	private void testReverseByteMatcherIterator(SequenceMatcher value, String expected) {
		Iterator<ByteMatcher> iterator = value.iterator();
		assertNotNull("Byte matcher iterator is not null", iterator);
		
		try {
			iterator.remove();
			fail("expected an unsupported operation exception removing from the iterator");
		} catch (UnsupportedOperationException ex) {};
		
		assertTrue("Byte matcher iterator must always have at least one element", iterator.hasNext());
		int position = expected.length();
		while (iterator.hasNext()) {
			ByteMatcher bm = iterator.next();
			byte[] matchingbytes = bm.getMatchingBytes();
			assertEquals("Matching bytes has one byte", 1, matchingbytes.length);
			
			byte b = (byte) expected.charAt(--position);
			assertEquals("Bytes match for byte " + b, b, matchingbytes[0]);
		}
		
		assertEquals("Lengths match after iteration", 0, position);
		
		try {
			iterator.next();
			fail("expected NoSuchElementException");
		} catch (NoSuchElementException ex) {};
	}
	
	
	///////////////////////////////
	//   reader matching tests   //
	///////////////////////////////

	@Test
	public void testMatches_ByteReader_long() throws FileNotFoundException, IOException {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher(3, (byte) 0x2A);
		runTestMatchesAround(matcher, 0, 61, 1017);

		matcher = new ByteMatcherSequenceMatcher(3, (byte) 0x2A).reverse();
		runTestMatchesAround(matcher, 0, 61, 1017);

		matcher = new ByteMatcherSequenceMatcher("Here");
		runTestMatchesAround(matcher, 28200, 60836, 64481);

		matcher = matcher.subsequence(1, 4);
		runTestMatchesAround(matcher, 28201, 60837, 64482);

		matcher = new ByteMatcherSequenceMatcher("ereH").reverse();
		runTestMatchesAround(matcher, 28200, 60836, 64481);

		matcher = matcher.subsequence(1, 3);
		runTestMatchesAround(matcher, 28201, 60837, 64482);

		matcher = new ByteMatcherSequenceMatcher(new byte[] { 0x2e, 0x0d, 0x0a });
		runTestMatchesAround(matcher, 196, 42004, 112277);

		matcher = matcher.subsequence(1);
		runTestMatchesAround(matcher, 197, 42005, 112278);

		matcher = new ByteMatcherSequenceMatcher(new byte[] { 0x0a, 0x0d, 0x2e }).reverse();
		runTestMatchesAround(matcher, 196, 42004, 112277);

		matcher = matcher.subsequence(1);
		runTestMatchesAround(matcher, 197, 42005, 112278);
	}

	/**
	 * Test matching successfully over a window boundary.  
	 * A FileReader uses a default window size of 4096,
	 * so the last position in the first window is 4095.
	 * 
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	@Test
	public void testMatchesOverBoundary_ByteReader_long() throws FileNotFoundException, IOException {
		// Test around a window boundary at 4096
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("be");
		runTestMatchesAround(matcher, 4095);

		matcher = new ByteMatcherSequenceMatcher("eb").reverse();
		runTestMatchesAround(matcher, 4095);

		matcher = new ByteMatcherSequenceMatcher("Gutenberg");
		runTestMatchesAround(matcher, 4090);

		matcher = new ByteMatcherSequenceMatcher("grebnetuG").reverse();
		runTestMatchesAround(matcher, 4090);
	}

	////////////////////////////////////
	//   reader out of bounds tests   //
	////////////////////////////////////    

	@Test
	public void testMatchesReaderOutOfBoundsNegative() throws IOException {
		for (FileReader reader : readers) {
			SequenceMatcher matcher = new ByteMatcherSequenceMatcher("xxx");
			assertFalse("negative position", matcher.matches(reader, -1));
			assertFalse("past end", matcher.matches(reader, 10000000));
			
			matcher = matcher.reverse();
			assertFalse("reverse negative position", matcher.matches(reader, -1));
			assertFalse("reverse past end", matcher.matches(reader, 10000000));
		}
	}

	@Test
	public void testMatchesReaderOutOfBoundsCrossingEnd() throws IOException {
		for (FileReader reader : readers) {
			SequenceMatcher matcher = new ByteMatcherSequenceMatcher(new byte[] { 0x65, 0x2e, 0x0d, 0x0a, 0x00 });
			assertFalse("longer than end", matcher.matches(reader, 112276));
	
			matcher = new ByteMatcherSequenceMatcher(new byte[] { 0x00, 0x0a, 0x0d, 0x2e, 0x65 }).reverse();
			assertFalse("reverse longer than end", matcher.matches(reader, 112276));
		}
	}

	/////////////////////////////////
	//   byte array matches tests  //
	/////////////////////////////////      

	@Test
	public void testMatches_byteArr_int() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher(3, (byte) 0x2A);
		runTestMatchesAroundArray(matcher, 0, 61, 1017);

		matcher = new ByteMatcherSequenceMatcher(3, (byte) 0x2A).reverse();
		runTestMatchesAroundArray(matcher, 0, 61, 1017);
	}

	@Test
	public void testMatchesNoBoundsCheck_byteArr_int() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher(3, (byte) 0x2A);
		testMatchesAroundArrayNoCheck(matcher, 61);
		testMatchesAroundArrayNoCheck(matcher, 1017);

		matcher = new ByteMatcherSequenceMatcher(3, (byte) 0x2a).reverse();
		testMatchesAroundArrayNoCheck(matcher, 61);
		testMatchesAroundArrayNoCheck(matcher, 1017);
	}

	////////////////////////////////////////
	//   byte array out of bounds tests   //
	////////////////////////////////////////  

	@Test
	public void testMatches_outOfBoundsNegative() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("Titania");
		assertFalse("matches at negative pos", matcher.matches(bytes, -1));
		
		matcher = matcher.reverse();
		assertFalse("matches at negative pos", matcher.matches(bytes, -1));
	}
	

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testMatchesNoBoundsCheck_outOfBoundsNegative() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("Oberon");
		matcher.matchesNoBoundsCheck(bytes, -1);
		
		matcher = matcher.reverse();
		matcher.matchesNoBoundsCheck(bytes, -1);
	}

	
	@Test
	public void testMatches_outOfBoundsPastEnd() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("Bottom");
		assertFalse("matches past end", matcher.matches(bytes, 4096));
		
		matcher = matcher.reverse();
		assertFalse("matches past end", matcher.matches(bytes, 4096));
	}

	
	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testMatchesNoBoundsCheck_outOfBoundsPastEnd() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("Puck");
		matcher.matchesNoBoundsCheck(bytes, 4096);
	}
	

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testReverseMatchesNoBoundsCheck_outOfBoundsPastEnd() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("kcuP");
		matcher = matcher.reverse();
		matcher.matchesNoBoundsCheck(bytes, 4096);
	}
	
	
	@Test
	public void testMatches_outOfBoundsCrossingEnd() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("be");
		assertFalse("matches crossing end", matcher.matches(bytes, 4095));
		
		matcher = new ByteMatcherSequenceMatcher("eb").reverse();
		assertFalse("matches crossing end", matcher.matches(bytes, 4095));
	}
	

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testMatchesNoBoundsCheck_outOfBoundsCrossingEnd() {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher("be");
		matcher.matchesNoBoundsCheck(bytes, 4095);
	}
	
	
	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testReverseMatchesNoBoundsCheck_outOfBoundsCrossingEnd() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("eb");
		matcher = matcher.reverse();
		matcher.matchesNoBoundsCheck(bytes, 4095);
	}


	///////////////////////////////////
	//  representation test methods  //
	///////////////////////////////////   

	@Test
	public void testToRegularExpression() {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher("abc");
		assertEquals("reg ex abc", "'abc'", matcher.toRegularExpression(true));
		assertEquals("reg ex abc", "616263", matcher.toRegularExpression(false));

		matcher = new ByteMatcherSequenceMatcher(new TwoByteMatcher((byte) 0x01, (byte) 0x02),
				 								 new OneByteMatcher((byte) 0x00)); 
		assertEquals("reg ex [01 02] 00", "[01 02] 00", matcher.toRegularExpression(true));
		assertEquals("reg ex 00 [01 02]", "[0102]00", matcher.toRegularExpression(false));
		
		matcher = new ByteMatcherSequenceMatcher(new OneByteMatcher((byte) 0x00), 
												 new TwoByteMatcher((byte) 0x01, (byte) 0x02));
		assertEquals("reg ex 00 [01 02]", "00 [01 02]", matcher.toRegularExpression(true));
		assertEquals("reg ex 00 [01 02]", "00[0102]", matcher.toRegularExpression(false));

		matcher = new ByteMatcherSequenceMatcher(new OneByteMatcher((byte) 0x00), 
				                                 new OneByteMatcher((byte) 0xff),
				                                 new TwoByteMatcher((byte) 0x01, (byte) 0x02),
				                                 new TwoByteMatcher((byte) 0x03, (byte) 0x04),
		 										 new OneByteMatcher((byte) 0xfe));
		 										 
		assertEquals("reg ex 00 ff [01 02] [03 04] fe", "00 ff [01 02] [03 04] fe", matcher.toRegularExpression(true));
		assertEquals("reg ex 00 [01 02]", "00ff[0102][0304]fe", matcher.toRegularExpression(false));
		
		matcher = new ByteMatcherSequenceMatcher(new OneByteMatcher((byte) 0x41), 
                								 new OneByteMatcher((byte) 0x42),
                								 new TwoByteMatcher((byte) 0x01, (byte) 0x02),
                								 new TwoByteMatcher((byte) 0x03, (byte) 0x04),
                								 new OneByteMatcher((byte) 0x01));
				 
		assertEquals("reg ex 'AB' [01 02] [03 04] 01", "'AB' [01 02] [03 04] 01", matcher.toRegularExpression(true));
		assertEquals("reg ex 4142[0102][0304]01", "4142[0102][0304]01", matcher.toRegularExpression(false));
	}
	
	
	@Test
	public void testReverseToRegularExpression() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("abc").reverse();
		assertEquals("reg ex cba", "'cba'", matcher.toRegularExpression(true));
		assertEquals("reg ex cba", "636261", matcher.toRegularExpression(false));

		matcher = new ByteMatcherSequenceMatcher(new TwoByteMatcher((byte) 0x01, (byte) 0x02),
				 								 new OneByteMatcher((byte) 0x00)).reverse(); 
		assertEquals("reg ex 00 [01 02]", "00 [01 02]", matcher.toRegularExpression(true));
		assertEquals("reg ex 00 [01 02]", "00[0102]", matcher.toRegularExpression(false));
		
		matcher = new ByteMatcherSequenceMatcher(new OneByteMatcher((byte) 0x00), 
												 new TwoByteMatcher((byte) 0x01, (byte) 0x02)).reverse();
		assertEquals("reg ex [01 02] 00", "[01 02] 00", matcher.toRegularExpression(true));
		assertEquals("reg ex [0102]00", "[0102]00", matcher.toRegularExpression(false));

		matcher = new ByteMatcherSequenceMatcher(new OneByteMatcher((byte) 0x00), 
				                                 new OneByteMatcher((byte) 0xff),
				                                 new TwoByteMatcher((byte) 0x01, (byte) 0x02),
				                                 new TwoByteMatcher((byte) 0x03, (byte) 0x04),
		 										 new OneByteMatcher((byte) 0xfe)).reverse();
		 										 
		assertEquals("reg ex fe [03 04] [01 02] ff 00", "fe [03 04] [01 02] ff 00", matcher.toRegularExpression(true));
		assertEquals("reg ex fe[0304][0102]ff00", "fe[0304][0102]ff00", matcher.toRegularExpression(false));
		
		matcher = new ByteMatcherSequenceMatcher(new OneByteMatcher((byte) 0x41), 
                								 new OneByteMatcher((byte) 0x42),
                								 new TwoByteMatcher((byte) 0x01, (byte) 0x02),
                								 new TwoByteMatcher((byte) 0x03, (byte) 0x04),
                								 new OneByteMatcher((byte) 0x01)).reverse();
				 
		assertEquals("reg ex 01 [03 04] [01 02] 'BA'", "01 [03 04] [01 02] 'BA'", matcher.toRegularExpression(true));
		assertEquals("reg ex 01[0304][0102]4241", "01[0304][0102]4241", matcher.toRegularExpression(false));
	}

	
	@Test
	public void testToString() {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher("abc");
		assertTrue("String contains class name", matcher.toString().contains("ByteMatcherSequenceMatcher"));
		assertTrue("String contains data", matcher.toString().contains("abc"));
	}

	@Test
	public void testGetByteMatcherForPosition() {
		testMatchersForSequence("abc");
		testMatchersForSequence("x");
		testMatchersForSequence("Midsommer");
		testMatchersForSequence("testGetByteMatcherForPosition");
	}

	/////////////////////////
	//  view test methods  //
	/////////////////////////    

	@Test
	public void testReverse() {
		testReversed("a");
		testReversed("abcdefg");
		testReversed("xx");
		testReversed("1234567890abcdefghijklmnopqrstuvwxyz");
	}
	

	@Test
	public void testSubsequence() {
		testSubSequence(" ");
		testSubSequence("abc");
		testSubSequence("I know a banke where the wilde thyme blows");
		testSubSequence("xx");
		testSubSequence("\tq\tw\te\tr\ni\r\t\t \tx\t.");
	}
	
	
	@Test(expected = IndexOutOfBoundsException.class) 
	public void testNegativeSubsequenceBeginIndex() {
		ByteMatcherSequenceMatcher test = new ByteMatcherSequenceMatcher("0123456789");
		test.subsequence(-1);
	}

	
	@Test(expected = IndexOutOfBoundsException.class) 
	public void testSubsequenceEndIndexTooBig() {
		ByteMatcherSequenceMatcher test = new ByteMatcherSequenceMatcher("0123456789");
		test.subsequence(0, 11);
	}

	
	@Test(expected = IndexOutOfBoundsException.class) 
	public void testSubsequenceStartIndexTooBig() {
		ByteMatcherSequenceMatcher test = new ByteMatcherSequenceMatcher("0123456789");
		test.subsequence(5, 5);
	}
	
	
	@Test
	public void testRepeatSubSequence() {
		byte[] array = new byte[] {(byte) 0xc3};
		int repeats = 1;
		for (int testNo = 0; testNo < 10; testNo++) {
			
			for (int startIndex = 0; startIndex < array.length; startIndex++) {
				testRepeatSubSequence(repeats, array, startIndex, array.length);
			}
			
			for (int endIndex = 1; endIndex <= array.length; endIndex++) {
				testRepeatSubSequence(repeats, array, 0, endIndex);
			}
			
			for (int randTest = 0; randTest < 10; randTest++) {
				int startIndex = rand.nextInt(array.length);
				int endIndex = startIndex + rand.nextInt(array.length - startIndex) + 1;
				testRepeatSubSequence(repeats, array, startIndex, endIndex);
			}
			
			array = createRandomArray(513);
			repeats = rand.nextInt(10) + 1;
		}
	}
	
	private void testRepeatSubSequence(int repeats, byte[] array, int startIndex, int endIndex) {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(repeats, array, startIndex, endIndex);
		testRepeatSubSequence(array, matcher, repeats, startIndex, endIndex);
		matcher = new ByteMatcherSequenceMatcher(array, startIndex, endIndex);
		testRepeatSubSequence(array, matcher.repeat(repeats), repeats, startIndex, endIndex);
	}
	
	
	private void testRepeatSubSequence(byte[] array, SequenceMatcher matcher, int repeats, int startIndex, int endIndex) {
		int length = endIndex - startIndex;
		assertEquals("Length  is " + repeats * length, repeats * length, matcher.length());
		for (int i = 0; i < matcher.length(); i++) {
			byte arrayByte = array[startIndex + (i % length)];
			ByteMatcher bm = matcher.getMatcherForPosition(i);
			byte[] matchingBytes = bm.getMatchingBytes();
			assertEquals("Matcher only matches one byte", 1, matchingBytes.length);
			assertEquals("Bytes are correct in repeated matcher", arrayByte, matchingBytes[0]);
		}
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testRepeatNotZero() {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher((byte) 0x01);
		matcher.repeat(0);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testRepeatNotNegative() {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher((byte) 0x01);
		matcher.repeat(-1);
	}	
	
	
	@Test
	public void testRepeatReverseSubSequence() {
		byte[] array = new byte[] {(byte) 0xc3};
		int repeats = 1;
		for (int testNo = 0; testNo < 10; testNo++) {
			
			for (int startIndex = 0; startIndex < array.length; startIndex++) {
				testRepeatReverseSubSequence(repeats, array, startIndex, array.length);
			}
			
			for (int endIndex = 1; endIndex <= array.length; endIndex++) {
				testRepeatReverseSubSequence(repeats, array, 0, endIndex);
			}
			
			for (int randTest = 0; randTest < 10; randTest++) {
				int startIndex = rand.nextInt(array.length);
				int endIndex = startIndex + rand.nextInt(array.length - startIndex) + 1;
				testRepeatReverseSubSequence(repeats, array, startIndex, endIndex);
			}
			
			array = createRandomArray(513);
			repeats = rand.nextInt(10) + 1;
		}
	}
	
	private void testRepeatReverseSubSequence(int repeats, byte[] array, int startIndex, int endIndex) {
		ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(repeats, array, startIndex, endIndex);
		testRepeatReverseSubSequence(array, matcher, repeats, startIndex, endIndex);
		matcher = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(1, array, startIndex, endIndex);
		testRepeatReverseSubSequence(array, matcher.repeat(repeats), repeats, startIndex, endIndex);
	}
	
	
	private void testRepeatReverseSubSequence(byte[] array, SequenceMatcher matcher, int repeats, int startIndex, int endIndex) {
		int length = endIndex - startIndex;
		assertEquals("Length  is " + repeats * length, repeats * length, matcher.length());
		for (int i = 0; i < matcher.length(); i++) {
			byte arrayByte = array[endIndex - (i % length) - 1];
			ByteMatcher bm = matcher.getMatcherForPosition(i);
			byte[] matchingBytes = bm.getMatchingBytes();
			assertEquals("Matcher only matches one byte", 1, matchingBytes.length);
			assertEquals("Bytes are correct in repeated matcher", arrayByte, matchingBytes[0]);
		}
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testRepeatReverseNotZero() {
		ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher((byte) 0x01);
		matcher.repeat(0);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testRepeatReverseNotNegative() {
		ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher((byte) 0x01);
		matcher.repeat(-1);
	}	
	
	@Test
	public void testReverseRepeatEntireSequence() {
		for (int testNo = 0; testNo < 100; testNo++) {
			byte[] array = createRandomArray(513);
			int repeats = rand.nextInt(10) + 1;
			ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher(repeats, array, 0, array.length);
			assertEquals("Length  is " + repeats * array.length, repeats * array.length, matcher.length());
			for (int i = 0; i < matcher.length(); i++) {
				byte arrayByte = array[array.length - (i % array.length) - 1];
				ByteMatcher bm = matcher.getMatcherForPosition(i);
				byte[] matchingBytes = bm.getMatchingBytes();
				assertEquals("Matcher only matches one byte", 1, matchingBytes.length);
				assertEquals("Bytes are correct in repeated matcher", arrayByte, matchingBytes[0]);
			}
		}
	}
	
	
	@Test
	public void testReverseToString() {
		SequenceMatcher matcher = new ByteMatcherSequenceMatcher("abc").reverse();
		assertTrue("String contains class name", matcher.toString().contains("ByteMatcherSequenceMatcher.ReverseByteMatcherSequenceMatcher"));
		assertTrue("String contains data", matcher.toString().contains("cba"));
	}
	

	//////////////////////////////////////////////
	//  standard sequence matcher test methods  //
	//////////////////////////////////////////////
	
	@Test(expected = IndexOutOfBoundsException.class)
	public void testNegativeByteMatcherPosition() {
		ByteMatcherSequenceMatcher test = new ByteMatcherSequenceMatcher("0123456789");
		test.getMatcherForPosition(-1);
	}
	
	@Test(expected = IndexOutOfBoundsException.class)
	public void testNegativeByteMatcherSubsequencePosition() {
		ByteMatcherSequenceMatcher test = new ByteMatcherSequenceMatcher("0123456789");
		
		SequenceMatcher testSub = test.subsequence(3);
		testSub.getMatcherForPosition(-1);
	}
	
	
	
	
	
	////////////////////////////
	//  private test methods  //
	////////////////////////////

	private void testSubSequence(String sequence) {
		testSubSequenceBeginIndex(sequence);
		testSubSequenceBeginEndIndex(sequence);
	}

	private void testSubSequenceBeginIndex(String sequence) {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(sequence);

		// Test all possible subsequences of it using only beginIndex:
		SequenceMatcher sub = matcher;
		for (int count = 1; count < sequence.length(); count++) {
			sub = sub.subsequence(1);
			assertEquals("subsequence length correct", sequence.length() - count, sub.length());
			for (int pos = count; pos < sequence.length(); pos++) {
				int charvalue = sequence.charAt(pos);
				byte[] matchingbytes = sub.getMatcherForPosition(pos - count).getMatchingBytes();
				assertEquals("only one byte matches at position", 1, matchingbytes.length);
				assertEquals("values correct at pos " + pos, charvalue, (matchingbytes[0] & 0xFF));
			}
		}

		// Now run equivalent tests for the reversed sequence:
		sub = matcher.reverse();
		for (int count = 1; count < sequence.length(); count++) {
			sub = sub.subsequence(1);
			assertEquals("subsequence length correct", sequence.length() - count, sub.length());
			for (int pos = count; pos < sequence.length(); pos++) {
				int charvalue = sequence.charAt(sequence.length() - pos - 1);
				byte[] matchingbytes = sub.getMatcherForPosition(pos - count).getMatchingBytes();
				assertEquals("only one byte matches at position", 1, matchingbytes.length);
				assertEquals("values correct at pos " + pos, charvalue, (matchingbytes[0] & 0xFF));
			}
		}

	}

	private void testSubSequenceBeginEndIndex(String sequence) {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(sequence);

		// Test all possible subsequences of it from begin index to end index:
		SequenceMatcher sub = matcher;
		for (int beginIndex = 0; beginIndex < sequence.length(); beginIndex++) {
			for (int endIndex = beginIndex + 1; endIndex <= sequence.length(); endIndex++) {
				sub = matcher.subsequence(beginIndex, endIndex);
				int sequencelength = endIndex - beginIndex;
				assertEquals("subsequence length correct",sequencelength, sub.length());
				for (int pos = 0; pos < sequencelength; pos++) {
					int charvalue = sequence.charAt(beginIndex + pos);
					byte[] matchingbytes = sub.getMatcherForPosition(pos).getMatchingBytes();
					assertEquals("only one byte matches at position", 1, matchingbytes.length);
					assertEquals("values correct at pos " + pos, charvalue, (matchingbytes[0] & 0xFF));
				}
			}
		}

		// Now run equivalent tests for the reversed sequence:
		SequenceMatcher reversed = matcher.reverse();
		for (int beginIndex = 0; beginIndex < sequence.length(); beginIndex++) {
			for (int endIndex = beginIndex + 1; endIndex <= sequence.length(); endIndex++) {
				sub = reversed.subsequence(beginIndex, endIndex);
				int sequencelength = endIndex - beginIndex;
				assertEquals("subsequence length correct", sequencelength, sub.length());
				for (int pos = 0; pos < sequencelength; pos++) {
					int charvalue = sequence.charAt(sequence.length() - beginIndex - pos - 1);
					byte[] matchingbytes = sub.getMatcherForPosition(pos).getMatchingBytes();
					assertEquals("only one byte matches at position", 1, matchingbytes.length);
					assertEquals("values correct at pos " + pos + 
							     " for sequence " + sequence +
							     " begin at " + beginIndex +
							     " end at " + endIndex,
							     charvalue, (matchingbytes[0] & 0xFF));
				}
			}
		}

	}

	/**
	 * Tests that a sequence matcher matches at a series of positions, but not
	 * immediately surrounding them using a WindowReader interface.
	 * 
	 * Also tests that the reverse of the reverse of the matcher has identical
	 * behaviour.
	 * 
	 * @param matcher
	 * @param positions
	 * @throws IOException 
	 */
	private void runTestMatchesAround(SequenceMatcher matcher, long... positions)
			throws IOException {
		runTestMatchesAroundOriginal(matcher, positions);
		runTestMatchesAroundDoubleReversed(matcher, positions);
	}

	/**
	 * Tests that a sequence matcher matches at a series of positions, but not
	 * immediately surrounding them, using a WindowReader interface.
	 * 
	 * @param matcher
	 * @param positions
	 * @throws IOException 
	 */
	private void runTestMatchesAroundOriginal(SequenceMatcher matcher, long... positions)
			throws IOException {
		for (long position : positions) {
			testMatchesAroundReader(matcher, position);
		}
	}

	/**
	 * Tests that the reverse of the reverse of a sequence matcher matches 
	 * at a series of positions, but not immediately surrounding them, 
	 * using a WindowReader interface.
	 * 
	 * @param matcher
	 * @param positions
	 * @throws IOException 
	 */
	private void runTestMatchesAroundDoubleReversed(SequenceMatcher matcher, long... positions)
			throws IOException {
		SequenceMatcher doubleReversed = matcher.reverse().reverse();
		for (long position : positions) {
			testMatchesAroundReader(doubleReversed, position);
		}
	}

	/**
	 * Tests that a sequence matcher matches at a series of positions, but not
	 * immediately surrounding them, using a byte array.
	 * 
	 * Also tests that the reverse of the reverse of the matcher has identical
	 * behaviour.
	 * 
	 * @param matcher
	 * @param positions
	 * @throws IOException 
	 */
	private void runTestMatchesAroundArray(SequenceMatcher matcher, int... positions) {
		runTestMatchesAroundOriginalArray(matcher, positions);
		runTestMatchesAroundDoubleReversedArray(matcher, positions);
	}

	/**
	 * Tests that a sequence matcher matches at a series of positions, but not
	 * immediately surrounding them, using a byte array.
	 * 
	 * @param matcher
	 * @param positions
	 * @throws IOException 
	 */
	private void runTestMatchesAroundOriginalArray(SequenceMatcher matcher, int... positions) {
		for (int position : positions) {
			testMatchesAroundArray(matcher, position);
		}
	}

	/**
	 * Tests that the reverse of the reverse of a sequence matcher matches 
	 * at a series of positions, but not immediately surrounding them, using a byte array.
	 * 
	 * @param matcher
	 * @param positions
	 * @throws IOException 
	 */
	private void runTestMatchesAroundDoubleReversedArray(SequenceMatcher matcher, int... positions) {
		SequenceMatcher doubleReversed = matcher.reverse().reverse();
		for (int position : positions) {
			testMatchesAroundArray(doubleReversed, position);
		}
	}

	/**
	 * Tests that:
	 * 
	 * - the length of a reversed sequence is the same as the original.
	 * - that each position in the reversed matcher matches only one byte.
	 * - that the bytes in the reversed matcher correspond to the original, but reversed.
	 * 
	 * @param sequence A string to construct a ByteMatcherSequenceMatcher from.
	 */
	private void testReversed(String sequence) {
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(sequence);
		SequenceMatcher reversed = matcher.reverse();
		int matcherLength = matcher.length();
		assertEquals(sequence + " length", matcherLength, reversed.length());

		for (int index = 0; index < matcherLength; index++) {
			byte[] matcherbytes = matcher.getMatcherForPosition(index).getMatchingBytes();
			assertEquals(sequence + " matches one byte at index" + index, 1, matcherbytes.length);
			byte[] reversebytes = reversed.getMatcherForPosition(reversed.length() - index - 1)
					.getMatchingBytes();
			assertArrayEquals(sequence + " bytes match", matcherbytes, reversebytes);
		}
	}

	/**
	 * Tests that:
	 * 
	 * - a matcher matches at a given position in a FileReader.
	 * - it does not match one position behind that position.
	 * - it does not match one position ahead of that position.
	 * 
	 * @param matcher
	 * @param pos
	 * @throws IOException 
	 */
	private void testMatchesAroundReader(SequenceMatcher matcher, long pos) throws IOException {
		String matchDesc = matcher.toRegularExpression(true);
		for (FileReader reader : readers) {
			assertTrue(matchDesc + " at pos " + Long.toString(pos), matcher.matches(reader, pos));
			assertFalse(matchDesc + " at pos " + Long.toString(pos - 1),
					matcher.matches(reader, pos - 1));
			assertFalse(matchDesc + " at pos " + Long.toString(pos + 1),
					matcher.matches(reader, pos + 1));
		}
	}

	/**
	 * Tests that:
	 * 
	 * - a matcher matches at a given position in a byte array.
	 * - it does not match one position behind that position.
	 * - it does not match one position ahead of that position.
	 * 
	 * @param matcher
	 * @param pos 
	 */
	private void testMatchesAroundArray(SequenceMatcher matcher, int pos) {
		String matchDesc = matcher.toRegularExpression(true);
		assertTrue(matchDesc + " at pos " + Long.toString(pos), matcher.matches(bytes, pos));
		assertFalse(matchDesc + " at pos " + Long.toString(pos - 1),
				matcher.matches(bytes, pos - 1));
		assertFalse(matchDesc + " at pos " + Long.toString(pos + 1),
				matcher.matches(bytes, pos + 1));
	}

	/**
	 * Tests that:
	 * 
	 * - a matcher matches at a given position in a byte array using a no bounds check match.
	 * - it does not match one position behind that position.
	 * - it does not match one position ahead of that position.
	 * 
	 * @param matcher
	 * @param pos 
	 */
	private void testMatchesAroundArrayNoCheck(SequenceMatcher matcher, int pos) {
		String matchDesc = matcher.toRegularExpression(true);
		assertTrue(matchDesc + " at pos " + Long.toString(pos),
				matcher.matchesNoBoundsCheck(bytes, pos));
		assertFalse(matchDesc + " at pos " + Long.toString(pos - 1),
				matcher.matchesNoBoundsCheck(bytes, pos - 1));
		assertFalse(matchDesc + " at pos " + Long.toString(pos + 1),
				matcher.matchesNoBoundsCheck(bytes, pos + 1));
	}

	/**
	 * Tests that:
	 * 
	 * - a matcher is the right length with the right byte values for a string sequence.
	 * - the same holds true for the reverse matcher on the reversed string.
	 * 
	 * @param sequence 
	 */
	private void testMatchersForSequence(String sequence) {
		// test forwards matcher
		ByteMatcherSequenceMatcher matcher = new ByteMatcherSequenceMatcher(sequence);
		testByteMatcherForPosition(sequence, matcher);

		// test the reversed matcher
		SequenceMatcher reversed = matcher.reverse();
		String reverseSequence = new StringBuffer(sequence).reverse().toString();
		testByteMatcherForPosition(reverseSequence, reversed);
	}

	/**
	 * Tests that:
	 * 
	 * - each position in a byte matcher constructed from a string matches only one byte.
	 * - the value is the corresponding value in the original string.
	 * 
	 * @param sequence
	 * @param m 
	 */
	private void testByteMatcherForPosition(String sequence, SequenceMatcher m) {
		for (int position = 0; position < sequence.length(); position++) {
			byte[] onebytes = m.getMatcherForPosition(position).getMatchingBytes();
			assertEquals(sequence + " length", 1, onebytes.length);
			assertEquals(sequence + "value", sequence.charAt(position), onebytes[0] & 0xFF);
		}
	}

	///////////////////////////////
	//  private utility methods  //
	/////////////////////////////// 

	/**
	 * Returns a file given a resource name of a file in the test packages.
	 * 
	 * @param resourceName
	 * @return 
	 */
	private File getFile(final String resourceName) {
		URL url = this.getClass().getResource(resourceName);
		return new File(url.getPath());
	}
	
	private List<FileReader> getReaders(String resourceName, int... windowFromTo) throws FileNotFoundException {
		List<FileReader> result = new ArrayList<FileReader>(32);
		for (int pos = 0; pos < windowFromTo.length; pos += 2) {
			int windowSizeFrom = windowFromTo[pos];
			int windowSizeTo   = windowFromTo[pos + 1];
			for (int windowSize = windowSizeFrom; windowSize <= windowSizeTo; windowSize++) {
				result.add(new FileReader(getFile(resourceName), windowSize));
			}
		}
		return result;
	}

	/**
	 * Creates a random length byte array containing random bytes.
	 * 
	 * @param maxLength
	 * @return 
	 */
	private byte[] createRandomArray(final int maxLength) {
		final int length = rand.nextInt(maxLength) + 1;
		final byte[] array = new byte[length];
		for (int pos = 0; pos < length; pos++) {
			array[pos] = (byte) rand.nextInt(256);
		}
		return array;
	}
	
	
	/**
	 * Creates a random length ByteMatcher array containing random bytes.
	 * 
	 * @param maxLength
	 * @return 
	 */
	private ByteMatcher[] createRandomByteMatcherArray(final int maxLength) {
		final int length = rand.nextInt(maxLength) + 1;
		final ByteMatcher[] array = new ByteMatcher[length];
		for (int pos = 0; pos < length; pos++) {
			array[pos] = OneByteMatcher.valueOf((byte) rand.nextInt(256));
		}
		return array;
	}

	/**
	 * Creates a random length list of random length matchers.
	 * The matchers are constructed using either a random byte value,
	 * a random length byte array, or a random number of repeated random
	 * byte values.
	 * 
	 * @param maxNum
	 * @return 
	 */
	private List<ByteMatcherSequenceMatcher> createRandomList(final int maxNum) {
		final int noOfMatchers = rand.nextInt(maxNum) + 1;
		final List<ByteMatcherSequenceMatcher> matchers = new ArrayList<ByteMatcherSequenceMatcher>();
		for (int num = 0; num < noOfMatchers; num++) {
			final int matchType = rand.nextInt(3);
			ByteMatcherSequenceMatcher matcher;
			switch (matchType) {
			case 0: {
				final int byteValue = rand.nextInt(256);
				matcher = new ByteMatcherSequenceMatcher((byte) byteValue);
				break;
			}
			case 1: {
				final byte[] values = createRandomArray(256);
				matcher = new ByteMatcherSequenceMatcher(values);
				break;
			}
			case 2: {
				final int byteValue = rand.nextInt(256);
				final int repeats = rand.nextInt(256) + 1;
				matcher = new ByteMatcherSequenceMatcher(repeats, (byte) byteValue);
				break;
			}
			default: {
				throw new IllegalArgumentException("Invalid matcher type");
			}
			}
			matchers.add(matcher);
		}
		return matchers;
	}

}
