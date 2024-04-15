package com.hdu.lexer;


import com.hdu.bean.Token;
import com.hdu.common.JavaTokens;
import com.hdu.conf.Config;
import java.io.IOException;

public class JavaLexer extends Lexer {

    public JavaLexer(String inputFileName) throws IOException {
        super(inputFileName);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public Token scan() throws IOException {
        for (; ; getChar()) {
            if (getReaderIsEnd()) {
                break;
            } else if (peek == ' ' || peek == '\t')
                continue;
            else if (peek == '\n' && lastPeek != '\'') {
                line = line + 1;
                indexOfChar = 0;
                continue;
            } else if (lastPeek == '\r' && peek == '\r') {
                line = line + 1;
                indexOfChar = 0;
                continue;
            }else if (peek == '/') {
                getChar();
                if (peek == '*') {
                    for (; ; getChar()) {
                        if (peek == '\n'){
                            line++;
                            indexOfChar = 0;
                        }
                        if (lastPeek == '\r' && peek == '\r'){
                            line++;
                            indexOfChar = 0;
                        }
                        if (lastPeek == '*' && peek == '/') {
                            break;
                        }
                        if (getReaderIsEnd()) {
                            break;
                        }
                    }
                } else if (peek == '/') {
                    reader.readLine();
                    indexOfChar = 0;
                    line++;
                } else if (peek == '\n') {
                    line++;
                    indexOfChar = 0;
                }
            } else
                break;
        }
        wordIndexOfChar = indexOfChar;

        switch (peek) {
            case '\\':
                getChar();
                getChar();
                return new Token((byte)-1, line);
            case '[':
                getChar();
                return new Token(JavaTokens.LEFT_MID_BRACKET, line);
            case ']':
                getChar();
                return new Token(JavaTokens.RIGHT_MID_BRACKET, line);
            case '(':
                getChar();
                return new Token(JavaTokens.LEFT_SMALL_BRACKET, line);
            case ')':
                getChar();
                return new Token(JavaTokens.RIGHT_SMALL_BRACKET, line);
            case '.':
                getChar();
                return new Token(JavaTokens.DOT, line);
            case ',':
                getChar();
                return new Token(JavaTokens.COMMA, line);
            case ':':
                getChar();
                return new Token(JavaTokens.COLON, line);
            case ';':
                getChar();
                return new Token(JavaTokens.SEMICOLON, line);
            case '{':
                getChar();
                return new Token(JavaTokens.LEFT_LARGE_BRACKET, line);
            case '}':
                getChar();
                return new Token(JavaTokens.RIGHT_LARGE_BRACKET, line);
            case '+':
                getChar();
                return new Token(JavaTokens.PLUS, line);
            case '-':
                getChar();
                return new Token(JavaTokens.MIN, line);
            case '*':
                getChar();
                return new Token(JavaTokens.MUL, line);
            case '/':
                getChar();
                return new Token(JavaTokens.DIV, line);
            case '%':
                getChar();
                return new Token(JavaTokens.PERCENT, line);
            case '=':
                if (getChar('=')) {
                    return new Token(JavaTokens.EQ, line);
                } else {
                    return new Token(JavaTokens.ASSIGNMENT, line);
                }
            case '>':
                if (getChar('=')) {
                    return new Token(JavaTokens.GTE, line);
                } else {
                    return new Token(JavaTokens.GT, line);
                }
            case '<':
                if (getChar('=')) {
                    return new Token(JavaTokens.LT, line);
                } else {
                    return new Token(JavaTokens.LTE, line);
                }
            case '!':
                if (getChar('=')) {
                    return new Token(JavaTokens.UEQ, line);
                } else {
                    return new Token(JavaTokens.NOT, line);
                }
            case '&':
                getChar();
                return new Token(JavaTokens.AND, line);
            case '|':
                getChar();
                return new Token(JavaTokens.OR, line);
        }

        // 处理字符串
        if (peek == '\"' && lastPeek != '\'') {
            StringBuilder sb = new StringBuilder();
            sb.append(peek);
            do {
                if (peek == '\\') {
                    getChar();
                    sb.append(peek);
                    getChar();
                } else {
                    getChar();
                }
                sb.append(peek);
            } while (peek != '\"' && !getReaderIsEnd());
            getChar();
            if (Config.OpenStringHash == 1){
                return new Token(str2hash(sb.toString()), line);
            }
            return new Token(JavaTokens.ID, line);
        }

        if (peek == '\'') {
            StringBuilder sb = new StringBuilder();
            sb.append(peek);
            do {
                if (peek == '\\') {
                    getChar();
                    sb.append(peek);
                    getChar();
                } else {
                    getChar();
                }
                sb.append(peek);
            } while (peek != '\'' && !getReaderIsEnd());
            getChar();
            if (Config.OpenStringHash == 1){
                return new Token(str2hash(sb.toString()), line);
            }
            return new Token(JavaTokens.ID, line);
        }

        if (Character.isDigit(peek)) {
            int value = 0;
            do {
                value = 10 * value + Character.digit(peek, 10);
                getChar();
            } while (Character.isDigit(peek));
            return new Token(JavaTokens.DIGIT, line);
        }

        lastIdentifier = lastPeek;
        if (Character.isLetter(peek)) {
            StringBuilder sb = new StringBuilder();

            /* 首先得到整个的一个分割 */
            do {
                sb.append(peek);
                getChar();
            } while (Character.isLetterOrDigit(peek) || peek == '_' || peek == '-');
            String s = sb.toString();

            //直接跳行 加快速度。
            if (s.equals("import") || s.equals("package")) {
                reader.readLine();
                indexOfChar = 0;
                line++;
                return new Token((byte)-1, line);
            } else {
                if (s.equals(JavaTokens.classKeyword)) {
                    return new Token(JavaTokens.CLASSFLAG, line);
                } else if (s.endsWith(JavaTokens.exceptionKeyword)) {
                    return new Token((byte)-1, line);
                }

                if (JavaTokens.neglectfulKeyword.contains(s)) {
                    return new Token((byte)-1, line);
                }

                Byte isKeyWord = JavaTokens.map.get(s);
                if (isKeyWord != null) {
                    return new Token(isKeyWord, line);
                }
                if (Config.OpenStringHash == 1){
                    return new Token(str2hash(s), line);
                }
                return new Token(JavaTokens.ID, line);
            }
        }

        if ((int) peek != 0xffff) {
            getChar();
            return new Token((byte)-1, line);
        }
        return new Token((byte)-2, line);
    }
}
