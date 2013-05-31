package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.asJavaString;

import java.util.List;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.dynamicloading.TermReader;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class plugin_get_property_values_0_1 extends Strategy {

	public static plugin_get_property_values_0_1 instance = new plugin_get_property_values_0_1();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm property, IStrategoTerm language) {
		try {
			Descriptor descriptor = ObserverCache.getInstance().getDescriptor(asJavaString(language));
			List<IStrategoAppl> results = TermReader.collectTerms(descriptor.getDocument(), asJavaString(property));
			return context.getFactory().makeList(results.toArray(new IStrategoTerm[results.size()]));
		} catch (BadDescriptorException e) {
			return null;
		}
	}
}
