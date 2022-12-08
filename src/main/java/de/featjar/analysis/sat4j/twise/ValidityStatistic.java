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
package de.featjar.analysis.sat4j.twise;

/**
 * Holds statistics regarding validity of configurations within a configuration
 * sample.
 *
 * @author Sebastian Krieter
 */
public class ValidityStatistic {

    protected final boolean[] configValidities;
    protected int numberOfValidConfigurations;

    public ValidityStatistic(int sampleSize) {
        configValidities = new boolean[sampleSize];
    }

    public void setConfigValidity(int index, boolean valid) {
        configValidities[index] = valid;
        if (valid) {
            numberOfValidConfigurations++;
        }
    }

    public boolean[] getConfigValidities() {
        return configValidities;
    }

    public int getNumberOfConfigurations() {
        return configValidities.length;
    }

    public int getNumberOfValidConfigurations() {
        return numberOfValidConfigurations;
    }

    public int getNumberOfInvalidConfigurations() {
        return configValidities.length - numberOfValidConfigurations;
    }

    public double getValidInvalidRatio() {
        if (configValidities.length != 0) {
            return ((double) numberOfValidConfigurations / (double) configValidities.length);
        } else {
            return 1.0;
        }
    }
}
