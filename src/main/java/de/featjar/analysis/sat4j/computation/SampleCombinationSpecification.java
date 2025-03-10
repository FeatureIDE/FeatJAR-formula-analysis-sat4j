/*
 * Copyright (C) 2025 FeatJAR-Development-Team
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

import de.featjar.analysis.sat4j.twise.SampleBitIndex;
import de.featjar.base.data.BinomialCalculator;
import de.featjar.base.data.ICombination;
import de.featjar.base.data.SingleLexicographicIterator;
import de.featjar.formula.assignment.BooleanSolution;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SampleCombinationSpecification implements ICombinationSpecification {

    private final int t, totalSteps;

    private final int[] literals;
    private SampleBitIndex sampleIndex;

    public SampleCombinationSpecification(List<BooleanSolution> sample, int t) {
        if (t < 1) {
            throw new IllegalArgumentException("Value for t must be greater than 0. Value was " + t);
        }

        int numberOfLiterals = sample.stream().mapToInt(c -> c.size()).max().orElse(0);
        if (numberOfLiterals < t) {
            throw new IllegalArgumentException(
                    String.format("Value for t must be greater than number of variables", t, numberOfLiterals));
        }
        this.t = t;
        literals = IntStream.range(1, numberOfLiterals + 1).toArray();
        sampleIndex = new SampleBitIndex(sample, literals.length);

        totalSteps = (int) (BinomialCalculator.computeBinomial(literals.length, t));
    }

    private SampleCombinationSpecification(int[] literals, SampleBitIndex sampleIndex, int t) {
        if (t < 1) {
            throw new IllegalArgumentException("Value for t must be greater than 0. Value was " + t);
        }
        if (literals.length < t) {
            throw new IllegalArgumentException(
                    String.format("Value for t must be greater than number of variables", t, literals.length));
        }
        this.t = t;
        this.literals = literals;
        this.sampleIndex = sampleIndex;
        this.totalSteps = (int) (BinomialCalculator.computeBinomial(literals.length, t));
    }

    @Override
    public SampleCombinationSpecification forOtherT(int otherT) {
        return new SampleCombinationSpecification(literals, sampleIndex, otherT);
    }

    @Override
    public Stream<int[]> stream() {
        return SingleLexicographicIterator.stream(literals, t)
                .map(combo -> combo.select())
                .filter(sampleIndex::test);
    }

    @Override
    public <V> Stream<ICombination<V, int[]>> parallelStream(Supplier<V> environment) {
        return SingleLexicographicIterator.parallelStream(literals, t, environment)
                .filter(combo -> sampleIndex.test(combo.select()));
    }

    @Override
    public int getTotalSteps() {
        return totalSteps;
    }

    @Override
    public void shuffle(Random random) {
        final long seed = random.nextLong();
        Random curRandom = new Random(seed);
        for (int i = literals.length - 1; i >= 0; --i) {
            int swapIndex = curRandom.nextInt(literals.length);
            int temp = literals[i];
            literals[i] = literals[swapIndex];
            literals[swapIndex] = temp;
        }
    }
}
