import pytest
import pandas as pd
import numpy as np
from src.preprocessing import prepare_prophet_data, build_xgboost_features
from src.ensemble import EnsembleForecaster

@pytest.fixture
def dummy_price_data():
    dates = pd.date_range("2025-01-01", periods=100, freq="D")
    prices = 500.0 + np.cumsum(np.random.normal(0, 2, 100))
    brent = 75.0 + np.cumsum(np.random.normal(0, 0.5, 100))
    return pd.DataFrame({
        "timestamp": dates,
        "bunker_price": prices,
        "brent_crude": brent
    })

def test_prophet_data_structure(dummy_price_data):
    p_data = prepare_prophet_data(dummy_price_data, available_regressors=["brent_crude"])
    assert "ds" in p_data.columns
    assert "y" in p_data.columns
    assert "brent_crude" in p_data.columns
    assert len(p_data) == 100

def test_xgboost_no_leakage(dummy_price_data):
    featured, feature_cols = build_xgboost_features(dummy_price_data, lags=[1, 7], rolling_windows=[7])
    assert pd.isna(featured["rolling_mean_7"].iloc[0])
    assert featured["price_lag_1"].iloc[10] == dummy_price_data["bunker_price"].iloc[9]

def test_ensemble_math():
    ensemble = EnsembleForecaster(prophet_weight=0.6, xgboost_weight=0.4)
    dates = pd.date_range("2026-09-01", periods=3, freq="D")
    
    p_df = pd.DataFrame({"ds": dates, "yhat": [100.0, 100.0, 100.0], "yhat_lower": [90.0, 90.0, 90.0], "yhat_upper": [110.0, 110.0, 110.0]})
    x_df = pd.DataFrame({"ds": dates, "yhat_xgb": [200.0, 200.0, 200.0]})

    res = ensemble.combine(p_df, x_df)
    assert np.isclose(res["ensemble_yhat"].iloc[0], 140.0)
    assert np.isclose(res["ensemble_lower"].iloc[0], 130.0)
    assert np.isclose(res["ensemble_upper"].iloc[0], 150.0)