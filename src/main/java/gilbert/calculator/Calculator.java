package gilbert.calculator;

import java.text.ParseException;

/**
 * Evaluates an expression provided on the command line. 
 * An expression can be an integer value, or an operation. Supported operations are
 * add, sub, mult, div, each of which takes two expressions as arguments, in a prefix notation.
 * <p>
 * Example: add(sub(213,54),45)
 * <p>
 * There is also an assignment operation which defines a variable, assigns it a value,
 * then uses the value. The value of the assignment operation is that the last expression.
 * <p>
 * Example: let(aVar,sub(134,58),div(aVar,3))
 * <p>
 * A logging level can also be specified on the command line (either after or before the expression)
 * as -ERROR, -INFO or -DEBUG. Logs go to standard output.
 */
public class Calculator
{
  public static void main(String[] args)
  {
    String arg;
    String inputExpression = null;
    boolean loggingLevelSet = false;

    for (int i=0; i< args.length; i++)
    {
       arg = args[i];
       if (arg.charAt(0) == '-')
         if (arg.charAt(1) == 'h')
         {
            System.out.println("Valid logging levels are -ERROR, -INFO, -DEBUG");
            return;
         }
         else if (loggingLevelSet)
                Logger.info("Too many logging levels specified. Ignoring " + arg);
              else
              {
                 arg = arg.substring(1);
                 Logger.setLoggingLevel(arg);
                 loggingLevelSet = true;
                 Logger.info("Setting logging level to " + arg);
              }
       else if (inputExpression != null)
            {
               Logger.error("Too many expressions specified. " + arg + " is extra.");
               return;
            }
            else inputExpression = arg;
    }

    Logger.debug("Number of arguments passed: " + args.length);

    if (inputExpression == null)
    {
       Logger.info("No expression to evaluate.");
       return;
    }

    Logger.info("Evaluating expression: " + inputExpression);
    
    try
    {
       Expression e = Expression.build(inputExpression);
       System.out.println("Expression evaluates to " + e.eval());
    }
    catch (ParseException exc)
    {
       Logger.error(exc.getMessage());
    }
    catch (ArithmeticException a)
    {
       // Division by zero, overflow, etc...
       Logger.error(a.getMessage());
    }
  }
}