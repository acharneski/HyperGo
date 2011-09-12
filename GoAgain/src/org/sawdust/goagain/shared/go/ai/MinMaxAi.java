package org.sawdust.goagain.shared.go.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;

import org.sawdust.goagain.shared.Move;
import org.sawdust.goagain.shared.ai.FitnessValue;
import org.sawdust.goagain.shared.ai.GameFitness;
import org.sawdust.goagain.shared.ai.IterativeResult;
import org.sawdust.goagain.shared.ai.MoveFitness;
import org.sawdust.goagain.shared.go.GoGame;

public class MinMaxAi implements IterativeResult<Move<GoGame>> {

  private final MoveFitness<GoGame> intuition;
  private final GameFitness<GoGame> judgement;
  double totalProgress = 0;

  private int[] breadth;
  private long breadthProduct = 1;

  public class Frame {
    final GoGame game;
    final Iterator<Move<GoGame>> moves;

    Move<GoGame> thisMove = null;
    Move<GoGame> bestMove = null;
    double bestFitness = Integer.MIN_VALUE;
    GoGame bestEndGame = null;

    public final long denominator;
    private int counter;
    private int width;

    public Frame(GoGame game, long denominator) {
      this(game, denominator, breadth[stack.size()]);
    }

    public Frame(GoGame game, long denominator, int b) {
      this.width = b;
      this.game = game;
      this.counter = 0;
      Collection<Move<GoGame>> nextMoves = intuition(game);
      if (this.width > nextMoves.size()) this.width = nextMoves.size();
      this.moves = nextMoves.iterator();
      this.denominator = denominator;
    }

    protected void consider(GoGame hypotheticalGame) {
      IterativeResult<FitnessValue> gameFitness = judgement.gameFitness(hypotheticalGame, game.currentPlayer);
      while(1. > gameFitness.think()) {}
      double fitness = gameFitness.best().fitness;
      if (fitness > bestFitness) {
        bestMove = thisMove;
        bestFitness = fitness;
        bestEndGame = hypotheticalGame;
      }
    }
  }

  final Stack<Frame> stack = new Stack<Frame>();

  public MinMaxAi(GoGame game, MoveFitness<GoGame> intuition, GameFitness<GoGame> judgement, int... breadth) {
    this.breadth = breadth;
    for (int b : breadth)
      breadthProduct *= b;
    this.stack.push(new Frame(game, 1));
    this.intuition = intuition;
    this.judgement = judgement;
  }

  public double think() {
    Frame frame = stack.peek();
    if (frame.moves.hasNext() && frame.counter++ < frame.width) {
      frame.thisMove = frame.moves.next();
      GoGame hypotheticalGame = (GoGame) frame.thisMove.move(frame.game).unwrap();
      if (null != hypotheticalGame) {
        if (frame.game.winner == null && stack.size() < breadth.length) {
          this.stack.push(new Frame(hypotheticalGame, frame.denominator * frame.width));
        } else if (frame.game.winner == null && frame.denominator * frame.width * breadth[breadth.length - 1] < breadthProduct) {
          this.stack.push(new Frame(hypotheticalGame, frame.denominator * frame.width, breadth[breadth.length - 1]));
        } else {
          frame.consider(hypotheticalGame);
          totalProgress += 1. / (frame.width * frame.denominator);
        }
      }
    } else if (stack.size() > 1) {
      stack.pop();
      stack.peek().consider(frame.bestEndGame);
    } else {
      return 1;
    }
    return totalProgress;
  }

  public Move<GoGame> best() {
    for (Frame f : stack) {
      if (null != f.bestMove) return f.bestMove;
    }
    return null;
  }

  protected Collection<Move<GoGame>> intuition(final GoGame game) {
    ArrayList<Move<GoGame>> allMoves = game.getMoves();
    if (null != intuition) {
      final Map<Move<GoGame>, Double> fitnessCache = new HashMap<Move<GoGame>, Double>();
      TreeSet<Move<GoGame>> sortedMoves = new TreeSet<Move<GoGame>>(new Comparator<Move<GoGame>>()
          {
            public int compare(Move<GoGame> o1, Move<GoGame> o2)
            {
              double v1 = getFitness(o1);
              double v2 = getFitness(o2);
              int compare1 = Double.compare(v2, v1);
              if (0 == compare1) compare1 = o1.getCommandText().compareTo(o2.getCommandText());
              return compare1;
            }

            protected double getFitness(Move<GoGame> move) {
              double fitness;
              if (!fitnessCache.containsKey(move)) {
                fitness = intuition.moveFitness(move, game);
                fitnessCache.put(move, fitness);
              }
              else
              {
                fitness = fitnessCache.get(move);
              }
              return fitness;
            }
          });
      // Not allowed in GWT:
      // Collections.shuffle(m);
      for (Move<GoGame> move : allMoves) {
        double fitness = 0;
        if (!fitnessCache.containsKey(move)) {
          fitness = intuition.moveFitness(move, game);
          fitnessCache.put(move, fitness);
        } else {
          fitness = fitnessCache.get(move);
        }
        if (fitness >= 0) 
        {
          sortedMoves.add(move);
        }
        else
        {
          fitness = 0;
        }
      }
      return sortedMoves;
    }
    return allMoves;
  }

}