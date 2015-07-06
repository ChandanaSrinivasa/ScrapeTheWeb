import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sree.textbytes.jtopia.Configuration;
import com.sree.textbytes.jtopia.TermDocument;
import com.sree.textbytes.jtopia.TermsExtractor;

public class Main extends HttpServlet {

    private static Logger logger = Logger.getLogger("GetURLServlet");

    public static String frequentWords[] =
            {
                    " the ", " be ", " to ", " of ", " and ", " a ", " in ", " that ", " have ",
                    " i ", " it ", " for ", " not ", " on ", " with ", " he ", " as ", " you ",
                    " do ", " at ", " this ", " but ", " his ", " by ", " from ", " they ", " we ",
                    " say ", " her ", " she ", " or ", " an ", " will ", " my ", " one ", " all ",
                    " would ", " there ", " their ", " what ", " so ", " up ", " out ", " if ",
                    " about ", " who ", " get ", " which ", " go ", " me ", " when ", " make ",
                    " can ", " like ", " time ", " no ", " just ", " him ", " no ", " take ",
                    " people ", " into ", " year ", " your ", " good ", " some ", " could ",
                    " them ", " see ", " other ", " than ", " then ", " now ", " look ", " only ",
                    " come ", " its ", " over ", " think ", " also ", " back ", " after ", " use ",
                    " two ", " how ", " our ", " work ", " first ", " well ", " way ", " even ",
                    " new ", " want ", " because ", " any ", " these ", " give ", " day ", " most ", " us "
            };

    private String removeSpecialCharacters(String word)
    {
        word = word.replace(",", "");
        word = word.replace(".", "");
        word = word.replace(":", "");
        word = word.replace("*", "");
        word = word.replaceAll("[&]{1}.+[;]{1}", "");
        word = word.toLowerCase();
        return word;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String finalQuery = "";
        String url = request.getParameter("url");

        if (url != null) {
            //Get the HTML Body
            Connection con = Jsoup.connect(url);
            con.userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:38.0) Gecko/20100101 Firefox/38.0");
            Document doc = con.get();
            Element body = doc.body();
            String txtBody = body.text().toLowerCase();

            logger.info("=========" + txtBody);

            //Remove the frequest words from the body
            for (String delWords: frequentWords) {
                txtBody = txtBody.replace(delWords, " ");
            }

            logger.info("=========" + txtBody);

            //Get top 5 used keywords
            String[] topUsedWords = returnNumKeywords(txtBody, 5);
            HashMap<String, Integer> keywordList = new HashMap<String, Integer>();

            for (String txt: topUsedWords) {
                logger.info("==========" + txt);
                keywordList.put(txt, 1);
            }

            //HTML Doc title
            String title = doc.title();

            //Meta - Description of the page
            Elements metaLinksForDescription = doc.select("meta[name=description]");
            List<String> metaDescriptionList = new ArrayList<String>();

            String metaTagDescriptionContent = "";

            if (!metaLinksForDescription.isEmpty())
            {
                metaTagDescriptionContent = metaLinksForDescription.first().attr("content");
                metaDescriptionList = (List<String>) Arrays.asList(metaTagDescriptionContent.split(" "));
            }

            //Meta - Keyword of the page
            Elements metaLinksForKeywords = doc.select("meta[name=keywords]");
            List<String> metaKeywordsList = new ArrayList<String>();

            String metaTagKeywordscontent = "";

            if (!metaLinksForKeywords.isEmpty())
            {
                metaTagKeywordscontent = metaLinksForKeywords.first().attr("content");
                metaKeywordsList = (List<String>) Arrays.asList(metaTagKeywordscontent.split(","));
            }

            //Figure out the top most keyword now
            Iterator it = keywordList.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry pair = (Map.Entry) it.next();

                String term = (String) pair.getKey();
                int count = (Integer) pair.getValue();

                if (url.toLowerCase().contains(term)) {
                    count += 10;
                    logger.info("========" + term + " in URL");
                }

                if (title.toLowerCase().contains(term)) {
                    count += 5;
                    logger.info("========" + term + " in Title");
                }

                for (String metaDescription : metaDescriptionList) {
                    if (metaDescription.toLowerCase().contains(term)) {
                        count += 5;
                        logger.info("========" + term + " in Meta Desc");
                        break;
                    }
                }

                for (String metaKeyword : metaKeywordsList) {
                    if (metaKeyword.toLowerCase().contains(term)) {
                        count += 5;
                        logger.info("========" + term + " in Meta Key");
                        break;
                    }
                }

                keywordList.put(term, count);       //Give score 5 to title

                logger.info("==========Final Value:" + term + ":" + count);
            }

            int NUM_KEYWORDS = 3;
            int i = 0;

            String[] finalKeywords = new String[NUM_KEYWORDS];
            int[] finalKeywordsScore = new int[NUM_KEYWORDS];

