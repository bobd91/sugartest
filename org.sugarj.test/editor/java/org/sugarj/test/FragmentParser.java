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
 * Removed all parse result caching as SugarJ Driver does caching for us, and it tracks changes to dependent files
 * 
 * Modify fragment handling so that SugarJ doesn't have to keep re-compiling the same code just because of differences in whitespace
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 * @author Bob Davison
 *
 */
public class FragmentParser {
  
  private static final int FRAGMENT_PARSE_TIMEOUT = 3000;
  
  /**
   * HACK: edit/save on same file avoids the Spoofax
   * parse lock as different ParseControllers are used.
   * We must avoid parallel parses of the same file as SugarJ
   * will interrupt one of the parses causing errors on good tests.
   * Force the use of a single parser for each file so we can parse lock.
   */
  private static Map<IPath, JSGLRI> parsers = new HashMap<IPath, JSGLRI>();

  private final IStrategoConstructor[] setup_3;

  private Descriptor parseCacheDescriptor;
  
  private JSGLRI parser;

  private List<FragmentRegion> setupRegions;
  
  private boolean isLastSyntaxCorrect;

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
  
  private static IPath standardPath(IPath path) {
    // Path from edit has no device set but path from save does so we remove it to get a match
    return path.setDevice(null);
  }

  private static synchronized JSGLRI getParser(Descriptor descriptor, IPath path, ISourceProject project) {
    JSGLRI result = parsers.get(standardPath(path));
    if(result != null)
      return result;
    
    try {
      if (descriptor == null) return null;
      
      IParseController controller;
      controller = descriptor.createParseController();
      if (controller instanceof DynamicParseController)
        controller = ((DynamicParseController) controller).getWrapped();
      if (controller instanceof SGLRParseController) {
        SGLRParseController sglrController = (SGLRParseController) controller;
        controller.initialize(path, project, null);
        result = sglrController.getParser(); 
        
        if(result instanceof SugarJParser) {
          result = new SugarJTestParser(result);
          sglrController.setParser(result);
        } else {
          throw new IllegalStateException(
              new BadDescriptorException("SugarJParser expected: " + result.getClass().getName()));          
        }

        result.setTimeout(FRAGMENT_PARSE_TIMEOUT);
        result.setUseRecovery(true);
        parsers.put(standardPath(path), result);
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
      SGLRParseController controller = parser.getController();
      controller.getParseLock().lock();
      try {
        parsed = parser.parse(fragment.getText(), filename);
      } finally {
        controller.getParseLock().unlock();
      }
    
      if(parsed == null) {
        isLastSyntaxCorrect = false;
        return fragmentTerm;
      } else {
        parseInfo = FragmentParseInfo.cacheAdd(filename, fragment, parsed);
        SourceAttachment.putSource(parsed, SourceAttachment.getResource(fragmentTerm), controller);
      }
    }
      
    isLastSyntaxCorrect = parseInfo.isSyntaxCorrect();
    return fragment.realign(parseInfo.getParsed(), parseInfo.originalTokenizer());
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