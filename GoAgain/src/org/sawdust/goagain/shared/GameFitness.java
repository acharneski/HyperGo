package org.sawdust.goagain.shared;

import java.io.Serializable;

public interface GameFitness<T> extends Serializable {

  public double gameFitness(T game, int playerIdx);

}
