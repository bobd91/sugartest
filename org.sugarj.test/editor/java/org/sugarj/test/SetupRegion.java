package org.sugarj.test;

import static org.sugarj.test.AstConstructors.SOME_1;

import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.Term.termAt;

import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;

// Super class for all types of setup region
public abstract class SetupRegion {
  
  // Default implementations, class is still abstract
  public String getText(String input) { return ""; }
  public String getStartText(String input) { return ""; }
  public String getEndText(String input) { return ""; }
  public boolean matches(FragmentRegion region) { return false;};
  
  // Examine the provided ast to see what type of setup was specified and
  // return an appropriate SetupRegion
  public static SetupRegion setupRegionFor(IStrategoTerm ast) {
    if(ast.getSubtermCount() > 1) {
      IStrategoTerm term = ast.getSubterm(1);
      IStrategoConstructor cons = tryGetConstructor(term);
      if(cons == SOME_1) {
        term = termAt(term, 0); // SetupDecl
        term = termAt(term, 0); // Setup
        term = termAt(term, 1); // Setup part
        if(term.getSubtermCount() == 1) {
          return new SimpleSetupRegion(term);
        } else if (term.getSubtermCount() == 3) {
          return new ComplexSetupRegion(term);
        }
      }
    }
    return new NoSetupRegion();
  }
  
  // Setup region that actually exists
  static abstract class ConcreteSetupRegion extends SetupRegion {
    protected FragmentRegion region;
    
    ConcreteSetupRegion(IStrategoTerm setupTerm) {
      region = new FragmentRegion(setupTerm);
    }
    
    @Override
    public boolean matches(FragmentRegion region) { return this.region.equals(region); }
    
  }
  
  // Setup region with no test placeholder
  static class SimpleSetupRegion extends ConcreteSetupRegion {
    
    SimpleSetupRegion(IStrategoTerm setupTerm) {
      super(setupTerm);
    }
    
    @Override public String getText(String input) { return region.getText(input); }
    @Override public String getStartText(String input) { return getText(input); }
  }
  
  // Setup region with [[...]] placeholder for tests
  static class ComplexSetupRegion extends ConcreteSetupRegion {
    FragmentRegion start, placeHolder, end;
    
    ComplexSetupRegion(IStrategoTerm term) {
      super(term);
      start = new FragmentRegion(termAt(term,0));
      placeHolder = new FragmentRegion(termAt(term,1));
      end = new FragmentRegion(termAt(term,2));
    }
    
    @Override 
    public String getText(String input) { 
      return getStartText(input) + getBlankPlaceholder() + getEndText(input);
    }
  
    @Override
    public String getStartText(String input) { 
      return start.getText(input);
    }
    
    @Override
    public String getEndText(String input) {
      return end.getText(input);
    }
    
    // Blank out the test placeholder
    private String getBlankPlaceholder() {
      return new String(new char[placeHolder.getLength()]).replace('\0', ' ');
    }
    
  }
  
  // Defaults will do if there is no setup region
  static class NoSetupRegion extends SetupRegion { }
}