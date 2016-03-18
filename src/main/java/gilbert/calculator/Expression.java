package gilbert.calculator;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.ParseException;


/**
 * A high level abstraction on an expression.
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

public abstract class Expression
{
  public Expression()
  {
  }


  /**
   * Computes the value of the expression.
   * All syntactic errors are caught by the parser, so the only errors that can occur
   * during evaluation are the kind that will throw an ArithmeticException
   */
  abstract int eval();

  /**
   * Provides a public entry point for the expression parser.
   * Since the top level expression cannot be part of an assignment operator (let)
   * the context is set to null.
   * <p>
   * Syntactically, the input String may be an integer, a variable (upper or lower case
   * letters only) or an operator (one of add, sub, mult, div, let) followed by arguments
   * enclosed in parentheses. The four arithmetic operators take two arguments,
   * and the assignment operator takes three, the first of which must be a variable identifier.
   * <p>
   * A variable may only be referenced within the scope of an assignment operator.
   * The parser will throw an exception if one occurs elsewhere.
   */
  public static Expression build(String s) throws ParseException
  {
     return build(s, null);
  }

  /**
   * Parses and recursively builds an expression.
   * @param  s  an expression
   * @param  context  the enclosing assignment expression, if any (null for top level expression).
   * @return  parsed expression of the appropriate subclass, ready to be evaluated
   */

  private static Expression build(String s, Assignment context) throws ParseException
  {
     int intValue;

     // First check whether this is a simple integer value
     try
     {
        intValue = Integer.parseInt(s); 
        return new Value(intValue);
     }
     catch (NumberFormatException e)
     {
        // If it is not a value, next check whether it is a variable
        Pattern varPattern = Pattern.compile("[a-zA-Z]+");
        Matcher varMatcher = varPattern.matcher(s);
        Logger.debug("Not a value expression: " + s);

        // A variable may only occur within the scope of an assignment operator
        if (varMatcher.matches())
          if (context == null)
            throw new ParseException("Variable " + s +
                                     " is not allowed: not within the context of a let expression.", 0);
          else
          {
             // If in context, then obtain the corresponding Variable object, obtained from the context
             // If not found, an exception will be thrown, so just let it pass through...
             Logger.debug("Looking for variable " + s);
             return context.findVariable(s);
          }
        else
        {
           Pattern operatorPattern = Pattern.compile("([a-z]+)\\((.+)\\)");
           Matcher operatorMatcher = operatorPattern.matcher(s);
           Logger.debug("Not a variable expression: " + s);

            // Once we get here, the only case left is an operator.
           if (operatorMatcher.matches())
           {
              String arg1, arg2;
              Expression exp1, exp2;
              Expression newExp = null;
              String operator = operatorMatcher.group(1);
              String argumentString = operatorMatcher.group(2);  // Note that parentheses have been stripped
              int commaPos;
              boolean processingAssignment = operator.equals("let");
              Assignment newContext = context;  // Initialize now, in case this is not a let operator
              Logger.debug("Operator is " + operator + " with arguments " + argumentString);

              // The assignment operator has an extra argument (the variable being defined)
              // so we process it first.
              if (processingAssignment)
              {
                 String varName;
                 commaPos = argumentString.indexOf(',');
                 if (commaPos == -1)
                   throw new ParseException("Unable to parse arguments for let expression: " +
                                               argumentString, 0);
                 
                 varName = argumentString.substring(0, commaPos);
                 argumentString = argumentString.substring(commaPos+1);

                 varMatcher = varPattern.matcher(varName);
                 if (!varMatcher.matches())
                   throw new ParseException("Invalid variable name in let expression: " +
                                               varName, 0);
              
                 Logger.debug("Variable name is " + varName);

                 // This is tricky. We must instantiate the Assignment now, even though
                 // it is not yet complete (we have not parsed all its arguments).
                 // This is necessary in order to provide a context for the parsing of the
                 // third argument.
                 newContext = new Assignment(varName, context);
              }

              // Now we are looking for two expressions, separated by a comma.
              // However, there may be many commas if there are nested operators.
              // The one we want will be the one not enclosed within any set of parentheses.
              commaPos = findComma(argumentString);

              Logger.debug("Found comma at position " + commaPos);

              arg1 = argumentString.substring(0, commaPos);
              arg2 = argumentString.substring(commaPos+1);
              Logger.info("First argument is " + arg1);
              Logger.info("Second argument is " + arg2);

              // Parse the first expression using the "old" context.
              // In an assignment operation, the first expression, which provides the value
              // of the variable being defined, may obviously not refer to this variable
              exp1 = build(arg1, context);

              // Now update the context, because it will be needed for the second expression
              if (processingAssignment)
                 newContext.setVariable(exp1);

              exp2 = build(arg2, newContext);

              if (processingAssignment)
              {
                 newContext.setValue(exp2);
                 return newContext;
              }

              if (operator.equals("add"))
                newExp = new Addition(exp1, exp2, context);
              else if (operator.equals("sub"))
                newExp = new Subtraction(exp1, exp2, context);
              else if (operator.equals("mult"))
                newExp = new Multiplication(exp1, exp2, context);
              else if (operator.equals("div"))
                newExp = new Division(exp1, exp2, context);
              else throw new ParseException("Unknown operator " + operator, 0);

              return newExp;
           }
           else throw new ParseException("Unable to parse " + s + " as an expression.", 0);
        }
     }
  }


  /**
   * Looks for a top level comma (i.e. one not enclosed in any set of parentheses).
   * @param  s  two expressions separated by a comma
   * @return  index of the top level comma
   */
  protected static int findComma(String argumentString) throws ParseException
  {
     char c;
     int parCount = 0;
     int curPos = 0;

     // Go through the input string, counting parentheses
     do
     {
        c = argumentString.charAt(curPos);
        if (c == ',' &&  parCount == 0)
          return curPos;
        else if (c == '(')
               parCount++;
        else if (c == ')')
               parCount--;
     } while (++curPos < argumentString.length());

     throw new ParseException("Unable to find the comma separating the arguments in " + argumentString, 0);
  }
}



