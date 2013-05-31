package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class input_dialog_0_3 extends Strategy {

	public static input_dialog_0_3 instance = new input_dialog_0_3();
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm t1, IStrategoTerm t2, IStrategoTerm t3) {
		if (!isTermString(t1)) return null;
		if (!isTermString(t2)) return null;
		if (!isTermString(t3)) return null;
		
		String title   = ((IStrategoString)t1).stringValue();
		String message = ((IStrategoString)t2).stringValue();
		String value   = ((IStrategoString)t3).stringValue();

        final InputDialog d = new InputDialog(null, title, message, value, null);
		Display.getDefault().syncExec(new Runnable() {
            public void run() {
            	d.open();
            }
        });		
        
		return context.getFactory().makeString(d.getValue());
	}

}
