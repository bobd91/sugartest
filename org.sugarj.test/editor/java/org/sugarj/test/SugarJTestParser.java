package org.sugarj.test;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
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
import org.sugarj.driver.ModuleSystemCommands;
import org.sugarj.driver.Result;
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
      // SugarTestJSGLRI sets the file extension so this should never happen 
      throw new RuntimeException("Unknown source-file extension " + FileCommands.getExtension(filename));
    }
    
    prepareConsole();
    
    try {
       Result result = Driver.run(input, makeRelative(filename), environment, new NullProgressMonitor(), baseLanguage);
       IStrategoTerm sugared = result.getSugaredSyntaxTree();
       if(sugared != null)
         SugarTestAttachment.put(sugared,  result.getDesugaredSyntaxTree());
       return sugared;
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
    env.setGenerateFiles(false);
    return env;
  }
  
  private RelativePath makeRelative(String filename) {
    Path filePath = new Path(filename);
    IPath relativePath = relativeToSourceDir(filePath, environment.getSourcePath());
    return environment.createOutPath(relativePath.toString());
  }
  
  private IPath relativeToSourceDir(Path filePath, List<org.sugarj.common.path.Path> sourcePaths) {
    for(org.sugarj.common.path.Path sourcePath : sourcePaths) {
      IPath path = new Path(sourcePath.toString());
      if(path.isPrefixOf(filePath)) {
        return filePath.makeRelativeTo(path);
      }
    }
    Path root = new Path(environment.getRoot().getAbsolutePath());
    return filePath.makeRelativeTo(root);
  }
  
}
