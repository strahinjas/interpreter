package interpreter;

import interpreter.ast.*;
import interpreter.symbols.Symbol;
import interpreter.symbols.SymbolTable;
import interpreter.symbols.Type;

import java.util.Stack;

public class SemanticAnalyzer extends VisitorAdaptor
{
	//////////////////////////////////////
	/////////////// GLOBAL ///////////////
	//////////////////////////////////////

	private boolean errorDetected;

	private boolean mainFound;

	private int loopLevel;
	private int parameterCount;

	private final SymbolTable symbolTable;

	private Type currentType = SymbolTable.NO_TYPE;
	private Type currentMethodReturnType = SymbolTable.NO_TYPE;
	private Type currentBaseClassType = SymbolTable.NO_TYPE;

	private Symbol currentClass = SymbolTable.NO_SYMBOL;
	private Symbol currentMethod = SymbolTable.NO_SYMBOL;
	private Symbol currentOverrideMethod = SymbolTable.NO_SYMBOL;

	private static final class Callee
	{
		public Symbol callee;
		public int argumentCount;

		public Callee(Symbol callee)
		{
			this.callee = callee;
		}

		public int addArgument()
		{
			return ++argumentCount;
		}
	}

	private final Stack<Callee> calleeStack = new Stack<>();

	public SemanticAnalyzer(SymbolTable symbolTable)
	{
		this.symbolTable = symbolTable;
	}

	public void analyze(Program program)
	{
		program.traverseBottomUp(this);
	}

	public boolean isSemanticallyCorrect()
	{
		return !errorDetected;
	}

	//////////////////////////////////////
	//////////// HELPER METHODS //////////
	//////////////////////////////////////

	private boolean isDeclaredInCurrentScope(String name)
	{
		return symbolTable.findInCurrentScope(name) != SymbolTable.NO_SYMBOL;
	}

	private void initializeConstant(Literal literal, Symbol constSymbol)
	{
		if (literal instanceof IntLiteral)
		{
			constSymbol.setValue(((IntLiteral) literal).getValue());
		}
		else if (literal instanceof CharLiteral)
		{
			constSymbol.setValue(((CharLiteral) literal).getValue());
		}
		else
		{
			constSymbol.setValue(((BoolLiteral) literal).getValue());
		}
	}

	private boolean isAssignable(Type source, Type destination)
	{
		if (source.assignableTo(destination)) return true;

		if ((source.getKind() == Type.CLASS || source.getKind() == Type.ABSTRACT_CLASS) &&
			(destination.getKind() == Type.CLASS || destination.getKind() == Type.ABSTRACT_CLASS))
		{
			while ((source = symbolTable.getExtendingType(source)) != SymbolTable.NO_TYPE)
			{
				if (source.assignableTo(destination)) return true;
			}
		}

		return false;
	}

	private Symbol getSymbolCopy(Symbol original)
	{
		return new Symbol(original.getKind(), original.getName(),
						  original.getType(), original.getValue());
	}

	private void reportDeclaredInScope(int line, String name)
	{
		reportError(line, "Name '" + name + "' has already been declared in this scope");
	}

	public void reportError(int line, String message)
	{
		errorDetected = true;
		System.out.println("Semantic error (line " + line + "): " + message);
	}

	private void checkKeywords(int line, String name)
	{
		if (name.equals("int") || name.equals("char") || name.equals("bool") || name.equals("null") ||
			name.equals("eol") || name.equals("chr") || name.equals("ord") || name.equals("len"))
		{
			System.out.println("Semantic warning (line " + line + "): Redefinition of predefined word '" + name + "'");
		}
	}

	//////////////////////////////////////
	/////////////// PROGRAM //////////////
	//////////////////////////////////////

	@Override
	public void visit(ProgramName programName)
	{
		programName.symbol = symbolTable.insert(Symbol.PROGRAM, programName.getName(), SymbolTable.NO_TYPE);
		symbolTable.openScope();
	}

