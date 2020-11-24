package jadx.gui.ui.codearea;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.text.Highlighter;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.SmartHighlightPainter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.utils.JumpPosition;

class MouseHoverHighlighter extends MouseMotionAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(MouseHoverHighlighter.class);

	private final CodeArea codeArea;
	private final CodeLinkGenerator codeLinkGenerator;
	private final Highlighter.HighlightPainter highlighter;

	private Object tag;
	private int highlightedTokenOffset = -1;

	public MouseHoverHighlighter(CodeArea codeArea, CodeLinkGenerator codeLinkGenerator) {
		this.codeArea = codeArea;
		this.codeLinkGenerator = codeLinkGenerator;
		this.highlighter = new SmartHighlightPainter(codeArea.getMarkOccurrencesColor());
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (!addHighlight(e)) {
			removeHighlight();
		}
	}

	private boolean addHighlight(MouseEvent e) {
		if (e.getModifiersEx() != 0) {
			return false;
		}
		try {
			Token token = codeArea.viewToToken(e.getPoint());
			if (token == null || token.getType() != TokenTypes.IDENTIFIER) {
				return false;
			}
			int tokenOffset = token.getOffset();
			if (tokenOffset == highlightedTokenOffset) {
				// don't repaint highlight
				return true;
			}
			JumpPosition jump = codeLinkGenerator.getJumpLinkAtOffset(codeArea, tokenOffset);
			if (jump == null) {
				return false;
			}
			removeHighlight();
			tag = codeArea.getHighlighter().addHighlight(tokenOffset, token.getEndOffset(), this.highlighter);
			highlightedTokenOffset = tokenOffset;
			return true;
		} catch (Exception exc) {
			LOG.error("Mouse hover highlight error", exc);
			return false;
		}
	}

	private void removeHighlight() {
		if (tag != null) {
			codeArea.getHighlighter().removeHighlight(tag);
			tag = null;
			highlightedTokenOffset = -1;
		}
	}
}
