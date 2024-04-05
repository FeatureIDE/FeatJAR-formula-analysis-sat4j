package de.featjar.formula.transform.cli;

import de.featjar.base.FeatJAR;
import de.featjar.base.cli.ICommand;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.extension.IExtension;
import de.featjar.base.io.IO;
import de.featjar.base.tree.Trees;
import de.featjar.formula.io.FormulaFormats;
import de.featjar.formula.io.textual.*;
import de.featjar.formula.structure.formula.IFormula;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PrintCommand implements ICommand {

    public enum SymbolSelection {
        JAVA(JavaSymbols.INSTANCE),
        LOGICAL(LogicalSymbols.INSTANCE),
        PROPOSITIONAL(PropositionalModelSymbols.INSTANCE),
        SHORT(ShortSymbols.INSTANCE),
        TEXTUAL(TextualSymbols.INSTANCE);

        public Symbols symbols;

        SymbolSelection(Symbols symbols) {
            this.symbols = symbols;
        }
    }

    public static final Option<String> TAB_OPTION = new Option<>(
            "tab", Option.StringParser)
            .setDescription("Defines the tab string.");

    public static final Option<ExpressionSerializer.Notation> NOTATION_OPTION = new Option<>(
            "notation", (arg) -> ExpressionSerializer.Notation.valueOf(arg.toUpperCase()))
            .setDescription("Defines the notation. Possible options: " + Arrays.toString(ExpressionSerializer.Notation.values()));

    public static final Option<String> SEPARATOR_OPTION = new Option<>(
            "separator", Option.StringParser)
            .setDescription("Defines the separator string.");

    public static final Option<SymbolSelection> SYMBOLS_OPTION = new Option<>(
            "format", (arg) -> SymbolSelection.valueOf(arg.toUpperCase()))
            .setDescription("Defines the symbols. Possible options: " + Arrays.toString(SymbolSelection.values()));

    public static final Option<String> NEW_LINE_OPTION = new Option<>(
            "newline", Option.StringParser)
            .setDescription("Defines the new line string.");

    public static final  Option<Boolean> ENFORCE_PARENTHESES_OPTION = new Option<>(
            "enforce-parentheses", Option.BooleanParser)
            .setDescription("Enforces parentheses");

    public static final  Option<Boolean> ENQUOTE_WHITESPACE_OPTION = new Option<>(
            "enquote-whitespace", Option.BooleanParser)
            .setDescription("Enquotes whitespace");

    @Override
    public List<Option<?>> getOptions() {
        return  List.of(
                INPUT_OPTION,
                TAB_OPTION,
                NOTATION_OPTION,
                SEPARATOR_OPTION,
                SYMBOLS_OPTION,
                NEW_LINE_OPTION,
                ENFORCE_PARENTHESES_OPTION,
                ENQUOTE_WHITESPACE_OPTION);
    }


    @Override
    public void run(OptionList optionParser) {
        String tab = optionParser.getResult(TAB_OPTION).orElse(ExpressionSerializer.STANDARD_TAB_STRING);
        ExpressionSerializer.Notation notation = optionParser.getResult(NOTATION_OPTION).orElse(ExpressionSerializer.STANDARD_NOTATION);
        String separator = optionParser.getResult(SEPARATOR_OPTION).orElse(ExpressionSerializer.STANDARD_SEPARATOR);
        SymbolSelection symbolsEnum = optionParser.getResult(SYMBOLS_OPTION).orElse(null);
        String newLine = optionParser.getResult(NEW_LINE_OPTION).orElse(ExpressionSerializer.STANDARD_NEW_LINE);
        boolean ep = optionParser.getResult(ENFORCE_PARENTHESES_OPTION).orElse(ExpressionSerializer.STANDARD_ENFORCE_PARENTHESES);
        boolean ew = optionParser.getResult(ENQUOTE_WHITESPACE_OPTION).orElse(ExpressionSerializer.STANDARD_ENQUOTE_WHITESPACE);

        Symbols symbols = symbolsEnum == null ? ExpressionSerializer.STANDARD_SYMBOLS : symbolsEnum.symbols;

        IFormula formula = optionParser
                .getResult(INPUT_OPTION)
                .flatMap(p -> IO.load(p, FormulaFormats.getInstance()))
                .orElseThrow();

        ExpressionSerializer serializer = new ExpressionSerializer();

        serializer.setTab(tab);
        serializer.setNotation(notation);
        serializer.setSeparator(separator);
        serializer.setSymbols(symbols);
        serializer.setNewLine(newLine);
        serializer.setEnforceParentheses(ep);
        serializer.setEnquoteWhitespace(ew);

        if (formula != null) {
            String formulaString = Trees.traverse(formula, serializer).orElse("");
            FeatJAR.log().message(formulaString);
        }
    }
}
