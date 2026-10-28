#include"backend/generator.h"

#include<assert.h>
#include<cstdint>
#include<cstring>
#include<cmath>
#include<sstream>
#include<algorithm>
#include<unordered_map>
#include<unordered_set>


backend::Generator::Generator(ir::Program& p, std::ofstream& f): program(p), fout(f) {}

namespace {
// Keep it large and handle large immediates safely (see guidebook "如何偷懒" section).
constexpr int RA_OFF = -4;
constexpr int S0_OFF = -8;
constexpr int FIRST_LOCAL_OFF = -12;

struct FuncCtx {
    std::unordered_map<std::string, int> var_off;
    int next_off = FIRST_LOCAL_OFF;
    int frame_size = 16;
    std::string func_name;
};

FuncCtx* g_ctx = nullptr;
std::unordered_map<std::string, ir::GlobalVal> g_globals;
std::unordered_map<std::string, const ir::Function*> g_funcs;
std::unordered_map<const ir::Instruction*, int> g_pc;
std::unordered_set<int> g_branch_targets;
std::unordered_map<std::string, int> g_use_count;
std::unordered_map<const ir::Instruction*, int> g_alloc_base;

bool is_float_ty(ir::Type t) {
    return t == ir::Type::Float || t == ir::Type::FloatLiteral;
}

bool is_literal(ir::Type t) {
    return t == ir::Type::IntLiteral || t == ir::Type::FloatLiteral;
}

bool is_temp_name(const std::string& name) {
    return name.size() > 1 && name[0] == 't' && name[1] >= '0' && name[1] <= '9';
}

void count_operand_use(const ir::Operand& op) {
    if (op.name == "null" || is_literal(op.type) || op.type == ir::Type::null) return;
    ++g_use_count[op.name];
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

int align16(int x) {
    return (x + 15) & ~15;
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

bool int_lit_op(const ir::Operand& op, int& out) {
    if (op.type != ir::Type::IntLiteral) return false;
    out = parse_int_like_literal(op.name);
    return true;
}

bool emit_fused_cmp_branch(std::ofstream& fout, const ir::Instruction& cmp, const ir::Instruction& br) {
    if (br.op != ir::Operator::_goto || br.op1.name != cmp.des.name ||
        !is_temp_name(cmp.des.name) || g_use_count[cmp.des.name] != 1) {
        return false;
    }

    int cur = g_pc[&br];
    int dst = cur + parse_int_like_literal(br.des.name);
    std::string target = lbl(g_ctx->func_name, dst);

    switch (cmp.op) {
    case ir::Operator::lss:
        emit_load_int(fout, cmp.op1, "t0");
        emit_load_int(fout, cmp.op2, "t1");
        fout << "\tblt t0, t1, " << target << "\n";
        return true;
    case ir::Operator::leq:
        emit_load_int(fout, cmp.op1, "t0");
        emit_load_int(fout, cmp.op2, "t1");
        fout << "\tbge t1, t0, " << target << "\n";
        return true;
    case ir::Operator::gtr:
        emit_load_int(fout, cmp.op1, "t0");
        emit_load_int(fout, cmp.op2, "t1");
        fout << "\tblt t1, t0, " << target << "\n";
        return true;
    case ir::Operator::geq:
        emit_load_int(fout, cmp.op1, "t0");
        emit_load_int(fout, cmp.op2, "t1");
        fout << "\tbge t0, t1, " << target << "\n";
        return true;
    case ir::Operator::eq:
        emit_load_int(fout, cmp.op1, "t0");
        emit_load_int(fout, cmp.op2, "t1");
        fout << "\tbeq t0, t1, " << target << "\n";
        return true;
    case ir::Operator::neq:
        emit_load_int(fout, cmp.op1, "t0");
        emit_load_int(fout, cmp.op2, "t1");
        fout << "\tbne t0, t1, " << target << "\n";
        return true;
    default:
        return false;
    }
}

bool can_fuse_temp_result_to_mov(const ir::Instruction& in, const ir::Instruction& mov) {
    if (!is_temp_name(in.des.name) || g_use_count[in.des.name] != 1) return false;
    if (mov.op != ir::Operator::mov && mov.op != ir::Operator::fmov) return false;
    if (mov.op1.name != in.des.name) return false;
    switch (in.op) {
    case ir::Operator::def:
    case ir::Operator::fdef:
    case ir::Operator::mov:
    case ir::Operator::fmov:
    case ir::Operator::cvt_i2f:
    case ir::Operator::cvt_f2i:
    case ir::Operator::add:
    case ir::Operator::addi:
    case ir::Operator::fadd:
    case ir::Operator::sub:
    case ir::Operator::subi:
    case ir::Operator::fsub:
    case ir::Operator::mul:
    case ir::Operator::fmul:
    case ir::Operator::div:
    case ir::Operator::fdiv:
    case ir::Operator::mod:
    case ir::Operator::lss:
    case ir::Operator::flss:
    case ir::Operator::leq:
    case ir::Operator::fleq:
    case ir::Operator::gtr:
    case ir::Operator::fgtr:
    case ir::Operator::geq:
    case ir::Operator::fgeq:
    case ir::Operator::eq:
    case ir::Operator::feq:
    case ir::Operator::neq:
    case ir::Operator::fneq:
    case ir::Operator::_not:
    case ir::Operator::_and:
    case ir::Operator::_or:
    case ir::Operator::getptr:
    case ir::Operator::load:
        return true;
    default:
        return false;
    }
}

void prelayout_operand(const ir::Operand& op) {
    ensure_slot(op);
}

void prelayout_function(const ir::Function& f) {
    for (const auto& p : f.ParameterList) {
        prelayout_operand(p);
    }

    for (auto* raw : f.InstVec) {
        const ir::Instruction& in = *raw;
        if (in.op == ir::Operator::alloc) {
            ensure_slot(in.des);
            int bytes = parse_int_like_literal(in.op1.name) * 4;
            g_alloc_base[&in] = g_ctx->next_off;
            g_ctx->next_off -= bytes;
            continue;
        }

        prelayout_operand(in.op1);
        prelayout_operand(in.op2);
        if (in.op == ir::Operator::store) {
            prelayout_operand(in.des);
        } else if (in.op == ir::Operator::call) {
            const auto& c = static_cast<const ir::CallInst&>(in);
            for (const auto& a : c.argumentList) prelayout_operand(a);
            prelayout_operand(in.des);
        } else {
            prelayout_operand(in.des);
        }
    }

    int lowest_used = std::min(-8, g_ctx->next_off + 4);
    g_ctx->frame_size = align16(-lowest_used);
    if (g_ctx->frame_size < 16) g_ctx->frame_size = 16;
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
    g_branch_targets.clear();
    g_use_count.clear();
    g_alloc_base.clear();
    for (size_t i = 0; i < f.InstVec.size(); ++i) g_pc[f.InstVec[i]] = static_cast<int>(i);
    for (size_t i = 0; i < f.InstVec.size(); ++i) {
        const auto& in = *f.InstVec[i];
        count_operand_use(in.op1);
        count_operand_use(in.op2);
        if (in.op == ir::Operator::store) count_operand_use(in.des);
        if (in.op == ir::Operator::call) {
            const auto& c = static_cast<const ir::CallInst&>(in);
            for (const auto& a : c.argumentList) count_operand_use(a);
        }
        if (in.op == ir::Operator::_goto) {
            int dst = static_cast<int>(i) + parse_int_like_literal(in.des.name);
            g_branch_targets.insert(dst);
        }
    }
    prelayout_function(f);

    fout << ".globl " << f.name << "\n";
    fout << f.name << ":\n";
    // prologue (support large frame size)
    emit_addi_any(fout, "sp", "sp", -ctx.frame_size);
    // Save callee-saved regs at the high end of frame.
    emit_sw_sp(fout, "ra", ctx.frame_size + RA_OFF);
    emit_sw_sp(fout, "s0", ctx.frame_size + S0_OFF);
    emit_addi_any(fout, "s0", "sp", ctx.frame_size);

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
        if (i + 1 < f.InstVec.size() && !g_branch_targets.count(static_cast<int>(i + 1)) &&
            emit_fused_cmp_branch(fout, *f.InstVec[i], *f.InstVec[i + 1])) {
            ++i;
            fout << lbl(f.name, static_cast<int>(i)) << ":\n";
            continue;
        }
        if (i + 1 < f.InstVec.size() && !g_branch_targets.count(static_cast<int>(i + 1)) &&
            can_fuse_temp_result_to_mov(*f.InstVec[i], *f.InstVec[i + 1])) {
            ir::Instruction fused = *f.InstVec[i];
            fused.des = f.InstVec[i + 1]->des;
            gen_instr(fused);
            ++i;
            fout << lbl(f.name, static_cast<int>(i)) << ":\n";
            continue;
        }
        gen_instr(*f.InstVec[i]);
    }
    fout << lbl(f.name, static_cast<int>(f.InstVec.size())) << ":\n";
    emit_lw_sp(fout, "ra", ctx.frame_size + RA_OFF);
    emit_lw_sp(fout, "s0", ctx.frame_size + S0_OFF);
    emit_addi_any(fout, "sp", "sp", ctx.frame_size);
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
    case ir::Operator::add: {
        int imm = 0;
        if (int_lit_op(in.op2, imm)) {
            emit_load_int(fout, in.op1, "t0");
            emit_addi_any(fout, "t2", "t0", imm);
            emit_store_int(fout, in.des, "t2");
        } else if (int_lit_op(in.op1, imm)) {
            emit_load_int(fout, in.op2, "t0");
            emit_addi_any(fout, "t2", "t0", imm);
            emit_store_int(fout, in.des, "t2");
        } else {
            gen_bin_i(fout, "add", in);
        }
        break;
    }
    case ir::Operator::sub: {
        int imm = 0;
        if (int_lit_op(in.op2, imm)) {
            emit_load_int(fout, in.op1, "t0");
            emit_addi_any(fout, "t2", "t0", -imm);
            emit_store_int(fout, in.des, "t2");
        } else {
            gen_bin_i(fout, "sub", in);
        }
        break;
    }
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
        int ml = parse_int_like_literal(in.op1.name);
        int ptr_off = ensure_slot(in.des);
        // Low-address base: smallest s0 offset in this array (same as C / sylib / IR executor).
        int arr_off = g_alloc_base[&in];
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
