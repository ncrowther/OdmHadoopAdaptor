
package mapreduce;

import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class MapFactory {

	private static int MAX_COLS = 1024;
	private static String[] cols = new String[MAX_COLS];
	
	private static final Logger LOG = Logger.getLogger(MapFactory.class.getName());
	

	/**
	 * 
	 * @colsStr get the column names from the input parameters and copy it into
	 *          an internal structure
	 * @return void
	 */
	public static void getColumns(String colsStr) {

		int i = 0;
		StringTokenizer itr = new StringTokenizer(colsStr, ",");

		// Get Column Names. This will be the key values in the parameter hash
		// map
		while (itr.hasMoreTokens() && i < MAX_COLS) {
			cols[i] = itr.nextToken().trim();
			
			LOG.info("Col[" + i + "]=" + cols[i]);
			
			i++;
		}
	}

	/**
	 * 
	 * @param row
	 *            of CSV data representing a set of parameters to be passed to
	 *            the rule engine
	 * @return parameters as a HashMap
	 */
	public static Map<String, String> csvToMap(String row) {

		// Get parameters which are defined on a row of CSV data.
		StringTokenizer itr = new StringTokenizer(row, ",");

		Map<String, String> args = new HashMap<String, String>();
		int i = 0;

		while (itr.hasMoreTokens() && i < MAX_COLS) {

			String valueStr = itr.nextToken();

			// cols is the key containing the parameter name
			args.put(cols[i++], valueStr);
		}

		return args;
	}
}
