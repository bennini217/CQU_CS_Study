'''
鸢尾花数据集
150 个样本,按 7:3 划分为训练集和测试集
3 个类别('setosa', 'versicolor', 'virginica'),
4 个属性('sepal length','sepal width','petal length','petal width'), 训练神经网络模型并测试准确率
呈现训练过程,绘制分类结果,评估模型准确性
'''

import numpy as np
import matplotlib.pyplot as plt
from BPnn import BPNN
import pandas as pd

# 读取数据并将其按 7:3 划分为训练集和测试集
df = pd.read_csv('iris.csv')
label_dic = {'setosa': 0, 'versicolor': 1, 'virginica': 2}  # 标签序列化

data_train = []  # 训练集属性
data_test = []  # 测试集属性
label_train = []  # 训练集标签
label_test = []  # 测试集标签
labeln_test = []  # 测试集标签(分类值)

for i in range(len(df)):
    row = df.iloc[i].values
    one_hot = [0] * len(label_dic)  # 样本分类标签的 one-hot 向量
    label = label_dic[row[5]] # 映射(第6列是Species)
    one_hot[label] = 1
    row = row[1:5]  # 只保留特征值(Sepal.Length到Petal.Width)
    
    if (i % 10 < 7):
        data_train.append(row)
        label_train.append(one_hot)
    else:
        data_test.append(row)
        label_test.append(one_hot)
        labeln_test.append(label)

data_train = np.array(data_train)
data_test = np.array(data_test)
label_train = np.array(label_train)
label_test = np.array(label_test)
labeln_test = np.array(labeln_test)

# 实例化 BPNN 对象
d = len(data_train[0])
l = len(label_dic)
n = len(df)
ep = 1000
q = 10

bpnn = BPNN(data_train, label_train, q, l, ep, 0.001)
cst = bpnn.Train()  # 训练过程的损失函数
pred = bpnn.Pred(data_test)  # 训练后的分类结果

# 计算综合准确率
acc = 0
for i in range(len(pred)):
    if (pred[i] == labeln_test[i]):
        acc += 1
acc /= len(pred)
print('\nAccuracy of classification: %.2f%%' % (acc * 100))

