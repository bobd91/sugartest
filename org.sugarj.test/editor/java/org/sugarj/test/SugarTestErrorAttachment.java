package org.sugarj.test;

import org.spoofax.terms.attachments.AbstractTermAttachment;
import org.spoofax.terms.attachments.TermAttachmentType;
import org.spoofax.terms.attachments.VolatileTermAttachmentType;

/**
 * Marker attachment for cached terms that had syntax errors
 * 
 * @author Bob Davison
 *
 */
public class SugarTestErrorAttachment extends AbstractTermAttachment {
  
  private static final long serialVersionUID = -4691361658791256600L;
  
  public static TermAttachmentType<SugarTestErrorAttachment> TYPE =
      new VolatileTermAttachmentType<SugarTestErrorAttachment>(SugarTestErrorAttachment.class);
  
  public TermAttachmentType<SugarTestErrorAttachment> getAttachmentType() {
    return TYPE;
  }
}