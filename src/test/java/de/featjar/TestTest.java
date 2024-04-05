package de.featjar;

import de.featjar.base.FeatJAR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TestTest {

    private static final String sat4jstring = "java -jar build/libs/formula-analysis-sat4j-0.1.1-SNAPSHOT-all.jar";

    @Test
    void testSlicingCommand() {
        System.out.println("Testing Slicing Command");
        FeatJAR.main("--command de.featjar.formula.transform.cli.SlicingCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --output testSlicing.xml --features DirectedWithEdges,DirectedWithNeighbors".split(" "));
        // ProcessOutput output = runProcess(sat4jstring + " --command de.featjar.formula.transform.cli.SlicingCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --features DirectedWithEdges,DirectedWithNeighbors");
    }

    @Test
    void testCoreCommand() throws IOException {
        System.out.println("Testing CoreCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.CoreCommand --input ../formula/src/testFixtures/resources/GPL/model.xml");
        noOptions.printOutput();
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.CoreCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        seedOption.printOutput();
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.CoreCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        solverTimeoutOption.printOutput();
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());
    }

    @Test
    void testAtomicSetsCommand() throws IOException {
        System.out.println("Testing AtomicSetsCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.AtomicSetsCommand --input ../formula/src/testFixtures/resources/GPL/model.xml");
        noOptions.printOutput();
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.AtomicSetsCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        seedOption.printOutput();
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.AtomicSetsCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        solverTimeoutOption.printOutput();
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());
    }

    @Test
    void testSolutionCountCommand() throws IOException {
        System.out.println("Testing SolutionCountCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.SolutionCountCommand --input ../formula/src/testFixtures/resources/GPL/model.xml");
        noOptions.printOutput();
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.SolutionCountCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        seedOption.printOutput();
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.SolutionCountCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        solverTimeoutOption.printOutput();
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());
    }

    @Test
    void testSolutionsCommand() throws IOException {
        System.out.println("Testing SolutionsCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.SolutionsCommand --input ../formula/src/testFixtures/resources/GPL/model.xml");
        noOptions.printOutput();
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.SolutionsCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        seedOption.printOutput();
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.SolutionsCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        solverTimeoutOption.printOutput();
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());

        ProcessOutput limitOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.SolutionsCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --n 10");
        limitOption.printOutput();
        Assertions.assertTrue(limitOption.errorString.isBlank());

        ProcessOutput strategyOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.SolutionsCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --strategy negative");
        strategyOption.printOutput();
        Assertions.assertTrue(strategyOption.errorString.isBlank());

        ProcessOutput noDuplicatesOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.SolutionsCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --no-duplicates true");
        noDuplicatesOption.printOutput();
        Assertions.assertTrue(noDuplicatesOption.errorString.isBlank());
    }

    @Test
    void testTWiseCommand() throws IOException {
        System.out.println("Testing TWiseCommand:");
        ProcessOutput noOptions = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.TWiseCommand --input ../formula/src/testFixtures/resources/GPL/model.xml");
        noOptions.printOutput();
        Assertions.assertTrue(noOptions.errorString.isBlank());

        ProcessOutput seedOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.TWiseCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0");
        seedOption.printOutput();
        Assertions.assertTrue(seedOption.errorString.isBlank());

        ProcessOutput solverTimeoutOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.TWiseCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --solver_timeout 10");
        solverTimeoutOption.printOutput();
        Assertions.assertTrue(solverTimeoutOption.errorString.isBlank());

        ProcessOutput limitOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.TWiseCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --n 10");
        limitOption.printOutput();
        Assertions.assertTrue(limitOption.errorString.isBlank());

        ProcessOutput tOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.TWiseCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --t 5");
        tOption.printOutput();
        Assertions.assertTrue(tOption.errorString.isBlank());

        ProcessOutput iterationsOption = runProcess(sat4jstring + " --command de.featjar.formula.analysis.cli.TWiseCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --i 10");
        iterationsOption.printOutput();
        Assertions.assertTrue(iterationsOption.errorString.isBlank());
    }

    @Test
    void testSomething() throws IOException {
        FeatJAR.main("--command de.featjar.formula.analysis.cli.CoreCommand --input ../formula/src/testFixtures/resources/GPL/model.xml --seed 0".split(" "));
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
        }
        String outputString = sb.toString();

        sb = new StringBuilder();
        line = null;
        while ((line = errbr.readLine()) != null) {
            sb.append(line);
        }
        String errorString = sb.toString();

        return new ProcessOutput(outputString, errorString);
    }

    private class ProcessOutput {
        private String outputString;
        private String errorString;

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
