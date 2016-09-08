package org.apache.lucene.search;

public class IdentifiedScorer extends DisiWrapper {
  public IdentifiedScorer(Scorer scorer) {
    super(scorer);
  }

  public IdentifiedScorer nextIdentifiedScorer;

  public int id;
}