	@Override
	public void visit(Program program)
	{
		if (!mainFound)
		{
			reportError(program.getLine(), "Static void method 'main' not defined");
		}

		symbolTable.chainSymbols(program.getProgramName().symbol);
		symbolTable.closeScope();
	}

	//////////////////////////////////////
	/////// CONSTANTS & VARIABLES ////////
	//////////////////////////////////////

	@Override
	public void visit(TypeName typeName)
	{
		Symbol typeSymbol = symbolTable.find(typeName.getName());

		if (typeSymbol == SymbolTable.NO_SYMBOL)
		{
			reportError(typeName.getLine(), "Type '" + typeName.getName() + "' has not been defined");
			typeName.type = SymbolTable.NO_TYPE;
		}
		else if (typeSymbol.getKind() == Symbol.TYPE)
		{
			typeName.type = typeSymbol.getType();
		}
		else
		{
			reportError(typeName.getLine(), "Identifier '" + typeName.getName() + "' does not represent a type");
			typeName.type = SymbolTable.NO_TYPE;
		}

		currentType = typeName.type;
	}

	@Override
	public void visit(BoolLiteral boolLiteral)
	{
		boolLiteral.type = SymbolTable.BOOL_TYPE;
	}

	@Override
	public void visit(CharLiteral charLiteral)
	{
		charLiteral.type = SymbolTable.CHAR_TYPE;
	}

	@Override
	public void visit(IntLiteral intLiteral)
	{
		intLiteral.type = SymbolTable.INT_TYPE;
	}

	@Override
	public void visit(Const constant)
	{
		if (constant.getLiteral().type.equals(currentType))
		{
			if (isDeclaredInCurrentScope(constant.getName()))
			{
				reportDeclaredInScope(constant.getLine(), constant.getName());
			}
			else
			{
				checkKeywords(constant.getLine(), constant.getName());

				Symbol constSymbol = symbolTable.insert(Symbol.CONSTANT,
														constant.getName(),
														constant.getLiteral().type);

				initializeConstant(constant.getLiteral(), constSymbol);
			}
		}
		else
		{
			reportError(constant.getLine(), "Literal type does not match one in constant definition");
		}
	}

	@Override
	public void visit(ValidVar validVar)
	{
		if (isDeclaredInCurrentScope(validVar.getName()))
		{
			reportDeclaredInScope(validVar.getLine(), validVar.getName());
		}
		else
		{
			checkKeywords(validVar.getLine(), validVar.getName());

			if (validVar.getBrackets() instanceof ScalarType)
			{
				symbolTable.insert(Symbol.VARIABLE, validVar.getName(), currentType);
			}
			else
			{
				symbolTable.insert(Symbol.VARIABLE, validVar.getName(), symbolTable.getArrayType(currentType));
			}
		}
	}

	//////////////////////////////////////
	/////////////// CLASSES //////////////
	//////////////////////////////////////

	@Override
	public void visit(ClassName className)
	{
		if (isDeclaredInCurrentScope(className.getName()))
		{
			reportDeclaredInScope(className.getLine(), className.getName());
		}
		else
		{
			checkKeywords(className.getLine(), className.getName());

			currentClass = symbolTable.insert(Symbol.TYPE,
											  className.getName(),
											  symbolTable.getClassType(className.getName()));

			symbolTable.openScope();
		}

		className.symbol = currentClass;
	}

	@Override
	public void visit(WithInheritance withInheritance)
	{
		String baseClassName = withInheritance.getTypeName().getName();
		Type baseClassType = withInheritance.getTypeName().type;

		if (symbolTable.find(baseClassName) == SymbolTable.NO_SYMBOL)
		{
			reportError(withInheritance.getLine(), "Base class '" + baseClassName + "' has not been defined");
		}
		else if (currentClass.getType() == baseClassType)
		{
			reportError(withInheritance.getLine(), "Class cannot extend itself");
		}
		else if (baseClassType.getKind() != Type.CLASS &&
				 baseClassType.getKind() != Type.ABSTRACT_CLASS)
		{
			reportError(withInheritance.getLine(), "Class can only extend another class type or abstract class type");
		}
		else
		{
			currentBaseClassType = baseClassType;
			currentClass.getType().setParentType(baseClassType);

			for (Symbol member : baseClassType.getMembersCollection())
			{
				if (member.getKind() == Symbol.FIELD)
				{
					symbolTable.insert(member.getKind(), member.getName(), member.getType());
				}
			}
		}
	}

