public class SampleAssignment
{
    public static void main(String[] args) throws Exception
    {
        String parameter = args[0];

        if (parameter.equalsIgnoreCase("ask"))
        {

        } 
        else if (args.length > 0)
        {
            int value = Integer.parseInt(parameter);
            System.out.println(value+value);
        }
        else if (parameter.equalsIgnoreCase("esception"))
        {
            throw new Exception("hello");
        }
        else
        {
            System.out.println("OK");
        }
    }
}