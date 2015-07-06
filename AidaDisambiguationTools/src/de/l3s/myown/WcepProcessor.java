package de.l3s.myown;

//**********  NOTE : THIS CODE WAS USED TO DISAMBIGUATE WCEP DOCS

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mpi.aida.Disambiguator;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.JsonSettings.JSONTYPE;
import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.PreparationSettings.DOCUMENT_INPUT_FORMAT;
import mpi.aida.config.settings.disambiguation.CocktailPartyLangaugeModelDefaultDisambiguationSettings;
import mpi.aida.config.settings.preparation.StanfordHybridPreparationSettings;
import mpi.aida.data.DisambiguationResults;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.ResultProcessor;
import mpi.aida.preparator.Preparator;
import mpi.aida.util.Counter;
import mpi.aida.util.timing.RunningTimer;
import mpi.tools.javatools.util.FileUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class WcepProcessor {
	private Set<OutputFormat> outputFormats;
	private LinkedList<JsonNode> listJSON = new LinkedList<JsonNode>();
	enum OutputFormat {
		HTML, JSON, TSV
	}

	public static void main(String[] args) throws Exception {
		new WcepProcessor().run(args);
	}

	
	
	public static LinkedList<LinkedList<JsonNode>> chopped(LinkedList<JsonNode> list, final int L) {
	    LinkedList<LinkedList<JsonNode>> parts = new LinkedList<LinkedList<JsonNode>>();
	    final int N = list.size();
	    for (int i = 0; i < N; i += L) {
	        parts.add(new LinkedList<JsonNode>(list.subList(i, Math.min(N, i + L)))
	        );
	    }
	    return parts;
	}

	
	
	
	public void run(String args[]) throws Exception {
		 if(args.length < 1){
			  System.out.println("Must provide input directory");
			  System.exit(1);
		  }
		  outputFormats = new HashSet<>();
		  outputFormats.add(OutputFormat.JSON);
		 // outputFormats.add(OutputFormat.HTML);
		  
		  List<File> files = new ArrayList<File>();
		  File inputFile = new File(args[0]);
		  
		  if(inputFile.isDirectory()) {
			  System.out.println("Number of files ..." + FileUtils.getAllFiles(inputFile).size());
			  for (File f : FileUtils.getAllFiles(inputFile)) {
			      boolean add = true;
			      if (f.getName().toLowerCase().endsWith(".json")) {
			          add = false;
			      }
			      if (add) {
			          files.add(f);
			      }
			   }
		  }else{
		       	files.add(inputFile);
		       	}
		
		PreparationSettings prepSettings = new StanfordHybridPreparationSettings();
		String inputFormat = "PLAIN";
		String jsonFormat = "EXTENDED";
		DOCUMENT_INPUT_FORMAT[] docInpFormats = DOCUMENT_INPUT_FORMAT.values();
		boolean valid = false;
		String disambiguationTechniqueSetting = "LM";
		prepSettings.setDocumentInputFormat(DOCUMENT_INPUT_FORMAT.valueOf(inputFormat));
		int threadCount = 5;
		int chunkThreadCount = 1;
	    int resultCount = 10;
		Double threshold = 0.0;
		String encoding = "UTF-8";
		prepSettings.setEncoding(encoding);
		boolean isTimed = true;
	    boolean isVerbose = true;
	    boolean writeTimingInfo = false;
		//if (!f.exists() || !f.isDirectory()) {
		//		System.out.println("Timing directory doesnt exists or not a directory!");
		//	}
		boolean multiDoc = false;


		ExecutorService es = Executors.newFixedThreadPool(threadCount);
		Preparator p = new Preparator();
		System.out.println("Processing " + files.size() + " document(s) with " + threadCount	+ " threads, ignoring existing .html and .json files.");
		for (File ff : files) {
			JsonFactory fac = new MappingJsonFactory();
			JsonParser jp = fac.createJsonParser(ff);
			JsonToken current;
			current = jp.nextToken();
			if (current != JsonToken.START_OBJECT) {
				//System.out.println("Error: root should be object: quiting.");
				return;
			} else {
				//System.out.println("root is object");
			}
			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String fieldName = jp.getCurrentName();
				current = jp.nextToken();
				if (fieldName.equals("docs")) {
					if (current == JsonToken.START_ARRAY) {
						int counter=0;
						while (jp.nextToken() != JsonToken.END_ARRAY) { 	// read the record into a tree model, // this moves the parsing position to the end of it
						String docId = inputFile.getAbsolutePath()+"_"+counter;
						JsonNode node = jp.readValueAsTree(); 
						listJSON.add(node);
						}
					}
				}
				
				
			}
			
			System.gc();
			System.out.println(listJSON.size());
			LinkedList<LinkedList<JsonNode>> listas = chopped(listJSON, listJSON.size()/100);
			//System.out.println("Elem num in each linkedlist "+listJSON.size()/threadCount);
			for(int i=0; i < 100; i++){
				Processor proc = new Processor(listas.get(i), ff.getAbsolutePath()+"_"+i,disambiguationTechniqueSetting, p, prepSettings,outputFormats, jsonFormat, resultCount,	!inputFile.isDirectory(), isTimed);
			// pass the threshold
				proc.setThreshold(threshold);
				proc.setChunkThreadCount(chunkThreadCount);
				es.execute(proc);
		}
		}

		es.shutdown();
		es.awaitTermination(1, TimeUnit.DAYS);
		if (es.isTerminated()) {
			String timingDir = inputFile.getAbsolutePath()+"/timingDir";
			File f = new File(timingDir);
			if (!f.exists() || !f.isDirectory()) {
				f.mkdir();
			}
			String content = RunningTimer.getDetailedOverview();
			FileUtils.writeFileContent(	new File(timingDir + File.separator + "overall_timing_"	+ new SimpleDateFormat(	"yyyy_MM_dd_hh_mm_ss'.txt'")
										.format(new Date())), content);
			content = RunningTimer.getTrackedDocumentTime();
			FileUtils.writeFileContent(	new File(timingDir + File.separator	+ "document_timing_" + new SimpleDateFormat( "yyyy_MM_dd_hh_mm_ss'.txt'")
									.format(new Date())), content);
			}
			System.out.println(Counter.getOverview());
		}


	public static class Processor implements Runnable {
		private final String inputFile;
		private final String disambiguationTechniqueSetting;
		private final Preparator p;
		private final PreparationSettings prepSettings;
		private final Set<OutputFormat> outputFormats;
		private final String jsonFormat;
		private final int resultCount;
		private int numChunkThread;
		private final boolean logResults;
		private double threshold;
		private final boolean isTimed;
		private boolean multiDoc;
		private String docDelimiter;
		private LinkedList<JsonNode> list;

		public Processor(LinkedList<JsonNode> list, String inputFile,String disambiguationTechniqueSetting, Preparator p, PreparationSettings prepSettings,Set<OutputFormat> outputFormats, String jsonFormat,int resultCount, boolean logResults, boolean isTimed) {
			super();
			this.list = list;
			this.inputFile = inputFile;
			this.disambiguationTechniqueSetting = disambiguationTechniqueSetting;
			this.p = p;
			this.prepSettings = prepSettings;
			this.outputFormats = outputFormats;
			this.jsonFormat = jsonFormat;
			this.resultCount = resultCount;
			this.logResults = logResults;
			this.isTimed = isTimed;
		}

		public void enableMultiDocsPerFile(String docDelim) {
			multiDoc = true;
			docDelimiter = docDelim;
		}

		public void setChunkThreadCount(int numChunkThread) {
			this.numChunkThread = numChunkThread;
		}

		public void setThreshold(double threshold) {
			this.threshold = threshold;
		}

		
		
		
		
		
		
		
		
		
		
		
		@Override
		public void run() {
		
		LinkedList<Map<String, Object>> outputJSON = new LinkedList<Map<String,Object>>();
		//Map<String, Object>
  		try {
	     	//if (isTimed) {
					//System.out.println("Timing info requested. Enabling Real Time Tracker.");
					//RunningTimer.enableRealTimeTracker();
			//}
			for (OutputFormat of : outputFormats) {
				File resultFile = new File(inputFile + "."+ of.toString().toLowerCase());
				if (resultFile.exists()) {
						System.out.println(of + " output for " + inputFile + " exists, skipping.");
						return;
				}
			}

			//BufferedReader reader = new BufferedReader(	new InputStreamReader(new FileInputStream(inputFile),prepSettings.getEncoding()));
			StringBuilder content = new StringBuilder();
			//for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			//	content.append(line).append('\n');
			//}
			//reader.close();
			File resultFile = new File(inputFile + ".json");
			FileWriter fW = new FileWriter(resultFile,true);
			BufferedWriter buffW = new BufferedWriter(fW);
			LinkedList<String> inputToProcess = new LinkedList<String>();
			List<String> lstFailedDocs = new ArrayList<>();
			String docId = inputFile;
			int multiPart = 0;
			for(int i=0; i < list.size(); i++){
			//content.append(list.get(i).getTextValue());
				inputToProcess.add(list.get(i).get("Content").toString());
				String text = list.get(i).get("Content").toString();
			//}
			//if (content.length() == 0) {
			//	System.out.println("Empty Input file : " + inputFile+ ". skipping.");
			//	return;
			//}
			//if (!multiDoc) {
			//	inputToProcess.add(content.toString());
			//} else {
			//	inputToProcess.addAll(DelimBasedTextSplitter.split(	content.toString(), docDelimiter));
			//	System.out.println("Multidoc enabled - Number of documents extracted : "+ inputToProcess.size());
			//}
			PreparedInput input = null;
			Map<String, DisambiguationResults> disambiguationResults = new HashMap<>();
			Map<String, PreparedInput> inputs = new HashMap<>();
			DisambiguationSettings disSettings = new CocktailPartyLangaugeModelDefaultDisambiguationSettings();
			if (threshold > 0.0) {
				disSettings.setNullMappingThreshold(threshold);
			}
			disSettings.setNumChunkThreads(numChunkThread);
			
			//for (String text : inputToProcess) {
				try {
					
					docId = inputFile+"_" + multiPart;
					++multiPart;
					input = p.prepare(docId, text, prepSettings,new ExternalEntitiesContext());
					inputs.put(docId, input);
					Disambiguator d = new Disambiguator(input, disSettings);
					DisambiguationResults results = d.disambiguate();
					disambiguationResults.put(docId, results);

				} catch (Exception e) {
					System.out.println("Exception while processing '"+ input.getDocId() + "': "	+ e.getLocalizedMessage());
					lstFailedDocs.add(input.getDocId());
					System.out.println("Error while processing : "+ input.getDocId()+ ". Adding to errored list ("+ lstFailedDocs.size() + ")");
				}
			//}

				//if (logResults) {
				//	System.out.println("Disambiguation for '" + inputFile+ "' done.");
				//}

				// retrieve JSON representation of Disambiguated results
				JSONTYPE jsonType = JSONTYPE.valueOf(jsonFormat);
				JSONArray jsonResults = generateJson(jsonType,disambiguationResults, inputs);
				String jsonStr;
				jsonStr = ((JSONObject) jsonResults.get(0)).toJSONString();
				ObjectMapper jsonMapper = new ObjectMapper();
				Object json = jsonMapper.readValue(jsonStr, Object.class);	

				
				Map<String, Object> mapa = new LinkedHashMap<String, Object>();
				mapa.put("eventid", list.get(i).get("eventid").getTextValue());
				mapa.put("Title", list.get(i).get("Title").getTextValue());
				mapa.put("sourceid",list.get(i).get("sourceid").getTextValue());
				mapa.put("Content",list.get(i).get("Content").getTextValue());
				mapa.put("URL",list.get(i).get("URL").getTextValue());
				mapa.put("publicationdate",list.get(i).get("publicationdate").getTextValue());
				mapa.put("annotations",list.get(i).findValues("annotations"));
				mapa.put("aida-annotation",json);
				
				outputJSON.add(mapa);
				//if (outputFormats.contains(OutputFormat.JSON)) {
				//File resultFile = new File(inputFile + ".json");
					
				String jsonFinal = jsonMapper.writeValueAsString(mapa);
				//System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonFinal));
									
				//buffW.write(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa));
					
					//Gson gson = new Gson();
					//String json = gson.toJson(jsonStr);
					//System.out.println(json);
					
					//FileUtils.writeFileContent(resultFile, jsonStr);
					//if (logResults) {
					//	System.out.println("Result written to '" + resultFile+ "' in JSON (" + jsonType + ").");
					//}
				//}

				//if (outputFormats.contains(OutputFormat.HTML)) {
				//	HtmlGenerator gen = new HtmlGenerator();
				//	File resultFile = new File(inputFile + ".html");
					// generate HTML from Disambiguated Results
				//	JSONObject json = ((JSONObject) jsonResults.get(0));
				//	FileUtils.writeFileContent(resultFile,gen.constructFromJson(inputFile, json));
				//	if (logResults) {
				//			System.out.println("Result written to '"+ resultFile + "' in HTML.");
				//		}
				//}
				//if (isTimed) {
					//System.out.println(RunningTimer.getDetailedOverview());
				//}
				// For debugging Multidoc: Writing out documents that failed.
			
				
				
			}
			if (lstFailedDocs.size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < lstFailedDocs.size(); ++i) {
					sb.append(lstFailedDocs.get(i)).append("\n");
				}
				resultFile = new File(inputFile + "_errored.txt");
				FileUtils.writeFileContent(resultFile, sb.toString());
				System.out.println("Errored Files list written to "+ resultFile);
			}
				ObjectMapper jsonMapper = new ObjectMapper();
				buffW.write(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(outputJSON));
				buffW.close();
			} catch (Exception e) {
				System.err.println("Error while processing '" + inputFile + "': " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}

		@SuppressWarnings("unchecked")
		private JSONArray generateJson(JSONTYPE jsonType,Map<String, DisambiguationResults> disambiguationResults,Map<String, PreparedInput> inputs) {
			String jsonStr;
			ResultProcessor rp;
			JSONArray jsonArray = new JSONArray();
			for (String tmpDocid : inputs.keySet()) {
				DisambiguationResults tmpResult = disambiguationResults.get(tmpDocid);
				PreparedInput tmpPInp = inputs.get(tmpDocid);
				if (tmpResult != null && tmpPInp != null) {
					rp = new ResultProcessor(tmpResult, inputFile, tmpPInp,	resultCount);
					jsonArray.add(rp.process(jsonType));
				}
			}
			return jsonArray;
		}
	}
}
