package org.sugarj.test;

import static org.spoofax.interpreter.core.Tools.termAt;
import static org.spoofax.jsglr_layout.client.imploder.AbstractTokenizer.getTokenAfter;
import static org.spoofax.jsglr_layout.client.imploder.AbstractTokenizer.getTokenBefore;
import static org.spoofax.jsglr_layout.client.imploder.IToken.TK_ESCAPE_OPERATOR;
import static org.spoofax.jsglr_layout.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr_layout.client.imploder.ImploderAttachment.getRightToken;

import java.util.ArrayList;
import java.util.List;

import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr_layout.client.imploder.IToken;
import org.spoofax.jsglr_layout.client.imploder.ITokenizer;
import org.spoofax.terms.TermVisitor;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.stratego.StrategoTermPath;

/**
 * Copied from org.strategoxt.imp.testing.SelectionFetcher
 * 
 * Lamely-named class for fetching selections in test
 * fragment (e.g., foo in [[ module [[foo]] ]]). 
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class SelectionFetcher {
	
	public IStrategoList fetch(IStrategoTerm parsedFragment) {
		final List<IStrategoTerm> results = new ArrayList<IStrategoTerm>();
		/*
		if (getTokenizer(parsedFragment) == null && "Error".equals(Term.tryGetName(parsedFragment))) {
			parsedFragment = termAt(parsedFragment, 0);
		}
		AstNodeLocator locator = new AstNodeLocator();
		IToken left = getLeftToken(parsedFragment);
		IToken right = getRightToken(parsedFragment);
		IToken quoteOpenToken = null;
		if (left == null) // parse error
			return Environment.getTermFactory().makeList();
		for (IToken token = left; token != right; token = getTokenAfter(token)) {
			if (isOpenQuote(token)) {
				quoteOpenToken = token;
			} else if (isCloseQuote(token)) {
				IStrategoTerm result = locator.findNode(parsedFragment, quoteOpenToken.getEndOffset() + 1, token.getStartOffset() - 1);
				results.add(result == null ? parsedFragment : result);
			}
		}
		return Environment.getTermFactory().makeList(results);
		*/

		new TermVisitor() {
			IStrategoTerm unclosedChild;
			IToken unclosedLeft;
			IToken lastCloseQuote;
			
			public void preVisit(IStrategoTerm term) {
				IToken left = getTokenBefore(getLeftToken(term));
				IToken right = getTokenAfter(getRightToken(term));
				if (isOpenQuote(left) && isNoQuoteBetween(left, right)) {
					if (isCloseQuote(right) && isNoQuoteBetween(left, right)) {
						if (right != lastCloseQuote) {
							lastCloseQuote = right;
							results.add(getMatchingDescendant(term));
						}
					} else if (unclosedChild == null) {
						unclosedChild = term;
						unclosedLeft = left;
					}
				}
			}
			
			@Override
			public void postVisit(IStrategoTerm term) {
				IToken right = getTokenAfter(getRightToken(term));
				if (unclosedChild != null && isCloseQuote(right)
						&& isNoQuoteBetween(unclosedLeft, right)) {
					results.add(StrategoTermPath.findCommonAncestor(unclosedChild, term));
					unclosedChild = null;
				}
			}
		}.visit(parsedFragment);
		return Environment.getTermFactory().makeList(results);
	}
	
	private static IStrategoTerm getMatchingDescendant(IStrategoTerm term) {
		IToken left = getLeftToken(term);
		IToken right = getRightToken(term);
		for (int i = 0; i < term.getSubtermCount(); i++) {
			IStrategoTerm child = termAt(term, i);
			if (getLeftToken(child) == left && getRightToken(child) == right)
				return getMatchingDescendant(child);
		}
		return term;
	}

	protected boolean isOpenQuote(IToken left) {
		return left != null && left.getKind() == TK_ESCAPE_OPERATOR && isQuoteOpenText(left.toString());
	}

	protected boolean isCloseQuote(IToken right) {
		return right != null && right.getKind() == TK_ESCAPE_OPERATOR && !isQuoteOpenText(right.toString());
	}
	
	protected boolean isNoQuoteBetween(IToken left, IToken right) {
		ITokenizer tokenizer = left.getTokenizer();
		for (int i = left.getIndex() + 1, end = right.getIndex(); i < end; i++) {
			IToken token = tokenizer.getTokenAt(i);
			if (token.getKind() == TK_ESCAPE_OPERATOR) {
				return false;
			}
		}
		return true;
	}

	protected boolean isQuoteOpenText(String contents) {
		// HACK: inspect string contents to find out if it's an open or close quote
		if (contents.contains("[")) {
			return true;
		} else {
			assert contents.contains("]");
			return false;
		}
	}
}
