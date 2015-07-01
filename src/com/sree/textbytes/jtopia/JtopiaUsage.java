package com.sree.textbytes.jtopia;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class JtopiaUsage {
	public static Logger logger = Logger.getLogger(JtopiaUsage.class.getName());
	public static void main( String[] args )
    {
		//for default lexicon POS tags
		Configuration.setTaggerType("default"); 
		// for openNLP POS tagger
		//Configuration.setTaggerType("openNLP");
		//for Stanford POS tagger
		//Configuration.setTaggerType("stanford"); 
		Configuration.setSingleStrength(3);
		Configuration.setNoLimitStrength(2);
		// if tagger type is "openNLP" then give the openNLP POS tagger path
		//Configuration.setModelFileLocation("model/openNLP/en-pos-maxent.bin"); 
		// if tagger type is "default" then give the default POS lexicon file
		Configuration.setModelFileLocation("model/default/english-lexicon.txt");
		// if tagger type is "stanford "
		//Configuration.setModelFileLocation("model/stanford/english-left3words-distsim.tagger");
		
        TermsExtractor termExtractor = new TermsExtractor();
        TermDocument topiaDoc = new TermDocument();
        
        StringBuffer stringBuffer = new StringBuffer();
        
        FileInputStream fileInputStream = null;
		BufferedReader bufferedReader = null;
		try {
			fileInputStream = new FileInputStream("example.txt");
		} catch (FileNotFoundException e) {
		}

		DataInputStream dataInputStream = new DataInputStream(fileInputStream);
		bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));
		String line = "";
		try {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuffer.append(line+"\n");
			}
		} catch (IOException e) {
			
		}
        
		topiaDoc = termExtractor.extractTerms(stringBuffer.toString());
		//logger.info("Extracted terms : "+topiaDoc.getExtractedTerms());
		//logger.info("Final Filtered Terms : "+topiaDoc.getFinalFilteredTerms());

		Map<String,Integer> finalTerms = topiaDoc.getExtractedTerms();
		Iterator it = finalTerms.entrySet().iterator();

		final int TOTAL_TERMS_OUTPUT = 5;

		String[] topTerms = new String[TOTAL_TERMS_OUTPUT];
		int[] topTermCount = new int[TOTAL_TERMS_OUTPUT];

		int fistTotalTerms = 0;

		while (it.hasNext()) {
        	Map.Entry pair = (Map.Entry)it.next();
        	//logger.info(pair.getKey() + " = " + pair.getValue());

        	if (fistTotalTerms < TOTAL_TERMS_OUTPUT) {
        		topTerms[fistTotalTerms] = (String)pair.getKey();
        		topTermCount[fistTotalTerms] = (Integer)pair.getValue();
        		fistTotalTerms++;
        	}
        	else {
        		for (int i = 0 ; i < TOTAL_TERMS_OUTPUT ; i++) {
        			if (topTermCount[i] < (Integer)pair.getValue()) {
        				topTerms[i] = (String)pair.getKey();
        				topTermCount[i] = (Integer)pair.getValue();
        				break;
        			}
        		}
        	}

        	it.remove(); // avoids a ConcurrentModificationException
    	}

    	for (int i = 0 ; i < TOTAL_TERMS_OUTPUT ; i++) {
    		logger.info(topTerms[i] + " = " + topTermCount[i]);
    	}
    }


}
