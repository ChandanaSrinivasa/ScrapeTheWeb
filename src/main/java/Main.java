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

    private static Logger logger = Logger.getLogger("GetURLServlet");

    public static Set<String> stopWords = new HashSet<String>();
    private static CRFClassifier<CoreLabel> segmenter;

    // load stop words to stopWords
    private static Set<String> loadStopWords() {
        try {
            logger.info("loading StopWords...");
            File file = new File(".");
            return new HashSet<String>(Files.readAllLines(Paths.get(file.getCanonicalPath(), "stopwords.txt"), StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("error when load stop words");
        }
        return null;
    }

    // replace punctuations with spaces
    private String removeSpecialCharacters(String word) {
        word = word.replace(",", "");
        word = word.replace(".", "");
        word = word.replace(":", "");
        word = word.replace("*", "");
        word = word.replaceAll("[&].{2,6}[;]", "");
        word = word.toLowerCase();
        return word;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String finalQuery = "";
        String url = request.getParameter("url");

        if (url != null) {
            //Get the HTML Body
            Connection con = Jsoup.connect(url);
            con.userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:38.0) Gecko/20100101 Firefox/38.0");
            Document doc = con.get();
            Element body = doc.body();
            String txtBody = body.text().toLowerCase();
            logger.info("====original body=====" + txtBody);

            txtBody = txtBody.replaceAll("[&].{2,6}[;]", " ");
            txtBody = txtBody.replaceAll("\\p{P}", " ");

            // token words
            List<String> tokenWords = segmenter.segmentString(txtBody);

            //Remove the frequest words from the body
            StringBuilder newBody = new StringBuilder();

            for (String word : tokenWords) {
                if (!stopWords.contains(word)) {
                    newBody.append(word);
                    newBody.append(" ");
                }
            }

            txtBody = newBody.toString();

            logger.info("====without stopwords=====" + txtBody);

            //Get top 5 used keywords
            String[] topUsedWords = returnNumKeywords(txtBody, 5);
            HashMap<String, Integer> keywordList = new HashMap<String, Integer>();

            for (String txt : topUsedWords) {
                logger.info("=====top Used Words=====" + txt);
                keywordList.put(txt, 1);
            }

            //HTML Doc title
            String title = doc.title();

            //Meta - Description of the page
            Elements metaLinksForDescription = doc.select("meta[name=description]");
            List<String> metaDescriptionList = new ArrayList<String>();

            String metaTagDescriptionContent;

            if (!metaLinksForDescription.isEmpty()) {
                metaTagDescriptionContent = metaLinksForDescription.first().attr("content");
                metaDescriptionList = Arrays.asList(metaTagDescriptionContent.split(" "));
            }

            //Meta - Keyword of the page
            Elements metaLinksForKeywords = doc.select("meta[name=keywords]");
            List<String> metaKeywordsList = new ArrayList<String>();

            String metaTagKeywordscontent;

            if (!metaLinksForKeywords.isEmpty()) {
                metaTagKeywordscontent = metaLinksForKeywords.first().attr("content");
                metaKeywordsList = Arrays.asList(metaTagKeywordscontent.split(","));
            }

            //Figure out the top most keyword now
            for (Object o : keywordList.entrySet()) {
                Map.Entry pair = (Map.Entry) o;

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

    private boolean checkIfFrequentWord(String keyWord) {
        if (keyWord.matches("-?\\d+(\\.\\d+)?")) {
            return true;
        }

        for (String item : stopWords) {
            if (item.equals(keyWord)) {
                return true;
            }
        }

        return false;
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
        DefaultTagger tagger = new DefaultTagger();

        //for default lexicon POS tags
        Configuration.setTaggerType(tagger.TAGGER_TYPE);
        Configuration.setSingleStrength(3);
        Configuration.setNoLimitStrength(2);
        Configuration.setModelFileLocation(tagger.TAGGER_LOCATION);

        TermsExtractor termExtractor = new TermsExtractor();
        TermDocument topiaDoc = new TermDocument();

        topiaDoc = termExtractor.extractTerms(body);

        Map<String, Integer> finalTerms = topiaDoc.getExtractedTerms();
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
            if (!term.matches("[0-9]+")) {
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


    private static void startNLP() throws IOException {
        File file = new File(".");
        String path = "https://drive.google.com/open?id=0B4OaZL2O5H03fnFmMFRsOVFZbVpuSkxieEljXzl2QUh6ZTNBei1heUgyQkRkMUt0TkFEbVU";

        Properties props = new Properties();
        props.setProperty("sighanCorporaDict", path);
        props.setProperty("serDictionary", path + "/dict-chris6.ser.gz");
        props.setProperty("inputEncoding", "UTF-8");
        props.setProperty("sighanPostProcessing", "true");

        segmenter = new CRFClassifier<CoreLabel>(props);
        segmenter.loadClassifierNoExceptions(path + "/ctb.gz", props);
    }

    public static void main(String[] args) throws Exception {
        logger.setLevel(Level.INFO);            //Set the logging level here

        logger.info("loading stopwords...");
        stopWords = loadStopWords();
        logger.info("...stopwords loaded");
        logger.info("starting NLP...");
        startNLP();
        logger.info("...NLP started");

        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new Main()), "/*");
        server.start();
        server.join();
    }
}
