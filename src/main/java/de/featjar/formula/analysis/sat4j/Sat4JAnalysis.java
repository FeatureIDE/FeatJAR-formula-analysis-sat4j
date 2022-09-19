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
package de.featjar.formula.analysis.sat4j;

import de.featjar.formula.analysis.Analysis;
import de.featjar.formula.analysis.sat4j.solver.Sat4JSolver;
import de.featjar.formula.analysis.solver.RuntimeContradictionException;
import de.featjar.formula.analysis.solver.RuntimeTimeoutException;
import de.featjar.formula.clauses.CNF;
import de.featjar.base.task.Monitor;
import java.util.Random;

/**
 * Base class for analyses using a {@link Sat4JSolver}.
 *
 * @param <T> the type of the analysis result.
 *
 * @author Sebastian Krieter
 */
public abstract class Sat4JAnalysis<T> extends Analysis<T, Sat4JSolver, CNF> {

    protected boolean timeoutOccurred = false;
    private boolean throwTimeoutException = true;
    private int timeout = 1000;

    protected Random random = new Random(112358);

    public Sat4JAnalysis() {
        solverInputComputation = CNFComputation.fromFormula();
    }

    @Override
    public Object getParameters() {
        return assumptions != null ? assumptions : super.getParameters();
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    @Override
    public final T execute(CNF cnf, Monitor monitor) {
        if (solver == null) {
            solver = createSolver(cnf);
        }
        return execute(solver, monitor);
    }

    @Override
    protected Sat4JSolver createSolver(CNF input) throws RuntimeContradictionException {
        return new Sat4JSolver(input);
    }

    @Override
    protected void prepareSolver(Sat4JSolver solver) {
        super.prepareSolver(solver);
        solver.setTimeout(timeout);
        timeoutOccurred = false;
    }

    protected final void reportTimeout() throws RuntimeTimeoutException {
        timeoutOccurred = true;
        if (throwTimeoutException) {
            throw new RuntimeTimeoutException();
        }
    }

    public final boolean isThrowTimeoutException() {
        return throwTimeoutException;
    }

    public final void setThrowTimeoutException(boolean throwTimeoutException) {
        this.throwTimeoutException = throwTimeoutException;
    }

    public final boolean isTimeoutOccurred() {
        return timeoutOccurred;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}