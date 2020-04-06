package interpreter.symbols;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable
{
	public static final Type NO_TYPE   = new Type(Type.NONE);
	public static final Type INT_TYPE  = new Type(Type.INT);
	public static final Type CHAR_TYPE = new Type(Type.CHAR);
	public static final Type BOOL_TYPE = new Type(Type.BOOL);
	public static final Type NULL_TYPE = new Type(Type.CLASS);

	public static final Symbol NO_SYMBOL = new Symbol(Symbol.VARIABLE, "noSymbol", NO_TYPE);

	public static final	String ENTRY_POINT = "main";

	private final Enviroment universe = new Enviroment(null);
	private Enviroment enviroment = universe;

	private final Map<Type, Type> arrayTypes = new LinkedHashMap<>();
	private final Map<String, Type> classTypes = new LinkedHashMap<>();
	private final Map<String, Type> abstractClassTypes = new LinkedHashMap<>();

	public SymbolTable()
	{
		enviroment.addSymbol(new Symbol(Symbol.TYPE, "int", INT_TYPE));
		enviroment.addSymbol(new Symbol(Symbol.TYPE, "char", CHAR_TYPE));
		enviroment.addSymbol(new Symbol(Symbol.TYPE, "bool", BOOL_TYPE));
		enviroment.addSymbol(new Symbol(Symbol.CONSTANT, "null", NULL_TYPE, 0));
		enviroment.addSymbol(new Symbol(Symbol.CONSTANT, "eol", CHAR_TYPE, System.lineSeparator()));

		Symbol method = new Symbol(Symbol.METHOD, "chr", CHAR_TYPE, null, 1);
		enviroment.addSymbol(method);
		{
			openScope();
			enviroment.addSymbol(new Symbol(Symbol.VARIABLE, "integer", INT_TYPE, null, 1));
			method.setLocals(enviroment.getSymbols());
			closeScope();
		}

		method = new Symbol(Symbol.METHOD, "ord", INT_TYPE);
		enviroment.addSymbol(method);
		{
			openScope();
			enviroment.addSymbol(new Symbol(Symbol.VARIABLE, "character", CHAR_TYPE, null, 1));
			method.setLocals(enviroment.getSymbols());
			closeScope();
		}

		method = new Symbol(Symbol.METHOD, "len", INT_TYPE);
		enviroment.addSymbol(method);
		{
			openScope();
			enviroment.addSymbol(new Symbol(Symbol.VARIABLE, "array", new Type(Type.ARRAY, NO_TYPE), null, 1));
			method.setLocals(enviroment.getSymbols());
			closeScope();
		}
	}

	public void openScope()
	{
		enviroment = new Enviroment(enviroment);
	}

	public void closeScope()
	{
		enviroment = enviroment.getOuter();
	}

	public void chainSymbols(Symbol symbol)
	{
		symbol.setLocals(enviroment.getSymbols());
	}

	public void chainSymbols(Type type)
	{
		type.setMembers(enviroment.getSymbols());
	}

	public boolean insert(Symbol symbol)
	{
		return enviroment.addSymbol(symbol);
	}

	public Symbol insert(int kind, String name, Type type)
	{
		Symbol symbol = new Symbol(kind, name, type);

		if (enviroment.addSymbol(symbol))
		{
			return symbol;
		}
		else
		{
			symbol = enviroment.findSymbol(name);
			return symbol == null ? NO_SYMBOL : symbol;
		}
	}

	public Symbol find(String name)
	{
		for (Enviroment e = enviroment; e != null; e = e.getOuter())
		{
			Symbol symbol = e.findSymbol(name);
			if (symbol != null) return symbol;
		}

		return NO_SYMBOL;
	}

	public Symbol findInCurrentScope(String name)
	{
		Symbol symbol = enviroment.findSymbol(name);
		return symbol == null ? NO_SYMBOL : symbol;
	}

	public void remove(String name)
	{
		enviroment.removeSymbol(name);
	}

	public Type getArrayType(Type elementType)
	{
		if (arrayTypes.containsKey(elementType))
		{
			return arrayTypes.get(elementType);
		}
		else
		{
			Type type = new Type(Type.ARRAY, elementType);
			arrayTypes.put(elementType, type);
			return type;
		}
	}

	public Type getClassType(String className)
	{
		if (classTypes.containsKey(className))
		{
			return classTypes.get(className);
		}
		else
		{
			Type type = new Type(Type.CLASS);
			classTypes.put(className, type);
			return type;
		}
	}

	public Type getAbstractClassType(String abstractClassName)
	{
		if (abstractClassTypes.containsKey(abstractClassName))
		{
			return abstractClassTypes.get(abstractClassName);
		}
		else
		{
			Type type = new Type(Type.ABSTRACT_CLASS);
			abstractClassTypes.put(abstractClassName, type);
			return type;
		}
	}

	public Type getExtendingType(Type type)
	{
		if (type.getKind() == Type.CLASS ||
			type.getKind() == Type.ABSTRACT_CLASS)
		{
			Type parentType = type.getParentType();
			return parentType == null ? NO_TYPE : parentType;
		}

		return NO_TYPE;
	}

	public String getTypeName(Type type)
	{
		switch (type.getKind())
		{
		case Type.INT:
			return "int";
		case Type.CHAR:
			return "char";
		case Type.BOOL:
			return "bool";
		case Type.ARRAY:
			for (Map.Entry<Type, Type> entry : arrayTypes.entrySet())
			{
				if (entry.getValue() == type)
					return "array of " + getTypeName(entry.getKey());
			}
			return "array of notype";
		case Type.CLASS:
			for (Map.Entry<String, Type> entry : classTypes.entrySet())
			{
				if (entry.getValue() == type)
					return "class " + entry.getKey();
			}
			return "class";
		case Type.ABSTRACT_CLASS:
			for (Map.Entry<String, Type> entry : abstractClassTypes.entrySet())
			{
				if (entry.getValue() == type)
					return "abstract class " + entry.getKey();
			}
			return "abstract class";
		default:
			return "notype";
		}
	}

	public Symbol getFormalParameter(Symbol method, int index)
	{
		for (Symbol symbol : method.getLocals().values())
		{
			if (symbol.getParameter() == index)
				return symbol;
		}
		return NO_SYMBOL;
	}

	public void dump()
	{
		System.out.println("=========================== Symbol Table ============================");

		SymbolTableVisitor visitor = new SymbolTableVisitor(this);

		for (Enviroment e = enviroment; e != null; e = e.getOuter()) e.accept(visitor);

		System.out.println(System.lineSeparator() + visitor.getOutput());
	}
}
