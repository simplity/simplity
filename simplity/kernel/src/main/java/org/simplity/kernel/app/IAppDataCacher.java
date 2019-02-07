/*
 * Copyright (c) 2018 simplity.org
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.kernel.app;

/**
 * method signature for a utility that allows components to use caching instead
 * of going to db. Implementing class should utilize the caching infrastructure
 * that maybe standardized by the organization.
 *
 * @author simplity.org
 */
public interface IAppDataCacher {
	/**
	 * get a cached object with single key.
	 *
	 * @param key
	 *
	 * @return cached object, or null.
	 */
	public Object get(String key);

	/**
	 * cache this key-object pair
	 *
	 * @param key
	 * @param object
	 */
	public void put(String key, Object object);

	/**
	 * remove/invalidate cached key-object pair
	 *
	 * @param key
	 */
	public void invalidate(String key);

	/**
	 * get a cached object with primary and secondary key.
	 *
	 * @param primaryKey
	 *            non-null
	 * @param secondaryKey
	 *            non-null.
	 *
	 * @return cached object, or null.
	 */
	public Object get(String primaryKey, String secondaryKey);

	/**
	 * cache this object with primary and secondary key. Implementation may use
	 * the underlying caching infrastructure to cache a map by secondary key as
	 * he cached object, or keep a set of secondary keys as its attribute and
	 * cache objects as individual objects, depending on what is more
	 * appropriate for a given implementation. Caller is unaware of this
	 * mechanism.
	 *
	 * Also this feature is to be used ONLY IF invalidate all objects under a
	 * primary key is required.
	 *
	 * @param primaryKey
	 *            non-null
	 * @param secondaryKey
	 *            non-null.
	 * @param object
	 */
	public void put(String primaryKey, String secondaryKey, Object object);

	/**
	 * remove/invalidate cache with primary and secondary key
	 *
	 * @param primaryKey
	 *            non-null
	 *
	 * @param secondaryKey
	 *            if null, all entries under primary key to be invalidated
	 */
	public void invalidate(String primaryKey, String secondaryKey);

	/**
	 * zap everything
	 */
	public void clearAll();

}
