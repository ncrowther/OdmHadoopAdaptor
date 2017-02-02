import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class test {

	public static void main(String args[]) {

		String date1 = "2016-10-20";
		String date2 = "2017-12-01";
		
		if (date1.length() != 10 || date2.length() != 10) {
			System.out.println("ERROR: One or more bad dates");
		}

		try {

			java.text.DateFormat df = new java.text.SimpleDateFormat(
					"yyyy-MM-dd");

			Date startDate = df.parse(date1);
			Date endDate = df.parse(date2);

			Calendar startCalendar = new GregorianCalendar();
			startCalendar.setTime(startDate);
			Calendar endCalendar = new GregorianCalendar();
			endCalendar.setTime(endDate);

			int diffYear = endCalendar.get(Calendar.YEAR)
					- startCalendar.get(Calendar.YEAR);
			int diffMonth = diffYear * 12 + endCalendar.get(Calendar.MONTH)
					- startCalendar.get(Calendar.MONTH);

			System.out.println("Months: " + diffMonth);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
