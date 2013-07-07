package org.sugarj.test;

import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermList;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.termAt;
import static org.spoofax.terms.Term.tryGetConstructor;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.imp.language.ILanguageService;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.spoofax.terms.StrategoListIterator;
import org.spoofax.terms.TermTransformer;
import org.spoofax.terms.TermVisitor;
import org.spoofax.terms.attachments.ParentAttachment;
import org.spoofax.terms.attachments.ParentTermFactory;
import org.strategoxt.imp.runtime.Debug;
import org.strategoxt.imp.runtime.EditorState;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.dynamicloading.IDynamicLanguageService;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.strategoxt.imp.runtime.parser.SGLRParseController;
import org.sugarj.AbstractBaseLanguage;
import org.sugarj.BaseLanguageRegistry;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

/**
 * Slightly modified copy of org.strategoxt.imp.testing.SpoofaxTestingJSGLRI
 * for testing SugarJ languages
 * 
 * @author Bob Davison
 *
 */
public class SugarTestJSGLRI extends JSGLRI {
  private static final int PARSE_TIMEOUT = 20 * 1000;
  
  private static final IStrategoConstructor INPUT_4 =
    Environment.getTermFactory().makeConstructor("Input", 4);
  
  private static final IStrategoConstructor OUTPUT_4 =
    Environment.getTermFactory().makeConstructor("Output", 4);
  
  private static final IStrategoConstructor ERROR_1 =
    Environment.getTermFactory().makeConstructor("Error", 1);
  
  private static final IStrategoConstructor LANGUAGE_1 =
    Environment.getTermFactory().makeConstructor("Language", 1);

  private static final IStrategoConstructor TARGET_LANGUAGE_1 =
    Environment.getTermFactory().makeConstructor("TargetLanguage", 1);

  private static final IStrategoConstructor SETUP_3 =
    Environment.getTermFactory().makeConstructor("Setup", 3);

  private static final IStrategoConstructor TARGET_SETUP_3 =
    Environment.getTermFactory().makeConstructor("TargetSetup", 3);

  private static final IStrategoConstructor TOPSORT_1 =
    Environment.getTermFactory().makeConstructor("TopSort", 1);

  private static final IStrategoConstructor TARGET_TOPSORT_1 =
    Environment.getTermFactory().makeConstructor("TargetTopSort", 1);
  
  private final FragmentParser fragmentParser = new FragmentParser(SETUP_3, TOPSORT_1);
  
  private final FragmentParser outputFragmentParser = new FragmentParser(TARGET_SETUP_3, TARGET_TOPSORT_1);

  private final SelectionFetcher selections = new SelectionFetcher();
  
  private final static String SUGARJ = "SugarJ";

  public SugarTestJSGLRI(JSGLRI template) {
    super(template.getParseTable(), template.getStartSymbol(), template.getController());
    setTimeout(PARSE_TIMEOUT);
    setUseRecovery(true);
  }
  
  @Override
  protected IStrategoTerm doParse(String input, String filename)
      throws TokenExpectedException, BadTokenException, SGLRException,
      IOException {
    IStrategoTerm ast = super.doParse(input, filename);
    return parseTestedFragments(ast, filename);
  }

