package com.aplos.common.beans;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;

import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.utils.FormatUtil;

@Entity
@ManagedBean
public class DateRange extends AplosBean {

	private static final long serialVersionUID = -7236144242658539985L;
	private Date rangeStart;
	private Date rangeEnd;

	public DateRange() {}

	public DateRange(Date rangeStart, Date rangeEnd) {
		this.setRange(rangeStart, rangeEnd);
	}

	public DateRange(Date rangeStart, Integer rangeEndIncrement) {
		this.setRange(rangeStart, rangeEndIncrement);
	}

	public DateRange(Date rangeStart, Integer rangeEndIncrement, Integer calendarField) {
		this.setRange(rangeStart, rangeEndIncrement, calendarField);
	}

	public void setRangeStart(Date rangeStart) {
		this.rangeStart = rangeStart;
	}

	public Date getRangeStart() {
		return rangeStart;
	}

	public void setRangeEnd(Date rangeEnd) {
		this.rangeEnd = rangeEnd;
	}

	public void setRange(Date rangeStart, Date rangeEnd) {
		this.rangeStart = rangeStart;
		this.rangeEnd = rangeEnd;
	}

	public void setRange(Date rangeStart, Integer rangeEndIncrement) {
		this.rangeStart = rangeStart;
		this.setRangeEnd(rangeEndIncrement);
	}

	public void setRange(Date rangeStart, Integer rangeEndIncrement, Integer calendarField) {
		this.rangeStart = rangeStart;
		if (rangeEndIncrement != null && rangeEndIncrement > 0) {
			this.setRangeEnd(rangeEndIncrement, calendarField);
		} else {
			this.rangeEnd = rangeStart;
		}
	}

	public void setRangeEnd(Integer incrementValue, Integer calendarField) {
		if (rangeStart != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(rangeStart);
			cal.add(calendarField,incrementValue);
			this.rangeEnd = cal.getTime();
		}
	}

	public Boolean rangeConflicts(DateRange range) {
		return rangeConflicts(range, true);
	}

	/** alias for rangeConflicts **/
	public Boolean encompasesRange(DateRange range) {
		return rangeConflicts(range, true);
	}

	//tells us if the date we are querying is inside this range
	public Boolean dateConflicts(Date date) {
		return dateConflicts(date, true);
	}

	/** alias for dateConflicts **/
	public Boolean encompasesDate(Date date) {
		return dateConflicts(date, true);
	}

	/** Returns true if the date given as a parameter falls within the boundaries of this daterange
	 * 	@param date
	 *  @param inclusive - if the date is exactly on the border (first/last day)
	 * 								of range should we count it as conflicting?
	 */
	public Boolean dateConflicts(Date date, Boolean inclusive) {

		if (rangeStart != null && rangeEnd == null) {
			Date compDate = FormatUtil.removeDatesTime(date);
			Date rangeDate = FormatUtil.removeDatesTime(rangeStart);
			return compDate.compareTo(rangeDate) == 0;
		} else if (rangeStart == null && rangeEnd != null) {
			Date compDate = FormatUtil.removeDatesTime(date);
			Date rangeDate = FormatUtil.removeDatesTime(rangeEnd);
			return compDate.compareTo(rangeDate) == 0;
		}

		Calendar cal = Calendar.getInstance();

		cal.setTime(rangeStart);
		FormatUtil.resetTime( cal );
		Date rangeStartCopy = cal.getTime();

		cal.setTime(rangeEnd);
		FormatUtil.resetTime( cal );
		Date rangeEndCopy = cal.getTime();

		cal.setTime(date);
		FormatUtil.resetTime( cal );
		Date paramCopy = cal.getTime();

		int overlapStart = rangeStartCopy.compareTo(paramCopy);
		int overlapEnd = rangeEndCopy.compareTo(paramCopy);

		if (inclusive) {
			if (overlapStart == 0 || overlapEnd == 0) {
				return true;
			} else if (overlapStart < 0 && overlapEnd > 0) {
				return true;
			} else {
				return false;
			}
		} else {
			if (overlapStart < 0 && overlapEnd > 0) {
				return true;
			} else {
				return false;
			}
		}
	}

	/** Returns true if the range given as a parameter overlaps with this daterange
	 * 	@param range
	 *  @param inclusive - if the overlap is exactly on the border (first/last day)
	 * 								of range should we count it as conflicting?
	 */
	public Boolean rangeConflicts(DateRange range, Boolean inclusive) {

		Calendar cal = Calendar.getInstance();
		cal.setTime(rangeStart);
		FormatUtil.resetTime( cal );
		Date rangeStartCopy = cal.getTime();
		cal.setTime(rangeEnd);
		FormatUtil.resetTime( cal );
		Date rangeEndCopy = cal.getTime();
		cal.setTime(range.getRangeStart());
		FormatUtil.resetTime( cal );
		Date paramStartCopy = cal.getTime();
		cal.setTime(range.getRangeEnd());
		FormatUtil.resetTime( cal );
		Date paramEndCopy = cal.getTime();

		if (range.isValid()) {

			int end_beforeOurRange = paramEndCopy.compareTo(rangeStartCopy);
			int start_afterOurRange = rangeEndCopy.compareTo(paramStartCopy);

			if (inclusive) {
				return !(end_beforeOurRange==-1 || start_afterOurRange==-1);
			} else {
				return !(end_beforeOurRange==-1 || start_afterOurRange==-1)
						|| !(end_beforeOurRange==0 || start_afterOurRange==0); //both are either 1 or 0
			}

		} else {
			return true;
		}

	}

