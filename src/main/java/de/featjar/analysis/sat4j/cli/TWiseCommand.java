/*
 * Copyright (C) 2024 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-formula-analysis-sat4j.
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
package de.featjar.analysis.sat4j.cli;

import de.featjar.analysis.sat4j.computation.YASA;
import de.featjar.analysis.sat4j.computation.YASAIncremental;
import de.featjar.base.cli.ICommand;
import de.featjar.base.cli.Option;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.IComputation;
import de.featjar.base.io.format.IFormat;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignmentGroups;
import de.featjar.formula.assignment.BooleanSolutionList;
import de.featjar.formula.assignment.ComputeBooleanClauseList;
import de.featjar.formula.io.csv.BooleanSolutionListCSVFormat;
import java.util.List;
import java.util.Optional;

/**
 * Computes solutions for a given formula using SAT4J.
 *
 * @author Sebastian Krieter
 * @author Andreas Gerasimow
 */
public class TWiseCommand extends ASAT4JAnalysisCommand<BooleanSolutionList, BooleanSolutionList> {

    /**
     * Maximum number of configurations to be generated.
     */
    public static final Option<Integer> LIMIT_OPTION = new Option<>("n", Option.IntegerParser) //
            .setDescription("Maximum number of configurations to be generated.") //
            .setDefaultValue(Integer.MAX_VALUE);

    /**
     * Value of t.
     */
    public static final Option<Integer> T_OPTION = new Option<>("t", Option.IntegerParser) //
            .setDescription("Value of parameter t.") //
            .setDefaultValue(2);

    /**
     * Number of iterations.
     */
    public static final Option<Integer> ITERATIONS_OPTION = new Option<>("i", Option.IntegerParser) //
            .setDescription("Number of iterations.") //
            .setDefaultValue(1);

    /**
     * Incremental flag.
     */
    public static final Option<Boolean> INCREMENTAL_OPTION = new Option<>("incremental", Option.BooleanParser) //
            .setDescription("Use incremental version of YASA.") //
            .setDefaultValue(false);

    /**
     * Reduce flag.
     */
    public static final Option<Boolean> REDUCE_OPTION = new Option<>("reduce", Option.BooleanParser) //
            .setDescription("Use reduce function of YASA.") //
            .setDefaultValue(true);

    @Override
    public List<Option<?>> getOptions() {
        return ICommand.addOptions(super.getOptions(), LIMIT_OPTION, T_OPTION, ITERATIONS_OPTION);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Computes solutions for a given formula using SAT4J");
    }

    @Override
    public IComputation<BooleanSolutionList> newAnalysis(ComputeBooleanClauseList formula) {
        if (optionParser.get(INCREMENTAL_OPTION)) {
            return formula.map(Computations::getKey)
                    .map(YASAIncremental::new)
                    .set(YASAIncremental.T, optionParser.get(T_OPTION))
                    .set(YASAIncremental.CONFIGURATION_LIMIT, optionParser.get(LIMIT_OPTION))
                    .set(YASAIncremental.ITERATIONS, optionParser.get(ITERATIONS_OPTION))
                    .set(YASAIncremental.RANDOM_SEED, optionParser.get(RANDOM_SEED_OPTION))
                    .set(YASAIncremental.SAT_TIMEOUT, optionParser.get(SAT_TIMEOUT_OPTION))
                    .set(YASAIncremental.REDUCE_FINAL_SAMPLE, optionParser.get(REDUCE_OPTION));
        } else {
            return formula.map(Computations::getKey)
                    .map(YASA::new)
                    .set(YASA.T, optionParser.get(T_OPTION))
                    .set(YASA.CONFIGURATION_LIMIT, optionParser.get(LIMIT_OPTION))
                    .set(YASA.ITERATIONS, optionParser.get(ITERATIONS_OPTION))
                    .set(YASA.RANDOM_SEED, optionParser.get(RANDOM_SEED_OPTION))
                    .set(YASA.SAT_TIMEOUT, optionParser.get(SAT_TIMEOUT_OPTION))
                    .set(YASA.REDUCE_FINAL_SAMPLE, optionParser.get(REDUCE_OPTION));
        }
    }

    @Override
    protected Object getOuputObject(BooleanSolutionList list) {
        return new BooleanAssignmentGroups(VariableMap.of(inputFormula), List.of(list.getAll()));
    }

    @Override
    protected IFormat<?> getOuputFormat() {
        return new BooleanSolutionListCSVFormat();
    }

    @Override
    public String serializeResult(BooleanSolutionList assignments) {
        return assignments.serialize();
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("t-wise-sat4j");
    }
}
