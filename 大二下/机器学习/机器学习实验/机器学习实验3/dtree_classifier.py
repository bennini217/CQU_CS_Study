
import numpy as np
from collections import Counter
from math import log2

def entropy(y):
    counter = Counter(y)
    total = len(y)
    return -sum((count / total) * log2(count / total) for count in counter.values())

def info_gain(X_column, y):
    total_entropy = entropy(y)
    values = set(X_column)
    weighted_entropy = sum(
        (X_column == v).sum() / len(X_column) * entropy(y[X_column == v])
        for v in values
    )
    return total_entropy - weighted_entropy

def build_tree(X, y, feature_names):
    if len(set(y)) == 1:
        return y[0]
    if len(feature_names) == 0:
        return Counter(y).most_common(1)[0][0]

    gains = [info_gain(X[:, i], y) for i in range(X.shape[1])]
    best_idx = np.argmax(gains)
    best_feat = feature_names[best_idx]

    tree = {best_feat: {}}
    for value in set(X[:, best_idx]):
        idx = X[:, best_idx] == value
        subtree = build_tree(X[idx][:, np.arange(X.shape[1]) != best_idx],
                             y[idx],
                             feature_names[:best_idx] + feature_names[best_idx+1:])
        tree[best_feat][value] = subtree
    return tree

def predict(tree, sample, feature_names):
    if not isinstance(tree, dict):
        return tree
    feat = next(iter(tree))
    idx = feature_names.index(feat)
    value = sample[idx]
    subtree = tree[feat].get(value)
    if subtree is None:
        return None
    return predict(subtree, sample[:idx] + sample[idx+1:], feature_names[:idx] + feature_names[idx+1:])
