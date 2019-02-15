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

package org.simplity.core.adapter;

import java.util.Date;

import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date field may require some adjustments like trimming of time component.
 * Hence a specialized field over and above <code>PrimitiveField</code>
 *
 * @author simplity.org
 *
 */
public class DateField extends AbstractField {
	private static final Logger logger = LoggerFactory.getLogger(DateField.class);

	/**
	 * null if no default. default for date in business is generally relative to
	 * current date. Specify number of days from today. 0 for today, and -ve
	 * number for past date.
	 */
	String defaultNbrDaysFromToday;
	/**
	 * is this a pure date, in which case we have to strip time from it.
	 */
	boolean stripTimeComponent;

	private int nbrDays;

	@Override
	public void copy(IDataSource source, IDataTarget target, ServiceContext ctx) {
		Date date = source.getDateValue(this.fromName);
		if (date == null) {
			date = this.getDefaultDate();
			if (date == null) {
				logger.info("Source has no value date field {} ", this.fromName);
				return;
			}
		}
		if (this.stripTimeComponent) {
			date = new Date(DateUtil.trimDate(date.getTime()));
		}
		target.setDateValue(this.toName, date);
	}

	private Date getDefaultDate() {
		if (this.defaultNbrDaysFromToday == null) {
			return null;
		}
		Date date = DateUtil.getToday();
		if (this.nbrDays != 0) {
			date = DateUtil.addDays(date, this.nbrDays);
		}
		return date;
	}

	@Override
	public void getReady() {
		super.getReady();
		if (this.defaultNbrDaysFromToday != null) {
			try {
				this.nbrDays = Integer.parseInt(this.defaultNbrDaysFromToday);
			} catch (Exception e) {
				logger.error("defaultNbrDaysFromToday has to be an integer but we found {} for field {}",
						this.defaultNbrDaysFromToday, this.fromName);
			}
		}
	}

	@Override
	protected void validate(IValidationContext vtx) {
		super.validate(vtx);
		if (this.defaultNbrDaysFromToday != null) {
			try {
				this.nbrDays = Integer.parseInt(this.defaultNbrDaysFromToday);
			} catch (Exception e) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"defaultNbrDaysFromToday has to be an integer", "defaultNbrDaysFromToday"));
			}
		}
	}
}
