import numpy as np
from sklearn import datasets
import matplotlib.pyplot as plt
from LogisticModel import LogisticModel as LM

def Cal(X, w, c1, c0):
    pred = []
    for x in X:
        x_new = list(x.copy())
        x_new.insert(0, 1)
        z = np.matmul(x_new,w)
        if z>= 0:
            pred.append(c1)
        else:
            pred.append(c0)
    return pred

# 讀取數據集
iris = datasets.load_iris()
data = iris['data'] # 數據集每個對象的特徵
labal = iris['target'] # 標籤 即分類
feature = iris['feature_names'] # 特徵名稱
target = iris['target_names'] # 分類的名稱
target = list(target) # 轉化成數組

# 分類訓練集和測試機
data_train = []#訓練集屬性
data_test = []#測試機屬性
label_train = []#訓練集標籤
label_test = []#測試機標籤
for i in range(len(data)):
    if(i%10<7):
        data_train.append(data[i])
        label_train.append(labal[i])
    else:   
        data_test.append(data[i])
        label_test.append(labal[i])

# 從訓練集劃分類別
k = len(target)
data_train_target = [[] for i in range(k)]
for i in range(len(label_train)):
    data_train_target[label_train[i]].append(data_train[i])
    
# OvO
Pred_l = []# 存放預測結果
num = 100000
for i in range(k):
    for j in range(k):
        if(i==j):
            continue
        xtmp = data_train_target[i].copy()
        xtmp.extend(data_train_target[j])
        ytemp = [1]*len(data_train_target[i])
        ytemp1 = [0]*len(data_train_target[j])
        ytemp.extend(ytemp1)
        x = np.array(xtmp)
        y = np.array(ytemp)
        lm = LM(x, y, num, 0.001)
        cst = lm.GD()
        w = lm.w
        Pred_l.append(Cal(data_test,w,i,j))
        print("Finished training on the basis of Trainset %d and %d."%(i,j))
        
# 最終預測結果
pred_l = []
for i in range(len(Pred_l[0])): # 遍歷列
    l_list = {} # 在不同標籤中的出現次數
    for j in range(len(Pred_l)):
        l_list[Pred_l[j][i]] = l_list.get(Pred_l[j][i],0) + 1
    k,v = max(l_list.items(),key=lambda x:x[1]) # 返回出現次數最多的標籤
    pred_l.append(k)
print(pred_l)

# 計算準確率
acc=0
for i in range(len(pred_l)):
    if(pred_l[i]==label_test[i]):
        acc+=1
acc/=len(label_test)
print('Accuracy of classification: %.2f%%'%(acc*100))