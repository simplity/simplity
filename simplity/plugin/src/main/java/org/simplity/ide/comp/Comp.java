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

package org.simplity.ide.comp;

import java.util.HashSet;
import java.util.Set;

import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.IComponent;

/**
 * Represents an application component definition in an IDE environment. Wrapper
 * on a <code>org.simplity.kernel.comp.Component</code>.
 *
 * @author simplity.org
 *
 */
class Comp {

	/**
	 * what type of component is this
	 */
	private final ComponentType componentType;
	/**
	 * unique id. compType + compId would be unique
	 */
	private final String compId;

	/**
	 * in which file/resource is this defined?
	 */
	private Resource resource;

	/**
	 * set of components referenced (used) by this comp. This is one-step, and
	 * does not take care of transitive dependencies. That is, if this comp
	 * refers to A and A refers to B we do not store B here
	 */
	private Set<Comp> referredComps;

	/**
	 * set of other comps that refer this comp. Does not store transitively
	 * dependent comps. We will have to traverse the dependency tree to get
	 * that, if required
	 */
	private Set<Comp> dependentComps;

	/**
	 * construct a comp
	 *
	 * @param resource
	 *            - null if this is constructed before it is parsed. non-null if
	 *            it is constructed because it is found in a resource. If this
	 *            is null, then a subsequent call to setResource() is possible
	 *
	 * @param componentType
	 *            null for app, non-null for regular <code>Component</code>
	 * @param compId
	 *            null for app. unique id for the comp-time for regular
	 *            Component
	 */
	Comp(Resource resource, ComponentType componentType, String compId) {
		this.resource = resource;
		this.componentType = componentType;
		this.compId = compId;
	}

	/**
	 * construct a comp
	 *
	 * @param resource
	 *            - null if this is constructed before it is parsed. non-null if
	 *            it is constructed because it is found in a resource. If this
	 *            is null, then a subsequent call to setResource() is possible
	 *
	 * @param component
	 *            non-null
	 */
	Comp(Resource resource, IComponent component) {
		this.resource = resource;
		this.compId = component.getQualifiedName();
		this.componentType = component.getComponentType();
	}

	public String getCompId() {
		return this.compId;
	}

	public ComponentType getComponentType() {
		return this.componentType;
	}

	public boolean exists() {
		return this.resource != null;
	}

	private void addDependentComp(Comp comp) {
		if (this.dependentComps == null) {
			this.dependentComps = new HashSet<>();
		}
		this.dependentComps.add(comp);
	}

	public void setResource(Resource res) {
		this.resource = res;
	}

	public boolean removeResource() {
		this.resource = null;
		if (this.referredComps == null) {
			return false;
		}

		for (Comp comp : this.referredComps) {
			comp.removeDependentComp(this);
		}
		this.referredComps = null;
		return true;
	}

	private void removeDependentComp(Comp comp) {
		if (this.dependentComps == null) {
			return;
		}

		if (!this.dependentComps.remove(comp)) {
			return;
		}

		if (this.dependentComps.size() == 0) {
			this.dependentComps = null;
		}
	}

	public Resource getResource() {
		return this.resource;
	}

	public void addReferredComp(Comp comp) {
		if (this.referredComps == null) {
			this.referredComps = new HashSet<>();
		}
		this.referredComps.add(comp);
		comp.addDependentComp(this);
	}

	public Set<Comp> getReferredComp() {
		return this.referredComps;
	}

	public Set<Comp> getDependentComps() {
		return this.dependentComps;
	}

	public boolean isDefined() {
		return this.resource != null;
	}

	@Override
	public String toString() {
		return "Comp " + this.compId + " of type " + this.componentType;
	}
}
