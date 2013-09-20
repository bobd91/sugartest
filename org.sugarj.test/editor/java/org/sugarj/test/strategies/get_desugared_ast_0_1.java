package org.sugarj.test.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;
import org.sugarj.test.SugarTestAttachment;

/**
 * Gets the desugared syntax tree from the current ast
 * 
 * The desugared synatx tree is attached by SugarJTestParser
 * 
 * @author Bob Davison
 */
public class get_desugared_ast_0_1 extends Strategy {

  public static get_desugared_ast_0_1 instance = new get_desugared_ast_0_1();

  @Override
  public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm ast) {
    try {
      return SugarTestAttachment.get(ast).getDesugaredSyntaxTree();
    } catch(Exception e) {
      return ast;
    }
  }
}