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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.simplity.core.ApplicationError;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.idb.ArrayCreator;
import org.simplity.core.idb.DbAccessType;
import org.simplity.core.idb.IDbClient;
import org.simplity.core.idb.IDbDriver;
import org.simplity.core.idb.StructCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper on JDBC driver to implement IDbDriver
 *
 * @author simplity.org
 *
 */
public class RdbDriver implements IDbDriver {
	private static final Logger logger = LoggerFactory.getLogger(IDbDriver.class);
	/*
	 * for sql escaping
	 */
	private static final String OUR_ESCAPE_CHAR = "!";
	private static final String OUR_ESCAPE_STR = "!!";

	private static final String CONTEXT_PREFIX = "java:/comp/env/";

	private static RdbDriver defaultDriver;

	/**
	 * @return an instance of the default driver. null if db is not set-up
	 */
	public static IDbDriver getDefaultDriver() {
		return defaultDriver;
	}

	/*
	 * static code for set up
	 */
	/**
	 * should the sqls that are executed be added to the log?? Required during
	 * development. Some corporate have security policy that requires you not
	 * log sqls in production
	 */
	boolean logSqls;
	/** The database vendor we are using */
	DbVendor dbVendor;

	/** The database driver to use */
	@FieldMetaData(relevantBasedOnField = "dbVendor")
	String dbDriverClassName;

	/**
	 * for connecting to data base, we either use connection string with driver
	 * class name, or use dataSource. Connection string overrides.
	 */
	@FieldMetaData(leaderField = "dbDriverClassName")
	String connectionString;

	/** Data source name to be used to look-up in JNDI for dataSource */
	@FieldMetaData(relevantBasedOnField = "dbVendor", irrelevantBasedOnField = "dbDriverClassName")
	String dataSourceName;

	/**
	 * Some projects use multiple schema. In such a case, it is possible that a
	 * given service may use a schema other than the default. We have optimized
	 * the design for a frequently used default schema that is set for the user,
	 * and a few services use their own schema. Provide such schema with
	 * dataSource/connection
	 */
	@FieldMetaData(relevantBasedOnField = "dbVendor")
	SchemaDetail[] schemaDetails;

	private Map<String, DataSource> otherDataSources = null;
	private Map<String, String> otherConStrings = null;

	/*
	 * RDBMS vendor dependent settings. set based on db vendor
	 */
	private String timeStampFn;
	private String[] charsToEscapeForLike;
	private String defaultSchema;
	private DataSource dataSourceObject;
	private ArrayCreator arrayCreator;
	private StructCreator structCreator;

	@Override
	public String getTimeStampFn() {
		return this.timeStampFn;
	}

	@Override
	public void accessDb(IDbClient dbClient, DbAccessType accessType, String schema) {
		try (Connection con = this.createConnection(schema)) {
			AbstractHandle dbHandle = null;
			switch (accessType) {
			case AUTO_COMMIT:
				dbHandle = new AutoCommitHandle(con, this, schema);
				con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
				con.setAutoCommit(true);
				break;

			case META_DATA:
				con.setReadOnly(true);
				dbHandle = new MetadataHandle(con, this, schema);
				break;

			case MULTI_TRANS:
				con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
				con.setAutoCommit(false);
				dbHandle = new MultiTransHandle(con, this, schema);
				break;

			case READ_ONLY:
				con.setReadOnly(true);
				dbHandle = new ReadonlyHandle(con, this, schema);
				break;

			case SINGLE_TRANS:
				con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
				con.setAutoCommit(false);
				dbHandle = new TransactionHandle(con, this, schema);
				break;

			default:
				throw new ApplicationError("Design error: DbAccessType " + accessType + " is not handled");
			}

			boolean allOk = dbClient.accessDb(dbHandle);
			if (accessType == DbAccessType.SINGLE_TRANS || accessType == DbAccessType.MULTI_TRANS) {
				if (allOk) {
					con.commit();
				} else {
					con.rollback();
				}
			}
			dbHandle.close();

		} catch (SQLException e) {
			throw new ApplicationError(e, "");
		}
	}

	@Override
	public String escapeForLike(String text) {
		String result = text.replaceAll(OUR_ESCAPE_CHAR, OUR_ESCAPE_STR);
		for (String s : this.charsToEscapeForLike) {
			result = result.replace(s, OUR_ESCAPE_CHAR + s);
		}
		return result;
	}

