package com.hdu.common;

import java.util.*;

/**
 * Created by huage on 2017/3/28.
 */
public class JavaTokens {

    public static final byte ID = 0;
    public static final byte DIGIT = 1;
    public static final byte LEFT_SMALL_BRACKET = 2;
    public static final byte LEFT_MID_BRACKET = 3;
    public static final byte PLUS = 4;//加
    public static final byte MIN = 5;//减
    public static final byte PERCENT = 6;//取余
    public static final byte MUL = 7;//乘
    public static final byte DIV = 8;//除
    public static final byte AND = 9;//与
    public static final byte OR = 10;//或
    public static final byte NOT = 11;//非
    public static final byte DOT = 12;//点
    public static final byte EQ = 13;//等于
    public static final byte LT = 14;//小于
    public static final byte LTE = 15;//小等于
    public static final byte GT = 16;//大于
    public static final byte GTE = 17;//大于
    public static final byte UEQ = 18; //不等于
    public static final byte RIGHT_SMALL_BRACKET = 19;
    public static final byte RIGHT_MID_BRACKET = 20;
    public static final byte BREAK = 21;
    public static final byte BYTE = 22;
    public static final byte CASE = 23;
    public static final byte THIS = 24;
    public static final byte CATCH = 25;
    public static final byte CHAR = 26;
    public static final byte STRING = 27;
    public static final byte CONTINUE = 28;
    public static final byte ELSE = 29;
    public static final byte FLOAT = 30;
    public static final byte DOUBLE = 31;
    public static final byte FOR = 32;
    public static final byte LONG = 33;
    public static final byte SHORT = 34;
    public static final byte NEW = 35;
    public static final byte IF = 36;
    public static final byte RETURN = 37;
    public static final byte SWITCH = 38;
    public static final byte WHILE = 39;
    public static final byte BOUND_LEFT = 40;
    public static final byte BOUND_RIGHT = 41;
    public static final byte LEFT_LARGE_BRACKET = 42;
    public static final byte RIGHT_LARGE_BRACKET = 43;
    public static final byte COMMA = 44;
    public static final byte COLON = 45;
    public static final byte TRY = 46;
    public static final byte ASSIGNMENT = 47;
    public static final byte CLASSFLAG = 48;
    public static final byte SYSTEM = 49;
    public static final byte OUT = 50;
    public static final byte SEMICOLON = 51;

    public static final String KEY_WORDs_WITH_PAREN = "for,switch,catch,if,while,try,synchronized";


    public static final Map<String, Byte> map = new HashMap<String, Byte>() {{
        put("break", BREAK);
        put("byte", BYTE);
        put("case", CASE);
        put("catch", CATCH);
        put("char", CHAR);
        put("String", STRING);
        put("continue", CONTINUE);
        put("else", ELSE);
        put("float", FLOAT);
        put("double", DOUBLE);
        put("for", FOR);
        put("byte", BYTE);
        put("long", LONG);
        put("short", SHORT);
        put("new", NEW);
        put("if", IF);
        put("return", RETURN);
        put("switch", SWITCH);
        put("try", TRY);
        put("while", WHILE);

        //方便鉴别
        put("System", SYSTEM);
        put("out", OUT);
        put("this", THIS);
    }};


    public static Set<String> neglectfulKeyword = new HashSet<String>(Arrays.asList(new String[]{
            "synchronized", "transient", "volatile", "private", "protected",
            "public", "static", "final", "throws", "interface", "assert", "void"}));

    public static final String exceptionKeyword = "Exception";
    public static final String classKeyword = "class";


    public static final Map<Byte, String> tokenMap = new HashMap<>();

    static {
        tokenMap.put(LEFT_SMALL_BRACKET, "(");
        tokenMap.put(LEFT_MID_BRACKET, "[");
        tokenMap.put(PLUS, "+");
        tokenMap.put(MIN, "-");
        tokenMap.put(PERCENT, "%");
        tokenMap.put(MUL, "*");
        tokenMap.put(DIV, "/");
        tokenMap.put(AND, "&");
        tokenMap.put(OR, "|");
        tokenMap.put(NOT, "!");
        tokenMap.put(DOT, ".");
        tokenMap.put(EQ, "==");
        tokenMap.put(LT, "<");
        tokenMap.put(LTE, "<=");
        tokenMap.put(GT, ">");
        tokenMap.put(GTE, ">=");
        tokenMap.put(UEQ, "!=");
        tokenMap.put(RIGHT_SMALL_BRACKET, ")");
        tokenMap.put(RIGHT_MID_BRACKET, "]");
        tokenMap.put(BREAK, "break");
        tokenMap.put(BYTE, "byte");
        tokenMap.put(CASE, "case");
        tokenMap.put(THIS, "this");
        tokenMap.put(CATCH, "catch");
        tokenMap.put(CHAR, "char");
        tokenMap.put(STRING, "String");
        tokenMap.put(CONTINUE, "continue");
        tokenMap.put(ELSE, "else");
        tokenMap.put(FLOAT, "float");
        tokenMap.put(DOUBLE, "double");
        tokenMap.put(FOR, "for");
        tokenMap.put(LONG, "long");
        tokenMap.put(SHORT, "short");
        tokenMap.put(NEW, "new");
        tokenMap.put(IF, "if");
        tokenMap.put(RETURN, "return");
        tokenMap.put(SWITCH, "switch");
        tokenMap.put(WHILE, "while");
        tokenMap.put(LEFT_LARGE_BRACKET, "{");
        tokenMap.put(RIGHT_LARGE_BRACKET, "}");
        tokenMap.put(COMMA, ",");
        tokenMap.put(COLON, ":");
        tokenMap.put(TRY, "try");
        tokenMap.put(ASSIGNMENT, "=");
        tokenMap.put(CLASSFLAG, "class");
        tokenMap.put(SYSTEM, "System");
        tokenMap.put(OUT, "out");
        tokenMap.put(SEMICOLON, ";");
    }
}
