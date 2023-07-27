/*
 * Copyright (C) 2022 Sebastian Krieter
 *
 * This file is part of formula-analysis-sat4j.
 *
 * formula-analysis-sat4j is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * formula-analysis-sat4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with formula-analysis-sat4j. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.todo.formula.analysis.sat4j;

import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.FutureResult;
import de.featjar.base.data.Result;
import de.featjar.formula.analysis.sat4j.solver.SAT4JSolutionSolver;

import java.util.Arrays;
import java.util.List;
import org.sat4j.core.VecInt;

/**
 * Finds indeterminate features.
 *
 * @author Sebastian Krieter
 */
public class IndeterminateAnalysis extends VariableAnalysis<SortedIntegerList> { // TODO: variable-analysis does not work
    // reliably (false positives) (use old
    // analysis first?)

    public IndeterminateAnalysis(IComputation<CNF> inputComputation, SortedIntegerList variables) {
        super(inputComputation, variables);
    }

    public IndeterminateAnalysis(IComputation<CNF> inputComputation, SortedIntegerList variables, Assignment assumptions, long timeoutInMs, long randomSeed) {
        super(inputComputation, variables, assumptions, timeoutInMs, randomSeed);
    }

    @Override
    public FutureResult<SortedIntegerList> compute() {
        return initializeSolver().thenCompute(((solver, progress) -> {
            if (variables == null) {
                variables = SortedIntegerList.getAbsoluteValuesOfIntegers(solver.getCNF().getVariableMap());
            }
            monitor.setTotalSteps(variables.getIntegers().length);

            final VecInt resultList = new VecInt();
            variableLoop:
            for (final int variable : variables.getIntegers()) {
                final SAT4JSolutionSolver modSolver = new SAT4JSolutionSolver(solver.getCNF()); // TODO: before, this was passed the variable map?
                final List<SortedIntegerList> sortedIntegerLists = solver.getCNF().getClauseList();
                for (final SortedIntegerList sortedIntegerList : sortedIntegerLists) {
                    final SortedIntegerList newSortedIntegerList = sortedIntegerList.removeVariables(variable);
                    if (newSortedIntegerList != null) {
                        try {
                            modSolver.getSolverFormula().push(newSortedIntegerList);
                        } catch (final SolverContradictionException e) {
                            monitor.addStep();
                            continue variableLoop;
                        }
                    } else {
                        monitor.addStep();
                        continue variableLoop;
                    }
                }

                final Result<Boolean> hasSolution = modSolver.hasSolution();
                if (Result.of(false).equals(hasSolution)) {
                } else if (Result.empty().equals(hasSolution)) {
                    //reportTimeout();
                } else if (Result.of(true).equals(hasSolution)) {
                    resultList.push(variable);
                } else {
                    throw new AssertionError(hasSolution);
                }
                monitor.addStep();
            }
            return new SortedIntegerList(Arrays.copyOf(resultList.toArray(), resultList.size()));
        }));
    }
}
