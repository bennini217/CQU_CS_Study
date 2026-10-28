#include"front/syntax.h"

#include<cassert>
#ifdef DEBUG_PARSER
#include<iostream>
#endif

using frontend::Parser;
using frontend::TokenType;
using frontend::Term;
using frontend::AstNode;
using ir::Type;

// #define DEBUG_PARSER

Parser::Parser(const std::vector<frontend::Token>& tokens): index(0), token_stream(tokens) {}

Parser::~Parser() {}

bool Parser::parseTerm(AstNode* root, TokenType expect) {
    if (index >= token_stream.size() || token_stream[index].type != expect)
        return false;
    root->children.push_back(new Term(token_stream[index], root));
    index++;
    return true;
}

frontend::CompUnit* Parser::get_abstract_syntax_tree() {
    auto* root = new CompUnit(nullptr);
    assert(parseCompUnit(root));
    assert(index == token_stream.size());
    return root;
}

void Parser::log(AstNode* node) {
#ifdef DEBUG_PARSER
    std::cout << "in parse" << toString(node->type) << ", cur_token_type::"
              << toString(token_stream[index].type) << ", token_val::" << token_stream[index].value << '\n';
#endif
}

bool Parser::parseCompUnit(CompUnit* root) {
    log(root);
    if (index >= token_stream.size())
        return false;

    uint32_t save = index;
    auto* decl = new Decl(root);
    if (parseDecl(decl)) {
        root->children.push_back(decl);
        auto* rest = new CompUnit(root);
        if (parseCompUnit(rest))
            root->children.push_back(rest);
        else
            delete rest;
        return true;
    }
    delete decl;
    index = save;

    auto* fd = new FuncDef(root);
    if (parseFuncDef(fd)) {
        root->children.push_back(fd);
        auto* rest = new CompUnit(root);
        if (parseCompUnit(rest))
            root->children.push_back(rest);
        else
            delete rest;
        return true;
    }
    delete fd;
    index = save;
    return false;
}

bool Parser::parseDecl(Decl* root) {
    log(root);
    uint32_t save = index;
    auto* cd = new ConstDecl(root);
    if (parseConstDecl(cd)) {
        root->children.push_back(cd);
        return true;
    }
    delete cd;
    index = save;

    auto* vd = new VarDecl(root);
    if (parseVarDecl(vd)) {
        root->children.push_back(vd);
        return true;
    }
    delete vd;
    index = save;
    return false;
}

bool Parser::parseBType(BType* root) {
    log(root);
    if (index < token_stream.size() && token_stream[index].type == TokenType::INTTK) {
        root->t = Type::Int;
        return parseTerm(root, TokenType::INTTK);
    }
    if (index < token_stream.size() && token_stream[index].type == TokenType::FLOATTK) {
        root->t = Type::Float;
        return parseTerm(root, TokenType::FLOATTK);
    }
    return false;
}

bool Parser::parseConstDecl(ConstDecl* root) {
    log(root);
    if (!parseTerm(root, TokenType::CONSTTK))
        return false;
    auto* bt = new BType(root);
    if (!parseBType(bt)) {
        delete bt;
        return false;
    }
    root->t = bt->t;

    auto* cdf = new ConstDef(root);
    if (!parseConstDef(cdf)) {
        delete bt;
        delete cdf;
        return false;
    }
    root->children.push_back(bt);
    root->children.push_back(cdf);
    while (index < token_stream.size() && token_stream[index].type == TokenType::COMMA) {
        if (!parseTerm(root, TokenType::COMMA))
            return false;
        auto* c2 = new ConstDef(root);
        if (!parseConstDef(c2)) {
            delete c2;
            return false;
        }
        root->children.push_back(c2);
    }
    if (!parseTerm(root, TokenType::SEMICN))
        return false;
    return true;
}

