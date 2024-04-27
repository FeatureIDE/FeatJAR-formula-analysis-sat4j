package de.featjar;

import de.featjar.base.FeatJAR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CommandsTest {

    private static final String sat4jstring = "java -jar build/libs/formula-analysis-sat4j-0.1.1-SNAPSHOT-all.jar";

    @Test
    void testPrintCommand() throws IOException {
        System.out.println("Testing PrintCommand");
        String testFile = new String(Files.readAllBytes(Path.of("./src/test/java/de/featjar/res/testPrintCommand")));
        ProcessOutput output = runProcess(sat4jstring + " print-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --tab [tab] --notation PREFIX --separator [separator] --format de.featjar.formula.io.textual.JavaSymbols --newline [newline] --enforce-parentheses true --enquote-whitespace true");
        output.printOutput();
        Assertions.assertTrue(output.errorString.isBlank());
        Assertions.assertEquals(output.outputString.trim(), testFile.trim());
    }

    @Test
    void testConvertFormatCommand() throws IOException {
        // TODO: Write test
        System.out.println("Testing ConvertFormatCommand");
        //String testFile = new String(Files.readAllBytes(Path.of("./src/test/java/de/featjar/res/testConvertFormatCommand.dimacs")));
        FeatJAR.main("convert-format-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --format de.featjar.formula.io.KConfigReaderFormat".split(" "));
        //ProcessOutput output = runProcess(sat4jstring + " convert-format-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --format de.featjar.formula.io.xml.XMLFeatureModelFormulaFormat");
        //Assertions.assertTrue(output.errorString.isBlank());
        //Assertions.assertEquals(output.outputString.trim(), testFile.trim());
    }

    @Test
    void testConvertCNFFormatCommand() throws IOException {
        System.out.println("Testing ConvertCNFFormatCommand");
        String testFile = new String(Files.readAllBytes(Path.of("./src/test/java/de/featjar/res/testConvertFormatCommand.dimacs")));
        ProcessOutput output = runProcess(sat4jstring + " convert-cnf-format-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --format de.featjar.formula.io.dimacs.FormulaDimacsFormat");
        Assertions.assertTrue(output.errorString.isBlank());
        Assertions.assertEquals(output.outputString.trim(), testFile.trim());
    }

    @Test
    void testProjectionCommand() throws IOException {
        System.out.println("Testing ProjectionCommand");
        ProcessOutput output = runProcess(sat4jstring + " projection-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --literals DirectedWithEdges,DirectedWithNeighbors");
        Assertions.assertTrue(output.errorString.isBlank());
        Assertions.assertFalse(output.outputString.contains("DirectedWithEdges"));
        Assertions.assertFalse(output.outputString.contains("DirectedWithNeighbors"));
    }

    @Test
    void testCoreCommand() throws IOException {
        System.out.println("Testing CoreCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());

        ProcessOutput browserCacheOption = runProcess(sat4jstring + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.errorString.isBlank());

        ProcessOutput nonParallelOption = runProcess(sat4jstring + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.errorString.isBlank());

        ProcessOutput timeoutOption = runProcess(sat4jstring + " core-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.errorString.isBlank());
    }

    @Test
    void testAtomicSetsCommand() throws IOException {
        System.out.println("Testing AtomicSetsCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());

        ProcessOutput browserCacheOption = runProcess(sat4jstring + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.errorString.isBlank());

        ProcessOutput nonParallelOption = runProcess(sat4jstring + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.errorString.isBlank());

        ProcessOutput timeoutOption = runProcess(sat4jstring + " atomic-sets-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.errorString.isBlank());
    }

    @Test
    void testSolutionCountCommand() throws IOException {
        System.out.println("Testing SolutionCountCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " solution-count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " solution-count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " solution-count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());

        ProcessOutput browserCacheOption = runProcess(sat4jstring + " solution-count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.errorString.isBlank());

        ProcessOutput nonParallelOption = runProcess(sat4jstring + " solution-count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.errorString.isBlank());

        ProcessOutput timeoutOption = runProcess(sat4jstring + " solution-count-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.errorString.isBlank());
    }

    @Test
    void testSolutionsCommand() throws IOException {
        System.out.println("Testing SolutionsCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());

        ProcessOutput limitOption = runProcess(sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --n 10");
        Assertions.assertTrue(limitOption.errorString.isBlank());

        ProcessOutput strategyOption = runProcess(sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --strategy negative");
        Assertions.assertTrue(strategyOption.errorString.isBlank());

        ProcessOutput noDuplicatesOption = runProcess(sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --no-duplicates true");
        Assertions.assertTrue(noDuplicatesOption.errorString.isBlank());

        ProcessOutput browserCacheOption = runProcess(sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.errorString.isBlank());

        ProcessOutput nonParallelOption = runProcess(sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.errorString.isBlank());

        ProcessOutput timeoutOption = runProcess(sat4jstring + " solutions-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.errorString.isBlank());
    }

    @Test
    void testTWiseCommand() throws IOException {
        System.out.println("Testing TWiseCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml");
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());

        ProcessOutput limitOption = runProcess(sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --n 10");
        Assertions.assertTrue(limitOption.errorString.isBlank());

        ProcessOutput tOption = runProcess(sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --t 5");
        Assertions.assertTrue(tOption.errorString.isBlank());

        ProcessOutput iterationsOption = runProcess(sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --i 10");
        Assertions.assertTrue(iterationsOption.errorString.isBlank());

        ProcessOutput browserCacheOption = runProcess(sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --browser-cache true");
        Assertions.assertTrue(browserCacheOption.errorString.isBlank());

        ProcessOutput nonParallelOption = runProcess(sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --non-parallel true");
        Assertions.assertTrue(nonParallelOption.errorString.isBlank());

        ProcessOutput timeoutOption = runProcess(sat4jstring + " t-wise-sat4j --input ../formula/src/testFixtures/resources/GPL/model.xml --timeout 10");
        Assertions.assertTrue(timeoutOption.errorString.isBlank());
    }

    private ProcessOutput runProcess(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);

        InputStream processErr = process.getErrorStream();
        InputStream processOut = process.getInputStream();
        BufferedReader outbr = new BufferedReader(new InputStreamReader(processOut, StandardCharsets.UTF_8));
        BufferedReader errbr = new BufferedReader(new InputStreamReader(processErr, StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = outbr.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        String outputString = sb.toString();

        sb = new StringBuilder();
        line = null;
        while ((line = errbr.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        String errorString = sb.toString();

        return new ProcessOutput(outputString, errorString);
    }

    private static class ProcessOutput {
        private final String outputString;
        private final String errorString;

        public ProcessOutput(String outputString, String errorString) {
            this.outputString = outputString;
            this.errorString = errorString;
        }

        public void printOutput() {
            System.out.println(outputString);
            if (!errorString.isBlank()) {
                System.err.println(errorString);
            }
        }
    }
}
