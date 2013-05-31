package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.asJavaString;

import org.eclipse.core.resources.IProject;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.services.StrategoObserver;
import org.strategoxt.imp.runtime.stratego.EditorIOAgent;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * Invoke a strategy in a stratego instance belonging to a language plugin.
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class plugin_strategy_evaluate_1_2 extends Strategy {

	public static plugin_strategy_evaluate_1_2 instance = new plugin_strategy_evaluate_1_2();

	/**
	 * @return Fail(trace) for strategy failure, Error(message) a string for errors, 
	 *         or Some(term) for success.
	 */
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, Strategy printTrace, IStrategoTerm languageName, IStrategoTerm strategy) {
		ITermFactory factory = context.getFactory();
		try {
			String projectPath = ((EditorIOAgent) context.getIOAgent()).getProjectPath();
			IProject project = ((EditorIOAgent) context.getIOAgent()).getProject();
			StrategoObserver observer = ObserverCache.getInstance().getObserver(asJavaString(languageName), project, projectPath);
			observer.getRuntime().setCurrent(current);
			if (observer.getRuntime().evaluate((IStrategoAppl) strategy, true)) {
				current = observer.getRuntime().current();
				current = factory.makeAppl(factory.makeConstructor("Some", 1), current);
				return current;
			} else {
				IStrategoString trace = factory.makeString("rewriting failed\n" + context.getTraceString());
				if (printTrace.invoke(context, trace) != null)
					observer.reportRewritingFailed();
				return factory.makeAppl(factory.makeConstructor("Fail", 1), trace);
			}
		} catch (BadDescriptorException e) {
			Environment.logException("Problem loading descriptor for testing", e);
			return factory.makeAppl(factory.makeConstructor("Error", 1),
					factory.makeString("Problem loading descriptor for testing: " + e.getLocalizedMessage()));
		} catch (InterpreterException e) {
			Environment.logWarning("Problem evaluating strategy for testing", e);
			String message = e.getLocalizedMessage();
			if (e.getCause() != null)
				message += "; " + e.getCause().getMessage();
			return factory.makeAppl(factory.makeConstructor("Error", 1),
					factory.makeString(message));
		} catch (RuntimeException e) {
			Environment.logException("Problem evaluating strategy for testing", e);
			return factory.makeAppl(factory.makeConstructor("Error", 1),
					factory.makeString(e.getClass().getName() + ": " + e.getLocalizedMessage() + " (see error log)"));
		}
	}

}
