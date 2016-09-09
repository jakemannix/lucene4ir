package org.apache.lucene.search;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;

import java.io.IOException;
import java.util.Set;

public class CrossFieldPhraseQuery extends Query {

  protected TermQuery firstQuery;
  protected TermQuery secondQuery;
  public CrossFieldPhraseQuery(TermQuery firstQuery, TermQuery secondQuery) {
    this.firstQuery = firstQuery;
    this.secondQuery = secondQuery;

  }

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
    return new CrossFieldPhraseWeight(this, searcher, needsScores);
  }

  public static class CrossFieldPhraseWeight extends Weight {

    protected Weight firstWeight;
    protected Weight secondWeight;
    public CrossFieldPhraseWeight(CrossFieldPhraseQuery crossFieldPhraseQuery,
                                  IndexSearcher searcher, boolean needsScores) throws IOException {
      super(crossFieldPhraseQuery);
      firstWeight = crossFieldPhraseQuery.firstQuery.createWeight(searcher, needsScores);
      secondWeight = crossFieldPhraseQuery.secondQuery.createWeight(searcher, needsScores);
    }

    @Override
    public void extractTerms(Set<Term> terms) {
      // ??? do we need to do this?
    }

    @Override
    public Explanation explain(LeafReaderContext context, int doc) throws IOException {
      return null;  // TODO: later!
    }

    @Override
    public float getValueForNormalization() throws IOException {
      return 0; // returning zero is probably baaaaaad (divide by zero?)
    }

    @Override
    public void normalize(float norm, float boost) {
      // this is where someone who is a super-BooleanQuery containing us will pass down the 1/sumOfSquaredWEights
    }

    @Override
    public Scorer scorer(LeafReaderContext context) throws IOException {
      return new CrossFieldPhraseScorer(this, context);
    }

    public static class CrossFieldPhraseScorer extends Scorer {

      private Scorer firstScorer;
      private Scorer secondScorer;
      private DocIdSetIterator mergedIterator;

      public CrossFieldPhraseScorer(CrossFieldPhraseWeight crossFieldPhraseWeight,
                                    LeafReaderContext context) throws IOException {
        super(crossFieldPhraseWeight);
        firstScorer = crossFieldPhraseWeight.firstWeight.scorer(context);
        secondScorer = crossFieldPhraseWeight.secondWeight.scorer(context);

        final DocIdSetIterator firstIterator = firstScorer.iterator();
        final DocIdSetIterator secondIterator = secondScorer.iterator();

        mergedIterator = new DocIdSetIterator() {
          @Override
          public int docID() {
            return Math.min(firstIterator.docID(), secondIterator.docID());
          }

          @Override
          public int nextDoc() throws IOException {
            int first = firstIterator.docID();
            int second = secondIterator.docID();
            if (first < second) {
              return Math.min(firstIterator.nextDoc(), second);
            } else if (second < first) {
              return Math.min(secondIterator.nextDoc(), first);
            } else {
              return Math.min(firstIterator.nextDoc(), secondIterator.nextDoc());
            }
          }

          @Override
          public int advance(int target) throws IOException {
            return Math.min(firstIterator.advance(target), secondIterator.advance(target));
          }

          @Override
          public long cost() {
            return firstIterator.cost() + secondIterator.cost();
          }
        };
      }

      @Override
      public int docID() {
        return mergedIterator.docID();
      }

      @Override
      public float score() throws IOException {

        return 0;
      }

      @Override
      public int freq() throws IOException {
        return 0;
      }

      @Override
      public DocIdSetIterator iterator() { return mergedIterator; }
    }
  }

  @Override
  public String toString(String field) {
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    return false;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
