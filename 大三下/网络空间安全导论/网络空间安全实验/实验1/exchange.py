def get_conversion_table(ciphertext, plaintext):
    # 过滤掉非字母字符，确保只处理 A-Z
    c_list = [c for c in ciphertext if c.isalpha()]
    p_list = [p for p in plaintext if p.isalpha()]
    
    if len(c_list) != len(p_list):
        print("错误：字母数量不匹配！")
        return
    
    mapping = {}
    for c, p in zip(c_list, p_list):
        mapping[c] = p
        
    # 按照密文字母 A-Z 排序输出
    print("--- 转换关系表 ---")
    for k in sorted(mapping.keys()):
        print(f"{k} -> {mapping[k]}")

# --- 必须包含下面这两行，程序才会执行 ---
ct = "UZ QSO VUOHXMOPV GPOZPEVSG ZWSZ OPFPESX UDBMETSX AIZ VUEPHZ HMDZSHZO WSFP APPD TSVP QUZW YMXUZUHSX EPYEPOPDZSZUFPO MB ZWP FUPZ HMDJ UD TMOHMQ"
pt = "IT WAS DISCLOSED YESTERDAY THAT SEVERAL INFORMAL BUT DIRECT CONTACTS HAVE BEEN MADE WITH POLITICAL REPRESENTATIVES OF THE VIET CONG IN MOSCOW"

get_conversion_table(ct, pt)