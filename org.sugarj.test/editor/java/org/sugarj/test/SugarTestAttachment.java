package org.sugarj.test;


import org.spoofax.interpreter.terms.ISimpleTerm;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.attachments.AbstractTermAttachment;
import org.spoofax.terms.attachments.TermAttachmentType;
import org.spoofax.terms.attachments.VolatileTermAttachmentType;


/**
 * Attachment to hold desugared ast
 * 
 * @author Bob Davison
 *
 */
public class SugarTestAttachment extends AbstractTermAttachment {
  
  private static final long serialVersionUID = -4691361658791256600L;
  
  private IStrategoTerm desugaredSyntaxTree;
  
  public static TermAttachmentType<SugarTestAttachment> TYPE =
      new VolatileTermAttachmentType<SugarTestAttachment>(SugarTestAttachment.class);
  
  protected SugarTestAttachment(IStrategoTerm desugaredSyntaxTree) {
    this.desugaredSyntaxTree = desugaredSyntaxTree;
  }

  public TermAttachmentType<SugarTestAttachment> getAttachmentType() {
    return TYPE;
  }
  
  public IStrategoTerm getDesugaredSyntaxTree() {
    return desugaredSyntaxTree;
  }
  
  public static SugarTestAttachment get(ISimpleTerm term) {
    return term.getAttachment(TYPE);
  } 
  
  public static void put(ISimpleTerm term, IStrategoTerm desugaredSyntaxTree) {
    term.putAttachment(new SugarTestAttachment(desugaredSyntaxTree));
  }
  
}

