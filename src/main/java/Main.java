import java.io.File;
import java.io.IOException;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public static String frequentWords[] = {
            " need ", " fired ", " add ", " after ", " saw ", " chief ", " awesome ", " pay ", " grows ", " i ",
            " consider ", " | ", " from ", " every ", " you ", " people ", " updates ", " i ",
            " get ", " fascinating ", " friends ", " your ", " connect ", " login ", " other ", " others ",
            " sure ", " and ", " at ", " be ", " but ", " by ", " if ", " into ", " it ", " no ", " not ", " of ",
            " or ", " such ", " an ", " the ", " a ", " their ", " then ", " there ", " these ", " this ", " to ",
            " was ", " will ", " with ", " so ", " also ", " that ", " they ", " therefore ", " for ", " much ",
            " more ", " hence ", " is ", " are ", " why ", " what ", " how ", " as ", " on ", " in ", " like ", " am ", " pm "
    };

    private static Logger logger = Logger.getLogger("GetURLServlet");

    //    public static Set<String> stopWords = new HashSet<String>();
    //    private static CRFClassifier<CoreLabel> segmenter;

    //    // load stop words to stopWords --- For Chinese
    //    private static Set<String> loadStopWords() {
    //        try {
    //            logger.info("loading StopWords...");
    //            File file = new File(".");
    //            return new HashSet<String>(Files.readAllLines(Paths.get(file.getCanonicalPath(), "stopwords.txt"), StandardCharsets.UTF_8));
    //        } catch (IOException e) {
    //            System.err.println("error when load stop words");
    //        }
    //        return null;
    //    }

    // replace punctuations with spaces
    private String removeSpecialCharacters(String word) {
        word = word.replace(",", "");
        word = word.replace(".", "");
        word = word.replace(":", "");
        word = word.replace("*", "");
        word = word.replaceAll(" ", "");
        word = word.replaceAll("[&].{2,6}[;]", "");
        word = word.toLowerCase();
        return word;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String finalQuery = "";
        String url = request.getParameter("url");

        if (url != null) {

            Connection con = Jsoup.connect(url);
            con.userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:38.0) Gecko/20100101 Firefox/38.0");
            Document doc = con.get();


            if (url.endsWith("/")) // IF THE URL IS http://twitter.com/ THEN REMOVE THE LAST / AND MAKE IT http://twitter.com
            {
                url = url.substring(0, url.length() - 1);
            }

            if (url.contains("yelp.com")) {
                finalQuery = yelpSiteKeyWords(url, doc);
            } else if (url.contains("cnn.com") || url.contains("wikipedia.org")) {
                finalQuery = titleExtractor(url, doc);

            } else if (url.contains("imdb.com")) {
                if (doc.select("meta[property=og:title]").attr("content") != "") {
                    finalQuery = doc.select("meta[property=og:title]").attr("content");
                } else {
                    finalQuery = doc.title().trim();
                }
            } else if (url.contains("amazon.com")) {
                finalQuery = titleExtractor(url, doc);
            } else {
                if (url.lastIndexOf('/') <= 7) // If the URL is simple like http://twitter.com or http://wikipedia.com then return just THE TITLE of the document
                {
                    String title = doc.title();
                    title.replaceAll("[&].{2,6}[;]", " ");
                    finalQuery = title;
                }
                //Get the HTML Body
                else {
                    Element body = doc.body();
                    String txtBody = body.text().toLowerCase();
                    logger.info("====original body=====" + txtBody);

                    //Remove the frequest words from the body
                    for (String delWords: frequentWords) {
                        txtBody = txtBody.replace(delWords, " ");
                    }

                    txtBody = txtBody.replaceAll("[&].{2,6}[;]", " ");
                    txtBody = txtBody.replaceAll("\\p{P}", " ");

                    // token words ----- FOR Chinese Words
                    //            List<String> tokenWords = segmenter.segmentString(txtBody);
                    //
                    //            //Remove the frequest words from the body
                    //            StringBuilder newBody = new StringBuilder();
                    //
                    //            for (String word : tokenWords) {
                    //                if (!stopWords.contains(word)) {
                    //                    newBody.append(word);
                    //                    newBody.append(" ");
                    //                }
                    //            }
                    //          txtBody = newBody.toString();

                    logger.info("====without stopwords=====" + txtBody);

                    //Get top 5 used keywords
                    String[] topUsedWords = returnNumKeywords(txtBody, 5);
                    HashMap < String, Integer > keywordList = new HashMap < String, Integer > ();

                    for (String txt: topUsedWords) {
                        logger.info("=====top Used Words=====" + txt);
                        keywordList.put(txt, 1);
                    }

                    //HTML Doc title
                    String title = doc.title();

                    //Meta - Description of the page
                    Elements metaLinksForDescription = doc.select("meta[name=description]");
                    List < String > metaDescriptionList = new ArrayList < String > ();

                    String metaTagDescriptionContent;

                    if (!metaLinksForDescription.isEmpty()) {
                        metaTagDescriptionContent = metaLinksForDescription.first().attr("content");
                        metaDescriptionList = Arrays.asList(metaTagDescriptionContent.split(" "));
                    }

                    //Meta - Keyword of the page
                    Elements metaLinksForKeywords = doc.select("meta[name=keywords]");
                    List < String > metaKeywordsList = new ArrayList < String > ();

                    String metaTagKeywordscontent;

                    if (!metaLinksForKeywords.isEmpty()) {
                        metaTagKeywordscontent = metaLinksForKeywords.first().attr("content");
                        metaKeywordsList = Arrays.asList(metaTagKeywordscontent.split(","));
                    }

                    //Figure out the top most keyword now
                    for (Object o: keywordList.entrySet()) {
                        Map.Entry pair = (Map.Entry) o;

                        String term = ((String) pair.getKey()).toLowerCase();
                        int count = (Integer) pair.getValue();

                        if (url.toLowerCase().contains(term)) {
                            count += 10;
                            logger.info("========" + term + " in URL");
                        }

                        if (title.toLowerCase().contains(term)) {
                            count += 10;
                            logger.info("========" + term + " in Title");
                        }

                        for (String metaDescription: metaDescriptionList) {
                            if (metaDescription.toLowerCase().contains(term)) {
                                count += 5;
                                logger.info("========" + term + " in Meta Desc");
                                break;
                            }
                        }

                        for (String metaKeyword: metaKeywordsList) {
                            if (metaKeyword.toLowerCase().contains(term)) {
                                count += 5;
                                logger.info("========" + term + " in Meta Key");
                                break;
                            }
                        }

                        keywordList.put(term, count); //Give score 5 to title

                        logger.info("==========Final Value:" + term + ":" + count);
                    }

                    int NUM_KEYWORDS = 3;
                    int i = 0;

                    String[] finalKeywords = new String[NUM_KEYWORDS];
                    int[] finalKeywordsScore = new int[NUM_KEYWORDS];

                    Iterator sortIT = keywordList.entrySet().iterator();

                    logger.info("========Sorted List Count:" + keywordList.size());

                    while (sortIT.hasNext()) {
                        Map.Entry pair = (Map.Entry) sortIT.next();

                        String term = (String) pair.getKey();
                        int score = (Integer) pair.getValue();

                        if (i < NUM_KEYWORDS) {
                            finalKeywords[i] = term;
                            finalKeywordsScore[i] = score;
                        } else {
                            for (int j = 0; j < NUM_KEYWORDS; j++) {
                                if (finalKeywordsScore[j] < score) {
                                    finalKeywords[j] = term;
                                    finalKeywordsScore[j] = score;
                                    break;
                                }
                            }
                        }
                        i++;
                    }

                    for (int j = 0; j < NUM_KEYWORDS; j++) {
                        finalQuery += finalKeywords[j];

                        if (j + 1 != NUM_KEYWORDS) {
                            finalQuery += "+";
                        }
                    }
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

    public class DefaultTagger {
        public String TAGGER_TYPE = "default";
        public String TAGGER_LOCATION = "/model/default/english-lexicon.txt";
    }

    public class OpenNLPTagger extends DefaultTagger {
        public String TAGGER_TYPE = "openNLP";
        public String TAGGER_LOCATION = "/model/openNLP/en-pos-maxent.bin";
    }

    public class StanfordTagger extends DefaultTagger {
        public String TAGGER_TYPE = "default";
        public String TAGGER_LOCATION = "/model/stanford/english-left3words-distsim.tagger";
    }

    private String[] returnNumKeywords(String body, int num) {
        OpenNLPTagger tagger = new OpenNLPTagger();

        //for default lexicon POS tags
        Configuration.setTaggerType(tagger.TAGGER_TYPE);
        Configuration.setSingleStrength(3);
        Configuration.setNoLimitStrength(2);
        Configuration.setModelFileLocation(tagger.TAGGER_LOCATION);

        TermsExtractor termExtractor = new TermsExtractor();
        TermDocument topiaDoc = new TermDocument();

        topiaDoc = termExtractor.extractTerms(body);

        Map < String, Integer > finalTerms = topiaDoc.getExtractedTerms();
        Iterator it = finalTerms.entrySet().iterator();

        String[] topTerms = new String[num];
        int[] topTermCount = new int[num];
        int fistTotalTerms = 0;

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            String term = (String) pair.getKey();
            int count = (Integer) pair.getValue();

            term = removeSpecialCharacters(term);

            //If its only numbers then ignore it
            if (!term.matches("-?\\d+(\\.\\d+)?") && term.matches("[A-z]+")) {
                if (fistTotalTerms < num) {
                    topTerms[fistTotalTerms] = term;
                    topTermCount[fistTotalTerms] = count;
                    fistTotalTerms++;
                } else {
                    for (int i = 0; i < num; i++) {
                        if (topTermCount[i] < count) {
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
        System.arraycopy(topTerms, 0, topWords, 0, num);

        return topWords;
    }

    private String yelpSiteKeyWords(String url, Document doc) {
        String query = "";
        if (url.endsWith(".com")) {
            query = "yelp";
        } else if ((url.indexOf("com/") + 3) == url.lastIndexOf('/') && url.indexOf('?') == -1) {
            String kw = url.substring(url.lastIndexOf('/') + 1, url.length());
            query = "searching for restaurants in " + kw.replaceAll("-", " ");
        } else if (url.indexOf("cflt") > -1) {
            String kw = url.substring(url.indexOf("cflt") + 5, url.indexOf('&'));
            query = "search for business category " + kw;
        } else if (url.indexOf("biz") > -1) {
            String title = doc.title();
            String category = doc.select("span[itemprop=title]").text();
            String streetAddress = doc.select("span[itemprop=streetAddress]").text();
            String addressLocality = doc.select("span[itemprop=addressLocality]").text();
            String addressRegion = doc.select("span[itemprop=addressRegion]").text();
            String postalCode = doc.select("span[itemprop=postalCode]").text();

            query = "taxi for " + title.substring(0, title.indexOf('-') - 1);
        } else if (url.indexOf("menu") > -1) {
            if (url.indexOf("item") > -1) {
                String recipes = url.substring(url.lastIndexOf('/') + 1, url.length());
                query = "recipes of " + recipes.replace("-", " ");
            } else {
                query = doc.title().replaceFirst("-", "Restaurant").replaceFirst("-", "in");
            }

        } else if (url.indexOf("find_desc") > -1 && url.indexOf("find_loc") > -1) {
            String findDesc = doc.select("span[class=find-desc]").text();
            String findLoc = doc.select("span[class=find-loc]").text();
            query = "searching for " + findDesc.trim() + " near " + findLoc.trim();
        }

        return query;
    }

    private String titleExtractor(String url, Document doc) {
        String pageTitle = doc.title();
        String query = "";
        if (url.contains("amazon.com")) {
            if (pageTitle.indexOf(':') > -1) {
                query = pageTitle.substring(pageTitle.indexOf(':') + 1, pageTitle.length()).trim();
            }
        } else {
            if (pageTitle.indexOf('-') > -1) {
                query = pageTitle.substring(0, pageTitle.indexOf('-')).trim();
            }
        }
        if (query == "") {
            query = doc.title().trim();
        }
        return query;
    }


    //    private static void startNLP() throws IOException {   -- for chinese words
    //        File file = new File(".");
    //        String path = file.getCanonicalPath() + "/data";
    //
    //        Properties props = new Properties();
    //        props.setProperty("sighanCorporaDict", path);
    //        props.setProperty("serDictionary", path + "/dict-chris6.ser.gz");
    //        props.setProperty("inputEncoding", "UTF-8");
    //        props.setProperty("sighanPostProcessing", "true");
    //
    //        segmenter = new CRFClassifier<CoreLabel>(props);
    //        segmenter.loadClassifierNoExceptions(path + "/ctb.gz", props);
    //    }

    public static void main(String[] args) throws Exception {
        //        logger.setLevel(Level.INFO);            //Set the logging level here -- For Chinese Words
        //
        //        logger.info("loading stopwords...");
        //        stopWords = loadStopWords();
        //        logger.info("...stopwords loaded");
        //        logger.info("starting NLP...");
        //        startNLP();
        //        logger.info("...NLP started");

        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new Main()), "/*");
        server.start();
        server.join();
    }
}