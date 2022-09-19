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
package de.featjar.formula.analysis.sat4j.twise;

import de.featjar.formula.analysis.mig.io.MIGFormat;
import de.featjar.formula.analysis.mig.solver.ModalImplicationGraph;
import de.featjar.formula.analysis.mig.solver.MIGBuilder;
import de.featjar.formula.analysis.mig.solver.RegularMIGBuilder;
import de.featjar.formula.analysis.mig.solver.Vertex;
import de.featjar.formula.analysis.sat4j.FastRandomConfigurationGenerator;
import de.featjar.formula.analysis.sat4j.solver.SStrategy;
import de.featjar.formula.analysis.sat4j.solver.Sat4JSolver;
import de.featjar.formula.analysis.solver.SATSolver;
import de.featjar.formula.clauses.CNF;
import de.featjar.formula.clauses.ClauseList;
import de.featjar.formula.clauses.LiteralList;
import de.featjar.formula.clauses.solutions.SolutionList;
import de.featjar.base.data.Pair;
import de.featjar.base.io.IO;
import de.featjar.base.task.Executor;
import de.featjar.base.log.Log;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import org.sat4j.core.VecInt;

/**
 * Contains several intermediate results and functions for generating a t-wise
 * sample.
 *
 * @author Sebastian Krieter
 */
public class TWiseConfigurationUtil {

    public enum InvalidClausesList {
        None,
        Create,
        Use
    }

    public static final int GLOBAL_SOLUTION_LIMIT = 100_000;

    static final Comparator<Pair<LiteralList, TWiseConfiguration>> candidateLengthComparator =
            new CandidateLengthComparator();

    protected final LiteralList[] solverSolutions = new LiteralList[GLOBAL_SOLUTION_LIMIT];
    protected final HashSet<LiteralList> solutionSet = new HashSet<>();
    protected Random random = new Random(42);

    protected List<LiteralList> randomSample;

    private final List<TWiseConfiguration> incompleteSolutionList = new LinkedList<>();
    private final List<TWiseConfiguration> completeSolutionList = new ArrayList<>();
    private final HashSet<LiteralList> invalidClauses = new HashSet<>();
    private InvalidClausesList invalidClausesList = InvalidClausesList.None;

    protected final CNF cnf;
    protected final Sat4JSolver localSolver;
    protected final boolean hasSolver;

    protected ModalImplicationGraph modalImplicationGraph;
    protected LiteralList[] strongHull;
    protected LiteralList coreDead;

    protected int maxSampleSize = Integer.MAX_VALUE;

    private TWiseConfigurationGenerator.Deduce createConfigurationDeduce = TWiseConfigurationGenerator.Deduce.DP;
    private TWiseConfigurationGenerator.Deduce extendConfigurationDeduce = TWiseConfigurationGenerator.Deduce.NONE;

    public TWiseConfigurationUtil(CNF cnf, Sat4JSolver localSolver) {
        this.cnf = cnf;
        this.localSolver = localSolver;
        hasSolver = localSolver != null;

        randomSample = Collections.emptyList();
    }

    public void computeRandomSample(int randomSampleSize) {
        final FastRandomConfigurationGenerator randomGenerator = new FastRandomConfigurationGenerator();
        randomGenerator.setAllowDuplicates(true);
        randomGenerator.setRandom(random);
        randomGenerator.setLimit(randomSampleSize);
        randomSample = Executor.apply(randomGenerator::execute, cnf) //
                .map(SolutionList::getSolutions) //
                .orElse(Log::problems);

        for (final LiteralList solution : randomSample) {
            addSolverSolution(solution.getLiterals());
        }
    }

    public void computeMIG(boolean migCheckRedundancy, boolean migDetectStrong) {
        Feat.log().debug("Init graph... ");
        Feat.log().debug("\tCompute graph... ");
        final MIGBuilder migBuilder = new RegularMIGBuilder();
        migBuilder.setCheckRedundancy(migCheckRedundancy);
        migBuilder.setDetectStrong(migDetectStrong);
        modalImplicationGraph = Executor.apply(migBuilder, cnf).get();
        setupMIG();
    }

    public void computeMIG(Path migPath) {
        Feat.log().debug("Init graph... ");
        Feat.log().debug("\tLoad graph from " + migPath);
        modalImplicationGraph = IO.load(migPath, new MIGFormat()).get();
        setupMIG();
    }