	@Override
	public void visit(ValidField validField)
	{
		symbolTable.remove(validField.getName());
		checkKeywords(validField.getLine(), validField.getName());

		if (validField.getBrackets() instanceof ScalarType)
		{
			symbolTable.insert(Symbol.FIELD, validField.getName(), currentType);
		}
		else
		{
			symbolTable.insert(Symbol.FIELD, validField.getName(), symbolTable.getArrayType(currentType));
		}
	}

	@Override
	public void visit(FieldDeclarations fieldDeclarations)
	{
		if (currentClass.getType().getParentType() != null)
		{
			for (Symbol symbol : currentBaseClassType.getMembersCollection())
			{
				if (symbol.getKind() == Symbol.METHOD &&
					(currentClass.getType().getKind() == Type.ABSTRACT_CLASS || symbol.getParameter() >= 0))
				{
					Symbol method = getSymbolCopy(symbol);
					symbolTable.insert(method);
					symbolTable.openScope();

					symbolTable.insert(new Symbol(Symbol.VARIABLE, "this", currentClass.getType()));

					for (Symbol local : symbol.getLocals().values())
					{
						if (!local.getName().equals("this"))
						{
							symbolTable.insert(getSymbolCopy(local));
						}
					}

					symbolTable.chainSymbols(method);
					symbolTable.closeScope();
				}
			}
		}
	}

	@Override
	public void visit(ClassDecl classDecl)
	{
		symbolTable.chainSymbols(currentClass.getType());

		if (currentClass.getType().getKind() == Type.CLASS &&
			currentBaseClassType.getKind() == Type.ABSTRACT_CLASS)
		{
			for (Symbol member : currentBaseClassType.getMembersCollection())
			{
				if (member.getKind() == Symbol.METHOD &&
					member.getParameter() < 0)
				{
					Symbol method = symbolTable.findInCurrentScope(member.getName());

					if (method == SymbolTable.NO_SYMBOL)
					{
						reportError(classDecl.getLine(), "Class '" + currentClass.getName() +
														 "' does not implement abstract method '" + member.getName() + "'");
					}
					else if (method.getParameter() != -member.getParameter() - 1)
					{
						reportError(classDecl.getLine(), "Class '" + currentClass.getName() +
														 "' implements abstract method '" + member.getName() +
														 "' but with wrong number of parameters");
					}
					else if (!method.getType().equals(member.getType()))
					{
						reportError(classDecl.getLine(), "Class '" + currentClass.getName() +
														 "' implements abstract method '" + member.getName() +
														 "' but with wrong return type");
					}
					else
					{
						boolean areEqual = true;

						for (int i = 1; i < method.getParameter(); i++)
						{
							Symbol abstractParameter = symbolTable.getFormalParameter(member, i);
							Symbol concreteParameter = symbolTable.getFormalParameter(method, i);

							if (!concreteParameter.getType().equals(abstractParameter.getType()))
							{
								areEqual = false;
								break;
							}
						}

						if (!areEqual)
						{
							reportError(classDecl.getLine(), "Class '" + currentClass.getName() +
															 "' implements abstract method '" + member.getName() +
															 "' but with wrong type of parameters");
						}
					}
				}
			}
		}

		symbolTable.closeScope();
		currentClass = SymbolTable.NO_SYMBOL;
		currentBaseClassType = SymbolTable.NO_TYPE;
	}

