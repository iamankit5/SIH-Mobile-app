from typing import Dict, Any, Optional
import pandas as pd
import numpy as np
from src.config import settings

class EnsembleForecaster:
    def __init__(
        self,
        prophet_weight: float = settings.PROPHET_WEIGHT,
        xgboost_weight: float = settings.XGBOOST_WEIGHT
    ):
        total = prophet_weight + xgboost_weight
        if total <= 0:
            raise ValueError("Ensemble weights sum must be strictly positive.")
        self.prophet_weight = prophet_weight / total
        self.xgboost_weight = xgboost_weight / total

    def combine(
        self,
        prophet_forecast: pd.DataFrame,
        xgboost_forecast: pd.DataFrame
    ) -> pd.DataFrame:
        """
        Merges prophet (ds, yhat, yhat_lower, yhat_upper) and xgboost (ds, yhat_xgb).
        Uncertainty bounds are derived from Prophet's modeled error distribution
        calibrated around the ensemble's weighted centroid.
        """
        merged = pd.merge(prophet_forecast, xgboost_forecast, on="ds", how="inner")
        
        merged["ensemble_yhat"] = (
            self.prophet_weight * merged["yhat"] + 
            self.xgboost_weight * merged["yhat_xgb"]
        )

        # Statistical confidence spread from Prophet
        half_width = (merged["yhat_upper"] - merged["yhat_lower"]) / 2.0
        merged["ensemble_lower"] = merged["ensemble_yhat"] - half_width
        merged["ensemble_upper"] = merged["ensemble_yhat"] + half_width

        return merged[["ds", "yhat", "yhat_xgb", "ensemble_yhat", "ensemble_lower", "ensemble_upper"]]