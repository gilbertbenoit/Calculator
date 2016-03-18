Expression Calculator
=====================
The main class for this expression calculator is gilbert.calculator.Calculator. It will evaluate one expression
provided on the command line. A logging level can also be specified (either before or after the expression),
as -DEBUG, -INFO or -ERROR. No whitespace is allowed anywhere in the expression.

An expression can consist of an integer, or an operator (add, sub, mult, div or let). Additionally, within the scope of a let expression,
a variable (as defined in the let) can be used anywhere.

Syntax: Calculator <expression> [-<logging level>] [-help]

Where:
- <logging level> can be INFO, DEBUG or ERROR

- <expression> can be an integer, or one of
  - add(<expression>,<expression>)
  - sub(<expression>,<expression>)
  - mult(<expression>,<expression>)
  - div(<expression>,<expression>)
  - let(<variable>,<expression>,<expression>)

- <variable> is an identifier consisting of lower or upper case letters.

Within the scope of a let operator, an <expression> may also be a <variable>.
The <variable> in a let operator defines a variable, which takes the value
of the first <expression> (which may of course not refer the variable being defined).
The second <expression> provides the value of the let operation.