	public void setRangeEnd(Integer incrementValue) {
		setRangeEnd(incrementValue, Calendar.DATE);
	}

	public Date getRangeEnd() {
		return rangeEnd;
	}

	public boolean isValid() {
		return (rangeStart != null && rangeEnd != null && rangeStart.compareTo(rangeEnd) < 0);
	}

	public boolean endsInFuture() {
		return endsInFuture(false);
	}

	public boolean endsInFuture(boolean allowSingleDate) {
		if (rangeEnd == null && rangeStart != null && allowSingleDate) {
			return new Date().compareTo(rangeStart) < 0;
		} else {
			return rangeEnd != null && new Date().compareTo(rangeEnd) < 0;
		}
	}

	public boolean endsInPast() {
		return endsInPast(false);
	}

	public boolean endsInPast(boolean allowSingleDate) {
		if (rangeEnd == null && rangeStart != null && allowSingleDate) {
			return new Date().compareTo(rangeStart) > 0;
		} else {
			return rangeEnd != null && new Date().compareTo(rangeEnd) > 0;
		}
	}

	public boolean beginsInFuture() {
		return beginsInFuture(false);
	}

	public boolean beginsInFuture(boolean allowSingleDate) {
		if (rangeStart == null && rangeEnd != null && allowSingleDate) {
			return new Date().compareTo(rangeEnd) < 0;
		} else {
			return rangeStart != null && new Date().compareTo(rangeStart) < 0;
		}
	}

	public boolean beginsInPast() {
		return beginsInPast(false);
	}

	public boolean beginsInPast(boolean allowSingleDate) {
		if (rangeStart == null && rangeEnd != null && allowSingleDate) {
			return new Date().compareTo(rangeEnd) > 0;
		} else {
			return rangeStart != null && new Date().compareTo(rangeStart) > 0;
		}
	}

	public String getRange() {
		if (rangeStart == null || rangeEnd == null) {
			return "Invalid Range";
		}
		return formatDateRange(rangeStart,rangeEnd);
	}

	public String getShortRange() {
		return formatDateRange(rangeStart, rangeEnd, true);
	}

	public String getTimeRange() {
		SimpleDateFormat format = new SimpleDateFormat("HH:mm");
		SimpleDateFormat endFormat = new SimpleDateFormat(" 'to' HH:mm");
		StringBuffer strBuf = new StringBuffer();
		if( getRangeStart() == null && getRangeEnd() == null ) {
			strBuf.append( "N/A" );
		} else {
			if( getRangeStart() != null ) {
				strBuf.append( format.format(rangeStart) );
			} else {
				strBuf.append( "Until " );
			}
			if( getRangeEnd() != null ) {
				strBuf.append( endFormat.format(rangeEnd) );
			} else {
				strBuf.append( " Onwards" );
			}
		}
		return strBuf.toString();
	}

	public static String formatDateRange(Date dateFrom, Date dateTo) {
		return formatDateRange(dateFrom, dateTo, false);
	}

	public static String formatDateRange(Date dateFrom, Date dateTo, Boolean shortVersion) {
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy");
		SimpleDateFormat endFormat;
		if (shortVersion) {
			endFormat = new SimpleDateFormat("-yy");
		} else {
			endFormat = new SimpleDateFormat(" - dd/MM/yy");
		}
		return format.format(dateFrom) + endFormat.format(dateTo);
	}

	@Override
	public String getDisplayName() {
		return getRange();
	}

	public DateRange getCopy() {
		DateRange newRange = new DateRange();
		newRange.setRangeStart(rangeStart);
		newRange.setRangeEnd(rangeEnd);
		newRange.saveDetails();
		return newRange;
	}

	public static List<DateRange> sortByStartEndDate( List<DateRange> dateRanges) {
		Collections.sort( dateRanges, new Comparator<DateRange>() {
			@Override
			public int compare(DateRange range1, DateRange range2) {
				if (range1 == null && range2 == null) {
					return 0;
				} else if (range1 == null && range2 != null) {
					return 1;
				} else if (range1 != null && range2 == null) {
					return -1;
				}
				//now we know neither of the objects themselves are null
				if (range1.getRangeStart() == null && range2.getRangeStart() == null) {
					//compare the end to see if there's a difference there instead
					if (range1.getRangeEnd() == null && range2.getRangeEnd() == null) {
						return 0;
					} else if (range1.getRangeEnd() == null && range2.getRangeEnd() != null) {
						return 1;
					} else if (range1.getRangeEnd() != null && range2.getRangeEnd() == null) {
						return -1;
					} else {
						return range1.getRangeEnd().compareTo(range2.getRangeEnd());

					}
				} else if (range1.getRangeStart() == null && range2.getRangeStart() != null) {
					return 1;
				} else if (range1.getRangeStart() != null && range2.getRangeStart() == null) {
					return -1;
				} else {
					//neither of the range starts are null
					int startCompare = range1.getRangeStart().compareTo(range2.getRangeStart());
					if (startCompare == 0) {
						//compare the end to see if there's a difference there instead
						return range1.getRangeEnd().compareTo(range2.getRangeEnd());
					} else {
						return startCompare;
					}
				}
			}
		});
		return dateRanges;
	}

}
