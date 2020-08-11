package interpreter;

import interpreter.ast.*;
import interpreter.ir.Expression;
import interpreter.ir.Statement;
import interpreter.symbols.Symbol;
import interpreter.symbols.SymbolTable;
import interpreter.symbols.Type;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public class IntermediateCodeGenerator extends VisitorAdaptor
{
	private static final String ENTRY_POINT = "main";

	private final SymbolTable symbolTable;

	private final Stack<Expression> expressionStack = new Stack<>();
	private final Stack<LinkedList<Statement>> statementStack = new Stack<>();

	private Symbol method;

	private String superClass;
	private Symbol classSymbol;

	private Statement.Program intermediateCode;

	public IntermediateCodeGenerator(SymbolTable symbolTable)
	{
		this.symbolTable = symbolTable;
	}

	public void generate(Program program)
	{
		program.traverseBottomUp(this);
	}

	public Statement.Program getIntermediateCode()
	{
		return intermediateCode;
	}

	public void writeIRFile(String fileName) throws IOException
	{
		ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(fileName));

		output.writeObject(intermediateCode);

		output.close();
	}

	//////////////////////////////////////
	//////////// HELPER METHODS //////////
	//////////////////////////////////////

	private Statement.Declaration.Type getVariableType(Type type)
	{
		if (type == SymbolTable.INT_TYPE)
		{
			return Statement.Declaration.Type.INTEGER;
		}
		else if (type == SymbolTable.CHAR_TYPE)
		{
			return Statement.Declaration.Type.CHARACTER;
		}
		else if (type == SymbolTable.BOOL_TYPE)
		{
			return Statement.Declaration.Type.BOOLEAN;
		}
		else
		{
			return Statement.Declaration.Type.REFERENCE;
		}
	}

	private Expression.Binary.Operation getBinaryOperation(Object object)
	{
		final String regex = "([a-z])([A-Z])";
		final String replacement = "$1_$2";

		final String className = object.getClass().getSimpleName();

		return Expression.Binary.Operation.valueOf(className.replaceAll(regex, replacement).toUpperCase());
	}

	private void createClassStatement(int line)
	{
		String name = classSymbol.getName();
		List<Statement.Class.Field> fields = new ArrayList<>();

		for (Symbol member : classSymbol.getType().getMembersCollection())
		{
			if (member.getKind() == Symbol.FIELD)
			{
				fields.add(new Statement.Class.Field(member.getName(), getVariableType(member.getType())));
			}
		}

		List<Statement> statements = statementStack.pop();
		List<Statement.Method> methods = new LinkedList<>();

		for (Statement statement : statements)
		{
			methods.add((Statement.Method) statement);
		}

		symbolTable.exitScope();

		statementStack.peek().add(new Statement.Class(line, name, superClass, fields, methods));
	}

	private void createMethodStatement(int line)
	{
		List<String> parameters = new ArrayList<>();

		for (int i = 1; i <= method.getParameter(); i++)
		{
			parameters.add(symbolTable.getFormalParameter(method, i).getName());
		}

		LinkedList<Statement> body = statementStack.pop();
		boolean isVoid = method.getType() == SymbolTable.NO_TYPE;

		symbolTable.exitScope();

		statementStack.peek().add(new Statement.Method(line, isVoid, method.getName(), parameters, body));
	}

	//////////////////////////////////////
	///////////// EXPRESSIONS ////////////
	//////////////////////////////////////

	@Override
	public void visit(OrCondition orCondition)
	{
		int line = orCondition.getLine();

		Expression right = expressionStack.pop();
		Expression left  = expressionStack.pop();

		expressionStack.push(new Expression.Logical(line, left, Expression.Logical.Operation.OR, right));
	}

	@Override
	public void visit(AndCondition andCondition)
	{
		int line = andCondition.getLine();

		Expression right = expressionStack.pop();
		Expression left  = expressionStack.pop();

		expressionStack.push(new Expression.Logical(line, left, Expression.Logical.Operation.AND, right));
	}

	@Override
	public void visit(ExprRelation exprRelation)
	{
		int line = exprRelation.getLine();

		Expression right = expressionStack.pop();
		Expression left  = expressionStack.pop();

		Expression.Binary.Operation operation = getBinaryOperation(exprRelation.getRelop());

		expressionStack.push(new Expression.Binary(line, left, operation, right));
	}

	@Override
	public void visit(NegativeTermExpr negativeTermExpr)
	{
		int line = negativeTermExpr.getLine();

		Expression right = expressionStack.pop();
		expressionStack.push(new Expression.Unary(line, Expression.Unary.Operation.NEGATION, right));
	}

	@Override
	public void visit(AddopExpr addopExpr)
	{
		int line = addopExpr.getLine();

		Expression right = expressionStack.pop();
		Expression left  = expressionStack.pop();

		Expression.Binary.Operation operation = getBinaryOperation(addopExpr.getAddop());

		expressionStack.push(new Expression.Binary(line, left, operation, right));
	}

	@Override
	public void visit(MulopTerm mulopTerm)
	{
		int line = mulopTerm.getLine();

		Expression right = expressionStack.pop();
		Expression left  = expressionStack.pop();

		Expression.Binary.Operation operation = getBinaryOperation(mulopTerm.getMulop());

		expressionStack.push(new Expression.Binary(line, left, operation, right));
	}

	@Override
	public void visit(DesignatorName designatorName)
	{
		int line = designatorName.getLine();

		String name = designatorName.getName();
		expressionStack.push(new Expression.Variable(line, name));
	}

	@Override
	public void visit(DesignatorChaining designatorChaining)
	{
		int line = designatorChaining.getLine();

		Expression object = expressionStack.pop();
		String name = designatorChaining.getChainedName();

		expressionStack.push(new Expression.Property(line, object, name));
	}

	@Override
	public void visit(DesignatorIndexing designatorIndexing)
	{
		int line = designatorIndexing.getLine();

		Expression index = expressionStack.pop();
		Expression array = expressionStack.pop();

		expressionStack.push(new Expression.Index(line, array, index));
	}

	@Override
	public void visit(MethodCall methodCall)
	{
		int line = methodCall.getLine();
		Symbol method = methodCall.getCallee().getDesignator().symbol;

		LinkedList<Expression> arguments = new LinkedList<>();

		for (int i = 0; i < method.getParameter(); i++)
		{
			arguments.addFirst(expressionStack.pop());
		}

		Expression callee = expressionStack.pop();

		expressionStack.push(new Expression.Call(line, callee, arguments));
	}

	@Override
	public void visit(IntFactor intFactor)
	{
		int line = intFactor.getLine();

		Expression.Literal literal = new Expression.Literal(line, intFactor.getValue());
		expressionStack.push(literal);
	}

	@Override
	public void visit(CharFactor charFactor)
	{
		int line = charFactor.getLine();

		Expression.Literal literal = new Expression.Literal(line, charFactor.getValue());
		expressionStack.push(literal);
	}

	@Override
	public void visit(BoolFactor boolFactor)
	{
		int line = boolFactor.getLine();

		Expression.Literal literal = new Expression.Literal(line, boolFactor.getValue());
		expressionStack.push(literal);
	}

	@Override
	public void visit(NewScalarFactor newScalarFactor)
	{
		int line = newScalarFactor.getLine();
		String type = newScalarFactor.getTypeName().getName();

		expressionStack.push(new Expression.New(line, type, null));
	}

	@Override
	public void visit(NewVectorFactor newVectorFactor)
	{
		int line = newVectorFactor.getLine();
		String type = newVectorFactor.getTypeName().getName();
		Expression size = expressionStack.pop();

		expressionStack.push(new Expression.New(line, type, size));
	}

	@Override
	public void visit(DelimitedFactor delimitedFactor)
	{
		int line = delimitedFactor.getLine();

		Expression.Group group = new Expression.Group(line, expressionStack.pop());
		expressionStack.push(group);
	}

	//////////////////////////////////////
	///////////// STATEMENTS /////////////
	//////////////////////////////////////

	@Override
	public void visit(Program program)
	{
		int line = program.getLine();

		symbolTable.exitScope();
		intermediateCode = new Statement.Program(line, statementStack.pop());

		// call to main method which simulates
		// an entry point of the program
		Expression.Call mainCall = new Expression.Call(
				line,
				new Expression.Variable(line, ENTRY_POINT),
				Collections.emptyList()
		);

		intermediateCode.statements.add(new Statement.Call(line, mainCall));
	}

	@Override
	public void visit(ProgramName programName)
	{
		symbolTable.enterScope(programName.getName());
		statementStack.push(new LinkedList<>());
	}

	@Override
	public void visit(Const constant)
	{
		int line = constant.getLine();

		Symbol symbol = symbolTable.findOnStack(constant.getName());
		statementStack.peek().add(new Statement.Constant(line, symbol.getName(), symbol.getValue()));
	}

	@Override
	public void visit(ValidVar validVar)
	{
		int line = validVar.getLine();
		Symbol symbol = symbolTable.findOnStack(validVar.getName());

		statementStack.peek().add(new Statement.Declaration(line, getVariableType(symbol.getType()), symbol.getName()));
	}

	@Override
	public void visit(ClassDecl classDecl)
	{
		int line = classDecl.getLine();
		createClassStatement(line);
	}

	@Override
	public void visit(ClassName className)
	{
		classSymbol = symbolTable.findOnStack(className.getName());

		symbolTable.enterScope(classSymbol.getName());
		statementStack.push(new LinkedList<>());
	}

	@Override
	public void visit(WithInheritance withInheritance)
	{
		superClass = withInheritance.getTypeName().getName();
	}

	@Override
	public void visit(WithoutInheritance withoutInheritance)
	{
		superClass = null;
	}

	@Override
	public void visit(AbstractClassDecl abstractClassDecl)
	{
		int line = abstractClassDecl.getLine();
		createClassStatement(line);
	}

	@Override
	public void visit(AbstractClassName abstractClassName)
	{
		classSymbol = symbolTable.findOnStack(abstractClassName.getName());

		symbolTable.enterScope(classSymbol.getName());
		statementStack.push(new LinkedList<>());
	}

	@Override
	public void visit(MethodDecl methodDecl)
	{
		createMethodStatement(methodDecl.getLine());
	}

	@Override
	public void visit(ValidAbstractMethodDecl validAbstractMethodDecl)
	{
		createMethodStatement(validAbstractMethodDecl.getLine());
	}

	@Override
	public void visit(MethodName methodName)
	{
		method = symbolTable.findOnStack(methodName.getName());

		symbolTable.enterScope(method.getName());
		statementStack.push(new LinkedList<>());
	}

	@Override
	public void visit(DesignatorAssignment designatorAssignment)
	{
		int line = designatorAssignment.getLine();

		Expression value = expressionStack.pop();
		Expression destination = expressionStack.pop();

		statementStack.peek().add(new Statement.Assignment(line, destination, value));
	}

	@Override
	public void visit(DesignatorMethodCall designatorMethodCall)
	{
		int line = designatorMethodCall.getLine();
		Expression.Call expression = (Expression.Call) expressionStack.pop();

		statementStack.peek().add(new Statement.Call(line, expression));
	}

	@Override
	public void visit(DesignatorIncrement designatorIncrement)
	{
		int line = designatorIncrement.getLine();
		Expression number = expressionStack.pop();

		statementStack.peek().add(new Statement.Increment(line, number));
	}

	@Override
	public void visit(DesignatorDecrement designatorDecrement)
	{
		int line = designatorDecrement.getLine();
		Expression number = expressionStack.pop();

		statementStack.peek().add(new Statement.Decrement(line, number));
	}

	@Override
	public void visit(IfStatement ifStatement)
	{
		int line = ifStatement.getLine();
		Expression condition = expressionStack.pop();

		Statement elseBranch = null;

		if (ifStatement.getElseStatement() instanceof ElseBranch)
		{
			elseBranch = statementStack.peek().removeLast();
		}

		Statement thenBranch = statementStack.peek().removeLast();

		statementStack.peek().add(new Statement.If(line, condition, thenBranch, elseBranch));
	}

	@Override
	public void visit(LoopStatement loopStatement)
	{
		int line = loopStatement.getLine();

		Statement initializer = null;
		Statement increment = null;

		Expression condition;

		Statement body = statementStack.peek().removeLast();

		if (loopStatement.getLoopStep() instanceof LoopStepStatement)
		{
			increment = statementStack.peek().removeLast();
		}

		if (loopStatement.getLoopCondition() instanceof LoopStopCondition)
		{
			condition = expressionStack.pop();
		}
		else
		{
			condition = new Expression.Literal(line, true);
		}

		if (loopStatement.getLoopInit() instanceof LoopInitStatement)
		{
			initializer = statementStack.peek().removeLast();
		}

		statementStack.peek().add(new Statement.For(line, initializer, condition, increment, body));
	}

	@Override
	public void visit(BreakStatement breakStatement)
	{
		int line = breakStatement.getLine();
		statementStack.peek().add(new Statement.Control(line, Statement.Control.Type.BREAK));
	}

	@Override
	public void visit(ContinueStatement continueStatement)
	{
		int line = continueStatement.getLine();
		statementStack.peek().add(new Statement.Control(line, Statement.Control.Type.CONTINUE));
	}

	@Override
	public void visit(ReturnStatement returnStatement)
	{
		int line = returnStatement.getLine();
		statementStack.peek().add(new Statement.Return(line, null));
	}

	@Override
	public void visit(ReturnResultStatement returnResultStatement)
	{
		int line = returnResultStatement.getLine();
		statementStack.peek().add(new Statement.Return(line, expressionStack.pop()));
	}

	@Override
	public void visit(ReadStatement readStatement)
	{
		int line = readStatement.getLine();
		Expression destination = expressionStack.pop();
		Type symbolType = readStatement.getDesignator().symbol.getType();

		statementStack.peek().add(new Statement.Read(line, getVariableType(symbolType), destination));
	}

	@Override
	public void visit(PrintStatement printStatement)
	{
		int line = printStatement.getLine();

		Expression expression = expressionStack.pop();
		Integer width = null;

		if (printStatement.getPrintWidth() instanceof Width)
		{
			width = ((Width) printStatement.getPrintWidth()).getValue();
		}

		statementStack.peek().add(new Statement.Print(line, expression, width));
	}

	@Override
	public void visit(StatementBlock statementBlock)
	{
		int line = statementBlock.getLine();
		Statement.Block block = new Statement.Block(line, statementStack.pop());

		statementStack.peek().add(block);
	}

	@Override
	public void visit(BlockEntry blockEntry)
	{
		statementStack.push(new LinkedList<>());
	}
}
