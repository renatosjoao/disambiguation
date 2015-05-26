package org.homesweethome;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Utility class to disambiguate entities from input txt file
 * using DBPedia SpotLight
 * 
 * @author Renato Stoffalette Joao
 * @version 1.0
 * @since   2015-04 
 */
public class RestHttpClient {

	private static String production = "http://spotlight.dbpedia.org/rest/annotate";
	private static String dev = "http://spotlight.dbpedia.org/dev/rest/annotate";

	public static void main(String[] args) throws ClientProtocolException,
			IOException {

		if (args.length < 1) {
			System.out.println("ERROR !");
			System.out
					.println("*** You must specify the text to be disambiguated ***");
			System.exit(1);
		}
		File file = new File(args[0]);
		InputStream fileStream = new FileInputStream(file);
		Reader decoder = new InputStreamReader(fileStream, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		StringBuilder stBuilder = new StringBuilder();
		String line = null;
		while ((line = buffered.readLine()) != null) {
			stBuilder.append(line);
		}
		buffered.close();
		String text = stBuilder.toString();
		System.out.println(" ...");
		final long start = System.currentTimeMillis();
		HttpClient httpClient = HttpClientBuilder.create().build();
		String url = "http://spotlight.dbpedia.org/rest/annotate?text=";
		String stB = text.replace(" ", "%20");
		// Create new getRequest with below mentioned URL
		HttpGet getRequest = new HttpGet(url + stB + "&confidence=0.2&support=20");
		// Add additional header to getRequest which accepts application/xml
		getRequest.addHeader("accept", "application/json");
		// Execute your request and catch response
		HttpResponse response = httpClient.execute(getRequest);
		// Check for HTTP response code: 200 = success
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "+ response.getStatusLine().getStatusCode());
		}
		// Get-Capture Complete application/xml body response
		BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
		String output;
		//System.out.println("============Output:============");
		final long stop = System.currentTimeMillis();
		System.err.println("Finished disambiguation in "+ ((stop - start) / 1000.0) + " seconds.");
		// Simply iterate through XML response and show on console.
		while ((output = br.readLine()) != null) {
			System.out.println(output);
		}

	}
}