bool Parser::parseConstDef(ConstDef* root) {
    log(root);
    if (!parseTerm(root, TokenType::IDENFR))
        return false;
    while (index < token_stream.size() && token_stream[index].type == TokenType::LBRACK) {
        assert(parseTerm(root, TokenType::LBRACK));
        auto* ce = new ConstExp(root);
        assert(parseConstExp(ce));
        root->children.push_back(ce);
        assert(parseTerm(root, TokenType::RBRACK));
    }
    assert(parseTerm(root, TokenType::ASSIGN));
    auto* civ = new ConstInitVal(root);
    assert(parseConstInitVal(civ));
    root->children.push_back(civ);
    return true;
}

bool Parser::parseConstInitVal(ConstInitVal* root) {
    log(root);
    if (index >= token_stream.size() || token_stream[index].type != TokenType::LBRACE) {
        auto* ce = new ConstExp(root);
        if (!parseConstExp(ce)) {
            delete ce;
            return false;
        }
        root->children.push_back(ce);
        return true;
    }
    assert(parseTerm(root, TokenType::LBRACE));
    if (index < token_stream.size() && token_stream[index].type == TokenType::RBRACE) {
        assert(parseTerm(root, TokenType::RBRACE));
        return true;
    }
    for (;;) {
        auto* inner = new ConstInitVal(root);
        assert(parseConstInitVal(inner));
        root->children.push_back(inner);
        if (index < token_stream.size() && token_stream[index].type == TokenType::COMMA) {
            assert(parseTerm(root, TokenType::COMMA));
            continue;
        }
        break;
    }
    assert(parseTerm(root, TokenType::RBRACE));
    return true;
}

bool Parser::parseVarDecl(VarDecl* root) {
    log(root);
    auto* bt = new BType(root);
    if (!parseBType(bt)) {
        delete bt;
        return false;
    }
    auto* vd = new VarDef(root);
    if (!parseVarDef(vd)) {
        delete bt;
        delete vd;
        return false;
    }
    root->children.push_back(bt);
    root->children.push_back(vd);
    while (index < token_stream.size() && token_stream[index].type == TokenType::COMMA) {
        if (!parseTerm(root, TokenType::COMMA))
            return false;
        auto* v2 = new VarDef(root);
        if (!parseVarDef(v2)) {
            delete v2;
            return false;
        }
        root->children.push_back(v2);
    }
    if (!parseTerm(root, TokenType::SEMICN))
        return false;
    return true;
}

bool Parser::parseVarDef(VarDef* root) {
    log(root);
    if (!parseTerm(root, TokenType::IDENFR))
        return false;
    while (index < token_stream.size() && token_stream[index].type == TokenType::LBRACK) {
        assert(parseTerm(root, TokenType::LBRACK));
        auto* ce = new ConstExp(root);
        assert(parseConstExp(ce));
        root->children.push_back(ce);
        assert(parseTerm(root, TokenType::RBRACK));
    }
    if (index < token_stream.size() && token_stream[index].type == TokenType::ASSIGN) {
        assert(parseTerm(root, TokenType::ASSIGN));
        auto* iv = new InitVal(root);
        assert(parseInitVal(iv));
        root->children.push_back(iv);
    }
    return true;
}

bool Parser::parseInitVal(InitVal* root) {
    log(root);
    if (index >= token_stream.size() || token_stream[index].type != TokenType::LBRACE) {
        auto* e = new Exp(root);
        if (!parseExp(e)) {
            delete e;
            return false;
        }
        root->children.push_back(e);
        return true;
    }
    assert(parseTerm(root, TokenType::LBRACE));
    if (index < token_stream.size() && token_stream[index].type == TokenType::RBRACE) {
        assert(parseTerm(root, TokenType::RBRACE));
        return true;
    }
    for (;;) {
        auto* inner = new InitVal(root);
        assert(parseInitVal(inner));
        root->children.push_back(inner);
        if (index < token_stream.size() && token_stream[index].type == TokenType::COMMA) {
            assert(parseTerm(root, TokenType::COMMA));
            continue;
        }
        break;
    }
    assert(parseTerm(root, TokenType::RBRACE));
    return true;
}

