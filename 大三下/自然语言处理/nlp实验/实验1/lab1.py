# ==========================================
# HMM Viterbi 算法自编码实现
# 场景：天气预测
# ==========================================

def viterbi(obs_seq, states, start_p, trans_p, emit_p):
    """
    Viterbi 算法实现
    :param obs_seq: 观测序列 (List)
    :param states: 隐状态集合 (Tuple/List)
    :param start_p: 初始概率 (Dict)
    :param trans_p: 转移概率 (Dict)
    :param emit_p: 发射概率 (Dict)
    :return: 最大的 delta 值 (最终概率), 最优隐状态序列 (List), 完整的 delta 矩阵 (List of Dicts)
    """
    
    # 【鲁棒性改进】处理空观测序列的边界情况
    if not obs_seq:
        return 0.0, [], []
        
    # V 用于存储每一步的 delta 值 (局部最大概率)
    V = [{}]
    # path 用于记录到达当前状态的最优路径，方便最终回溯
    path = {}
    
    # ----------------------------------------
    # 步骤 1：初始化 (t = 0)
    # ----------------------------------------
    for y in states:
        # delta_1(i) = pi_i * b_i(o_1)
        V[0][y] = start_p[y] * emit_p[y][obs_seq[0]]
        path[y] = [y] # 初始化每条路径的起点
        
    # ----------------------------------------
    # 步骤 2：递推计算 (t > 0)
    # ----------------------------------------
    for t in range(1, len(obs_seq)):
        V.append({})
        new_path = {}
        
        for y in states:
            # 遍历寻找前一个时刻所有状态 y0 到当前状态 y 的最大概率
            (prob, state) = max(
                (V[t-1][y0] * trans_p[y0][y] * emit_p[y][obs_seq[t]], y0)
                for y0 in states
            )
            
            # 记录当前时刻 t 状态 y 的最大 delta 值
            V[t][y] = prob
            # 更新最优路径：将前驱状态 state 的最优路径加上当前状态 y
            new_path[y] = path[state] + [y]
            
        # 更新整个 path 字典为当前时刻的最优路径字典
        path = new_path
        
    # ----------------------------------------
    # 步骤 3 & 4：终止与回溯
    # ----------------------------------------
    # 使用 Pythonic 的 [-1] 获取最后一个时刻的数据
    (final_prob, final_state) = max((V[-1][y], y) for y in states)
    
    # 返回: 最终概率, 最优路径, 以及整个过程的 delta 矩阵
    return final_prob, path[final_state], V


# ==========================================
# 实验数据定义与测试执行
# ==========================================

if __name__ == "__main__":
    # 1. 定义 HMM 模型参数
    states = ('Sunny', 'Rainy')
    
    start_probability = {
        'Sunny': 0.6, 
        'Rainy': 0.4
    }
    
    transition_probability = {
        'Sunny': {'Sunny': 0.8, 'Rainy': 0.2},
        'Rainy': {'Sunny': 0.4, 'Rainy': 0.6}
    }
    
    emission_probability = {
        'Sunny': {'walk': 0.6, 'shop': 0.3, 'clean': 0.1},
        'Rainy': {'walk': 0.1, 'shop': 0.4, 'clean': 0.5}
    }
    
    # 2. 定义任务中需要预测的 3 个观测序列
    test_sequences = [
        ['walk', 'shop', 'clean'],
        ['clean', 'shop', 'shop', 'walk'],
        ['shop', 'clean', 'shop', 'walk'],
        []  # 额外增加一个空序列测试，验证鲁棒性
    ]
    
    # 3. 运行算法并打印格式化的结果 (用于实验报告截图)
    print("="*60)
    print("隐马尔可夫模型 Viterbi 算法预测结果")
    print("="*60)
    
    for i, seq in enumerate(test_sequences):
        print(f"\n【任务 {i+1}】")
        print(f"观测序列 (O) : {seq}")
        
        # 调用 Viterbi 函数
        max_prob, best_path, delta_matrix = viterbi(
            seq, states, start_probability, transition_probability, emission_probability
        )
        
        # 打印各时刻的 Delta 值 (仅当序列非空时打印)
        if seq:
            print("各时刻 Delta (δ) 值:")
            for t, deltas in enumerate(delta_matrix):
                print(f"  t={t} (观测: {seq[t]:<5}): ", end="")
                for state, prob in deltas.items():
                    print(f"δ({state})={prob:.6f}  ", end="")
                print()
            
        print("-" * 40)
        print(f"最优隐状态序列 (I) : {best_path}")
        print(f"最终最大 Delta 值 (P) : {max_prob:.8f}")
        
    print("\n" + "="*60)