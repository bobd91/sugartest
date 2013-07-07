package org.sugarj.test;

import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermString;
import static org.spoofax.interpreter.core.Tools.listAt;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
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
import org.strategoxt.imp.runtime.Environment;

/**
 * Keep track of the fragment text, and offsets
 * so that only required text is parsed (helps caching)
 * and original token positioning can be regained
 * 
 * @author Bob Davison
 */
class Fragment {
  
  private static final IStrategoConstructor FAILS_PARSING_0 =
      Environment.getTermFactory().makeConstructor("FailsParsing", 0);
  
  private static final IStrategoConstructor SETUP_3 =
    Environment.getTermFactory().makeConstructor("Setup", 3);

  private static final IStrategoConstructor TARGET_SETUP_3 =
    Environment.getTermFactory().makeConstructor("TargetSetup", 3);

  private static final IStrategoConstructor OUTPUT_4 =
    Environment.getTermFactory().makeConstructor("Output", 4);
  
  private static final IStrategoConstructor QUOTEPART_1 =
    Environment.getTermFactory().makeConstructor("QuotePart", 1);
  
  private static final IStrategoConstructor MARKED_TEST_NUMBER_3 =
      Environment.getTermFactory().makeConstructor("MarkedTestNumber", 3);
  
  private static final int EXCLUSIVE = 1;
    
  private String input;
  private String output;

  private IStrategoTerm term;
  private FragmentRegion region;
  private int outputStart;
  private int outputEnd;
  
  private boolean successExpected;
  
  Fragment(String input, IStrategoTerm term, int testNumber, List<FragmentRegion> setupRegions) {
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
        appendFragment(region.getHead(), region.getTail(), input, testNumber, result);
        addedFragment = true;
      }
      if(setup.getStartOffset() != region.getStartOffset()) {
        appendSetup(setup.getHead(), setup.getTail(), input, testNumber, result);
      }
    }
    
    if (!addedFragment) {
      appendFragment(region.getHead(), region.getTail(), input, testNumber, result);
    }
 
    output = result.toString(); 
  }
  
   private void appendFragment(IStrategoTerm head, IStrategoTerm tail, String input, int testNumber, StringBuilder result) {
    outputStart = result.length();
    appendFragment(head, input, testNumber, result);
    appendFragment(tail, input, testNumber, result);
    outputEnd = result.length();
  }
  
   private void appendSetup(IStrategoTerm head, IStrategoTerm tail, String input, int testNumber, StringBuilder result) {
     appendFragment(head, input, testNumber, result);
     appendFragment(tail, input, testNumber, result);
   }

   private void appendFragment(IStrategoTerm term, String input, int testNumber, StringBuilder output) {
    IToken left = getLeftToken(term);
    IToken right = getRightToken(term);
    if (tryGetConstructor(term) == QUOTEPART_1) {
      output.append(input, left.getStartOffset(), right.getEndOffset() + EXCLUSIVE);
    } else if(tryGetConstructor(term) == MARKED_TEST_NUMBER_3) {
      output.append(formatTestNumber(testNumber, EXCLUSIVE + right.getEndOffset() - left.getStartOffset()));
    } else if (isTermString(term)) {
      // Brackets: treat as whitespace
      assert asJavaString(term).length() <= 4 : "Bracket expected: " + term;
      addWhitespace(input, left.getStartOffset(), right.getEndOffset(), output);
    } else {
      // Other: recurse
      for (int i = 0; i < term.getSubtermCount(); i++) {
        appendFragment(term.getSubterm(i), input, testNumber, output);
      }
    }
  }
  
  private String formatTestNumber(int testNumber, int width) {
    return (testNumber + new String(new char[width]).replace('\0', ' ')).substring(0,width);
  }

  private void addWhitespace(String input, int startOffset, int endOffset, StringBuilder output) {
    for (int i = startOffset; i <= endOffset; i++)
      output.append(input.charAt(i) == '\n' ? '\n' : ' ');
  }

  private boolean isSuccessExpected() {
    if (tryGetConstructor(term) == OUTPUT_4)
      return true;
    IStrategoAppl test = (IStrategoAppl) getParent(term);
    if (test.getConstructor() == SETUP_3 || test.getConstructor() == TARGET_SETUP_3)
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