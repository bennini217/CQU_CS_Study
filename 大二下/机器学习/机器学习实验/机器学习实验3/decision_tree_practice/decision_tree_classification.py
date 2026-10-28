
# 决策树分类示例：鸢尾花与乳腺癌数据集
from sklearn.datasets import load_iris, load_breast_cancer
from sklearn.tree import DecisionTreeClassifier, plot_tree
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score
import matplotlib.pyplot as plt

# Iris 分类
iris = load_iris()
X_train, X_test, y_train, y_test = train_test_split(iris.data, iris.target, test_size=0.3, random_state=0)
clf = DecisionTreeClassifier()
clf.fit(X_train, y_train)
y_pred = clf.predict(X_test)
print("Iris 分类准确率：", accuracy_score(y_test, y_pred))
plt.figure(figsize=(12, 6))
plot_tree(clf, feature_names=iris.feature_names, class_names=iris.target_names, filled=True)
plt.title("Iris 决策树")
plt.show()

# 乳腺癌分类
cancer = load_breast_cancer()
X_train, X_test, y_train, y_test = train_test_split(cancer.data, cancer.target, test_size=0.3, random_state=1)
clf2 = DecisionTreeClassifier(max_depth=5)
clf2.fit(X_train, y_train)
y_pred2 = clf2.predict(X_test)
print("Breast Cancer 分类准确率：", accuracy_score(y_test, y_pred2))
