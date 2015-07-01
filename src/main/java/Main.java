import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Main extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    try{
    response.addHeader("Access-Control-Allow-Origin", "*");
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");

    String jsonResponse="{\"query\":";
    
    String queryString = request.getParameter("url");
    if (queryString == null) {
      response.setStatus(HttpServletResponse.SC_OK);
      jsonResponse+="\"\"}";
      response.getWriter().append(jsonResponse);
      response.getWriter().flush();
      
      return;
    }
    
    Connection con = Jsoup.connect(queryString);
    con.userAgent("Mozilla");
    
    Document doc = con.get();
    
    queryString = queryString.replace("-", " ");

    String frequentWords[] = {"need","fired","add","after","saw","chief","awesome","pay","grows","consider","|","from","every","you","people","updates","in-the-moment","get","fascinating","friends","your","connect","login","other","others","sure","and","at","be","but","by","if","into","it","no","not","of","or","such","an","the","a","their","then","there","these","this","to","was","will","with","so","also","that","they","therefore","for","much","more","hence","is","are","why","what","how","as","on","in","-","&"," "};
    
    boolean titleFound = false;
    String titleRequired = "";
    
    String title = doc.title();
    
    if (!title.isEmpty())
    { 
      titleRequired = title.replaceAll("[&]{1}.+[;]{1}", "");    //To replace HTML Codes like &#132; with ""
      titleFound = true;

      if(queryString.contains("wikipedia"))
        {
          String kw = title.substring(0, title.indexOf("- Wikipedia") - 1);
          jsonResponse+="\""+kw+"\"}";
          response.getWriter().append(jsonResponse);
          response.getWriter().flush();
          return;
        }

    }
    else 
    {
      titleRequired = "NOT_FOUND";
      titleFound = false;
    }
    
    Elements metaLinksForDescription = doc.select("meta[name=description]");
    boolean metaDescriptionFound = false;
    List<String> descriptionList = new ArrayList<String>();
    
    String metaTagDescriptionContent = "", metaDescriptionRequired = "";
    
    if (!metaLinksForDescription.isEmpty())
    { 
      metaTagDescriptionContent = metaLinksForDescription.first().attr("content");
      descriptionList = (List<String>) Arrays.asList(metaTagDescriptionContent.split(" "));
      metaDescriptionRequired = metaTagDescriptionContent;
      metaDescriptionFound = true;
    }
    else 
    {
      metaDescriptionRequired = "NOT_FOUND";
      metaDescriptionFound = false;
    }
    

    Elements metaLinksForKeywords = doc.select("meta[name=keywords]");
    boolean metaKeywordFound = false;
    List<String> keyWordsList = new ArrayList<String>();
  
    String metaTagKeywordscontent = "", metarKeyWordsRequired = "";
    
    if (!metaLinksForKeywords.isEmpty())
    { 
      metaTagKeywordscontent = metaLinksForKeywords.first().attr("content");
      keyWordsList = (List<String>) Arrays.asList(metaTagKeywordscontent.split(","));
      metarKeyWordsRequired = metaTagKeywordscontent;
        metaKeywordFound = true;
    }
    else 
    {
      metarKeyWordsRequired = "NOT_FOUND";
        metaKeywordFound = false;
    }
    
    HashMap<String, Integer> keywordsHM = new HashMap<String, Integer>();
    
    if(metaDescriptionFound)
    {
      for(String keyWord : descriptionList)
      {
        int count = 1;
        keyWord = keyWord.replace(",", "");
        keyWord = keyWord.replace(".", "");
        keyWord = keyWord.replace(":", "");
        keyWord = keyWord.replace("*", "");
        keyWord = keyWord.replaceAll("[&]{1}.+[;]{1}", "");
        keyWord = keyWord.toLowerCase();
        boolean frequentWord = checkIfFrequentWord(frequentWords,keyWord);
        if(!frequentWord)
        {
          if(keywordsHM.get(keyWord)==null)
          {
            keywordsHM.put(keyWord,count);
          }
          else
          {
            int cnt = keywordsHM.get(keyWord);
            keywordsHM.put(keyWord, ++cnt);
          }
        }
        
      }
    }
    
    if(metaKeywordFound)
    {
      for(String keyWord : keyWordsList)
      {
        int count = 5;
        keyWord = keyWord.trim();
        keyWord = keyWord.replaceAll("[&]{1}.+[;]{1}", "");
        keyWord = keyWord.toLowerCase();
        boolean frequentWord = checkIfFrequentWord(frequentWords,keyWord);
        if(!frequentWord)
        {
          if(keywordsHM.get(keyWord)==null)
          {
            keywordsHM.put(keyWord,count);
          }
          else
          {
            int cnt = keywordsHM.get(keyWord);
            keywordsHM.put(keyWord, ++cnt);
          }
        }
          
      }
    }
    
    for(String kw: keywordsHM.keySet())
    {
      if(titleFound)
      {
        if(titleRequired.toLowerCase().contains(kw))
        {
          int value = keywordsHM.get(kw);
          keywordsHM.put(kw, value+2);
        }
      }
      if(queryString.contains(kw))
      {
        int value = keywordsHM.get(kw);
        keywordsHM.put(kw, value+4);
      }
    }
    
    /*for(String item: keywordsHM.keySet())
    {
      int value = keywordsHM.get(item);
      System.out.println("Key: "+item+"   Value: "+value);
    }*/
    
    if(!keywordsHM.isEmpty())
		{
			int maxValueInMap=(Collections.max(keywordsHM.values()));  // This will return max value in the Hashmap
	        for (Entry<String, Integer> entry : keywordsHM.entrySet()) {  
	            if (entry.getValue()==maxValueInMap) {
	                //System.out.println("The highest occurred word: "+ entry.getKey());     // ---- Print the key with max value
	                jsonResponse+="\""+entry.getKey()+"\"}";
	    			response.getWriter().append(jsonResponse);
	    			response.getWriter().flush();
	    			return;
	            }
	        }
		}

		else
		{
			if(titleFound)
			{
				List<String> titleWordsList = new ArrayList<String>();
				titleWordsList = (List<String>) Arrays.asList(titleRequired.split(" "));
				
				for(String keyWord : titleWordsList)
				{
					int count = 1;
					keyWord = keyWord.replace(",", "");
					keyWord = keyWord.replace(".", "");
					keyWord = keyWord.replace(":", "");
					keyWord = keyWord.replace("*", "");
					keyWord = keyWord.toLowerCase();
					boolean frequentWord = checkIfFrequentWord(frequentWords,keyWord);
					if(!frequentWord)
					{
						if(keywordsHM.get(keyWord)==null)
						{
							keywordsHM.put(keyWord,count);
						}
						else
						{
							int cnt = keywordsHM.get(keyWord);
							keywordsHM.put(keyWord, ++cnt);
						}
					}
					
				}
				
				for(String kw: keywordsHM.keySet())
				{
					if(queryString.contains(kw))
					{
						int value = keywordsHM.get(kw);
						keywordsHM.put(kw, value+4);
					}
				}
				
				if(!keywordsHM.isEmpty())
				{
					int maxValueInMap=(Collections.max(keywordsHM.values()));  // This will return max value in the Hashmap
			        for (Entry<String, Integer> entry : keywordsHM.entrySet()) {  
			            if (entry.getValue()==maxValueInMap) {
			                //System.out.println("The highest occurred word: "+ entry.getKey());     // ---- Print the key with max value in the title
			                jsonResponse+="\""+entry.getKey()+"\"}";
			    			response.getWriter().append(jsonResponse);
			    			response.getWriter().flush();
			    			return;
			            }
			        }
				}
				
			}
		}
	}
	catch(Exception e) {
		response.getWriter().append("Error:" + e.toString());
		response.getWriter().flush();
	}
}
    
  private boolean checkIfFrequentWord(String[] frequentWords,String keyWord)
	{
		for(String item : frequentWords) {
		    if(item.contains(keyWord))
		       return true;
		}
		if(keyWord.matches("-?\\d+(\\.\\d+)?"))
		{
			return true;
		}
		return false;
	}

  public static void main(String[] args) throws Exception {
    Server server = new Server(Integer.valueOf(System.getenv("PORT")));
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new Main()),"/*");
    server.start();
    server.join();
  }
}
