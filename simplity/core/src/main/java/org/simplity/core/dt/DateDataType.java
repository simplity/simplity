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

import java.time.LocalDate;

import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.value.DateValue;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/** @author simplity.org */
public class DateDataType extends DataType {
	private static DateDataType defaultInstance = createDefault();

	private static DateDataType createDefault() {
		DateDataType dt = new DateDataType();
		dt.name = BuiltInDataTypes.DATE;
		return dt;
	}

	/**
	 *
	 * @return default DateDataType
	 */
	public static DateDataType getDefaultInstance() {
		return defaultInstance;
	}

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
		LocalDate date = ((DateValue) value).getDate();
		LocalDate today = LocalDate.now();
		if (date.isAfter(today.plusDays(this.maxDaysIntoFuture))
				|| date.isBefore(today.minusDays(this.maxDaysIntoPast))) {
			return null;
		}
		return value;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.DATE;
	}

	@Override
	public int getMaxLength() {
		return 15;
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

	@Override
	protected String synthesiseDscription() {
		LocalDate minDate = null;
		LocalDate maxDate = null;
		LocalDate date = LocalDate.now();
		if (this.maxDaysIntoFuture != Integer.MAX_VALUE) {
			maxDate = date.plusDays(this.maxDaysIntoFuture);
		}
		if (this.maxDaysIntoPast != Integer.MAX_VALUE) {
			minDate = date.minusDays(this.maxDaysIntoPast);
		}
		if (minDate != null) {
			if (maxDate != null) {
				return "Expecting a date between " + minDate + " and " + maxDate;
			}
			return "Expecting a date after " + minDate;
		}
		if (maxDate != null) {
			return "Expecting a date before " + maxDate;
		}
		return "Expecting a valid date";
	}
}
