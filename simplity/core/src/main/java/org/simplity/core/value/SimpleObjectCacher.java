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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.value;

import java.util.HashMap;
import java.util.Map;

import org.simplity.core.app.IAppDataCacher;

/**
 * a simple implementation of <code>AppDataCaccher</code> for testing purposes.
 * Not meant for production use
 *
 * @author simplity.org
 *
 */
public class SimpleObjectCacher implements IAppDataCacher {
	private final Map<String, Object> primaryStore = new HashMap<>();
	private final Map<String, Map<String, Object>> secondaryStore = new HashMap<>();

	@Override
	public Object get(String key) {
		return this.primaryStore.get(key);
	}

	@Override
	public void put(String key, Object object) {
		this.primaryStore.put(key, object);

	}

	@Override
	public void invalidate(String key) {
		this.primaryStore.remove(key);
	}

	@Override
	public Object get(String primaryKey, String secondaryKey) {
		Map<String, Object> cache = this.secondaryStore.get(primaryKey);
		if (cache == null) {
			return null;
		}
		return cache.get(secondaryKey);
	}

	@Override
	public void put(String primaryKey, String secondaryKey, Object object) {
		Map<String, Object> cache = this.secondaryStore.get(primaryKey);
		if (cache == null) {
			cache = new HashMap<>();
			this.secondaryStore.put(primaryKey, cache);
		}
		cache.put(secondaryKey, object);
	}

	@Override
	public void invalidate(String primaryKey, String secondaryKey) {
		if (secondaryKey == null) {
			this.secondaryStore.remove(primaryKey);
			return;
		}
		Map<String, Object> cache = this.secondaryStore.get(primaryKey);
		if (cache != null) {
			cache.remove(secondaryKey);
		}
	}

	@Override
	public void clearAll() {
		this.primaryStore.clear();
		this.secondaryStore.clear();
	}
}
