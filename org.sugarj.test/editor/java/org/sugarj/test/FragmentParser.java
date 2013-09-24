package org.sugarj.test;

import static org.sugarj.test.AstConstructors.INPUT_4;
import static org.spoofax.terms.Term.tryGetConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.parser.IParseController;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.spoofax.terms.TermVisitor;
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
  
  private final IStrategoConstructor[] setup_3;

  private Descriptor parseCacheDescriptor;
  
  private JSGLRI parser;

  private List<FragmentRegion> setupRegions;
  
  private boolean isLastSyntaxCorrect;
  
  /*
   * Mustn't use controller.getParseLock().lock()
   * as edit/save use different controllers permitting simultaneous parses of the
   * same file.  SugarJ will interrupt one of the parses causing
   * parse failure on a possibly good file.
   * 
   * Use a lock based on the name of the file being edited
   */
  private Map<String, Object> parseLocks = new HashMap<String, Object>();

  public FragmentParser(IStrategoConstructor... setup_3) {
    this.setup_3 = setup_3;
  }

  public void configure(Descriptor descriptor, IPath path, ISourceProject project, IStrategoTerm ast) {
    if (parseCacheDescriptor != descriptor) {
      parseCacheDescriptor = descriptor;
      parser = getParser(descriptor, path, project);
    }

    setupRegions = getSetupRegions(ast);
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

  public IStrategoTerm parse(ITokenizer oldTokenizer, IStrategoTerm fragmentTerm, String filename)
      throws TokenExpectedException, BadTokenException, SGLRException
      , IOException, InterruptedException {
    
    Fragment fragment = new Fragment(oldTokenizer.getInput(), fragmentTerm, setupRegions);
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
  
  private List<FragmentRegion> getSetupRegions(IStrategoTerm ast) {
    final List<FragmentRegion> results = new ArrayList<FragmentRegion>();
    new TermVisitor() {
      public void preVisit(IStrategoTerm term) {
        IStrategoConstructor constructor = tryGetConstructor(term);
        for(IStrategoConstructor setup : setup_3) {
          if (constructor == setup) {
            new TermVisitor() {
              public final void preVisit(IStrategoTerm term) {
                IStrategoConstructor constructor = tryGetConstructor(term);
                if (constructor == INPUT_4) {
                  results.add(new FragmentRegion(term));
                }
              }
            }.visit(term);
            return;
          }
        }
      }
    }.visit(ast);
    return results;
  }
  
  
  public boolean isLastSyntaxCorrect() {
    return isLastSyntaxCorrect;
  }
  
}