import csv
import numpy as np
import matplotlib.pyplot as plt
from LogisticModel import LogisticModel as LM

# 读取数据集
f = open(r'数据集\3.0a.csv', encoding='UTF-8')
reader = csv.reader(f)
rows = [row for row in reader]
X = []
Y = []
for i in range(1,len(rows)):
    X.append([float(rows[i][1]), float(rows[i][2])])
    Y.append(float(rows[i][3]))
x = np.array(X)
y = np.array(Y)

# 训练数据集
num = 1000000
alpha = 0.001
lm = LM(x, y, num, alpha) #建立对象
cst = lm.GD() #训练
w = lm.w #得到参数

pred_z = []
pred_l = []
acc = 0
for x in lm.x_new:
    z = np.matmul(x,w)
    pred_z.append(z)
    if z >= 0:
        pred_l.append(1)
    else:
        pred_l.append(0)
for i in range(len(pred_l)):
    if (pred_l[i] == Y[i]):
        acc += 1
acc /= len(pred_l)
print('\nAccuracy of classification: %.2f%%' % (acc*100))

pred1 = []
pred0 = []
for i in range(len(pred_l)):
    if (pred_l[i] ==1):
        pred1.append([X[i][0], X[i][1]])
    else:
        pred0.append([X[i][0], X[i][1]])
pred1 = np.array(pred1)
pred0 = np.array(pred0)

# 绘制分类结果 1(预测值+预测标签+真实标签)
plt.plot(range(len(pred_l)), pred_l, pred_z)
plt.plot(range(len(pred_l)), y)
plt.ylabel('Prediction & Label')
plt.xlabel('number of sample')
plt.legend(['Pred_label', 'Pred_value', 'Label'])
plt.show()

# 绘制分类结果 2(真实分类&预测分类)
fig0 = plt.figure(figsize=(10, 5))
ax = plt.subplot(121)  # 真实分类
ax.scatter(X[:8, 0], X[:8, 1])  # 正类
ax.scatter(X[8:, 0], X[8:, 1])  # 负类
ax.set_xlabel('density')
ax.set_ylabel('Sugar content')
ax.set_title("Real label")

ax = plt.subplot(122)  # 预测分类
ax.scatter(pred1[:, 0], pred1[:, 1])
ax.scatter(pred0[:, 0], pred0[:, 1])
ax.set_xlabel('density')
ax.set_ylabel('Sugar content')
ax.set_title("Prediction label")

a = np.linspace(0, 1, 1000)
b = -(w[1]*a + w[0])/w[2]
ax.plot(a, b)
plt.show()

# 绘制损失函数
plt.plot(range(len(cst)), cst)
plt.ylabel('Cost')
plt.xlabel('Epoch_times')
plt.show()