/**
 * The most basic kind of Expression
 */
class Value extends Expression
{
   int myValue;

   public Value(int val)
   {
      myValue = val; 
   }

   public int eval()
   {
      return myValue; 
   }
}


/**
 * A Variable is really just a Value with a name...
 */
class Variable extends Value
{
   public final String name;


  /**
   * The expression providing the value is evaluated immediately (there is no point in waiting)
   * @param  varName  name of the variable being defined
   * @param  varValue expression providing the value of the newly defined variable
   */
   public Variable(String varName, Expression varValue)
   {
      super(varValue.eval());
      name = varName;
   }
}



/**
 * The base class for all operations. They may be nested within another operation
 * which provides the context for eventual variable references.
 */
abstract class ContextualExpression extends Expression
{
   protected final Assignment parent;

  /**
   * The only significant aspect of a contextual expression is its enclosing Assignment expression (if any).
   * @param  context  enclosing expression
   */
   public ContextualExpression(Assignment context)
   {
      parent = context;
   }
}



/**
 * The base class for all arithmetic operations.
 */
abstract class Arithmetic extends ContextualExpression
{
   protected Expression first, second;

  /**
   * 
   * @param  e1  first operand
   * @param  e2  second operand
   * @param  e3  enclosing expression (if any)
   */
   public Arithmetic(Expression e1, Expression e2, Assignment e3)
   {
      super(e3);
      first = e1; 
      second = e2;
   }
}

class Addition extends Arithmetic
{
   public Addition(Expression e1, Expression e2, Assignment e3)
   { 
      super(e1,e2,e3);
      Logger.debug("Addition constructor");
   }

   public int eval()
   {
      Logger.info("Performing addition");
      return first.eval() + second.eval(); 
   }
}

class Subtraction extends Arithmetic
{
   public Subtraction(Expression e1, Expression e2, Assignment e3)
   { 
      super(e1,e2,e3);
   }

   public int eval()
   {
      Logger.info("Performing subtraction");
      return first.eval() - second.eval(); 
   }
}

class Multiplication extends Arithmetic
{
   public Multiplication(Expression e1, Expression e2, Assignment e3)
   { 
      super(e1,e2,e3);
   }

   public int eval()
   {
      Logger.info("Performing multiplication");
      return first.eval() * second.eval(); 
   }
}

class Division extends Arithmetic
{
   public Division(Expression e1, Expression e2, Assignment e3)
   { 
      super(e1,e2,e3);
   }

   public int eval()
   {
      Logger.info("Performing division");
      return first.eval() / second.eval(); 
   }
}


/**
 * Assignment operation which defines a variable and assigns it a value.
 */
class Assignment extends ContextualExpression
{
   public final String myVarName;
   protected Variable myVar;
   protected Expression second;

  /**
   * When parsing an assignment expression, the object needs to be instantiated
   * before parsing the enclosed expressions, so there is no point in trying to set
   * the corresponding members in the constructor. We just take what is available...
   * @param  name  newly defined variable identifier
   * @param  parent  enclosing expression (if any)
   */
   public Assignment(String name, Assignment parent)
   {
      super(parent);
      myVarName = name;
      Logger.debug("Assignment constructor. Variable Name = " + name);
   }

  /**
   * Looks for a locally defined variable.
   * @param  name  variable name
   * @return  corresponding Variable, null if not defined here
   */
   protected Variable getVariable(String name)
   {
      Logger.debug("Checking (" + myVarName + "), looking for (" + name + ')');
      return myVarName.equals(name) ? myVar : null;
   }

  /**
   * Instantiates a Variable once its defining expression is available.
   * @param  value  expression providing the variable's value
   */
   void setVariable(Expression value)
   {
      myVar = new Variable(myVarName, value);
   }


  /**
   * There is nothing special to do with the second expression (which provides the value
   * of the assignment operation. Just store it until needed (when the expression is evaluated)
   * @param  e  expression providing the operation's value
   */
   void setValue(Expression e)
   {
      second = e;
   }



  /**
   * Looks for a variable within this expression's context hierarchy.
   * @param  name  variable name
   * @return  corresponding Variable (will never be null: exception thrown if not found)
   */
   public Variable findVariable(String name) throws ParseException
   {
      // First, check whether the variable is defined here.
      Variable v = getVariable(name);

      Logger.debug("Looking for variable " + name);

      // If not defined here, and if this expression has a parent, look recursively
      if (v == null && parent != null)
        v = parent.findVariable(name);

      if (v != null)
        return v;
      else throw new ParseException("Undefined variable: " + name, 0);
   }

   public int eval()
   {
      Logger.info("Performing assignment");
      return second.eval(); 
   }
}