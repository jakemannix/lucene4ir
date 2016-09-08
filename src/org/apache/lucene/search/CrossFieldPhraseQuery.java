package org.apache.lucene.search;

public class CrossFieldPhraseQuery extends Query {

  public CrossFieldPhraseQuery(TermQuery firstQuery, TermQuery secondQuery) {

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
