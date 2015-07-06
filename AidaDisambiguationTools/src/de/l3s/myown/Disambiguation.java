package de.l3s.myown;

//**********  NOTE : THIS CODE WAS USED TO DISAMBIGUATE FINACIAL TIMES DEUTSCHLAND
/***
 * 
 * @author  Renato Stoffalette Joao
 * @version 1.0
 * @since   2015-05 
 *
 ***/
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import mpi.aida.util.htmloutput.HtmlGenerator;
import mpi.aida.util.timing.RunningTimer;
import mpi.tools.javatools.util.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

/**
 * Disambiguates a document from the command line.
 *
 */
public class Disambiguation {
    //private final String inputFile; 
    private final String disambiguationTechniqueSetting = "LM"; //CocktailPartyLangaugeModelDefaultDisambiguationSettings(); 
    private final Preparator p = null;
    private final PreparationSettings prepSettings = null;
    private Set<OutputFormat> outputFormats = null;
    private final String jsonFormat = null;
    private final static int resultCount = 10;
    private int numChunkThread;
    private final boolean logResults = false;
    private double threshold = 0.0;
    private final boolean isTimed = false;
    private boolean multiDoc;
    private String docDelimiter = "#==========#\n";
    private static String NEW_LINE_SEPARATOR = "\n";
	static String inputFilesPath = null;
	static String outputFilesPath = null;
	static Map<String, String> mObj = new LinkedHashMap<String,String>();


