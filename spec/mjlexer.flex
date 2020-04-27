package interpreter.lexer;

import interpreter.parser.sym;
import java_cup.runtime.Symbol;

%%

%{
	private Symbol newSymbol(int type)
	{
		return new Symbol(type, yyline + 1, yycolumn);
	}

	private Symbol newSymbol(int type, Object value)
	{
		return new Symbol(type, yyline + 1, yycolumn, value);
	}
%}

%cup
%public
%line
%column

%xstate COMMENT

%eofval{
	return newSymbol(sym.EOF);
%eofval}

%%

" "						{ }
"\b"					{ }
"\t"					{ }
"\r\n"					{ }
"\f"					{ }

"program"				{ return newSymbol(sym.PROGRAM, yytext()); }
"const"					{ return newSymbol(sym.CONST, yytext()); }
"class"					{ return newSymbol(sym.CLASS, yytext()); }
"abstract"				{ return newSymbol(sym.ABSTRACT, yytext()); }
"extends"				{ return newSymbol(sym.EXTENDS, yytext()); }
"if"					{ return newSymbol(sym.IF, yytext()); }
"else"					{ return newSymbol(sym.ELSE, yytext()); }
"new"					{ return newSymbol(sym.NEW, yytext()); }
"read"					{ return newSymbol(sym.READ, yytext()); }
"print"					{ return newSymbol(sym.PRINT, yytext()); }
"for"					{ return newSymbol(sym.FOR, yytext()); }
"break"					{ return newSymbol(sym.BREAK, yytext()); }
"continue"				{ return newSymbol(sym.CONTINUE, yytext()); }
"void"					{ return newSymbol(sym.VOID, yytext()); }
"return"				{ return newSymbol(sym.RETURN, yytext()); }

"+"						{ return newSymbol(sym.PLUS, yytext()); }
"-"						{ return newSymbol(sym.MINUS, yytext()); }
"*"						{ return newSymbol(sym.MUL, yytext()); }
"/"						{ return newSymbol(sym.DIV, yytext()); }
"%"						{ return newSymbol(sym.MOD, yytext()); }
"=="					{ return newSymbol(sym.EQUAL, yytext()); }
"!="					{ return newSymbol(sym.NOT_EQUAL, yytext()); }
">"						{ return newSymbol(sym.GREATER, yytext()); }
">="					{ return newSymbol(sym.GREATER_EQUAL, yytext()); }
"<"						{ return newSymbol(sym.LESS, yytext()); }
"<="					{ return newSymbol(sym.LESS_EQUAL, yytext()); }
"&&"					{ return newSymbol(sym.AND, yytext()); }
"||"					{ return newSymbol(sym.OR, yytext()); }
"="						{ return newSymbol(sym.ASSIGN, yytext()); }
"++"					{ return newSymbol(sym.INCREMENT, yytext()); }
"--"					{ return newSymbol(sym.DECREMENT, yytext()); }
";"						{ return newSymbol(sym.SEMICOLON, yytext()); }
","						{ return newSymbol(sym.COMMA, yytext()); }
"."						{ return newSymbol(sym.DOT, yytext()); }
"("						{ return newSymbol(sym.LEFT_PARENTHESIS, yytext()); }
")"						{ return newSymbol(sym.RIGHT_PARENTHESIS, yytext()); }
"["						{ return newSymbol(sym.LEFT_BRACKET, yytext()); }
"]"						{ return newSymbol(sym.RIGHT_BRACKET, yytext()); }
"{"						{ return newSymbol(sym.LEFT_BRACE, yytext()); }
"}"						{ return newSymbol(sym.RIGHT_BRACE, yytext()); }

"//"					{ yybegin(COMMENT); }
<COMMENT> .				{ yybegin(COMMENT); }
<COMMENT> "\r\n"		{ yybegin(YYINITIAL); }

([0-9]|[1-9][0-9]+)		{ return newSymbol(sym.INTEGER, new Integer(yytext())); }
"'"[\040-\176]"'"		{ return newSymbol(sym.CHARACTER, new Character(yytext().charAt(1))); }
("true"|"false")		{ return newSymbol(sym.BOOLEAN, new Boolean(yytext())); }

([a-zA-Z])[a-zA-Z0-9_]*	{ return newSymbol(sym.IDENTIFIER, yytext()); }

.						{ System.out.println("Lexical error (line " + (yyline + 1) + "): Invalid token \'" + yytext() + "\'"); }