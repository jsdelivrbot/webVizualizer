package net.networkdowntime.javaAnalyzer.javaModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EmptyMemberDeclaration;
import com.github.javaparser.ast.body.EmptyTypeDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.MultiTypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralMinValueExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralMinValueExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.TypeDeclarationStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

@SuppressWarnings("rawtypes")
public class AstVisitor extends VoidVisitorAdapter {
	private static final Logger LOGGER = LogManager.getLogger(AstVisitor.class.getName());

	// begin dump the AST
	int depth = 0;
	BufferedWriter astDumpWriter = null;
	// end dump the AST

	private static final boolean DEBUGGING_ENABLED = false;

	private Deque<DependentBase> heirarchyStack = new ArrayDeque<DependentBase>();

	private Project project = null;
	private Package currentPackage = null;
	private DependentBase current = null;
	private String fileName = null;

	private Set<String> imports = new LinkedHashSet<String>();

	public AstVisitor(int depth, String fileName, Project prj, CompilationUnit cu) {
		this.fileName = fileName;
		if (DEBUGGING_ENABLED) {
			try {
				File dir = new File("astDumps");
				if (!dir.exists())
					dir.mkdir();
				File astDump = new File("astDumps/" + new File(this.fileName).getName().replace(".java", ".txt"));
				astDumpWriter = new BufferedWriter(new FileWriter(astDump));
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		this.depth = depth;
		this.project = prj;

		this.visit(cu, null);

		if (DEBUGGING_ENABLED) {
			try {
				astDumpWriter.flush();
				astDumpWriter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

	public static void main(String[] args) {
		Project prj = new Project();

		long time = System.currentTimeMillis();

		prj.addFile(new File("src/test/java/testClasses/TestClass1.java"));

		LOGGER.info("Time to parse files (ms): " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();

		prj.validate();
		LOGGER.info("Time to validate (ms): " + (System.currentTimeMillis() - time));
	}

	public void logAST(int depth, String str) {
		if (DEBUGGING_ENABLED) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < depth; i++) {
				sb.append("\t");
			}
			sb.append(str);
			sb.append("\r");
			try {
				astDumpWriter.write(sb.toString());
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

	private void addImports() {
		for (String importStr : imports) {
			((Class) current).addImport(heirarchyStack.size() + 1, importStr);
		}
	}

	private void addVariable(Node variableDecloratorOrFieldDeclaration) {
		String type = null;
		List<String> variableNames = new ArrayList<String>();

		for (Node child : variableDecloratorOrFieldDeclaration.getChildrenNodes()) {
			if (child instanceof ReferenceType) {
				type = ((ReferenceType) child).toString();
			} else if (child instanceof PrimitiveType) {
				type = ((PrimitiveType) child).toString();
			} else if (child instanceof VariableDeclarator) {
				variableNames.add(((VariableDeclarator) child).getId().getName());
			}
		}

		for (String variableName : variableNames) {
			current.addVariable(heirarchyStack.size() + 1, variableName, type);
		}
	}

	//	private static String modifiersToString(int i) {
	//		String retval = "";
	//		retval += ModifierSet.isPublic(i) ? "public" : "";
	//		retval += ModifierSet.isProtected(i) ? "protected" : "";
	//		retval += ModifierSet.isPrivate(i) ? "private" : "";
	//
	//		retval += (retval.isEmpty()) ? "" : " ";
	//
	//		retval += ModifierSet.isStatic(i) ? "static" : "";
	//		retval += ModifierSet.isAbstract(i) ? "abstract" : "";
	//		retval += ModifierSet.isFinal(i) ? "final" : "";
	//
	//		return retval.trim();
	//	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(AnnotationDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.getNameExpr().getName());

		// String modifiers = modifiersToString(n.getModifiers());
		// TODO implement handling modifiers

		Class newClass;
		if (n.getParentNode() instanceof CompilationUnit) {
			if (currentPackage == null) {
				this.currentPackage = project.getOrCreateAndGetPackage(1, "default", true, true);
			}

			newClass = currentPackage.getOrCreateAndGetClass(heirarchyStack.size(), n.getName(), true, this.fileName);
		} else {
			String parentNodeName = null;
			if (n.getParentNode() instanceof ClassOrInterfaceDeclaration) {
				parentNodeName = ((ClassOrInterfaceDeclaration) n.getParentNode()).getName();
			} else if (n.getParentNode() instanceof AnnotationDeclaration) {
				parentNodeName = ((AnnotationDeclaration) n.getParentNode()).getName();
			}

			newClass = currentPackage.getOrCreateAndGetClass(1, parentNodeName + "." + n.getName(), true, this.fileName);
		}

		project.addSearchIndex(currentPackage.getName(), newClass.getCanonicalName(), n.toString());

		current = newClass;
		heirarchyStack.push(newClass);

		((Class) current).setIsAnnotation(true);
		addImports();

		depth++;
		super.visit(n, arg);
		depth--;

		logIndented(heirarchyStack.size(), "Done with " + ((Class) current).getCanonicalName());
		heirarchyStack.pop();
		if (!heirarchyStack.isEmpty())
			current = heirarchyStack.peek();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(AnnotationMemberDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ArrayAccessExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ArrayCreationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ArrayInitializerExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(AssertStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(AssignExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(BinaryExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(BlockComment n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(BlockStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(BooleanLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(BreakStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(CastExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(CatchClause n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(CharLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ClassExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ClassOrInterfaceDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "-" + n.getEndLine() + "): " + n.getNameExpr().getName());

		logIndented(0, n.getParentNode().getClass().getName());

		Class newClass;
		if (n.getParentNode() instanceof CompilationUnit) {
			if (currentPackage == null) {
				this.currentPackage = project.getOrCreateAndGetPackage(1, "default", true, true);
			}
			newClass = currentPackage.getOrCreateAndGetClass(heirarchyStack.size(), n.getName(), true, this.fileName);
		} else if (n.getParentNode() instanceof ClassOrInterfaceDeclaration) {
			ClassOrInterfaceDeclaration parent = (ClassOrInterfaceDeclaration) n.getParentNode();
			newClass = currentPackage.getOrCreateAndGetClass(heirarchyStack.size(), parent.getName() + "." + n.getName(), true, this.fileName);
		} else if (n.getParentNode() instanceof TypeDeclarationStmt) {
			logAST(0, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.getNameExpr().getName());
			logIndented(0, "Creating class: " + n.getName());

			//			MethodDeclaration parent = (MethodDeclaration) n.getParentNode().getParentNode().getParentNode();
			newClass = currentPackage.getOrCreateAndGetClass(heirarchyStack.size(), n.getName(), true, this.fileName);
			newClass.setIsAnonymous(true, current);

		} else {
			logIndented(0, "Shouldn't get here!!!! " + n.getParentNode().getParentNode().getParentNode().getClass().getName());
			logIndented(1, "Shouldn't get here!!!! " + n.getParentNode().getParentNode().getClass().getName());
			logIndented(2, "Shouldn't get here!!!! " + n.getParentNode().getClass().getName());
			return;
		}

		project.addSearchIndex(currentPackage.getName(), newClass.getCanonicalName(), n.toString());

		current = newClass;
		heirarchyStack.push(newClass);

		((Class) current).setIsInterface(n.isInterface());
		addImports();

		for (ClassOrInterfaceType type : n.getExtends()) {
			((Class) current).setExtendsStr(type.getName());
			current.addUnresolvedClass(heirarchyStack.size() + 1, type.getName());
		}

		for (ClassOrInterfaceType type : n.getImplements()) {
			((Class) current).addImplsStr(type.getName());
			current.addUnresolvedInterface(type.getName());
		}

		depth++;
		super.visit(n, arg);
		depth--;

		logIndented(heirarchyStack.size(), "Done with " + ((Class) current).getCanonicalName());
		heirarchyStack.pop();
		if (!heirarchyStack.isEmpty())
			current = heirarchyStack.peek();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ClassOrInterfaceType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		for (String typeStr : DependentBase.splitType(n.toString())) {
			current.addUnresolvedClass(heirarchyStack.size() + 1, typeStr);
		}

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(CompilationUnit n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ConditionalExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ConstructorDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// String modifier = modifiersToString(n.getModifiers());
		// TODO implement handling modifiers

		String params = "";
		Map<String, String> paramMap = new LinkedHashMap<String, String>();

		if (n.getParameters() != null) {
			for (Parameter param : n.getParameters()) {
				params += (params.isEmpty()) ? "" : ", ";
				params += param.getType();

				paramMap.put(param.getId().getName(), param.getType().toString());
			}
		}

		Method method = ((Class) current).getOrCreateAndGetMethod(heirarchyStack.size() + 1, n.getName() + "(" + params + ")");
		method.setReturnType(heirarchyStack.size() + 1, current.getCanonicalName(), false);

		current = method;
		heirarchyStack.push(method);

		method.setParamMap(heirarchyStack.size() + 1, paramMap);

		depth++;
		super.visit(n, arg);
		depth--;

		logIndented(heirarchyStack.size(), "Done with " + current.getCanonicalName());
		heirarchyStack.pop();
		if (!heirarchyStack.isEmpty())
			current = heirarchyStack.peek();
		logIndented(heirarchyStack.size(), "Back with " + current.getCanonicalName());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ContinueStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(DoStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(DoubleLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(EmptyMemberDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(EmptyStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(EmptyTypeDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(EnclosedExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(EnumConstantDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(EnumDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.getNameExpr().getName());

		// String modifiers = modifiersToString(n.getModifiers());
		// TODO implement handling modifiers

		Class newClass;
		if (n.getParentNode() instanceof CompilationUnit) {
			newClass = currentPackage.getOrCreateAndGetClass(heirarchyStack.size(), n.getName(), true, this.fileName);
		} else {
			String parentNodeName = null;
			if (n.getParentNode() instanceof ClassOrInterfaceDeclaration) {
				parentNodeName = ((ClassOrInterfaceDeclaration) n.getParentNode()).getName();
			} else if (n.getParentNode() instanceof AnnotationDeclaration) {
				parentNodeName = ((AnnotationDeclaration) n.getParentNode()).getName();
			}
			newClass = currentPackage.getOrCreateAndGetClass(heirarchyStack.size(), parentNodeName + "." + n.getName(), true, this.fileName);
		}

		project.addSearchIndex(currentPackage.getName(), newClass.getCanonicalName(), n.toString());

		current = newClass;
		heirarchyStack.push(newClass);

		((Class) current).setIsEnum(true);
		addImports();

		depth++;
		super.visit(n, arg);
		depth--;

		logIndented(heirarchyStack.size(), "Done with " + ((Class) current).getCanonicalName());
		heirarchyStack.pop();
		if (!heirarchyStack.isEmpty())
			current = heirarchyStack.peek();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ExplicitConstructorInvocationStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ExpressionStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(FieldAccessExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		current.addPotentialClass(heirarchyStack.size() + 1, n.getScope().toString());
		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(FieldDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		addVariable(n);

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(ForeachStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(ForStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(IfStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(ImportDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// Handled in QualifiedNameExpr

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(InitializerDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(InstanceOfExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		current.addUnresolvedClass(heirarchyStack.size() + 1, n.getType().toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(IntegerLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(IntegerLiteralMinValueExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(JavadocComment n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(LabeledStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(LambdaExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		String params = "";
		Map<String, String> paramMap = new LinkedHashMap<String, String>();

		if (n.getParameters() != null) {
			for (Parameter param : n.getParameters()) {
				params += (params.isEmpty()) ? "" : ", ";
				params += param.getType();

				paramMap.put(param.getId().getName(), param.getType().toString());
			}
		}

		Block block = new Block(heirarchyStack.size() + 1, current);

		current = block;
		heirarchyStack.push(block);

		((Block) current).setParamMap(heirarchyStack.size() + 1, paramMap);

		depth++;
		super.visit(n, arg);
		depth--;

		logIndented(heirarchyStack.size(), "Done with block");
		heirarchyStack.pop();
		if (!heirarchyStack.isEmpty())
			current = heirarchyStack.peek();
		logIndented(heirarchyStack.size(), "Back with " + ((DependentBase) current).getCanonicalName());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(LineComment n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(LongLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(LongLiteralMinValueExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(MarkerAnnotationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(MemberValuePair n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override

	public void visit(MethodCallExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// TODO Need to try to match Arg params to types and add to method call

		String typeOrVarName = null;
		if (n.getScope() != null) {
			typeOrVarName = n.getScope().toString();

		} else {
			// when scope == null, it appears that the method calls are class local.  going to use current
			// class name as the type
			typeOrVarName = "this";

			//			LOGGER.info("Method call nameExpr: " + n.getNameExpr().toString());
			//
			//			for (Node child : n.getChildrenNodes()) {
			//				LOGGER.info("  Method call (" + child.getClass().getName() + "): " + child.toString());
			//			}
			//
			//			for (Type t : n.getTypeArgs()) {
			//				LOGGER.info("  Type Arg: " + t.toString());
			//			}
			//
			//			for (Expression t : n.getArgs()) {
			//				LOGGER.info("  Arg: " + t.toString());
			//			}
		}

		current.addUnresolvedMethodCall(heirarchyStack.size() + 1, typeOrVarName, n.getName());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(MethodDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString() + "; modifiers: " + n.getModifiers());

		// String modifiers = modifiersToString(n.getModifiers());
		// TODO implement handling modifiers

		String params = "";
		Map<String, String> paramMap = new LinkedHashMap<String, String>();

		if (n.getParameters() != null) {
			for (Parameter param : n.getParameters()) {
				params += (params.isEmpty()) ? "" : ", ";
				params += param.getType();

				paramMap.put(param.getId().getName(), param.getType().toString());
			}
		}

		if (current instanceof Class) {
			Method newMethod = ((Class) current).getOrCreateAndGetMethod(heirarchyStack.size() + 1, n.getName() + "(" + params + ")");

			current = newMethod;
			heirarchyStack.push(newMethod);

			((Method) current).setParamMap(heirarchyStack.size() + 1, paramMap);
			((Method) current).setReturnType(heirarchyStack.size() + 1, n.getType().toString(), true);

			depth++;
			super.visit(n, arg);
			depth--;

			logIndented(heirarchyStack.size(), "Done with " + ((Method) current).getCanonicalName());
			heirarchyStack.pop();
			if (!heirarchyStack.isEmpty())
				current = heirarchyStack.peek();
			logIndented(heirarchyStack.size(), "Back with " + ((DependentBase) current).getCanonicalName());
		} else {
			super.visit(n, arg);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(MethodReferenceExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(MultiTypeParameter n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(NameExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString() + "; " + n.getParentNode().getClass().getName());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(NormalAnnotationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		current.addUnresolvedAnnotations(n.getName().getName());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(NullLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ObjectCreationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		current.addUnresolvedClass(heirarchyStack.size() + 1, n.getType().getName());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(PackageDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString().trim());

		this.currentPackage = project.getOrCreateAndGetPackage(1, n.getName().toString(), true, true);

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(Parameter n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(PrimitiveType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// I think these can be safely ignored

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(QualifiedNameExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		if (n.getParentNode() instanceof ImportDeclaration) {
			imports.add(n.toString());
		}

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ReferenceType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// I think these can be safely ignored

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ReturnStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(SingleMemberAnnotationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(StringLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(SuperExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(SwitchEntryStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(SwitchStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(SynchronizedStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ThisExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(ThrowStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(TryStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(TypeDeclarationStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(TypeExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(TypeParameter n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(UnaryExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(UnknownType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(VariableDeclarationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		addVariable(n);

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(VariableDeclarator n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(VariableDeclaratorId n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(VoidType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// I think these can be safely ignored

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(WhileStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(WildcardType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// I think these can be safely ignored

		depth++;
		super.visit(n, arg);
		depth--;

	}

	public void logIndented(int depth, String str) {
		if (LOGGER.isDebugEnabled()) {
			String retval = "";
			for (int i = 0; i < depth; i++) {
				retval += "    ";
			}
			LOGGER.debug(retval + str);
		}
	}
}
