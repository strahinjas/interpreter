package interpreter;

import interpreter.ast.*;
import interpreter.ir.Expression;
import interpreter.ir.Statement;
import interpreter.symbols.Symbol;
import interpreter.symbols.SymbolTable;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class IntermediateCodeGenerator extends VisitorAdaptor
{
	private final SymbolTable symbolTable;

	private final Stack<Expression> expressionStack = new Stack<>();
	private final Stack<List<Statement>> statementStack = new Stack<>();

	private Statement.Program intermediateCode;

	public IntermediateCodeGenerator(SymbolTable symbolTable)
	{
		this.symbolTable = symbolTable;
	}

	public void generate(Program program)
	{
		program.traverseBottomUp(this);
	}

	//////////////////////////////////////
	//////////// HELPER METHODS //////////
	//////////////////////////////////////

	private Expression.Binary.Operation getBinaryOperation(Object object)
	{
		final String regex = "([a-z])([A-Z]+)";
		final String replacement = "$1_$2";

		final String className = object.getClass().getSimpleName();

		return Expression.Binary.Operation.valueOf(className.replaceAll(regex, replacement).toUpperCase());
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
		// TODO: Designator Chaining
	}

	@Override
	public void visit(DesignatorIndexing designatorIndexing)
	{
		// TODO: Designator Indexing
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
		// TODO: New Operator
	}

	@Override
	public void visit(NewVectorFactor newVectorFactor)
	{
		// TODO: New Operator
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
		statementStack.peek().add(new Statement.Variable(line, symbol.getName()));
	}

	@Override
	public void visit(DesignatorAssignment designatorAssignment)
	{
		Expression value = expressionStack.pop();

		// TODO: Assignment
	}

	@Override
	public void visit(DesignatorIncrement designatorIncrement)
	{
		// TODO: Increment
	}

	@Override
	public void visit(DesignatorDecrement designatorDecrement)
	{
		// TODO: Decrement
	}

	@Override
	public void visit(IfStatement ifStatement)
	{
		Statement thenBranch = null;
		Statement elseBranch = null;
	}

	@Override
	public void visit(ReadStatement readStatement)
	{
		// TODO: Read
	}

	@Override
	public void visit(PrintStatement printStatement)
	{
		int line = printStatement.getLine();

		Expression expression = expressionStack.pop();
		Integer width = null;

		if (printStatement.getPrintWidth() instanceof Width)
			width = ((Width) printStatement.getPrintWidth()).getValue();

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
