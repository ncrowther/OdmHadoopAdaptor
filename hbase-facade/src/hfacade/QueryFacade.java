package hfacade;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

/**
 * Class to demonstrate ODM integration to external java-based rules.
 * These rules are primarily designed to reach out to external data sources
 * (e.g. HBase to perform validation).
 */
public class QueryFacade
{
    private static final Logger LOG = Logger.getLogger (QueryFacade.class.getName());
    
    private static Map<String, HTable> tables = new HashMap <String, HTable>();
    private static Map<String, Set<String>> keyCache = new HashMap<String, Set<String>>();
    private static Map<String, String> dupKeyCache = new HashMap<String, String>();;
    private static Configuration conf = null;
    
    
    /**
     * This should be called when you are done with the QueryFacade to ensure
     * that all database connections are released.
     */
    public static void close()
    {      
        for (HTable table : tables.values ())
        {
            try
            {
                table.close ();
            }
            catch (IOException e)
            {
                /* IGNORED */
            }
        }
    }
    
    public static boolean keyExists (String table, String key)
        throws IOException
    {
        LOG.info("KeyExists('" + table + "', '" + key);
        
        return keyExists(table, key, false);
    }
    
    /**
     * Checks to see if a given key from a given table is a duplicate.
     * This method relies upon data from the given table being received
     * in order...that is, a duplicate is known it appears one record
     * right after another.
     * 
     * @param table The table
     * @param key The value that is being checked for duplication.
     * @return true if it is a duplicate
     */
    public static boolean isOrderedDuplicate (String table, String key)
    {
        String old = dupKeyCache.put (table, key);
        boolean isDup = (old != null && old.equals (key));
        
        LOG.info ("isDuplicate('" + table + "', '" + key + "': " + isDup);
        return isDup;
    }
    
    /**
     * Tests whether or not a given key value exists within a given
     * tabnle.
     * 
     * @param table The name of the table
     * @param key The key to test for
     * @param doCache If true, then the first call for a given table will
     *    cause all keys to be read in and cached for subsequent requests.
     * 
     * @return true if the key exists, false otherwise
     */
    public static boolean keyExists (String table, String key, boolean doCache)
        throws IOException
    {
        
        boolean exists;
        
        if (doCache)
        {
            Set<String> keys = null;
            
            keys = keyCache.get (table);
            if (keys == null)
            {
                keys = getKeys (table);
                keyCache.put (table, keys);
            }
            
            exists = keys.contains(key);
        }
        
        HTable tab = getTable(table);
        Get get = new Get(key.getBytes ());
        exists = tab.exists (get);
        
        LOG.info ("keyExists('" + table + "', '" + key + "': " + exists
                + (doCache ? " (cached)" : ""));
        
        return exists;
    }
    
    public static boolean partialKeyExists (String table, String key)
        throws IOException
    {
       
        boolean exists;
        
        HTable tab = getTable(table);
        byte []startKey = key.getBytes ();
        byte []stopKey = new byte[startKey.length+1];
        
        System.arraycopy (startKey, 0, stopKey, 0, startKey.length);
        stopKey[startKey.length] = (byte) 0xFF;
        
        Scan scan = new Scan(startKey, stopKey);
        ResultScanner scanner = tab.getScanner (scan);
        
        if (scanner.next () != null)
        {
            scanner.close ();
            exists = true;
        }
        else
        {
            exists = false;
        }

        LOG.info("partialKeyExists('" + table + "', '" + key + "': " + exists);
        
        return exists;
    }
    
    /**
     * Queries a column of a table to verify if the value matches a supplied
     *  value.
     * @param table The table
     * @param key The key value for the table
     * @param family The column family for the column to retrieve
     * @param column The column to retrieve
     * @param value The to check
     * @return true/false
     * @throws IOException
     */
    public static boolean valueExists (String table, String key, 
           String family, String column, String value)
        throws IOException
    {
        String valStr = getValue(table, key, family, column);
        
        if (value == null && valStr == null)
            return true;
        
        if (valStr == null)
            return false;
        
        return valStr.equals(value);
    }
    
    /**
     * Puts a value into the table
     * @param table The table
     * @param key The key value for the table
     * @param family The column family for the column to retrieve
     * @param column The column to retrieve
     * @param value The data to add
     * @throws IOException
     */
	 public static void putValue(String table, String key, 
	           String family, String column, String value) throws IOException {
		 
	     byte[] famBytes = family.getBytes ();
	     byte[] colBytes = column.getBytes ();
	     byte[] valBytes = value.getBytes ();
	     
	     if (conf == null)
             conf = HBaseConfiguration.create();
	     
	     HTable tab = new HTable (conf, table);

		 Put p = new Put(key.getBytes());

		 p.add(famBytes, colBytes, valBytes);
		 
		 tab.put(p);
		 tab.close();
	 }
    
    /**
     * Retrieves the value of a column from a table.
     * 
     * @param table The table
     * @param key The key value for the table
     * @param family The column family for the column to retrieve
     * @param column The column to retrieve
     * @param value The to check
     * @return true/false
     * @throws IOException
     */
    public static String getValue (String table, String key, 
           String family, String column)
        throws IOException
        {
            
        byte[] famBytes = family.getBytes ();
        byte[] colBytes = column.getBytes ();
        
        HTable tab = getTable(table);
        Get get = new Get(key.getBytes());
        get.addColumn (famBytes, colBytes);
        
        Result result = tab.get (get);
        byte []val = result.getValue (famBytes, colBytes);
        
        if (val == null)
            return null;
        
        return new String(val);
    }	 
    
    private static Set<String> getKeys (String table)
        throws IOException
    {
        HTable tab = getTable(table);
        Set<String> keys = new HashSet<String>();
        Scan scan = new Scan();
        ResultScanner scanner = tab.getScanner (scan);
        
        for (Result res : scanner)
        {
            keys.add (new String(res.getRow ()));
        }
        
        return keys;
    }
	
	
	 private static HTable getTable (String name)
		        throws IOException
		    {		         
		        HTable tab = tables.get (name);
		        if (tab == null)
		        {
		            if (conf == null)
		                conf = HBaseConfiguration.create();
		            
			        HBaseAdmin hbaseadmin = new HBaseAdmin(conf);
			        
			        if(!hbaseadmin.tableExists(name)) {
			             throw new RuntimeException("Table " + name + " does not exist in " + conf.toString());
			        }
			  
			         
		            tab = new HTable (conf, name);
		            
		            tables.put(name, tab);
		        }
		        
		        return tab;
		    }
	
}
   
