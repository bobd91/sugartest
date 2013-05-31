package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class testlistener_open_0_0 extends Strategy {

	public static testlistener_open_0_0 instance = new testlistener_open_0_0();
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		if (!isTermString(current)) return null;
		
//		Display.getDefault().syncExec(new Runnable() {
//            public void run() {
//				try {
//					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.strategoxt.imp.testing.views.testrunview");
//				} catch (PartInitException e) {
//					e.printStackTrace();
//				}
//            }
//		});
		
		return current;
	}

}
