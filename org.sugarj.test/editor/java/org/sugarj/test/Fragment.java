package org.sugarj.test;

import static org.sugarj.test.AstConstructors.FAILS_PARSING_0;
import static org.sugarj.test.AstConstructors.SETUP_3;
import static org.sugarj.test.AstConstructors.OUTPUT_3;
import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Tokenizer;

/**
 * Keep track of the fragment text, and offsets
 * so that only required text is parsed (helps caching)
 * and original token positioning can be regained
 * 
 * @author Bob Davison
 */
public class Fragment {
  private String input;

  private IStrategoTerm term;
  private FragmentRegion region;
  private SetupRegion setupRegion;
  
  private int outputStart;
  private int outputEnd;
  
  private boolean successExpected;
  
  public Fragment(String input, IStrategoTerm term, SetupRegion setupRegion) {
    this.input = input;
    this.term = term;
    this.setupRegion = setupRegion;
    region = new FragmentRegion(term); 
    successExpected = isSuccessExpected();
  }
  
  /**
   * Returns the fragment text to be parsed
   * 
   * @return
   */
  public String getText() {
    return isSetup()
             ? getSetupText()
             : getTestText();
  }
  
  private boolean isSetup() {
    return setupRegion.matches(region);
  }
  
  private String getSetupText() {
    String result = setupRegion.getText(input);
    outputStart = 0;
    outputEnd = result.length();
    return result;
  }
  
  private String getTestText() {
    StringBuilder result = new StringBuilder(input.length());
    result.append(setupRegion.getStartText(input));
    outputStart = result.length();
    result.append(region.getText(input));
    outputEnd = result.length();
    result.append(setupRegion.getEndText(input));
    return result.toString();
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
  
  private boolean isSuccessExpected() {
    IStrategoAppl parent = (IStrategoAppl) getParent(term);
    IStrategoConstructor cons = tryGetConstructor(parent);
    if (cons == OUTPUT_3 || cons == SETUP_3)
      return true;
    IStrategoAppl test = (IStrategoAppl) getParent(parent);
    IStrategoTerm expectation = test.getSubterm(1);
    cons = tryGetConstructor(expectation);
    return cons != FAILS_PARSING_0;
  }
}