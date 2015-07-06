package de.l3s.myown;

/***
 * 
 * @author joao
 * 		MyWcep class object mapping
 *
 */
public class MyWcep {
	public String eventid = null;
	public String Title = null;
	public String sourceid = null;
	public String Content = null;
	public String URL = null;
	public String publicationdate = null;
	public String annotations = null;
	public String lang = null;
	public String timestamp = null;
	public String api = null;

	
	public MyWcep(String eventid, String title, String sourceid,String content, String uRL, String publicationdate) {
		super();
		this.eventid = eventid;
		Title = title;
		this.sourceid = sourceid;
		Content = content;
		URL = uRL;
		this.publicationdate = publicationdate;
	}
	
	
	public String getEventid() {
		return eventid;
	}
	public void setEventid(String eventid) {
		this.eventid = eventid;
	}
	public String getTitle() {
		return Title;
	}
	public void setTitle(String title) {
		Title = title;
	}
	public String getSourceid() {
		return sourceid;
	}
	public void setSourceid(String sourceid) {
		this.sourceid = sourceid;
	}
	public String getContent() {
		return Content;
	}
	public void setContent(String content) {
		Content = content;
	}
	public String getURL() {
		return URL;
	}
	public void setURL(String uRL) {
		URL = uRL;
	}
	public String getPublicationdate() {
		return publicationdate;
	}
	public void setPublicationdate(String publicationdate) {
		this.publicationdate = publicationdate;
	}
	public String getAnnotations() {
		return annotations;
	}
	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}
	public String getLang() {
		return lang;
	}
	public void setLang(String lang) {
		this.lang = lang;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getApi() {
		return api;
	}
	public void setApi(String api) {
		this.api = api;
	}
	
	
}
	 /*"docs": [{
		    "eventid": "1", 
		    "Title": "World news and comment from the Guardian", 
		    "sourceid": "1", 
		    "Content": "Danes have gone to the polls on Thursday after a tightly fought election campaign. Join us for the exit polls and results as they come through", 
		    "URL": "http://www.guardian.co.uk/worldlatest/story/0,,-5793952,00.html", 
		    "publicationdate": "20060501", 
		    "annotations": {
		        "lang": "en", 
		        "timestamp": "2015-06-18T16:21:40", 
		        "api": "tag", 
		        "annotations": [
		            {
		                "end": 5, 
		                "title": "Denmark", 
		                "spot": "Danes", 
		                "start": 0, 
		                "rho": "0.17411", 
		                "id": 76972
		            }, 
		            */

