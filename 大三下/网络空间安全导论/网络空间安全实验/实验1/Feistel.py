import hashlib
import random

# 1. 密钥生成函数：用于生成每轮用到的子密钥
def Get_Key(K, n):
    # 将 int 转换为二进制编码，去掉开头 0b 
    K_bin = bin(K)[2:]
    # 计算每轮密钥的大小 
    K_size = len(K_bin) // n
    # 将原始密钥分成多个块作为子密钥 
    Kis = [K_bin[K_size * j : K_size * (j + 1)] for j in range(n)]
    return Kis

# 2. 轮函数：使用 SHA256 增加混淆性 
def F(R, K):
    # 输入需要转换为字节串，得到 SHA256 哈希对象 
    hash_object = hashlib.sha256(str(K).encode('utf-8') + str(R).encode('utf-8'))
    # 将哈希对象压缩为固定长度的字节串 
    R_hashed = hash_object.digest()
    # 将字节串转换为二进制字符串输出 
    bin_R_hashed = bin(int.from_bytes(R_hashed, byteorder='big'))
    return bin_R_hashed

# 3. 字符串转二进制串：按 16 位补齐以确保 L 和 R 长度相等
def str2bin(message):
    bin_M = "".join(format(ord(c), '016b') for c in message)
    return bin_M

# 4. 二进制串转字符串：按 8 位一组还原 
def bin2str(bin_M):
    # 去掉开头的 0b 
    bin_M = bin_M[2:].zfill((len(bin_M) + 7) // 8 * 8)
    message = ""
    for i in range(0, len(bin_M), 8):
        bin_substr = bin_M[i : i + 8]
        n = int(bin_substr, 2)
        message += chr(n)
    return message

# 5. Feistel 网络加密函数
def Feistel_encode(M, K, round=16):
    bin_M = str2bin(M)
    # 分割出 L0 和 R0 
    mid = len(bin_M) // 2
    L = [int(bin_M[:mid], 2)]
    R = [int(bin_M[mid:], 2)]
    Kis = Get_Key(K, round)
    
    for i in range(round):
        preR = R[-1]
        preL = L[-1]
        curL = preR # L_i = R_{i-1} [cite: 169]
        # R_i = L_{i-1} XOR F(R_{i-1}, K_i) 
        curR = preL ^ int(F(preR, Kis[i]), 2)
        L.append(curL)
        R.append(curR)
    
    # 拼接最后的左右部分作为密文 
    return str(L[-1]) + "|" + str(R[-1])

# 6. Feistel 网络解密函数
def Feistel_decode(C, K, round=16):
    # 将密文拆回 L 和 R
    parts = C.split("|")
    L = [int(parts[0])]
    R = [int(parts[1])]
    Kis = Get_Key(K, round)
    
    for i in range(round):
        preR = R[-1]
        preL = L[-1]
        curR = preL # 解密逆向过程 
        # 解密时子密钥逆序使用 
        curL = preR ^ int(F(preL, Kis[round - i - 1]), 2)
        L.append(curL)
        R.append(curR)
    
    # 还原二进制并转回字符串
    M = bin2str(bin(L[-1])) + bin2str(bin(R[-1]))
    return M

# --- 实验测试主程序  ---
if __name__ == '__main__':
    plaintext = "CQUINFORMATIONSECURITYEXP"
    round_num = 16
    # 随机生成 8 位密钥 
    master_key = random.randint(10000000, 99999999)
    
    print(f"原始明文: {plaintext}")
    
    # 执行加密 
    ciphertext = Feistel_encode(plaintext, master_key, round_num)
    print(f"加密密文: {ciphertext}")
    
    # 执行解密 
    decrypted_text = Feistel_decode(ciphertext, master_key, round_num)
    print(f"解密结果: {decrypted_text}")