    private void setupMIG() {
        strongHull = new LiteralList[modalImplicationGraph.getVertices().size()];

        for (final Vertex vertex : modalImplicationGraph.getVertices()) {
            strongHull[ModalImplicationGraph.getVertexIndex(vertex)] = new LiteralList(
                    vertex.getStrongEdges().stream().mapToInt(Vertex::getVar).toArray());
        }
    }

    public LiteralList getDeadCoreFeatures() {
        if (coreDead == null) {
            if (hasMig()) {
                computeDeadCoreFeaturesMig();
            } else {
                computeDeadCoreFeatures();
            }
        }
        return coreDead;
    }

    public LiteralList computeDeadCoreFeaturesMig() {
        if (hasSolver()) {
            coreDead = new LiteralList();
        } else {
            final int[] coreDeadArray = new int[cnf.getVariableMap().getVariableCount()];
            int index = 0;
            for (final Vertex vertex : modalImplicationGraph.getVertices()) {
                if (vertex.isCore()) {
                    coreDeadArray[index++] = vertex.getVar();
                }
            }
            coreDead = new LiteralList(Arrays.copyOf(coreDeadArray, index));
            if (!coreDead.isEmpty()) {
                localSolver.getAssumptions().pushAll(coreDead.getLiterals());
            }
        }
        return coreDead;
    }

    public LiteralList computeDeadCoreFeatures() {
        final Sat4JSolver solver = new Sat4JSolver(cnf);
        final int[] firstSolution = solver.findSolution().getLiterals();
        if (firstSolution != null) {
            final int[] coreDeadArray = new int[firstSolution.length];
            int coreDeadIndex = 0;
            solver.setSelectionStrategy(SStrategy.inverse(firstSolution));
            solver.hasSolution();
            LiteralList.resetConflicts(firstSolution, solver.getInternalSolution());

            // find core/dead features
            for (int i = 0; i < firstSolution.length; i++) {
                final int varX = firstSolution[i];
                if (varX != 0) {
                    solver.getAssumptions().push(-varX);
                    switch (solver.hasSolution()) {
                        case FALSE:
                            solver.getAssumptions().replaceLast(varX);
                            coreDeadArray[coreDeadIndex++] = varX;
                            break;
                        case TIMEOUT:
                            solver.getAssumptions().pop();
                            break;
                        case TRUE:
                            solver.getAssumptions().pop();
                            LiteralList.resetConflicts(firstSolution, solver.getInternalSolution());
                            solver.shuffleOrder(random);
                            break;
                    }
                }
            }
            coreDead = new LiteralList(Arrays.copyOf(coreDeadArray, coreDeadIndex));
            if (!coreDead.isEmpty()) {
                localSolver.getAssumptions().pushAll(coreDead.getLiterals());
            }
        } else {
            coreDead = new LiteralList();
        }
        return coreDead;
    }

    public CNF getCnf() {
        return cnf;
    }

    public Sat4JSolver getSolver() {
        return localSolver;
    }

    public ModalImplicationGraph getMig() {
        return modalImplicationGraph;
    }

    public boolean hasSolver() {
        return hasSolver;
    }

    public boolean hasMig() {
        return modalImplicationGraph != null;
    }

    public Random getRandom() {
        return random;
    }

    protected int solverSolutionEndIndex = -1;

    public void addSolverSolution(int[] literals) {
        final LiteralList solution = new LiteralList(literals, LiteralList.Order.INDEX, false);
        if (solutionSet.add(solution)) {
            solverSolutionEndIndex++;
            solverSolutionEndIndex %= GLOBAL_SOLUTION_LIMIT;
            final LiteralList oldSolution = solverSolutions[solverSolutionEndIndex];
            if (oldSolution != null) {
                solutionSet.remove(oldSolution);
            }
            solverSolutions[solverSolutionEndIndex] = solution;

            for (final TWiseConfiguration configuration : getIncompleteSolutionList()) {
                configuration.updateSolverSolutions(literals, solverSolutionEndIndex);
            }
        }
    }

    public LiteralList getSolverSolution(int index) {
        return solverSolutions[index];
    }

    public LiteralList[] getSolverSolutions() {
        return solverSolutions;
    }

    public boolean isCombinationValid(LiteralList literals) {
        return !isCombinationInvalidMIG(literals) && isCombinationValidSAT(literals);
    }

