package de.featjar.analysis.sat4j.computation;

import de.featjar.base.computation.ComputeConstant;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.Result;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanClauseList;

import java.util.List;

public class ComputeProjectionSAT4J extends ASAT4JAnalysis.Solution<BooleanAssignment> {

    public ComputeProjectionSAT4J(IComputation<BooleanClauseList> booleanClauseList) {
        super(booleanClauseList, new ComputeConstant<>(new BooleanAssignment()));
    }

    @Override
    public Result<BooleanAssignment> compute(List<Object> dependencyList, Progress progress) {
        return null;
    }
}
