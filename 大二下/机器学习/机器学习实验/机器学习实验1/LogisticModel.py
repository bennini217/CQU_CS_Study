# 、
# import sys
import numpy as np
# from scipy.special import expit

# 创建一个类

class LogisticModel():

    # 初始化参数
    '''args:
    X(m*n):np 数组的样本集,包含 m 个样本,每个样本包含 n 个属性
    y:标签集
    m/n:样本数/属性数
    w:权重向量
    X_new:扩展后的样本集
    epoch_times:梯度下降法中的迭代次数
    visible:训练过程是否可视化
    alpha:梯度下降法中的学习率
    '''
    def __init__(self, x, y, num_iter, alpha):
        self.x = x
        self.y = y
        # m/n:样本数/属性数
        self.m = self.x.shape[0]
        self.n = self.x.shape[1]
        self.w = self.Init_wgt()
        self.x_new = self.Add_bias()
        # 迭代次数
        self.num_iter = num_iter
        self.alpha = alpha


    def Add_bias(self):
        x_new = np.ones((self.m,self.n+1))
        x_new[:,1:] = self.x
        return x_new
    
    def Init_wgt(self): # 初始化参数
        w = np.random.uniform(-1.0, 1.0, size=self.n+1)
        w.reshape(self.n+1, 1)
        return w
    
    def Sigmoid(self, z): # 激活函数
        return 1/(1+np.exp(-z))
    
    def Sigmoid_gradient(self, z): # 激活函数的梯度
        sg = self.Sigmoid(z)
        return sg * (1-sg)  
    
    def Cost(self, w, x): # 计算损失函数
        cst = 0.0
        z = np.matmul(x, w)
        for i in range(self.m):
            hw = self.Sigmoid(z[i])
            cst  += self.y[i] * np.log(hw) + (1 - self.y[i]) * np.log(1 - hw)
        cst /= (-self.m)
        return cst
    
    # 梯度下降法计算损失函数最小值 
    def Gradient(self, w, x): # 按公式求梯度
        cnt = [0] * (self.n + 1)
        
        for i in range(self.m):
            hw = self.Sigmoid(np.dot(x[i], w))
            cnt += (hw - self.y[i]) * x[i]
        cnt /= self.m
        cnt = np.array(cnt)
        cnt.reshape(self.n + 1, 1)
        return cnt
    
    def GD(self):  # 梯度下降求解 w,并记录损失函数的下降过程
        cst = [self.Cost(self.w, self.x_new)]
        for i in range(self.num_iter):
            delta = self.Gradient(self.w, self.x_new)
            self.w -= self.alpha * delta
            cst.append(self.Cost(self.w, self.x_new))
        return cst     
        
# 检查
# arr = np.array([[1,2],[3,4],[5,6],[7,8],[9,10]])
# y = np.array([0.1,0.2,0.3,0.4,0.5])
# lm = LogisticModel(arr, y, 1000, 0.001)
# cst = lm.GD()
# print(cst[950:])

