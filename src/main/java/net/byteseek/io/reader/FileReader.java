/*
 * Copyright Matt Palmer 2011-2015, All rights reserved.
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

package net.byteseek.io.reader;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.byteseek.io.IOUtils;
import net.byteseek.io.reader.cache.LeastRecentlyUsedCache;
import net.byteseek.io.reader.cache.WindowCache;
import net.byteseek.io.reader.windows.HardWindow;
import net.byteseek.io.reader.windows.SoftWindow;
import net.byteseek.io.reader.windows.SoftWindowRecovery;
import net.byteseek.io.reader.windows.Window;
import net.byteseek.utils.ArgUtils;

/**
 * A WindowReader extending {@link AbstractReader} which reads a random access file
 * into cached byte arrays.  It also implements the SoftWindowRecovery interface,
 * which allows windows to reload their byte arrays when using SoftWindows (as the
 * garbage collector may have re-claimed their array under low memory conditions
 * previously).
 * <p>
 * This class (like the underlying RandomAccessFile) is not thread-safe.
 * 
 * @author matt
 */
public class FileReader extends AbstractReader implements SoftWindowRecovery {

	private final static String READ_ONLY = "r";

	private final File file;
	private final RandomAccessFile randomAccessFile;
	private final long length;
    private boolean useSoftWindows;

	/**
	 * Constructs a FileReader which defaults to an array size of 4096, caching
	 * the last 32 most recently used Windows in a {@link net.byteseek.io.reader.cache.LeastRecentlyUsedCache}
	 *
	 * @param file The file to read from.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IllegalArgumentException if the file passed in is null.
	 */
	public FileReader(final File file) throws FileNotFoundException {
		this(file, DEFAULT_WINDOW_SIZE, new LeastRecentlyUsedCache(DEFAULT_CAPACITY));
	}

	/**
	 * Constructs a FileReader which defaults to a {@link net.byteseek.io.reader.windows.Window} size of 4096
	 * using the WindowCache passed in to cache ArrayWindows.
	 * 
	 * @param file  The file to read from.
	 * @param cache The cache of Windows to use.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IllegalArgumentException if the file passed in is null.
	 */
	public FileReader(final File file, final WindowCache cache)
			throws FileNotFoundException {
		this(file, DEFAULT_WINDOW_SIZE, cache);
	}

	/**
	 * Constructs a FileReader using the {@link net.byteseek.io.reader.windows.Window} size passed in, and
	 * caches the last 32 Windows in a {@link net.byteseek.io.reader.cache.LeastRecentlyUsedCache}.
	 * 
	 * @param file The file to read from.
	 * @param windowSize The size of the byte array to read from the file.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IllegalArgumentException if the file passed in is null.
	 */
	public FileReader(final File file, final int windowSize)
			throws FileNotFoundException {
		this(file, windowSize, new LeastRecentlyUsedCache(DEFAULT_CAPACITY));
	}

	/**
	 * Constructs a FileReader using the array size passed in, and caches the
	 * last most recently used Windows up to the capacity specified in a
	 * {@link net.byteseek.io.reader.cache.LeastRecentlyUsedCache}.
	 * 
	 * @param file The file to read from.
	 * @param windowSize the size of the byte array to read from the file.
	 * @param capacity the number of byte arrays to cache (using a most recently used strategy).
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IllegalArgumentException if the file passed in is null.
	 */
	public FileReader(final File file, final int windowSize, final int capacity)
			throws FileNotFoundException {
		this(file, windowSize, new LeastRecentlyUsedCache(capacity));
	}

	/**
	 * Constructs a FileReader which defaults to a {@link net.byteseek.io.reader.windows.Window} size of 4096,
	 * caching the last 32 most recently used {@link net.byteseek.io.reader.windows.Window}s in a
	 * {@link net.byteseek.io.reader.cache.LeastRecentlyUsedCache}.
	 * 
	 * @param path The path of the file to read from.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IllegalArgumentException if the path passed in is null.
	 */
	public FileReader(final String path) throws FileNotFoundException {
		this(path == null? null : new File(path), DEFAULT_WINDOW_SIZE, new LeastRecentlyUsedCache(DEFAULT_CAPACITY));
	}

