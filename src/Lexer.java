import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Lexer {

	private final String letters = "abcdefghijklmnopqrstuvwxyz" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private final String digits = "0123456789";
	private final char eolnCh = '\n';
	private final char eofCh = '\004';
	private boolean isEof = false;
	private char ch = ' ';
	private BufferedReader input;
	private String line = "";
	private int lineno = 0;
	private int col = 1;

	public Lexer(String fileName) { // source filename
		try {
			input = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + fileName);
			System.exit(1);
		}
	}

	static public void main(String[] argv) {
		Lexer lexer = new Lexer(argv[0]);
		Token tok = lexer.next();
		while (tok != Token.eofTok) {
			System.out.println(tok.toString());
			tok = lexer.next();
		}
	} // main

	private char nextChar() { // Return next char
		if (ch == eofCh)
			error("Attempt to read past end of file");
		col++;
		if (col >= line.length()) {
			try {
				line = input.readLine();
			} catch (IOException e) {
				System.err.println(e);
				System.exit(1);
			} // try
			if (line == null) // at end of file
				line = "" + eofCh;
			else {
				// System.out.println("### Line " + lineno + ":\t" + line);
				lineno++;
				line += eolnCh;
			} // if line
			col = 0;
		} // if col
		return line.charAt(col);
	}

	public Token next() { // Return next token
		do {
			if (isLetter(ch)) { // ident or keyword
				String spelling = concat(letters + digits);
				return Token.keyword(spelling);
			} else if (isDigit(ch)) { // int or float literal
				String number = concat(digits);
				if (ch != '.') // int Literal
					return Token.mkIntLiteral(number);
				number += concat(digits);
				return Token.mkFloatLiteral(number);
			} else
				switch (ch) {
					case ' ':
					case '\t':
					case '\r':
					case eolnCh:
						ch = nextChar();
						break;

					case '/': // divide or comment
						ch = nextChar();
						if (ch != '/')
							return Token.divideTok;
						// comment
						do {
							ch = nextChar();
						} while (ch != eolnCh);
						ch = nextChar();
						break;

					case '\'': // char literal
						char ch1 = nextChar();
						nextChar(); // get '
						ch = nextChar();
						return Token.mkCharLiteral("" + ch1);

					case eofCh:
						return Token.eofTok;

					case '+':
						ch = nextChar();
						return Token.plusTok;

					// - * ( ) { } ; , student exercise
					case '-':
						// -- (감소) 연산자는 없으므로 무조건 뺄셈임
						ch = nextChar();
						return Token.minusTok;
					case '*':
						ch = nextChar();
						return Token.multiplyTok;
					case '(':
						ch = nextChar();
						return Token.leftParenTok;
					case ')':
						ch = nextChar();
						return Token.rightParenTok;
					case '{':
						ch = nextChar();
						return Token.leftBraceTok;
					case '}':
						ch = nextChar();
						return Token.rightBraceTok;
					case ';':
						ch = nextChar();
						return Token.semicolonTok;
					case ',':
						ch = nextChar();
						return Token.commaTok;

					case '&':
						check('&');
						return Token.andTok;
					case '|':
						check('|');
						return Token.orTok;

					case '=':
						return chkOpt('=', Token.assignTok, Token.eqeqTok);
					// < > ! student exercise
					case '<':
						// < 와 <= 구분 필요
						return chkOpt('=', Token.ltTok, Token.lteqTok);
					case '>':
						// > 와 >= 구분 필요
						return chkOpt('=', Token.gtTok, Token.gteqTok);
					case '!':
						// ! 와 != 구분 필요
						return chkOpt('=', Token.notTok, Token.noteqTok);

					default:
						error("Illegal character " + ch);
				} // switch
		} while (true);
	} // next

	private boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
	}

	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private void check(char c) {
		ch = nextChar();
		if (ch != c)
			error("Illegal character, expecting " + c);
		ch = nextChar();
	}

	private Token chkOpt(char c, Token one, Token two) {
		// 문자 c가 다음으로 오는지 확인해서 틀리면 one, 맞으면 two 반환
		ch = nextChar();
		if (c == ch) {
			ch = nextChar(); // 다음 문자를 읽어주고 리턴
			return two;
		} else {
			// c가 다음으로 오지 않음
			return one;
		}
	}

	private String concat(String set) {
		String r = "";
		do {
			r += ch;
			ch = nextChar();
		} while (set.indexOf(ch) >= 0);
		return r;
	}

	public void error(String msg) {
		System.err.print(line);
		System.err.println("Error: column " + col + " " + msg);
		System.exit(1);
	}

}
