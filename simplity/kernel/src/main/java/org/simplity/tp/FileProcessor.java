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

package org.simplity.tp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public class FileProcessor extends Block {
	/**
	 * folder in which we look for files to process
	 */
	String inFolderName;
	/**
	 * example *.txt
	 */
	String fileNamePattern;
	/**
	 * example .bak. Note that we do not remove existing extension, but append
	 * it
	 */
	String archivalExtension;
	/**
	 * record that describes the structure of this file
	 */
	String recordName;

	/**
	 * if we expect multiple rows of this record, or if you want the single row
	 * to be extracted to a data sheet. If this is not specified, we extract the
	 * first/only row as fields into service context
	 */
	String sheetName;

	/**
	 * Are there end-of-line markers in this file (could be LF or CR/LF )
	 */
	boolean fileHasEndOfLineChars;
	/**
	 * filter corresponding to the input file
	 */
	private FilenameFilter filter;

	private File inbox;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#getReady(int)
	 */
	@Override
	public void getReady(int idx) {
		super.getReady(idx);
		this.filter = TextUtil.getFIleNameFilter(this.fileNamePattern);
		this.inbox = new File(this.inFolderName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#delegate(org.simplity.service.ServiceContext,
	 * org.simplity.kernel.db.DbDriver)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		Tracer.trace("GOing to process files in folder " + this.inFolderName
				+ " that exists = " + this.inbox.exists());
		int nbrFiles = 0;
		for (File file : this.inbox.listFiles(this.filter)) {
			Tracer.trace("File " + file.getAbsolutePath());
			if (this.processOneFile(file, ctx, driver)) {
				nbrFiles++;
			}
		}
		return Value.newIntegerValue(nbrFiles);
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	private boolean processOneFile(File file, ServiceContext ctx,
			DbDriver driver) {
		BufferedReader reader = null;
		try {
			Tracer.trace("Processing " + file.getAbsolutePath() + "....");
			Record record = ComponentManager.getRecord(this.recordName);
			FileInputStream ins = new FileInputStream(file);
			reader = new BufferedReader(new InputStreamReader(ins));
			List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
			DataSheet ds = record.fromFlatFile(reader, errors,
					this.fileHasEndOfLineChars);
			reader.close();

			if (errors.size() > 0) {
				ctx.addMessages(errors);
				return false;
			}

			ctx.putDataSheet(this.sheetName, ds);
			BlockWorker worker = new BlockWorker(this.actions,
					this.indexedActions, ctx);
			worker.execute(driver);
			Path path = Paths.get(file.getAbsolutePath());
			Files.move(path, path.resolveSibling(file.getName() + ".bak"));
			return true;
		} catch (Exception e) {
			Tracer.trace("Error while processing file " + file.getName() + ". "
					+ e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

}