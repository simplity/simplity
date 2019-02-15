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
package org.simplity.core.value;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.simplity.core.ApplicationError;
import org.simplity.core.file.FileManager;

/**
 * represents a CLOB as defined in an RDBMS.
 *
 * @author simplity.org
 */
public class ClobValue extends TextValue {

	/** */
	private static final long serialVersionUID = 1L;

	ClobValue() {
		super();
	}

	/**
	 * @param key
	 */
	ClobValue(String key) {
		super(key);
	}

	@Override
	public ValueType getValueType() {
		return ValueType.CLOB;
	}

	@Override
	public void setToStatement(PreparedStatement statement, int idx) throws SQLException {
		Clob clob = null;
		if (this.valueIsNull == false) {
			clob = this.getClob(statement.getConnection());
		}
		if (clob == null) {
			statement.setNull(idx, Types.CLOB);
		} else {
			statement.setClob(idx, clob);
		}
	}

	/**
	 * Create a Clob
	 *
	 * @param con
	 * @return
	 */
	private Clob getClob(Connection con) {
		File file = FileManager.getTempFile(this.value);
		if (file == null) {
			return null;
		}
		Writer writer = null;
		Reader reader = null;
		Clob clob = null;
		try {
			reader = new FileReader(file);
			clob = con.createClob();
			writer = clob.setCharacterStream(1);
			int c;
			while ((c = reader.read()) != -1) {
				writer.write(c);
			}
		} catch (Exception e) {
			throw new ApplicationError(e, "error while setting Blob using key " + this.value);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception ignore) {
					//
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
		return clob;
	}
}
