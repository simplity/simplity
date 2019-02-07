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

package org.simplity.kernel.adapter;

/**
 * source of data for a list. defines methods to supply list of struct data for
 * copying to a list target. If the source has defined one struct, it may wrap
 * that itself as a list source with just one member. Such an arrangement
 * provides good compatibility between generic cases and specific cases
 *
 * @author simplity.org
 *
 */
public interface IDataListSource {
	/**
	 *
	 * @return number of elements in the source
	 */
	public int length();

	/**
	 *
	 * @param zeroBasedIdx
	 * @return child source, or null if no element at that idx, or element at
	 *         that idx is null, or this is beyond the source list range
	 */
	public IDataSource getChildSource(int zeroBasedIdx);
}
