//  sMOCell1.java
//
//  Author:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.uma.jmetal.metaheuristic.multiobjective.mocell;

import org.uma.jmetal.core.*;
import org.uma.jmetal.util.Distance;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.Neighborhood;
import org.uma.jmetal.util.Ranking;
import org.uma.jmetal.util.archive.CrowdingArchive;
import org.uma.jmetal.util.comparator.CrowdingComparator;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.random.PseudoRandom;

import java.util.Comparator;

/**
 * This class represents the original synchronous MOCell algorithm
 * A description of MOCell can be consulted in
 * Nebro A. J., Durillo J.J, Luna F., Dorronsoro B., Alba E. :
 * "MOCell: A cellular genetic algorithm for multiobjective optimization",
 * International Journal of Intelligent Systems. Vol.24, No. 7 (July 2009),
 * pp. 726-746
 */
public class sMOCell1 extends Algorithm {

  /**
   *
   */
  private static final long serialVersionUID = -5671233949239815443L;

  /**
   * Constructor
   */
  public sMOCell1() {
    super();
  } // sMOCell1


  /**
   * Runs of the sMOCell1 algorithm.
   *
   * @return a <code>SolutionSet</code> that is a set of non dominated solutions
   * as a result of the algorithm execution
   * @throws org.uma.jmetal.util.JMetalException
   */
  public SolutionSet execute() throws JMetalException, ClassNotFoundException {
    int populationSize, archiveSize, maxEvaluations, evaluations, feedBack;
    Operator mutationOperator, crossoverOperator, selectionOperator;
    SolutionSet population, newSolutionSet;
    CrowdingArchive archive;
    SolutionSet[] neighbors;
    Neighborhood neighborhood;
    Comparator<Solution> dominance = new DominanceComparator(),
      crowding = new CrowdingComparator();
    Distance distance = new Distance();

    //Read the params
    populationSize = (Integer) getInputParameter("populationSize");
    archiveSize = (Integer) getInputParameter("archiveSize");
    maxEvaluations = (Integer) getInputParameter("maxEvaluations");
    feedBack = (Integer) getInputParameter("feedBack");

    //Read the operator
    mutationOperator = operators_.get("mutation");
    crossoverOperator = operators_.get("crossover");
    selectionOperator = operators_.get("selection");

    //Initialize the variables    
    population = new SolutionSet(populationSize);
    newSolutionSet = new SolutionSet(populationSize);
    archive = new CrowdingArchive(archiveSize, problem_.getNumberOfObjectives());
    evaluations = 0;
    neighborhood = new Neighborhood(populationSize);
    neighbors = new SolutionSet[populationSize];

    //Create the initial population
    for (int i = 0; i < populationSize; i++) {
      Solution solution = new Solution(problem_);
      problem_.evaluate(solution);
      problem_.evaluateConstraints(solution);
      population.add(solution);
      solution.setLocation(i);
      evaluations++;
    }

    while (evaluations < maxEvaluations) {
      newSolutionSet = new SolutionSet(populationSize);
      for (int ind = 0; ind < population.size(); ind++) {
        Solution individual = new Solution(population.get(ind));

        Solution[] parents = new Solution[2];
        Solution[] offSpring;

        //neighbors[ind] = neighborhood.getFourNeighbors(currentSolutionSet,ind);
        neighbors[ind] = neighborhood.getEightNeighbors(population, ind);
        neighbors[ind].add(individual);

        //parents
        parents[0] = (Solution) selectionOperator.execute(neighbors[ind]);
        parents[1] = (Solution) selectionOperator.execute(neighbors[ind]);

        //Create a new solutiontype, using genetic operator mutation and crossover
        offSpring = (Solution[]) crossoverOperator.execute(parents);
        mutationOperator.execute(offSpring[0]);

        //->Evaluate offspring and constraints
        problem_.evaluate(offSpring[0]);
        problem_.evaluateConstraints(offSpring[0]);
        evaluations++;

        int flag = dominance.compare(individual, offSpring[0]);

        if (flag == -1) {
          newSolutionSet.add(new Solution(population.get(ind)));
        }

        if (flag == 1) {
          offSpring[0].setLocation(individual.getLocation());
          newSolutionSet.add(offSpring[0]);
          archive.add(new Solution(offSpring[0]));
        } else if (flag == 0) {
          neighbors[ind].add(offSpring[0]);
          Ranking rank = new Ranking(neighbors[ind]);
          for (int j = 0; j < rank.getNumberOfSubfronts(); j++) {
            distance
              .crowdingDistanceAssignment(rank.getSubfront(j), problem_.getNumberOfObjectives());
          }

          boolean deleteMutant = true;
          int compareResult = crowding.compare(individual, offSpring[0]);
          if (compareResult == 1) {
            deleteMutant = false;
          }

          if (!deleteMutant) {
            offSpring[0].setLocation(individual.getLocation());
            newSolutionSet.add(offSpring[0]);
            archive.add(new Solution(offSpring[0]));
          } else {
            newSolutionSet.add(new Solution(population.get(ind)));
            archive.add(new Solution(offSpring[0]));
          }
        }
      }
      //Store a portion of the archive into the population
      distance.crowdingDistanceAssignment(archive, problem_.getNumberOfObjectives());
      for (int j = 0; j < feedBack; j++) {
        if (archive.size() > j) {
          int r = PseudoRandom.randInt(0, population.size() - 1);
          if (r < population.size()) {
            Solution individual = archive.get(j);
            individual.setLocation(r);
            newSolutionSet.replace(r, new Solution(individual));
          }
        }
      }

      population = newSolutionSet;
    }
    return archive;
  }
}
