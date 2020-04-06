package interpreter;

import interpreter.ast.Program;
import interpreter.ast.SyntaxNode;
import interpreter.parser.Parser;
import interpreter.parser.Yylex;
import interpreter.symbols.SymbolTable;
import java_cup.runtime.Symbol;

import java.io.*;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		if (args.length != 1)
		{
			System.err.println("Wrong number of arguments!");
			System.err.println("Program should be called with exactly one argument: input_file(.mj)");
			return;
		}

		Reader reader = null;
		try
		{
			File sourceFile = new File(args[0]);

			if (!sourceFile.exists() ||
				!sourceFile.getName().endsWith(".mj"))
			{
				System.err.println("Invalid MicroJava source code provided");
				return;
			}

			System.out.println("Interpreting source file: " + sourceFile.getAbsolutePath());

			reader = new BufferedReader(new FileReader(sourceFile));
			Yylex lexer = new Yylex(reader);
			Parser parser = new Parser(lexer);

			System.out.println("========================= Syntax Analysis ===========================");

			Symbol symbol = parser.parse();
			SyntaxNode root = (SyntaxNode) symbol.value;

			if (!(root instanceof Program))
			{
				System.err.println("Syntax error! Interpreting cannot continue!");
				return;
			}

			Program program = (Program) root;

			System.out.println(program.toString(""));

			if (!parser.isSyntacticallyCorrect())
			{
				System.err.println("Syntax error! Interpreting cannot continue!");
				return;
			}

			System.out.println("========================= Semantic Analysis =========================");

			SymbolTable symbolTable = new SymbolTable();
			SemanticAnalyzer analyzer = new SemanticAnalyzer(symbolTable);

			analyzer.analyze(program);

			symbolTable.dump();

			if (analyzer.isSemanticallyCorrect())
			{
				System.out.println("=========================== Interpretation ==========================");
				System.out.println("Interpreting finished successfully!");
			}
			else
			{
				System.err.println("Semantic error! Interpreting cannot continue!");
			}
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					System.err.println(e.getMessage());
				}
			}
		}
	}
}
