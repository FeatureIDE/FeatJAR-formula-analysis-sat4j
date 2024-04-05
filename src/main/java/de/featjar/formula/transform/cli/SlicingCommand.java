package de.featjar.formula.transform.cli;

import de.featjar.base.FeatJAR;
import de.featjar.base.cli.*;
import de.featjar.base.computation.AComputation;
import de.featjar.base.computation.Computations;
import de.featjar.base.data.Pair;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.base.io.format.IFormat;
import de.featjar.formula.analysis.VariableMap;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanAssignmentGroups;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.ComputeBooleanClauseList;
import de.featjar.formula.io.FormulaFormats;
import de.featjar.formula.io.binary.BooleanAssignmentGroupsBinaryFormat;
import de.featjar.formula.io.csv.BooleanAssignmentGroupsCSVFormat;
import de.featjar.formula.io.dimacs.BooleanAssignmentGroupsDimacsFormat;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transform.CNFSlicer;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class SlicingCommand implements ICommand {

    public enum Format {
        CSV(new BooleanAssignmentGroupsCSVFormat()),
        DIMACS(new BooleanAssignmentGroupsDimacsFormat()),
        BINARY(new BooleanAssignmentGroupsBinaryFormat());

        private final IFormat<BooleanAssignmentGroups> format;

        Format(IFormat<BooleanAssignmentGroups> format) {
            this.format = format;
        }

        public IFormat<BooleanAssignmentGroups> getFormat() {
            return this.format;
        }
    }

    public static final ListOption<String> FEATURES_OPTION = (ListOption<String>) new ListOption<>(
            "features", Option.StringParser)
            .setDescription("Features to be sliced.")
            .setRequired(true);

    public static final Option<Duration> TIMEOUT_OPTION = new Option<>(
            "timeout", s -> Duration.ofSeconds(Long.parseLong(s)))
            .setDescription("Timeout in seconds")
            .setValidator(timeout -> !timeout.isNegative())
            .setDefaultValue(Duration.ZERO);

    public static final Option<Format> FORMAT_OPTION = new Option<>(
            "format", Format::valueOf)
            .setDescription("Select output format: CSV, DIMACS, BINARY.")
            .setDefaultValue(Format.CSV);


    @Override
    public List<Option<?>> getOptions() {
        return  List.of(FEATURES_OPTION, TIMEOUT_OPTION, FORMAT_OPTION, INPUT_OPTION, OUTPUT_OPTION);
    }

    @Override
    public void run(OptionList optionParser) {
        Path outputPath = optionParser.getResult(OUTPUT_OPTION).orElse(null);
        List<String> features = optionParser.getResult(FEATURES_OPTION).get();
        Duration timeout = optionParser.getResult(TIMEOUT_OPTION).get();
        Format format = optionParser.getResult(FORMAT_OPTION).orElse(Format.CSV);

        IFormula inputFormula = optionParser
                .getResult(INPUT_OPTION)
                .flatMap(p -> IO.load(p, FormulaFormats.getInstance()))
                .orElseThrow();

        Pair<BooleanClauseList, VariableMap> pair = Computations.of(inputFormula)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(ComputeBooleanClauseList::new)
                .compute();

        VariableMap variableMap = pair.getValue();

        VariableMap variableMapClone = variableMap.clone();

        // check if feature name exists
        features.forEach((feature) -> {
            if (variableMap.get(feature).isEmpty()) {
                FeatJAR.log().warning("Feature " + feature + " does not exist in feature model.");
            } else {
                variableMap.remove(feature);
            }
        });

        variableMap.normalize();

        int[] array = features.stream()
               .map(variableMapClone::get)
               .filter(Result::isPresent)
               .mapToInt(Result::get).toArray();

        AComputation<BooleanClauseList> computation = Computations.of(pair.getKey())
                .map(CNFSlicer::new)
                .set(CNFSlicer.VARIABLES_OF_INTEREST, new BooleanAssignment(array));

        Result<BooleanClauseList> result;

        if (!timeout.isZero()) {
            result = computation.computeResult(true, true, timeout);
            if (result.isEmpty()) {
                FeatJAR.log().error("Timeout");
            }
        } else {
            result = computation.computeResult(true, true);
        }

        if (result.isPresent()) {

            BooleanClauseList clauseList = result.get().adapt(variableMapClone, variableMap);

            try {
                if (outputPath == null) {
                    String string = format.getFormat().serialize(new BooleanAssignmentGroups(variableMap, List.of(clauseList.getAll()))).orElseThrow();
                    FeatJAR.log().message(string);
                } else {
                    IO.save(new BooleanAssignmentGroups(variableMap, List.of(clauseList.getAll())), outputPath, format.getFormat());
                }
            } catch (IOException | RuntimeException e) {
                FeatJAR.log().error(e);
            }
        } else {
            FeatJAR.log().error("Couldn't compute result:\n" + result.printProblems());
        }
    }
    // TODO: Write tests, flag for different formats, ExpressionSerializer.java, ExpressionFormat.java, Create command for printing, create command for converting to other format
}
