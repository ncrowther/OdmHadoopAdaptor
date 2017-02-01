package parameters;

import java.util.Date;
import java.util.logging.Logger;


public class RuleParameterFactory {

	private static final Logger LOG = Logger.getLogger(RuleParameterFactory.class.getName());

	public static Class getType(String type) throws ConversionException {
	try {
		
		LOG.info("type::"  + type);
		
		// Convert to corresponding Wrapper type
		if (type.equals("int")) {
			return Integer.class;
		}
		if (type.equals("double")) {
			return Double.class;
		}
		if (type.equals("float")) {
			return Float.class;
		}	
		if (type.equals("boolean")) {
			return Boolean.class;
		}			
		return Class.forName(type);
	} catch (Exception e) {
		e.printStackTrace();
		throw new ConversionException("Cannot convert class: " + type);
	}
}
	
	public static RuleParameter create(String direction, String kind,
			String name, String type) throws ConversionException {

		Object classType = getType(type);
				
		if (classType.equals(Date.class)) {
			//LOG.info("IN Date Param =" + name);
			return new DateParameter(direction, kind, name);
		} else if (classType.equals(Integer.class)) {
			//LOG.info("IN Integer Param =" + name);
			return new IntegerParameter(direction, kind, name);
		} else if (classType.equals(Double.class)) {
			//LOG.info("IN Date Param =" + name);
			return new DoubleParameter(direction, kind, name);
		} else if (classType.equals(String.class)) {
			//LOG.info("IN String Param =" + name);
			return new StringParameter(direction, kind, name);
		} else if (classType.equals(Float.class)) {
			//LOG.info("IN String Param =" + name);
			return new FloatParameter(direction, kind, name);
		} else if (classType.equals(Boolean.class)) {
			//LOG.info("IN String Param =" + name);
			return new BooleanParameter(direction, kind, name);
		}else {
			throw new ConversionException("Unsupported parameter type: " + classType);
		}
	}
}
