
from sklearn.datasets import fetch_california_housing
from sklearn.model_selection import train_test_split
from dtree_regressor import build_reg_tree, predict_reg
import numpy as np

data = fetch_california_housing()
X_train, X_test, y_train, y_test = train_test_split(data.data, data.target, test_size=0.2, random_state=42)
tree = build_reg_tree(X_train, y_train, max_depth=4)
y_pred = [predict_reg(tree, x) for x in X_test]
print("加州房价预测 MSE:", np.mean((y_test - y_pred) ** 2))
