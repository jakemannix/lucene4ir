package org.apache.lucene.search;

import java.io.IOException;
import java.util.List;


/**
 * Scorer for Disjunctions where you want to keep track of which sub-scorer
 * contributed to the current hit, and scoring can non-linearly combine once
 * all sub-scores are available
 */
abstract class IdentifiedDisjunctionScorer extends DisjunctionScorer {

  private final int numSubScorers;

  public IdentifiedDisjunctionScorer(Weight weight, List<Scorer> subScorers, boolean needsScores) {
    super(weight, subScorers, needsScores);
    numSubScorers = subScorers.size();
  }

  protected float score(IdentifiedScorer topList) throws IOException {
    double[] scores = new double[numSubScorers];
    for (IdentifiedScorer w = topList; w != null; w = w.nextIdentifiedScorer) {
      scores[topList.id] = w.scorer.score();
    }
    return combineScores(scores);
  }

  public abstract float combineScores(double[] subScorerScores);
}
