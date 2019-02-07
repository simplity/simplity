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
package org.simplity.kernel.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * utilities for handling date
 *
 * @author simplity.org
 */
public class DateUtil {
	private static TimeZone UTC_ZONE = TimeZone.getTimeZone("UTC");
	/**
	 * server always uses this format for date, and the date is stored as UTC
	 * date with no time
	 * component in it.
	 */
	public static final String SERVER_DATE_FORMAT = "yyyy-MM-dd";
	/** server always uses this format for date-time */
	public static final String SERVER_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	/** trying to be lenient while parsing date-time coming from clients */
	private static final String SERVER_DATE_SHORT_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	private static final int MILLISECS_PER_DAY = 24 * 60 * 60 * 1000;
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(SERVER_DATE_FORMAT);
	private static final int DATE_TIME_LENGTH = 24;
	private static final int DATE_SHORT_TIME_LENGTH = 20;
	private static final int DATE_LENGTH = 10;
	private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat(SERVER_DATE_TIME_FORMAT);
	private static final SimpleDateFormat DATE_SHORT_TIME_FORMATTER = new SimpleDateFormat(
			SERVER_DATE_SHORT_TIME_FORMAT);

	/**
	 * Dates are time-zone agnostic, and hence are dealt with local calendar.
	 * time, by that we ALWAYS
	 * mean date-time, which is an instance of time like 4:30PM on 12-feb-2016,
	 * is time-zone
	 * sensitive. We expect date-time to be in UTC, and we send them in UTC
	 */
	static {
		DATE_FORMATTER.setTimeZone(UTC_ZONE);
		DATE_FORMATTER.setLenient(false);
		DATE_TIME_FORMATTER.setTimeZone(UTC_ZONE);
		DATE_TIME_FORMATTER.setLenient(false);
		DATE_SHORT_TIME_FORMATTER.setTimeZone(UTC_ZONE);
		DATE_SHORT_TIME_FORMATTER.setLenient(false);
	}

	/**
	 * number of days between two dates. time part is ignored from both dates.
	 * If you want to consider
	 * time as well, then use elapsedDaysBetweenDates()
	 *
	 * @param toDate
	 *            milli-second representation of to-date. By convention, this
	 *            should be a UTC date
	 *            for 0:00 AM
	 * @param fromDate
	 *            milli-second representation of from-date. By convention, this
	 *            should be a UTC
	 *            date for 0:00 AM
	 * @return number of days as counted from date to date, and not based on 24
	 *         hours of elapsed time
	 *         time.
	 */
	public static int daysBetweenDates(long toDate, long fromDate) {
		return (int) TimeUnit.DAYS.convert(toDate - fromDate, TimeUnit.MILLISECONDS);
	}

	/**
	 * number of days the supplied date is from today in the future. same as
	 * date - today.
	 *
	 * @param toDate
	 *            note that this is long - date in milliseconds (date.getTime())
	 * @return number of days as counted from today forward into toDate. If
	 *         toDate is the past,
	 *         returned value is -ve.
	 */
	public static int daysFromToday(long toDate) {
		return daysBetweenDates(toDate, getToday().getTime());
	}

	/**
	 * number of elapsed days (completed 24 hours) between two specific time
	 * intervals.
	 *
	 * @param toTime
	 *            note that this is long - date in milliseconds (date.getTime())
	 * @param fromTime
	 *            note that this is long - date in milliseconds (date.getTime())
	 * @return number of days counted as 1 for every 24 hours from startDate =
	 *         toDate - fromDate
	 */
	public static int elapsedDaysBetweenDates(long toTime, long fromTime) {
		return (int) (toTime - fromTime) / DateUtil.MILLISECS_PER_DAY;
	}

	/**
	 * number of elapsed days (completed 24 hours) between two specific time
	 * intervals.
	 *
	 * @param toDate
	 * @param fromDate
	 * @return number of days counted as 1 for every 24 hours from startDate =
	 *         toDate - fromDate
	 */
	public static int elapsedDaysBetweenDates(Date toDate, Date fromDate) {
		return (int) (toDate.getTime() - fromDate.getTime()) / DateUtil.MILLISECS_PER_DAY;
	}

	/**
	 * add number of days to a date
	 *
	 * @param date
	 * @param days
	 * @return date offset date
	 */
	public static long addDays(long date, long days) {
		return date + days * DateUtil.MILLISECS_PER_DAY;
	}

	/**
	 * add number of days to a date
	 *
	 * @param date
	 * @param days
	 * @return date offset date
	 */
	public static Date addDays(Date date, long days) {
		return new Date(date.getTime() + days * DateUtil.MILLISECS_PER_DAY);
	}

