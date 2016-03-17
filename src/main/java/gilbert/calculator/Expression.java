package gilbert.calculator;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.ParseException;

public abstract class Expression
{
  public Expression()
  {
  }

  abstract int eval();

  public static Expression build(String s) throws ParseException
  {
     return build(s, null);
  }

  private static Expression build(String s, ContextualExpression context) throws ParseException
  {
     int intValue;

     try
     {
        intValue = Integer.parseInt(s); 
        return new Value(intValue);
     }
     catch (NumberFormatException e)
     {
        Pattern varPattern = Pattern.compile("[a-zA-Z]+");
        Matcher varMatcher = varPattern.matcher(s);
        Logger.debug("Not a value expression: " + s);

        if (varMatcher.matches())
          if (context == null)
            throw new ParseException("Variable " + s +
                                     " is not allowed: not within the context of a let expression.", 0);
          else
          {
             Logger.debug("Looking for variable " + s);
             return context.findVariable(s);
          }
        else
        {
           Pattern operatorPattern = Pattern.compile("([a-z]+)\\((.+)\\)");
           Matcher operatorMatcher = operatorPattern.matcher(s);
           Logger.debug("Not a variable expression: " + s);

           if (operatorMatcher.matches())
           {
              String arg1, arg2;
              Expression exp1, exp2;
              Expression newExp = null;
              String operator = operatorMatcher.group(1);
              String argumentString = operatorMatcher.group(2);
              int commaPos;
              boolean processingAssignment = operator.equals("let");
              ContextualExpression newContext = context;
              Assignment assignment = null;
              Logger.debug("Operator is " + operator + " with arguments " + argumentString);

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
                 newContext = assignment = new Assignment(varName, context);
              }
              commaPos = findComma(argumentString);
              Logger.debug("Found comma at position " + commaPos);
              arg1 = argumentString.substring(0, commaPos);
              arg2 = argumentString.substring(commaPos+1);
              Logger.info("First argument is " + arg1);
              Logger.info("Second argument is " + arg2);
              exp1 = build(arg1, context);

              if (processingAssignment)
                 assignment.setVariable(exp1);

              exp2 = build(arg2, newContext);

              if (processingAssignment)
              {
                 assignment.setValue(exp2);
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

  protected static int findComma(String argumentString) throws ParseException
  {
     char c;
     int commaCount = 0;
     int curPos = 0;

     do
     {
        c = argumentString.charAt(curPos);
        if (c == ',' &&  commaCount == 0)
          return curPos;
        else if (c == '(')
               commaCount++;
        else if (c == ')')
               commaCount--;
     } while (++curPos < argumentString.length());

     throw new ParseException("Unable to find the comma separating the arguments in " + argumentString, 0);
  }
}

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

class Variable extends Value
{
   public final String name;

   public Variable(String varName, Expression varValue)
   {
      super(varValue.eval());
      name = varName;
   }
}

abstract class ContextualExpression extends Expression
{
   protected final ContextualExpression parent;

   public ContextualExpression(ContextualExpression context)
   {
      parent = context;
   }

   protected Variable getVariable(String name)
   {
      return null;
   }

   public Variable findVariable(String name) throws ParseException
   {
      Variable v = getVariable(name);

      Logger.debug("Looking for variable " + name);
      if (v == null && parent != null)
        v = parent.getVariable(name);

      if (v != null)
        return v;
      else throw new ParseException("Undefined variable: " + name, 0);
   }
}

abstract class Arithmetic extends ContextualExpression
{
   protected Expression first, second;

   public Arithmetic(Expression e1, Expression e2, ContextualExpression e3)
   {
      super(e3);
      first = e1; 
      second = e2;
   }
}

class Addition extends Arithmetic
{
   public Addition(Expression e1, Expression e2, ContextualExpression e3)
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
   public Subtraction(Expression e1, Expression e2, ContextualExpression e3)
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
   public Multiplication(Expression e1, Expression e2, ContextualExpression e3)
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
   public Division(Expression e1, Expression e2, ContextualExpression e3)
   { 
      super(e1,e2,e3);
   }

   public int eval()
   {
      Logger.info("Performing division");
      return first.eval() / second.eval(); 
   }
}

class Assignment extends ContextualExpression
{
   public final String myVarName;
   protected Variable myVar;
   protected Expression second;

   public Assignment(String name, ContextualExpression parent)
   {
      super(parent);
      myVarName = name;
      Logger.debug("Assignment constructor. Variable Name = " + name);
   }

   protected Variable getVariable(String name)
   {
      Logger.debug("Checking (" + myVarName + "), looking for (" + name + ')');
      return myVarName.equals(name) ? myVar : null;
   }

   void setVariable(Expression value)
   {
      myVar = new Variable(myVarName, value);
   }

   void setValue(Expression e)
   {
      second = e;
   }

   public int eval()
   {
      Logger.info("Performing assignment");
      return second.eval(); 
   }
}