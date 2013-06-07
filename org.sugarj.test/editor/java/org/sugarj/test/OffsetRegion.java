package org.sugarj.test;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;

/**
 * An (inclusive) offset tuple. (Modified from org.spoofax.imp.testing)
 * 
 * Added start line and column as required to restore original token positions
 *
 * @author Lennart Kats <lennart add lclnet.nl>
 * @author Bob Davison
 */
class OffsetRegion {
  private int startOffset, endOffset;
  private int startLine, startColumn;

  OffsetRegion(IStrategoTerm term) {
    this(term, term);
  }
  
  OffsetRegion(IStrategoTerm head, IStrategoTerm tail) {
    IToken left = getLeftToken(head);
    IToken right = getRightToken(tail);
    startOffset = left.getStartOffset();
    startLine = left.getLine();
    startColumn = left.getColumn();
    endOffset = right.getEndOffset();
  }
  @Override
  public String toString() {
    return "[" + startLine + ":" + startColumn + "(" + startOffset + "-" + endOffset + ")]";
  }
  
  int getStartOffset() {
    return startOffset;
  }
  
  int getEndOffset() {
    return endOffset;
  }
  
  int getStartLine() {
    return startLine;
  }
  
  int getStartColumn() {
    return startColumn;
  }
}