bool Parser::parseFuncType(FuncType* root) {
    log(root);
    if (index < token_stream.size() && token_stream[index].type == TokenType::VOIDTK)
        return parseTerm(root, TokenType::VOIDTK);
    if (index < token_stream.size() && token_stream[index].type == TokenType::INTTK)
        return parseTerm(root, TokenType::INTTK);
    if (index < token_stream.size() && token_stream[index].type == TokenType::FLOATTK)
        return parseTerm(root, TokenType::FLOATTK);
    return false;
}

bool Parser::parseFuncDef(FuncDef* root) {
    log(root);
    auto* ft = new FuncType(root);
    if (!parseFuncType(ft)) {
        delete ft;
        return false;
    }
    root->children.push_back(ft);

    TokenType rt = token_stream[index - 1].type;
    if (rt == TokenType::VOIDTK)
        root->t = Type::null;
    else if (rt == TokenType::INTTK)
        root->t = Type::Int;
    else
        root->t = Type::Float;

    assert(parseTerm(root, TokenType::IDENFR));
    root->n = token_stream[index - 1].value;

    assert(parseTerm(root, TokenType::LPARENT));
    if (index < token_stream.size() && token_stream[index].type != TokenType::RPARENT) {
        auto* fps = new FuncFParams(root);
        assert(parseFuncFParams(fps));
        root->children.push_back(fps);
    }
    assert(parseTerm(root, TokenType::RPARENT));

    auto* blk = new Block(root);
    assert(parseBlock(blk));
    root->children.push_back(blk);
    return true;
}

bool Parser::parseFuncFParams(FuncFParams* root) {
    log(root);
    auto* fp = new FuncFParam(root);
    if (!parseFuncFParam(fp)) {
        delete fp;
        return false;
    }
    root->children.push_back(fp);
    while (index < token_stream.size() && token_stream[index].type == TokenType::COMMA) {
        assert(parseTerm(root, TokenType::COMMA));
        auto* fp2 = new FuncFParam(root);
        assert(parseFuncFParam(fp2));
        root->children.push_back(fp2);
    }
    return true;
}

bool Parser::parseFuncFParam(FuncFParam* root) {
    log(root);
    auto* bt = new BType(root);
    if (!parseBType(bt)) {
        delete bt;
        return false;
    }
    root->children.push_back(bt);
    assert(parseTerm(root, TokenType::IDENFR));
    if (index + 1 < token_stream.size() && token_stream[index].type == TokenType::LBRACK
        && token_stream[index + 1].type == TokenType::RBRACK) {
        assert(parseTerm(root, TokenType::LBRACK));
        assert(parseTerm(root, TokenType::RBRACK));
        while (index < token_stream.size() && token_stream[index].type == TokenType::LBRACK) {
            assert(parseTerm(root, TokenType::LBRACK));
            auto* ex = new Exp(root);
            assert(parseExp(ex));
            root->children.push_back(ex);
            assert(parseTerm(root, TokenType::RBRACK));
        }
    }
    return true;
}

bool Parser::parseBlock(Block* root) {
    log(root);
    assert(parseTerm(root, TokenType::LBRACE));
    while (index < token_stream.size() && token_stream[index].type != TokenType::RBRACE) {
        auto* bi = new BlockItem(root);
        assert(parseBlockItem(bi));
        root->children.push_back(bi);
    }
    assert(parseTerm(root, TokenType::RBRACE));
    return true;
}

bool Parser::parseBlockItem(BlockItem* root) {
    log(root);
    auto* d = new Decl(root);
    if (parseDecl(d)) {
        root->children.push_back(d);
        return true;
    }
    delete d;
    auto* st = new Stmt(root);
    if (parseStmt(st)) {
        root->children.push_back(st);
        return true;
    }
    delete st;
    return false;
}

