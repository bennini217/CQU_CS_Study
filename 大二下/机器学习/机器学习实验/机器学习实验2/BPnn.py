import sys
import numpy as np
from scipy.special import expit

class BPNN(object):
    def __init__(self, X, Y, q, l, epoch_times , Eta ):
        """
        args:
        X(n*d):样本集属性的 np 数组,包含 n 个样本,每个样本包含 d 个属性
        Y(n*l):样本集标签的 np 数组
        gama:隐藏层神经元的阈值[1*q]
        theta:输出层神经元的阈值[1*l]
        n:样本数
        d:属性数
        q:隐藏层中神经元数
        l:分类数
        v:输入层->隐藏层的权重向量[(d+1)*q]
        w:隐藏层->输出层的权重向量[q*l]
        epoch_times:梯度下降法中的迭代次数
        Eta:梯度下降法中的学习率
        visible:训练过程是否可视化
        """
        
        self.X = X
        self.Y = Y
        self.n,self.d = self.X.shape
        self.q = q
        self.l = l
        self.v,self.w = self.Init_wgt()
        self.gama,self.theta = self.Init_threshold()
        self.epoch_times = epoch_times
        self.Eta = Eta
        
    def Add_bias(self,X):
        # 对输入数据添加偏置项
        X_new = np.ones(X.shape[0] + 1)
        X_new[1:] = X   
        return X_new
    
    # 初始化输入层到隐藏层的权重矩阵v和隐藏层到输出层的权重矩阵w
    def Init_wgt(self):
        v = np.random.uniform(-1.0,1.0,size = (self.d + 1)*self.q)
        v = v.reshape(self.d + 1,self.q)
        w = np.random.uniform(-1.0,1.0,size = self.q*self.l)
        w = w.reshape(self.q,self.l)
        return v,w
    
    # 初始化隐藏层神经元的阈值gama和输出层神经元的阈值theta
    def Init_threshold(self):
        gama = np.random.uniform(-1.0,1.0,size = self.q)
        gama = gama.reshape(1,self.q)
        theta = np.random.uniform(-1.0,1.0,size = self.l)
        theta = theta.reshape(1,self.l)
        return gama,theta
    
    # 激活函数
    def Sigmoid(self,z):
        return expit(z)
    
    # 激活函数的梯度
    def Sigmoid_gradient(self,z):
        sg = self.Sigmoid(z)
        return sg * (1 - sg)
    
    # 输入训练样本
    def FP(self, X):
        X_new = self.Add_bias(X)
        alpha = np.matmul(X_new,self.v)
        b = self.Sigmoid(alpha - self.gama)
        beita = np.matmul(b, self.w)
        y = self.Sigmoid(beita - self.theta)
        return alpha, b, beita, y
    
    # 损失函数
    def Get_cost(self, y, label):
        tmp = (y - label)*(y - label)/2
        cost =  np.mean(tmp)
        return cost
    
    # 反向传播
    def GD(self, X, b, y, label):
        g = y*(1-y)*(y - label)
        tmp1 = np.matmul(g,self.w.T)
        e = b*(1-b)*tmp1
        X_new = self.Add_bias(X)
        self.w -= self.Eta * np.matmul(b.T,g)
        self.theta += self.Eta * g
        tmp2 = X_new.T.reshape(-1,1)
        self.v -= self.Eta * np.matmul(tmp2,e)
        self.gama += self.Eta * e
        return self
    
    # 对预测结果分类，取最大值
    def Pred(self, X):
        pred = []
        for i in range(X.shape[0]):
            alpha, b, beita, y = self.FP(X[i])
            pred_tmp = np.argmax(y,axis = -1) # 结果是一个数组
            pred.append(pred_tmp[0]) # 由此返回标量
        return pred
    
    def Train(self):
        costs = []
        for i in range(self.epoch_times):
            cost_tmp = []
            for j in range(len(self.X)):
                alpha, b,beita, y = self.FP(self.X[j])
                cost = self.Get_cost(y, self.Y[j])  
                cost_tmp.append(cost)
                self.GD(self.X[j], b, y, self.Y[j])
            costs.append(np.mean(np.array(cost_tmp)))
        return costs