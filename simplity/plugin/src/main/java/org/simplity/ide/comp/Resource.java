/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.ide.comp;

import java.util.HashSet;
import java.util.Set;

import org.simplity.core.comp.ComponentType;

/**
 * represents a resource/file that may contain one or more component definitions
 *
 * @author simplity.org
 *
 */
public class Resource {
	private static final Comp[] EMPTY = new Comp[0];

	private ComponentType ct;
	private String path;
	private Comp soleComp;
	private Set<Comp> allComps;

	/**
	 * @param path
	 * @param ct
	 */
	public Resource(String path, ComponentType ct) {
		this.ct = ct;
		this.path = path;
		if (ct != null && ct.isPreloaded()) {
			this.allComps = new HashSet<>();
		}
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * @return the ct
	 */
	public ComponentType getComponentType() {
		return this.ct;
	}

	/**
	 * @param comp
	 */
	public void addComp(Comp comp) {
		if (this.allComps != null) {
			this.allComps.add(comp);
		} else {
			this.soleComp = comp;
		}
	}

	/**
	 * delete this resource. Comps in this resource are now marked as
	 * non-existent, but may remain as reference by other comps
	 *
	 * @return non-null array of comps that were defined in this resource. comps
	 *         are already marked as "undefined" by removing reference to the
	 *         resource. caller has to take care of any changes in the
	 *         repository, like missingComponents
	 */
	public Comp[] delete() {
		if (this.allComps != null) {
			for (Comp comp : this.allComps) {
				comp.removeResource();
			}
			return this.allComps.toArray(EMPTY);
		}

		if (this.soleComp != null) {
			this.soleComp.removeResource();
			Comp[] result = { this.soleComp };
			return result;
		}
		return EMPTY;
	}

	/**
	 *
	 * @return non-null array of comps inside this resource. array length could
	 *         be zero
	 */
	public Comp[] getComps() {
		if (this.allComps != null) {
			return this.allComps.toArray(EMPTY);
		}

		if (this.soleComp != null) {
			Comp[] result = { this.soleComp };
			return result;
		}
		return EMPTY;

	}
}