	//////////////////////////////////////
	////////// ABSTRACT CLASSES //////////
	//////////////////////////////////////

	@Override
	public void visit(AbstractClassName abstractClassName)
	{
		if (isDeclaredInCurrentScope(abstractClassName.getName()))
		{
			reportDeclaredInScope(abstractClassName.getLine(), abstractClassName.getName());
		}
		else
		{
			checkKeywords(abstractClassName.getLine(), abstractClassName.getName());

			currentClass = symbolTable.insert(Symbol.TYPE,
											  abstractClassName.getName(),
											  symbolTable.getAbstractClassType(abstractClassName.getName()));

			symbolTable.openScope();
		}

		abstractClassName.symbol = currentClass;
	}

	@Override
	public void visit(AbstractClassDecl abstractClassDecl)
	{
		symbolTable.chainSymbols(currentClass.getType());
		symbolTable.closeScope();
		currentClass = SymbolTable.NO_SYMBOL;
		currentBaseClassType = SymbolTable.NO_TYPE;
	}

	//////////////////////////////////////
	/////////////// METHODS //////////////
	//////////////////////////////////////

	@Override
	public void visit(SpecialReturnType specialReturnType)
	{
		currentMethodReturnType = specialReturnType.getTypeName().type;
	}

	@Override
	public void visit(VoidReturnType voidReturnType)
	{
		currentMethodReturnType = SymbolTable.NO_TYPE;
	}

	@Override
	public void visit(MethodName methodName)
	{
		loopLevel = 0;

		if (currentClass != SymbolTable.NO_SYMBOL &&
			currentBaseClassType != SymbolTable.NO_TYPE)
		{
			Symbol methodSymbol = currentBaseClassType.getMembers().get(methodName.getName());

			if (methodSymbol != null && methodSymbol.getKind() == Symbol.METHOD)
			{
				currentOverrideMethod = methodSymbol;
				symbolTable.remove(methodName.getName());
			}
		}

		if (isDeclaredInCurrentScope(methodName.getName()))
		{
			reportDeclaredInScope(methodName.getLine(), methodName.getName());
		}
		else
		{
			checkKeywords(methodName.getLine(), methodName.getName());
			currentMethod = symbolTable.insert(Symbol.METHOD, methodName.getName(), currentMethodReturnType);

			if (currentClass == SymbolTable.NO_SYMBOL &&
				methodName.getName().equals(SymbolTable.ENTRY_POINT))
			{
				mainFound = true;
				if (currentMethodReturnType != SymbolTable.NO_TYPE)
				{
					reportError(methodName.getLine(), "Main method must have void return type");
				}
			}

			if (currentClass != SymbolTable.NO_SYMBOL)
			{
				symbolTable.chainSymbols(currentClass.getType());
			}

			parameterCount = 0;
			symbolTable.openScope();

			if (currentClass != SymbolTable.NO_SYMBOL)
			{
				symbolTable.insert(Symbol.VARIABLE, "this", currentClass.getType());
			}
		}

		methodName.symbol = currentMethod;
	}

	@Override
	public void visit(ValidParameter validParameter)
	{
		if (isDeclaredInCurrentScope(validParameter.getName()))
		{
			reportDeclaredInScope(validParameter.getLine(), validParameter.getName());
		}
		else
		{
			if (currentClass == SymbolTable.NO_SYMBOL &&
				currentMethod.getName().equals(SymbolTable.ENTRY_POINT))
			{
				reportError(validParameter.getLine(), "Main method cannot have any formal parameters");
			}

			checkKeywords(validParameter.getLine(), validParameter.getName());
			Symbol parameter;

			if (validParameter.getBrackets() instanceof ScalarType)
			{
				parameter = symbolTable.insert(Symbol.VARIABLE,
											   validParameter.getName(),
											   currentType);
			}
			else
			{
				parameter = symbolTable.insert(Symbol.VARIABLE,
											   validParameter.getName(),
											   symbolTable.getArrayType((currentType)));
			}

			parameter.setParameter(++parameterCount);

			if (currentOverrideMethod != SymbolTable.NO_SYMBOL)
			{
				Symbol baseParameter = symbolTable.getFormalParameter(currentOverrideMethod, parameterCount);

				if (baseParameter == SymbolTable.NO_SYMBOL)
				{
					reportError(validParameter.getLine(),
								"Invalid number of parameters in method '" + currentMethod.getName() +
								"' in class '" + currentClass.getName() + "'");
				}
				else if (parameter.getType() != baseParameter.getType())
				{
					reportError(validParameter.getLine(),
								"Invalid parameter type in method '" + currentMethod.getName() +
								"' in class '" + currentClass.getName() + "'");
				}
			}
		}
	}

