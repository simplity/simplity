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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.core.trans;

import java.util.Map;
import java.util.Set;

import org.simplity.core.data.IDataSheet;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.util.JsonUtil;
import org.simplity.core.value.Value;
import org.simplity.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * show values of fields/table : meant for debugging
 *
 * @author simplity.org
 */
public class Log extends AbstractNonDbAction {
	private static final Logger logger = LoggerFactory.getLogger(Log.class);

	/** field/table names to be logged */
	String[] names;

	boolean setMDC;

	@Override
	protected boolean act(ServiceContext ctx) {
		if (this.names == null) {
			return false;
		}
		if (this.names[0].equals("*")) {
			this.logAll(ctx);
			return true;
		}
		logger.info("Values at log action " + this.getName());

		boolean found = false;
		for (String nam : this.names) {
			if (ctx.hasValue(nam)) {
				found = true;
				logger.info(nam + " = " + ctx.getValue(nam));

				if (this.setMDC) {
					MDC.put(nam, ctx.getValue(nam).toText());
				}
			}
			if (ctx.hasDataSheet(nam)) {
				found = true;
				ctx.getDataSheet(nam).trace();
			}
			if (!found) {
				logger.info(nam + " is not a field or sheet name");
			}
		}
		return true;
	}

	private void logAll(ServiceContext ctx) {
		for (Map.Entry<String, Value> entry : ctx.getAllFields()) {
			logger.info(entry.getKey() + " = " + entry.getValue());
		}
		JSONWriter writer = new JSONWriter();
		writer.object();

		Set<Map.Entry<String, IDataSheet>> sheets = ctx.getAllSheets();
		if (sheets.size() == 0) {
			logger.info("There are no data sheets");
			return;
		}

		logger.info("Data sheets provided as a json, so that you can copy-paste into a display utility");
		for (Map.Entry<String, IDataSheet> entry : sheets) {
			writer.key(entry.getKey());
			JsonUtil.sheetToJson(writer, entry.getValue(), false);
		}
		logger.info(writer.toString());
	}
}
