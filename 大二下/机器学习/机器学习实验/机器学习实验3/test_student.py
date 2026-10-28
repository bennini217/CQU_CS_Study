
import pandas as pd
from sklearn.model_selection import train_test_split
from dtree_regressor import build_reg_tree, predict_reg
import numpy as np


df = pd.read_csv(r"E:\专业课\机器学习\机器学习实验\机器学习实验3\StudentsPerformance.csv")
df = pd.get_dummies(df.drop(columns=["gender", "race/ethnicity", "parental level of education", "lunch", "test preparation course"]))
X = df.drop("math score", axis=1).values
y = df["math score"].values
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
tree = build_reg_tree(X_train, y_train, max_depth=4)
y_pred = [predict_reg(tree, x) for x in X_test]
print("学生分数预测 MSE:", np.mean((y_test - y_pred) ** 2))