package net.swordie.ms.client.character.skills.info;

import java.util.ArrayList;
import java.util.List;

public final class SkillFormulaEvaluator {

    private SkillFormulaEvaluator() {
    }

    public static int eval(String expr, int x) {
        if (expr == null || expr.isEmpty()) {
            return 0;
        }
        String normalized = preprocess(expr);
        Parser p = new Parser(normalized, x);
        double val = p.parseExpression();
        return (int) val; // giữ behaviour cũ: cast về int
    }

    private static String preprocess(String v) {
        // bỏ xuống dòng, escape
        v = v.replace("\n", "").replace("\r", "");
        v = v.replace("\\n", "").replace("\\r", "");
        // bỏ % nếu có
        v = v.replace("%", "");
        v = v.replace("０", "0");
        // nếu có mấy token rác kiểu "slashStorm2" mà bạn không dùng, có thể replace ở đây
        // v = v.replace("slashStorm2", "");
        return v;
    }

    // ======================= TOKEN & LEXER =======================

    private enum TokenType {
        NUMBER,     // 123
        IDENT,      // x, log20, min, max, d, u ...
        PLUS, MINUS, MUL, DIV,
        LPAREN, RPAREN,
        COMMA,
        EOF
    }

    private static final class Token {
        final TokenType type;
        final String text;

        Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    private static final class Lexer {
        private final String s;
        private int pos;
        private final int len;

        Lexer(String s) {
            this.s = s;
            this.len = s.length();
        }

        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<>();
            while (true) {
                skipWS();
                if (pos >= len) {
                    tokens.add(new Token(TokenType.EOF, ""));
                    break;
                }
                char c = s.charAt(pos);
                if (Character.isDigit(c)) {
                    tokens.add(readNumber());
                } else if (Character.isLetter(c)) {
                    tokens.add(readIdent());
                } else {
                    switch (c) {
                        case '+':
                            tokens.add(new Token(TokenType.PLUS, "+"));
                            pos++;
                            break;
                        case '-':
                            tokens.add(new Token(TokenType.MINUS, "-"));
                            pos++;
                            break;
                        case '*':
                            tokens.add(new Token(TokenType.MUL, "*"));
                            pos++;
                            break;
                        case '/':
                            tokens.add(new Token(TokenType.DIV, "/"));
                            pos++;
                            break;
                        case '(':
                            tokens.add(new Token(TokenType.LPAREN, "("));
                            pos++;
                            break;
                        case ')':
                            tokens.add(new Token(TokenType.RPAREN, ")"));
                            pos++;
                            break;
                        case ',':
                            tokens.add(new Token(TokenType.COMMA, ","));
                            pos++;
                            break;
                        default:
                            // ký tự lạ -> bỏ qua
                            pos++;
                            break;
                    }
                }
            }
            return tokens;
        }

        private void skipWS() {
            while (pos < len) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private Token readNumber() {
            int start = pos;
            while (pos < len && Character.isDigit(s.charAt(pos))) {
                pos++;
            }
            return new Token(TokenType.NUMBER, s.substring(start, pos));
        }

        private Token readIdent() {
            int start = pos;
            while (pos < len && Character.isLetterOrDigit(s.charAt(pos))) {
                pos++;
            }
            return new Token(TokenType.IDENT, s.substring(start, pos));
        }
    }

    // ======================= PARSER =======================

    private static final class Parser {
        private final List<Token> tokens;
        private int idx;
        private final int xVal;

        Parser(String expr, int xVal) {
            this.tokens = new Lexer(expr).tokenize();
            this.xVal = xVal;
        }

        private Token peek() {
            return tokens.get(idx);
        }

        private Token consume() {
            return tokens.get(idx++);
        }

        private boolean match(TokenType t) {
            if (peek().type == t) {
                consume();
                return true;
            }
            return false;
        }

        double parseExpression() {
            // expr = term (('+'|'-') term)*
            double val = parseTerm();
            while (true) {
                Token t = peek();
                if (t.type == TokenType.PLUS) {
                    consume();
                    val += parseTerm();
                } else if (t.type == TokenType.MINUS) {
                    consume();
                    val -= parseTerm();
                } else {
                    break;
                }
            }
            return val;
        }

        double parseTerm() {
            // term = factor (('*'|'/') factor | implicit-mul factor)*
            double val = parseFactor();
            while (true) {
                Token t = peek();
                if (t.type == TokenType.MUL) {
                    consume();
                    val *= parseFactor();
                } else if (t.type == TokenType.DIV) {
                    consume();
                    double rhs = parseFactor();
                    val /= rhs;
                } else if (isFactorStart(t)) {
                    // nhân ẩn: 2x, 2d(...), )x, )d(...)
                    val *= parseFactor();
                } else {
                    break;
                }
            }
            return val;
        }

        private boolean isFactorStart(Token t) {
            return t.type == TokenType.NUMBER
                    || t.type == TokenType.IDENT
                    || t.type == TokenType.LPAREN;
        }

        double parseFactor() {
            // unary: '+' factor | '-' factor | primary
            Token t = peek();
            if (t.type == TokenType.PLUS) {
                consume();
                return parseFactor();
            } else if (t.type == TokenType.MINUS) {
                consume();
                return -parseFactor();
            }
            return parsePrimary();
        }

        double parsePrimary() {
            Token t = peek();
            switch (t.type) {
                case NUMBER:
                    consume();
                    return Double.parseDouble(t.text);
                case IDENT:
                    return parseIdentOrFunc();
                case LPAREN:
                    consume();
                    double v = parseExpression();
                    if (peek().type == TokenType.RPAREN) {
                        consume();
                    }
                    return v;
                default:
                    consume();
                    return 0;
            }
        }

        double parseIdentOrFunc() {
            Token t = consume();
            String id = t.text;

            // biến: x, X, y
            if (id.equals("x") || id.equals("X") || id.equals("y")) {
                return xVal;
            }

            // logN(...)  ví dụ: log10(x), log20(x), log3(x)
            if (id.startsWith("log") && id.length() > 3) {
                String baseStr = id.substring(3);
                double base = Double.parseDouble(baseStr);
                if (match(TokenType.LPAREN)) {
                    double inner = parseExpression();
                    if (peek().type == TokenType.RPAREN) {
                        consume();
                    }
                    double logarithm = Math.log(inner) / Math.log(base);
                    // TMS282 boost tiers are integral: log40(x)*20 grants20% at40, not22% at60.
                    // Keep the legacy265 evaluator unchanged.
                    return net.swordie.ms.connection.protocol.Protocol.active().version()
                            == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                            ? Math.floor(logarithm) : logarithm;
                } else {
                    return 0;
                }
            }

            // min / max
            if (id.equals("min") || id.equals("max")) {
                if (!match(TokenType.LPAREN)) {
                    return 0;
                }
                double a = parseExpression();
                if (match(TokenType.COMMA)) {
                    double b = parseExpression();
                    if (peek().type == TokenType.RPAREN) {
                        consume();
                    }
                    return id.equals("min") ? Math.min(a, b) : Math.max(a, b);
                } else {
                    if (peek().type == TokenType.RPAREN) {
                        consume();
                    }
                    return a;
                }
            }

            // d(...) = floor, u(...) = ceil
            if ((id.equals("d") || id.equals("u")) && match(TokenType.LPAREN)) {
                double inner = parseExpression();
                if (peek().type == TokenType.RPAREN) {
                    consume();
                }
                return id.equals("d") ? Math.floor(inner) : Math.ceil(inner);
            }

            // ident khác: coi như 0 (vd: "slashStorm2" nếu còn sót)
            return 0;
        }
    }
}
