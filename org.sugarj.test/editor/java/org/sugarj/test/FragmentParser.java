package org.sugarj.test;

import static org.spoofax.terms.Term.tryGetConstructor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
  
  private static final IStrategoConstructor QUOTEPART_1 =
    Environment.getTermFactory().makeConstructor("QuotePart", 1);
  
  private final IStrategoConstructor setup_3;

  private final IStrategoConstructor topsort_1;

  private Descriptor parseCacheDescriptor;
  
  private JSGLRI parser;

  private List<OffsetRegion> setupRegions;
  
  private boolean isLastSyntaxCorrect;

  public FragmentParser(IStrategoConstructor setup_3, IStrategoConstructor topsort_1) {
    assert setup_3.getArity() == 3;
    assert topsort_1.getArity() == 1;
    this.setup_3 = setup_3;
    this.topsort_1 = topsort_1;
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
  
  protected JSGLRI getParser(Descriptor descriptor, IPath path, ISourceProject project) {
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

  public IStrategoTerm parse(ITokenizer oldTokenizer, IStrategoTerm fragmentTerm, String filename, boolean ignoreSetup)
      throws TokenExpectedException, BadTokenException, SGLRException, IOException {
    
    Fragment fragment = new Fragment(oldTokenizer.getInput(), fragmentTerm, setupRegions);
    IStrategoTerm parsed;

    SGLRParseController controller = parser.getController();
    controller.getParseLock().lock();
    try {
      parsed = parser.parse(fragment.getText(), filename);
    } finally {
      controller.getParseLock().unlock();
    }
    
    FragmentParseInfo info = new FragmentParseInfo(parsed);
    isLastSyntaxCorrect = info.wasSyntaxCorrect();
    if(!info.wasCached()) {
      SourceAttachment.putSource(parsed, SourceAttachment.getResource(fragmentTerm), controller);
    }
    return fragment.retokenize(parsed, info.originalTokenizer());
  }

  
  private List<OffsetRegion> getSetupRegions(IStrategoTerm ast) {
    final List<OffsetRegion> results = new ArrayList<OffsetRegion>();
    new TermVisitor() {
      public void preVisit(IStrategoTerm term) {
        if (tryGetConstructor(term) == setup_3) {
          new TermVisitor() {
            public final void preVisit(IStrategoTerm term) {
              if (tryGetConstructor(term) == QUOTEPART_1) {
                term = term.getSubterm(0);
                results.add(new OffsetRegion(term));
              }
            }
          }.visit(term);
        }
      }
    }.visit(ast);
    return results;
  }
  
  
  public boolean isLastSyntaxCorrect() {
    return isLastSyntaxCorrect;
  }
  
}