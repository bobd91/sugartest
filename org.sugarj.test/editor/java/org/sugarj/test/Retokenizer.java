package org.sugarj.test;

import static org.sugarj.test.AstConstructors.QUOTEPART_1;
import static org.spoofax.jsglr.client.imploder.AbstractTokenizer.findRightMostLayoutToken;
import static org.spoofax.jsglr.client.imploder.AbstractTokenizer.getTokenAfter;
import static org.spoofax.jsglr.client.imploder.IToken.TK_ESCAPE_OPERATOR;
import static org.spoofax.jsglr.client.imploder.IToken.TK_LAYOUT;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getSort;
import static org.spoofax.terms.Term.isTermString;
import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;

import java.util.ArrayList;
import java.util.List;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.terms.TermVisitor;

/** 
 * Slightly modified copy of org.strategoxt.imp.testing.Retokenizer
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 * @author Bob Davison
 */
public class Retokenizer {
	
	private final Tokenizer oldTokenizer;
	
	private final Tokenizer newTokenizer;
	
	int oldTokenizerCopiedIndex;
	
	public Retokenizer(Tokenizer oldTokenizer) {
		this.oldTokenizer = oldTokenizer;
		newTokenizer = new Tokenizer(oldTokenizer.getInput(), oldTokenizer.getFilename(), null);
		newTokenizer.setSyntaxCorrect(oldTokenizer.isSyntaxCorrect());
	}
	
	public void copyTokensUpToIndex(int index) {
		reassignTokenRange(oldTokenizer, oldTokenizerCopiedIndex, index);
		oldTokenizerCopiedIndex = index + 1;
	}
	
	public void skipTokensUpToIndex(int index) {
		oldTokenizerCopiedIndex = index + 1;
	}
	
	public void copyTokensAfterFragments() {
		copyTokensUpToIndex(oldTokenizer.getTokenCount() - 1);
	}
	
	public void copyTokensFromFragment(IStrategoTerm fragment, IStrategoTerm parsedFragment) {
	  int startOffset = getLeftToken(fragment).getStartOffset();
	  int endOffset = getRightToken(fragment).getEndOffset();
		Tokenizer fragmentTokenizer = (Tokenizer) ImploderAttachment.getTokenizer(parsedFragment);
		IToken startToken, endToken;
		if (fragmentTokenizer.getStartOffset() <= startOffset) {
			endToken = fragmentTokenizer.currentToken();
			startToken = Tokenizer.getFirstTokenWithSameOffset(endToken);
		} else {
			startToken = Tokenizer.getFirstTokenWithSameOffset(
					fragmentTokenizer.getTokenAtOffset(startOffset));
			endToken = Tokenizer.getLastTokenWithSameEndOffset(
					fragmentTokenizer.getTokenAtOffset(endOffset));
		}
		((Token) endToken).setEndOffset(endOffset); // cut off if too long
		int startIndex = startToken.getIndex();
		int endIndex = endToken.getIndex();
		// Reassign new starting token to parsed fragment (skipping whitespace)
		if (startToken.getKind() == TK_LAYOUT && startIndex + 1 < fragmentTokenizer.getTokenCount()
				&& startIndex < endIndex)
			startToken = fragmentTokenizer.getTokenAt(++startIndex);
		moveTokenErrorsToRange(fragmentTokenizer, startIndex, endIndex);
		reassignTokenRange(fragmentTokenizer, startIndex, endIndex);
		ImploderAttachment old = ImploderAttachment.get(parsedFragment);
		ImploderAttachment.putImploderAttachment(parsedFragment, parsedFragment.isList(), old.getSort(), startToken, endToken);
		
		// Reassign new tokens to unparsed fragment
		recolorMarkingBrackets(fragment, fragmentTokenizer);
		assignTokens(fragment, startToken, endToken);
	}

	private void assignTokens(IStrategoTerm tree, final IToken startToken, final IToken endToken) {
		// HACK: asssign the same tokens to all tree nodes in fragments
		//       (breaks some editor services)
		new TermVisitor() {
			public void preVisit(IStrategoTerm term) {
				ImploderAttachment.putImploderAttachment(term, false, getSort(term), startToken, endToken);
			}
		}.visit(tree);
	}

	private void recolorMarkingBrackets(IStrategoTerm term, final ITokenizer tokenizer) {
		new TermVisitor() {
			public void preVisit(IStrategoTerm term) {
				if (isTermString(term) && tryGetConstructor(getParent(term)) != QUOTEPART_1) {
					IToken token1 = getLeftToken(term);
					IToken token2 = tokenizer.getTokenAtOffset(token1.getStartOffset());
					token2.setKind(TK_ESCAPE_OPERATOR);
				}
			}
		}.visit(term);
	}
	
	private void moveTokenErrorsToRange(Tokenizer tokenizer, int startIndex, int endIndex) {
		List<IToken> prefixErrors = collectErrorTokens(tokenizer, 0, startIndex);
		Token startToken = tokenizer.getTokenAt(startIndex);
		if (prefixErrors.size() != 1 || prefixErrors.get(0) != startToken)
			startToken.setError(combineAndClearErrors(prefixErrors));

		List<IToken> postfixErrors = collectErrorTokens(tokenizer, 0, startIndex);
		Token endToken = tokenizer.getTokenAt(endIndex);
		if (postfixErrors.size() != 1 || postfixErrors.get(0) != endToken)
			endToken.setError(combineAndClearErrors(postfixErrors));
	}
	
	private List<IToken> collectErrorTokens(Tokenizer tokenizer, int startIndex, int endIndex) {
		List<IToken> results = new ArrayList<IToken>();
		for (int i = 0; i < endIndex; i++) {
			Token token = tokenizer.internalGetTokenAt(i);
			if (token.getError() != null)
				results.add(token);
		}
		return results;
	}
	
	private String combineAndClearErrors(List<IToken> tokens) {
		String lastError = null;
		StringBuilder result = new StringBuilder();
		for (IToken token : tokens) {
			String error = token.getError();
			if (error != lastError) {
				if (error.startsWith(ITokenizer.ERROR_SKIPPED_REGION)) {
					token = getTokenAfter(findRightMostLayoutToken(token));
					error = "unexpected construct(s)";
				}
				result.append("line " + token.getLine() + ": " + error + ", \n");
				lastError = error;
			}
		}
		if (result.length() == 0)
			return null;
		result.delete(result.length() - 3, result.length());
		return result.toString();
	}

	private void reassignTokenRange(Tokenizer fromTokenizer, int startIndex, int endIndex) {
		for (int i = startIndex; i <= endIndex; i++) {
			Token token = fromTokenizer.getTokenAt(i);
			/*Token newToken = newTokenizer.makeToken(token.getEndOffset(), token.getKind(), true);
			newToken.setAstNode(token.getAstNode());
			newToken.setError(token.getError());*/
			// Since we case, we first clone before changing the token
			if (token.getTokenizer() != newTokenizer) // can happen w/ ambs
				newTokenizer.reassignToken(token);
		}
	}

	public ITokenizer getTokenizer() {
		return newTokenizer;
	}
	
	@Override
	public String toString() {
		return newTokenizer.toString();	  
	}
	
}
