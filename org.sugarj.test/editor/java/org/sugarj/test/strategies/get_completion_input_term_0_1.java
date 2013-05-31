package org.sugarj.test.strategies;

import org.spoofax.NotImplementedException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class get_completion_input_term_0_1 extends Strategy {

	public static get_completion_input_term_0_1 instance = new get_completion_input_term_0_1();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm original) {
		// TODO: new ContentProposerParser(new Regex ..
		/*
		current = InputTermBuilder.getMatchingAncestor(current, false);
		HybridInterpreter runtime = HybridInterpreter.getInterpreter(context);
		InputTermBuilder inputBuilder = new InputTermBuilder(runtime);
		return inputBuilder.makeInputTerm(current, true);
		*/
		throw new NotImplementedException();
	}

}