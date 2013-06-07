package org.sugarj.test;

import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.terms.attachments.AbstractTermAttachment;
import org.spoofax.terms.attachments.TermAttachmentType;
import org.spoofax.terms.attachments.VolatileTermAttachmentType;

/**
 * Attachment to hold parse info as cached parses get modifed by the etst farmework
 * 
 * @author Bob Davison
 *
 */
public class SugarTestAttachment extends AbstractTermAttachment {
  
  private static final long serialVersionUID = -4691361658791256600L;
  
  private boolean syntaxCorrect;
  private ITokenizer tokenizer;
  
  public static TermAttachmentType<SugarTestAttachment> TYPE =
      new VolatileTermAttachmentType<SugarTestAttachment>(SugarTestAttachment.class);
  
  public TermAttachmentType<SugarTestAttachment> getAttachmentType() {
    return TYPE;
  }
  
  public void setSyntaxCorrect(boolean syntaxCorrect) {
    this.syntaxCorrect = syntaxCorrect;
  }
  
  public boolean isSyntaxCorrect() {
    return syntaxCorrect;
  }
  
  public void setTokenizer(ITokenizer tokenizer) {
    this.tokenizer = tokenizer;
  }
  
  public ITokenizer getTokenizer() {
    return tokenizer;
  }
}