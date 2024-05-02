package de.featjar.formula.transform.cli;

import de.featjar.base.FeatJAR;
import de.featjar.base.cli.*;
import de.featjar.base.computation.AComputation;
import de.featjar.base.computation.Computations;
import de.featjar.base.data.Pair;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.formula.analysis.VariableMap;
import de.featjar.formula.analysis.bool.BooleanAssignment;
import de.featjar.formula.analysis.bool.BooleanAssignmentGroups;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.analysis.bool.ComputeBooleanClauseList;
import de.featjar.formula.io.FormulaFormats;
import de.featjar.formula.io.csv.BooleanAssignmentGroupsCSVFormat;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.transform.CNFSlicer;
import de.featjar.formula.transformer.ComputeCNFFormula;
import de.featjar.formula.transformer.ComputeNNFFormula;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Removes literals of a given formula using SAT4J.
 *
 * @author Andreas Gerasimow
 */
public class ProjectionCommand implements ICommand {

    /**
     * Literals to be removed.private long mixedCount;
     */
    public static final ListOption<String> LITERALS_OPTION = (ListOption<String>) new ListOption<>(
            "literals", Option.StringParser)
            .setDescription("Literals to be removed.")
            .setRequired(true);

    /**
     * Timeout in seconds.
     */
    public static final Option<Duration> TIMEOUT_OPTION = new Option<>(
            "timeout", s -> Duration.ofSeconds(Long.parseLong(s)))
            .setDescription("Timeout in seconds.")
            .setValidator(timeout -> !timeout.isNegative())
            .setDefaultValue(Duration.ZERO);

    @Override
    public List<Option<?>> getOptions() {
        return  List.of(LITERALS_OPTION, TIMEOUT_OPTION, INPUT_OPTION, OUTPUT_OPTION);
    }

    @Override
    public void run(OptionList optionParser) {
        Path outputPath = optionParser.getResult(OUTPUT_OPTION).orElse(null);
        List<String> literals = optionParser.getResult(LITERALS_OPTION).get();
        Duration timeout = optionParser.getResult(TIMEOUT_OPTION).get();
        BooleanAssignmentGroupsCSVFormat format = new BooleanAssignmentGroupsCSVFormat();

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
        literals.forEach((literal) -> {
            if (variableMap.get(literal).isEmpty()) {
                FeatJAR.log().warning("Feature " + literal + " does not exist in feature model.");
            } else {
                variableMap.remove(literal);
            }
        });

        variableMap.normalize();

        int[] array = literals.stream()
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
                if (outputPath == null || outputPath.toString().equals("results")) {
                    String string = format.serialize(new BooleanAssignmentGroups(variableMap, List.of(clauseList.getAll()))).orElseThrow();
                    FeatJAR.log().message(string);
                } else {
                    IO.save(new BooleanAssignmentGroups(variableMap, List.of(clauseList.getAll())), outputPath, format);
                }
            } catch (IOException | RuntimeException e) {
                FeatJAR.log().error(e);
            }
        } else {
            FeatJAR.log().error("Couldn't compute result:\n" + result.printProblems());
        }
    }

    @Override
    public String getDescription() {
        return "Removes literals of a given formula using SAT4J.";
    }

    @Override
    public String getShortName() {
        return "projection-sat4j";
    }
}
