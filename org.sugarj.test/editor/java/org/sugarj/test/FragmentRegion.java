package org.sugarj.test;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;

/**
 * An (inclusive) offset tuple. (Modified from org.spoofax.imp.testing.OffsetRegion)
 * 
 * Added start line and column as required to restore original token positions
 * Also store fragment head and tail
 *
 * @author Lennart Kats <lennart add lclnet.nl>
 * @author Bob Davison
 */
class FragmentRegion {
  private int startOffset, endOffset;
  private int startLine, startColumn;
  private IStrategoTerm head, tail;

  FragmentRegion(IStrategoTerm term) {
    head = term.getSubterm(1);
    tail = term.getSubterm(2);
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
  
  IStrategoTerm getHead() {
    return head;
  }

  IStrategoTerm getTail() {
    return tail;
  }
}
