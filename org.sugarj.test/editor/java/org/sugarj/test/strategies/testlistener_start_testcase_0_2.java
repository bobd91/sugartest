package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.testing.listener.ITestListener;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;
import org.sugarj.test.listener.helper.ListenerWrapper;

public class testlistener_start_testcase_0_2 extends Strategy {

	public static testlistener_start_testcase_0_2 instance = new testlistener_start_testcase_0_2();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm arg0, IStrategoTerm arg1) {
		if (!isTermString(arg0) || !isTermString(arg1))
			return null;

		String ts = ((IStrategoString) arg0).stringValue();
		String desc = ((IStrategoString) arg1).stringValue();

		try {
			ITestListener listener = ListenerWrapper.instance();
			listener.startTestcase(ts, desc);
		} catch (Exception e) {
			ITermFactory factory = context.getFactory();
			Environment.logException("Failed to start test case to listener. Maybe no listeners?", e);
			return factory.makeAppl(
					factory.makeConstructor("Error", 1),
					factory.makeString("Failed to start test case to listener. Maybe no listeners?: "
							+ e.getLocalizedMessage()));
		}

		return current;
	}

}
