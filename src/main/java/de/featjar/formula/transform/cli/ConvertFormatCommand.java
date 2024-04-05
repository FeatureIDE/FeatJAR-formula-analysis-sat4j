package de.featjar.formula.transform.cli;

import de.featjar.base.FeatJAR;
import de.featjar.base.cli.ICommand;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.extension.IExtension;
import de.featjar.base.io.IO;
import de.featjar.base.io.format.AFormats;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.tree.Trees;
import de.featjar.formula.analysis.bool.BooleanClauseList;
import de.featjar.formula.io.FormulaFormats;
import de.featjar.formula.io.dimacs.CnfDimacsFormat;
import de.featjar.formula.io.dimacs.FormulaDimacsFormat;
import de.featjar.formula.io.textual.ExpressionSerializer;
import de.featjar.formula.structure.formula.IFormula;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ConvertFormatCommand implements ICommand {

    public static final Option<String> FORMAT_OPTION = new Option<>(
            "format", Option.StringParser)
            .setDescription("Select output format: " + FormulaFormats
                    .getInstance()
                    .getExtensions()
                    .stream()
                    .map(IExtension::getIdentifier)
                    .collect(Collectors.joining(", ")))
            .setRequired(true);

    @Override
    public List<Option<?>> getOptions() {
        return  List.of(FORMAT_OPTION, INPUT_OPTION, OUTPUT_OPTION);
    }

    @Override
    public void run(OptionList optionParser) {
        Path outputPath = optionParser.getResult(OUTPUT_OPTION).orElse(null);
        String formatString = optionParser.getResult(FORMAT_OPTION).orElse(null);

        try {
            Class<IFormat<IFormula>> classObj = (Class<IFormat<IFormula>>) Class.forName(formatString);
            IFormat<IFormula> format = FeatJAR.extension(classObj);

            IFormula formula = optionParser
                    .getResult(INPUT_OPTION)
                    .flatMap(p -> IO.load(p, FormulaFormats.getInstance()))
                    .orElseThrow();

            if (outputPath == null) {
                FeatJAR.log().message(formula);
            } else {
                IO.save(formula, outputPath, format);
            }

        } catch (ClassNotFoundException | IOException e) {
            FeatJAR.log().error(e);
        }
    }
}
