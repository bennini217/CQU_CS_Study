from collections import Counter

def analyze_uppercase_frequency(text):
    # 1. 过滤出所有大写字母
    uppercase_chars = [char for char in text if char.isupper()]
    total_count = len(uppercase_chars)

    if total_count == 0:
        print("字符串中没有大写字母。")
        return

    # 2. 统计频次
    counts = Counter(uppercase_chars)
    
    # 3. 按频次（及字母顺序）排序：counts.most_common() 已经按频次降序排列
    sorted_counts = counts.most_common()

    print(f"{'字母':<5} | {'频次':<5} | {'百分比'}")
    print("-" * 25)

    for char, count in sorted_counts:
        percentage = (count / total_count) * 100
        print(f"{char:<6} | {count:<6} | {percentage:.2f}%")

# 测试数据
sample_text = "UZ QSO VUOHXMOPV GPOZPEVSG ZWSZ OPFPESX UDBMETSX AIZ VUEPHZ HMDZSHZO WSFPF APPD TSVP QUZW YMXUZUHSX EPYEPOPDZSZUFPO MB ZWP FUPZ HMDJ UD TMOHMQ"
analyze_uppercase_frequency(sample_text)