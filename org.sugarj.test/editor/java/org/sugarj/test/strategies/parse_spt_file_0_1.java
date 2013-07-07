package org.sugarj.test.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.model.ModelFactory;
import org.eclipse.imp.parser.IParseController;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.dynamicloading.DynamicParseController;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.strategoxt.imp.runtime.parser.SGLRParseController;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * parse-spt-string strategy to get AST of SugarTest testsuite, 
 * where the input fragments have been annotated with the AST of the input.
 * 
 * The current term is the name of the file to parse and the subsequemnt argument is the name 
 * of the project containing the file
 */
public class parse_spt_file_0_1 extends Strategy {

	public static parse_spt_file_0_1 instance = new parse_spt_file_0_1();
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm projectName) {
		if (!isTermString(current)  || !isTermString(projectName)) return null;
		String filename = ((IStrategoString)current).stringValue();
		
		ISourceProject project = findProject(((IStrategoString)projectName).stringValue());
		if(project == null) return null;
		
		File file = new File(filename);

		Descriptor descriptor = Environment.getDescriptor(LanguageRegistry.findLanguage("SugarTest"));
		IStrategoTerm result = null;
		try {
			IParseController ip = descriptor.createParseController();
			if (ip instanceof DynamicParseController)
				ip = ((DynamicParseController) ip).getWrapped();
			if (ip instanceof SGLRParseController) {
				SGLRParseController sglrController = (SGLRParseController) ip;
				sglrController.initialize(new Path(filename), project, null);
				// Must lock the parse lock of this controller
				// or hit the assertion in AbstractSGLRI.parse
				sglrController.getParseLock().lock();
				try {
					JSGLRI parser = sglrController.getParser(); 
					parser.setUseRecovery(false);
					result = parser.parse(new FileInputStream(file), file.getAbsolutePath());
				} finally {
					sglrController.getParseLock().unlock();
				}
			}
		} catch (BadDescriptorException e) {
			Environment.logException("Could not parse testing string", e);
		} catch (TokenExpectedException e) {
			Environment.logException("Could not parse testing string", e);
		} catch (BadTokenException e) {
			Environment.logException("Could not parse testing string", e);
		} catch (SGLRException e) {
			Environment.logException("Could not parse testing string", e);
		} catch (IOException e) {
			Environment.logException("Could not parse testing string", e);
		}
		
		return result;
	}
	
	private static ISourceProject findProject(String projectName) {
	  IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    IProject[] projects = root.getProjects();
    for (IProject project : projects) {
      if(project.getLocation().toPortableString().equals(projectName)) {
        try {
          return ModelFactory.open(project);
        } catch(ModelFactory.ModelException e) {
          return null;
        }
      }
    }
    return null;
	}

}
