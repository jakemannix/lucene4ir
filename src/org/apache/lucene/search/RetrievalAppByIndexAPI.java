package org.apache.lucene.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXB;

/**
 * Created by dibuccio on 09/09/16.
 */
public class RetrievalAppByIndexAPI {

    public RetrievalParams p;

    private Similarity simfn;
    private IndexReader reader;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private QueryParser parser;
    private LMSimilarity.CollectionModel colModel;

    public RetrievalAppByIndexAPI(String retrievalParamFile){
        System.out.println("Retrieval App");
        readParamsFromFile(retrievalParamFile);
        try {
            reader = DirectoryReader.open(FSDirectory.open( new File(p.indexName).toPath()) );
            searcher = new IndexSearcher(reader);

            // create similarity function and parameter
            searcher.setSimilarity(simfn);
            analyzer = new StandardAnalyzer();

            parser = new QueryParser("content", analyzer);


        } catch (Exception e){
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    public ScoreDoc[] runQuery(String qno, String queryTerms){

        float k1 = p.k;
        float b = p.b;
        float k3 = Float.POSITIVE_INFINITY;

        int numHits = 1000;

        String field = "content";

        ScoreDoc[] hits = new ScoreDoc[0];

        int N = reader.numDocs(); // should I use numDocs OR maxDocs?

        System.out.println("Query No.: " + qno + " " + queryTerms);
        try {

            List<String> queryTokens = getTokens(field, queryTerms);

            int[] qtf = new int[queryTokens.size()];
            for (int qt = 0; qt < queryTokens.size(); qt++) {
                qtf[qt] = 1;
            }

            //

            final double avgDocLength = 1.0d
                    * reader.getSumTotalTermFreq(field) / N;

            //

            final double[] IDFs = new double[queryTokens.size()];

            Term[] qTerms = new Term[queryTokens.size()];

            for (int qt = 0; qt < queryTokens.size(); qt++) {

                Term qTerm = new Term(field, queryTokens.get(qt));

                int df = reader.docFreq(qTerm);

                IDFs[qt] = Math.log(N - df + 0.5D) - Math.log(df + 0.5D);

//                System.out.printf("Term: %s | DF: %s | IDF: %s%n",queryTokens.get(qt),df,IDFs[qt]);

            }

            int totalHits = 0;

            HitQueue pq = new HitQueue(numHits, true);

            for (LeafReaderContext leafReaderContext : reader.leaves()) {

                PostingsEnum[] postingLists = new PostingsEnum[qTerms.length];

                Terms terms = leafReaderContext.reader().terms(field);

                TermsEnum te = terms.iterator();

                for (int qt = 0; qt < qTerms.length; qt++) {

                    te.seekCeil(new BytesRef(queryTokens.get(qt)));

                    postingLists[qt] = te.postings(null);

                    if (postingLists[qt] == null) {

                    }

                }

                NumericDocValues norms = leafReaderContext.reader().getNormValues(field);

                ScoreDoc pqTop = null;

                for (int doc = 0; doc < leafReaderContext.reader().numDocs(); doc++) {

                    float score = 0.0f;

                    long docLength = norms.get(doc);

                    for (int qt = 0; qt < postingLists.length; qt++) {

                        PostingsEnum pl = postingLists[qt];

                        if (pl == null) {
                            continue;
                        }

                        if (pl.docID() < doc) {
                            while (pl.nextDoc() < doc && pl.docID() < leafReaderContext.reader().maxDoc()) {
                                ;
                            }
                        }
                        if (pl.docID() == doc) {

                            double K = k1 * ((1 - b) + b * docLength / avgDocLength);

                            if (Float.isInfinite(k3)) {
                                score += 1.0f * (pl.freq() / (pl.freq() + K))
                                        * IDFs[qt]
                                        * qtf[qt];

                            } else {
                                score += 1.0f * (pl.freq() / (pl.freq() + K))
                                        * IDFs[qt]
                                        * (k3 + 1) * qtf[qt] / (k3 + qtf[qt]);
                            }

                            System.out.println("AvgDL: " + avgDocLength);
                            System.out.println("DL: " + docLength);
                            System.out.println("T:" + queryTokens.get(qt));
                            System.out.println("TF: " + pl.freq());
                            System.out.println("K: " + K);
                            System.out.println("S: " + (pl.freq() / (pl.freq() + K)));
                            System.out.println("IDF: " + IDFs[qt]);
                            System.out.println("BM25_t: " + (1.0f * (pl.freq() / (pl.freq() + K))
                                    * IDFs[qt]
                                    * (k3 + 1) * qtf[qt] / (k3 + qtf[qt])));

                            System.out.println();

                        }

                    }

                    // This collector cannot handle these scores:
                    assert score != Float.NEGATIVE_INFINITY;
                    assert !Float.isNaN(score);

                    if (score>0 && totalHits<numHits) {
                        pq.add(new ScoreDoc(doc + leafReaderContext.docBase,score));
                        pqTop.doc = doc + leafReaderContext.docBase;
                        pqTop.score = score;
                        pqTop = pq.updateTop();
                        totalHits++;
                    } else if (score > pqTop.score) {
                        // Since docs are returned in-order (i.e., increasing doc Id), a document
                        // with equal score to pqTop.score cannot compete since HitQueue favors
                        // documents with lower doc Ids. Therefore reject those docs too.
                        pqTop.doc = doc + leafReaderContext.docBase;
                        pqTop.score = score;
                        pqTop = pq.updateTop();
                    }

                }

            }

            ScoreDoc[] results = new ScoreDoc[totalHits];
            System.out.println("Match count: " + totalHits + " pq size: " + pq.size());
            if (totalHits > 0 && pq.size() > 0) {
                int k = 1;
                while ((numHits - totalHits) - k >= 0) {
                    pq.pop();
                    k++;
                }
                k = 1;
                while (totalHits - k >= 0) {
                    results[totalHits - k] = pq.pop();
                    k++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return hits;

    }


    public List<String> getTokens(String field, String queryTerms) throws IOException {

        List<String> tokens = new ArrayList<String>();

        StringReader topicTitleReader = new StringReader(queryTerms);

        Set<String> seenTokens = new TreeSet<String>();

        TokenStream tok;
        tok = analyzer.tokenStream(field, topicTitleReader);
        tok.reset();
        while (tok.incrementToken()) {
            Iterator<AttributeImpl> atts = tok.getAttributeImplsIterator();
            AttributeImpl token = atts.next();
            String text = "" + token;
            if (seenTokens.contains(text)) {
                continue;
            }
            seenTokens.add(text);
            tokens.add(text);
        }
        tok.close();

        return tokens;
    }



    public void readParamsFromFile(String paramFile){
        /*
        Reads in the xml formatting parameter file
        Maybe this code should go into the RetrievalParams class.

        Actually, it would probably be neater to create a ParameterFile class
        which these apps can inherit from - and customize accordinging.
         */


        try {
            p = JAXB.unmarshal(new File(paramFile), RetrievalParams.class);
        } catch (Exception e){
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
            System.exit(1);
        }

        setSim(p.model);

        if (p.maxResults==0.0) {p.maxResults=1000;}
        if (p.b == 0.0){ p.b = 0.75f;}
        if (p.beta == 0.0){p.beta = 500f;}
        if (p.k ==0.0){ p.k = 1.2f;}
        if (p.lam==0.0){p.lam = 0.5f;}
        if (p.mu==0.0){p.mu = 500f;}
        if (p.c==0.0){p.c=10.0f;}
        if (p.model == null){
            p.model = "def";
        }
        if (p.runTag == null){
            p.runTag = p.model.toLowerCase();
        }

        if (p.resultFile == null){
            p.resultFile = p.runTag+"_results.res";
        }

        System.out.println("Path to index: " + p.indexName);
        System.out.println("Query File: " + p.queryFile);
        System.out.println("Result File: " + p.resultFile);
        System.out.println("Model: " + p.model);
        System.out.println("Max Results: " + p.maxResults);
        System.out.println("b: " + p.b);


    }

    private enum SimModel {
        DEF, BM25, LMD, LMJ, PL2, TFIDF
    }

    private SimModel sim;

    private void setSim(String val){
        try {
            sim = SimModel.valueOf(p.model.toUpperCase());
        } catch (Exception e){
            System.out.println("Similarity Function Not Recognized - Setting to Default");
            System.out.println("Possible Similarity Functions are:");
            for(SimModel value: SimModel.values()){
                System.out.println("<model>"+value.name()+"</model>");
            }
            sim = SimModel.DEF;

        }
    }

    public void processQueryFile(){
        /*
        Assumes the query file contains a qno followed by the query terms.
        One query per line. i.e.

        Q1 hello world
        Q2 hello hello
        Q3 hello etc
         */
        try {
            BufferedReader br = new BufferedReader(new FileReader(p.queryFile));
            File file = new File(p.resultFile);
            FileWriter fw = new FileWriter(file);

            try {
                String line = br.readLine();
                while (line != null){

                    String[] parts = line.split(" ");
                    String qno = parts[0];
                    String queryTerms = "";
                    for (int i=1; i<parts.length; i++)
                        queryTerms = queryTerms + " " + parts[i];

                    ScoreDoc[] scored = runQuery(qno, queryTerms);

                    int n = Math.min(p.maxResults, scored.length);

                    for(int i=0; i<n; i++){
                        Document doc = searcher.doc(scored[i].doc);
                        String docno = doc.get("docnum");
                        fw.write(qno + " QO " + docno + " " + (i+1) + " " + scored[i].score + " " + p.runTag);
                        fw.write(System.lineSeparator());
                    }

                    line = br.readLine();
                }

            } finally {
                br.close();
                fw.close();
            }
        } catch (Exception e){
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }

    }

    public static void main(String []args) {

        String retrievalParamFile = "";

        try {
            retrievalParamFile = args[0];
        } catch (Exception e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
            System.exit(1);
        }

        RetrievalAppByIndexAPI retriever = new RetrievalAppByIndexAPI(retrievalParamFile);
        retriever.processQueryFile();

    }

}

class RetrievalParams {
    public String indexName;
    public String queryFile;
    public String resultFile;
    public String model;
    public int maxResults;
    public float k;
    public float b;
    public float lam;
    public float beta;
    public float mu;
    public float c;
    public String runTag;
}