	@Override
	public boolean isSchemaDefined(String schema) {
		if (schema == null) {
			return false;
		}
		String sn = schema.toUpperCase();
		if (sn.equals(this.defaultSchema)) {
			return true;
		}
		if (this.dataSourceObject != null) {
			if (this.otherDataSources != null && this.otherDataSources.containsKey(sn)) {
				return true;
			}
			return false;
		}
		if (this.otherConStrings != null && this.otherConStrings.containsKey(sn)) {
			return true;
		}
		return false;
	}

	@Override
	public String getDefaultSchema() {
		return this.defaultSchema;
	}

	@Override
	public DbVendor getDbVendor() {
		return this.dbVendor;
	}

	@Override
	public boolean sequenceGeneratorRequired() {
		return this.dbVendor == DbVendor.ORACLE;
	}

	/**
	 * get a connection to the db
	 *
	 * @param schema
	 * @return connection
	 * @throws SQLException
	 */
	private Connection createConnection(String schema) throws SQLException {
		/*
		 * set sch to an upper-cased schema, but only if it is non-null and
		 * different from default schema
		 */
		String sch = null;
		if (schema != null) {
			sch = schema.toUpperCase();
			if (sch.equals(this.defaultSchema)) {
				logger.info(
						"service is asking for schema " + schema + " but that is the default. default connection used");
				sch = null;
			} else {
				logger.info("Going to open a non-default connection for schema " + schema);
			}
		}
		if (this.dataSourceObject != null) {
			/*
			 * this application uses dataSource
			 */
			if (sch == null) {
				return this.dataSourceObject.getConnection();
			}
			/*
			 * this service is using a different schema
			 */
			DataSource ds = this.otherDataSources.get(sch);
			if (ds == null) {
				throw new ApplicationError("No dataSource configured for schema " + sch);
			}
			return ds.getConnection();
		}
		/*
		 * old-fashioned application :-(
		 */
		if (this.connectionString == null) {
			throw new ApplicationError("Database should be initialized properly before any operation can be done.");
		}
		if (sch == null) {
			return DriverManager.getConnection(this.connectionString);
		}
		/*
		 * service uses a non-default schema
		 */
		String conString = this.otherConStrings.get(sch);
		if (conString == null) {
			throw new ApplicationError("No connection string configured for schema " + sch);
		}
		return DriverManager.getConnection(conString);
	}

