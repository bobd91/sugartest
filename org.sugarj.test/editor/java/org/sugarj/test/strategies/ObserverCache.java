package org.sugarj.test.strategies;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.services.StrategoObserver;

/** 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class ObserverCache {
	
	private static final ObserverCache instance = new ObserverCache();
	
	// We need to maintain a reference here to each observer as long as the descriptor lives,
	// as there may not be another reference betweens calls to this class.
	private static final Map<Descriptor, StrategoObserver> asyncCache =
		new WeakHashMap<Descriptor, StrategoObserver>();

	private ObserverCache() {}
	
	public static ObserverCache getInstance() {
		return instance;
	}

	public StrategoObserver getObserver(String languageName, IProject project, String projectPath) throws BadDescriptorException {
		Descriptor descriptor = getDescriptor(languageName);
		
		return getObserver(descriptor, project, projectPath);
	}

	public Descriptor getDescriptor(String languageName) throws BadDescriptorException {
		Language language = LanguageRegistry.findLanguage(languageName);
		if (language == null) throw new BadDescriptorException("No language known with the name " + languageName);
		Descriptor descriptor = Environment.getDescriptor(language);
		if (descriptor == null) throw new BadDescriptorException("No language known with the name " + languageName);
		descriptor.setDynamicallyLoaded(true); // make console visible
		return descriptor;
	}

	private synchronized StrategoObserver getObserver(Descriptor descriptor, IProject project, String projectPath) throws BadDescriptorException {
		assert project != null;
		StrategoObserver result = asyncCache.get(descriptor);

		if (result == null)
			result = descriptor.createService(StrategoObserver.class, null);
		
		result.getLock().lock();
		try {
			result.configureRuntime(project, projectPath);
			asyncCache.put(descriptor, result);
		} catch (NoSuchMethodError e) {
			Environment.logException("Oops, old Spoofax version installed, cannot properly initialize runtime!", e);
		} finally {
			result.getLock().unlock();
		}
		return result;
	}
}
