/*
 * Copyright (c) 2019 simplity.org
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

package org.simplity.core.dm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.simplity.core.app.Application;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.dm.field.DbField;
import org.simplity.core.dm.field.Field;
import org.simplity.core.idb.DbAccessType;
import org.simplity.core.idb.IDbClient;
import org.simplity.core.idb.IDbDriver;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.idb.IMetadataHandle;
import org.simplity.core.rdb.MetadataHandle;
import org.simplity.core.rdb.RdbDriver;
import org.simplity.core.util.RdbUtil;
import org.simplity.core.util.XmlUtil;
import org.simplity.core.value.BooleanValue;
import org.simplity.core.value.IntegerValue;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public class DbRecordGenerator {
	protected static final Logger logger = LoggerFactory.getLogger(DbRecordGenerator.class);

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		Application.runInsideApp("c:/repos/lms/src/main/resources/res/", new Application.AppRunnable() {
			@Override
			public void runInsideApp() {
				File folder = new File("c:/repos/lms/recs");
				if (folder.exists() == false) {
					folder.mkdirs();
				}
				createAllRecords(folder, DbToJavaNameConversion.CAMEL_CASE);
			}
		});
	}

	/**
	 * crates a default record component for a table from rdbms
	 *
	 * @param schemaName
	 *            null to use default schema. non-null to use that specific
	 *            schema that this table belongs to
	 * @param qualifiedName
	 *            like modulename.recordName
	 * @param tableName
	 *            as in rdbms
	 * @param conversion
	 *            how field names are to be derived from externalName
	 * @param isView
	 *            is this a view else it is a table
	 * @return default record component for a table from rdbms
	 */
	public static DbTable createFromTable(String schemaName, String qualifiedName, String tableName,
			DbToJavaNameConversion conversion, boolean isView) {
		IDbDriver driver = RdbDriver.getDefaultDriver();
		if (driver == null) {
			logger.error("No active db driver to get table details from. returning null record");
			return null;
		}

		RecordCreator reader = new RecordCreator(schemaName, qualifiedName, tableName, conversion, isView);
		driver.accessDb(reader, DbAccessType.META_DATA, schemaName);
		return reader.getResult();
	}

	/**
	 * generate and save draft record.xmls for all tables and views in the rdbms
	 *
	 * @param folder
	 *            where record.xmls are to be saved. Should be a valid folder.
	 *            Created if the path is valid but folder does not exist. since
	 *            we replace any existing file, we recommend that you call with
	 *            a new folder, and then do file copying if required
	 * @param conversion
	 *            how do we form record/field names table/column
	 * @return number of records saved
	 */
	public static int createAllRecords(File folder, DbToJavaNameConversion conversion) {
		IDbDriver driver = RdbDriver.getDefaultDriver();
		if (driver == null) {
			logger.error("No active db driver to get table details from. returning null record");
			return 0;
		}

		AllRecordsCreator reader = new AllRecordsCreator(folder, conversion);
		driver.accessDb(reader, DbAccessType.META_DATA, null);
		return reader.getResult();
	}

	/**
	 * need this functionality in a class because the dbDriver insists on
	 * calling us back with connection/handle, rather than giving us
	 * connection/handle directly
	 *
	 * @author simplity.org
	 *
	 */
	private static class RecordCreator implements IDbClient {

		private DbTable result;
		private final String schemaName;
		private final String qualifiedName;
		private final String tableName;
		private final DbToJavaNameConversion conversion;
		private final boolean isView;

		RecordCreator(String schemaName, String qualifiedName, String tableName,
				DbToJavaNameConversion conversion, boolean isView) {
			this.schemaName = schemaName;
			this.qualifiedName = qualifiedName;
			this.tableName = tableName;
			this.conversion = conversion;
			this.isView = isView;
		}

		@Override
		public boolean accessDb(IDbHandle handle) {
			MetadataHandle dbHandle = (MetadataHandle) handle;
			IDataSheet columns = dbHandle.getTableColumns(this.tableName);
			if (columns == null) {
				String msg = "No table in db with name " + this.tableName;
				if (this.schemaName != null) {
					msg += " in schema " + this.schemaName;
				}
				logger.info(msg);
				return false;
			}
			DbTable record = this.isView ? new DbView() : new DbTable();
			record.name = this.qualifiedName;
			int idx = this.qualifiedName.lastIndexOf('.');
			if (idx != -1) {
				record.name = this.qualifiedName.substring(idx + 1);
				record.moduleName = this.qualifiedName.substring(0, idx);
			}
			record.tableName = this.tableName;

			int nbrCols = columns.length();
			Field[] fields = new Field[nbrCols];
			for (int i = 0; i < fields.length; i++) {
				Value[] row = columns.getRow(i);

				String extern = row[2].toText();
				String name = extern;
				if (this.conversion != null) {
					name = this.conversion.toJavaName(extern);
				}

				int sqlType = (int) ((IntegerValue) row[3]).getLong();
				ValueType vt = RdbUtil.sqlTypeToValueType(sqlType);
				int len = 0;

				if (row[5] != null) {
					len = (int) ((IntegerValue) row[5]).getLong();
				}

				int nbrDecimals = 0;
				if (row[6] != null) {
					nbrDecimals = (int) ((IntegerValue) row[6]).getLong();
				}

				String desc = "" + vt + " len=" + len;
				if (nbrDecimals != 0) {
					desc += "." + nbrDecimals;
				}
				boolean nullable = ((BooleanValue) row[8]).getBoolean();
				fields[i] = new DbField(name, extern, desc, vt, nullable);
			}
			record.fields = fields;
			this.result = record;
			return true;
		}

		public DbTable getResult() {
			return this.result;
		}
	}

	private static class AllRecordsCreator implements IDbClient {
		private int nbrTables = 0;
		private File folder;
		private DbToJavaNameConversion conversion;

		AllRecordsCreator(File folder, DbToJavaNameConversion conversion) {
			this.folder = folder;
			this.conversion = conversion;
		}

		@Override
		public boolean accessDb(IDbHandle handle) {
			IMetadataHandle dbHandle = (IMetadataHandle) handle;
			if (this.folder.exists() == false) {
				this.folder.mkdirs();
				logger.info("Folder created for path " + this.folder.getAbsolutePath());
			} else if (this.folder.isDirectory() == false) {
				logger.info(
						this.folder.getAbsolutePath() + " is a file but not a folder. Record generation abandoned.");
				return false;
			}
			String path = this.folder.getAbsolutePath() + '/';
			IDataSheet tables = dbHandle.getTables(null);
			if (tables == null) {
				logger.info("No tables in the db. Records not created.");
				return false;
			}

			logger.info("Found " + tables.length() + " tables for which we are going to create records.");
			String[][] rows = tables.getRawData();
			/*
			 * first row is header. Start from second row.
			 */
			for (int i = 1; i < rows.length; i++) {
				String[] row = rows[i];
				String schemaName = row[0];
				if (schemaName != null && schemaName.isEmpty()) {
					schemaName = null;
				}
				String tableName = row[1];
				String recordName = tableName;
				if (this.conversion != null) {
					recordName = this.conversion.toJavaName(tableName);
				}

				DbTable record = createFromTable(schemaName, recordName, tableName, this.conversion,
						row[2].equals("VIEW"));
				if (record == null) {
					logger.info("Record " + recordName + " could not be generated from table/view " + tableName);
					continue;
				}
				File file = new File(path + recordName + ".xml");
				OutputStream out = null;
				try {
					if (file.exists() == false) {
						file.createNewFile();
					}
					out = new FileOutputStream(file);
					if (XmlUtil.objectToXml(out, record)) {
						this.nbrTables++;
					}
				} catch (Exception e) {
					logger.error("Record " + recordName + " generated from table/view " + tableName
							+ " but could not be saved. ", e);
					continue;
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (Exception ignore) {
							//
						}
					}
				}
			}
			return true;
		}

		int getResult() {
			return this.nbrTables;
		}
	}
}