bool Parser::parseStmt(Stmt* root) {
    log(root);
    if (index < token_stream.size() && token_stream[index].type == TokenType::SEMICN)
        return parseTerm(root, TokenType::SEMICN);

    if (index < token_stream.size() && token_stream[index].type == TokenType::LBRACE) {
        auto* b = new Block(root);
        if (!parseBlock(b)) {
            delete b;
            return false;
        }
        root->children.push_back(b);
        return true;
    }

    if (index < token_stream.size() && token_stream[index].type == TokenType::IFTK) {
        assert(parseTerm(root, TokenType::IFTK));
        assert(parseTerm(root, TokenType::LPARENT));
        auto* cd = new Cond(root);
        assert(parseCond(cd));
        root->children.push_back(cd);
        assert(parseTerm(root, TokenType::RPARENT));
        auto* s1 = new Stmt(root);
        assert(parseStmt(s1));
        root->children.push_back(s1);
        if (index < token_stream.size() && token_stream[index].type == TokenType::ELSETK) {
            assert(parseTerm(root, TokenType::ELSETK));
            auto* s2 = new Stmt(root);
            assert(parseStmt(s2));
            root->children.push_back(s2);
        }
        return true;
    }

    if (index < token_stream.size() && token_stream[index].type == TokenType::WHILETK) {
        assert(parseTerm(root, TokenType::WHILETK));
        assert(parseTerm(root, TokenType::LPARENT));
        auto* cd = new Cond(root);
        assert(parseCond(cd));
        root->children.push_back(cd);
        assert(parseTerm(root, TokenType::RPARENT));
        auto* body = new Stmt(root);
        assert(parseStmt(body));
        root->children.push_back(body);
        return true;
    }

    if (index < token_stream.size() && token_stream[index].type == TokenType::BREAKTK) {
        assert(parseTerm(root, TokenType::BREAKTK));
        assert(parseTerm(root, TokenType::SEMICN));
        return true;
    }

    if (index < token_stream.size() && token_stream[index].type == TokenType::CONTINUETK) {
        assert(parseTerm(root, TokenType::CONTINUETK));
        assert(parseTerm(root, TokenType::SEMICN));
        return true;
    }

    if (index < token_stream.size() && token_stream[index].type == TokenType::RETURNTK) {
        assert(parseTerm(root, TokenType::RETURNTK));
        if (index < token_stream.size() && token_stream[index].type == TokenType::SEMICN) {
            assert(parseTerm(root, TokenType::SEMICN));
            return true;
        }
        auto* ex = new Exp(root);
        assert(parseExp(ex));
        root->children.push_back(ex);
        assert(parseTerm(root, TokenType::SEMICN));
        return true;
    }

    uint32_t save = index;
    auto* lv = new LVal(root);
    if (parseLVal(lv) && index < token_stream.size() && token_stream[index].type == TokenType::ASSIGN) {
        root->children.push_back(lv);
        assert(parseTerm(root, TokenType::ASSIGN));
        auto* e = new Exp(root);
        assert(parseExp(e));
        root->children.push_back(e);
        assert(parseTerm(root, TokenType::SEMICN));
        return true;
    }
    delete lv;
    index = save;

    auto* ex2 = new Exp(root);
    if (parseExp(ex2) && index < token_stream.size() && token_stream[index].type == TokenType::SEMICN) {
        root->children.push_back(ex2);
        assert(parseTerm(root, TokenType::SEMICN));
        return true;
    }
    delete ex2;
    index = save;
    return false;
}

bool Parser::parseExp(Exp* root) {
    log(root);
    auto* ad = new AddExp(root);
    if (!parseAddExp(ad)) {
        delete ad;
        return false;
    }
    root->children.push_back(ad);
    return true;
}

