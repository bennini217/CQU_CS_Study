#include"backend/generator.h"

#include<assert.h>
#include<cstdint>
#include<cstring>
#include<cmath>
#include<sstream>
#include<unordered_map>


backend::Generator::Generator(ir::Program& p, std::ofstream& f): program(p), fout(f) {}

namespace {
// Keep it large and handle large immediates safely (see guidebook "如何偷懒" section).
constexpr int FRAME_SIZE = 8192;
constexpr int RA_OFF = -4;
constexpr int S0_OFF = -8;
constexpr int FIRST_LOCAL_OFF = -12;

struct FuncCtx {
    std::unordered_map<std::string, int> var_off;
    int next_off = FIRST_LOCAL_OFF;
    std::string func_name;
};

FuncCtx* g_ctx = nullptr;
std::unordered_map<std::string, ir::GlobalVal> g_globals;
std::unordered_map<std::string, const ir::Function*> g_funcs;
std::unordered_map<const ir::Instruction*, int> g_pc;

bool is_float_ty(ir::Type t) {
    return t == ir::Type::Float || t == ir::Type::FloatLiteral;
}

bool is_literal(ir::Type t) {
    return t == ir::Type::IntLiteral || t == ir::Type::FloatLiteral;
}

int ensure_slot(const ir::Operand& op) {
    if (op.name == "null" || is_literal(op.type)) return 0;
    // Globals must live in .bss/.data, never in current stack frame.
    if (g_globals.count(op.name)) return 0;
    auto it = g_ctx->var_off.find(op.name);
    if (it != g_ctx->var_off.end()) return it->second;
    int off = g_ctx->next_off;
    g_ctx->var_off[op.name] = off;
    g_ctx->next_off -= 4;
    return off;
}

std::string lbl(const std::string& f, int pc) {
    return ".L_" + f + "_" + std::to_string(pc);
}

uint32_t f2u(float x) {
    uint32_t u = 0;
    std::memcpy(&u, &x, sizeof(float));
    return u;
}

int parse_int_like_literal(const std::string& s) {
    // Some IR literals are encoded like "-1.000000" but used in integer ops.
    if (s.find('.') != std::string::npos) {
        return static_cast<int>(std::trunc(std::stof(s)));
    }
    return std::stoi(s);
}

bool imm12(int x) { return x >= -2048 && x <= 2047; }

void emit_addi_any(std::ofstream& fout, const std::string& rd, const std::string& rs, int imm) {
    if (imm12(imm)) {
        fout << "\taddi " << rd << ", " << rs << ", " << imm << "\n";
    } else {
        fout << "\tli t5, " << imm << "\n";
        fout << "\tadd " << rd << ", " << rs << ", t5\n";
    }
}

void emit_lw_s0(std::ofstream& fout, const std::string& rd, int off) {
    if (imm12(off)) fout << "\tlw " << rd << ", " << off << "(s0)\n";
    else fout << "\tli t5, " << off << "\n\tadd t5, s0, t5\n\tlw " << rd << ", 0(t5)\n";
}
void emit_sw_s0(std::ofstream& fout, const std::string& rs, int off) {
    if (imm12(off)) fout << "\tsw " << rs << ", " << off << "(s0)\n";
    else fout << "\tli t5, " << off << "\n\tadd t5, s0, t5\n\tsw " << rs << ", 0(t5)\n";
}
void emit_flw_s0(std::ofstream& fout, const std::string& rd, int off) {
    if (imm12(off)) fout << "\tflw " << rd << ", " << off << "(s0)\n";
    else fout << "\tli t5, " << off << "\n\tadd t5, s0, t5\n\tflw " << rd << ", 0(t5)\n";
}
void emit_fsw_s0(std::ofstream& fout, const std::string& rs, int off) {
    if (imm12(off)) fout << "\tfsw " << rs << ", " << off << "(s0)\n";
    else fout << "\tli t5, " << off << "\n\tadd t5, s0, t5\n\tfsw " << rs << ", 0(t5)\n";
}

void emit_lw_sp(std::ofstream& fout, const std::string& rd, int off) {
    if (imm12(off)) fout << "\tlw " << rd << ", " << off << "(sp)\n";
    else fout << "\tli t5, " << off << "\n\tadd t5, sp, t5\n\tlw " << rd << ", 0(t5)\n";
}
void emit_sw_sp(std::ofstream& fout, const std::string& rs, int off) {
    if (imm12(off)) fout << "\tsw " << rs << ", " << off << "(sp)\n";
    else fout << "\tli t5, " << off << "\n\tadd t5, sp, t5\n\tsw " << rs << ", 0(t5)\n";
}

void emit_load_int(std::ofstream& fout, const ir::Operand& op, const std::string& rd) {
    if (op.type == ir::Type::IntLiteral) {
        fout << "\tli " << rd << ", " << parse_int_like_literal(op.name) << "\n";
        return;
    }
    if (op.type == ir::Type::FloatLiteral) {
        float fv = std::stof(op.name);
        fout << "\tli " << rd << ", " << static_cast<int>(fv) << "\n";
        return;
    }
    if (g_ctx->var_off.count(op.name)) {
        if (op.type == ir::Type::Float) {
            emit_flw_s0(fout, "ft7", g_ctx->var_off[op.name]);
            fout << "\tfcvt.w.s " << rd << ", ft7, rtz\n";
        } else {
            emit_lw_s0(fout, rd, g_ctx->var_off[op.name]);
        }
        return;
    }
    if (g_globals.count(op.name)) {
        if (op.type == ir::Type::IntPtr || op.type == ir::Type::FloatPtr) {
            fout << "\tla " << rd << ", " << op.name << "\n";
            return;
        }
        fout << "\tla t6, " << op.name << "\n";
        if (op.type == ir::Type::Float) {
            fout << "\tflw ft7, 0(t6)\n";
            fout << "\tfcvt.w.s " << rd << ", ft7, rtz\n";
        } else {
            fout << "\tlw " << rd << ", 0(t6)\n";
        }
        return;
    }
    assert(0 && "unknown int operand");
}

void emit_store_int(std::ofstream& fout, const ir::Operand& op, const std::string& rs) {
    int off = ensure_slot(op);
    if (off != 0) {
        emit_sw_s0(fout, rs, off);
        return;
    }
    if (g_globals.count(op.name)) {
        fout << "\tla t6, " << op.name << "\n";
        fout << "\tsw " << rs << ", 0(t6)\n";
        return;
    }
}

void emit_load_float(std::ofstream& fout, const ir::Operand& op, const std::string& frd) {
    if (op.type == ir::Type::FloatLiteral) {
        float v = std::stof(op.name);
        fout << "\tli t6, " << f2u(v) << "\n";
        fout << "\tfmv.w.x " << frd << ", t6\n";
        return;
    }
    if (op.type == ir::Type::IntLiteral) {
        fout << "\tli t6, " << parse_int_like_literal(op.name) << "\n";
        fout << "\tfcvt.s.w " << frd << ", t6\n";
        return;
    }
    if (g_ctx->var_off.count(op.name)) {
        if (op.type == ir::Type::Int || op.type == ir::Type::IntPtr) {
            emit_lw_s0(fout, "t6", g_ctx->var_off[op.name]);
            fout << "\tfcvt.s.w " << frd << ", t6\n";
        } else {
            emit_flw_s0(fout, frd, g_ctx->var_off[op.name]);
        }
        return;
    }
    if (g_globals.count(op.name)) {
        fout << "\tla t6, " << op.name << "\n";
        if (op.type == ir::Type::Int || op.type == ir::Type::IntPtr) {
            fout << "\tlw t5, 0(t6)\n";
            fout << "\tfcvt.s.w " << frd << ", t5\n";
        } else {
            fout << "\tflw " << frd << ", 0(t6)\n";
        }
        return;
    }
    // Keep codegen alive for corner-case mixed-typed IR operands.
    fout << "\tli t6, 0\n";
    fout << "\tfmv.w.x " << frd << ", t6\n";
}

void emit_store_float(std::ofstream& fout, const ir::Operand& op, const std::string& frs) {
    int off = ensure_slot(op);
    if (off != 0) {
        emit_fsw_s0(fout, frs, off);
        return;
    }
    if (g_globals.count(op.name)) {
        fout << "\tla t6, " << op.name << "\n";
        fout << "\tfsw " << frs << ", 0(t6)\n";
        return;
    }
}

void gen_bin_i(std::ofstream& fout, const char* op, const ir::Instruction& in) {
    emit_load_int(fout, in.op1, "t0");
    emit_load_int(fout, in.op2, "t1");
    fout << "\t" << op << " t2, t0, t1\n";
    emit_store_int(fout, in.des, "t2");
}

void gen_bin_f(std::ofstream& fout, const char* op, const ir::Instruction& in) {
    emit_load_float(fout, in.op1, "ft0");
    emit_load_float(fout, in.op2, "ft1");
    fout << "\t" << op << " ft2, ft0, ft1\n";
    emit_store_float(fout, in.des, "ft2");
}

// flt/feq etc. write to integer reg t2; IR may store result as Float (0.0f/1.0f) like ir_executor.
void emit_store_cmp01(std::ofstream& fout, const ir::Instruction& in) {
    if (is_float_ty(in.des.type)) {
        fout << "\tfcvt.s.w ft2, t2\n";
        emit_store_float(fout, in.des, "ft2");
    } else {
        emit_store_int(fout, in.des, "t2");
    }
}

}