  private IStrategoTerm parseTestedFragments(final IStrategoTerm root, final String filename) {
    final Tokenizer oldTokenizer = (Tokenizer) getTokenizer(root);
    final Retokenizer retokenizer = new Retokenizer(oldTokenizer);
    final ITermFactory nonParentFactory = Environment.getTermFactory();
    final ITermFactory factory = new ParentTermFactory(nonParentFactory);
    final AbstractBaseLanguage language = getLanguage(root);
    final AbstractBaseLanguage targetLanguage = hasTargetLanguage(root)
              ? getTargetLanguage(root)
              : language;
    final FragmentParser testedParser = configureFragmentParser(root, getSugarJLanguage(), fragmentParser);
    final FragmentParser outputParser = hasTargetLanguage(root)
        ? testedParser : configureFragmentParser(root, getSugarJLanguage(), outputFragmentParser);
    assert !(nonParentFactory instanceof ParentTermFactory);

    if (language == null || targetLanguage == null
        || testedParser == null || !testedParser.isInitialized()
        || outputParser == null || !outputParser.isInitialized()) {
      return root;
    }
    
    IStrategoTerm result = new TermTransformer(factory, true) {
      
      private int testNumber;
      
      @Override
      public IStrategoTerm preTransform(IStrategoTerm term) {
        IStrategoConstructor cons = tryGetConstructor(term);
        FragmentParser parser = null;
        String testFilename = null;
        
        if (cons == INPUT_4) {
          parser = testedParser;
          testFilename = makeTestFilename(filename, language, ++testNumber);
        }
        else if (cons == OUTPUT_4) {
          parser = outputParser;
          testFilename = makeTestFilename(filename, targetLanguage, ++testNumber);
        }
        
        if (parser != null && testFilename != null) {
          IStrategoTerm fragmentHead = termAt(term, 1);
          IStrategoTerm fragmentTail = termAt(term, 2);
          retokenizer.copyTokensUpToIndex(getLeftToken(fragmentHead).getIndex() - 1);
          try {
            IStrategoTerm parsed = parser.parse(oldTokenizer, term, testFilename, testNumber);
            int oldFragmentEndIndex = getRightToken(fragmentTail).getIndex();
            retokenizer.copyTokensFromFragment(fragmentHead, fragmentTail, parsed,
                getLeftToken(fragmentHead).getStartOffset(), getRightToken(fragmentTail).getEndOffset());
            if (!parser.isLastSyntaxCorrect())
              parsed = nonParentFactory.makeAppl(ERROR_1, parsed);
            ImploderAttachment implodement = ImploderAttachment.get(term);
            IStrategoList selected = selections.fetch(parsed);
            term = factory.annotateTerm(term, nonParentFactory.makeListCons(parsed, selected));
            term.putAttachment(implodement.clone());
            retokenizer.skipTokensUpToIndex(oldFragmentEndIndex);
          } catch (IOException e) {
            Debug.log("Could not parse tested code fragment", e);
          } catch (SGLRException e) {
            // TODO: attach ErrorMessage(_) term with error?
            Debug.log("Could not parse tested code fragment", e);
          } catch (CloneNotSupportedException e) {
            Environment.logException("Could not parse tested code fragment", e);
          } catch (RuntimeException e) {
            Environment.logException("Could not parse tested code fragment", e);
          }
        }
        return term;
      }
      
      @Override
      public IStrategoTerm postTransform(IStrategoTerm term) {
        Iterator<IStrategoTerm> iterator = TermVisitor.tryGetListIterator(term); 
        for (int i = 0, max = term.getSubtermCount(); i < max; i++) {
          IStrategoTerm kid = iterator == null ? term.getSubterm(i) : iterator.next();
          ParentAttachment.putParent(kid, term, null);
        }
        return term;
      }
    }.transform(root);
    retokenizer.copyTokensAfterFragments();
    retokenizer.getTokenizer().setAst(result);
    retokenizer.getTokenizer().initAstNodeBinding();
    return result;
  }

  private FragmentParser configureFragmentParser(IStrategoTerm root, Language language, FragmentParser fragmentParser) {
    if (language == null) return null;
    Descriptor descriptor = Environment.getDescriptor(language);
    if (descriptor == null) return null;
    fragmentParser.configure(descriptor, getController().getRelativePath(), getController().getProject(), root);
    attachToLanguage(language);
    return fragmentParser;
  }

  private String getLanguageName(IStrategoTerm root, IStrategoConstructor which) {
    if (root.getSubtermCount() < 1 || !isTermList(termAt(root, 0)))
      return null;
    IStrategoList headers = termAt(root, 0);
    for (IStrategoTerm header : StrategoListIterator.iterable(headers)) {
      if (tryGetConstructor(header) == which) {
        IStrategoString name = termAt(header, 0);
        return asJavaString(name);
      }
    }
    return null;
  }

  /**
   * Add our language service to the descriptor of a fragment language,
   * so our service gets reinitialized once the fragment language changes.
   */
  private void attachToLanguage(Language theirLanguage) {
    SGLRParseController myController = getController();
    EditorState myEditor = myController.getEditor();
    if (myEditor == null)
      return;
    ILanguageService myWrapper = myEditor.getEditor().getParseController();
    if (myWrapper instanceof IDynamicLanguageService) {
      Descriptor theirDescriptor = Environment.getDescriptor(theirLanguage);
      theirDescriptor.addActiveService((IDynamicLanguageService) myWrapper);
    } else {
      Environment.logException("SpoofaxTestingParseController wrapper is not IDynamicLanguageService");
    }
  }
  
  private AbstractBaseLanguage getLanguage(IStrategoTerm root) {
    final String languageName = getLanguageName(root, LANGUAGE_1);
    if (languageName == null) return null;
    return BaseLanguageRegistry.getInstance().getBaseLanguageByName(languageName);
  }
  
  private AbstractBaseLanguage getTargetLanguage(IStrategoTerm root) {
    String languageName = getLanguageName(root, TARGET_LANGUAGE_1);
    if (languageName == null) return null;
    return BaseLanguageRegistry.getInstance().getBaseLanguageByName(languageName);
  }
  
  private boolean hasTargetLanguage(IStrategoTerm root) {
    return null != getLanguageName(root, TARGET_LANGUAGE_1);
  }
  
  private Language getSugarJLanguage() {
    Language sugarj = LanguageRegistry.findLanguage(SUGARJ);
    if(sugarj == null) 
      throw new RuntimeException("SugarJ is not loaded");
    return sugarj;
  }
  
  private String makeTestFilename(String filename, AbstractBaseLanguage language, int count) {
    return FileCommands.dropExtension(filename) + count + "." + language.getSugarFileExtension();
  }
   
}