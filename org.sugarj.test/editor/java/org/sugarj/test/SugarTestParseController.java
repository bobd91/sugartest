package org.sugarj.test;

import org.eclipse.imp.parser.IParseController;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.strategoxt.imp.runtime.parser.SGLRParseController;

/**
 * Copied from org.strategoxt.imp.testing.SpoofaxTesterParseController
 * 
 * @author Bob Davison
 *
 */
public class SugarTestParseController extends
		SugarTestParseControllerGenerated {

	@Override
	public IParseController getWrapped() {
		getDescriptor().setAttachmentProvider(SugarTestParseController.class);
		IParseController result = super.getWrapped();
		if (result instanceof SGLRParseController) {
			JSGLRI parser = ((SGLRParseController) result).getParser();
			if (!(parser instanceof SugarTestJSGLRI)) {
				((SGLRParseController) result).setParser(new SugarTestJSGLRI(parser));
			}
		}
		return result;
	}
}