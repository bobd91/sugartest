package org.sugarj.test;

/**
 * Shared access to AST Constructors 
 */
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.strategoxt.imp.runtime.Environment;

public class AstConstructors {

  public static final IStrategoConstructor FAILS_PARSING_0 =
      Environment.getTermFactory().makeConstructor("FailsParsing", 0);
  
  public static final IStrategoConstructor SOME_1 =
      Environment.getTermFactory().makeConstructor("Some", 1);

  public static final IStrategoConstructor SETUP_3 =
    Environment.getTermFactory().makeConstructor("Setup", 3);

  public static final IStrategoConstructor INPUT_3 =
    Environment.getTermFactory().makeConstructor("Input", 3);
  
  public static final IStrategoConstructor OUTPUT_3 =
    Environment.getTermFactory().makeConstructor("Output", 3);
  
  public static final IStrategoConstructor QUOTEPART_1 =
    Environment.getTermFactory().makeConstructor("QuotePart", 1);
  
  public static final IStrategoConstructor ERROR_1 =
    Environment.getTermFactory().makeConstructor("Error", 1);
  
  public static final IStrategoConstructor LANGUAGE_1 =
    Environment.getTermFactory().makeConstructor("Language", 1);

}