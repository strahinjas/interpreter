package interpreter.symbols;

import java.util.Map;

public class Symbol
{
	public static final int CONSTANT = 0;
	public static final int VARIABLE = 1;
	public static final int TYPE	 = 2;
	public static final int METHOD	 = 3;
	public static final int FIELD	 = 4;
	public static final int ELEMENT	 = 5;
	public static final int PROGRAM	 = 6;

	private final int kind;
	private final String name;

	private Type type;
	private Object value;

	/**
	 * VARIABLE - parameter ordinal
	 * METHOD 	- parameter count
	 */
	private int parameter;

	/**
	 * METHOD   - paramaters & local variables
	 * PROGRAM  - all program-wide defined symbols
	 */
	private Map<String, Symbol> locals;

	public Symbol(int kind, String name, Type type)
	{
		this.kind = kind;
		this.name = name;
		this.type = type;
	}

	public Symbol(int kind, String name, Type type, Object value)
	{
		this.kind = kind;
		this.name = name;
		this.type = type;
		this.value = value;
	}

	public Symbol(int kind, String name, Type type, Object value, int parameter)
	{
		this.kind = kind;
		this.name = name;
		this.type = type;
		this.value = value;
		this.parameter = parameter;
	}

	public int getKind()
	{
		return kind;
	}

	public String getName()
	{
		return name;
	}

	public Type getType()
	{
		return type;
	}

	public Object getValue()
	{
		return value;
	}

	public void setValue(Object value)
	{
		this.value = value;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public int getParameter()
	{
		return parameter;
	}

	public void setParameter(int parameter)
	{
		this.parameter = parameter;
	}

	public Map<String, Symbol> getLocals()
	{
		return locals;
	}

	public void setLocals(Map<String, Symbol> locals)
	{
		this.locals = locals;
	}

	@Override
	public boolean equals(Object object)
	{
		if (super.equals(object)) return true;
		if (!(object instanceof Symbol)) return false;

		Symbol other = (Symbol) object;

		return kind == other.kind &&
			   name.equals(other.name) &&
			   type.equals(other.type) &&
			   locals.equals(other.locals);
	}

	public void accept(SymbolTableVisitor visitor)
	{
		visitor.visit(this);
	}
}
