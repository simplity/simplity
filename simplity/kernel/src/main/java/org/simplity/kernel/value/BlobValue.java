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
package org.simplity.kernel.value;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.file.FileManager;

/**
 * represents a BLOB as defined in an RDBMS.
 *
 * @author simplity.org
 */
public class BlobValue extends TextValue {
	/** */
	private static final long serialVersionUID = 1L;

	BlobValue() {
		super();
	}

	/**
	 * @param key
	 */
	BlobValue(String key) {
		super(key);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.value.TextValue#getValueType()
	 */
	@Override
	public ValueType getValueType() {
		return ValueType.BLOB;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.value.TextValue#setToStatement(java.sql.
	 * PreparedStatement , int)
	 */
	@Override
	public void setToStatement(PreparedStatement statement, int idx) throws SQLException {
		Blob blob = null;
		if (!this.valueIsNull) {
			blob = this.getBlob(statement.getConnection());
		}
		if (blob == null) {
			statement.setNull(idx, Types.BLOB);
		} else {
			statement.setBlob(idx, blob);
		}
	}

	private Blob getBlob(Connection con) throws SQLException {
		File file = FileManager.getTempFile(this.value);
		if (file == null) {

			logger.info("Unable to get temp file content for key {} RDBMS will have null for this Blob.", this.value);

			return null;
		}

		logger.info("Got file {} of size {} ", file.getPath(), file.length());

		InputStream in = null;
		OutputStream out = null;
		Blob blob = null;
		try {
			in = new FileInputStream(file);
			blob = con.createBlob();
			out = blob.setBinaryStream(1);
			FileManager.copyOut(in, out);
		} catch (Exception e) {
			throw new ApplicationError(e, "error while setting Blob using key " + this.value);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception ignore) {
					//
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
		/*
		 * we want to do this after closing all streams
		 */

		logger.info("We created a blob of length {}", blob.length());

		return blob;
	}
}
