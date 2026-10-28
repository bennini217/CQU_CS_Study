
import numpy as np

def split(X, y, feature_index, threshold):
    left_idx = X[:, feature_index] <= threshold
    right_idx = X[:, feature_index] > threshold
    return X[left_idx], y[left_idx], X[right_idx], y[right_idx]

def mse(y):
    return np.mean((y - y.mean()) ** 2)

def build_reg_tree(X, y, depth=0, max_depth=3):
    if len(y) < 5 or depth == max_depth:
        return y.mean()
    best_mse = float('inf')
    best_split = None

    for feature in range(X.shape[1]):
        thresholds = np.unique(X[:, feature])
        for threshold in thresholds:
            X_left, y_left, X_right, y_right = split(X, y, feature, threshold)
            if len(y_left) == 0 or len(y_right) == 0:
                continue
            error = mse(y_left) * len(y_left) + mse(y_right) * len(y_right)
            if error < best_mse:
                best_mse = error
                best_split = (feature, threshold, X_left, y_left, X_right, y_right)
    if best_split is None:
        return y.mean()
    f, t, xl, yl, xr, yr = best_split
    return {
        'feature': f,
        'threshold': t,
        'left': build_reg_tree(xl, yl, depth + 1, max_depth),
        'right': build_reg_tree(xr, yr, depth + 1, max_depth)
    }

def predict_reg(tree, x):
    if not isinstance(tree, dict):
        return tree
    if x[tree['feature']] <= tree['threshold']:
        return predict_reg(tree['left'], x)
    else:
        return predict_reg(tree['right'], x)