	/**
	 * Constructs a FileReader which defaults to a {@link net.byteseek.io.reader.windows.Window} size of 4096
	 * using the {@link WindowCache} passed in to cache Windows.
	 * 
	 * @param path The path of the file to read from.
	 * @param cache	The cache of Windows to use.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IllegalArgumentException If the path or cache passed in is null.
	 */
	public FileReader(final String path, final WindowCache cache)
			throws FileNotFoundException {
		this(path == null? null : new File(path), DEFAULT_WINDOW_SIZE, cache);
	}

	/**
	 * Constructs a FileReader using the {@link net.byteseek.io.reader.windows.Window} size passed in, and
	 * caches the last 32 Windows in a {@link net.byteseek.io.reader.cache.LeastRecentlyUsedCache}.
	 * 
	 * @param path  The path of the file to read from.
	 * @param windowSize The size of the byte array to read from the file.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IllegalArgumentException If the file passed in is null.
	 */
	public FileReader(final String path, final int windowSize)
			throws FileNotFoundException {
		this(path == null? null : new File(path), windowSize, new LeastRecentlyUsedCache(DEFAULT_CAPACITY));
	}

	/**
	 * Constructs a FileReader using the {@link net.byteseek.io.reader.windows.Window} size passed in, and
	 * caches the last Windows up to the capacity supplied using a
	 * {@link net.byteseek.io.reader.cache.LeastRecentlyUsedCache}.
	 * 
	 * @param path The path of the file to read from.
	 * @param windowSize The size of the byte array to read from the file.
	 * @param capacity The number of byte arrays to cache (using a least recently used strategy).
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IllegalArgumentException if the file passed in is null.
	 */
	public FileReader(final String path, final int windowSize,
			final int capacity) throws FileNotFoundException {
		this(path == null? null : new File(path), windowSize, new LeastRecentlyUsedCache(capacity));
	}

	/**
	 * Constructs a FileReader which reads the file into {@link net.byteseek.io.reader.windows.Window}s of the
	 * specified size, using the {@link WindowCache} supplied to cache them.
	 * 
	 * @param file The file to read from.
	 * @param windowSize The size of the byte array to read from the file.
	 * @param cache The cache of Windows to use.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IllegalArgumentException If the file or cache passed in is null.
	 */
	public FileReader(final File file, final int windowSize,
			final WindowCache cache) throws FileNotFoundException {
		super(windowSize, cache);
		ArgUtils.checkNullObject(file, "file");
		this.file = file;
		randomAccessFile = new RandomAccessFile(file, READ_ONLY);
		length = file.length();
	}

	/**
	 * Returns the length of the file.
	 * 
	 * @return The length of the file accessed by the reader.
	 */
	@Override
	public final long length() {
		return length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Window createWindow(final long windowStart) throws IOException {
		if (windowStart >= 0) {
			try {
				randomAccessFile.seek(windowStart);
				final byte[] bytes = new byte[windowSize];
				final int totalRead = IOUtils.readBytes(randomAccessFile, bytes);
				if (totalRead > 0) {
					return useSoftWindows? new SoftWindow(bytes, windowStart, totalRead, this)
							             : new HardWindow(bytes, windowStart, totalRead);
				}
			} catch (final EOFException justReturnNull) {
			}
		}
		return null;
	}

	/**
	 * Closes the underlying {@link java.io.RandomAccessFile}, then clears any
	 * cache associated with this WindowReader.
	 */
	@Override
	public void close() throws IOException {
		try {
			randomAccessFile.close();
		} finally {
			super.close();
		}
	}

	/**
	 * Returns the {@link java.io.File} object accessed by this WindowReader.
	 * 
	 * @return The File object accessed by this WindowReader.
	 */
	public final File getFile() {
		return file;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[file:" + file + " length: " + file.length() + " cache:" + cache + ']'; 
	}

	public void useSoftWindows(boolean useSoftWindows) {
		this.useSoftWindows = useSoftWindows;
	}

	@Override
	public byte[] reloadWindowBytes(final Window window) throws IOException {
		randomAccessFile.seek(window.getWindowPosition());
		final byte[] bytes = new byte[windowSize];
		IOUtils.readBytes(randomAccessFile, bytes);
		return bytes;
	}

}