bool Parser::parseCond(Cond* root) {
    log(root);
    auto* lo = new LOrExp(root);
    if (!parseLOrExp(lo)) {
        delete lo;
        return false;
    }
    root->children.push_back(lo);
    return true;
}

bool Parser::parseLVal(LVal* root) {
    log(root);
    if (!parseTerm(root, TokenType::IDENFR))
        return false;
    while (index < token_stream.size() && token_stream[index].type == TokenType::LBRACK) {
        assert(parseTerm(root, TokenType::LBRACK));
        auto* e = new Exp(root);
        assert(parseExp(e));
        root->children.push_back(e);
        assert(parseTerm(root, TokenType::RBRACK));
    }
    return true;
}

bool Parser::parseNumber(Number* root) {
    log(root);
    if (index < token_stream.size() && token_stream[index].type == TokenType::INTLTR)
        return parseTerm(root, TokenType::INTLTR);
    if (index < token_stream.size() && token_stream[index].type == TokenType::FLOATLTR)
        return parseTerm(root, TokenType::FLOATLTR);
    return false;
}

bool Parser::parsePrimaryExp(PrimaryExp* root) {
    log(root);
    if (index < token_stream.size() && token_stream[index].type == TokenType::LPARENT) {
        assert(parseTerm(root, TokenType::LPARENT));
        auto* e = new Exp(root);
        assert(parseExp(e));
        root->children.push_back(e);
        assert(parseTerm(root, TokenType::RPARENT));
        return true;
    }
    auto* num = new Number(root);
    if (parseNumber(num)) {
        root->children.push_back(num);
        return true;
    }
    delete num;
    auto* lv = new LVal(root);
    if (parseLVal(lv)) {
        root->children.push_back(lv);
        return true;
    }
    delete lv;
    return false;
}

bool Parser::parseUnaryOp(UnaryOp* root) {
    log(root);
    if (index < token_stream.size() && token_stream[index].type == TokenType::PLUS)
        return parseTerm(root, TokenType::PLUS);
    if (index < token_stream.size() && token_stream[index].type == TokenType::MINU)
        return parseTerm(root, TokenType::MINU);
    if (index < token_stream.size() && token_stream[index].type == TokenType::NOT)
        return parseTerm(root, TokenType::NOT);
    return false;
}

bool Parser::parseUnaryExp(UnaryExp* root) {
    log(root);
    uint32_t save = index;
    if (index < token_stream.size()
        && (token_stream[index].type == TokenType::PLUS || token_stream[index].type == TokenType::MINU
            || token_stream[index].type == TokenType::NOT)) {
        auto* uo = new UnaryOp(root);
        if (parseUnaryOp(uo)) {
            auto* inner = new UnaryExp(root);
            if (parseUnaryExp(inner)) {
                root->children.push_back(uo);
                root->children.push_back(inner);
                return true;
            }
            delete inner;
        }
        delete uo;
        index = save;
    }

    if (index + 1 < token_stream.size() && token_stream[index].type == TokenType::IDENFR
        && token_stream[index + 1].type == TokenType::LPARENT) {
        assert(parseTerm(root, TokenType::IDENFR));
        assert(parseTerm(root, TokenType::LPARENT));
        if (index < token_stream.size() && token_stream[index].type != TokenType::RPARENT) {
            auto* fr = new FuncRParams(root);
            assert(parseFuncRParams(fr));
            root->children.push_back(fr);
        }
        assert(parseTerm(root, TokenType::RPARENT));
        return true;
    }

    auto* pe = new PrimaryExp(root);
    if (!parsePrimaryExp(pe)) {
        delete pe;
        return false;
    }
    root->children.push_back(pe);
    return true;
}

bool Parser::parseFuncRParams(FuncRParams* root) {
    log(root);
    auto* e = new Exp(root);
    if (!parseExp(e)) {
        delete e;
        return false;
    }
    root->children.push_back(e);
    while (index < token_stream.size() && token_stream[index].type == TokenType::COMMA) {
        assert(parseTerm(root, TokenType::COMMA));
        auto* e2 = new Exp(root);
        assert(parseExp(e2));
        root->children.push_back(e2);
    }
    return true;
}

