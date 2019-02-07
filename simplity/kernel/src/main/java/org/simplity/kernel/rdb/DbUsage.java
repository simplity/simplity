/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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
package org.simplity.kernel.rdb;

import org.simplity.kernel.idb.DbAccessType;

/** defines the type of database access required for a service */
public enum DbUsage {
	/** No data base access. No need to open a connection */
	NONE(null, false) {
		@Override
		public boolean childTypeIsOk(DbUsage childType) {
			return childType == NONE;
		}
	},
	/**
	 * use a read-only connection. transaction processing is not started. any
	 * attempt inside the service to update will result in an exception
	 */
	READ_ONLY(DbAccessType.READ_ONLY, false) {
		@Override
		public boolean childTypeIsOk(DbUsage childType) {
			return childType == NONE || childType == READ_ONLY;
		}
	},
	/**
	 * a transaction processing is initiated in the beginning. At the end, it is
	 * committed except for exceptions and for error in the returned db
	 */
	READ_WRITE(DbAccessType.SINGLE_TRANS, true) {
		@Override
		public boolean childTypeIsOk(DbUsage childType) {
			return childType != DbUsage.AUTO_COMMIT && childType != SUB_SERVICE;
		}
	},
	/**
	 * a read-write connection, but no transaction processing. In case of any
	 * error/exception, earlier updates are not rolled back. This the most
	 * efficient way of updating, but has no protection for any exception.
	 * Suitable if recovery processes are defined outside the application.
	 */
	AUTO_COMMIT(DbAccessType.AUTO_COMMIT, true) {
		@Override
		public boolean childTypeIsOk(DbUsage childType) {
			return childType != DbUsage.READ_WRITE && childType != SUB_SERVICE;
		}
	},
	/**
	 * service consists of only sub-services. each sub-service has its own
	 * access type and commitment control. This Should be used ONLY UNDER
	 * EXCEPTIONAL CONDITIONS as it violates the golden rule of
	 * one-client-request-one-transaction. If one sub-service succeeds, and the
	 * second one fails, the first one IS NOT ROLLED BACK. typically, you use
	 * this feature to do some set of read operations, like extracting data for
	 * a report, and then may record the fact that the data was extracted. If
	 * you run the whole thing in a transaction, the reads will result in large
	 * number of locks, affecting performance.
	 */
	SUB_SERVICE(DbAccessType.READ_ONLY, true) {
		/**
		 * this is to be used cautiously, as the compatibility is to checked
		 * only if it is a non-sub-service action.
		 */
		@Override
		public boolean childTypeIsOk(DbUsage childType) {
			return childType == DbUsage.NONE || childType == READ_ONLY;
		}
	},
	/** service is NOT managing the transaction. It is managed by JCA/JTA/XA */
	EXTERNAL(null, true) {
		/**
		 * this is to be used cautiously, as the compatibility is to checked
		 * only if it is a non-sub-service action.
		 */
		@Override
		public boolean childTypeIsOk(DbUsage childType) {
			return true;
		}
	};
	private final DbAccessType dbAccessType;
	private final boolean updatable;

	/** */
	private DbUsage(DbAccessType dbAccessType, boolean updatable) {
		this.dbAccessType = dbAccessType;
		this.updatable = updatable;
	}

	/** @return does this access mean updates to the database? */
	public boolean updatesDb() {
		return this.updatable;
	}

	/**
	 * if a service is using this dbAccess type, can it manage an action with
	 * the supplied access type? For example, a READ_ONLY service can not have
	 * an action that requiresREAD_WRITE
	 *
	 * @param childType
	 * @return true if the child can have that access. False if it is not
	 *         possible.
	 */
	public boolean canWorkWithChildType(DbUsage childType) {
		if (childType == null) {
			return true;
		}
		return this.childTypeIsOk(childType);
	}

	/**
	 *
	 * @return db access type required for the sub-service
	 */
	public DbAccessType getDbAccessType() {
		return this.dbAccessType;
	}

	/**
	 * @param childType
	 * @return
	 */
	protected abstract boolean childTypeIsOk(DbUsage childType);
}
