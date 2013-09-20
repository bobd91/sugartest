package org.sugarj.test;

import static org.sugarj.test.AstConstructors.SETUP_3;
import static org.sugarj.test.AstConstructors.SUGAR_SETUP_3;
import static org.sugarj.test.AstConstructors.INPUT_4;
import static org.sugarj.test.AstConstructors.OUTPUT_4;
import static org.sugarj.test.AstConstructors.DESUGAR_4;
import static org.sugarj.test.AstConstructors.LANGUAGE_1;
import static org.sugarj.test.AstConstructors.ERROR_1;
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

/**
 * Slightly modified copy of org.strategoxt.imp.testing.SpoofaxTestingJSGLRI
 * for testing SugarJ languages
 * 
 * Parses text using SugarTest grammar then parses test fragments 
 * against the specified SugarJ language
 * 
 * @author Bob Davison
 *
 */
public class SugarTestJSGLRI extends JSGLRI {
  private static final int PARSE_TIMEOUT = 20 * 1000;

  private final FragmentParser sugarFragmentParser = new FragmentParser(SETUP_3, SUGAR_SETUP_3);
  
  private final FragmentParser desugarFragmentParser = new FragmentParser(SETUP_3);

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
      IOException, InterruptedException {
    IStrategoTerm ast = super.doParse(input, filename);
    return parseTestedFragments(ast, filename);
  }

  private IStrategoTerm parseTestedFragments(final IStrategoTerm root, final String filename) {
    final Tokenizer oldTokenizer = (Tokenizer) getTokenizer(root);
    final Retokenizer retokenizer = new Retokenizer(oldTokenizer);
    final ITermFactory nonParentFactory = Environment.getTermFactory();
    final ITermFactory factory = new ParentTermFactory(nonParentFactory);
    final AbstractBaseLanguage language = getLanguage(root);
    final FragmentParser sugarParser = configureFragmentParser(root, getSugarJLanguage(), sugarFragmentParser);
    final FragmentParser desugarParser = configureFragmentParser(root, getSugarJLanguage(), desugarFragmentParser);
    assert !(nonParentFactory instanceof ParentTermFactory);

    if (language == null 
        || sugarParser == null || !sugarParser.isInitialized()
        || desugarParser == null || !desugarParser.isInitialized()) {
      return root;
    }
    
    IStrategoTerm result = new TermTransformer(factory, true) {
      
      @Override
      public IStrategoTerm preTransform(IStrategoTerm term) {
        IStrategoConstructor cons = tryGetConstructor(term);
        FragmentParser parser = null;
        
        if (cons == INPUT_4 || cons == OUTPUT_4) {
          parser = sugarParser;
        }
        else if (cons == DESUGAR_4) {
          parser = desugarParser;
        }
        
        if (parser != null) {
          IStrategoTerm fragmentHead = termAt(term, 1);
          IStrategoTerm fragmentTail = termAt(term, 2);
          retokenizer.copyTokensUpToIndex(getLeftToken(fragmentHead).getIndex() - 1);
          try {
            String testFilename = makeTestFilename(filename, language);
            IStrategoTerm parsed = parser.parse(oldTokenizer, term, testFilename);
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
          } catch (Exception e) {
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
  
 
  private Language getSugarJLanguage() {
    Language sugarj = LanguageRegistry.findLanguage(SUGARJ);
    if(sugarj == null) 
      throw new RuntimeException("SugarJ is not loaded");
    return sugarj;
  }
  
  private String makeTestFilename(String filename, AbstractBaseLanguage language) {
    return FileCommands.dropExtension(filename) + "." + language.getSugarFileExtension();
  }
   
}