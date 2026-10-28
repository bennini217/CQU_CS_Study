#include"front/lexical.h"

#include<cassert>
#include<cctype>
#include<map>
#include<sstream>
#include<string>

// #define DEBUG_DFA
// #define DEBUG_SCANNER

namespace {

bool is_space(char c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\v';
}

bool is_ident_start(char c) {
    return std::isalpha(static_cast<unsigned char>(c)) || c == '_';
}

bool is_ident_body(char c) {
    return std::isalnum(static_cast<unsigned char>(c)) || c == '_';
}

bool is_hex_digit(char c) {
    return std::isxdigit(static_cast<unsigned char>(c)) != 0;
}

void emit_ident(frontend::Token& buf, const std::string& s) {
    static const std::map<std::string, frontend::TokenType> kw = {
        {"const", frontend::TokenType::CONSTTK},
        {"void", frontend::TokenType::VOIDTK},
        {"int", frontend::TokenType::INTTK},
        {"float", frontend::TokenType::FLOATTK},
        {"if", frontend::TokenType::IFTK},
        {"else", frontend::TokenType::ELSETK},
        {"while", frontend::TokenType::WHILETK},
        {"continue", frontend::TokenType::CONTINUETK},
        {"break", frontend::TokenType::BREAKTK},
        {"return", frontend::TokenType::RETURNTK},
    };
    auto it = kw.find(s);
    buf.type = (it != kw.end()) ? it->second : frontend::TokenType::IDENFR;
    buf.value = s;
}

void emit_single_op(frontend::Token& buf, char c) {
    buf.value = std::string(1, c);
    switch (c) {
    case '+': buf.type = frontend::TokenType::PLUS; break;
    case '-': buf.type = frontend::TokenType::MINU; break;
    case '*': buf.type = frontend::TokenType::MULT; break;
    case '/': buf.type = frontend::TokenType::DIV; break;
    case '%': buf.type = frontend::TokenType::MOD; break;
    case '<': buf.type = frontend::TokenType::LSS; break;
    case '>': buf.type = frontend::TokenType::GTR; break;
    case ':': buf.type = frontend::TokenType::COLON; break;
    case ';': buf.type = frontend::TokenType::SEMICN; break;
    case ',': buf.type = frontend::TokenType::COMMA; break;
    case '(': buf.type = frontend::TokenType::LPARENT; break;
    case ')': buf.type = frontend::TokenType::RPARENT; break;
    case '[': buf.type = frontend::TokenType::LBRACK; break;
    case ']': buf.type = frontend::TokenType::RBRACK; break;
    case '{': buf.type = frontend::TokenType::LBRACE; break;
    case '}': buf.type = frontend::TokenType::RBRACE; break;
    case '!': buf.type = frontend::TokenType::NOT; break;
    default:
        assert(0 && "unknown single-char op");
    }
}

std::string strip_comments(std::istream& in) {
    std::ostringstream oss;
    char c;
    while (in.get(c)) {
        if (c == '/' && in.peek() == '/') {
            in.get();
            for (;;) {
                int p = in.peek();
                if (p == EOF || p == '\n')
                    break;
                in.get();
            }
            continue;
        }
        if (c == '/' && in.peek() == '*') {
            in.get();
            char p = '\0';
            while (in.get(c)) {
                if (p == '*' && c == '/')
                    break;
                p = c;
            }
            continue;
        }
        oss << c;
    }
    return oss.str();
}

} // namespace

std::string frontend::toString(State s) {
    switch (s) {
    case State::Empty: return "Empty";
    case State::Ident: return "Ident";
    case State::IntLiteral: return "IntLiteral";
    case State::FloatLiteral: return "FloatLiteral";
    case State::op: return "op";
    default:
        assert(0 && "invalid State");
    }
    return "";
}

std::set<std::string> frontend::keywords= {
    "const", "int", "float", "if", "else", "while", "continue", "break", "return", "void"
};

frontend::DFA::DFA(): cur_state(frontend::State::Empty), cur_str(), int_is_hex(false) {}

frontend::DFA::~DFA() {}

