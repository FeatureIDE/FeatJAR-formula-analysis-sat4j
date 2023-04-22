/*
 * Copyright (C) 2023 Sebastian Krieter
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
package de.featjar.analysis.sat4j;

import de.featjar.analysis.sat4j.solver.SStrategy;
import de.featjar.analysis.sat4j.solver.Sat4JSolver;
import de.featjar.analysis.solver.RuntimeContradictionException;
import de.featjar.analysis.solver.SatSolver.SatResult;
import de.featjar.clauses.CNF;
import de.featjar.clauses.CNFProvider;
import de.featjar.clauses.Clauses;
import de.featjar.clauses.LiteralList;
import de.featjar.clauses.LiteralList.Order;
import de.featjar.clauses.solutions.analysis.ConfigurationUpdater;
import de.featjar.formula.ModelRepresentation;
import de.featjar.formula.structure.Formula;
import de.featjar.formula.structure.atomic.Assignment;
import de.featjar.formula.structure.atomic.literal.VariableMap;
import de.featjar.util.data.Pair;
import de.featjar.util.job.NullMonitor;
import de.featjar.util.logging.Logger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class RandomConfigurationUpdater implements ConfigurationUpdater {
    private final RandomConfigurationGenerator generator;
    private final ModelRepresentation model;
    private final Random random;

    public RandomConfigurationUpdater(ModelRepresentation model, Random random) {
        this.model = model;
        this.random = random;
        generator = new FastRandomConfigurationGenerator();
        generator.setAllowDuplicates(true);
        generator.setRandom(random);
        generator.init(model.getCache(), new NullMonitor());
    }

    @Override
    public Optional<LiteralList> update(LiteralList partialSolution) {
        final CoreDeadAnalysis analysis = new CoreDeadAnalysis();
        for (int l : partialSolution.getLiterals()) {
            analysis.getAssumptions().set(Math.abs(l), l > 0);
        }
        final LiteralList otherLiterals = model.get(analysis);
        return otherLiterals == null ? Optional.empty() : Optional.of(partialSolution.addAll(otherLiterals));
    }

    @Override
    public Optional<LiteralList> complete(LiteralList partialSolution, Collection<LiteralList> excludeClauses) {
        excludeClauses = excludeClauses != null ? excludeClauses : Collections.emptyList();
        if (partialSolution == null && excludeClauses.isEmpty()) {
            return Optional.ofNullable(generator.get());
        }
        final Assignment assumptions = generator.getAssumptions();
        final List<Pair<Integer, Object>> oldAssumptions = assumptions.getAll();

        if (partialSolution != null) {
            for (int literal : partialSolution.getLiterals()) {
                assumptions.set(Math.abs(literal), literal > 0);
            }
        }
        VariableMap variables = model.getVariables();
        List<Formula> assumedConstraints = generator.getAssumedConstraints();
        for (LiteralList clause : excludeClauses) {
            assumedConstraints.add(Clauses.toOrClause(clause.negate(), variables));
        }
        try {
            generator.updateAssumptions();
            return Optional.ofNullable(generator.get());
        } catch (RuntimeContradictionException e) {
            return Optional.empty();
        } finally {
            generator.resetAssumptions();
            assumptions.unsetAll();
            assumptions.setAll(oldAssumptions);
            assumedConstraints.clear();
            generator.updateAssumptions();
        }
    }

    public LiteralList random() {
        CNF modelCnf = model.getCache().get(CNFProvider.fromFormula()).get();
        Sat4JSolver solver = new Sat4JSolver(modelCnf);
        solver.setSelectionStrategy(SStrategy.random(random));
        solver.shuffleOrder(random);
        return solver.findSolution();
    }

    @Override
    public LiteralList choose(List<int[]> clauses) {
    	Logger.logError("Choose");
    	Logger.logError("\tClauses: " + clauses.stream()
                .map(Arrays::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
        CNF modelCnf = model.getCache().get(CNFProvider.fromFormula()).get();
        VariableMap variables = new VariableMap(modelCnf.getVariableMap());
        CNF cnf = new CNF(variables, modelCnf.getClauses());
        final int orgVariableCount = variables.getVariableCount();
        Logger.logError("\tOrg CNF: " + cnf.getClauses().stream()
                .map(LiteralList::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
        
        int[] newNegativeLiterals = new int[clauses.size()];
        int i = 0;
        for (int[] clause : clauses) {
            int newVar = variables.addBooleanVariable().getIndex();
            newNegativeLiterals[i++] = -newVar;

            for (int l : clause) {
                cnf.addClause(new LiteralList(new int[] {l, newVar}, Order.UNORDERED, false));
            }
        }
        cnf.addClause(new LiteralList(newNegativeLiterals, Order.UNORDERED, false));
        cnf.addClause(LiteralList.mergeParallel(clauses, orgVariableCount).negate());
        Logger.logError("\tNew CNF: " + cnf.getClauses().stream()
                .map(LiteralList::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
        try {
            Sat4JSolver solver = new Sat4JSolver(cnf);
            solver.setSelectionStrategy(SStrategy.random(random));
            solver.shuffleOrder(random);
            return solver.hasSolution() != SatResult.TRUE
                    ? null
                    : new LiteralList(
                            Arrays.copyOf(solver.getInternalSolution(), orgVariableCount), Order.INDEX, false);
        } catch (RuntimeContradictionException e) {
            return null;
        }
    }

    public LiteralList getConfig(List<int[]> include, List<int[]> exclude, List<int[]> choose) {

        CNF modelCnf = model.getCache().get(CNFProvider.fromFormula()).get();
        VariableMap variables = new VariableMap(modelCnf.getVariableMap());
        CNF cnf = new CNF(variables, modelCnf.getClauses());
        final int orgVariableCount = variables.getVariableCount();

        int[] newNegativeLiterals = new int[choose.size()];
        int i = 0;
        for (int[] clause : choose) {
            int newVar = variables.addBooleanVariable().getIndex();
            newNegativeLiterals[i++] = -newVar;

            for (int l : clause) {
                cnf.addClause(new LiteralList(new int[] {l, newVar}, Order.UNORDERED, false));
            }
        }
        cnf.addClause(new LiteralList(newNegativeLiterals, Order.UNORDERED, false));
        cnf.addClause(LiteralList.mergeParallel(choose, orgVariableCount).negate());

        if (include != null) {
            LiteralList includeMerge =
                    LiteralList.mergeParallel(include, model.getVariables().getVariableCount());
            for (int literal : includeMerge.getLiterals()) {
                cnf.addClause(new LiteralList(new int[] {literal}, Order.UNORDERED, false));
            }
        }
        if (exclude != null) {
            for (int[] clause : exclude) {
                cnf.addClause(new LiteralList(clause, Order.UNORDERED, false).negate());
            }
        }
        try {
            Sat4JSolver solver = new Sat4JSolver(cnf);
            solver.setSelectionStrategy(SStrategy.random(random));
            solver.shuffleOrder(random);
            return solver.hasSolution() != SatResult.TRUE
                    ? null
                    : new LiteralList(
                            Arrays.copyOf(solver.getInternalSolution(), orgVariableCount), Order.INDEX, false);
        } catch (RuntimeContradictionException e) {
            return null;
        }
    }
}
