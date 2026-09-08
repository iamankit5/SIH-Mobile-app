import pandas as pd
import numpy as np
from typing import List, Tuple, Optional
from src.config import settings

def prepare_prophet_data(df: pd.DataFrame, available_regressors: Optional[List[str]] = None) -> pd.DataFrame:
    prophet_df = pd.DataFrame()
    prophet_df["ds"] = pd.to_datetime(df["timestamp"])
    prophet_df["y"] = df["bunker_price"].values

    if available_regressors:
        for reg in available_regressors:
            if reg in df.columns and df[reg].notna().sum() > 0:
                prophet_df[reg] = df[reg].values

    return prophet_df.dropna(subset=["ds", "y"]).sort_values("ds").reset_index(drop=True)

def build_xgboost_features(
    df: pd.DataFrame,
    lags: List[int] = [1, 2, 3, 7, 14, 30],
    rolling_windows: List[int] = [7, 14, 30],
    available_regressors: Optional[List[str]] = None
) -> Tuple[pd.DataFrame, List[str]]:
    """
    Engineers purely backward-looking features to prevent data leakage.
    Every row t contains features computed strictly using data <= t-1 for autoregression,
    or current known day values for exogenous market variables.
    """
    data = df.sort_values("timestamp").copy().reset_index(drop=True)
    feature_cols: List[str] = []

    # Target
    target = data["bunker_price"]

    # Lag features
    for lag in lags:
        col_name = f"price_lag_{lag}"
        data[col_name] = target.shift(lag)
        feature_cols.append(col_name)

    # Rolling statistics based strictly on shifted values (prevents self-inclusion leakage)
    shifted_target = target.shift(1)
    for window in rolling_windows:
        mean_col = f"rolling_mean_{window}"
        std_col = f"rolling_std_{window}"
        data[mean_col] = shifted_target.rolling(window=window, min_periods=max(2, window // 2)).mean()
        data[std_col] = shifted_target.rolling(window=window, min_periods=max(2, window // 2)).std().fillna(0.0)
        feature_cols.extend([mean_col, std_col])

    # Calendar features
    data["day_of_week"] = data["timestamp"].dt.dayofweek
    data["month"] = data["timestamp"].dt.month
    data["day_of_year"] = data["timestamp"].dt.dayofyear
    feature_cols.extend(["day_of_week", "month", "day_of_year"])

    # Optional Exogenous Regressors (Shifted by 1 to represent day-open availability)
    if available_regressors:
        for reg in available_regressors:
            if reg in data.columns:
                col_name = f"{reg}_lag_1"
                data[col_name] = data[reg].shift(1)
                feature_cols.append(col_name)

    return data, feature_cols