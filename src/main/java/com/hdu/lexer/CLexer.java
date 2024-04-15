package com.hdu.lexer;

import com.hdu.bean.Token;
import com.hdu.common.CTokens;
import com.hdu.conf.Config;
import java.io.IOException;

/**
 * author wujun
 */
public class CLexer extends Lexer {

    public CLexer(String inputFileName) throws IOException {
        super(inputFileName);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public Token scan() throws IOException {
        for (; ; getChar()) {
            if (getReaderIsEnd()) {
                break;
            } else if (peek == ' ' || peek == '\t' || peek == '\r') {
            } else if (peek == '\n') {
                line = line + 1;
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
                    line = line + 1;
                } else if (peek == '\n') {
                    line = line + 1;
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
                return new Token(CTokens.LEFT_BRACKET, line);
            case ']':
                getChar();
                return new Token(CTokens.RIGHT_BRACKET, line);
            case '(':
                getChar();
                return new Token(CTokens.LEFT_PAREN, line);
            case ')':
                getChar();
                return new Token(CTokens.RIGHT_PAREN, line);
            case '.':
                getChar();
                return new Token(CTokens.DOT, line);
            case ',':
                getChar();
                return new Token(CTokens.COMMA, line);
            case ':':
                getChar();
                return new Token(CTokens.COLON, line);
            case ';':
                getChar();
                return new Token(CTokens.SEMI, line);
            case '{':
                getChar();
                return new Token(CTokens.LEFT_BRACE, line);
            case '}':
                getChar();
                return new Token(CTokens.RIGHT_BRACE, line);
            case '+':
                getChar();
                if (peek == '+') {
                    getChar();
                    return new Token(CTokens.PLUS_PLUS, line);
                } else if (peek == '=') {
                    getChar();
                    return new Token(CTokens.PLUS_ASSIGN, line);
                } else {
                    return new Token(CTokens.PLUS, line);
                }
            case '-':
                getChar();
                if (peek == '-') {
                    getChar();
                    return new Token(CTokens.MINUS_MINUS, line);
                } else if (peek == '=') {
                    getChar();
                    return new Token(CTokens.MINUS_ASSIGN, line);
                } else if (peek == '>') {
                    getChar();
                    return new Token(CTokens.ARROW, line);
                } else {
                    return new Token(CTokens.MINUS, line);
                }
            case '*':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CTokens.STAR_ASSIGN, line);
                } else {
                    return new Token(CTokens.STAR, line);
                }
            case '/':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CTokens.DIV_ASSIGN, line);
                } else {
                    return new Token(CTokens.DIV, line);
                }
            case '%':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CTokens.MOD_ASSIGN, line);
                } else {
                    return new Token(CTokens.MOD, line);
                }
            case '=':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CTokens.EQUAL, line);
                } else {
                    return new Token(CTokens.ASSIGN, line);
                }
            case '>':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CTokens.GREATER_EQUAL, line);
                } else if (peek == '>') {
                    getChar();
                    return new Token(CTokens.RIGHT_SHIFT, line);
                } else {
                    return new Token(CTokens.GREATER, line);
                }
            case '<':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CTokens.LESS_EQUAL, line);
                } else if (peek == '<') {
                    getChar();
                    return new Token(CTokens.LEFT_SHIFT, line);
                } else {
                    return new Token(CTokens.LESS, line);
                }
            case '!':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CTokens.NOT_EQUAL, line);
                } else {
                    return new Token(CTokens.NOT, line);
                }
            case '&':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CTokens.AND_ASSIGN, line);
                } else if (peek == '&') {
                    getChar();
                    return new Token(CTokens.AND_AND, line);
                } else {
                    return new Token(CTokens.AND, line);
                }
            case '|':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CTokens.OR_ASSIGN, line);
                } else if (peek == '|') {
                    getChar();
                    return new Token(CTokens.OR_OR, line);
                } else {
                    return new Token(CTokens.OR, line);
                }
            case '^':
                getChar();
                return new Token(CTokens.CARET, line);
            case '~':
                getChar();
                return new Token(CTokens.TILDE, line);
        }

        // 处理字符串
        if (peek == '\"' && lastPeek != '\'') {
            StringBuilder builder = new StringBuilder();
            builder.append(peek);
            do {
                if (peek == '\\') {
                    getChar();
                    builder.append(peek);
                    if (peek == '\n'){
                        line++;
                        indexOfChar = 0;
                    }
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
            return new Token(CTokens.IDENTIFIER, line);
        }

        // 处理数字
        if (Character.isDigit(peek)) {
            int value = 0;
            do {
                value = 10 * value + Character.digit(peek, 10);
                getChar();
            } while (Character.isDigit(peek));
            return new Token(CTokens.DIGIT, line);
        }

        // 处理标识符
        if (Character.isLetter(peek)) {
            StringBuilder buffer = new StringBuilder();
            do {
                buffer.append(peek);
                getChar();
            } while (Character.isLetterOrDigit(peek) || peek == '_');
            String s = buffer.toString();

            if (s.equals("include")) {
                reader.readLine();
                getChar();
                indexOfChar = 0;
                line = line + 1;
                return new Token((byte)-1, line);
            } else {
                Byte token = CTokens.tokenMap.get(s);
                if (token != null) {
                    return new Token(token, line);
                }
                if (Config.OpenStringHash == 1){
                    return new Token(str2hash(s), line);
                }
                return new Token(CTokens.IDENTIFIER, line);
            }
        }
        if ((int) peek != 0xffff) {
            getChar();
            return new Token((byte)-1, line);
        }
        return new Token((byte)-2, line);
    }
}
