package org.sugarj.test;

import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.strategoxt.imp.runtime.Environment;

public class AstConstructors {

  public static final IStrategoConstructor FAILS_PARSING_0 =
      Environment.getTermFactory().makeConstructor("FailsParsing", 0);
  
  public static final IStrategoConstructor SETUP_3 =
    Environment.getTermFactory().makeConstructor("Setup", 3);

  public static final IStrategoConstructor SUGAR_SETUP_3 =
    Environment.getTermFactory().makeConstructor("SugarSetup", 3);

  public static final IStrategoConstructor INPUT_4 =
    Environment.getTermFactory().makeConstructor("Input", 4);
  
  public static final IStrategoConstructor OUTPUT_4 =
    Environment.getTermFactory().makeConstructor("Output", 4);
  
  public static final IStrategoConstructor DESUGAR_4 =
    Environment.getTermFactory().makeConstructor("Desugar", 4);
  
  public static final IStrategoConstructor QUOTEPART_1 =
    Environment.getTermFactory().makeConstructor("QuotePart", 1);
  
  public static final IStrategoConstructor MARKED_TEST_NUMBER_3 =
    Environment.getTermFactory().makeConstructor("MarkedTestNumber", 3);

  public static final IStrategoConstructor ERROR_1 =
    Environment.getTermFactory().makeConstructor("Error", 1);
  
  public static final IStrategoConstructor LANGUAGE_1 =
    Environment.getTermFactory().makeConstructor("Language", 1);

}