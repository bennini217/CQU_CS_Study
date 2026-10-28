import numpy as np

tang_file = np.load("tang.npz",allow_pickle=True)
tang_file.files

data = tang_file['data']
word2ix = tang_file['word2ix'].item()
idx2word = tang_file['ix2word'].item()

def idx2poem(idx_poem):
    poem = []
    for id in idx_poem:
        poem.append(idx2word[id])
    return "".join(poem)

idx2poem(data[314])

word2ix["</s>"]

poems = []
for poem in data:
    for index,ix in enumerate(poem):
        if ix == word2ix["</s>"]:
            continue
        else:
            break
    poems.append(poem[index:])

# del data
poems[0],idx2poem(poems[0]),len(idx2word)

seq_len = 48
X = []
Y = []
poems_data = [j for i in poems for j in i]

for i in range(0,len(poems_data) - seq_len -1,seq_len):
    X.append(poems_data[i:i+seq_len])
    Y.append(poems_data[i+1:i+seq_len+1])

from torch.utils.data import DataLoader,Dataset
import torch
class PoemDataset(Dataset):
    def __init__(self,X,Y):
        self.X = X
        self.Y = Y
        self.len = len(X)
    def __getitem__(self,index):
        x = np.array(X[index])
        y = np.array(Y[index])
        return torch.from_numpy(x).long(),torch.from_numpy(y).long()
    def __len__(self):
        return self.len
        
data_loader = DataLoader(PoemDataset(X,Y),batch_size=512,num_workers=2)

a,b = next(iter(data_loader))
a.shape,b.shape

import torch
import torch.nn.functional as F
import torch.nn as nn
class PoemNet(nn.Module):
    def __init__(self, vocab_size, embedding_dim, hidden_dim):
        """
            vocab_size：训练集合字典大小（8293）
            embedding_dim：word2vec的维度
            hidden_dim：LSTM的hidden_dim
        """
        super(PoemNet, self).__init__()
        self.hidden_dim = hidden_dim
        self.embeddings = nn.Embedding(vocab_size, embedding_dim)
        self.lstm = nn.LSTM(embedding_dim, self.hidden_dim,batch_first=True)

        self.fc = nn.Sequential(
            nn.Linear(self.hidden_dim,2048),
            nn.ReLU(),
            nn.Dropout(0.25),
            nn.Linear(2048,4096),
            nn.Dropout(0.2),
            nn.ReLU(),
            nn.Linear(4096,vocab_size),
        )

    def forward(self, input,hidden=None):
        """
            input：输入的诗词
            hidden：在生成诗词的时候需要使用，在pytorch中，如果不指定初始状态h_0和C_0，则其默认为0.
            pytorch的LSTM的输出是(output,(h_n,c_n))。实际上，output就是h_1,h_2,……h_n
        """
        embeds = self.embeddings(input)
        batch_size, seq_len = input.size()
        if hidden is None:
            output, hidden = self.lstm(embeds)
        else:
            # h_0,c_0 = hidden
            output, hidden = self.lstm(embeds,hidden)

        output = self.fc(output)
        output = output.reshape(batch_size * seq_len, -1)
        output = F.log_softmax(output,dim=1)
        return output,hidden

vocab_size = len(word2ix.keys()) # 8293
embedding_dim = 200
hidden_dim = 1024

device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
my_net = PoemNet(vocab_size,embedding_dim,hidden_dim).to(device)

import torch.optim as optim
import matplotlib.pyplot as plt # 导入画图库

optimzer = optim.Adam(my_net.parameters(),lr=0.001)
loss_function = nn.CrossEntropyLoss()

epoch_losses = [] # 用于记录每个epoch的平均loss，方便画图

for epoch in range(5):
    my_net.train()
    losses = []
    for i,data in enumerate(data_loader):
        # 请从这儿开始补充代码
        # 1. 获取输入数据x和标签y，并迁移到对应设备 (GPU/CPU)
        x, y = data[0].to(device), data[1].to(device)
        
        # 2. 梯度清零
        optimzer.zero_grad()
        
        # 3. 前向传播，获取预测结果和隐藏状态
        output, _ = my_net(x)
        
        # 4. 计算损失。因为output被reshape成了二维，y也需要被展平为一维
        loss = loss_function(output, y.view(-1))
        
        # 5. 反向传播，计算梯度
        loss.backward()
        
        # 6. 更新网络参数
        optimzer.step()
        
        # 记录当前的loss值
        losses.append(loss.item())
        # 补充代码结束
        
    # 计算当前epoch的平均loss并打印
    avg_loss = sum(losses)/len(losses)
    epoch_losses.append(avg_loss)
    print(f"Epoch {epoch+1}, Average Loss: {avg_loss:.4f}")

# 训练结束后保存模型
torch.save(my_net,"model.h5")

# 绘制并显示Loss曲线 (运行到这里会弹出一张图，记得截图保存到实验报告)
plt.plot(range(1, 6), epoch_losses, marker='o', color='b')
plt.title('Training Loss over Epochs')
plt.xlabel('Epoch')
plt.ylabel('Loss')
plt.grid(True)
plt.show()