            Iterator sortIT = keywordList.entrySet().iterator();

            logger.info("========Sorted List Count:" + keywordList.size());

            while (sortIT.hasNext())
            {
                Map.Entry pair = (Map.Entry) sortIT.next();

                String term = (String)pair.getKey();
                int score = (Integer)pair.getValue();

                if (i < NUM_KEYWORDS) {
                    finalKeywords[i] = term;
                    finalKeywordsScore[i] = score;
                } else {
                    for (int j = 0 ; j < NUM_KEYWORDS ; j++) {
                        if (finalKeywordsScore[j] < score) {
                            finalKeywords[j] = term;
                            finalKeywordsScore[j] = score;
                            break;
                        }
                    }
                }
                i++;
            }

            for (int j = 0 ; j < NUM_KEYWORDS ; j++) {
                finalQuery += finalKeywords[j];

                if (j+1 != NUM_KEYWORDS) {
                    finalQuery += ",";
                }
            }
        }

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();
        out.append("{\"query\":\"");
        out.append(finalQuery);
        out.append("\"}");
        out.flush();
    }

    /*
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");

        String url = request.getParameter("url");

        if (null == url)
        {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().append("{\"query\": \"\"}");
            response.getWriter().flush();
            return;
        }

        // Get the HTML Document using JSOUP
        Connection con = Jsoup.connect(url);
        con.userAgent("Mozilla");
        Document doc = con.get();

        Elements body = doc.select("body");
        String[] topUsedWords = returnNumKeywords(body.text(), 5);     //Get top 5 used keywords from the HTML Text

        String topUsedPhrase = "";
        for (String str : topUsedWords)
        {
            topUsedPhrase = topUsedPhrase + str + " ";
        }

        boolean titleFound = false;
        String titleRequired = "";
        String title = doc.title();

        if (!title.isEmpty())
        {
            titleRequired = title.replaceAll("[&]{1}.+[;]{1}", "");    //To replace HTML Codes like &#132; with ""
            titleFound = true;
        }

        Elements metaLinksForDescription = doc.select("meta[name=description]");
        boolean metaDescriptionFound = false;
        List<String> descriptionList = new ArrayList<String>();

        String metaTagDescriptionContent = "";

        if (!metaLinksForDescription.isEmpty())
        {
            metaTagDescriptionContent = metaLinksForDescription.first().attr("content");
            descriptionList = (List<String>) Arrays.asList(metaTagDescriptionContent.split(" "));
            metaDescriptionFound = true;
        }

        Elements metaLinksForKeywords = doc.select("meta[name=keywords]");
        boolean metaKeywordFound = false;
        List<String> keyWordsList = new ArrayList<String>();

        String metaTagKeywordscontent = "";

        if (!metaLinksForKeywords.isEmpty())
        {
            metaTagKeywordscontent = metaLinksForKeywords.first().attr("content");
            keyWordsList = (List<String>) Arrays.asList(metaTagKeywordscontent.split(","));
            metaKeywordFound = true;
        }

        HashMap<String, Integer> keywordsHM = new HashMap<String, Integer>();

        if (metaDescriptionFound)
        {
            for (String keyWord : descriptionList)
            {
                keyWord = removeSpecialCharacters(keyWord);
                boolean frequentWord = checkIfFrequentWord(keyWord);
                if (!frequentWord)
                {
                    Integer count = keywordsHM.get(keyWord);
                    if (count == null)
                    {
                        keywordsHM.put(keyWord, 1);
                    }
                    else
                    {
                        keywordsHM.put(keyWord, ++count);
                    }
                }
            }
        }

        if (metaKeywordFound)
        {
            for (String keyWord : keyWordsList)
            {
                keyWord = removeSpecialCharacters(keyWord);
                boolean frequentWord = checkIfFrequentWord(keyWord);
                if (!frequentWord)
                {
                    Integer count = keywordsHM.get(keyWord);
                    if (count == null)
                    {
                        keywordsHM.put(keyWord, 1);
                    }
                    else
                    {
                        keywordsHM.put(keyWord, ++count);
                    }
                }
            }
        }

        for (String kw: keywordsHM.keySet())
        {
            if (titleFound)
            {
                if (titleRequired.toLowerCase().contains(kw))
                {
                    keywordsHM.put(kw, keywordsHM.get(kw) + 2);     //Add 2 for keyword in title
                }
            }

            if (url.contains(kw))
            {
                keywordsHM.put(kw, keywordsHM.get(kw) + 4);         //Add 4 for keyword in URL
            }
        }

        if (!keywordsHM.isEmpty())
        {
            for (Entry<String, Integer> entry : keywordsHM.entrySet())
            {
                for (String str : topUsedWords)
                {
                    if (entry.getKey().equals(str))
                    {
                        keywordsHM.put(entry.getKey(), entry.getValue() + 1);     //Add 1 since this is most used keyword in the page
                    }
                }
            }

            int maxValueInMap = (Collections.max(keywordsHM.values()));  // This will return max value in the Hashmap

            for (Entry<String, Integer> entry : keywordsHM.entrySet())
            {
                if (entry.getValue() == maxValueInMap)
                {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().append("{\"query\": \"");
                    response.getWriter().append(entry.getKey());
                    response.getWriter().append("\"}");
                    response.getWriter().flush();
                    return;
                }
            }
        }
        else              //IF META TAGS NOT PRESENT THEN
        {
          if (titleFound)
          {
              List<String> titleWordsList = new ArrayList<String>();
              titleWordsList = (List<String>) Arrays.asList(titleRequired.split(" "));

              for (String keyWord : titleWordsList)
              {
                  int count = 1;

                  keyWord = removeSpecialCharacters(keyWord);

                  boolean frequentWord = checkIfFrequentWord(keyWord);

                  if (!frequentWord)
                  {
                      if (keywordsHM.get(keyWord)==null)
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

              for (String kw: keywordsHM.keySet())
              {
                  if (url.contains(kw))
                  {
                      int value = keywordsHM.get(kw);
                      keywordsHM.put(kw, value+4);
                  }
              }

              if (!keywordsHM.isEmpty())
              {
                  for (Entry<String, Integer> entry : keywordsHM.entrySet())
                  {
                      for (String str : topWords)
                      {
                          if (entry.getKey()==str)
                          {
                              int value = entry.getValue();
                              keywordsHM.put(entry.getKey(), value+5);
                          }
                      }
                  }

                  int maxValueInMap = (Collections.max(keywordsHM.values()));  // This will return max value in the Hashmap

                  for (Entry<String, Integer> entry : keywordsHM.entrySet())
                  {
                      if (entry.getValue()==maxValueInMap)
                      {
                          jsonResponse += "\"" + entry.getKey() + "\"}";
                          response.getWriter().append(jsonResponse);
                          response.getWriter().flush();
                          return;
                      }
                  }
              }
          }
        }
    }
    */
    
    private boolean checkIfFrequentWord(String keyWord)
    {
        if (keyWord.matches("-?\\d+(\\.\\d+)?"))
        {
            return true;
        }

        for (String item : frequentWords)
        {
            if (item.equals(keyWord))
            {
                return true;
            }
        }

        return false;
    }

    public class DefaultTagger
    {
        public String TAGGER_TYPE = "default";
        public String TAGGER_LOCATION = "/model/default/english-lexicon.txt";
    }

    public class OpenNLPTagger extends DefaultTagger
    {
        public String TAGGER_TYPE = "openNLP";
        public String TAGGER_LOCATION = "/model/openNLP/en-pos-maxent.bin";
    }

    public class StanfordTagger extends DefaultTagger
    {
        public String TAGGER_TYPE = "default";
        public String TAGGER_LOCATION = "/model/stanford/english-left3words-distsim.tagger";
    }

    private String[] returnNumKeywords(String body, int num)
    {
        DefaultTagger tagger = new DefaultTagger();

        //for default lexicon POS tags
        Configuration.setTaggerType(tagger.TAGGER_TYPE);
        Configuration.setSingleStrength(3);
        Configuration.setNoLimitStrength(2);
        Configuration.setModelFileLocation(tagger.TAGGER_LOCATION);

        TermsExtractor termExtractor = new TermsExtractor();
        TermDocument topiaDoc = new TermDocument();

        topiaDoc = termExtractor.extractTerms(body);

        Map<String,Integer> finalTerms = topiaDoc.getExtractedTerms();
        Iterator it = finalTerms.entrySet().iterator();

        String[] topTerms = new String[num];
        int[] topTermCount = new int[num];
        int fistTotalTerms = 0;

        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry)it.next();

            String term = (String)pair.getKey();
            int count = (Integer)pair.getValue();

            term = removeSpecialCharacters(term);

            //If its only numbers then ignore it
            if (!term.matches("[0-9]+"))
            {
                if (fistTotalTerms < num)
                {
                    topTerms[fistTotalTerms] = term;
                    topTermCount[fistTotalTerms] = count;
                    fistTotalTerms++;
                }
                else
                {
                    for (int i = 0; i < num; i++)
                    {
                        if (topTermCount[i] < count)
                        {
                            topTerms[i] = term;
                            topTermCount[i] = count;
                            break;
                        }
                    }
                }
            }
            it.remove(); // avoids a ConcurrentModificationException
        }

        String topWords[] = new String[num];
        for (int i = 0 ; i < num ; i++)
        {
            topWords[i] = topTerms[i];
        }

        return topWords;
    }

    public static void main(String[] args) throws Exception
    {
        logger.setLevel(Level.INFO);            //Set the logging level here

        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new Main()),"/*");
        server.start();
        server.join();
    }
}
