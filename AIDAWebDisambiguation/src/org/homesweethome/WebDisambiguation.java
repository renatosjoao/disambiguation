package org.homesweethome;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.client.ClientProtocolException;

/**
 * Utility class to disambiguate entities from input txt file
 * using AIDA
 * 
 * @author Renato Stoffalette Joao
 *
 */

public class WebDisambiguation {
	/**
	 *
	 * Method to call the AIDA disambiguation service.
  * 
  * Input: HTTP POST request containing "application/json"
  * parameters specifying the settings and the input text. See below for expected
  * values. Output: JSON containing the disambiguation results.
  * A JSON input object should contain the following input parameters
  *  "text" : "...",<br />
  *  "inputType" : "...",<br />
  *  "tagMode" : "...",<br />
  *  "docId" : "...",<br />
  *  "technique" : "...",<br />
  *  "algorithm" : "...",<br />
  *  "coherenceMeasure" : "...",<br />
  *  "alpha" : "DOUBLE VALUE",<br />
  *  "ppWeight" : "DOUBLE VALUE",<br />
  *  "importanceWeight" : "DOUBLE VALUE",<br />
  *  "ambiguity" : "INTEGER VALUE",<br />
  *  "coherence" : "DOUBLE VALUE",<br />
  *  "isWebInterface" : "BOOLEAN VALUE",<br />
  *  "exhaustiveSearch" : "BOOLEAN VALUE",<br />
  *  "fastMode" : "BOOLEAN VALUE",<br />
  *  "filteringTypes" : "...",<br />
  *  "keyphrasesSourceWeightsStr" : "...",<br />
  *  "maxResults" : "INTEGER VALUE",<br />
  *  "nullMappingThreshold" : "DOUBLE VALUE", <br /
  *  "mentionDictionary" : Map<String, List<String>",
  *  "entityKeyphrases" : Map<String, List<String>"
  * }
  */
	static String localServer = "http://localhost:8080/aida/service/disambiguate";
	static String remoteServer = "https://gate.d5.mpi-inf.mpg.de/aida/service/disambiguate";
	
	String text = null; // The input text to disambiguate
	String inputType = "TEXT";
	String tagMode = null;
	String docId = null;
	String technique = "LOCAL";
	String algorithm = null;
	String coherenceMeasure = null;
	Double alpha = 0.6;
	Double ppWeight = null;
    Double importanceWeight = null;
    Integer ambiguity = null;
    Double coherence = null;
    Boolean isWebInterface = null;
    Boolean exhaustiveSearch = null;
    Boolean fastMode = null;
    String filteringTypes = null;
	String keyphrasesSourceWeightsStr = null;
	Integer maxResults = null;
	Double nullMappingThreshold = null;
	String jsonType = null;

	public String sendPost(String urlString, String text) throws IOException{
		 URL url = new URL(remoteServer);
		 URLConnection conn = url.openConnection();
		 conn.setDoOutput(true);
		 OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
		 writer.write("text="+text);
		 writer.flush();
		 String line;
		 BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		 StringBuffer stB = new StringBuffer();
		 while ((line = reader.readLine()) != null) {
		     stB.append(line);
		 }
		 writer.close();
		 reader.close();
		 return stB.toString();
	}

	public static void main(String[] args) throws ClientProtocolException,IOException {
		if(args.length < 1){
			System.out.println("*** Please provide input text file ! ***");
			System.exit(1);
			
		}
		File file = new File(args[0]);
		InputStream fileStream = new FileInputStream(file);
		Reader decoder = new InputStreamReader(fileStream, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		StringBuilder stB = new StringBuilder();
		String line = null;
		while ((line = buffered.readLine()) != null) {
			stB.append(line);
		}
		buffered.close();
		String text = stB.toString();
		//System.out.println("============ HTTP Request ================");
		System.out.println("...");
		final long start = System.currentTimeMillis();
		WebDisambiguation wb = new WebDisambiguation();
		String disambiguated = wb.sendPost(remoteServer,text);
		//System.out.println("============Output:============");
		final long stop = System.currentTimeMillis();
		System.err.println("Finished disambiguation in "+ ((stop - start) / 1000.0) + " seconds.");
		System.out.println(disambiguated);
		}
}
