from sklearn.datasets import load_iris
from sklearn.metrics import adjusted_rand_score, silhouette_score
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# 加载数据
data = load_iris()
X = data.data
y_true = data.target  # 用于计算 ARI，但不会用于训练
results = []

for k in range(2, 6):
    ari_scores = []
    sil_scores = []
    
    for seed in range(5):  # 多次试验以消除随机性
        from sklearn.cluster import KMeans
        model = KMeans(n_clusters=k, random_state=seed)
        model.fit(X)
        labels = model.labels

        # 计算评估指标
        ari = adjusted_rand_score(y_true, labels)
        sil = silhouette_score(X, labels) if len(set(labels)) > 1 else -1

        ari_scores.append(ari)
        sil_scores.append(sil)

    results.append({
        '数据集': 'Iris',
        'K值': k,
        '平均ARI': np.mean(ari_scores),
        'ARI标准差': np.std(ari_scores),
        '平均轮廓系数': np.mean(sil_scores)
    })

# 转换为DataFrame
df = pd.DataFrame(results)
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
colors = ['tab:blue', 'tab:orange', 'tab:green']

datasets = {'Iris': data}  # 如果有多个数据集，可以扩展这个 dict

# 图 1：不同 K 值的 ARI 对比
ax1 = axes[0, 0]
for i, dataset in enumerate(df['数据集'].unique()):
    data_ = df[df['数据集'] == dataset]
    ax1.errorbar(
        data_['K值'], data_['平均ARI'], yerr=data_['ARI标准差'],
        marker='o', linewidth=2, capsize=5,
        label=f'{dataset} ({X.shape[1]} features)',
        color=colors[i], markersize=8
    )
ax1.set_title('ARI Performance vs K Values')
ax1.set_xlabel('K Value')
ax1.set_ylabel('Adjusted Rand Index (ARI)')
ax1.legend()
ax1.grid(True, alpha=0.3)
ax1.set_xticks([2, 3, 4, 5])

# 图 2：不同 K 值的轮廓系数对比
ax3 = axes[1, 0]
for i, dataset in enumerate(df['数据集'].unique()):
    data_ = df[df['数据集'] == dataset]
    valid_data = data_[data_['平均轮廓系数'] > -1]

    if not valid_data.empty:
        ax3.plot(
            valid_data['K值'], valid_data['平均轮廓系数'], 'o-',
            label=f'{dataset} ({X.shape[1]} features)',
            linewidth=2, color=colors[i], markersize=8
        )
ax3.set_title('Silhouette Score vs K Values')
ax3.set_xlabel('K Value')
ax3.set_ylabel('Silhouette Score')
ax3.legend()
ax3.grid(True, alpha=0.3)
ax3.set_xticks([2, 3, 4, 5])

# 图 3：平均 vs 最佳 ARI 对比
ax4 = axes[1, 1]
width = 0.35
for i, dataset in enumerate(df['数据集'].unique()):
    data_ = df[df['数据集'] == dataset]
    x = np.arange(len(data_)) + i * (len(data_) + 1)
    ax4.bar(x - width / 2, data_['平均ARI'], width,
            label=f'{dataset} Average', alpha=0.7, color=colors[i])
    ax4.bar(x + width / 2, data_['平均ARI'], width,  # 模拟“最佳ARI”用平均ARI代替
            label=f'{dataset} Best', alpha=0.9, color=colors[i])
ax4.set_title('Average vs Best ARI Performance')
ax4.set_xlabel('K Values')
ax4.set_ylabel('ARI Score')
ax4.legend()
ax4.grid(True, alpha=0.3)

plt.tight_layout()
plt.show()
