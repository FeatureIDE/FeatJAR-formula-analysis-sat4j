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

import de.featjar.formula.assignment.BooleanAssignment;
import java.util.BitSet;
import java.util.List;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class SampleBitIndex {

    private final BitSet[] bitSetReference;
    private final int numberOfVariables;
    private int sampleSize;

    public SampleBitIndex(final int numberOfVariables) {
        this.numberOfVariables = numberOfVariables;
        bitSetReference = new BitSet[2 * numberOfVariables + 1];

        sampleSize = 0;
        for (int j = 0; j < bitSetReference.length; j++) {
            bitSetReference[j] = new BitSet();
        }
    }

    public SampleBitIndex(final int numberOfVariables, int numberOfInitialConfigs) {
        this.numberOfVariables = numberOfVariables;
        bitSetReference = new BitSet[2 * numberOfVariables + 1];

        sampleSize = 0;
        for (int j = 0; j < bitSetReference.length; j++) {
            bitSetReference[j] = new BitSet(numberOfInitialConfigs);
        }
    }

    public SampleBitIndex(List<? extends BooleanAssignment> sample, final int numberOfVariables) {
        this(numberOfVariables, sample.size());
        sample.forEach(this::addConfiguration);
    }

    public void addConfiguration(BooleanAssignment config) {
        addConfiguration(config.get());
    }

    public void addConfiguration(int[] config) {
        int i = sampleSize++;

        for (int l : config) {
            if (l != 0) {
                bitSetReference[numberOfVariables + l].set(i);
            }
        }
    }

    public int addEmptyConfiguration() {
        return sampleSize++;
    }

    public void clear(int index) {
        for (int j = 0; j < bitSetReference.length; j++) {
            bitSetReference[j].clear(index);
        }
    }

    public void set(int index, BooleanAssignment config) {
        set(index, config.get());
    }

    public void set(int index, int[] config) {
        for (int l : config) {
            set(index, l);
        }
    }

    public void set(int index, int literal) {
        bitSetReference[numberOfVariables - literal].clear(index);
        bitSetReference[numberOfVariables + literal].set(index, literal != 0);
    }

    public BitSet getBitSet(int... literals) {
        BitSet first = bitSetReference[numberOfVariables + literals[0]];
        BitSet bitSet = new BitSet(first.size());
        bitSet.xor(first);
        for (int k = 1; k < literals.length; k++) {
            bitSet.and(bitSetReference[numberOfVariables + literals[k]]);
        }
        return bitSet;
    }

    public BitSet getInternalBitSet(int literal) {
        return bitSetReference[numberOfVariables + literal];
    }

    public BitSet getNegatedBitSet(int... literals) {
        BitSet first = bitSetReference[numberOfVariables - literals[0]];
        BitSet bitSet = new BitSet(first.size());
        bitSet.xor(first);
        for (int k = 1; k < literals.length; k++) {
            bitSet.or(bitSetReference[numberOfVariables - literals[k]]);
        }
        return bitSet;
    }

    public BitSet getBitSet(int[] literals, int n) {
        if (n <= 0) {
            return new BitSet();
        }
        BitSet first = bitSetReference[numberOfVariables + literals[0]];
        BitSet bitSet = new BitSet(first.size());
        bitSet.xor(first);
        for (int k = 1; k < n; k++) {
            bitSet.and(bitSetReference[numberOfVariables + literals[k]]);
            if (bitSet.isEmpty()) {
                return bitSet;
            }
        }
        return bitSet;
    }

    public boolean test(int... literals) {
        switch (literals.length) {
            case 0:
                return false;
            case 1:
                return getInternalBitSet(literals[0]).cardinality() > 0;
            case 2:
                return bitSetReference[numberOfVariables + literals[0]].intersects(
                        bitSetReference[numberOfVariables + literals[1]]);
            default:
                return !getBitSet(literals).isEmpty();
        }
    }

    public int index(int... literals) {
        return literals.length == 1
                ? getInternalBitSet(literals[0]).length()
                : getBitSet(literals).length();
    }

    public int size(int... literals) {
        return literals.length == 1
                ? getInternalBitSet(literals[0]).cardinality()
                : getBitSet(literals).cardinality();
    }

    public int size() {
        return sampleSize;
    }

    public int[] getConfiguration(int configurationID) {
        int[] model = new int[numberOfVariables];
        for (int i = 1; i <= numberOfVariables; i++) {
            if (bitSetReference[numberOfVariables + i].get(configurationID)) {
                model[i - 1] = i;
            } else if (bitSetReference[numberOfVariables - i].get(configurationID)) {
                model[i - 1] = -i;
            }
        }
        return model;
    }
}