    public boolean isCombinationValid(ClauseList clauses) {
        if (hasSolver()) {
            if (invalidClausesList == InvalidClausesList.Use) {
                for (final LiteralList literalSet : clauses) {
                    if (invalidClauses.contains(literalSet)) {
                        return false;
                    }
                }
                return !clauses.isEmpty();
            }
            if (hasMig()) {
                for (final LiteralList literalSet : clauses) {
                    if (isCombinationInvalidMIG(literalSet)) {
                        if (invalidClausesList == InvalidClausesList.Create) {
                            invalidClauses.add(literalSet);
                        }
                        return false;
                    }
                }
            }
            for (final LiteralList literalSet : clauses) {
                if (isCombinationValidSAT(literalSet)) {
                    return true;
                } else {
                    if (invalidClausesList == InvalidClausesList.Create) {
                        invalidClauses.add(literalSet);
                    }
                }
            }
            return false;
        }
        return !clauses.isEmpty();
    }

    public boolean isCombinationInvalidMIG(LiteralList literals) {
        if (hasMig()) {
            for (final int literal : literals.getLiterals()) {
                if (strongHull[ModalImplicationGraph.getVertexIndex(literal)].hasConflicts(literals)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isCombinationValidSAT(LiteralList literals) {
        if (hasSolver()) {
            for (final LiteralList s : randomSample) {
                if (!s.hasConflicts(literals)) {
                    return true;
                }
            }

            final Sat4JSolver solver = getSolver();
            //			solver.setSelectionStrategy(SStrategy.random(getRandom()));
            final int orgAssignmentLength = solver.getAssumptions().size();
            try {
                solver.getAssumptions().pushAll(literals.getLiterals());
                final SATSolver.SatResult hasSolution = solver.hasSolution();
                switch (hasSolution) {
                    case TRUE:
                        final int[] solution = solver.getInternalSolution();
                        addSolverSolution(Arrays.copyOf(solution, solution.length));
                        solver.shuffleOrder(random);
                        break;
                    case FALSE:
                    case TIMEOUT:
                        return false;
                    default:
                        break;
                }
            } finally {
                solver.getAssumptions().clear(orgAssignmentLength);
            }
        }
        return true;
    }

    public boolean removeInvalidClauses(
            ClauseList nextCondition, List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        final LinkedList<LiteralList> invalidClauses = new LinkedList<>();
        for (final Iterator<LiteralList> conditionIterator = nextCondition.iterator(); conditionIterator.hasNext(); ) {
            final LiteralList literals = conditionIterator.next();
            if (!isCombinationValid(literals)) {
                invalidClauses.add(literals);
                conditionIterator.remove();
            }
        }
        if (nextCondition.isEmpty()) {
            candidatesList.clear();
            return true;
        } else {
            removeInvalidCandidates(candidatesList, invalidClauses);
            return false;
        }
    }

    public boolean removeInvalidClausesSat(
            ClauseList nextCondition, List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        final LinkedList<LiteralList> invalidClauses = new LinkedList<>();
        for (final Iterator<LiteralList> conditionIterator = nextCondition.iterator(); conditionIterator.hasNext(); ) {
            final LiteralList literals = conditionIterator.next();
            if (!isCombinationValidSAT(literals)) {
                invalidClauses.add(literals);
                conditionIterator.remove();
            }
        }
        if (nextCondition.isEmpty()) {
            candidatesList.clear();
            return true;
        } else {
            removeInvalidCandidates(candidatesList, invalidClauses);
            return false;
        }
    }

    public boolean removeInvalidClausesLight(
            ClauseList nextCondition, List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        final LinkedList<LiteralList> invalidClauses = new LinkedList<>();
        for (final Iterator<LiteralList> conditionIterator = nextCondition.iterator(); conditionIterator.hasNext(); ) {
            final LiteralList literals = conditionIterator.next();
            if (isCombinationInvalidMIG(literals)) {
                invalidClauses.add(literals);
                conditionIterator.remove();
            }
        }
        if (nextCondition.isEmpty()) {
            candidatesList.clear();
            return true;
        } else {
            removeInvalidCandidates(candidatesList, invalidClauses);
            return false;
        }
    }

    private void removeInvalidCandidates(
            List<Pair<LiteralList, TWiseConfiguration>> candidatesList, final LinkedList<LiteralList> invalidClauses) {
        for (final LiteralList literals : invalidClauses) {
            for (final Iterator<Pair<LiteralList, TWiseConfiguration>> candidateIterator = candidatesList.iterator();
                    candidateIterator.hasNext(); ) {
                final Pair<LiteralList, TWiseConfiguration> pair = candidateIterator.next();
                if (pair.getKey().equals(literals)) {
                    candidateIterator.remove();
                }
            }
        }
    }

    public boolean removeInvalidClausesLight(ClauseList nextCondition) {
        for (final Iterator<LiteralList> conditionIterator = nextCondition.iterator(); conditionIterator.hasNext(); ) {
            final LiteralList literals = conditionIterator.next();
            if (isCombinationInvalidMIG(literals)) {
                conditionIterator.remove();
            }
        }
        return nextCondition.isEmpty();
    }

    private boolean isSelectionPossibleSol(Pair<LiteralList, TWiseConfiguration> candidate) {
        final VecInt solverSolutionIndex = candidate.getValue().getSolverSolutionIndex();
        for (int i = 0; i < solverSolutionIndex.size(); i++) {
            if (!getSolverSolution(solverSolutionIndex.get(i)).hasConflicts(candidate.getKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSelectionPossibleSol(LiteralList literals, TWiseConfiguration configuration) {
        final VecInt solverSolutionIndex = configuration.getSolverSolutionIndex();
        for (int i = 0; i < solverSolutionIndex.size(); i++) {
            if (!getSolverSolution(solverSolutionIndex.get(i)).hasConflicts(literals)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSelectionPossibleSat(final LiteralList literals, final TWiseConfiguration configuration) {
        if (hasSolver) {
            final Sat4JSolver localSolver = getSolver();
            //			localSolver.setSelectionStrategy(SStrategy.random());
            final int orgAssignmentSize = configuration.setUpSolver(localSolver);
            try {
                final int[] configurationLiterals = configuration.getLiterals();
                for (final int literal : literals.getLiterals()) {
                    if (configurationLiterals[Math.abs(literal) - 1] == 0) {
                        localSolver.getAssumptions().push(literal);
                    }
                }
                if (orgAssignmentSize < localSolver.getAssumptions().size()) {
                    if (localSolver.hasSolution() == SATSolver.SatResult.TRUE) {
                        final int[] solution = localSolver.getInternalSolution();
                        addSolverSolution(Arrays.copyOf(solution, solution.length));
                        localSolver.shuffleOrder(random);
                    } else {
                        return false;
                    }
                }
            } finally {
                localSolver.getAssumptions().clear(orgAssignmentSize);
            }
        }
        return true;
    }

    public static boolean isCovered(ClauseList condition, Iterable<? extends LiteralList> solutionList) {
        for (final LiteralList configuration : solutionList) {
            for (final LiteralList literals : condition) {
                if (configuration.containsAll(literals)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Stream<TWiseConfiguration> getConfigurationStream() {
        return Stream.concat(getCompleteSolutionList().parallelStream(), getIncompleteSolutionList().parallelStream());
    }

    public boolean isCoveredPara(ClauseList condition) {
        final Optional<TWiseConfiguration> coveringSolution = condition.stream() //
                .flatMap(literals -> getConfigurationStream() //
                        .filter(configuration -> configuration.containsAll(literals))) //
                .findAny();
        return coveringSolution.isPresent();
    }

    public boolean isCovered(ClauseList condition) {
        return isCovered(condition, completeSolutionList) || isCovered(condition, incompleteSolutionList);
    }

    public boolean select(
            TWiseConfiguration solution, TWiseConfigurationGenerator.Deduce deduce, LiteralList literals) {
        selectLiterals(solution, deduce, literals);

        if (solution.isComplete()) {
            solution.clear();
            for (final Iterator<TWiseConfiguration> iterator = incompleteSolutionList.iterator();
                    iterator.hasNext(); ) {
                if (iterator.next() == solution) {
                    iterator.remove();
                    completeSolutionList.add(solution);
                    break;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private void selectLiterals(
            TWiseConfiguration solution, TWiseConfigurationGenerator.Deduce deduce, LiteralList literals) {
        solution.setLiteral(literals.getLiterals());
        if (hasSolver()) {
            switch (deduce) {
                case AC:
                    solution.autoComplete();
                    break;
                case DP:
                    solution.propagation();
                    break;
                case NONE:
                    break;
            }
        }
    }

    public boolean isCandidate(final LiteralList literals, TWiseConfiguration solution) {
        return !solution.hasConflicts(literals);
    }

    public boolean isCandidate(final Pair<LiteralList, TWiseConfiguration> pair) {
        return !pair.getValue().hasConflicts(pair.getKey());
    }

    public void addCandidates(final LiteralList literals, List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        for (final TWiseConfiguration configuration : getIncompleteSolutionList()) {
            if (isCandidate(literals, configuration)) {
                candidatesList.add(new Pair<>(literals, configuration));
            }
        }
    }

    public void initCandidatesListPara(
            ClauseList nextCondition, List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        candidatesList.clear();
        nextCondition.stream() //
                .flatMap(literals -> getIncompleteSolutionList().parallelStream() //
                        .filter(configuration -> isCandidate(literals, configuration)) //
                        .map(configuration -> new Pair<>(literals, configuration))) //
                .sorted(candidateLengthComparator) //
                .forEach(candidatesList::add);
    }

    public void initCandidatesList(
            ClauseList nextCondition, List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        candidatesList.clear();
        for (final LiteralList literals : nextCondition) {
            for (final TWiseConfiguration configuration : getIncompleteSolutionList()) {
                if (isCandidate(literals, configuration)) {
                    candidatesList.add(new Pair<>(literals, configuration));
                }
            }
        }
        Collections.sort(candidatesList, candidateLengthComparator);
    }

    protected boolean coverSol(List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        for (final Pair<LiteralList, TWiseConfiguration> pair : candidatesList) {
            if (isSelectionPossibleSol(pair.getKey(), pair.getValue())) {
                assert pair.getValue().isValid();
                select(pair.getValue(), extendConfigurationDeduce, pair.getKey());
                return true;
            }
        }
        return false;
    }

    protected boolean coverSat(List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        for (final Pair<LiteralList, TWiseConfiguration> pair : candidatesList) {
            if (isSelectionPossibleSat(pair.getKey(), pair.getValue())) {
                select(pair.getValue(), extendConfigurationDeduce, pair.getKey());
                assert pair.getValue().isValid();
                return true;
            }
        }
        return false;
    }

    protected boolean coverNoSat(List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        for (final Pair<LiteralList, TWiseConfiguration> pair : candidatesList) {
            select(pair.getValue(), extendConfigurationDeduce, pair.getKey());
            return true;
        }
        return false;
    }

    protected boolean coverSolPara(List<Pair<LiteralList, TWiseConfiguration>> candidatesList) {
        final Optional<Pair<LiteralList, TWiseConfiguration>> candidate = candidatesList.parallelStream() //
                .filter(this::isSelectionPossibleSol) //
                .findFirst();

        if (candidate.isPresent()) {
            final Pair<LiteralList, TWiseConfiguration> pair = candidate.get();
            select(pair.getValue(), extendConfigurationDeduce, pair.getKey());
            assert pair.getValue().isValid();
            return true;
        } else {
            return false;
        }
    }

    public void newConfiguration(final LiteralList literals) {
        if (completeSolutionList.size() < maxSampleSize) {
            final TWiseConfiguration configuration = new TWiseConfiguration(this);
            selectLiterals(configuration, createConfigurationDeduce, literals);
            assert configuration.isValid();
            configuration.updateSolverSolutions();
            if (configuration.isComplete()) {
                configuration.clear();
                completeSolutionList.add(configuration);
            } else {
                incompleteSolutionList.add(configuration);
                Collections.sort(incompleteSolutionList, (a, b) -> a.countLiterals() - b.countLiterals());
            }
        }
    }

    public List<TWiseConfiguration> getIncompleteSolutionList() {
        return incompleteSolutionList;
    }

    public List<TWiseConfiguration> getCompleteSolutionList() {
        return completeSolutionList;
    }

    public List<TWiseConfiguration> getResultList() {
        final ArrayList<TWiseConfiguration> resultList =
                new ArrayList<>(completeSolutionList.size() + incompleteSolutionList.size());
        resultList.addAll(incompleteSolutionList);
        resultList.addAll(completeSolutionList);
        return resultList;
    }

    public int getMaxSampleSize() {
        return maxSampleSize;
    }

    public void setMaxSampleSize(int maxSampleSize) {
        this.maxSampleSize = maxSampleSize;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public void setCreateConfigurationDeduce(TWiseConfigurationGenerator.Deduce createConfigurationDeduce) {
        this.createConfigurationDeduce = createConfigurationDeduce;
    }

    public void setExtendConfigurationDeduce(TWiseConfigurationGenerator.Deduce extendConfigurationDeduce) {
        this.extendConfigurationDeduce = extendConfigurationDeduce;
    }

    public void setMIG(ModalImplicationGraph modalImplicationGraph) {
        this.modalImplicationGraph = modalImplicationGraph;
        setupMIG();
    }

    public void setInvalidClausesList(InvalidClausesList invalidClausesList) {
        this.invalidClausesList = invalidClausesList;
    }
}