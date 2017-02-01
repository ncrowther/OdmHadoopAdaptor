package odmengine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import parameters.RuleParameter;


public class RestEngineRunner implements IRuleEngineRunner {

	private static List<RuleParameter> outSignature = new ArrayList<RuleParameter>();

	private final HttpClient client = new DefaultHttpClient();
	private HttpPost httpPost;
	
	private static final Logger LOG = Logger.getLogger(RestEngineRunner.class.getName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see odmengine.IRuleEngineRunner#initialise()
	 */
	@Override
	public void initialise(final String rulesetVersion, final String host,
			final String user, final String resPwd, final String executionPwd, boolean https) throws Exception {

		try {
			List<RuleParameter> signature = RestRuleSetExtractor.getSignature(
					host, rulesetVersion, user, resPwd, https);

			// Create Input and Output signatures from ruleset signature.
			for (RuleParameter sig : signature) {

				if (sig.getDirection().endsWith("OUT")) {
					outSignature.add(sig);
					
					LOG.info("OUT PARAM: " + sig);
				}
			}

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Failure getting ruleset signature from RES: "
					+ e.toString());
			throw (e);

		}

		// Now initialise the REST client
		final String restUrl;
		
		// Now determine RES URL depending on whether it is HTTPS or HTTP		
		if (https) {
			restUrl = "https://"  + host + ":443" + "/DecisionService/rest/v1" + rulesetVersion + "/json";  
		} else {
			restUrl = "http://"  + host +  "/DecisionService/rest/v1" + rulesetVersion + "/json";  
		}
		
		LOG.info("REST Execution URL: " + restUrl);
		
		httpPost = new HttpPost(restUrl);  
		
	    httpPost.addHeader("Authorization", executionPwd); 
	    httpPost.addHeader("Content-Type", "application/json");
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see odmengine.IRuleEngineRunner#runRules(java.util.Map)
	 */
	public String runRules(Map<String, String> params) {

		StringEntity input = null;

		String result = "";

		try {

			// Create a Jackson Serializer from parameter map
			ObjectMapper mapper = new ObjectMapper();
			input = new StringEntity(mapper.defaultPrettyPrintingWriter()
					.writeValueAsString(params));

			input.setContentType("application/json");

			httpPost.setEntity(input);
			HttpResponse httpResponse = client.execute(httpPost);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					httpResponse.getEntity().getContent()));
			
			StringBuffer responseBuf = new StringBuffer("");
			
			String line = rd.readLine();

			while (line != null) {

				responseBuf.append(line);
				line = rd.readLine();
			}
			
			//LOG.info("RAW OUT:" + responseBuf);
			
			// Deserialize the response
			TypeReference<Map> typeRef = new TypeReference<Map>() {};
			Map results = (HashMap) mapper.readValue(responseBuf.toString(), typeRef);

			// Get the returned object
			result = getOutput(results);			

		} catch (Exception e) {
			
			LOG.log(Level.SEVERE, "Failed to read output parameters: " + e.toString());
			e.printStackTrace();
		}

		return result;
	}

	private String getOutput(Map output) {

		StringBuffer strValue = new StringBuffer("");

		// Get outputs from ruleset signature and convert to string
		for (RuleParameter sig : outSignature) {

			if (strValue.length() > 0)
				strValue.append(",");

			Object value = output.get(sig.getName());

			if (value == null) {
				value = "";
			}
			
			strValue.append(value.toString());

		}
		
		
		return strValue.toString();
		
		
	}

	public static void main(String[] args) throws Exception {

		IRuleEngineRunner restEngine = new RestEngineRunner();
		
		DeTester.runRuleTest(restEngine);
		
	}
}
