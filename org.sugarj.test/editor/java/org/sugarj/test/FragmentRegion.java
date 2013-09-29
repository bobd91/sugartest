package org.sugarj.test;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;

/**
 * An (inclusive) offset tuple. (Modified from org.spoofax.imp.testing.OffsetRegion)
 * 
 * Added start line and column as required to restore original token positions
 *
 * @author Lennart Kats <lennart add lclnet.nl>
 * @author Bob Davison
 */
public class FragmentRegion {
  private int startOffset, endOffset;
  private int startLine, startColumn;

  public FragmentRegion(IStrategoTerm fragment) {
    IToken left = getLeftToken(fragment);
    IToken right = getRightToken(fragment);
    startOffset = left.getStartOffset();
    startLine = left.getLine();
    startColumn = left.getColumn();
    endOffset = right.getEndOffset();
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
  
  public String getText(String input) {
    return input.substring(startOffset, endOffset + 1);
  }
  
  public int getLength() {
    return 1 + endOffset - startOffset;
  }
  
  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof FragmentRegion
        && startOffset == ((FragmentRegion)o).startOffset
        && endOffset == ((FragmentRegion)o).endOffset;
  }
  
  @Override
  public int hashCode() {
    return startOffset;
  }
}
