package org.sugarj.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.parser.IParseController;
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
import org.strategoxt.imp.runtime.stratego.SourceAttachment;
import org.sugarj.editor.SugarJParser;

/**
 * Heavily modified copy of org.strategoxt.imp.testing.FragmentParser
 * for testing SugarJ languages
 * 
 * Removed all parse result caching as it didn't work, caching is done by FragmentParseInfo
 * 
 * Modify fragment handling so that SugarJ doesn't have to keep re-compiling the same code
 * just because of differences in whitespace
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 * @author Bob Davison
 *
 */
public class FragmentParser {
  
  private static final int FRAGMENT_PARSE_TIMEOUT = 3000;
  
  private Descriptor parseCacheDescriptor;
  
  private JSGLRI parser;

  private SetupRegion setupRegion;
  
  private boolean isLastSyntaxCorrect;
  
  /*
   * Mustn't use controller.getParseLock().lock()
   * as edit/save use different controllers permitting simultaneous parses of the
   * same file.  SugarJ will interrupt one of the parses causing
   * parse failure on a possibly good file.
   * 
   * Use a lock based on the name of the file being edited
   */
  private Map<String, Object> parseLocks = new HashMap<>();

  public void configure(Descriptor descriptor, IPath path, ISourceProject project, IStrategoTerm ast) {
    if (parseCacheDescriptor != descriptor) {
      parseCacheDescriptor = descriptor;
      parser = getParser(descriptor, path, project);
    }

    setupRegion = getSetupRegion(ast);
  }
  
  public boolean isInitialized() {
    return parser != null;
  }
  
  private static synchronized JSGLRI getParser(Descriptor descriptor, IPath path, ISourceProject project) {
    try {
      if (descriptor == null) return null;
      
      IParseController controller;
      controller = descriptor.createParseController();
      if (controller instanceof DynamicParseController)
        controller = ((DynamicParseController) controller).getWrapped();
      if (controller instanceof SGLRParseController) {
        SGLRParseController sglrController = (SGLRParseController) controller;
        controller.initialize(path, project, null);
        JSGLRI result = sglrController.getParser(); 
        
        if(result instanceof SugarJParser) {
          result = new SugarJTestParser(result);
          sglrController.setParser(result);
        } else {
          throw new IllegalStateException(
              new BadDescriptorException("SugarJParser expected: " + result.getClass().getName()));          
        }

        result.setTimeout(FRAGMENT_PARSE_TIMEOUT);
        result.setUseRecovery(true);

        return result;
      } else {
        throw new IllegalStateException(
          new BadDescriptorException("SGLRParseController expected: " + controller.getClass().getName()));
      }
    } catch (BadDescriptorException e) {
      Environment.logWarning("Could not load parser for testing language");
    } catch (RuntimeException e) {
      Environment.logWarning("Could not load parser for testing language");
    }
    return null;
  }

  public IStrategoTerm parse(String input, IStrategoTerm fragmentTerm, String filename)
      throws TokenExpectedException, BadTokenException, SGLRException
      , IOException, InterruptedException {
    
    Fragment fragment = new Fragment(input, fragmentTerm, setupRegion);
    FragmentParseInfo parseInfo = FragmentParseInfo.cacheGet(filename, fragment);

    if(parseInfo == null) {
      IStrategoTerm parsed;
      synchronized(getParseLock(filename)) {
        parsed = parser.parse(fragment.getText(), filename);
      }
      
      if(parsed == null) {
        isLastSyntaxCorrect = false;
        return fragmentTerm;
      } else {
        parseInfo = FragmentParseInfo.cacheAdd(filename, fragment, parsed);
        SGLRParseController controller = parser.getController();
        SourceAttachment.putSource(parsed, SourceAttachment.getResource(fragmentTerm), controller);
      }
    }
      
    isLastSyntaxCorrect = parseInfo.isSyntaxCorrect();
    return fragment.realign(parseInfo.getParsed(), parseInfo.originalTokenizer());
  }

  private synchronized Object getParseLock(String filename) {
    Object lock = parseLocks.get(filename);
    if(lock == null) {
      lock = new Object();
      parseLocks.put(filename,  lock);
    }
    return lock;
  }
  
  private SetupRegion getSetupRegion(IStrategoTerm ast) {
    return SetupRegion.setupRegionFor(ast);
   }
  
  
  public boolean isLastSyntaxCorrect() {
    return isLastSyntaxCorrect;
  }
  
}