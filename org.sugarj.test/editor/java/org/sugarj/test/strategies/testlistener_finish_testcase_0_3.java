package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.testing.listener.ITestListener;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;
import org.sugarj.test.listener.helper.ListenerWrapper;

public class testlistener_finish_testcase_0_3 extends Strategy {

	public static testlistener_finish_testcase_0_3 instance = new testlistener_finish_testcase_0_3();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm arg0, IStrategoTerm arg1,
			IStrategoTerm arg2) {
		if (!isTermString(arg0) || !isTermString(arg1))
			return null;

		String ts = ((IStrategoString) arg0).stringValue();
		String desc = ((IStrategoString) arg1).stringValue();
		String appl = ((IStrategoAppl) arg2).getConstructor().getName();

		try {
			ITestListener listener = ListenerWrapper.instance();
			if (appl.equals("True"))
				listener.finishTestcase(ts, desc, true);
			else
				listener.finishTestcase(ts, desc, false);
		} catch (Exception e) {
			ITermFactory factory = context.getFactory();
			Environment.logException("Failed to finish test case to listener. Maybe no listeners?", e);
			return factory.makeAppl(
					factory.makeConstructor("Error", 1),
					factory.makeString("Failed to finish test case to listener. Maybe no listeners?: "
							+ e.getLocalizedMessage()));
		}

		return current;
	}

}
