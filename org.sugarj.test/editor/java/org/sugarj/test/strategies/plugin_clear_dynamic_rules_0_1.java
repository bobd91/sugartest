package org.sugarj.test.strategies;
/*
import static org.spoofax.interpreter.core.Tools.asJavaString;

import org.spoofax.interpreter.library.ssl.SSLLibrary;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.services.StrategoObserver;
import org.strategoxt.imp.runtime.stratego.EditorIOAgent;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * Clear all dynamic rules in the stratego instance belonging to a language plugin.
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 *
public class plugin_clear_dynamic_rules_0_1 extends Strategy {

	public static plugin_clear_dynamic_rules_0_1 instance = new plugin_clear_dynamic_rules_0_1();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm languageName) {
		ITermFactory factory = context.getFactory();
		StrategoObserver observer = null;
		try {
			String dir = ((EditorIOAgent) context.getIOAgent()).getProjectPath();
			observer = ObserverCache.getInstance().getObserver(asJavaString(languageName), dir);
			observer.getLock().lock();
			SSLLibrary.instance(observer.getRuntime().getContext()).getDynamicRuleTable().clear();
			SSLLibrary.instance(observer.getRuntime().getContext()).getTableTable().clear();
			return current;
		} catch (BadDescriptorException e) {
			Environment.logException("Problem loading descriptor for testing", e);
			return factory.makeAppl(factory.makeConstructor("Error", 1),
					factory.makeString("Problem loading descriptor for testing: " + e.getLocalizedMessage()));
		} catch (RuntimeException e) {
			Environment.logException("Problem executing strategy for testing: " + getClass().getSimpleName(), e);
			return factory.makeAppl(factory.makeConstructor("Error", 1),
					factory.makeString(e.getClass().getName() + ": " + e.getLocalizedMessage() + " (see error log)"));
		} finally {
			if (observer != null) observer.getLock().unlock();
		}
	}

}
*/