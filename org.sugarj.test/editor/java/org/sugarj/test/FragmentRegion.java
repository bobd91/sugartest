package org.sugarj.test;

import static org.sugarj.test.AstConstructors.QUOTEPART_1;
import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermString;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.terms.Term.tryGetConstructor;

import java.util.LinkedList;
import java.util.List;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;

/**
 * An (inclusive) offset tuple. (Modified from org.spoofax.imp.testing.OffsetRegion)
 * 
 * Added start line and column as required to restore original token positions
 * Also store fragment head and tail
 * 
 * Collect info on subregions so that text can be recovered, needed as setup fragments may have different tokens
 * after the first parse and so using tokens to find setup text after the first parse does not work
 *
 * @author Lennart Kats <lennart add lclnet.nl>
 * @author Bob Davison
 */
public class FragmentRegion {
  private int startOffset, endOffset;
  private int startLine, startColumn;
  private IStrategoTerm head, tail;
  private List<SubRegion> subRegions = new LinkedList<SubRegion>();

  public FragmentRegion(IStrategoTerm term) {
    head = term.getSubterm(1);
    tail = term.getSubterm(2);
    IToken left = getLeftToken(head);
    IToken right = getRightToken(tail);
    startOffset = left.getStartOffset();
    startLine = left.getLine();
    startColumn = left.getColumn();
    endOffset = right.getEndOffset();
    
    addSubRegions(head);
    addSubRegions(tail);
  }
  
  @Override
  public String toString() {
    return "[" + startLine + ":" + startColumn + "(" + startOffset + "-" + endOffset + ")]";
  }
  
  public int getStartOffset() {
    return startOffset;
  }
  
  public int getEndOffset() {
    return endOffset;
  }
  
  public int getStartLine() {
    return startLine;
  }
  
  public int getStartColumn() {
    return startColumn;
  }
  
  public IStrategoTerm getHead() {
    return head;
  }

  public IStrategoTerm getTail() {
    return tail;
  }
  
  public String getText(String input) {
    StringBuilder output = new StringBuilder();
    for(SubRegion sub : subRegions) {
      output.append(sub.getText(input));
    }
    return output.toString();
  }
  
  private void addSubRegions(IStrategoTerm term) {
    IToken left = getLeftToken(term);
    IToken right = getRightToken(term);
    if (tryGetConstructor(term) == QUOTEPART_1) {
      subRegions.add(new TextSubRegion(left.getStartOffset(), right.getEndOffset()));
   } else if (isTermString(term)) {
      // Brackets: treat as whitespace
      assert asJavaString(term).length() <= 4 : "Bracket expected: " + term;
      subRegions.add(new WhitespaceSubRegion(left.getStartOffset(), right.getEndOffset()));
    } else {
      // Other: recurse
      for (int i = 0; i < term.getSubtermCount(); i++) {
        addSubRegions(term.getSubterm(i));
      }
    }
  }
  
  static abstract class SubRegion {
    int start;
    int end;
    SubRegion(int start, int end) {
      this.start = start;
      this.end = end;
    }
    abstract String getText(String input);
 }
  
  static class TextSubRegion extends SubRegion {
    TextSubRegion(int start, int end) {
      super(start, end);
    }
    String getText(String input) {
      return input.substring(start, end + 1);
    }
  }
  
  static class WhitespaceSubRegion extends SubRegion {
    WhitespaceSubRegion(int start, int end) {
      super(start, end);
    }
    String getText(String input) {
      int width = 1 + end - start;
      return spaces(width);
    }
    static String spaces(int n) {
      return new String(new char[n]).replace('\0', ' ');
    }
  }  
}
