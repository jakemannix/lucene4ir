package lucene4ir;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CrossFieldPhraseQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * dibuccio
 */
public class CrossFieldPhraseQueryTest {

    private String indexPath = "target/test_index";


    @Before
    public void buildTestIndex(){

        Directory dir = null;
        try {

            dir = FSDirectory.open(Paths.get(indexPath));
            System.out.println("Indexing to directory '" + indexPath + "'...");
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(dir, iwc);

            // Document 1
            String doc1_docnum = "AP890101-0001_A";
            String doc1_title = "You Don't Need a Weatherman To Know '60s Films Are Here Eds: Also in Monday AMs report.";
            String doc1_content = "The celluloid torch has been passed to a new\n" +
                    "generation: filmmakers who grew up in the 1960s.\n" +
                    "   ``Platoon,'' ``Running on Empty,'' ``1969'' and ``Mississippi\n" +
                    "Burning'' are among the movies released in the past two years from\n" +
                    "writers and directors who brought their own experiences of that\n" +
                    "turbulent decade to the screen.";


            Document doc1 = new Document();
            Field docnumField = new StringField("docnum", doc1_docnum, Field.Store.YES);
            doc1.add(docnumField);
            Field titleField = new StringField("title", doc1_title, Field.Store.YES);
            doc1.add(titleField);
            Field textField = new TextField("content", doc1_content, Field.Store.YES);
            doc1.add(textField);
            writer.addDocument(doc1);


            // Document 2
            String doc2_docnum = "AP890101-0003";
            String doc2_title = "Woman, Firefighter Killed In House Fire";
            String doc2_content = "An early morning house fire killed a woman\n" +
                    "and a firefighter who was fatally injured as he searched the house,\n" +
                    "officials said Saturday. Four other members of the woman's family\n" +
                    "were injured.\n" +
                    "   Fire Investigator Ray Mauck said the 4 a.m. fire started in the\n" +
                    "front room of the house in northwest Wichita but he would not\n" +
                    "comment on the cause. ``We are fairly sure at this time that it was\n" +
                    "an accidental fire,'' he said.";


            Document doc2 = new Document();
            docnumField = new StringField("docnum", doc2_docnum, Field.Store.YES);
            doc2.add(docnumField);
            titleField = new StringField("title", doc2_title, Field.Store.YES);
            doc2.add(titleField);
            textField = new TextField("content", doc2_content, Field.Store.YES);
            doc2.add(textField);
            writer.addDocument(doc2);

            // Document 3
            String doc3_docnum = "AP890101-0001_B";
            String doc3_title = "You Don't Need a Weatherman To Know '60s Films Are Here Eds: Also in Monday AMs report.";
            String doc3_content = "Stone, who based ``Platoon'' on some of his own experiences as a\n" +
                    "grunt, said the film brought up issues that had yet to be resolved.\n" +
                    "   ``People are responding to the fact that it's real. They're\n" +
                    "curious about the war in Vietnam after 20 years,'' he said.\n" +
                    "   While Southeast Asia was the pivotal foreign issue in American\n" +
                    "society of the '60s, civil rights was the major domestic issue. The\n" +
                    "civil rights movement reached its peak in the ``Freedom Summer'' of\n" +
                    "1964, when large groups of volunteers headed South to help register\n" +
                    "black voters.";

            Document doc3 = new Document();
            docnumField = new StringField("docnum", doc3_docnum, Field.Store.YES);
            doc3.add(docnumField);
            titleField = new StringField("title", doc3_title, Field.Store.YES);
            doc3.add(titleField);
            textField = new TextField("content", doc3_content, Field.Store.YES);
            doc3.add(textField);
            writer.addDocument(doc3);

            writer.close();


            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));

            System.out.println("Number of documents in the index: "+reader.numDocs());


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void testRankingWithCrossFieldPhraseQuery(){

        IndexReader reader = null;
        try {
            reader = DirectoryReader.open(FSDirectory.open( new File(indexPath).toPath()) );

            IndexSearcher searcher = new IndexSearcher(reader);

            String queryTerms = "weatherman film";

            String[] terms = queryTerms.split("\\s+");

            TermQuery tq1 = new TermQuery(new Term("title",terms[0]));
            TermQuery tq2 = new TermQuery(new Term("content",terms[1]));

            Query query = new BooleanQuery.Builder()
                    .add(tq1, BooleanClause.Occur.SHOULD)
                    .add(tq2, BooleanClause.Occur.SHOULD)
                    .build();

            TopDocs results = searcher.search(query, 1000);
            ScoreDoc[] hits = results.scoreDocs;
            System.out.printf("Number of hits for query \"%s\" with BooleanQuery of TermQuery: %s %n",queryTerms,hits.length);


            Query queryOrderOne = new CrossFieldPhraseQuery(
                    new TermQuery(new Term("title", terms[0])),
                    new TermQuery(new Term("contents", terms[1])));
            Query queryOrderTwo = new CrossFieldPhraseQuery(
                    new TermQuery(new Term("contents", terms[0])),
                    new TermQuery(new Term("title", terms[1])));

            query = new BooleanQuery.Builder()
                    .add(queryOrderOne, BooleanClause.Occur.SHOULD)
                    .add(queryOrderTwo, BooleanClause.Occur.SHOULD)
                    .build();

            results = searcher.search(query, 1000);
            hits = results.scoreDocs;
            System.out.printf("Number of hits for query \"%s\" with BooleanQuery of CrossFieldPhraseQuery: %s %n",queryTerms,hits.length);



        } catch (IOException ioe) {
            System.out.println(" caught a " + ioe.getClass() +
                    "\n with message: " + ioe.getMessage());
            ioe.printStackTrace();
        }

    }

}
