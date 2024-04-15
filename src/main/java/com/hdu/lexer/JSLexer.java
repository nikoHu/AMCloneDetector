package com.hdu.lexer;

import com.hdu.bean.Token;
import com.hdu.common.JSTokens;
import com.hdu.conf.Config;

import java.io.IOException;

@SuppressWarnings("Duplicates")
public class JSLexer extends Lexer{

    public JSLexer(String inputFileName) throws IOException {
        super(inputFileName);
    }

    @Override
    public Token scan() throws IOException {
        for (; ; getChar()) {
            if (getReaderIsEnd()) {
                break;
            } else if (peek == ' ' || peek == '\t' || peek == '\r') {
            } else if (peek == '\n') {
                line++;
                indexOfChar = 0;
            } else if (peek == '/') {
                getChar();
                if (peek == '*') {
                    for (; ; getChar()) {
                        if (peek == '\n'){
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
                if (peek == '\n') {
                    line++;
                    indexOfChar = 0;
                }
                getChar();
                return new Token((byte)-1, line);
            case '[':
                getChar();
                return new Token(JSTokens.LEFT_MID_BRACKET, line);
            case ']':
                getChar();
                return new Token(JSTokens.RIGHT_MID_BRACKET, line);
            case '(':
                getChar();
                return new Token(JSTokens.LEFT_SMALL_BRACKET, line);
            case ')':
                getChar();
                return new Token(JSTokens.RIGHT_SMALL_BRACKET, line);
            case '.':
                getChar();
                return new Token(JSTokens.DOT, line);
            case ',':
                getChar();
                return new Token(JSTokens.COMMA, line);
            case ':':
                getChar();
                return new Token(JSTokens.COLON, line);
            case ';':
                getChar();
                return new Token(JSTokens.SEMI, line);
            case '{':
                getChar();
                return new Token(JSTokens.LEFT_LARGE_BRACKET, line);
            case '}':
                getChar();
                return new Token(JSTokens.RIGHT_LARGE_BRACKET, line);
            case '+':
                getChar();
                if (peek == '+') {
                    getChar();
                    return new Token(JSTokens.PLUS_PLUS, line);
                } else if (peek == '=') {
                    getChar();
                    return new Token(JSTokens.PLUS_ASSIGN, line);
                } else {
                    return new Token(JSTokens.PLUS, line);
                }
            case '-':
                getChar();
                if (peek == '-') {
                    getChar();
                    return new Token(JSTokens.MINUS_MINUS, line);
                } else if (peek == '=') {
                    getChar();
                    return new Token(JSTokens.MINUS_ASSIGN, line);
                } else {
                    return new Token(JSTokens.MINUS, line);
                }
            case '*':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(JSTokens.STAR_ASSIGN, line);
                } else {
                    return new Token(JSTokens.STAR, line);
                }
            case '/':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(JSTokens.DIV_ASSIGN, line);
                } else {
                    return new Token(JSTokens.DIV, line);
                }
            case '%':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(JSTokens.MOD_ASSIGN, line);
                } else {
                    return new Token(JSTokens.MOD, line);
                }
            case '=':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(JSTokens.EQUAL, line);
                } else {
                    return new Token(JSTokens.ASSIGN, line);
                }
            case '>':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(JSTokens.GREATER_EQUAL, line);
                }  else {
                    return new Token(JSTokens.GREATER, line);
                }
            case '<':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(JSTokens.LESS_EQUAL, line);
                } else {
                    return new Token(JSTokens.LESS, line);
                }
            case '!':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(JSTokens.NOT_EQUAL, line);
                } else {
                    return new Token(JSTokens.NOT, line);
                }
            case '&':
                getChar();
                if (peek == '&') {
                    getChar();
                    return new Token(JSTokens.AND, line);
                } else {
                    return new Token(JSTokens.AND, line);
                }
            case '|':
                getChar();
                if (peek == '|') {
                    getChar();
                    return new Token(JSTokens.OR, line);
                } else {
                    return new Token(JSTokens.OR, line);
                }
        }

        // 处理字符串
        if (peek == '\"' && lastPeek != '\'') {
            StringBuilder builder = new StringBuilder();
            builder.append(peek);
            do {
                if (peek == '\\') {
                    getChar();
                    builder.append(peek);
                    getChar();
                } else {
                    getChar();
                }
                builder.append(peek);
            } while (peek != '\"' && !getReaderIsEnd());
            getChar();
            if (Config.OpenStringHash == 1){
                return new Token(str2hash(builder.toString()), line);
            }
            return new Token(JSTokens.ID, line);
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
            return new Token(JSTokens.ID, line);
        }

        // 处理数字
        if (Character.isDigit(peek)) {
            int value = 0;
            do {
                value = 10 * value + Character.digit(peek, 10);
                getChar();
            } while (Character.isDigit(peek));
            return new Token(JSTokens.DIGIT, line);
        }

        // 处理标识符
        if (Character.isLetter(peek)) {
            StringBuilder buffer = new StringBuilder();
            do {
                buffer.append(peek);
                getChar();
            } while (Character.isLetterOrDigit(peek) || peek == '_');
            String s = buffer.toString();

            if (s.equals("import")) {
                reader.readLine();
                getChar();
                indexOfChar = 0;
                line = line + 1;
                return new Token((byte)-1, line);
            } else {
                Byte token = JSTokens.tokenMap.get(s);
                if (token != null) {
                    return new Token(token, line);
                }
                if (Config.OpenStringHash == 1){
                    return new Token(str2hash(s), line);
                }
                return new Token(JSTokens.ID, line);
            }
        }
        if ((int) peek != 0xffff) {
            getChar();
            return new Token((byte)-1, line);
        }
        return new Token((byte)-2, line);
    }
}