	@Override
	public void visit(MethodEntry methodEntry)
	{
		currentMethod.setParameter(parameterCount);
		symbolTable.chainSymbols(currentMethod);
	}

	@Override
	public void visit(MethodDecl methodDecl)
	{
		symbolTable.closeScope();

		currentMethod = SymbolTable.NO_SYMBOL;
		currentOverrideMethod = SymbolTable.NO_SYMBOL;
		currentMethodReturnType = SymbolTable.NO_TYPE;
	}

	@Override
	public void visit(ValidAbstractMethodDecl validAbstractMethodDecl)
	{
		currentMethod.setParameter(-parameterCount - 1);
		symbolTable.chainSymbols(currentMethod);
		symbolTable.closeScope();

		currentMethod = SymbolTable.NO_SYMBOL;
		currentOverrideMethod = SymbolTable.NO_SYMBOL;
		currentMethodReturnType = SymbolTable.NO_TYPE;
	}

	//////////////////////////////////////
	///////////// DESIGNATOR /////////////
	//////////////////////////////////////

	@Override
	public void visit(DesignatorName designatorName)
	{
		Symbol designator = symbolTable.find(designatorName.getName());

		if (designator == SymbolTable.NO_SYMBOL)
		{
			reportError(designatorName.getLine(), "Name '" + designatorName.getName() + "' has not been declared");
		}

		designatorName.symbol = designator;
	}

	@Override
	public void visit(DesignatorChaining designatorChaining)
	{
		Symbol designator = designatorChaining.getDesignator().symbol;

		if (designator.getType().getKind() == Type.CLASS ||
			designator.getType().getKind() == Type.ABSTRACT_CLASS)
		{
			Symbol member = designator.getType().getMembers().get(designatorChaining.getChainedName());

			if (member == null)
			{
				reportError(designatorChaining.getLine(),
							"Name '" + designatorChaining.getChainedName() + "' is neither a method nor a field");

				designatorChaining.symbol = SymbolTable.NO_SYMBOL;
			}
			else
			{
				designatorChaining.symbol = member;
			}
		}
		else
		{
			reportError(designatorChaining.getLine(), "Invalid use of dot operator");
			designatorChaining.symbol = SymbolTable.NO_SYMBOL;
		}
	}

	@Override
	public void visit(DesignatorIndexing designatorIndexing)
	{
		Symbol designator = designatorIndexing.getDesignator().symbol;

		if (designator.getType().getKind() == Type.ARRAY)
		{
			if (designatorIndexing.getExpr().symbol.getType() != SymbolTable.INT_TYPE)
			{
				reportError(designatorIndexing.getLine(), "Index expression must be of integer type");
				designatorIndexing.symbol = SymbolTable.NO_SYMBOL;
			}

			designatorIndexing.symbol = new Symbol(Symbol.ELEMENT, "element", designator.getType().getParentType());
		}
		else
		{
			reportError(designatorIndexing.getLine(), "Indexing non-array type");
			designatorIndexing.symbol = SymbolTable.NO_SYMBOL;
		}
	}

