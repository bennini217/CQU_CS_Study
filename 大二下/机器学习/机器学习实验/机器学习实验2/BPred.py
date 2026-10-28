import xlrd
from xlrd import open_workbook
import numpy as np
import matplotlib.pyplot as plt
from BPnn import BPNN

# 读取数据并将其按 7:3 划分为训练集和测试集
path = r"E:\专业课\机器学习\机器学习实验\机器学习实验2\winequality_data.xlsx"
book = open_workbook(path)
sheet = book.sheets()[0] 
nrow = sheet.nrows        # 行
ncol = sheet.ncols        # 列

label_dic = {'Bad':0, 'Normal':1, 'Good':2}  # 标签序列化
data_train = []  # 训练集属性
data_test = []   # 测试集属性
label_train = [] # 训练集标签
label_test = []  # 测试集标签
labeln_test = [] # 测试集标签(分类值)

for i in range(1, nrow):
    row = sheet.row_values(i)
    one_hot = [0] * len(label_dic)  # 样本分类标签的 one-hot 向量
    label = label_dic[row[11]]
    one_hot[label] = 1
    row.pop(11)  
    
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

# 实例化 BPNN 
d = len(data_train[0])  
l = len(label_dic)      
n = nrow               
ep = 2000               
q = 20                

bpnn = BPNN(data_train, label_train, q, l, ep, 0.001)
cst = bpnn.Train()      # 训练过程的损失函数
pred = bpnn.Pred(data_test)  # 训练后的分类结果

# 计算综合准确率
acc = 0
for i in range(len(pred)):
    if (pred[i] == labeln_test[i]):
        acc += 1
acc /= len(pred)
print('\nAccuracy of classification: %.2f%%' % (acc * 100))


# # 绘制损失函数
# plt.plot(range(len(cst)), cst)
# plt.ylabel('Cost')
# plt.xlabel('Epoch_times')
# plt.show()