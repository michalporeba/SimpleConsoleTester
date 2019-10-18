import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;

public class Test 
{
    static final String COMMAND_SET_EXECUTION       = "!";
    static final String COMMAND_EXECUTE             = "e";
    static final String COMMAND_MATCH               = "m";
    static final String COMMAND_TYPE                = "t";
    static final String COMMAND_BECAUSE             = "b";
    static final String MATCH_SEPARATOR             = "#";
    static final String EXECUTION_ARGUMENTS_PATTERN = "\\s(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    static final ArrayList<String> _output = new ArrayList<String>();

    static String _classUnderTest = "";
    static ArrayList<String> _baseExecutionArguments = new ArrayList<String>();
    static Process _testProcess = null;
    static BufferedReader _outputReader = null;
    static BufferedWriter _inputWriter = null;

    static int _passedTests = 0;
    static int _failedTests = 0;

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
                case COMMAND_MATCH: 
                    checkIfOutputMatches(argument);
                    break;
                default:
                    break;
            }
        }

        System.out.println();
        System.out.printf("Tests executed: %d, Failed: %d, Passed: %d", _failedTests+_passedTests, _failedTests, _passedTests);
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

        System.out.print("New execution arguments:");
        for(String s : _baseExecutionArguments)
        {
            System.out.print(" " + s);
        }
        System.out.println();
        System.out.println();
    }

    private static void finishExecution() throws IOException, InterruptedException
    {
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
        }

        ArrayList<String> executionArguments = new ArrayList<String>(_baseExecutionArguments);
        for(String s : arguments.split(EXECUTION_ARGUMENTS_PATTERN))
        {
            executionArguments.add(s);
        }

        System.out.print("Executing");
        for(String s : executionArguments)
        {
            System.out.print(" " + s);
        }
        System.out.println();

        ProcessBuilder processBuilder = new ProcessBuilder(executionArguments);
        processBuilder.redirectErrorStream(true);
        _testProcess = processBuilder.start();

        OutputStream output = _testProcess.getOutputStream();
        InputStream input = _testProcess.getInputStream();
        _outputReader = new BufferedReader(new InputStreamReader(input));
    }

    public static void checkIfOutputMatches(String arguments) throws IOException
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

        String line = null;
        boolean newoutput = false;

        while((line = _outputReader.readLine()) != null) 
        {
            newoutput = true;
            _output.add(line);
            System.out.println(">> " + line);
        }

        if (newoutput) 
        {
            System.out.println();
        }

        boolean found = false; 

        for(String l : _output)
        {
            if (l.matches(expected))
            {
                found = true;
            }
        }

        String outcome = "";

        if (found) 
        {
            _passedTests++;
            outcome = " OK   ";
        }
        else {
            _failedTests++;
            outcome = "Failed";
        }

        System.out.printf("[%s] %-20s", outcome, test);
        if (!found) 
        {
            if (expected.length() <= 20)
            {
                System.out.printf("Expected: %s", expected);
            }
            else 
            {
                System.out.printf("\n  Expected: %s", expected);
            }
        }

        System.out.println();
    }
}