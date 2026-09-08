import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score

# 1. Load the processed data
df = pd.read_csv("processed_freight_training_data.csv", index_col=0, parse_dates=True)

# 2. Define Features (X) and Target (y)
# Ensure features only contain information available prior to or at t-1
if 'BDI_Lag1' not in df.columns:
    df['BDI_Lag1'] = df['Dry_Bulk_Index'].shift(1)
    df['BDI_MA7'] = df['Dry_Bulk_Index'].shift(1).rolling(7).mean()
    df['BDI_MA30'] = df['Dry_Bulk_Index'].shift(1).rolling(30).mean()
    df = df.dropna()

X = df.drop(columns=['Dry_Bulk_Index'])
y = df['Dry_Bulk_Index']

# 3. Chronological Split (80% Train, 20% Out-of-Sample Test) - NEVER SHUFFLE TIME SERIES
split_idx = int(len(df) * 0.8)
X_train, X_test = X.iloc[:split_idx], X.iloc[split_idx:]
y_train, y_test = y.iloc[:split_idx], y.iloc[split_idx:]

# 4. Common-sense Naive Persistence Benchmark ("Tomorrow's rate = Today's rate")
# For rows where BDI_Lag1 is present:
if 'BDI_Lag1' in X_test.columns:
    naive_predictions = X_test['BDI_Lag1']
else:
    naive_predictions = df['Dry_Bulk_Index'].shift(1).iloc[split_idx:]
naive_rmse = mean_squared_error(y_test, naive_predictions, squared=False)
naive_mae = mean_absolute_error(y_test, naive_predictions)

# 5. Train the Machine Learning Model (Random Forest Regressor)
model = RandomForestRegressor(n_estimators=100, random_state=42, max_depth=8)
model.fit(X_train, y_train)

# 6. Make Predictions and Evaluate Generalization
train_preds = model.predict(X_train)
test_preds = model.predict(X_test)

train_rmse = mean_squared_error(y_train, train_preds, squared=False)
train_mae = mean_absolute_error(y_train, train_preds)
train_r2 = r2_score(y_train, train_preds)

test_rmse = mean_squared_error(y_test, test_preds, squared=False)
test_mae = mean_absolute_error(y_test, test_preds)
test_r2 = r2_score(y_test, test_preds)

print("=" * 60)
print("       MODEL EVALUATION & HONESTY BENCHMARK REPORT")
print("=" * 60)
print(f"1. Naive Persistence Baseline (Common Sense Guess):")
print(f"   - RMSE: {naive_rmse:.2f} | MAE: {naive_mae:.2f}")
print("-" * 60)
print(f"2. Random Forest Regressor (Zero Data Leakage):")
print(f"   - Train: RMSE = {train_rmse:.2f} | MAE = {train_mae:.2f} | R² = {train_r2:.3f}")
print(f"   - Test:  RMSE = {test_rmse:.2f} | MAE = {test_mae:.2f} | R² = {test_r2:.3f}")
print("-" * 60)

# Overfit/Underfit Diagnostic
if train_r2 > 0.95 and test_r2 < 0.50:
    diagnostic = "OVERFIT (Memorized training data, poor test generalization)"
elif test_r2 < 0.20 and train_r2 < 0.20:
    diagnostic = "UNDERFIT (Model failed to capture market signal)"
else:
    diagnostic = "HEALTHY GENERALIZATION (Balanced train/test performance)"

print(f"Diagnostic Assessment: {diagnostic}")
if test_rmse <= naive_rmse:
    improvement = ((naive_rmse - test_rmse) / naive_rmse) * 100.0
    print(f"Beat Naive Baseline: YES (Won by {improvement:.1f}% lower RMSE)")
else:
    gap = ((test_rmse - naive_rmse) / naive_rmse) * 100.0
    print(f"Beat Naive Baseline: NO (Lost by {gap:.1f}% higher RMSE — Naive Persistence benchmark wins)")
print("=" * 60)