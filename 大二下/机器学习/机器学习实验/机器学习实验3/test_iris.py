
from sklearn.datasets import load_iris
import numpy as np
from dtree_classifier import build_tree, predict

iris = load_iris()
X = iris.data[:, [0, 2]]  # 使用两个特征简化为离散变量
y = iris.target
X_discrete = (X > np.mean(X, axis=0)).astype(int)
feature_names = ['sepal length > mean', 'petal length > mean']

tree = build_tree(X_discrete, y, feature_names)
correct = sum(predict(tree, list(x), feature_names) == y[i] for i, x in enumerate(X_discrete))
print("Iris_accuracy", correct / len(y))