	/**
	 * chop milliseconds from this date, as per default calendar
	 *
	 * @param date
	 *            milli seconds in this date
	 * @return milli seconds that correspond to date with no time
	 */
	public static long trimDate(long date) {
		Calendar cal = Calendar.getInstance(UTC_ZONE);
		cal.setTimeInMillis(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		/*
		 * set date to this millis
		 */
		return cal.getTimeInMillis();
	}

	/**
	 * Get today's date as per local calendar.
	 *
	 * @return a date object that represents a UTC date that is equal to today
	 *         in local calendar. for
	 *         example, if local calendar says 20-Aug-2016, but UTC would say
	 *         21-Aug-2016, this function
	 *         returns date that would print in UTC as
	 *         '2016-08-20T00:00:00.000Z'
	 */
	public static Date getToday() {
		Calendar cal = Calendar.getInstance(UTC_ZONE);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		/*
		 * set date to this millis
		 */
		return cal.getTime();
	}

	/**
	 * @param value
	 * @return date or null if it is not a valid date
	 */
	public static Date parseDate(String value) {
		try {
			return DateUtil.DATE_FORMATTER.parse(value);
		} catch (Exception ignore) {
			return null;
		}
	}

	/**
	 * @param value
	 * @return date or null if it is not a valid date
	 */
	public static Date parseDateTime(String value) {
		try {
			return DateUtil.DATE_TIME_FORMATTER.parse(value);
		} catch (Exception ignore) {
			try {
				return DateUtil.DATE_TIME_FORMATTER.parse(value);
			} catch (Exception ignoreAgain) {
				//
			}
		}
		return null;
	}

	/**
	 * @param textToParse
	 *            date string in server date format, or date-time string in utc
	 *            format
	 * @return parsed date, null if the string is not a valid date/time
	 */
	public static Date parseDateWithOptionalTime(String textToParse) {
		int len = textToParse.length();
		try {
			if (len == DATE_LENGTH) {
				return DATE_FORMATTER.parse(textToParse);
			}
			if (len == DATE_TIME_LENGTH) {
				return DATE_TIME_FORMATTER.parse(textToParse);
			}
			if (len == DATE_SHORT_TIME_LENGTH) {
				return DATE_SHORT_TIME_FORMATTER.parse(textToParse);
			}
		} catch (Exception ignore) {
			//
		}
		return null;
	}

	/**
	 * format date into text
	 *
	 * @param date
	 * @return text
	 */
	public static String formatDate(long date) {
		return DATE_FORMATTER.format(date);
	}

	/**
	 * format date into text
	 *
	 * @param date
	 * @return text
	 */
	public static String formatDate(Date date) {
		return DATE_FORMATTER.format(date);
	}

	/**
	 * @param date
	 * @return UTC formatted date-time
	 */
	public static String formatDateTime(long date) {
		return DATE_TIME_FORMATTER.format(date);
	}

	/**
	 * @param date
	 * @return UTC formatted date-time
	 */
	public static String formatDateTime(Date date) {
		return DATE_TIME_FORMATTER.format(date);
	}

	/**
	 * format date into text, in an economic way. If this is pure date with no
	 * time, format it as
	 * date, else format it is UTC date-time. Use this ONLY if it suits you to
	 * have a shorter version
	 * if possible. If you need the output to be in a predictable format, use
	 * formatDate() or
	 * formatDateTime()
	 *
	 * @param date
	 *            may be date may be date and time, and we do
	 * @return text
	 */
	public static String format(Date date) {
		if (hasTime(date)) {
			return DATE_TIME_FORMATTER.format(date);
		}
		return DATE_FORMATTER.format(date);
	}

	/**
	 * @return calendar for UTC
	 */
	public Calendar getUtcCalendar() {
		return Calendar.getInstance(UTC_ZONE);
	}

	/**
	 * format date into text, in an economic way. If this is pure date with no
	 * time, format it as
	 * date, else format it is UTC date-time. Use this ONLY if it suits you to
	 * have a shorter version
	 * if possible. If you need the output to be in a predictable format, use
	 * formatDate() or
	 * formatDateTime()
	 *
	 * @param date
	 *            may be date may be date and time, and we do
	 * @return text
	 */
	public static String format(long date) {
		if (hasTime(date)) {
			return DATE_TIME_FORMATTER.format(date);
		}
		return DATE_FORMATTER.format(date);
	}

	/**
	 * is this date a pure date, or has it got time component? It is dangerous
	 * to guess this in your
	 * logic, as it is perfectly possible that the date-time field may actually
	 * happen exactly at that
	 * time. Your algorithm should KNOW whether the date in question represents
	 * a pure date or it
	 * date-time. This function is to be used for formatting where such a
	 * possible error does not
	 * cause the algorithm to fail (For example for display purposes)
	 *
	 * @param date
	 * @return true if this has time component, false otherwise
	 */
	public static boolean hasTime(Date date) {
		Calendar cal = Calendar.getInstance(UTC_ZONE);
		cal.setTime(date);
		return (cal.get(Calendar.MILLISECOND) != 0 || cal.get(Calendar.SECOND) != 0 || cal.get(Calendar.MINUTE) != 0
				|| cal.get(Calendar.HOUR_OF_DAY) != 0);
	}

	/**
	 * is this date a pure date, or has it got time component? It is dangerous
	 * to guess this in your
	 * logic, as it is perfectly possible that the date-time field may actually
	 * happen exactly at that
	 * time. Your algorithm should KNOW whether the date in question represents
	 * a pure date or it
	 * date-time. This function is to be used for formatting where such a
	 * possible error does not
	 * cause the algorithm to fail (For example for display purposes)
	 *
	 * @param value
	 * @return true if there is time of the day, false if it is a pure date
	 */
	public static boolean hasTime(long value) {
		Calendar cal = Calendar.getInstance(UTC_ZONE);
		cal.setTimeInMillis(value);
		return (cal.get(Calendar.MILLISECOND) != 0 || cal.get(Calendar.SECOND) != 0 || cal.get(Calendar.MINUTE) != 0
				|| cal.get(Calendar.HOUR_OF_DAY) != 0);
	}
}
