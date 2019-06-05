/*
 * Copyright (c) 2019 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.fm.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author simplity.org
 *
 */
public abstract class DataStore{
	private static DataStore instance = new FileSystemStore();
	/**
	 * 
	 * @return get a store configured for this app
	 */
	public static DataStore getStore() {
		return instance;
	}
	/**
	 *
	 * @param id
	 *            non-null unique id of the data
	 * @return data for the key. null if no data stored with this key
	 * @throws IOException
	 *             in case of any error in persistence process
	 */
	public abstract String retrieve(String id) throws IOException;

	/**
	 *
	 * @param id
	 *            non-null unique id of the data
	 * @param outs
	 *            to which the content is written to
	 * @return true if all ok. false if file is not located
	 * @throws IOException
	 *             in case of any error in persistence process
	 */
	public abstract  boolean retrieve(String id, OutputStream outs) throws IOException;

	/**
	 *
	 * @param data
	 *            non-null
	 * @param id
	 *            unique id of this form across all forms handled b this store
	 * @throws IOException
	 *             in case of any error in persistence process
	 */
	public abstract void store(String data, String id) throws IOException;
	/**
	 *
	 * @param id
	 *            unique id of this form across all forms handled b this store
	 * @return non-null output stream to which data to be saved is to be written to
	 * @throws IOException
	 *             in case of any error in persistence process
	 */
	public abstract OutputStream getOutStream(String id) throws IOException;
}
