#include "opt/optimizer.h"

#include <cmath>
#include <cstdlib>
#include <unordered_set>
#include <unordered_map>

namespace {

struct ConstVal {
    bool is_float = false;
    int i = 0;
    float f = 0.0f;
};

bool is_int_like(ir::Type t) {
    return t == ir::Type::Int || t == ir::Type::IntLiteral;
}

bool is_float_like(ir::Type t) {
    return t == ir::Type::Float || t == ir::Type::FloatLiteral;
}

int eval_int(const std::string& s) {
    if (s.size() > 2 && s[0] == '0' && (s[1] == 'x' || s[1] == 'X')) {
        return static_cast<int>(std::strtol(s.c_str(), nullptr, 16));
    }
    if (s.size() > 2 && s[0] == '0' && (s[1] == 'b' || s[1] == 'B')) {
        return static_cast<int>(std::strtol(s.c_str() + 2, nullptr, 2));
    }
    if (s.size() > 1 && s[0] == '0' && s[1] >= '0' && s[1] <= '7') {
        return static_cast<int>(std::strtol(s.c_str(), nullptr, 8));
    }
    return std::stoi(s);
}

float eval_float(const std::string& s) {
    return std::stof(s);
}

ir::Operand int_lit(int v) {
    return ir::Operand(std::to_string(v), ir::Type::IntLiteral);
}

ir::Operand float_lit(float v) {
    return ir::Operand(std::to_string(v), ir::Type::FloatLiteral);
}

bool lookup_const(const ir::Operand& op,
                  const std::unordered_map<std::string, ConstVal>& env,
                  ConstVal& out) {
    if (op.type == ir::Type::IntLiteral) {
        out.is_float = false;
        out.i = eval_int(op.name);
        return true;
    }
    if (op.type == ir::Type::FloatLiteral) {
        out.is_float = true;
        out.f = eval_float(op.name);
        return true;
    }
    auto it = env.find(op.name);
    if (it == env.end()) return false;
    out = it->second;
    return true;
}

void set_const(std::unordered_map<std::string, ConstVal>& env,
               const std::string& name,
               const ConstVal& val) {
    env[name] = val;
}

void kill_const(std::unordered_map<std::string, ConstVal>& env, const std::string& name) {
    env.erase(name);
}

void kill_all(std::unordered_map<std::string, ConstVal>& env) {
    env.clear();
}

void resolve_operand(ir::Operand& op,
                     const std::unordered_map<std::string, ConstVal>& env) {
    if (op.type == ir::Type::IntLiteral || op.type == ir::Type::FloatLiteral) return;
    ConstVal val;
    if (!lookup_const(op, env, val)) return;
    if (!val.is_float) op = int_lit(val.i);
    else op = float_lit(val.f);
}

bool is_pure_bin(ir::Operator op) {
    static const unsigned long long mask = 0x6ffffda000ULL;
    int v = static_cast<int>(op);
    if (v < 0 || v > 38) return false;
    return (mask >> v) & 1ULL;
}

bool fold_int_bin(ir::Operator op, int a, int b, int& out) {
    switch (op) {
    case ir::Operator::add: out = a + b; return true;
    case ir::Operator::sub: out = a - b; return true;
    case ir::Operator::mul: out = a * b; return true;
    case ir::Operator::div:
        if (b == 0) return false;
        out = a / b;
        return true;
    case ir::Operator::mod:
        if (b == 0) return false;
        out = a % b;
        return true;
    case ir::Operator::lss: out = a < b; return true;
    case ir::Operator::leq: out = a <= b; return true;
    case ir::Operator::gtr: out = a > b; return true;
    case ir::Operator::geq: out = a >= b; return true;
    case ir::Operator::eq: out = a == b; return true;
    case ir::Operator::neq: out = a != b; return true;
    case ir::Operator::_not: out = a == 0; return true;
    case ir::Operator::_and: out = (a != 0) && (b != 0); return true;
    case ir::Operator::_or: out = (a != 0) || (b != 0); return true;
    default: return false;
    }
}

bool fold_float_bin(ir::Operator op, float a, float b, float& out) {
    switch (op) {
    case ir::Operator::fadd: out = a + b; return true;
    case ir::Operator::fsub: out = a - b; return true;
    case ir::Operator::fmul: out = a * b; return true;
    case ir::Operator::fdiv: out = a / b; return true;
    case ir::Operator::flss: out = a < b; return true;
    case ir::Operator::fleq: out = a <= b; return true;
    case ir::Operator::fgtr: out = a > b; return true;
    case ir::Operator::fgeq: out = a >= b; return true;
    case ir::Operator::feq: out = a == b; return true;
    case ir::Operator::fneq: out = a != b; return true;
    default: return false;
    }
}

void replace_with_mov(ir::Instruction& inst, const ir::Operand& src) {
    if (is_float_like(inst.des.type)) {
        inst.op = ir::Operator::fmov;
    } else {
        inst.op = ir::Operator::mov;
    }
    inst.op1 = src;
    inst.op2 = ir::Operand("null", ir::Type::null);
}

void replace_with_def(ir::Instruction& inst, const ir::Operand& lit) {
    if (is_float_like(inst.des.type)) {
        inst.op = ir::Operator::fdef;
    } else {
        inst.op = ir::Operator::def;
    }
    inst.op1 = lit;
    inst.op2 = ir::Operand("null", ir::Type::null);
}

void propagate_def_like(ir::Instruction& inst,
                        std::unordered_map<std::string, ConstVal>& env,
                        bool is_float) {
    if (is_float) {
        if (inst.op1.type == ir::Type::FloatLiteral) {
            ConstVal val;
            val.is_float = true;
            val.f = eval_float(inst.op1.name);
            set_const(env, inst.des.name, val);
            return;
        }
        ConstVal src;
        if (lookup_const(inst.op1, env, src) && src.is_float) {
            set_const(env, inst.des.name, src);
            return;
        }
    } else {
        if (inst.op1.type == ir::Type::IntLiteral) {
            ConstVal val;
            val.is_float = false;
            val.i = eval_int(inst.op1.name);
            set_const(env, inst.des.name, val);
            return;
        }
        ConstVal src;
        if (lookup_const(inst.op1, env, src) && !src.is_float) {
            set_const(env, inst.des.name, src);
            return;
        }
    }
    kill_const(env, inst.des.name);
}

bool try_int_algebraic(ir::Instruction& inst,
                       const ConstVal* a,
                       const ConstVal* b,
                       std::unordered_map<std::string, ConstVal>& env) {
    if (!is_int_like(inst.des.type)) return false;
    switch (inst.op) {
    case ir::Operator::add:
        if (b && !b->is_float && b->i == 0) {
            replace_with_mov(inst, inst.op1);
            propagate_def_like(inst, env, false);
            return true;
        }
        if (a && !a->is_float && a->i == 0) {
            replace_with_mov(inst, inst.op2);
            propagate_def_like(inst, env, false);
            return true;
        }
        break;
    case ir::Operator::sub:
        if (b && !b->is_float && b->i == 0) {
            replace_with_mov(inst, inst.op1);
            propagate_def_like(inst, env, false);
            return true;
        }
        break;
    case ir::Operator::mul:
        if ((a && !a->is_float && a->i == 0) || (b && !b->is_float && b->i == 0)) {
            replace_with_def(inst, int_lit(0));
            ConstVal val;
            val.is_float = false;
            val.i = 0;
            set_const(env, inst.des.name, val);
            return true;
        }
        if (b && !b->is_float && b->i == 1) {
            replace_with_mov(inst, inst.op1);
            propagate_def_like(inst, env, false);
            return true;
        }
        if (a && !a->is_float && a->i == 1) {
            replace_with_mov(inst, inst.op2);
            propagate_def_like(inst, env, false);
            return true;
        }
        break;
    case ir::Operator::div:
        if (b && !b->is_float && b->i == 1) {
            replace_with_mov(inst, inst.op1);
            propagate_def_like(inst, env, false);
            return true;
        }
        break;
    default:
        break;
    }
    return false;
}

bool try_float_algebraic(ir::Instruction& inst,
                         const ConstVal* a,
                         const ConstVal* b,
                         std::unordered_map<std::string, ConstVal>& env) {
    if (!is_float_like(inst.des.type)) return false;
    switch (inst.op) {
    case ir::Operator::fadd:
        if (b && b->is_float && b->f == 0.0f) {
            replace_with_mov(inst, inst.op1);
            propagate_def_like(inst, env, true);
            return true;
        }
        if (a && a->is_float && a->f == 0.0f) {
            replace_with_mov(inst, inst.op2);
            propagate_def_like(inst, env, true);
            return true;
        }
        break;
    case ir::Operator::fsub:
        if (b && b->is_float && b->f == 0.0f) {
            replace_with_mov(inst, inst.op1);
            propagate_def_like(inst, env, true);
            return true;
        }
        break;
    case ir::Operator::fmul:
        if ((a && a->is_float && a->f == 0.0f) || (b && b->is_float && b->f == 0.0f)) {
            replace_with_def(inst, float_lit(0.0f));
            ConstVal val;
            val.is_float = true;
            val.f = 0.0f;
            set_const(env, inst.des.name, val);
            return true;
        }
        if (b && b->is_float && b->f == 1.0f) {
            replace_with_mov(inst, inst.op1);
            propagate_def_like(inst, env, true);
            return true;
        }
        if (a && a->is_float && a->f == 1.0f) {
            replace_with_mov(inst, inst.op2);
            propagate_def_like(inst, env, true);
            return true;
        }
        break;
    case ir::Operator::fdiv:
        if (b && b->is_float && b->f == 1.0f) {
            replace_with_mov(inst, inst.op1);
            propagate_def_like(inst, env, true);
            return true;
        }
        break;
    default:
        break;
    }
    return false;
}

bool optimize_function(ir::Function& func) {
    bool changed = false;
    std::unordered_map<std::string, ConstVal> env;
    std::unordered_set<int> jump_targets;

    for (size_t i = 0; i < func.InstVec.size(); ++i) {
        const auto& inst = *func.InstVec[i];
        if (inst.op == ir::Operator::_goto) {
            int target = static_cast<int>(i) + eval_int(inst.des.name);
            if (target >= 0 && target < static_cast<int>(func.InstVec.size())) {
                jump_targets.insert(target);
            }
        }
    }

    for (size_t i = 0; i < func.InstVec.size(); ++i) {
        ir::Instruction& inst = *func.InstVec[i];
        if (jump_targets.count(static_cast<int>(i))) kill_all(env);
        resolve_operand(inst.op1, env);
        if (inst.op2.name != "null") resolve_operand(inst.op2, env);

        switch (inst.op) {
        case ir::Operator::def:
            propagate_def_like(inst, env, false);
            break;
        case ir::Operator::fdef:
            propagate_def_like(inst, env, true);
            break;
        case ir::Operator::mov:
            propagate_def_like(inst, env, false);
            break;
        case ir::Operator::fmov:
            propagate_def_like(inst, env, true);
            break;
        case ir::Operator::addi:
        case ir::Operator::subi: {
            int imm = eval_int(inst.op2.name);
            ConstVal lhs;
            if (lookup_const(inst.op1, env, lhs) && !lhs.is_float) {
                int out = (inst.op == ir::Operator::addi) ? lhs.i + imm : lhs.i - imm;
                replace_with_def(inst, int_lit(out));
                ConstVal val;
                val.is_float = false;
                val.i = out;
                set_const(env, inst.des.name, val);
                changed = true;
            } else if (inst.op == ir::Operator::addi && imm == 0) {
                replace_with_mov(inst, inst.op1);
                propagate_def_like(inst, env, false);
                changed = true;
            } else if (inst.op == ir::Operator::subi && imm == 0) {
                replace_with_mov(inst, inst.op1);
                propagate_def_like(inst, env, false);
                changed = true;
            } else {
                kill_const(env, inst.des.name);
            }
            break;
        }
        case ir::Operator::cvt_i2f: {
            ConstVal src;
            if (lookup_const(inst.op1, env, src) && !src.is_float) {
                replace_with_def(inst, float_lit(static_cast<float>(src.i)));
                ConstVal val;
                val.is_float = true;
                val.f = static_cast<float>(src.i);
                set_const(env, inst.des.name, val);
                changed = true;
            } else {
                kill_const(env, inst.des.name);
            }
            break;
        }
        case ir::Operator::cvt_f2i: {
            ConstVal src;
            if (lookup_const(inst.op1, env, src) && src.is_float) {
                int out = static_cast<int>(std::trunc(src.f));
                replace_with_def(inst, int_lit(out));
                ConstVal val;
                val.is_float = false;
                val.i = out;
                set_const(env, inst.des.name, val);
                changed = true;
            } else {
                kill_const(env, inst.des.name);
            }
            break;
        }
        default:
            if (is_pure_bin(inst.op)) {
                ConstVal a, b;
                bool ha = lookup_const(inst.op1, env, a);
                bool hb = lookup_const(inst.op2, env, b);
                if (ha && hb && a.is_float == b.is_float) {
                    if (!a.is_float) {
                        int out = 0;
                        if (fold_int_bin(inst.op, a.i, b.i, out)) {
                            replace_with_def(inst, int_lit(out));
                            ConstVal val;
                            val.is_float = false;
                            val.i = out;
                            set_const(env, inst.des.name, val);
                            changed = true;
                            break;
                        }
                    } else {
                        float out = 0.0f;
                        if (fold_float_bin(inst.op, a.f, b.f, out)) {
                            replace_with_def(inst, float_lit(out));
                            ConstVal val;
                            val.is_float = true;
                            val.f = out;
                            set_const(env, inst.des.name, val);
                            changed = true;
                            break;
                        }
                    }
                }
                const ConstVal* pa = ha ? &a : nullptr;
                const ConstVal* pb = hb ? &b : nullptr;
                if (try_int_algebraic(inst, pa, pb, env)) {
                    changed = true;
                    break;
                }
                if (try_float_algebraic(inst, pa, pb, env)) {
                    changed = true;
                    break;
                }
                kill_const(env, inst.des.name);
            } else if (inst.op == ir::Operator::store ||
                       inst.op == ir::Operator::load ||
                       inst.op == ir::Operator::getptr ||
                       inst.op == ir::Operator::alloc) {
                kill_const(env, inst.des.name);
            } else if (inst.op == ir::Operator::call) {
                kill_all(env);
                if (inst.des.name != "null") kill_const(env, inst.des.name);
            } else if (inst.op == ir::Operator::_goto) {
                if (inst.op1.name != "null") {
                    ConstVal cond;
                    if (lookup_const(inst.op1, env, cond) && !cond.is_float && cond.i != 0) {
                        inst.op1 = ir::Operand("null", ir::Type::null);
                        changed = true;
                    }
                }
                kill_all(env);
            } else if (inst.op == ir::Operator::_return) {
                kill_all(env);
            } else {
                break;
            }
            break;
        }
    }
    return changed;
}

bool has_result(const ir::Instruction& inst) {
    return inst.des.name != "null" && inst.des.type != ir::Type::null;
}

bool is_removable_def(const ir::Instruction& inst) {
    switch (inst.op) {
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
        return has_result(inst);
    default:
        return false;
    }
}

bool uses_operand(const ir::Instruction& inst, const std::string& name) {
    auto same = [&](const ir::Operand& op) {
        return op.name == name && op.type != ir::Type::IntLiteral &&
               op.type != ir::Type::FloatLiteral && op.type != ir::Type::null;
    };
    if (same(inst.op1) || same(inst.op2)) return true;
    if (inst.op == ir::Operator::store && same(inst.des)) return true;
    if (inst.op == ir::Operator::call) {
        const auto& c = static_cast<const ir::CallInst&>(inst);
        for (const auto& arg : c.argumentList) {
            if (same(arg)) return true;
        }
    }
    return false;
}

void erase_inst(ir::Instruction& inst) {
    inst.op = ir::Operator::__unuse__;
    inst.op1 = ir::Operand("null", ir::Type::null);
    inst.op2 = ir::Operand("null", ir::Type::null);
    inst.des = ir::Operand("null", ir::Type::null);
}

bool remove_overwritten_defs(ir::Function& func) {
    bool changed = false;
    std::unordered_map<std::string, ir::Instruction*> last_def;

    for (auto* raw : func.InstVec) {
        ir::Instruction& inst = *raw;

        for (auto it = last_def.begin(); it != last_def.end();) {
            if (uses_operand(inst, it->first)) it = last_def.erase(it);
            else ++it;
        }

        if (inst.op == ir::Operator::_goto ||
            inst.op == ir::Operator::_return ||
            inst.op == ir::Operator::call ||
            inst.op == ir::Operator::store) {
            last_def.clear();
        }

        if (!is_removable_def(inst)) continue;

        auto it = last_def.find(inst.des.name);
        if (it != last_def.end()) {
            erase_inst(*it->second);
            changed = true;
        }
        last_def[inst.des.name] = &inst;
    }
    return changed;
}

void count_use(const ir::Operand& op, std::unordered_map<std::string, int>& uses) {
    if (op.type == ir::Type::IntLiteral || op.type == ir::Type::FloatLiteral ||
        op.type == ir::Type::null || op.name == "null") {
        return;
    }
    ++uses[op.name];
}

bool is_temp_name(const std::string& name) {
    return name.size() > 1 && name[0] == 't' && name[1] >= '0' && name[1] <= '9';
}

bool remove_unused_temps(ir::Function& func) {
    std::unordered_map<std::string, int> uses;
    for (auto* raw : func.InstVec) {
        const ir::Instruction& inst = *raw;
        count_use(inst.op1, uses);
        count_use(inst.op2, uses);
        if (inst.op == ir::Operator::store) count_use(inst.des, uses);
        if (inst.op == ir::Operator::call) {
            const auto& c = static_cast<const ir::CallInst&>(inst);
            for (const auto& arg : c.argumentList) count_use(arg, uses);
        }
    }

    bool changed = false;
    for (auto* raw : func.InstVec) {
        ir::Instruction& inst = *raw;
        if (is_removable_def(inst) && is_temp_name(inst.des.name) && uses[inst.des.name] == 0) {
            erase_inst(inst);
            changed = true;
        }
    }
    return changed;
}

}

namespace opt {

void optimize(ir::Program& program) {
    bool changed = true;
    for (int round = 0; changed && round < 8; ++round) {
        changed = false;
        for (auto& func : program.functions) {
            if (optimize_function(func)) changed = true;
        }
    }
    for (auto& func : program.functions) {
        remove_overwritten_defs(func);
    }
    bool removed = true;
    for (int round = 0; removed && round < 8; ++round) {
        removed = false;
        for (auto& func : program.functions) {
            if (remove_unused_temps(func)) removed = true;
        }
    }
}

}