	/**
	 * validate all field values of this as a component
	 *
	 * @param vtx
	 *            validation context
	 */
	public void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
	}

	/**
	 * @return error message in case of trouble. null if all OK.
	 */
	public String setup() {
		if (defaultDriver != null) {
			logger.warn("Existing driver is going to be replaced with a new one...");
		}
		if (this.dbVendor == null) {
			logger.info(
					"This Application has not set dbVendor. We assume that the application does not require any db connection.");
			if (this.dataSourceName != null || this.dbDriverClassName != null || this.connectionString != null
					|| this.schemaDetails != null) {
				logger.info("WARNING: Since dbVendor is not set, we ignore other db related settings.");
			}
			return null;
		}

		this.setVendorParams();
		/*
		 * use data source if specified
		 */
		String msg = null;
		if (this.dataSourceName != null) {
			msg = this.setDataSource();
		} else {
			msg = this.setupConnection();
		}
		if (msg == null) {
			logger.info("Driver class name " + this.dbDriverClassName + " invoked successfully");
			defaultDriver = this;
			return null;
		}

		logger.error("DB Driver set up failed. No db operation is possible");
		return msg;
	}

	private String setupConnection() {
		/*
		 * connection string
		 */
		if (this.dbDriverClassName == null) {
			return "No dataSource or driverClassName specified. If you do not need db connection, do not set dbVendor attribute.";
		}

		if (this.connectionString == null) {
			return "driveClassName is specified but connection string is missing in your application set up.";
		}
		try {
			Class.forName(this.dbDriverClassName);
		} catch (Exception e) {
			return "Could not use class " + this.dbDriverClassName + " as driver class name.";
		}

		String msg = this.checkConnectionString(this.connectionString, true);
		if (msg != null) {
			return msg;
		}

		if (this.schemaDetails == null) {
			return null;
		}
		this.otherConStrings = new HashMap<String, String>();

		for (SchemaDetail sd : this.schemaDetails) {
			String sch = sd.getSchemaName();
			String conStr = sd.getConnectionString();
			if (sch == null || conStr == null) {
				logger.error("schemaName and connectionString are required for mutli-schema operation. Entry skipped");
				continue;
			}

			if (sd.getDataSourceName() != null) {
				logger.info("Warning: This application uses connection string, and hence dataSource for schema "
						+ sd.getDataSourceName() + " ignored");
			}
			sch = sch.toUpperCase();
			if (this.checkConnectionString(conStr, false) == null) {
				this.otherConStrings.put(sch, conStr);
			} else {
				logger.error("Connection string for schema {} could not beused to get a connection. Entry skipped",
						sch);
			}
		}
		this.structCreator = this.dbVendor.getStructCreator();
		this.arrayCreator = this.dbVendor.getArrayCreator();
		return null;
	}

	private String checkConnectionString(String str, boolean setDefaultSchema) {
		try (Connection con = DriverManager.getConnection(str)) {
			if (setDefaultSchema) {
				this.setDefaultSchema(con);
				if (this.defaultSchema == null) {
					return "Unable to get default schema for this connection";
				}
			}
			return null;
		} catch (Exception e) {
			return "We encounutered an invalid connection string. Plese check db set up.";
		}
	}

	/**
	 * set parameters that depend on the selected vendor
	 *
	 * @param vendor
	 */
	private void setVendorParams() {
		logger.info("dbVendor is set to " + this.dbVendor);
		char[] chars = this.dbVendor.getEscapesForLike();
		this.charsToEscapeForLike = new String[chars.length];

		for (int i = 0; i < chars.length; i++) {
			this.charsToEscapeForLike[i] = chars[i] + "";
		}

		this.timeStampFn = this.dbVendor.getTimeStamp();
	}

	private String setDataSource() {
		if (this.dbDriverClassName != null || this.connectionString != null) {
			logger.info(
					"WARNING: Since dataSourceName is specified, we ignore driverClassName and connectionString attributes");
		}

		this.dataSourceObject = this.getDataSource(this.dataSourceName);
		if (this.dataSourceObject == null) {
			return this.dataSourceName + " could not be used as data source name for data base setup";
		}
		if (this.schemaDetails == null) {
			return null;
		}

		this.otherDataSources = new HashMap<String, DataSource>();
		String msg = null;
		for (SchemaDetail sd : this.schemaDetails) {
			String sch = sd.getSchemaName();
			String src = sd.getDataSourceName();
			String csr = sd.getConnectionString();
			DataSource source = null;
			if (sch == null || src == null) {
				logger.error("schemaName and dataSourceName are required for mutli-schema operation");
			} else {
				if (csr != null) {
					logger.info(
							"Warning : This application uses data source, and hence connection string for schema "
									+ csr + " ignored");
					sch = sch.toUpperCase();
					source = this.getDataSource(sch);
				}
			}
			if (source == null) {
				msg = "At least one entry in schemaDetails in invalid";
			} else {
				this.otherDataSources.put(sch, source);
			}
		}
		return msg;
	}

	private DataSource getDataSource(String sourceName) {
		Object obj = null;
		String msg = null;
		try {
			obj = new InitialContext().lookup(sourceName);
		} catch (Exception e) {
			msg = e.getMessage();
		}
		if (obj == null && sourceName.startsWith(CONTEXT_PREFIX) == false) {
			try {
				obj = new InitialContext().lookup(CONTEXT_PREFIX + sourceName);
			} catch (Exception e) {
				msg = e.getMessage();
			}
		}
		if (obj == null) {
			logger.error("Error while using data source name {}. error: {}", sourceName, msg);
			return null;
		}
		if (obj instanceof DataSource == false) {
			logger.error("data source for name {} is an instance of {} but it shoudl be an instance of {} ", sourceName,
					obj.getClass().getName(), DataSource.class.getName());
			return null;
		}
		DataSource ds = (DataSource) obj;
		try (Connection con = ds.getConnection()) {
			//
		} catch (SQLException e) {
			logger.error(e.getMessage());
			return null;
		}
		return ds;
	}

	private void setDefaultSchema(Connection con) {
		String schema = null;
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery(this.dbVendor.getGetSchemaSql());
			try (ResultSet rs = stmt.getResultSet()) {

				if (!rs.next()) {
					logger.error(
							"data base returned no result for sql " + this.dbVendor.getGetSchemaSql());
					return;
				}
				schema = rs.getString(1);
				if (rs.wasNull()) {
					logger.error("data base returned null as default schema.");
					return;
				}
				this.defaultSchema = schema.toUpperCase();
				return;
			}
		} catch (SQLException e) {
			logger.error("Error while getting default schema for this db connection. {}", e.getMessage());
		}
	}

	@Override
	public StructCreator getStructCreator() {
		return this.structCreator;
	}

	@Override
	public ArrayCreator getArrayCreator() {
		return this.arrayCreator;
	}
}
