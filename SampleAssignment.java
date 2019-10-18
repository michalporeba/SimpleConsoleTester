import java.util.Scanner;

public class SampleAssignment
{
    public static void main(String[] args) throws Exception
    {
        if (args.length == 0)
        {
            System.out.println("There were No Arguments passed");
        }
        else if (args[0].equalsIgnoreCase("ask"))
        {
            System.out.print("give me a number: ");
            Scanner in = new Scanner(System.in);
            //int value = in.nextInt();
            int value = 0;
            System.out.printf("%d square is %d", value, value * value);
        } 
        else if (args[0].equalsIgnoreCase("exception"))
        {
            System.out.println("I am about to throw an exception\n");
            throw new Exception("hello");
        }
        else if (args.length > 0)
        {
            int value = Integer.parseInt(args[0]);
            System.out.println(value+value);
        }
    }
}