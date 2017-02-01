package odmengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import parameters.RuleParameter;

public class RestRuleSetExtractor {

	private static final String ARCHIVE_START_ELEMENT = "<archive>";
	private static final String ARCHIVE_END_ELEMENT = "</archive>";
	private static final String ARCHIVE_QUERY = "?parts=archive";
	private static final String SIGNATURE_QUERY = "/signature";

	private static final Logger LOG = Logger.getLogger(RestRuleSetExtractor.class.getName());

	@SuppressWarnings("finally")
	public static String getArchive(final String host, final String rulesetPath, final String username, final String password, boolean https) {

		String archive = "ERROR";

		String restQuery = getRestUrl(host, rulesetPath, https);
		restQuery += ARCHIVE_QUERY;
		
		// Load the rule set using Rest API
		LOG.info("Ruleset REST query: " + restQuery);

		try {
			
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password.toCharArray());
				}
			});
			
			URL url = new URL(restQuery);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/xml");

			if (conn.getResponseCode() != 200) {
				LOG.log(Level.SEVERE, "Failed : HTTP error code : "+ conn.getResponseCode());
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}

			// get result 
			BufferedReader br = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));

			String l = null;
			while ((l = br.readLine().trim()) != null) {
	
				if (l.startsWith(ARCHIVE_START_ELEMENT)) {

					archive = l.substring(ARCHIVE_START_ELEMENT.length(),
							l.length() - ARCHIVE_END_ELEMENT.length());
					break;
				}
			}

			br.close();
			conn.disconnect();
			return archive;

		} catch (Exception e) {
			LOG.log(Level.SEVERE,e.toString());
		}  finally {
			LOG.info("Archive length:" + archive.length());
			return archive;
		}
	}

	public static List<RuleParameter> getSignature(String host,
			String rulesetPath, final String username, final String password, boolean https) throws IOException, ParserConfigurationException, SAXException {

		
		String restQuery = getRestUrl(host, rulesetPath, https);
		
		restQuery += SIGNATURE_QUERY;
	
		// Determine the rule set signature
		LOG.info("Signature REST query: " + restQuery);

		List<RuleParameter> signature = new ArrayList<RuleParameter>();

		Authenticator.setDefault(new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password
						.toCharArray());
			}
		});

		URL url = new URL(restQuery);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/xml");

		if (conn.getResponseCode() != 200) {
			throw new IOException(
					"Failed to get REST parameters from RES. Query was: " 
			        + restQuery +  ".\n Is the ruleset correct?  Are the user/password credetials correct?  \nHTTP error code : " + conn.getResponseCode());
		}

		// get result
		InputStream xml = conn.getInputStream();

		signature = RulesetSignatureParser.parse(xml);
		
		LOG.info("Signature: " + signature);
		
		System.out.println("Signature: " + signature);

		conn.disconnect();

		return signature;

	}
	
	private static String getRestUrl(String host, String rulesetPath, boolean https) {
		
		final String RES_URL;
		if (https) {
			RES_URL = "https://" + host + "/res/api/v1/ruleapps";
		} else {
			RES_URL = "http://" + host + "/res/apiauth/v1/ruleapps";
		}

		String REST_QUERY = RES_URL + rulesetPath;
		return REST_QUERY;
	}
	
	public static void main(String[] args) {
		   
		String host = "brsv2-ae05805f.ng.bluemix.net";  
		String password = "skn9o0rwqng2";

		//String host = "localhost:9080";  
		//String password = "resAdmin";
		
		String rulesetPath = "/validatePnrApp/1.0/validatePnr/1.0"; 
		try {
			getSignature(host, rulesetPath, "resAdmin", password, true);
			
			getArchive(host, rulesetPath, "resAdmin", password, true);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}		
	}	
}
