import numpy as np
import warnings
import matplotlib.pyplot as plt

# 忽略警告、设置中文字体显示
warnings.filterwarnings('ignore')
plt.rcParams['font.sans-serif'] = ['DejaVu Sans', 'Arial Unicode MS', 'SimHei']
plt.rcParams['axes.unicode_minus'] = False

class SimpleKMeans:
    """K-means算法实现"""

    def __init__(self, k=3, max_iter=100, random_state=None):
        self.k = k
        self.max_iter = max_iter
        self.random_state = random_state
        self.centroids = None
        self.labels = None
        self.sse = None
        self.n_iter = 0

    def init_centroids(self, X, method='random_range'):
        """初始化中心点"""
        if self.random_state is not None:
            np.random.seed(self.random_state)

        n_samples, n_features = X.shape

        if method == 'random_points':
            # 随机选择数据点作为初始化中心
            indices = np.random.choice(n_samples, self.k, replace=False)
            centroids = X[indices].copy()
        elif method == 'random_range':
            # 在数据范围内随机生成中心点
            centroids = np.random.uniform(X.min(axis=0), X.max(axis=0), size=(self.k, n_features))

        return centroids

    def assign_clusters(self, X, centroids):
        """分配每个点到最近的中心 - 纯粹基于距离计算"""
        n_samples = X.shape[0]
        labels = np.zeros(n_samples, dtype=int)

        for i, point in enumerate(X):
            distances = [np.sqrt(np.sum((point - centroid) ** 2)) for centroid in centroids]
            labels[i] = np.argmin(distances)

        return labels

    def update_centroids(self, X, labels):
        """更新中心点 - 计算每个簇的均值"""
        centroids = np.zeros((self.k, X.shape[1]))
        for i in range(self.k):
            cluster_points = X[labels == i]
            if len(cluster_points) > 0:
                centroids[i] = cluster_points.mean(axis=0)
            else:
                # 如果某个簇为空，重新随机初始化该中心点
                centroids[i] = X[np.random.randint(len(X))]
        return centroids

    def calculate_sse(self, X, labels, centroids):
        """计算簇内平方误差和"""
        sse = 0
        for i in range(self.k):
            cluster_points = X[labels == i]
            if len(cluster_points) > 0:
                sse += np.sum((cluster_points - centroids[i]) ** 2)
        return sse

    def fit(self, X, init_method='random_points', verbose=False):
        """训练模型"""
        if verbose:
            print(f"开始K-means训练（K={self.k}，初始化={init_method}）")

        # 步骤1：初始化中心点
        self.centroids = self.init_centroids(X, init_method)

        if verbose:
            print(f"初始中心点：")
            for i, c in enumerate(self.centroids):
                print(f"  簇{i+1}: [{' ,'.join([f'{x:.3f}' for x in c])}]")

        for iteration in range(self.max_iter):
            if verbose:
                print(f"\n第{iteration+1}轮迭代:")

            # 步骤2：根据距离分配样本到最近的簇
            old_labels = self.labels.copy() if self.labels is not None else None
            self.labels = self.assign_clusters(X, self.centroids)

            if verbose:
                cluster_counts = [np.sum(self.labels == i) for i in range(self.k)]
                print(f"  各簇样本数: {cluster_counts}")

            # 步骤3：更新中心点
            new_centroids = self.update_centroids(X, self.labels)

            # 步骤4：检查收敛（中心点不再显著变化）
            centroid_shift = np.sqrt(np.sum((new_centroids - self.centroids) ** 2))
            if verbose:
                print(f"  中心点变化量: {centroid_shift:.6f}")

            if centroid_shift < 1e-6:
                if verbose:
                    print(f"  算法收敛！")
                break

            self.centroids = new_centroids
            self.n_iter = iteration + 1

        # 计算最终的SSE
        self.sse = self.calculate_sse(X, self.labels, self.centroids)

        if verbose:
            print(f"\n训练完成:")
            print(f"  最终SSE: {self.sse:.3f}")
            print(f"  迭代次数: {self.n_iter}")
            print(f"  最终中心点:")
            for i, c in enumerate(self.centroids):
                print(f"  簇{i+1}: [{' ,'.join([f'{x:.3f}' for x in c])}]")

        return self
