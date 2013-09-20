package org.sugarj.test;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.Tokenizer;

/**
 * Cache information on parses.
 * 
 * @author Bob Davison
 *
 */
public class FragmentParseInfo {
  private static ParseCache cache = new ParseCache();
  
  private boolean syntaxCorrect;
  private ITokenizer tokenizer;
  private IStrategoTerm parsed;
  
  private FragmentParseInfo(IStrategoTerm parsed) {
    this.parsed = parsed;
    this.syntaxCorrect = !Tokenizer.isErrorInRange(getLeftToken(parsed), getRightToken(parsed));
    this.tokenizer = getTokenizer(parsed);
  }
  
  public boolean isSyntaxCorrect() {
    return syntaxCorrect;
  }
  
  public IStrategoTerm getParsed() {
    return parsed;
  }
  
  public ITokenizer originalTokenizer() {
    return tokenizer;
  }
  
  public static FragmentParseInfo cacheGet(String filename, Fragment fragment) {
    return cache.getInfo(new ParseCacheKey(filename, fragment));
  }
  
  public static FragmentParseInfo cacheAdd(String filename, Fragment fragment, IStrategoTerm parsed) {
    FragmentParseInfo info = new FragmentParseInfo(parsed);
    cache.putInfo(new ParseCacheKey(filename, fragment), info);
    return info;
  }
  
  /*
   * The MRU cache
   */
  static class ParseCache extends LinkedHashMap<ParseCacheKey, SoftReference<FragmentParseInfo>> {

    private static final long serialVersionUID = -510754665005651197L;
    private static final int MAX_ENTRIES = 200;
    
    ParseCache() {
      // default, default, access ordered
      super(16, 0.75f, true);
    }
    
    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<ParseCacheKey, SoftReference<FragmentParseInfo>> eldest) {
        return size() > MAX_ENTRIES;
    }
    
    public synchronized FragmentParseInfo putInfo(ParseCacheKey key, FragmentParseInfo value) {
        SoftReference<FragmentParseInfo> previousValueReference = put(key, new SoftReference<FragmentParseInfo>(value));
        return previousValueReference != null ? previousValueReference.get() : null;
    }

    public synchronized FragmentParseInfo getInfo(ParseCacheKey key) {
        SoftReference<FragmentParseInfo> valueReference = get(key);
        FragmentParseInfo info = null;
        if(valueReference != null) {
          info = valueReference.get();
          if(info == null)
            remove(key);
        }
        return info;
    }

  }
  
  /*
   * The key for the cache
   */
  static class ParseCacheKey {
    private String filename;
    private String fragmentText;
    
    ParseCacheKey(String filename, Fragment fragment) {
      this.filename = filename;
      this.fragmentText = fragment.getText();
    }
    
    public boolean equals(Object o) {
      return
          o != null
          && o instanceof ParseCacheKey
          && this.filename.equals(((ParseCacheKey)o).filename)
          && this.fragmentText.equals(((ParseCacheKey)o).fragmentText);
    }
    
    public int hashCode() {
      return filename.hashCode() + fragmentText.hashCode();
    }
    
  }
}

