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
package de.featjar.assignment;

import de.featjar.analysis.sat4j.FastRandomConfigurationGenerator;
import de.featjar.analysis.sat4j.twise.YASA;
import de.featjar.clauses.LiteralList;
import de.featjar.clauses.solutions.analysis.SampleReducer;
import de.featjar.formula.ModelRepresentation;
import de.featjar.util.extension.ExtensionLoader;
import de.featjar.util.logging.Logger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * YASA sampling algorithm. Generates configurations for a given propositional
 * formula such that t-wise feature coverage is achieved.
 *
 * @author Sebastian Krieter
 */
public class SampleReducerTest {

    private static final int randomSeed = 1234;

    public static void main(String[] args) {
        ExtensionLoader.load();

        ModelRepresentation model = ModelRepresentation.load(Paths.get("src/test/resources/GPL/model.xml"))
                .orElse(Logger::logProblems);
        int t = 2;

        YASA twiseGenerator = new YASA();
        twiseGenerator.setT(t);
        List<LiteralList> solutionList1 = model.get(twiseGenerator).getSolutions();

        FastRandomConfigurationGenerator randomGenerator = new FastRandomConfigurationGenerator();
        randomGenerator.setLimit(solutionList1.size());
        randomGenerator.setRandom(new Random(randomSeed));
        List<LiteralList> solutionList2 = model.get(randomGenerator).getSolutions();

        List<LiteralList> sample = new ArrayList<>(solutionList1.size() + solutionList2.size());
        sample.addAll(solutionList1);
        sample.addAll(solutionList2);
        Collections.shuffle(sample, new Random(randomSeed));

        List<LiteralList> reducedSample = SampleReducer.reduce(sample, t);

        Collections.sort(solutionList1, Comparator.comparing(LiteralList::toString));
        Collections.sort(reducedSample, Comparator.comparing(LiteralList::toString));

        System.out.println(solutionList1.size());
        solutionList1.forEach(l -> System.out.println(l.toString()));
        System.out.println(reducedSample.size());
        reducedSample.forEach(l -> System.out.println(l.toString()));
    }
}
