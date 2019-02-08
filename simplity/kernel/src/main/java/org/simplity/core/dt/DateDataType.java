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
package org.simplity.core.dt;

import java.util.Date;

import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.util.DateUtil;
import org.simplity.core.value.DateValue;
import org.simplity.core.value.InvalidValueException;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author simplity.org */
public class DateDataType extends DataType {
	private static final Logger logger = LoggerFactory.getLogger(DateDataType.class);

	/** do we keep time as well? */
	boolean hasTime;

	/**
	 * how far back into past is this date valid? -n means a min of n days into
	 * future (as if minDaysIntoFuture = n)
	 */
	int maxDaysIntoPast = Integer.MAX_VALUE;
	/**
	 * how far into future can this date go? In case the date has to have a min
	 * days into past, use -n here
	 */
	int maxDaysIntoFuture = Integer.MAX_VALUE;

	@Override
	public Value validateValue(Value value) {
		if (value.getValueType() != ValueType.DATE) {
			return null;
		}
		/*
		 * If this is just date, then we assume that the milli-second represents
		 * the date in UTC
		 */
		long date = ((DateValue) value).getDate();
		int nbrDays = DateUtil.daysFromToday(date);
		if (nbrDays >= 0) {
			/*
			 * it is a date into the future
			 */
			if (nbrDays > this.maxDaysIntoFuture) {
				/*
				 * it is too far into future
				 */
				return null;
			}
			if (this.maxDaysIntoPast < 0 && nbrDays < -this.maxDaysIntoPast) {
				/*
				 * it should have been farther into future
				 */
				return null;
			}
		} else {
			/*
			 * it is a date in the past
			 */
			nbrDays = -nbrDays;
			if (nbrDays > this.maxDaysIntoPast) {
				/*
				 * it is too far into the past
				 */
				return null;
			}
			if (this.maxDaysIntoFuture < 0 && nbrDays < -this.maxDaysIntoFuture) {
				/*
				 * it should have been farther into past
				 */
				return null;
			}
		}

		if (this.hasTime) {
			return value;
		}

		return Value.newDateValue(DateUtil.trimDate(date));
	}

	@Override
	public ValueType getValueType() {
		return ValueType.DATE;
	}

	@Override
	public int getMaxLength() {
		return 15;
	}

	/**
	 * does this include time also
	 *
	 * @return true if this contains time, false otherwise
	 */
	public boolean includesTime() {
		return this.hasTime;
	}

	@Override
	protected void validateSpecific(IValidationContext vtx) {
		if (this.maxDaysIntoFuture != Integer.MAX_VALUE && this.maxDaysIntoPast != Integer.MAX_VALUE) {
			if (-this.maxDaysIntoPast > this.maxDaysIntoFuture) {
				String msg = "Invalid date range. Earliest possible date is specified to be after the latest possibe date.";
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, msg, "maxDaysIntoPast"));
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, msg, "maxDaysIntoFuture"));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.dt.DataType#synthesiseDscription()
	 */
	@Override
	protected String synthesiseDscription() {
		Date minDate = null;
		Date maxDate = null;
		Date date = new Date();
		if (this.maxDaysIntoFuture != Integer.MAX_VALUE) {
			maxDate = DateUtil.addDays(date, this.maxDaysIntoFuture);
		}
		if (this.maxDaysIntoPast != Integer.MAX_VALUE) {
			minDate = DateUtil.addDays(date, -this.maxDaysIntoPast);
		}
		if (minDate != null) {
			if (maxDate != null) {
				return "Expecting a date between "
						+ DateUtil.formatDate(minDate)
						+ " and "
						+ DateUtil.formatDate(maxDate);
			}
			return "Expecting a date after " + DateUtil.formatDate(minDate);
		}
		if (maxDate != null) {
			return "Expecting a date before " + DateUtil.formatDate(maxDate);
		}
		return "Expecting a valid date";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.dt.DataType#formtValue(org.simplity.kernel.value.
	 * Value)
	 */
	@Override
	public String formatVal(Value value) {
		Date date;
		try {
			date = value.toDate();
		} catch (InvalidValueException e) {
			logger.error("Value of type " + value.getValueType() + " passed for frmatting as date.");
			return "";
		}
		return DateUtil.formatDateTime(date);
	}
}