bool Parser::parseMulExp(MulExp* root) {
    log(root);
    auto* u = new UnaryExp(root);
    if (!parseUnaryExp(u)) {
        delete u;
        return false;
    }
    root->children.push_back(u);
    while (index < token_stream.size()
        && (token_stream[index].type == TokenType::MULT || token_stream[index].type == TokenType::DIV
            || token_stream[index].type == TokenType::MOD)) {
        TokenType tk = token_stream[index].type;
        assert(parseTerm(root, tk));
        auto* u2 = new UnaryExp(root);
        assert(parseUnaryExp(u2));
        root->children.push_back(u2);
    }
    return true;
}

bool Parser::parseAddExp(AddExp* root) {
    log(root);
    auto* m = new MulExp(root);
    if (!parseMulExp(m)) {
        delete m;
        return false;
    }
    root->children.push_back(m);
    while (index < token_stream.size()
        && (token_stream[index].type == TokenType::PLUS || token_stream[index].type == TokenType::MINU)) {
        TokenType tk = token_stream[index].type;
        assert(parseTerm(root, tk));
        auto* m2 = new MulExp(root);
        assert(parseMulExp(m2));
        root->children.push_back(m2);
    }
    return true;
}

bool Parser::parseRelExp(RelExp* root) {
    log(root);
    auto* a = new AddExp(root);
    if (!parseAddExp(a)) {
        delete a;
        return false;
    }
    root->children.push_back(a);
    while (index < token_stream.size()
        && (token_stream[index].type == TokenType::LSS || token_stream[index].type == TokenType::GTR
            || token_stream[index].type == TokenType::LEQ || token_stream[index].type == TokenType::GEQ)) {
        TokenType tk = token_stream[index].type;
        assert(parseTerm(root, tk));
        auto* a2 = new AddExp(root);
        assert(parseAddExp(a2));
        root->children.push_back(a2);
    }
    return true;
}

bool Parser::parseEqExp(EqExp* root) {
    log(root);
    auto* r = new RelExp(root);
    if (!parseRelExp(r)) {
        delete r;
        return false;
    }
    root->children.push_back(r);
    while (index < token_stream.size()
        && (token_stream[index].type == TokenType::EQL || token_stream[index].type == TokenType::NEQ)) {
        TokenType tk = token_stream[index].type;
        assert(parseTerm(root, tk));
        auto* r2 = new RelExp(root);
        assert(parseRelExp(r2));
        root->children.push_back(r2);
    }
    return true;
}

bool Parser::parseLAndExp(LAndExp* root) {
    log(root);
    auto* e = new EqExp(root);
    if (!parseEqExp(e)) {
        delete e;
        return false;
    }
    root->children.push_back(e);
    if (index < token_stream.size() && token_stream[index].type == TokenType::AND) {
        assert(parseTerm(root, TokenType::AND));
        auto* rest = new LAndExp(root);
        assert(parseLAndExp(rest));
        root->children.push_back(rest);
    }
    return true;
}

bool Parser::parseLOrExp(LOrExp* root) {
    log(root);
    auto* la = new LAndExp(root);
    if (!parseLAndExp(la)) {
        delete la;
        return false;
    }
    root->children.push_back(la);
    if (index < token_stream.size() && token_stream[index].type == TokenType::OR) {
        assert(parseTerm(root, TokenType::OR));
        auto* rest = new LOrExp(root);
        assert(parseLOrExp(rest));
        root->children.push_back(rest);
    }
    return true;
}

bool Parser::parseConstExp(ConstExp* root) {
    log(root);
    auto* ad = new AddExp(root);
    if (!parseAddExp(ad)) {
        delete ad;
        return false;
    }
    root->children.push_back(ad);
    return true;
}