void backend::Generator::gen() {
    for (const auto& gv : program.globalVal) {
        g_globals.emplace(gv.val.name, gv);
    }
    for (const auto& f : program.functions) {
        g_funcs[f.name] = &f;
    }

    fout << ".option nopic\n";
    if (!program.globalVal.empty()) {
        fout << ".bss\n";
        for (const auto& gv : program.globalVal) {
            fout << ".globl " << gv.val.name << "\n";
            fout << gv.val.name << ":\n";
            if (gv.maxlen > 0) fout << "\t.space " << (gv.maxlen * 4) << "\n";
            else fout << "\t.word 0\n";
        }
    }
    fout << ".text\n";
    for (const auto& f : program.functions) gen_func(f);
}

void backend::Generator::gen_func(const ir::Function& f) {
    FuncCtx ctx;
    ctx.func_name = f.name;
    g_ctx = &ctx;
    g_pc.clear();
    for (size_t i = 0; i < f.InstVec.size(); ++i) g_pc[f.InstVec[i]] = static_cast<int>(i);

    fout << ".globl " << f.name << "\n";
    fout << f.name << ":\n";
    // prologue (support large frame size)
    emit_addi_any(fout, "sp", "sp", -FRAME_SIZE);
    // Save callee-saved regs at the high end of frame.
    emit_sw_sp(fout, "ra", FRAME_SIZE + RA_OFF);
    emit_sw_sp(fout, "s0", FRAME_SIZE + S0_OFF);
    emit_addi_any(fout, "s0", "sp", FRAME_SIZE);

    for (size_t i = 0; i < f.ParameterList.size(); ++i) {
        const auto& p = f.ParameterList[i];
        int off = ensure_slot(p);
        if (is_float_ty(p.type)) {
            if (i < 8) emit_fsw_s0(fout, "fa" + std::to_string(i), off);
            else {
                int stk = static_cast<int>((i - 8) * 4);
                emit_lw_s0(fout, "t0", stk);
                emit_sw_s0(fout, "t0", off);
            }
        } else {
            if (i < 8) emit_sw_s0(fout, "a" + std::to_string(i), off);
            else {
                int stk = static_cast<int>((i - 8) * 4);
                emit_lw_s0(fout, "t0", stk);
                emit_sw_s0(fout, "t0", off);
            }
        }
    }

    for (size_t i = 0; i < f.InstVec.size(); ++i) {
        fout << lbl(f.name, static_cast<int>(i)) << ":\n";
        gen_instr(*f.InstVec[i]);
    }
    fout << lbl(f.name, static_cast<int>(f.InstVec.size())) << ":\n";
    emit_lw_sp(fout, "ra", FRAME_SIZE + RA_OFF);
    emit_lw_sp(fout, "s0", FRAME_SIZE + S0_OFF);
    emit_addi_any(fout, "sp", "sp", FRAME_SIZE);
    fout << "\tjr ra\n";
}

