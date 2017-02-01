package odmengine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import parameters.ConversionException;
import parameters.RuleParameter;


import com.ibm.rules.engine.load.EngineLoader;
import com.ibm.rules.engine.runtime.Engine;
import com.ibm.rules.engine.runtime.EngineDefinition;
import com.ibm.rules.engine.runtime.EngineInput;
import com.ibm.rules.engine.runtime.EngineOutput;
import com.ibm.rules.engine.service.EngineService;

public class DecisionEngineRunner implements IRuleEngineRunner {

	private static Engine engine = null;
	private static List<RuleParameter>inSignature = new ArrayList<RuleParameter>();
	private static List<RuleParameter>outSignature = new ArrayList<RuleParameter>();

	
	private static final Logger LOG = Logger.getLogger(DecisionEngineRunner.class.getName());

	/*
	 * (non-Javadoc)
	 * @see odmengine.IRuleEngineRunner#initialise(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void initialise(final String rulesetVersion, final String host,
			final String user, final String resPwd, String executionPwd, boolean https) throws Exception {

		try {
			
			engine = loadRulesetFromRes(host, rulesetVersion, user, resPwd, https);

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Failure reading ruleset from RES.  \nThis may be due to connection problems to the RES or " + 
		               "\nit may be in an invalid format due to the ruleset being built with the Classic Rule Engine: \n" + e.toString());
			throw (e);
			
		}
		
		try {
			List<RuleParameter> signature = RestRuleSetExtractor.getSignature(host, rulesetVersion, user, resPwd, https);
			
			LOG.log(Level.INFO,"Ruleset Signature: " + signature);
			
			// Create Input and Output signatures from ruleset signature.
			for (RuleParameter sig : signature) {

				if (sig.getDirection().startsWith("IN")) {
					inSignature.add(sig);
				}
				
				if (sig.getDirection().endsWith("OUT")) {
					outSignature.add(sig);
				}				
			}		
						

		} catch (Exception e) {
			LOG.log(Level.SEVERE,"Failure getting ruleset signature from RES: " + e.toString());
			throw (e);
		} 
		
	}

	/* (non-Javadoc)onStart = System.currentTimeMillis();

	 * @see odmengine.IRuleEngineRunner#runRules(java.util.Map)
	 */
	@Override
	public String runRules(Map<String, String>params) {

		String response = "";

		try {			
			EngineInput input = createInput(params);
			
			// Execute the rules
			EngineOutput output = engine.execute(input);
			
			response = getOutput(output, params);

			//LOG.info("Response: " + response);

		} catch (Exception e) {
			LOG.log(Level.SEVERE,"Failure running Decision Engine: " + e.toString());
			e.printStackTrace();
		} finally {
			// Reset is necessary to execute the engine with a new call
			if (engine != null) engine.reset();
		}

		return response;
	}
	
	private EngineInput createInput(Map<String, String> params)
			throws ConversionException {

		EngineInput input = engine.createInput();

		// Create Input from ruleset signature.
		for (RuleParameter sig : inSignature) {
			String strValue = params.get(sig.getName());
			input.setParameter(sig.getName(), sig.convert(strValue));
		}

		return input;
	}

	
	private String getOutput(EngineOutput output, Map<String, String> params)
			throws ConversionException {

		StringBuffer strValue = new StringBuffer("");

		// Get outputs from ruleset signature and convert to string
		for (RuleParameter sig : outSignature) {

			if (strValue.length() > 0)
				strValue.append( ",");

			Object value = output.getParameter(sig.getName());

			if (value == null) {
				value = "";
			}
			strValue.append( value.toString());
		}
		return strValue.toString();
	}
	
	
	/**
	 * Loads the ruleset using the RES REST API errors messages
	 * 
	 * @param the ruleset archive path
	 * @throws Exception when parsing detected errors
	 */
	private Engine loadRulesetFromRes(String host, String rulesetPath,
			String username, String password, boolean https) throws Exception {

		String archiveContents = RestRuleSetExtractor.getArchive(host,
				rulesetPath, username, password, https);

		
		if (archiveContents.equals("ERROR")) {
			throw new java.util.MissingResourceException("Cannot read ruleset " + rulesetPath + " from  " + host, host, rulesetPath);
		}
		
		
		byte[] decoded = org.apache.commons.codec.binary.Base64
				.decodeBase64(archiveContents.getBytes());

		InputStream is = new ByteArrayInputStream(decoded);

		// Load the ruleset 
		EngineService[] es = new EngineService[0];
		EngineLoader loader = new EngineLoader(is, null, es);

		// Get an engine definition
		EngineDefinition definition = loader.load();

		// Create a decision engine
		return definition.createEngine();

	}
	
	
	public static void main(String[] args) throws Exception {
		
		IRuleEngineRunner de = new DecisionEngineRunner();	
		
		DeTester.runRuleTest(de);
	}

};
