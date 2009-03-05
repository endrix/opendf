package net.sf.opendf.profiler.schedule.gui.views;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;

public class MilliDateFormat extends DateFormat {

	public Date parse(String source, ParsePosition pos) {
		throw new RuntimeException("Cannot parse millisecond date.");
	}

	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
		return toAppendTo.append(date.getTime());
	}
	
}

