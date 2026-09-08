import pandas as pd
import numpy as np
from typing import List, Dict, Any, Tuple, Optional
from sklearn.metrics import mean_absolute_error, mean_squared_error
from src.preprocessing import prepare_prophet_data
from src.prophet_model import ProphetModelWrapper
from src.xgboost_model import XGBoostModelWrapper
from src.ensemble import EnsembleForecaster
from src.config import settings

def calculate_metrics(actual: np.ndarray, predicted: np.ndarray) -> Dict[str, float]:
    actual, predicted = np.array(actual), np.array(predicted)
    mae = float(mean_absolute_error(actual, predicted))
    rmse = float(np.sqrt(mean_squared_error(actual, predicted)))
    # Safe MAPE avoiding division by zero
    non_zero = actual != 0
    mape = float(np.mean(np.abs((actual[non_zero] - predicted[non_zero]) / actual[non_zero])) * 100.0)
    return {"MAE": round(mae, 2), "RMSE": round(rmse, 2), "MAPE": round(mape, 2)}

def walk_forward_backtest(
    cleaned_df: pd.DataFrame,
    horizon_days: int = 7,
    n_splits: int = 4,
    fuel_type: str = settings.FUEL_TYPE,
    regressors: Optional[List[str]] = None
) -> Tuple[pd.DataFrame, Dict[str, Dict[str, float]]]:
    """
    Strict out-of-sample expanding window time-series cross validation.
    """
    total_len = len(cleaned_df)
    min_train_len = 180  # Minimum 6 months of daily data
    
    if total_len < min_train_len + (horizon_days * n_splits):
        raise ValueError(
            f"Dataset length ({total_len}) insufficient for {n_splits} test windows of {horizon_days} days. "
            f"Required: >= {min_train_len + (horizon_days * n_splits)}"
        )

    test_records = []
    prophet_preds_all, xgb_preds_all, ensemble_preds_all, actuals_all = [], [], [], []

    step_offset = (total_len - min_train_len) // n_splits

    for i in range(n_splits):
        train_end_idx = min_train_len + (i * step_offset)
        test_end_idx = min(train_end_idx + horizon_days, total_len)

        train_data = cleaned_df.iloc[:train_end_idx].copy()
        test_data = cleaned_df.iloc[train_end_idx:test_end_idx].copy()
        
        current_horizon = len(test_data)
        if current_horizon == 0:
            break

        # Fit Prophet on expanding historical slice
        p_train = prepare_prophet_data(train_data, available_regressors=regressors)
        p_wrapper = ProphetModelWrapper(fuel_type=fuel_type, regressors=regressors)
        p_wrapper.fit(p_train)
        p_pred_df = p_wrapper.predict(p_train, horizon_days=current_horizon)

        # Fit XGBoost on expanding historical slice
        x_wrapper = XGBoostModelWrapper(fuel_type=fuel_type, regressors=regressors)
        x_wrapper.fit(train_data)
        x_pred_df = x_wrapper.predict_recursive(train_data, horizon_days=current_horizon)

        # Combine via Ensemble
        ens = EnsembleForecaster()
        ens_df = ens.combine(p_pred_df, x_pred_df)

        actuals = test_data["bunker_price"].values
        actuals_all.extend(actuals)
        prophet_preds_all.extend(ens_df["yhat"].values)
        xgb_preds_all.extend(ens_df["yhat_xgb"].values)
        ensemble_preds_all.extend(ens_df["ensemble_yhat"].values)

        for d, act, py, xy, ey in zip(
            test_data["timestamp"],
            actuals,
            ens_df["yhat"],
            ens_df["yhat_xgb"],
            ens_df["ensemble_yhat"]
        ):
            test_records.append({
                "split": i + 1,
                "date": d,
                "actual": act,
                "prophet": py,
                "xgboost": xy,
                "ensemble": ey
            })

    results_df = pd.DataFrame(test_records)
    
    summary = {
        "Prophet": calculate_metrics(np.array(actuals_all), np.array(prophet_preds_all)),
        "XGBoost": calculate_metrics(np.array(actuals_all), np.array(xgb_preds_all)),
        "Ensemble": calculate_metrics(np.array(actuals_all), np.array(ensemble_preds_all)),
    }

    return results_df, summary