	@Override
	public void visit(DesignatorAssignment designatorAssignment)
	{
		Symbol designator = designatorAssignment.getDesignator().symbol;

		if (designator.getKind() != Symbol.VARIABLE &&
			designator.getKind() != Symbol.ELEMENT &&
			designator.getKind() != Symbol.FIELD)
		{
			reportError(designatorAssignment.getLine(),
						"Value can only be assigned to variable, array element or class field");
		}
		else
		{
			Type exprType = ((ValidExpr) designatorAssignment.getErrorProneExpr()).getExpr().symbol.getType();

			if (!isAssignable(exprType, designator.getType()))
			{
				String expectedType = symbolTable.getTypeName(designator.getType());
				String givenType = symbolTable.getTypeName((exprType));

				reportError(designatorAssignment.getLine(),
							"Incompatible types in assignment statement (expected '" + expectedType +
							"' but given '" + givenType + "')");
			}
		}
	}

	@Override
	public void visit(DesignatorIncrement designatorIncrement)
	{
		Symbol designator = designatorIncrement.getDesignator().symbol;

		if (designator.getKind() != Symbol.VARIABLE &&
			designator.getKind() != Symbol.ELEMENT &&
			designator.getKind() != Symbol.FIELD)
		{
			reportError(designatorIncrement.getLine(),
						"Increment can only be applied to variable, array element or class field");
		}
		else if (designator.getType() != SymbolTable.INT_TYPE)
		{
			reportError(designatorIncrement.getLine(),
						"Increment can only be applied to integer type");
		}
	}

	@Override
	public void visit(DesignatorDecrement designatorDecrement)
	{
		Symbol designator = designatorDecrement.getDesignator().symbol;

		if (designator.getKind() != Symbol.VARIABLE &&
			designator.getKind() != Symbol.ELEMENT &&
			designator.getKind() != Symbol.FIELD)
		{
			reportError(designatorDecrement.getLine(),
						"Decrement can only be applied to variable, array element or class field");
		}
		else if (designator.getType() != SymbolTable.INT_TYPE)
		{
			reportError(designatorDecrement.getLine(),
						"Decrement can only be applied to integer type");
		}
	}

	@Override
	public void visit(interpreter.ast.Callee callee)
	{
		Symbol calleeMethod = callee.getDesignator().symbol;

		if (calleeMethod.getKind() != Symbol.METHOD)
		{
			reportError(callee.getLine(), "Name '" + calleeMethod.getName() + "' is not a name of a method");
			calleeStack.push(new Callee(SymbolTable.NO_SYMBOL));
		}
		else
		{
			calleeStack.push(new Callee(calleeMethod));
		}

		callee.symbol = calleeMethod;
	}

	@Override
	public void visit(Argument argument)
	{
		Symbol currentCallee = calleeStack.peek().callee;
		int argumentCount = calleeStack.peek().addArgument();

		Symbol parameter = symbolTable.getFormalParameter(currentCallee, argumentCount);

		if (parameter == SymbolTable.NO_SYMBOL)
		{
			reportError(argument.getLine(), "Number of formal and actual parameters does not match");
		}
		else if (!argument.getExpr().symbol.getType().assignableTo(parameter.getType()))
		{
			reportError(argument.getLine(), "Type of actual parameter #" + argumentCount +
											" is incompatible with corresponding formal parameter");
		}
	}

	@Override
	public void visit(MethodCall methodCall)
	{
		int argumentCount = calleeStack.peek().argumentCount;
		int parameterCount = calleeStack.peek().callee.getParameter();

		if (parameterCount < 0)
			parameterCount = -parameterCount - 1;

		calleeStack.pop();

		if (argumentCount != parameterCount)
		{
			reportError(methodCall.getLine(), "Invalid number of arguments");
		}
	}

	//////////////////////////////////////
	///////////// STATEMENTS /////////////
	//////////////////////////////////////

	@Override
	public void visit(ReturnStatement returnStatement)
	{
		if (currentMethod == SymbolTable.NO_SYMBOL)
		{
			reportError(returnStatement.getLine(), "Return statements are not allowed outside of method body");
		}
		else if (currentMethodReturnType != SymbolTable.NO_TYPE)
		{
			reportError(returnStatement.getLine(), "Method must return a value");
		}
	}

