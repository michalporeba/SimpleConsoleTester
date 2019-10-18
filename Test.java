import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.io.OutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

public class Test 
{
    static final String COMMAND_SET_EXECUTION       = "!";
    static final String COMMAND_EXECUTE             = "e";
    static final String COMMAND_MATCH               = "m";
    static final String COMMAND_FIND                = "f";
    static final String COMMAND_TYPE                = "t";
    static final String COMMAND_BECAUSE             = "b";
    static final String MATCH_SEPARATOR             = "#";
    static final String EXECUTION_ARGUMENTS_PATTERN = "\\s(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
    static final String TEST_OUTPUT_PREFIX          = ">> ";
    static final String TEST_INPUT_PREFIX           = "<< ";

    static final int MAX_NAME_LENGTH    = 20;
    static final int MAX_MATCH_LENGTH   = 30;

    static final ArrayList<String> _output = new ArrayList<String>();

    static String _classUnderTest = "";
    static ArrayList<String> _baseExecutionArguments = new ArrayList<String>();
    static Process _testProcess = null;
    static BufferedReader _outputReader = null;
    static PrintStream _inputWriter = null;

    static int _passedTests = 0;
    static int _failedTests = 0;
    static int _passedAssertions = 0;
    static int _failedAssertions = 0;
    static boolean _currentTestOutput = true;

    static IOThreadHandler _outputHandler = null;
    public static void main(String[] args) throws IOException, InterruptedException
    {
        
        _classUnderTest = args[0];
        String testDefinitionsPath = args[2];

        System.out.println();
        System.out.printf("Testing %s\nusing %s as test definition\n", _classUnderTest, testDefinitionsPath);
        System.out.println();

        Scanner definitions = getTestDefinitions(testDefinitionsPath);

        String command = "";
        String argument = "";

        while(definitions.hasNext())
        {
            command = getNextCommand(definitions).toLowerCase();
            argument = getNextArgument(definitions);

            switch(command) 
            {
                case COMMAND_SET_EXECUTION: 
                    setBaseExecutionArguments(argument);
                    break;
                case COMMAND_EXECUTE:
                    startExecution(argument);
                    break;
                case COMMAND_FIND: 
                    checkOutput(argument, false);
                    break; 
                case COMMAND_MATCH: 
                    checkOutput(argument, true);
                    break;
                case COMMAND_TYPE:
                    typeIn(argument);
                    break;
                default:
                    break;
            }
        }

        finishExecution();

        System.out.println();
        System.out.printf("Tests       Failed: %5d, Passed: %5d%n", _failedTests, _passedTests);
        System.out.printf("Assertions  Failed: %5d, Passed: %5d%n", _failedAssertions, _passedAssertions);
        System.out.println();
    }

    private static Scanner getTestDefinitions(String path) throws FileNotFoundException
    {
        return new Scanner(new File(path));
    }

    private static String getNextCommand(Scanner scanner)
    {
        return scanner.next();
    }

    private static String getNextArgument(Scanner scanner)
    {
        return scanner.nextLine().trim();
    }

    private static void setBaseExecutionArguments(String arguments) 
    {
        _baseExecutionArguments.clear();
        for(String s : arguments.split(EXECUTION_ARGUMENTS_PATTERN))
        {
            if (s != null && !s.isEmpty())
            {
                _baseExecutionArguments.add(s);
            }
        }

        if (_baseExecutionArguments.size() == 0) 
        {
            _baseExecutionArguments.add("java");
        }

        _baseExecutionArguments.add(_classUnderTest);
    }

    private static void finishExecution() throws IOException, InterruptedException
    {
        if (_currentTestOutput && _testProcess != null && _testProcess.exitValue() == 0) 
        {
            _passedTests++;
            System.out.println("TEST PASSED");
        }
        else 
        {
            _failedTests++;
            System.out.println("TEST FAILED");
        }

        _output.clear();

        if (_inputWriter != null)
        {
            _inputWriter.close();
            _inputWriter = null;
        }

        if (_outputReader != null) 
        {
            _outputReader.close();
            _outputReader = null;
        }

        if (_testProcess != null) 
        {
            _testProcess.destroy();
            _testProcess.waitFor();
            _testProcess = null;
        }

        System.out.println();
    }

