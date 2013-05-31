package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Display;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.imp.runtime.EditorState;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.stratego.EditorIOAgent;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class open_editor_0_0 extends Strategy {

	public static open_editor_0_0 instance = new open_editor_0_0();
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		if (!isTermString(current)) return null;

		String f = ((IStrategoString)current).stringValue();
		try {
			IFile file = EditorIOAgent.getFile(HybridInterpreter.getContext(context), f);
			if (file.exists()) {
				EditorState.asyncOpenEditor(Display.getDefault(), file, true);
			} else {
				Environment.logException("File does not exist: "+f);
			}
		} 
		catch(FileNotFoundException e) {
			Environment.logException("File not in workspace: "+f);
		}
		return null;
	}

}