void backend::Generator::gen_instr(const ir::Instruction& in) {
    switch (in.op) {
    case ir::Operator::def:
    case ir::Operator::mov:
        emit_load_int(fout, in.op1, "t0");
        emit_store_int(fout, in.des, "t0");
        break;
    case ir::Operator::fdef:
    case ir::Operator::fmov:
        emit_load_float(fout, in.op1, "ft0");
        emit_store_float(fout, in.des, "ft0");
        break;
    case ir::Operator::add: gen_bin_i(fout, "add", in); break;
    case ir::Operator::sub: gen_bin_i(fout, "sub", in); break;
    case ir::Operator::mul: gen_bin_i(fout, "mul", in); break;
    case ir::Operator::div: gen_bin_i(fout, "div", in); break;
    case ir::Operator::mod: gen_bin_i(fout, "rem", in); break;
    case ir::Operator::addi:
        emit_load_int(fout, in.op1, "t0");
        fout << "\taddi t2, t0, " << parse_int_like_literal(in.op2.name) << "\n";
        emit_store_int(fout, in.des, "t2");
        break;
    case ir::Operator::subi:
        emit_load_int(fout, in.op1, "t0");
        fout << "\taddi t2, t0, -" << parse_int_like_literal(in.op2.name) << "\n";
        emit_store_int(fout, in.des, "t2");
        break;
    case ir::Operator::fadd: gen_bin_f(fout, "fadd.s", in); break;
    case ir::Operator::fsub: gen_bin_f(fout, "fsub.s", in); break;
    case ir::Operator::fmul: gen_bin_f(fout, "fmul.s", in); break;
    case ir::Operator::fdiv: gen_bin_f(fout, "fdiv.s", in); break;
    case ir::Operator::cvt_i2f:
        emit_load_int(fout, in.op1, "t0");
        fout << "\tfcvt.s.w ft0, t0\n";
        emit_store_float(fout, in.des, "ft0");
        break;
    case ir::Operator::cvt_f2i:
        emit_load_float(fout, in.op1, "ft0");
        fout << "\tfcvt.w.s t0, ft0, rtz\n";
        emit_store_int(fout, in.des, "t0");
        break;
    case ir::Operator::lss: gen_bin_i(fout, "slt", in); break;
    case ir::Operator::flss:
        emit_load_float(fout, in.op1, "ft0");
        emit_load_float(fout, in.op2, "ft1");
        fout << "\tflt.s t2, ft0, ft1\n";
        emit_store_cmp01(fout, in);
        break;
    case ir::Operator::gtr:
        emit_load_int(fout, in.op1, "t0"); emit_load_int(fout, in.op2, "t1");
        fout << "\tslt t2, t1, t0\n"; emit_store_int(fout, in.des, "t2"); break;
    case ir::Operator::fgtr:
        emit_load_float(fout, in.op1, "ft0");
        emit_load_float(fout, in.op2, "ft1");
        fout << "\tflt.s t2, ft1, ft0\n";
        emit_store_cmp01(fout, in);
        break;
    case ir::Operator::leq:
        emit_load_int(fout, in.op2, "t0"); emit_load_int(fout, in.op1, "t1");
        fout << "\tslt t2, t0, t1\n\txori t2, t2, 1\n"; emit_store_int(fout, in.des, "t2"); break;
    case ir::Operator::fleq:
        emit_load_float(fout, in.op1, "ft0");
        emit_load_float(fout, in.op2, "ft1");
        fout << "\tfle.s t2, ft0, ft1\n";
        emit_store_cmp01(fout, in);
        break;
    case ir::Operator::geq:
        emit_load_int(fout, in.op1, "t0"); emit_load_int(fout, in.op2, "t1");
        fout << "\tslt t2, t0, t1\n\txori t2, t2, 1\n"; emit_store_int(fout, in.des, "t2"); break;
    case ir::Operator::fgeq:
        emit_load_float(fout, in.op1, "ft0");
        emit_load_float(fout, in.op2, "ft1");
        fout << "\tflt.s t2, ft0, ft1\n\txori t2, t2, 1\n";
        emit_store_cmp01(fout, in);
        break;
    case ir::Operator::eq:
        emit_load_int(fout, in.op1, "t0"); emit_load_int(fout, in.op2, "t1");
        fout << "\txor t2, t0, t1\n\tseqz t2, t2\n"; emit_store_int(fout, in.des, "t2"); break;
    case ir::Operator::feq:
        emit_load_float(fout, in.op1, "ft0");
        emit_load_float(fout, in.op2, "ft1");
        fout << "\tfeq.s t2, ft0, ft1\n";
        emit_store_cmp01(fout, in);
        break;
    case ir::Operator::neq:
        emit_load_int(fout, in.op1, "t0"); emit_load_int(fout, in.op2, "t1");
        fout << "\txor t2, t0, t1\n\tsnez t2, t2\n"; emit_store_int(fout, in.des, "t2"); break;
    case ir::Operator::fneq:
        emit_load_float(fout, in.op1, "ft0");
        emit_load_float(fout, in.op2, "ft1");
        fout << "\tfeq.s t2, ft0, ft1\n\txori t2, t2, 1\n";
        emit_store_cmp01(fout, in);
        break;
    case ir::Operator::_not:
        emit_load_int(fout, in.op1, "t0");
        fout << "\tseqz t1, t0\n";
        emit_store_int(fout, in.des, "t1");
        break;
    case ir::Operator::_and:
        emit_load_int(fout, in.op1, "t0"); emit_load_int(fout, in.op2, "t1");
        fout << "\tsnez t0, t0\n\tsnez t1, t1\n\tand t2, t0, t1\n"; emit_store_int(fout, in.des, "t2"); break;
    case ir::Operator::_or:
        emit_load_int(fout, in.op1, "t0"); emit_load_int(fout, in.op2, "t1");
        fout << "\tor t2, t0, t1\n\tsnez t2, t2\n"; emit_store_int(fout, in.des, "t2"); break;
    case ir::Operator::alloc: {
        int bytes = parse_int_like_literal(in.op1.name) * 4;
        int ml = parse_int_like_literal(in.op1.name);
        int ptr_off = ensure_slot(in.des);
        // Low-address base: smallest s0 offset in this array (same as C / sylib / IR executor).
        int arr_off = g_ctx->next_off;
        g_ctx->next_off -= bytes;
        emit_addi_any(fout, "t0", "s0", arr_off);
        emit_addi_any(fout, "t0", "t0", -((ml - 1) * 4));
        emit_sw_s0(fout, "t0", ptr_off);
        break;
    }
    case ir::Operator::load:
        emit_load_int(fout, in.op1, "t0");
        if (in.op2.type == ir::Type::IntLiteral) emit_addi_any(fout, "t0", "t0", +(parse_int_like_literal(in.op2.name) * 4));
        else { emit_load_int(fout, in.op2, "t1"); fout << "\tslli t1, t1, 2\n\tadd t0, t0, t1\n"; }
        if (is_float_ty(in.des.type)) { fout << "\tflw ft0, 0(t0)\n"; emit_store_float(fout, in.des, "ft0"); }
        else { fout << "\tlw t2, 0(t0)\n"; emit_store_int(fout, in.des, "t2"); }
        break;
    case ir::Operator::store:
        emit_load_int(fout, in.op1, "t0");
        if (in.op2.type == ir::Type::IntLiteral) emit_addi_any(fout, "t0", "t0", +(parse_int_like_literal(in.op2.name) * 4));
        else { emit_load_int(fout, in.op2, "t1"); fout << "\tslli t1, t1, 2\n\tadd t0, t0, t1\n"; }
        if (is_float_ty(in.des.type)) { emit_load_float(fout, in.des, "ft0"); fout << "\tfsw ft0, 0(t0)\n"; }
        else { emit_load_int(fout, in.des, "t2"); fout << "\tsw t2, 0(t0)\n"; }
        break;
    case ir::Operator::getptr:
        emit_load_int(fout, in.op1, "t0");
        if (in.op2.type == ir::Type::IntLiteral) emit_addi_any(fout, "t2", "t0", +(parse_int_like_literal(in.op2.name) * 4));
        else { emit_load_int(fout, in.op2, "t1"); fout << "\tslli t1, t1, 2\n\tadd t2, t0, t1\n"; }
        emit_store_int(fout, in.des, "t2");
        break;
    case ir::Operator::_goto: {
        int cur = g_pc[&in];
        int dst = cur + parse_int_like_literal(in.des.name);
        if (in.op1.name == "null") fout << "\tj " << lbl(g_ctx->func_name, dst) << "\n";
        else {
            if (is_float_ty(in.op1.type)) {
                emit_load_float(fout, in.op1, "ft0");
                fout << "\tfmv.x.w t0, ft0\n";
            } else {
                emit_load_int(fout, in.op1, "t0");
            }
            fout << "\tbne t0, x0, " << lbl(g_ctx->func_name, dst) << "\n";
        }
        break;
    }
    case ir::Operator::call: {
        // IR stores CallInst* as Instruction*; avoid dynamic_cast (some builds disable RTTI).
        const auto& c = static_cast<const ir::CallInst&>(in);
        {
            for (size_t i = 0; i < c.argumentList.size(); ++i) {
                const auto& a = c.argumentList[i];
                if (is_float_ty(a.type)) {
                    if (i < 8) { emit_load_float(fout, a, "ft0"); fout << "\tfmv.s fa" << i << ", ft0\n"; }
                    else { emit_load_float(fout, a, "ft0"); fout << "\tfsw ft0, " << ((i - 8) * 4) << "(sp)\n"; }
                } else {
                    if (i < 8) { emit_load_int(fout, a, "t0"); fout << "\tmv a" << i << ", t0\n"; }
                    else { emit_load_int(fout, a, "t0"); fout << "\tsw t0, " << ((i - 8) * 4) << "(sp)\n"; }
                }
            }
        }
        fout << "\tcall " << in.op1.name << "\n";
        // Void calls must not spill garbage from a0/fa0 into a synthetic temp (semantic always allocates a temp name).
        if (in.des.name != "null" && in.des.type != ir::Type::null) {
            if (is_float_ty(in.des.type)) emit_store_float(fout, in.des, "fa0");
            else emit_store_int(fout, in.des, "a0");
        }
        break;
    }
    case ir::Operator::_return:
        if (in.op1.name != "null") {
            if (is_float_ty(in.op1.type)) { emit_load_float(fout, in.op1, "fa0"); }
            else { emit_load_int(fout, in.op1, "a0"); }
        }
        fout << "\tj " << lbl(g_ctx->func_name, static_cast<int>(g_pc.size())) << "\n";
        break;
    default:
        break;
    }
}