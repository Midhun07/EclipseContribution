/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class QuickFixTest18 extends QuickFixTest {

	private static final Class THIS= QuickFixTest18.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public QuickFixTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java18ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.DO_NOT_INSERT);


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= Java18ProjectTestSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	public void testUnimplementedMethods() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    int getCount(Object[] o) throws IOException;\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 20;}\n");
		buf.append("}\n");
		pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public abstract class E implements Inter{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int getCount(Object[] o) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	public void testUnimplementedMethods2() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class MyString implements CharSequence{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("MyString.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class MyString implements CharSequence{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class MyString implements CharSequence{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public char charAt(int arg0) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public int length() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public CharSequence subSequence(int arg0, int arg1) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	public void testLambdaReturnType1() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    int fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("        };\n");
		buf.append("        return 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    int fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("        return 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testLambdaReturnType2() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testLambdaReturnType3() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		// Inferred parameter type
		buf.append("        I i= (x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("        };\n");
		buf.append("        i.foo(10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		buf.append("        I i= (x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("        i.foo(10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testLambdaReturnType4() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    I i2= (int x) -> {\n");
		buf.append("        x++;\n");
		buf.append("        System.out.println(x);\n");
		buf.append("    };\n");
		buf.append("    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    I i2= (int x) -> {\n");
		buf.append("        x++;\n");
		buf.append("        System.out.println(x);\n");
		buf.append("        return x;\n");
		buf.append("    };\n");
		buf.append("    \n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	// bug 424172
	public void testImportTypeInMethodReference() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test.one", false, null);
		buf= new StringBuffer();
		buf.append("package test.one;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    public static F2 staticMethod() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);
		buf= new StringBuffer();
		buf.append("package test.one;\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("public interface F2 {\n");
		buf.append("    void foo();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test.two", false, null);
		buf= new StringBuffer();
		buf.append("package test.two;\n");
		buf.append("\n");
		buf.append("import test.one.F2;\n");
		buf.append("\n");
		buf.append("public class C1 {\n");
		buf.append("    public void fun1() {\n");
		buf.append("        F2 f = X::staticMethod;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack2.createCompilationUnit("C1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test.two;\n");
		buf.append("\n");
		buf.append("import test.one.F2;\n");
		buf.append("import test.one.X;\n");
		buf.append("\n");
		buf.append("public class C1 {\n");
		buf.append("    public void fun1() {\n");
		buf.append("        F2 f = X::staticMethod;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testLambdaReturnType5() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.math.BigInteger;\n");
		buf.append("\n");
		buf.append("interface A { Object m(Class c); }\n");
		buf.append("interface B<S extends Number> { Object m(Class<S> c); }\n");
		buf.append("interface C<T extends BigInteger> { Object m(Class<T> c); }\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface D<S,T> extends A, B<BigInteger>, C<BigInteger> {}\n");
		buf.append("\n");
		buf.append("class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("         D<BigInteger,BigInteger> d1= (x) -> {\n");
		buf.append("            };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.math.BigInteger;\n");
		buf.append("\n");
		buf.append("interface A { Object m(Class c); }\n");
		buf.append("interface B<S extends Number> { Object m(Class<S> c); }\n");
		buf.append("interface C<T extends BigInteger> { Object m(Class<T> c); }\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface D<S,T> extends A, B<BigInteger>, C<BigInteger> {}\n");
		buf.append("\n");
		buf.append("class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("         D<BigInteger,BigInteger> d1= (x) -> {\n");
		buf.append("            return x;\n");
		buf.append("            };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testChangeModifierToStatic1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList proposals= collectCorrections(cu, astRoot, 4, 0);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    static int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposals= collectCorrections(cu, astRoot, 4, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    static int bar1() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposals= collectCorrections(cu, astRoot, 4, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    static void bar2() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposals= collectCorrections(cu, astRoot, 4, 3);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    static int fun1() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}
}