    private static void startExecution(String arguments) throws IOException, InterruptedException
    {
        
        if (_testProcess != null) 
        {
            finishExecution();
            _currentTestOutput = true;
        }

        ArrayList<String> executionArguments = new ArrayList<String>(_baseExecutionArguments);
        for(String s : arguments.split(EXECUTION_ARGUMENTS_PATTERN))
        {
            executionArguments.add(s);
        }

        System.out.print("TESTING:");
        for(String s : executionArguments)
        {
            System.out.print(" " + s);
        }
        System.out.println();

        ProcessBuilder processBuilder = new ProcessBuilder(executionArguments);
        processBuilder.redirectErrorStream(true);
        _testProcess = processBuilder.start();

        InputStream input = _testProcess.getInputStream();
        _outputReader = new BufferedReader(new InputStreamReader(input));
        OutputStream output = _testProcess.getOutputStream();
        _inputWriter = new PrintStream(output);
    }

    private static void checkOutput(String arguments, boolean strong) throws IOException, InterruptedException
    {
        String expected = "";
        String test = "";
        String[] split = arguments.split(MATCH_SEPARATOR);

        if (split.length > 0)
        {
            expected = split[0].trim();
        }

        if (split.length > 1) 
        {
            test = split[1].trim();
        } 

        readOutput();

        boolean found = false; 
        String matchingLine = "";

        Pattern pattern = Pattern.compile(expected);
        Matcher matcher = null;

        for(String l : _output)
        {
            matcher = pattern.matcher(l);

            if ((strong && matcher.matches()) || (!strong && matcher.find()))
            {
                found = true;
                matchingLine = l;
                break;
            }
        }

        String outcome = "";

        if (found) 
        {
            _passedAssertions++;
            outcome = " OK   ";
        }
        else 
        {
            _failedAssertions++;
            outcome = "Failed";
            _currentTestOutput = false;
        }

        System.out.printf("[%s] %-"+(2+MAX_NAME_LENGTH)+"s", outcome, limit(test, MAX_NAME_LENGTH));
        if (!found) 
        {
            System.out.println("Expected: " + limit(expected, MAX_MATCH_LENGTH));
        }
        else 
        {
            System.out.println("Found in: " + limit(matchingLine, MAX_MATCH_LENGTH));
        }
    }

    private static String limit(String value, int maxLength)
    {
        if (value == null) 
        {
            return null;
        }

        if (value.length() <= maxLength)
        {
            return value;
        }
        
        return value.substring(0, maxLength-3)+"...";
    }

    private static void typeIn(String argument) throws IOException, InterruptedException
    {
        readOutput();

        _inputWriter.println(argument + System.lineSeparator());
        _inputWriter.flush();
    }

    private static void readOutput() throws IOException, InterruptedException
    {
        String line = null;
        boolean newoutput = false;

        while((line = _outputReader.readLine()) != null) 
        {
            newoutput = true;
            _output.add(line);
            System.out.println(TEST_OUTPUT_PREFIX + line);
        }

        if (newoutput) 
        {
            System.out.println();
        }
    }

    private static class IOThreadHandler extends Thread {
        private InputStream inputStream;
        private StringBuilder output = new StringBuilder();
 
        IOThreadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }
 
        public void run() {
            Scanner br = null;
            try {
                br = new Scanner(new InputStreamReader(inputStream));
                String line = null;
                while (br.hasNextLine()) {
                    line = br.nextLine();
                    output.append(line
                            + System.getProperty("line.separator"));
                }
            } finally {
                br.close();
            }
        }
 
        public StringBuilder getOutput() {
            return output;
        }
    }
}