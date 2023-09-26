/*
 * Copyright (C) 2023 FeatJAR-Development-Team
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
package de.featjar.formula.analysis.sat4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.featjar.Common;
import de.featjar.base.computation.Computations;
import de.featjar.formula.analysis.bool.BooleanAssignmentList;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.BooleanRepresentationComputation;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;
import org.junit.jupiter.api.Test;

public class ComputeAtomicSetsTest extends Common {

    @Test
    public void gplHas27AtomicSets() {
        BooleanAssignmentList atomicSets = Computations.of(load("GPL/model.xml"))
                .cast(IFormula.class)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(BooleanRepresentationComputation::new)
                .map(Computations::getKey)
                .cast(BooleanClauseList.class)
                .map(ComputeAtomicSetsSAT4J::new)
                .compute();

        assertEquals(27, atomicSets.size());
    }
}