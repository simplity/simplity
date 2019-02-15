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

package org.simplity.core.rdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.simplity.core.ApplicationError;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.idb.DbAccessType;
import org.simplity.core.idb.ITransactionHandle;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.sql.ProcedureParameter;
import org.simplity.core.util.RdbUtil;
import org.simplity.core.value.Value;

/**
 * @author simplity.org
 *
 */
public class TransactionHandle extends ReadonlyHandle implements ITransactionHandle {
	private static final DbAccessType HANDLE_TYPE = DbAccessType.READ_ONLY;

	TransactionHandle(Connection con, RdbSetup driver, String schema) {
		super(con, driver, schema);
	}

	@Override
	public DbAccessType getHandleType() {
		return HANDLE_TYPE;
	}

	@Override
	public int execute(String sql, Value[] values, boolean treatSqlErrorAsNoAction) {
		this.checkActive();
		if (this.dbDriver.logSqls) {
			RdbUtil.traceSql(sql, values);
		}
		int result = 0;
		try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
			setPreparedStatementParams(stmt, values);
			result = stmt.executeUpdate();
		} catch (SQLException e) {
			if (treatSqlErrorAsNoAction) {
				logger.info("SQLException code:" + e.getErrorCode() + " message :" + e.getMessage()
						+ " is treated as zero rows affected.");
			} else {
				throw new ApplicationError(e, "Sql Error while executing sql ");
			}
		}

		if (result < 0) {
			logger.info("Number of affected rows is not reliable as we got it as " + result);
		} else {
			logger.info(result + " rows affected.");
		}
		return result;
	}

	@Override
	public int[] executeBatch(String sql, Value[][] values, boolean treatSqlErrorAsNoAction) {
		this.checkActive();
		if (this.dbDriver.logSqls) {
			RdbUtil.traceBatchSql(sql, values);
		}
		int[] result = new int[0];
		try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
			for (Value[] row : values) {
				setPreparedStatementParams(stmt, row);
				stmt.addBatch();
			}
			result = stmt.executeBatch();
		} catch (SQLException e) {
			if (treatSqlErrorAsNoAction) {

				logger.info("SQLException code:" + e.getErrorCode() + " message :" + e.getMessage()
						+ " is treated as zero rows affected.");

			} else {
				throw new ApplicationError(e, "Sql Error while executing batch ");
			}
		}

		int rows = 0;
		for (int j : result) {
			if (j < 0) {
				rows = j;
			} else if (rows >= 0) {
				rows += j;
			}
		}
		if (rows < 0) {
			logger.info("Number of affected rows is not reliable as we got it as " + rows);
		} else {
			logger.info(rows + " rows affected.");
		}
		return result;
	}

	@Override
	public int insertAndGetKeys(String sql, Value[] values, long[] generatedKeys, String[] keyNames,
			boolean treatSqlErrorAsNoAction) {
		this.checkActive();
		if (this.dbDriver.logSqls) {
			RdbUtil.traceSql(sql, values);
		}
		int result = 0;
		try (PreparedStatement stmt = this.connection.prepareStatement(sql, keyNames)) {
			setPreparedStatementParams(stmt, values);
			result = stmt.executeUpdate();
			if (result > 0) {
				this.getGeneratedKeys(stmt, generatedKeys);
			}
		} catch (SQLException e) {
			if (treatSqlErrorAsNoAction) {
				logger.info("SQLException code:" + e.getErrorCode() + " message :" + e.getMessage()
						+ " is treated as zero rows affected.");
			} else {
				throw new ApplicationError(e, "Sql Error while executing sql ");
			}
		}

		if (result < 0) {
			logger.info("Number of affected rows is not reliable as we got it as " + result);
		} else {
			logger.info(result + " rows affected.");
		}
		return result;
	}

	/**
	 * extract generated keys into the array
	 *
	 * @param stmt
	 * @param generatedKeys
	 * @throws SQLException
	 */
	private void getGeneratedKeys(Statement stmt, long[] generatedKeys) throws SQLException {

		ResultSet rs = stmt.getGeneratedKeys();
		for (int i = 0; i < generatedKeys.length && rs.next(); i++) {
			generatedKeys[i] = rs.getLong(1);
		}
		rs.close();
	}

	@Override
	public int executeSp(String sql, IFieldsCollection inputFields, IFieldsCollection outputFields,
			ProcedureParameter[] params, IDataSheet[] outputSheets, ServiceContext ctx) {

		return super.readUsingStoredProcedure(sql, inputFields, outputFields, params, outputSheets, ctx);
	}
}
