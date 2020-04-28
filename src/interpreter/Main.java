package interpreter;

import interpreter.ast.Program;
import interpreter.ast.SyntaxNode;
import interpreter.lexer.Yylex;
import interpreter.parser.Parser;
import interpreter.symbols.SymbolTable;
import java_cup.runtime.Symbol;

import java.io.*;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		if (args.length < 2 || args.length > 3)
		{
			System.err.println("Wrong number of arguments!");
			System.err.println("Program should be called with two or three arguments: input_file(.mj) to_interpret [output_file(.ir)]");
			return;
		}

		String inputFileName = args[0];

		if (!inputFileName.endsWith(".mj"))
		{
			System.err.println("Invalid MicroJava source code provided");
			System.err.println("Input file should have (.mj) extension");
			return;
		}

		boolean toInterpret = Boolean.parseBoolean(args[1]);

		String outputFileName;

		if (toInterpret)
		{
			if (args.length == 2)
			{
				outputFileName = inputFileName.replace(".mj", ".ir");
			}
			else
			{
				outputFileName = args[2];
			}

			if (!outputFileName.endsWith(".ir"))
			{
				System.err.println("Invalid output file provided");
				System.err.println("Output file should have (.ir) extension");
				return;
			}
		}

		Reader reader = null;
		try
		{
			File sourceFile = new File(inputFileName);

			if (!sourceFile.exists())
			{
				System.err.println("Provided input file could not be found");
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
				System.out.println("================== Intermediate Code Generation =====================");

				IntermediateCodeGenerator generator = new IntermediateCodeGenerator(symbolTable);

				generator.generate(program);

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
