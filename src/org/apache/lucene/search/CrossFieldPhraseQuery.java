package org.apache.lucene.search;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;

import java.io.IOException;
import java.util.Set;

public class CrossFieldPhraseQuery extends Query {

  public CrossFieldPhraseQuery(TermQuery firstQuery, TermQuery secondQuery) {

  }

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
    return new CrossFieldPhraseWeight(this);
  }

  public static class CrossFieldPhraseWeight extends Weight {

    public CrossFieldPhraseWeight(CrossFieldPhraseQuery crossFieldPhraseQuery) {
      super(crossFieldPhraseQuery);
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
      return null; // here's where the magic happens
    }

    public static class CrossFieldPhraseScorer extends Scorer {

      public CrossFieldPhraseScorer(CrossFieldPhraseWeight crossFieldPhraseWeight) {
        super(crossFieldPhraseWeight);
      }

      @Override
      public int docID() {
        return 0;
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
      public DocIdSetIterator iterator() {
        return null;
      }
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
