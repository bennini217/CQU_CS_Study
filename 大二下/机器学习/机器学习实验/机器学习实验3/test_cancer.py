
from sklearn.datasets import load_breast_cancer
import numpy as np
from dtree_classifier import build_tree, predict

data = load_breast_cancer()
X = data.data[:, :2]  # 使用两个特征离散化处理
y = data.target
X_discrete = (X > np.mean(X, axis=0)).astype(int)
feature_names = ['feat1 > mean', 'feat2 > mean']

tree = build_tree(X_discrete, y, feature_names)
correct = sum(predict(tree, list(x), feature_names) == y[i] for i, x in enumerate(X_discrete))
print("accuracy:", correct / len(y))
