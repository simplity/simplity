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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.simplity.core.ApplicationError;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.idb.DbAccessType;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.idb.IResultSetReader;
import org.simplity.core.idb.IRowConsumer;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.sql.ProcedureParameter;
import org.simplity.core.util.RdbUtil;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * @author simplity.org
 *
 */
public class ReadonlyHandle extends AbstractHandle implements IReadOnlyHandle {
	private static final String ERROR = "SQLException while extracting data using prepared statement";
	private static final DbAccessType HANDLE_TYPE = DbAccessType.READ_ONLY;

	/**
	 * @param con
	 * @param driver
	 */
	ReadonlyHandle(Connection con, RdbSetup driver, String schema) {
		super(con, driver, schema);
	}

	@Override
	public DbAccessType getHandleType() {
		return HANDLE_TYPE;
	}

	@Override
	public int readBatch(String sql, Value[][] values, IResultSetReader reader) {
		this.checkActive();
		if (this.dbDriver.logSqls) {
			RdbUtil.traceBatchSql(sql, values);
		}
		try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
			int nbr = 0;
			for (Value[] vals : values) {
				setPreparedStatementParams(stmt, vals);
				ResultSet rs = stmt.executeQuery();
				nbr += reader.read(rs);
				rs.close();
			}
			return nbr;
		} catch (SQLException e) {
			throw new ApplicationError(e, ERROR);
		}
	}

	@Override
	public int read(String sql, Value[] values, IResultSetReader reader) {
		this.checkActive();
		if (this.dbDriver.logSqls) {
			RdbUtil.traceSql(sql, values);
		}
		try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
			setPreparedStatementParams(stmt, values);
			ResultSet rs = stmt.executeQuery();
			int nbr = reader.read(rs);
			rs.close();
			return nbr;
		} catch (SQLException e) {
			throw new ApplicationError(e, ERROR);
		}
	}

	@Override
	public int readBatch(String sql, Value[][] values, ValueType[] outputTypes, IRowConsumer consumer) {
		return this.readBatch(sql, values, new IResultSetReader() {

			@Override
			public int read(ResultSet rs) {
				int nbr = 0;
				try {
					while (rs.next()) {
						consumer.consume(RdbUtil.resultToValueRow(rs, outputTypes));
						nbr++;
					}
					return nbr;
				} catch (SQLException e) {
					throw new ApplicationError(e, "");
				}
			}
		});
	}

	@Override
	public int read(String sql, Value[] values, ValueType[] outputTypes, IRowConsumer consumer) {
		return this.read(sql, values, new IResultSetReader() {

			@Override
			public int read(ResultSet rs) {
				try {
					int nbr = 0;
					while (rs.next()) {
						consumer.consume(RdbUtil.resultToValueRow(rs, outputTypes));
						nbr++;
					}
					return nbr;
				} catch (SQLException e) {
					throw new ApplicationError(e, "");
				}
			}
		});
	}

	@Override
	public int readBatch(String sql, Value[][] values, IDataSheet dataSheet) {
		return this.readBatch(sql, values, dataSheet.getValueTypes(), new IRowConsumer() {

			@Override
			public boolean consume(Value[] row) {
				dataSheet.addRow(row);
				return true;
			}
		});
	}

	@Override
	public int read(String sql, Value[] values, IDataSheet dataSheet) {
		return this.read(sql, values, dataSheet.getValueTypes(), new IRowConsumer() {

			@Override
			public boolean consume(Value[] row) {
				dataSheet.addRow(row);
				return true;
			}
		});
	}

	@Override
	public boolean hasData(String sql, Value[] values) {
		this.checkActive();
		if (this.dbDriver.logSqls) {
			RdbUtil.traceSql(sql, values);
		}
		try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
			setPreparedStatementParams(stmt, values);
			ResultSet rs = stmt.executeQuery();
			boolean result = rs.next();
			rs.close();
			return result;
		} catch (SQLException e) {
			throw new ApplicationError(e, ERROR);
		}
	}

	@Override
	public IDataSheet readBatchIntoSheet(String sql, Value[][] values) {
		this.checkActive();
		if (this.dbDriver.logSqls) {
			RdbUtil.traceBatchSql(sql, values);
		}
		IDataSheet sheet = null;
		ValueType[] types = null;
		try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
			for (Value[] vals : values) {
				setPreparedStatementParams(stmt, vals);
				ResultSet rs = stmt.executeQuery();
				if (sheet == null) {
					sheet = RdbUtil.getDataSheetForSqlResult(rs);
					types = sheet.getValueTypes();
				}
				while (rs.next()) {
					sheet.addRow(RdbUtil.resultToValueRow(rs, types));
				}
				rs.close();
			}
		} catch (SQLException e) {
			throw new ApplicationError(e, ERROR);
		}
		return sheet;
	}

	@Override
	public IDataSheet readIntoSheet(String sql, Value[] values) {
		this.checkActive();
		if (this.dbDriver.logSqls) {
			RdbUtil.traceSql(sql, values);
		}
		try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
			setPreparedStatementParams(stmt, values);
			ResultSet rs = stmt.executeQuery();
			IDataSheet sheet = RdbUtil.getDataSheetForSqlResult(rs);
			ValueType[] types = sheet.getValueTypes();
			while (rs.next()) {
				sheet.addRow(RdbUtil.resultToValueRow(rs, types));
			}
			rs.close();
			return sheet;
		} catch (SQLException e) {
			throw new ApplicationError(e, ERROR);
		}
	}

	@Override
	public int readUsingStoredProcedure(String sql, IFieldsCollection inputFields, IFieldsCollection outputFields,
			ProcedureParameter[] params, IDataSheet[] outputSheets, ServiceContext ctx) {
		int result = 0;
		SQLException err = null;
		try (CallableStatement stmt = this.connection.prepareCall(sql)) {
			if (params != null) {
				for (ProcedureParameter param : params) {
					/*
					 * programmers often make mistakes while defining
					 * parameters. Better to pin-point such errors
					 */
					try {
						if (param.setParameter(stmt, this, inputFields, ctx) == false) {
							logger.info("Error while setting " + param.getName() + " You will get an error.");
							/*
							 * issue in setting parameter. May be a mandatory
							 * field is not set
							 */
							return 0;
						}
					} catch (Exception e) {
						logger.info("Unable to set param " + param.getName() + " error : " + e.getMessage());
						param.reportError(e);
					}
				}
			}
			boolean hasResult = stmt.execute();
			int i = 0;
			if (outputSheets != null && hasResult) {
				int nbrSheets = outputSheets.length;
				while (hasResult) {
					if (i >= nbrSheets) {
						logger.info(
								"Stored procedure is ready to give more results, but the requester has supplied only "
										+ nbrSheets + " data sheets to read data into. Other data ignored.");
						break;
					}
					IDataSheet outputSheet = outputSheets[i];
					ValueType[] outputTypes = outputSheet.getValueTypes();
					ResultSet rs = stmt.getResultSet();
					while (rs.next()) {
						outputSheet.addRow(RdbUtil.resultToValueRow(rs, outputTypes));
						result++;
					}
					rs.close();
					i++;
					hasResult = stmt.getMoreResults();
				}
			}
			if (params != null) {
				for (ProcedureParameter param : params) {
					try {
						param.extractOutput(stmt, outputFields, ctx);
					} catch (Exception e) {
						param.reportError(e);
					}
				}
			}
		} catch (SQLException e) {
			err = e;
		}
		if (err != null) {
			throw new ApplicationError(err, "Sql Error while extracting data using stored procedure");
		}
		logger.info(result + " rows extracted.");

		if (result > 0) {
			return result;
		}
		if (outputFields != null) {
			return 1;
		}
		return 0;
	}
}
