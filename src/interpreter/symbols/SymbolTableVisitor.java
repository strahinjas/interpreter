package interpreter.symbols;

public class SymbolTableVisitor
{
	private SymbolTable symbolTable;

	private final StringBuilder output = new StringBuilder();

	private final String indentation = "\t";
	private StringBuilder currentIndentation = new StringBuilder();

	public SymbolTableVisitor(SymbolTable symbolTable)
	{
		this.symbolTable = symbolTable;
	}

	private void nextIndentationLevel()
	{
		currentIndentation.append(indentation);
	}

	private void previousIndentationLevel()
	{
		if (currentIndentation.length() > 0)
			currentIndentation.setLength(currentIndentation.length() - indentation.length());
	}

	public void visit(Symbol symbol)
	{
		switch (symbol.getKind())
		{
		case Symbol.CONSTANT:
			output.append("Constant ");
			break;
		case Symbol.VARIABLE:
			output.append("Variable ");
			break;
		case Symbol.TYPE:
			output.append("Type ");
			break;
		case Symbol.METHOD:
			if (symbol.getParameter() < 0)
				output.append("Abstract Method ");
			else
				output.append("Method ");
			break;
		case Symbol.FIELD:
			output.append("Field ");
			break;
		case Symbol.PROGRAM:
			output.append("Program ");
			break;
		}

		output.append(symbol.getName());
		output.append(": ");

		if ((symbol.getKind() == Symbol.VARIABLE && "this".equalsIgnoreCase(symbol.getName())) ||
			 symbol.getKind() == Symbol.METHOD)
			output.append(symbolTable.getTypeName(symbol.getType()));
		else
			symbol.getType().accept(this);

		if (symbol.getKind() == Symbol.CONSTANT)
		{
			output.append(", ");
			output.append(symbol.getName().equals("eol") ? "\\r\\n" : symbol.getValue());
		}

		if (symbol.getKind() == Symbol.PROGRAM || symbol.getKind() == Symbol.METHOD)
		{
			nextIndentationLevel();

			for (Symbol localSymbol : symbol.getLocals().values())
			{
				output.append(System.lineSeparator()).append(currentIndentation.toString());
				localSymbol.accept(this);
			}

			previousIndentationLevel();
		}
	}

	public void visit(Type type)
	{
		switch (type.getKind())
		{
		case Type.NONE:
			output.append("notype");
			break;
		case Type.INT:
			output.append("int");
			break;
		case Type.CHAR:
			output.append("char");
			break;
		case Type.BOOL:
			output.append("bool");
			break;
		case Type.ARRAY:
			output.append("array of ");

			switch (type.getParentType().getKind())
			{
			case Type.NONE:
				output.append("notype");
				break;
			case Type.INT:
				output.append("int");
				break;
			case Type.CHAR:
				output.append("char");
				break;
			case Type.BOOL:
				output.append("bool");
				break;
			case Type.CLASS:
			case Type.ABSTRACT_CLASS:
				output.append(symbolTable.getTypeName(type.getParentType()));
				break;
			}
			break;
		case Type.CLASS:
			output.append("class [");
			nextIndentationLevel();
			for (Symbol symbol : type.getMembersCollection())
			{
				output.append(System.lineSeparator()).append(currentIndentation.toString());
				symbol.accept(this);
			}
			previousIndentationLevel();
			output.append(System.lineSeparator()).append(currentIndentation.toString()).append("]");
			break;
		case Type.ABSTRACT_CLASS:
			output.append("abstract class [");
			nextIndentationLevel();
			for (Symbol symbol : type.getMembersCollection())
			{
				output.append(System.lineSeparator()).append(currentIndentation.toString());
				symbol.accept(this);
			}
			previousIndentationLevel();
			output.append(System.lineSeparator()).append(currentIndentation.toString()).append("]");
			break;
		}
	}

	public void visit(Scope scope)
	{
		for (Symbol symbol : scope.getSymbols().values())
		{
			symbol.accept(this);
			output.append(System.lineSeparator());
		}
	}

	public String getOutput()
	{
		return output.toString();
	}
}