bool frontend::DFA::next(char input, Token& buf, bool& reprocess_input) {
    reprocess_input = false;
#ifdef DEBUG_DFA
#include<iostream>
    std::cout << "in state [" << toString(cur_state) << "], input = \'" << input << "\', str = " << cur_str << "\t";
#endif
    switch (cur_state) {
    case State::Empty:
        if (is_space(input))
            return false;
        if (is_ident_start(input)) {
            cur_state = State::Ident;
            cur_str = std::string(1, input);
            return false;
        }
        if (std::isdigit(static_cast<unsigned char>(input))) {
            cur_state = State::IntLiteral;
            cur_str = std::string(1, input);
            int_is_hex = false;
            return false;
        }
        if (input == '.') {
            cur_state = State::FloatLiteral;
            cur_str = ".";
            return false;
        }
        cur_state = State::op;
        cur_str = std::string(1, input);
        return false;

    case State::Ident:
        if (is_ident_body(input)) {
            cur_str += input;
            return false;
        }
        emit_ident(buf, cur_str);
        reset();
        reprocess_input = true;
        return true;

    case State::IntLiteral:
        if (!int_is_hex && cur_str == "0" && (input == 'x' || input == 'X')) {
            cur_str += input;
            int_is_hex = true;
            return false;
        }
        if (int_is_hex) {
            if (is_hex_digit(input)) {
                cur_str += input;
                return false;
            }
            buf.type = TokenType::INTLTR;
            buf.value = cur_str;
            reset();
            reprocess_input = true;
            return true;
        }
        if (std::isdigit(static_cast<unsigned char>(input))) {
            cur_str += input;
            return false;
        }
        if (input == '.') {
            cur_state = State::FloatLiteral;
            cur_str += input;
            return false;
        }
        buf.type = TokenType::INTLTR;
        buf.value = cur_str;
        reset();
        reprocess_input = true;
        return true;

    case State::FloatLiteral:
        if (std::isdigit(static_cast<unsigned char>(input))) {
            cur_str += input;
            return false;
        }
        buf.type = TokenType::FLOATLTR;
        buf.value = cur_str;
        reset();
        reprocess_input = true;
        return true;

    case State::op: {
        char a = cur_str[0];
        assert(cur_str.size() == 1);
        if (a == '=' && input == '=') {
            buf.type = TokenType::EQL;
            buf.value = "==";
            reset();
            return true;
        }
        if (a == '=') {
            buf.type = TokenType::ASSIGN;
            buf.value = "=";
            reset();
            reprocess_input = true;
            return true;
        }
        if (a == '<' && input == '=') {
            buf.type = TokenType::LEQ;
            buf.value = "<=";
            reset();
            return true;
        }
        if (a == '<') {
            buf.type = TokenType::LSS;
            buf.value = "<";
            reset();
            reprocess_input = true;
            return true;
        }
        if (a == '>' && input == '=') {
            buf.type = TokenType::GEQ;
            buf.value = ">=";
            reset();
            return true;
        }
        if (a == '>') {
            buf.type = TokenType::GTR;
            buf.value = ">";
            reset();
            reprocess_input = true;
            return true;
        }
        if (a == '!' && input == '=') {
            buf.type = TokenType::NEQ;
            buf.value = "!=";
            reset();
            return true;
        }
        if (a == '!') {
            buf.type = TokenType::NOT;
            buf.value = "!";
            reset();
            reprocess_input = true;
            return true;
        }
        if (a == '&' && input == '&') {
            buf.type = TokenType::AND;
            buf.value = "&&";
            reset();
            return true;
        }
        if (a == '&') {
            assert(0 && "invalid single '&'");
        }
        if (a == '|' && input == '|') {
            buf.type = TokenType::OR;
            buf.value = "||";
            reset();
            return true;
        }
        if (a == '|') {
            assert(0 && "invalid single '|'");
        }
        emit_single_op(buf, a);
        reset();
        reprocess_input = true;
        return true;
    }
    default:
        assert(0 && "invalid State");
    }
    return false;
}

void frontend::DFA::reset() {
    cur_state = State::Empty;
    cur_str.clear();
    int_is_hex = false;
}

frontend::Scanner::Scanner(std::string filename): fin(filename) {
    if(!fin.is_open()) {
        assert(0 && "in Scanner constructor, input file cannot open");
    }
}

frontend::Scanner::~Scanner() {
    fin.close();
}

std::vector<frontend::Token> frontend::Scanner::run() {
    std::string src = strip_comments(fin);
    src.push_back('\n');

    std::vector<Token> tokens;
    DFA dfa;
    Token buf;
    for (char c : src) {
        bool reprocess = true;
        while (reprocess) {
            bool rpin = false;
            if (dfa.next(c, buf, rpin)) {
#ifdef DEBUG_SCANNER
#include<iostream>
                std::cout << "token: " << toString(buf.type) << "\t" << buf.value << std::endl;
#endif
                tokens.push_back(buf);
            }
            reprocess = rpin;
        }
    }
    return tokens;
}