package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.asJavaString;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;
import org.sugarj.LanguageLibRegistry;

/**
 * @author Bob Davison
 */
public class is_sugar_language_0_1 extends Strategy {

	public static is_sugar_language_0_1 instance = new is_sugar_language_0_1();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm language) {
		if(null != LanguageLibRegistry.getInstance().getLanguageLibByName(asJavaString(language))) {
			return current;
		} else {
			return null;
		}
	}
}