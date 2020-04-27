package interpreter.ir;

public abstract class Expression
{
	public interface Visitor<R>
	{
		R visit(Binary expression);
		R visit(Group expression);
		R visit(Literal expression);
		R visit(Logical expression);
		R visit(Unary expression);
		R visit(Variable expression);
	}

	public int line;

	public Expression(int line)
	{
		this.line = line;
	}

	public abstract <R> R accept(Visitor<R> visitor);

	public static class Binary extends Expression
	{
		public enum Operation
		{
			ADDITION,
			SUBTRACTION,
			MULTIPLICATION,
			DIVISION,
			MODULUS,
			EQUAL,
			NOT_EQUAL,
			GREATER,
			GREATER_EQUAL,
			LESS,
			LESS_EQUAL
		}

		public final Expression left;
		public final Binary.Operation operation;
		public final Expression right;

		public Binary(int line, Expression left, Binary.Operation operation, Expression right)
		{
			super(line);
			this.left = left;
			this.operation = operation;
			this.right = right;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Group extends Expression
	{
		public final Expression expression;

		public Group(int line, Expression expression)
		{
			super(line);
			this.expression = expression;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Literal extends Expression
	{
		public final Object value;

		public Literal(int line, Object value)
		{
			super(line);
			this.value = value;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Logical extends Expression
	{
		public enum Operation { AND, OR }

		public final Expression left;
		public final Logical.Operation operation;
		public final Expression right;

		public Logical(int line, Expression left, Logical.Operation operation, Expression right)
		{
			super(line);
			this.left = left;
			this.operation = operation;
			this.right = right;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Unary extends Expression
	{
		public enum Operation { NEGATION }

		public final Unary.Operation operation;
		public final Expression right;

		public Unary(int line, Unary.Operation operation, Expression right)
		{
			super(line);
			this.operation = operation;
			this.right = right;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Variable extends Expression
	{
		public final String name;

		public Variable(int line, String name)
		{
			super(line);
			this.name = name;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}
}
