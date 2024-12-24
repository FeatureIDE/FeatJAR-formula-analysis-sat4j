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
package de.featjar.analysis.sat4j.computation;

import de.featjar.analysis.RuntimeContradictionException;
import de.featjar.analysis.RuntimeTimeoutException;
import de.featjar.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.analysis.sat4j.solver.MIGVisitorByte;
import de.featjar.analysis.sat4j.solver.MIGVisitorLight;
import de.featjar.analysis.sat4j.solver.ModalImplicationGraph;
import de.featjar.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.analysis.sat4j.solver.SAT4JSolver;
import de.featjar.analysis.sat4j.twise.SampleBitIndex;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.Ints;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanSolution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * YASA sampling algorithm. Generates configurations for a given propositional
 * formula such that t-wise feature coverage is achieved.
 *
 * @author Sebastian Krieter
 */
public class YASAIncremental extends ASAT4JAnalysis<BooleanAssignmentList> {

    private static class PartialConfiguration {
        private final int id;
        private final boolean allowChange;
        private final MIGVisitorLight visitor;

        private int randomCount;

        public PartialConfiguration(int id, boolean allowChange, ModalImplicationGraph mig, int... newliterals) {
            this.id = id;
            this.allowChange = allowChange;
            visitor = new MIGVisitorLight(mig);
            if (allowChange) {
                visitor.propagate(newliterals);
            } else {
                visitor.setLiterals(newliterals);
            }
        }

        public int setLiteral(int... literals) {
            final int oldModelCount = visitor.getAddedLiteralCount();
            visitor.propagate(literals);
            return oldModelCount;
        }

        public int countLiterals() {
            return visitor.getAddedLiteralCount();
        }
    }

