
# 决策树回归示例：加州房价与学生分数预测
from sklearn.datasets import fetch_california_housing
from sklearn.tree import DecisionTreeRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error
import pandas as pd

# 加州房价
housing = fetch_california_housing()
X_train, X_test, y_train, y_test = train_test_split(housing.data, housing.target, test_size=0.3, random_state=42)
reg1 = DecisionTreeRegressor(max_depth=6)
reg1.fit(X_train, y_train)
y_pred = reg1.predict(X_test)
print("California 房价预测 MSE:", mean_squared_error(y_test, y_pred))

# 学生分数预测
try:
    data = pd.read_csv("StudentsPerformance.csv")
    data = pd.get_dummies(data.drop(columns=["gender", "race/ethnicity", "parental level of education", "lunch", "test preparation course"]))
    X = data.drop("math score", axis=1)
    y = data["math score"]
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    reg2 = DecisionTreeRegressor(max_depth=5)
    reg2.fit(X_train, y_train)
    y_pred2 = reg2.predict(X_test)
    print("学生分数预测 MSE:", mean_squared_error(y_test, y_pred2))
except FileNotFoundError:
    print("缺少 StudentsPerformance.csv 文件，请下载后放在当前目录下。")
