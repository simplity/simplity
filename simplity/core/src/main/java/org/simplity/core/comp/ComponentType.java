/*
 * Copyright (c) 2016 simplity.org
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

package org.simplity.core.comp;

import org.simplity.core.adapter.DataAdapter;
import org.simplity.core.batch.BatchJobs;
import org.simplity.core.dm.Record;
import org.simplity.core.dt.DataType;
import org.simplity.core.fn.IFunction;
import org.simplity.core.msg.Message;
import org.simplity.core.service.ExternalService;
import org.simplity.core.sql.Sql;
import org.simplity.core.sql.StoredProcedure;
import org.simplity.core.test.TestRun;
import org.simplity.core.trans.Service;

/**
 * components are the basic building blocks of application. This is an
 * enumeration of them. Unlike typical Enum class, this one is quite
 * comprehensive with utility methods to load/cache components
 *
 * @author simplity.org
 */
public enum ComponentType {
	/** Data Type */
	DT(0, DataType.class, true, false),
	/** Message */
	MSG(1, Message.class, true, false),
	/** Record */
	REC(2, Record.class, false, true),
	/** service */
	SERVICE(3, Service.class, false, true),
	/** Sql */
	SQL(4, Sql.class, false, false),
	/** Stored procedure */
	SP(5, StoredProcedure.class, false, false),
	/** function */
	FUNCTION(6, IFunction.class, true, false),

	/** test cases for service */
	TEST_RUN(7, TestRun.class, false, false),

	/** test cases for service */
	JOBS(8, BatchJobs.class, false, false),
	/**
	 * external services
	 */
	EXTERN(9, ExternalService.class, false, false),
	/**
	 * data adapter
	 */
	ADAPTER(10, DataAdapter.class, false, false);

	/**
	 * allows us to use array instead of map while dealing with componentType
	 * based collections
	 */
	private final int idx;
	private final Class<?> compClass;
	private final boolean preLoaded;
	private final boolean allowExtensions;

	ComponentType(int idx, Class<?> compClass, boolean preLoaded, boolean allowExtensions) {
		this.idx = idx;
		this.preLoaded = preLoaded;
		this.compClass = compClass;
		this.allowExtensions = allowExtensions;
	}

	/**
	 * @return idx associated with this comp type
	 */
	public int getIdx() {
		return this.idx;
	}

	/**
	 *
	 * @return true if the components of this type are pre-loaded. false if they
	 *         are loaded as and when required
	 */
	public boolean isPreloaded() {
		return this.preLoaded;
	}

	/**
	 * @return base class of this component.
	 */
	public Class<?> getCompClass() {
		return this.compClass;
	}

	/**
	 *
	 * @return true if this component can be extended, and hence the root
	 *         element is the class name of this component. false if this root
	 *         element is pre-fixed, and teh class is design-time decided
	 */
	public boolean allowExtensions() {
		return this.allowExtensions;
	}
}
