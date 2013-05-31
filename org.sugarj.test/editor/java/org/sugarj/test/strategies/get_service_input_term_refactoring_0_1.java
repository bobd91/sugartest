package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.isTermAppl;
import static org.spoofax.interpreter.core.Tools.termAt;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.TermReader;
import org.strategoxt.imp.runtime.services.InputTermBuilder;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * @author Maartje de Jonge>
 */
public class get_service_input_term_refactoring_0_1 extends Strategy {

	public static get_service_input_term_refactoring_0_1 instance = new get_service_input_term_refactoring_0_1();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm info, IStrategoTerm analyzedAst) {
		// TODO: adapt to latest strategy of StrategoReferenceResolver?
		if (isTermAppl(analyzedAst) && ((IStrategoAppl) analyzedAst).getName().equals("None"))
			analyzedAst = null;
		IStrategoTerm[] semNodes = termAt(info, 1).getAllSubterms();
		boolean onSource = TermReader.findTerm(info, "Source") != null;
		IStrategoTerm current = termAt(info, 0);
		try {
			current = InputTermBuilder.getMatchingNode(semNodes, current, false);
			if(current == null) return null;
		} catch (BadDescriptorException e) {
			Environment.logException("Refactoring test failed", e);
		}
		HybridInterpreter runtime = HybridInterpreter.getInterpreter(context);
		InputTermBuilder inputBuilder = new InputTermBuilder(runtime, analyzedAst);
		return inputBuilder.makeInputTermRefactoring(termAt(info, 3), current, true, onSource);
	}
}
