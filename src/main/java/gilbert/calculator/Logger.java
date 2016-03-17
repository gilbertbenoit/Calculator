package gilbert.calculator;

public class Logger
{
  private static int level = 0;
  public static final String INFO  = "INFO";
  public static final String ERROR = "ERROR";
  public static final String DEBUG = "DEBUG";

  private static int errorLevel = 0;
  private static int infoLevel = 1;
  private static int debugLevel = 2;

  public static void setLoggingLevel(String lvlString)
  {
    if (lvlString.equals(ERROR))
      level = errorLevel;
    else if (lvlString.equals(INFO))
      level = infoLevel;
    else if (lvlString.equals(DEBUG))
      level = debugLevel;
    else
      error("Unsupported logging level:" + lvlString + ". Supported levels are: "
         + ERROR + ". " + INFO + ". " + ERROR + '.');
  }


  public static void info(String msg)
  {
    logIt(infoLevel, INFO, msg);
  }


  public static void debug(String msg)
  {
    logIt(debugLevel, DEBUG, msg);
  }


  public static void error(String msg)
  {
    logIt(errorLevel, ERROR, msg);
  }


  protected static void logIt(int lvl, String lvlString, String msg)
  {
    if (level >= lvl)
      System.out.println(lvlString + ": " + msg);
  }
}