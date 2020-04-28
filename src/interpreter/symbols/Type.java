package interpreter.symbols;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class Type
{
	public static final int NONE  = 0;
	public static final int INT   = 1;
	public static final int CHAR  = 2;
	public static final int BOOL  = 3;
	public static final int ARRAY = 4;
	public static final int CLASS = 5;
	public static final int ABSTRACT_CLASS = 6;

	private final int kind;

	/**
	 * ARRAY - element type
	 * CLASS - parent class type
	 * ABSTRACT CLASS - parent class type
	 */
	private Type parentType;

	/**
	 * CLASS - fields & methods
	 * ABSTRACT CLASS - fields & methods
	 */
	private Map<String, Symbol> members;

	public Type(int kind)
	{
		this.kind = kind;
	}

	public Type(int kind, Type parentType)
	{
		this.kind = kind;

		if (kind == ARRAY ||
			kind == CLASS ||
			kind == ABSTRACT_CLASS)
		{
			this.parentType = parentType;
		}
	}

	public int getKind()
	{
		return kind;
	}

	public Type getParentType()
	{
		return parentType;
	}

	public void setParentType(Type parentType)
	{
		this.parentType = parentType;
	}

	public Map<String, Symbol> getMembers()
	{
		return members;
	}

	public Collection<Symbol> getMembersCollection()
	{
		return members == null ? Collections.emptyList() : members.values();
	}

	public void setMembers(Map<String, Symbol> members)
	{
		this.members = members;
	}

	@Override
	public boolean equals(Object object)
	{
		if (super.equals(object)) return true;
		if (!(object instanceof Type)) return false;

		Type other = (Type) object;

		if (kind == ARRAY)
		{
			return other.kind == ARRAY && parentType.equals(other.parentType);
		}

		if (kind == CLASS || kind == ABSTRACT_CLASS)
		{
			return kind == other.kind && members.equals(other.members);
		}

		return false;
	}

	public boolean isReferenceType()
	{
		return kind == ARRAY || kind == CLASS || kind == ABSTRACT_CLASS;
	}

	public boolean compatibleWith(Type other)
	{
		return this.equals(other) ||
			   (this == SymbolTable.NULL_TYPE && other.isReferenceType()) ||
			   (this.isReferenceType() && other == SymbolTable.NULL_TYPE);
	}

	public boolean assignableTo(Type destination)
	{
		return this.equals(destination) ||
			   (this == SymbolTable.NULL_TYPE && destination.isReferenceType()) ||
			   (this.kind == ARRAY && destination.kind == ARRAY && destination.parentType == SymbolTable.NO_TYPE);
	}

	public void accept(SymbolTableVisitor visitor)
	{
		visitor.visit(this);
	}
}
