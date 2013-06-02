package org.sugarj.test.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;
import org.sugarj.common.Log;

/**
 * @author Bob Davison
 */
public class sugar_log_0_1 extends Strategy {

  public static sugar_log_0_1 instance = new sugar_log_0_1();

  @Override
  public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm arg) {
    Log.log.log("SUGAR-LOG<" + arg + ">", Log.ALWAYS);
    return current;
  }
}