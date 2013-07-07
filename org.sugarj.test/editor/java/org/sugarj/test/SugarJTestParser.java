package org.sugarj.test;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.sugarj.BaseLanguageRegistry;
import org.sugarj.AbstractBaseLanguage;
import org.sugarj.common.CommandExecution;
import org.sugarj.common.Environment;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.RelativePath;
import org.sugarj.driver.Driver;
import org.sugarj.editor.SugarJConsole;
import org.sugarj.editor.SugarJParseController;
import org.sugarj.editor.SugarJParser;

/**
 * A synchronous SugarJ parser for testing purposes
 * 
 * @author Bob Davison
 *
 */
class SugarJTestParser extends SugarJParser {

  private Environment environment;
  
  SugarJTestParser(JSGLRI parser) {
    super(parser);
    environment = makeTestEnvironment();
  }
  
  @Override
  public IStrategoTerm doParse(String input, String filename) throws IOException {
    AbstractBaseLanguage baseLanguage = BaseLanguageRegistry.getInstance().getBaseLanguage(FileCommands.getExtension(filename));
    if(baseLanguage == null) {
      // SugarTestJSGLRI sets the files extension so this should never happen 
      throw new RuntimeException("Unknown source-file extension " + FileCommands.getExtension(filename));
    }
    
    prepareConsole();
    
    try {
       return Driver.run(input, makeRelative(filename), environment, new NullProgressMonitor(), baseLanguage).getSugaredSyntaxTree();
    } catch(IOException ioe) {
      throw(ioe);
    } catch(Exception e) {
		  throw new RuntimeException(e);
		}
  }  
  
  private void prepareConsole() {
    CommandExecution.SILENT_EXECUTION = false;
    CommandExecution.SUB_SILENT_EXECUTION = false;
    CommandExecution.FULL_COMMAND_LINE = true;
    
    Log.out = SugarJConsole.getOutputPrintStream();
    Log.err = SugarJConsole.getErrorPrintStream();
    SugarJConsole.activateConsoleOnce();
  }
  
  private Environment makeTestEnvironment() {
    IProject project = getController().getProject().getRawProject();
    org.sugarj.common.Environment env = SugarJParseController.makeProjectEnvironment(project);
    org.sugarj.common.path.Path testdir = new org.sugarj.common.path.RelativePath(env.getRoot(), ".sugartest");
    env.setGenerateFiles(true);
    env.setBin(testdir);
    env.getSourcePath().add(testdir);
    return env;
  }
  
  private RelativePath makeRelative(String filename) {
    Path root = new Path(environment.getRoot().getAbsolutePath());
    String relativeName = new Path(filename).makeRelativeTo(root).toString();
    return environment.createOutPath(relativeName);
  }
  
}