	@Override
	public void visit(ReturnResultStatement returnResultStatement)
	{
		if (currentMethod == SymbolTable.NO_SYMBOL)
		{
			reportError(returnResultStatement.getLine(),
						"Return statements are not allowed outside of method body");
		}
		else if (currentMethodReturnType == SymbolTable.NO_TYPE)
		{
			reportError(returnResultStatement.getLine(),
						"Void method should not return an expression");
		}
		else if (!returnResultStatement.getExpr().symbol.getType().assignableTo(currentMethodReturnType))
		{
			reportError(returnResultStatement.getLine(),
						"Expression type in return statement is not assignable to the return type of surrounding method");
		}
	}

	@Override
	public void visit(ReadStatement readStatement)
	{
		int kind = readStatement.getDesignator().symbol.getKind();
		Type type = readStatement.getDesignator().symbol.getType();

		if (kind != Symbol.VARIABLE && kind != Symbol.ELEMENT && kind != Symbol.FIELD)
		{
			reportError(readStatement.getLine(),
						"Value can only be read to variable, array element or class field");
		}
		else if (type != SymbolTable.INT_TYPE &&
				 type != SymbolTable.CHAR_TYPE &&
				 type != SymbolTable.BOOL_TYPE)
		{
			reportError(readStatement.getLine(),
						"Read argument should have one of predeclared types (int, char, bool)");
		}
	}

	@Override
	public void visit(PrintStatement printStatement)
	{
		Type type = printStatement.getExpr().symbol.getType();

		if (type != SymbolTable.INT_TYPE &&
			type != SymbolTable.CHAR_TYPE &&
			type != SymbolTable.BOOL_TYPE)
		{
			reportError(printStatement.getLine(),
						"Print argument should have one of predeclared types (int, char, bool)");
		}
	}

	//////////////////////////////////////
	///////////// EXPRESSIONS ////////////
	//////////////////////////////////////

	@Override
	public void visit(TermExpr termExpr)
	{
		termExpr.symbol = termExpr.getTerm().symbol;
	}

	@Override
	public void visit(NegativeTermExpr negativeTermExpr)
	{
		Symbol term = negativeTermExpr.getTerm().symbol;

		if (term.getType() != SymbolTable.INT_TYPE)
		{
			reportError(negativeTermExpr.getLine(), "Only integer type can be negated");
		}

		negativeTermExpr.symbol = term;
	}

	@Override
	public void visit(AddopExpr addopExpr)
	{
		Symbol expr = addopExpr.getExpr().symbol;
		Symbol term = addopExpr.getTerm().symbol;

		if (expr.getType() != SymbolTable.INT_TYPE ||
			term.getType() != SymbolTable.INT_TYPE)
		{
			reportError(addopExpr.getLine(), "Addition/subtraction can only be done on integers");
			addopExpr.symbol = SymbolTable.NO_SYMBOL;
		}
		else
		{
			addopExpr.symbol = expr;
		}
	}

	@Override
	public void visit(FactorTerm factorTerm)
	{
		factorTerm.symbol = factorTerm.getFactor().symbol;
	}

	@Override
	public void visit(MulopTerm mulopTerm)
	{
		Symbol term = mulopTerm.getTerm().symbol;
		Symbol factor = mulopTerm.getFactor().symbol;

		if (term.getType() != SymbolTable.INT_TYPE ||
			factor.getType() != SymbolTable.INT_TYPE)
		{
			reportError(mulopTerm.getLine(), "Multiplication/division/modulus can only be done on integers");

			mulopTerm.symbol = SymbolTable.NO_SYMBOL;
		}
		else
		{
			mulopTerm.symbol = term;
		}
	}

	@Override
	public void visit(DesignatorFactor designatorFactor)
	{
		designatorFactor.symbol = designatorFactor.getDesignator().symbol;
	}

