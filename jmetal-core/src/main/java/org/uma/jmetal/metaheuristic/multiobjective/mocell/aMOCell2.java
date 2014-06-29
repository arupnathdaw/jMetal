//  aMOCell2.java
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

import java.util.Comparator;

/**
 * This class represents an asynchronous version of the MOCell algorithm, which
 * applies an archive feedback through parent selection.
 */
public class aMOCell2 extends Algorithm {

  /**
   *
   */
  private static final long serialVersionUID = 6036292773938388363L;


  /**
   * Constructor
   */
  public aMOCell2() {
    super();
  } // aMOCell2


  /**
   * Runs of the aMOCell2 algorithm.
   *
   * @return a <code>SolutionSet</code> that is a set of non dominated solutions
   * as a result of the algorithm execution
   * @throws org.uma.jmetal.util.JMetalException
   */
  public SolutionSet execute() throws JMetalException, ClassNotFoundException {
    //Init the param
    int populationSize, archiveSize, maxEvaluations, evaluations;
    Operator mutationOperator, crossoverOperator, selectionOperator;
    SolutionSet population;
    CrowdingArchive archive;
    SolutionSet[] neighbors;
    Neighborhood neighborhood;
    Comparator<Solution> dominance = new DominanceComparator(),
      crowding = new CrowdingComparator();
    Distance distance = new Distance();

    //Read the params
    populationSize = ((Integer) getInputParameter("populationSize")).intValue();
    archiveSize = ((Integer) getInputParameter("archiveSize")).intValue();
    maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();

    //Read the operator
    mutationOperator = operators_.get("mutation");
    crossoverOperator = operators_.get("crossover");
    selectionOperator = operators_.get("selection");

    //Initialize the variables
    population = new SolutionSet(populationSize);
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
      for (int ind = 0; ind < population.size(); ind++) {
        Solution individual = new Solution(population.get(ind));

        Solution[] parents = new Solution[2];
        Solution[] offSpring;

        //neighbors[ind] = neighborhood.getFourNeighbors(currentSolutionSet,ind);
        neighbors[ind] = neighborhood.getEightNeighbors(population, ind);
        neighbors[ind].add(individual);

        //parents
        parents[0] = (Solution) selectionOperator.execute(neighbors[ind]);
        if (archive.size() > 0) {
          parents[1] = (Solution) selectionOperator.execute(archive);
        } else {
          parents[1] = (Solution) selectionOperator.execute(neighbors[ind]);
        }

        //Create a new solutiontype, using genetic operator mutation and crossover
        offSpring = (Solution[]) crossoverOperator.execute(parents);
        mutationOperator.execute(offSpring[0]);

        //->Evaluate solutiontype and constraints
        problem_.evaluate(offSpring[0]);
        problem_.evaluateConstraints(offSpring[0]);
        evaluations++;

        int flag = dominance.compare(individual, offSpring[0]);

        if (flag == 1) {
          offSpring[0].setLocation(individual.getLocation());
          population.replace(offSpring[0].getLocation(), offSpring[0]);
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
            population.replace(offSpring[0].getLocation(), offSpring[0]);
            archive.add(new Solution(offSpring[0]));
          } else {
            archive.add(new Solution(offSpring[0]));
          }
        }
      }
    }
    return archive;
  }
}