  enum OutputFormat {
    HTML,JSON,TSV
  }
  
  
  public void run (String args[]) throws Exception {
	  
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
	  String jsonFormat =  "EXTENDED";
	  prepSettings.setDocumentInputFormat(DOCUMENT_INPUT_FORMAT.valueOf(inputFormat));
	  int threadCount = 10;
	  int chunkThreadCount = 1;
	  int resultCount = 10;
	  String encoding = "UTF-8";
	  prepSettings.setEncoding(encoding);
      boolean isTimed = true;
	  boolean isVerbose = true;
      boolean writeTimingInfo = true;
      //boolean multiDoc = true;
      
      ExecutorService es = Executors.newFixedThreadPool(threadCount);
      Preparator p = new Preparator();
   
      System.out.println("Processing " + files.size() + " documents with " + threadCount + " threads, ignoring existing .html and .json files.");
      
      for (File f : files) {
          Processor proc = new Processor(f.getAbsolutePath(),disambiguationTechniqueSetting, p, prepSettings, outputFormats, jsonFormat, resultCount,!inputFile.isDirectory(), isTimed);
          proc.setThreshold(threshold);
          proc.setChunkThreadCount(chunkThreadCount);
          proc.enableMultiDocsPerFile(docDelimiter);
          es.execute(proc);
        }
      es.shutdown();
      es.awaitTermination(1, TimeUnit.DAYS);
      
      if (es.isTerminated()) {
         
            String content = RunningTimer.getDetailedOverview();
            String timingDir = inputFile+"/timingDir";
            File f = new File(timingDir);
            if (!f.exists() || !f.isDirectory()) {
            	f.mkdir();
            }
            content = RunningTimer.getDetailedOverview();
			FileUtils.writeFileContent( new File(timingDir + File.separator + "overall_timing_" + new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss'.txt'").format(new Date())), content);
            content = RunningTimer.getTrackedDocumentTime();
            FileUtils.writeFileContent( new File(timingDir + File.separator + "document_timing_" + new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss'.txt'").format(new Date())), content);
          //  System.out.println(Counter.getOverview());
        }
  }
  


  class Processor implements Runnable {
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

	    public Processor(String inputFile, String disambiguationTechniqueSetting,Preparator p, PreparationSettings prepSettings, Set<OutputFormat> outputFormats,
	        String jsonFormat, int resultCount, boolean logResults, boolean isTimed) {
	      super();
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

	      @SuppressWarnings("unchecked")
		@Override
	      public void run() {
	    	  try{
	    	  File resultFile = null;
	    	  //System.out.println("Timing info requested. Enabling Real Time Tracker.");
	          //RunningTimer.enableRealTimeTracker();
	          
	          for (OutputFormat of : outputFormats) {
	              resultFile = new File(inputFile + "." + of.toString().toLowerCase());
	              if (resultFile.exists()) {
	                System.out.println(of + " output for " + inputFile + " exists, skipping.");
	                return;
	              }
	            }
	          BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), prepSettings.getEncoding()));
	          //StringBuilder content = new StringBuilder();
	          LinkedList<String> content = new LinkedList<String>();
	          JSONArray jsonOriginalArray = new JSONArray();
	          int multiPart = 0;
	          FileWriter fW = new FileWriter(resultFile);	
	  		  BufferedWriter buffW = new BufferedWriter(fW);
	          LinkedList<String> inputToProcess = new LinkedList<String>();
	          PreparedInput input = null;
        	  Map<String, DisambiguationResults> disambiguationResults = new LinkedHashMap<>();
        	  Map<String, PreparedInput> inputs = new LinkedHashMap<>();
        	  StringBuffer outputString = new StringBuffer();
        	  List<String> lstFailedDocs = new ArrayList<>();
        	  DisambiguationSettings disSettings = new CocktailPartyLangaugeModelDefaultDisambiguationSettings();
        	  disSettings.setNumChunkThreads(numChunkThread);
	          for (String line = reader.readLine(); line != null; line = reader.readLine()) {	        	      	  
	        	  
	        	  JSONObject jobj = new JSONObject();
	        	  JSONParser jparser = new JSONParser();
	        	  jobj = (JSONObject) jparser.parse(line);
	        	  jsonOriginalArray.add(jobj);
	        	  String text = (String) jobj.get("text");
	        	  //content.append(text);
	        	  //content.append(docDelimiter);
	        	  //content.add(text);
	        	  //content.append('\n');
	        	
	        	  //List<Integer> mySpecialLsit = new ArrayList<Integer>();
		          //int count = 0 ;
		          String docId = inputFile;
		          try {
		                 
	                  docId += "_" + multiPart;
	                  ++multiPart;
	                  input = p.prepare(docId, text, prepSettings, new ExternalEntitiesContext());
	                  inputs = new LinkedHashMap<String, PreparedInput>();
	                  inputs.put(docId, input);
	                  Disambiguator d = new Disambiguator(input, disSettings);
	                  DisambiguationResults results = d.disambiguate();
	                  disambiguationResults = new LinkedHashMap<String, DisambiguationResults>();
	                  disambiguationResults.put(docId, results);
	                  
	                 JSONTYPE jsonType = JSONTYPE.valueOf(jsonFormat);
	  	             JSONArray jsonResults = generateJson(jsonType, disambiguationResults, inputs);
	  	             //System.out.println("JsonResults==="+jsonResults.get(0));
	  	             
	  	            if( (!jsonResults.isEmpty()) ){
	  	           //for (int j = 0; j < jsonResults.size(); j++) {
		                HtmlGenerator gen = new HtmlGenerator();
		                JSONObject json = ((JSONObject) jsonResults.get(0));
		                
		                mObj.put("ts", jobj.get("ts").toString());
		                mObj.put("type", jobj.get("type").toString());
		                mObj.put("s",jobj.get("s").toString());	               
		    			mObj.put("url", jobj.get("url").toString());
			        	mObj.put("origTxt", jobj.get("text").toString());
		    			mObj.put("html",gen.constructFromJson(inputFile, json) );
		    			
		    			outputString.append(JSONValue.toJSONString(mObj));
		    			outputString.append(NEW_LINE_SEPARATOR);
	  	            }
	                  
		          } catch (Exception e) {
	                  lstFailedDocs.add(docId);
	                  System.out.println("Error while processing : " + docId + ". Adding to errored list (" + lstFailedDocs.size() + ")");
		          }
	          }
	          reader.close();
	          buffW.write(outputString.toString());
	          buffW.close();
	      }catch(Exception e){
	    	  e.printStackTrace();
	      }
	    	  
	      }
	      
	 @SuppressWarnings("unchecked")
	private JSONArray generateJson(JSONTYPE jsonType,Map<String, DisambiguationResults> disambiguationResults,Map<String, PreparedInput> inputs) {
	    	      String jsonStr;
	    	      ResultProcessor rp;
	    	      JSONArray jsonArray = new JSONArray();
	    	      for(String tmpDocid : inputs.keySet()) {
	    	        DisambiguationResults tmpResult = disambiguationResults.get(tmpDocid);
	    	        PreparedInput tmpPInp = inputs.get(tmpDocid);
	    	        if(tmpResult != null && tmpPInp != null) {
	    	        									//estou desconfiando de inputFile ***
	    	          rp = new ResultProcessor(tmpResult, inputFile, tmpPInp, resultCount);
	    	          jsonArray.add(rp.process(jsonType));
	    	        }else{
	    	        	System.out.println("Entrou aqui" );
	    	        }
	    	      }

	    	      return jsonArray;
	    	    }
  }
  
public static void main(String[] args) throws Exception{
	 new Disambiguation().run(args);  
    }
}