    public static final Dependency<ICombinationSpecification> LITERALS =
            Dependency.newDependency(ICombinationSpecification.class);
    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);
    public static final Dependency<Integer> CONFIGURATION_LIMIT = Dependency.newDependency(Integer.class);
    public static final Dependency<Integer> ITERATIONS = Dependency.newDependency(Integer.class);
    public static final Dependency<Integer> INTERNAL_SOLUTION_LIMIT = Dependency.newDependency(Integer.class);

    public static final Dependency<ModalImplicationGraph> MIG = Dependency.newDependency(ModalImplicationGraph.class);

    public static final Dependency<BooleanAssignmentList> INITIAL_SAMPLE =
            Dependency.newDependency(BooleanAssignmentList.class);

    public static final Dependency<Boolean> ALLOW_CHANGE_TO_INITIAL_SAMPLE = Dependency.newDependency(Boolean.class);
    public static final Dependency<Boolean> INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT =
            Dependency.newDependency(Boolean.class);
    public static final Dependency<Boolean> INCREMENTAL_T = Dependency.newDependency(Boolean.class);

    public YASAIncremental(IComputation<BooleanAssignmentList> clauseList) {
        super(
                clauseList,
                Computations.of(new NoneCombinationSpecification()),
                Computations.of(2),
                Computations.of(Integer.MAX_VALUE),
                Computations.of(2),
                Computations.of(65_536),
                new MIGBuilder(clauseList),
                Computations.of(new BooleanAssignmentList((VariableMap) null)),
                Computations.of(Boolean.TRUE),
                Computations.of(Boolean.TRUE),
                Computations.of(Boolean.TRUE));
    }

    private int minT,
            maxT,
            maxSampleSize,
            iterations,
            randomConfigurationLimit,
            variableCount,
            curSolutionId,
            randomSampleIdsIndex;
    private boolean allowChangeToInitialSample, initialSampleCountsTowardsConfigurationLimit;

    private ICombinationSpecification variables;
    private SAT4JSolutionSolver solver;
    private VariableMap variableMap;
    private ModalImplicationGraph mig;
    private Random random;
    private BooleanAssignmentList initialSample;

    private List<PartialConfiguration> currentSample, selectionCandidates;
    private SampleBitIndex bestSampleIndex, currentSampleIndex, randomSampleIndex;

    @Override
    public Result<BooleanAssignmentList> compute(List<Object> dependencyList, Progress progress) {
        maxT = T.get(dependencyList);
        if (maxT < 1) {
            throw new IllegalArgumentException("Value for t must be grater than 0. Value was " + maxT);
        }

        iterations = ITERATIONS.get(dependencyList);
        if (iterations == 0) {
            throw new IllegalArgumentException("Iterations must not equal 0.");
        }
        if (iterations < 0) {
            iterations = Integer.MAX_VALUE;
        }

        randomConfigurationLimit = INTERNAL_SOLUTION_LIMIT.get(dependencyList);
        if (randomConfigurationLimit < 0) {
            throw new IllegalArgumentException(
                    "Internal solution limit must be greater than 0. Value was " + randomConfigurationLimit);
        }

        maxSampleSize = CONFIGURATION_LIMIT.get(dependencyList);
        if (maxSampleSize < 0) {
            throw new IllegalArgumentException(
                    "Configuration limit must be greater than 0. Value was " + maxSampleSize);
        }

        initialSample = INITIAL_SAMPLE.get(dependencyList);

        random = new Random(RANDOM_SEED.get(dependencyList));

        allowChangeToInitialSample = ALLOW_CHANGE_TO_INITIAL_SAMPLE.get(dependencyList);
        initialSampleCountsTowardsConfigurationLimit =
                INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT.get(dependencyList);
        minT = INCREMENTAL_T.get(dependencyList) ? 1 : maxT;

        variableMap = BOOLEAN_CLAUSE_LIST.get(dependencyList).getVariableMap();
        variableCount = variableMap.getVariableCount();

        solver = initializeSolver(dependencyList);
        solver.setSelectionStrategy(ISelectionStrategy.random(random));

        mig = MIG.get(dependencyList);
        randomSampleIndex = new SampleBitIndex(variableCount);

        if (initialSampleCountsTowardsConfigurationLimit) {
            maxSampleSize = Math.max(maxSampleSize, maxSampleSize + initialSample.size());
        }

        variables = LITERALS.get(dependencyList);
        selectionCandidates = new ArrayList<>();

        maxT = Math.min(maxT, Math.max(variableCount, 1));
        if (variables instanceof NoneCombinationSpecification) {
            variables = new SingleCombinationSpecification(
                    new BooleanAssignment(new BooleanAssignment(IntStream.range(-variableCount, variableCount + 1)
                                    .filter(i -> i != 0)
                                    .toArray())
                            .removeAllVariables(
                                    Arrays.stream(mig.getCore()).map(Math::abs).toArray())),
                    maxT);
        }

        int count = variables.getTotalSteps();
        for (int t = minT; t <= maxT; t++) {
            count += (iterations - 1) * (1 << t) * variables.forOtherT(t).getTotalSteps();
        }
        progress.setTotalSteps(count);

        buildCombinations(progress);
        rebuildCombinations(progress);

        return finalizeResult();
    }

    @Override
    protected SAT4JSolver newSolver(BooleanAssignmentList clauseList) {
        return new SAT4JSolutionSolver(clauseList);
    }

    @Override
    public Result<BooleanAssignmentList> getIntermediateResult() {
        return finalizeResult();
    }

    private Result<BooleanAssignmentList> finalizeResult() {
        currentSample = null;
        currentSampleIndex = null;
        if (bestSampleIndex != null) {
            BooleanAssignmentList result = new BooleanAssignmentList(variableMap, bestSampleIndex.size());
            int initialSize = initialSample.size();
            for (int j = 0; j < initialSize; j++) {
                result.add(new BooleanSolution(bestSampleIndex.getConfiguration(j), false));
            }
            for (int j = bestSampleIndex.size() - 1; j >= initialSize; j--) {
                result.add(autoComplete(Arrays.stream(bestSampleIndex.getConfiguration(j))
                        .filter(l -> l != 0)
                        .toArray()));
            }
            return Result.of(result);
        } else {
            return Result.empty();
        }
    }

    private void buildCombinations(Progress monitor) {
        curSolutionId = 0;
        currentSample = new ArrayList<>();
        currentSampleIndex = new SampleBitIndex(variableCount);
        for (BooleanAssignment config : initialSample) {
            currentSampleIndex.addConfiguration(config);
        }

        variables.shuffle(random);
        variables.stream().forEach(combinationLiterals -> {
            checkCancel();
            monitor.incrementCurrentStep();

            if (currentSampleIndex.test(combinationLiterals)) {
                return;
            }
            if (isCombinationInvalidMIG(combinationLiterals)) {
                return;
            }
            newRandomConfiguration(combinationLiterals);
        });
        setBestSolutionList();
    }

    private void newRandomConfiguration(final int[] fixedLiterals) {
        int orgAssignmentSize = solver.getAssignment().size();
        try {
            solver.getAssignment().addAll(fixedLiterals);
            Result<Boolean> hasSolution = solver.hasSolution();
            if (hasSolution.isPresent()) {
                if (hasSolution.get()) {
                    int[] solution = solver.getInternalSolution();
                    currentSampleIndex.addConfiguration(solution);
                    if (randomSampleIndex.size() < randomConfigurationLimit) {
                        randomSampleIndex.addConfiguration(solution);
                    }
                    solver.shuffleOrder(random);
                }
            } else {
                throw new RuntimeTimeoutException();
            }
        } finally {
            solver.getAssignment().clear(orgAssignmentSize);
        }
    }

    private void rebuildCombinations(Progress monitor) {
        for (int j = 1; j < iterations; j++) {
            curSolutionId = 0;
            currentSample = new ArrayList<>();
            currentSampleIndex = new SampleBitIndex(variableCount);
            for (BooleanAssignment config : initialSample) {
                newConfiguration(config.get(), allowChangeToInitialSample);
            }
            for (int t = minT; t <= maxT; t++) {
                final int[] gray = Ints.grayCode(t);
                variables = variables.forOtherT(t);
                variables.shuffle(random);
                variables.stream().forEach(combinationLiterals -> {
                    for (int g : gray) {
                        checkCancel();
                        monitor.incrementCurrentStep();
                        if (!currentSampleIndex.test(combinationLiterals)
                                && bestSampleIndex.test(combinationLiterals)) {
                            getSelectionCandidates(combinationLiterals);
                            if (selectionCandidates.isEmpty()
                                    || (!tryCoverWithRandomSolutions(combinationLiterals)
                                            && !tryCoverWithSat(combinationLiterals))) {
                                newConfiguration(combinationLiterals, true);
                            }
                            selectionCandidates.clear();
                        }
                        combinationLiterals[g] = -combinationLiterals[g];
                    }
                });
            }
            setBestSolutionList();
        }
    }

    private void setBestSolutionList() {
        if (bestSampleIndex == null || bestSampleIndex.size() > currentSampleIndex.size()) {
            bestSampleIndex = currentSampleIndex;
        }
    }

    private void updateIndex(PartialConfiguration solution, int firstLiteralToConsider) {
        int addedLiteralCount = solution.visitor.getAddedLiteralCount();
        int[] addedLiterals = solution.visitor.getAddedLiterals();
        for (int i = firstLiteralToConsider; i < addedLiteralCount; i++) {
            currentSampleIndex.set(solution.id, addedLiterals[i]);
        }
    }

    private boolean getSelectionCandidates(int[] literals) {
        BitSet negatedBitSet = currentSampleIndex.getNegatedBitSet(literals);
        int nextBit = negatedBitSet.nextClearBit(0);
        while (nextBit < currentSampleIndex.size()) {
            PartialConfiguration configuration = currentSample.get(nextBit);
            if (canBeModified(configuration)) {
                selectionCandidates.add(configuration);
            }
            nextBit = negatedBitSet.nextClearBit(nextBit + 1);
        }
        return false;
    }

    private boolean tryCoverWithRandomSolutions(int[] literals) {
        BitSet literalBitSet = randomSampleIndex.getBitSet(literals);
        if (!literalBitSet.isEmpty()) {
            Collections.sort(
                    selectionCandidates,
                    Comparator.<PartialConfiguration>comparingInt(c -> c.visitor.countUndefined(literals))
                            .thenComparingInt(c -> c.countLiterals()));
            for (PartialConfiguration configuration : selectionCandidates) {
                BitSet configurationBitSet = randomSampleIndex.getBitSet(
                        configuration.visitor.getAddedLiterals(), configuration.visitor.getAddedLiteralCount());
                configuration.randomCount = configurationBitSet.cardinality();
                configurationBitSet.and(literalBitSet);
                if (!configurationBitSet.isEmpty()) {
                    updateIndex(configuration, configuration.setLiteral(literals));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCombinationInvalidMIG(int[] literals) {
        try {
            MIGVisitorByte visitor = new MIGVisitorByte(mig);
            visitor.propagate(literals);
        } catch (RuntimeContradictionException e) {
            return true;
        }
        return false;
    }

    private boolean tryCoverWithSat(int[] literals) {
        Collections.sort(selectionCandidates, Comparator.comparingInt(c -> c.randomCount));
        for (PartialConfiguration configuration : selectionCandidates) {
            if (trySelectSat(configuration, literals)) {
                return true;
            }
        }
        return false;
    }

    private void newConfiguration(int[] literals, boolean allowChange) {
        if (currentSample.size() < maxSampleSize) {
            PartialConfiguration newConfiguration =
                    new PartialConfiguration(curSolutionId++, allowChange, mig, literals);
            currentSample.add(newConfiguration);
            currentSampleIndex.addEmptyConfiguration();
            updateIndex(newConfiguration, 0);
        }
    }

    private BooleanSolution autoComplete(int[] configuration) {
        int nextSetBit =
                randomSampleIndex.getBitSet(configuration, configuration.length).nextSetBit(0);
        if (nextSetBit > -1) {
            return new BooleanSolution(randomSampleIndex.getConfiguration(nextSetBit), false);
        } else {
            final int orgAssignmentSize = setUpSolver(configuration);
            try {
                Result<BooleanSolution> hasSolution = solver.findSolution();
                if (hasSolution.isPresent()) {
                    return new BooleanSolution(hasSolution.get());
                } else {
                    throw new RuntimeTimeoutException();
                }
            } finally {
                solver.getAssignment().clear(orgAssignmentSize);
            }
        }
    }

    private boolean canBeModified(PartialConfiguration configuration) {
        return configuration.allowChange && configuration.visitor.getAddedLiteralCount() != variableCount;
    }

    private boolean trySelectSat(PartialConfiguration configuration, final int[] literals) {
        int addedLiteralCount = configuration.visitor.getAddedLiteralCount();
        final int oldModelCount = addedLiteralCount;
        try {
            configuration.visitor.propagate(literals);
        } catch (RuntimeException e) {
            configuration.visitor.reset(oldModelCount);
            return false;
        }

        final int orgAssignmentSize = setUpSolver(configuration);
        try {
            Result<Boolean> hasSolution = solver.hasSolution();
            if (hasSolution.isPresent()) {
                if (hasSolution.get()) {
                    updateIndex(configuration, oldModelCount);
                    randomSampleIdsIndex = (randomSampleIdsIndex + 1) % randomConfigurationLimit;
                    final int[] solution = solver.getInternalSolution();
                    randomSampleIndex.set(randomSampleIdsIndex, solution);
                    solver.shuffleOrder(random);
                    return true;
                } else {
                    configuration.visitor.reset(oldModelCount);
                }
            } else {
                throw new RuntimeTimeoutException();
            }
        } finally {
            solver.getAssignment().clear(orgAssignmentSize);
        }
        return false;
    }

    private int setUpSolver(PartialConfiguration configuration) {
        final int orgAssignmentSize = solver.getAssignment().size();
        int addedLiteralCount = configuration.visitor.getAddedLiteralCount();
        int[] addedLiterals = configuration.visitor.getAddedLiterals();
        for (int i = 0; i < addedLiteralCount; i++) {
            solver.getAssignment().add(addedLiterals[i]);
        }
        return orgAssignmentSize;
    }

    private int setUpSolver(int[] configuration) {
        final int orgAssignmentSize = solver.getAssignment().size();
        for (int i = 0; i < configuration.length; i++) {
            solver.getAssignment().add(configuration[i]);
        }
        return orgAssignmentSize;
    }
}