	@Override
	public void visit(MethodCallFactor methodCallFactor)
	{
		Symbol method = methodCallFactor.getMethodCall().getCallee().getDesignator().symbol;

		if (method.getType() == SymbolTable.NO_TYPE)
		{
			reportError(methodCallFactor.getLine(),
						"Void method '" + method.getName() + "' cannot be used in expressions");
		}

		methodCallFactor.symbol = method;
	}

	@Override
	public void visit(IntFactor intFactor)
	{
		intFactor.symbol = getSymbolCopy(SymbolTable.NO_SYMBOL);
		intFactor.symbol.setType(SymbolTable.INT_TYPE);
	}

	@Override
	public void visit(CharFactor charFactor)
	{
		charFactor.symbol = getSymbolCopy(SymbolTable.NO_SYMBOL);
		charFactor.symbol.setType(SymbolTable.CHAR_TYPE);
	}

	@Override
	public void visit(BoolFactor boolFactor)
	{
		boolFactor.symbol = getSymbolCopy(SymbolTable.NO_SYMBOL);
		boolFactor.symbol.setType(SymbolTable.BOOL_TYPE);
	}

	@Override
	public void visit(NewScalarFactor newScalarFactor)
	{
		if (currentType.getKind() != Type.CLASS)
		{
			reportError(newScalarFactor.getLine(),
						"Only concrete class objects can be dynamically allocated");
		}

		newScalarFactor.symbol = getSymbolCopy(SymbolTable.NO_SYMBOL);
		newScalarFactor.symbol.setType(currentType);
	}

	@Override
	public void visit(NewVectorFactor newVectorFactor)
	{
		if (newVectorFactor.getExpr().symbol.getType() != SymbolTable.INT_TYPE)
		{
			reportError(newVectorFactor.getLine(), "Array size expression must result in an integer");
		}

		newVectorFactor.symbol = getSymbolCopy(SymbolTable.NO_SYMBOL);
		newVectorFactor.symbol.setType(symbolTable.getArrayType(currentType));
	}

	@Override
	public void visit(DelimitedFactor delimitedFactor)
	{
		delimitedFactor.symbol = delimitedFactor.getExpr().symbol;
	}

	//////////////////////////////////////
	///////// CONTROL STRUCTURES /////////
	//////////////////////////////////////

	@Override
	public void visit(ExprFact exprFact)
	{
		exprFact.type = SymbolTable.BOOL_TYPE;
	}

	@Override
	public void visit(ExprRelation exprRelation)
	{
		Type left = exprRelation.getExpr().symbol.getType();
		Type right = exprRelation.getExpr1().symbol.getType();

		if (left.compatibleWith(right))
		{
			if (left.isReferenceType() || right.isReferenceType())
			{
				Relop operation = exprRelation.getRelop();

				if (!(operation instanceof Equals) && !(operation instanceof NotEquals))
				{
					reportError(exprRelation.getLine(),
								"Classes and arrays can only be compared for equality");
				}
			}
		}
		else
		{
			reportError(exprRelation.getLine(), "Incompatible expression types");
		}

		exprRelation.type = SymbolTable.BOOL_TYPE;
	}

	@Override
	public void visit(LoopInitStatement loopInitStatement)
	{
		loopLevel++;
	}

	@Override
	public void visit(NoLoopInit noLoopInit)
	{
		loopLevel++;
	}

	@Override
	public void visit(LoopStatement loopStatement)
	{
		loopLevel--;
	}

	@Override
	public void visit(BreakStatement breakStatement)
	{
		if (loopLevel == 0)
		{
			reportError(breakStatement.getLine(),
						"Break statement is not allowed outside of loop body");
		}
	}

	@Override
	public void visit(ContinueStatement continueStatement)
	{
		if (loopLevel == 0)
		{
			reportError(continueStatement.getLine(),
						"Continue statement is not allowed outside of loop body");
		}
	}
}
