package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.codemanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;

/**
 * Utility class used to get better source ranges for <code>ISourceReference</code>.
 */
class SourceReferenceSourceRangeComputer {
	
	private ISourceReference fSourceReference;
	private ICompilationUnit fCu;
	
	private SourceReferenceSourceRangeComputer(ISourceReference element, ICompilationUnit cu){
		Assert.isTrue(cu.exists());
		fCu= cu;
		Assert.isTrue(((IJavaElement)element).exists());
		fSourceReference= element;
	}
	
	/**
	 * Returns the computed source of the elements.
	 * @see SourceReferenceSourceRangeComputer#computeSourceRange(ISourceReference, ICompilationUnit)
	 */
	static String computeSource(ISourceReference elem) throws JavaModelException{
		ICompilationUnit cu= SourceReferenceUtil.getCompilationUnit(elem);
		ISourceRange range= SourceReferenceSourceRangeComputer.computeSourceRange(elem, cu);
		int endIndex= range.getOffset() + range.getLength();
		return cu.getSource().substring(range.getOffset(), endIndex);
	}
	
	static ISourceRange computeSourceRange(ISourceReference element, ICompilationUnit cu) throws JavaModelException{
		try{
		 	SourceReferenceSourceRangeComputer inst= new SourceReferenceSourceRangeComputer(element, cu);
		 	return new SourceRange(inst.computeOffset(), inst.computeLength());
		}	catch(CoreException e){
			//fall back to the default
			return element.getSourceRange();
		}	
	}
	
	private int computeLength() throws JavaModelException{
		int length= fSourceReference.getSourceRange().getLength();
		//if (true) //XXX
		//	return length; 
		try{	
			Scanner scanner= new Scanner(true, true);
			scanner.recordLineSeparator = true;
			scanner.setSourceBuffer(fCu.getSource().toCharArray());
			int start= fSourceReference.getSourceRange().getOffset() + length;
			scanner.currentPosition= start;
			int token = scanner.getNextToken();
			while (token != TerminalSymbols.TokenNameEOF) {
				switch (token) {
					case Scanner.TokenNameWHITESPACE:
						break;
					case TerminalSymbols.TokenNameSEMICOLON:
						break;	
					case Scanner.TokenNameCOMMENT_LINE :
						break;
					default:
						return scanner.currentPosition - fSourceReference.getSourceRange().getOffset() - scanner.getCurrentTokenSource().length;
				}
				token = scanner.getNextToken();
			}
			return length;
		} catch (InvalidInputException e){
			return length;
		}
	}
	
	private int computeOffset() throws CoreException{
		
		int offset= fSourceReference.getSourceRange().getOffset();
//		if (true) //XXX
			return offset;
//		TextBuffer buffer= TextBuffer.create(fCu.getSource());	
//		try{	
//			Scanner scanner= new Scanner(false, false);
//			scanner.recordLineSeparator = true;
//			int lineNumber= buffer.getLineOfOffset(fSourceReference.getSourceRange().getOffset());
//			String line= buffer.getLineContent(lineNumber);
//			TextRegion lineInfo= buffer.getLineInformation(lineNumber);
//			scanner.setSourceBuffer(line.toCharArray());
//			scanner.currentPosition= 0;
//			int token = scanner.getNextToken();
//			int curr= scanner.currentPosition + lineInfo.getOffset() - scanner.getCurrentTokenSource().length;
//			if (curr == offset)
//				return lineInfo.getOffset();
//			else
//				return offset;
//		} catch (InvalidInputException e){
//			return offset;
//		}
	}
	
}

