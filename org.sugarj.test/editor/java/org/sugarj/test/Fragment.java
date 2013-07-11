package org.sugarj.test;

import static org.sugarj.test.AstConstructors.FAILS_PARSING_0;
import static org.sugarj.test.AstConstructors.SETUP_3;
import static org.sugarj.test.AstConstructors.SUGAR_SETUP_3;
import static org.sugarj.test.AstConstructors.OUTPUT_4;
import static org.sugarj.test.AstConstructors.DESUGAR_4;
import static org.spoofax.interpreter.core.Tools.listAt;
import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;

import java.util.List;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.terms.StrategoListIterator;

/**
 * Keep track of the fragment text, and offsets
 * so that only required text is parsed (helps caching)
 * and original token positioning can be regained
 * 
 * @author Bob Davison
 */
public class Fragment {
  private String input;
  private String output;

  private IStrategoTerm term;
  private FragmentRegion region;
  private int outputStart;
  private int outputEnd;
  
  private boolean successExpected;
  
  public Fragment(String input, IStrategoTerm term, int testNumber, List<FragmentRegion> setupRegions) {
    this.input = input;
    this.term = term;
    region = new FragmentRegion(term);    
    successExpected = isSuccessExpected();
    createText(testNumber, setupRegions);
  }
  
  /**
   * Returns the fragment text to be parsed
   * 
   * @return
   */
  public String getText() {
    return output;
  }

  /**
   * Realign tokens from the parsed position to the position expected in the test file
   * Creates a new tokenizer and tokens which are attached to the ast via a new ImploderAttachment 
   * 
   * @param parsed            the result of the parse, maybe from SugarJ cache
   * @param parsedTokenizer   the tokenizer of the parse,
   *                          may not be the current tokenizer as that may have been compromised by the test system
   * @return                  the result of the parse with new ImploderAttachment
   */
  public IStrategoTerm realign(IStrategoTerm parsed, ITokenizer parsedTokenizer) { 
    IToken startToken = parsedTokenizer.getTokenAtOffset(outputStart);
    IToken endToken = parsedTokenizer.getTokenAtOffset(outputStart < outputEnd ? outputEnd - 1 : outputStart);
    int startIndex = startToken.getIndex();
    int endIndex = endToken.getIndex();
    int offsetAdjust = region.getStartOffset() - startToken.getStartOffset();
    
    Tokenizer tokenizer = new Tokenizer(input, parsedTokenizer.getFilename(), null);
    tokenizer.setPositions(region.getStartLine(), region.getStartOffset(), 
          region.getStartOffset() - region.getStartColumn());
    
    for(int index = startIndex ; index <= endIndex ; index++) {
      IToken token = parsedTokenizer.getTokenAt(index);
      String error = successExpected ? token.getError() : null;
      tokenizer.makeToken(offsetAdjust + token.getEndOffset(), token.getKind(), true, error);
    }
    
    IToken firstToken = tokenizer.getTokenAt(0);
    IToken lastToken = tokenizer.getTokenAt(tokenizer.getTokenCount() - 1);
    String sort = ImploderAttachment.get(parsed).getSort();
    ImploderAttachment.putImploderAttachment(parsed, parsed.isList(), sort, firstToken, lastToken);
    
    return parsed;
  }
  
 private void createText(int testNumber, List<FragmentRegion> setupRegions) {
    StringBuilder result = new StringBuilder(input.length());
    boolean addedFragment = false;

    for (FragmentRegion setup : setupRegions) {
      if (!addedFragment && setup.getStartOffset() >= region.getStartOffset()) {
        appendFragment(region, input, testNumber, result);
        addedFragment = true;
      }
      if(setup.getStartOffset() != region.getStartOffset()) {
        appendSetup(setup, input, testNumber, result);
      }
    }
    
    if (!addedFragment) {
      appendFragment(region, input, testNumber, result);
    }
 
    output = result.toString(); 
  }
  
   private void appendFragment(FragmentRegion reg, String input, int testNumber, StringBuilder result) {
    outputStart = result.length();
    result.append(reg.getText(input,  testNumber));
    outputEnd = result.length();
  }
  
   private void appendSetup(FragmentRegion reg, String input, int testNumber, StringBuilder result) {
     result.append(reg.getText(input,  testNumber));
   }

  private boolean isSuccessExpected() {
    if (tryGetConstructor(term) == OUTPUT_4 || tryGetConstructor(term) == DESUGAR_4)
      return true;
    IStrategoAppl test = (IStrategoAppl) getParent(term);
    if (test.getConstructor() == SETUP_3 || test.getConstructor() == SUGAR_SETUP_3)
      return true;
    IStrategoList expectations = listAt(test, test.getSubtermCount() - 1);
    for (IStrategoTerm expectation : StrategoListIterator.iterable(expectations)) {
      IStrategoConstructor cons = tryGetConstructor(expectation);
      if (/*cons == FAILS_0 ||*/ cons == FAILS_PARSING_0)
        return false;
    }
    return true;
  }
}