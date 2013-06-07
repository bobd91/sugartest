package org.sugarj.test;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.Tokenizer;

/**
 * Keep track of fragment parse info across parses.
 * 
 * SugarJ caches parse results some of which are then compromised by the test framework.
 * This class attaches a SugarTestAttachment to the parse results so that the original, uncompromised,
 * results can be retrieved 
 * 
 * @author Bob Davison
 *
 */
public class FragmentParseInfo {
  private boolean syntaxCorrect;
  private boolean cached;
  private ITokenizer tokenizer;
  
  FragmentParseInfo(IStrategoTerm parsed) {
    SugarTestAttachment attachment = parsed.getAttachment(SugarTestAttachment.TYPE);
    cached = attachment != null;
    if(!cached) {
      attachment = new SugarTestAttachment();
      attachment.setSyntaxCorrect(!Tokenizer.isErrorInRange(getLeftToken(parsed), getRightToken(parsed)));
      attachment.setTokenizer(getTokenizer(parsed));
      parsed.putAttachment(attachment);
    }
    syntaxCorrect = attachment.isSyntaxCorrect();
    tokenizer = attachment.getTokenizer();
  }
  
  public boolean wasSyntaxCorrect() {
    return syntaxCorrect;
  }
  
  public boolean wasCached() {
    return cached;
  }
  
  public ITokenizer originalTokenizer() {
    return tokenizer;
  }
}

