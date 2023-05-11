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
import de.featjar.clauses.CNF;
import de.featjar.clauses.CNFProvider;
import de.featjar.clauses.LiteralList;
import de.featjar.clauses.LiteralList.Order;
import de.featjar.clauses.solutions.analysis.ConfigurationUpdater;
import de.featjar.formula.ModelRepresentation;
import de.featjar.formula.structure.atomic.literal.VariableMap;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;

public class RandomConfigurationUpdater implements ConfigurationUpdater {
    private final ModelRepresentation model;
    private final Random random;

    public RandomConfigurationUpdater(ModelRepresentation model, Random random) {
        this.model = model;
        this.random = random;
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

    public Optional<LiteralList> complete(
            Collection<int[]> include, Collection<int[]> exclude, Collection<int[]> choose) {
        CNF cnf = model.getCache().get(CNFProvider.fromFormula()).get();
        VariableMap variables = cnf.getVariableMap();
        if (include != null || exclude != null || choose != null) {
            variables = new VariableMap(variables);
            cnf = new CNF(variables, cnf.getClauses());
        }
        final int orgVariableCount = variables.getVariableCount();

        if (choose != null) {
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
        }
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
            return Optional.ofNullable(solver.findSolution());
        } catch (RuntimeContradictionException e) {
            return Optional.empty();
        }
    }
}
