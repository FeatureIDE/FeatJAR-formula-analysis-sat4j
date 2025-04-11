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
package de.featjar.analysis.sat4j.twise;

import de.featjar.base.data.ExpandableIntegerList;
import de.featjar.formula.assignment.BooleanAssignment;
import java.util.Arrays;
import java.util.List;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class SampleListIndex {

    private final ExpandableIntegerList[] bitSetReference;
    private final int numberOfVariables;
    private int sampleSize;

    public SampleListIndex(final int numberOfVariables) {
        this.numberOfVariables = numberOfVariables;
        bitSetReference = new ExpandableIntegerList[2 * numberOfVariables + 1];

        sampleSize = 0;
        for (int j = 0; j < bitSetReference.length; j++) {
            bitSetReference[j] = new ExpandableIntegerList();
        }
    }

    public SampleListIndex(final int numberOfVariables, int numberOfInitialConfigs) {
        this.numberOfVariables = numberOfVariables;
        bitSetReference = new ExpandableIntegerList[2 * numberOfVariables + 1];

        sampleSize = 0;
        for (int j = 0; j < bitSetReference.length; j++) {
            bitSetReference[j] = new ExpandableIntegerList(numberOfInitialConfigs);
        }
    }

    public SampleListIndex(List<? extends BooleanAssignment> sample, final int numberOfVariables) {
        this(numberOfVariables, sample.size());
        sample.forEach(this::addConfiguration);
    }

    public void addConfiguration(BooleanAssignment config) {
        addConfiguration(config.get());
    }

    public void addConfiguration(int[] config) {
        int configurationIndex = sampleSize++;

        final int[] literals = config;
        for (int i = 0; i < literals.length; i++) {
            final int literal = literals[i];
            if (literal != 0) {
                bitSetReference[numberOfVariables + literal].add(configurationIndex);
            }
        }
    }

    public boolean test(int... literals) {
        return index(literals) >= 0;
    }

    public int index(int... literals) {
        if (literals.length < 2) {
            ExpandableIntegerList i0 = bitSetReference[numberOfVariables + literals[0]];
            return i0.isEmpty() ? -1 : i0.get(0);
        }
        ExpandableIntegerList[] selectedIndexedSolutions = new ExpandableIntegerList[literals.length];
        for (int i = 0; i < literals.length; i++) {
            final ExpandableIntegerList indexedSolution = bitSetReference[numberOfVariables + literals[i]];
            if (indexedSolution.size() == 0) {
                return -1;
            }
            selectedIndexedSolutions[i] = indexedSolution;
        }
        Arrays.sort(selectedIndexedSolutions, (a, b) -> a.size() - b.size());
        final int[] searchIndices = new int[literals.length - 1];

        final ExpandableIntegerList i0 = selectedIndexedSolutions[0];
        final int[] ia0 = i0.getInternalArray();
        loop:
        for (int i = 0; i < i0.size(); i++) {
            int id0 = ia0[i];
            for (int j = 1; j < literals.length; j++) {
                final ExpandableIntegerList ij = selectedIndexedSolutions[j];
                final int searchIndex = search(ij, searchIndices, id0, j);
                if (searchIndex < 0) {
                    int nextIndex = -searchIndex - 1;
                    if (nextIndex < ij.size()) {
                        searchIndices[j - 1] = nextIndex;
                        continue loop;
                    } else {
                        return -1;
                    }
                } else {
                    searchIndices[j - 1] = searchIndex;
                }
            }
            return id0;
        }
        return -1;
    }

    private int search(ExpandableIntegerList ij, final int[] searchIndices, int id0, int j) {
        int minIndex = searchIndices[j - 1];
        int maxIndex = ij.size();
        int[] iax = ij.getInternalArray();
        if (maxIndex - minIndex < 8) {
            for (int k = minIndex; k < maxIndex; k++) {
                int l = iax[k];
                if (l == id0) {
                    return k;
                } else if (l > id0) {
                    return -(k + 1);
                }
            }
            return -(maxIndex + 1);
        } else {
            return Arrays.binarySearch(iax, minIndex, maxIndex, id0);
        }
    }

    public int size() {
        return sampleSize;
    }
}
