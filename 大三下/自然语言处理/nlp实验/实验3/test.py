import numpy as np
tang_file = np.load("tang.npz",allow_pickle=True)
tang_file.files

word2ix = tang_file['word2ix'].item()
idx2word = tang_file['ix2word'].item()

import torch
device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
my_net = torch.load("model.h5").to(device)

my_net.eval()

def generate_poem(my_words,max_len=128):
    '''
        根据提示词my_words生成一首诗。max_len表示生成诗的最大长度。
    '''
    def __generate_next(idx,hidden=None):
        """
            根据input和hidden输出下一个预测词
        """
        input = torch.Tensor([idx]).view(1,1).long().to(device)
        output,hidden = my_net(input,hidden)
        return output,hidden

    # 初始化hidden状态
    output,hidden = __generate_next(word2ix["<START>"])
    my_words_len = len(my_words)
    result = []
    # 获取整个提示词串的hidden状态
    for word in my_words:
        result.append(word)
        output,hidden = __generate_next(word2ix[word],hidden)
    
    # 找到概率/分数最高的那个字的ID
    _,top_index = torch.max(output,1)
    word = idx2word[top_index[0].item()]
    result.append(word)

    for i in range(max_len-my_words_len):
        #请从这儿开始补充代码
        # 该部分完成诗歌内容的续写、生成，直至达到最大长度或生成结束标记<EOP>，生成内容存入result中
        # 1. 使用上一步预测出的 word，传入网络生成下一个词的概率分布和新的隐藏状态
        output, hidden = __generate_next(word2ix[word], hidden)
        
        # 2. 找到概率/分数最高的那个字的ID
        _, top_index = torch.max(output, 1)
        word_id = top_index[0].item()
        
        # 3. 将ID转回对应的汉字
        word = idx2word[word_id]
        
        # 4. 判断是否生成了结束标记。如果是，则提前停止生成
        if word == "</s>":
            break
            
        # 5. 将生成的字加入结果列表
        result.append(word)
        # 补充代码结束
        
    return "".join(result)

# 调用诗词续写方法generate_poem()
generate_poem("重庆")