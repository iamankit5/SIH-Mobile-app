import json
from pathlib import Path
from typing import List, Optional, Tuple, Dict, Any
import pandas as pd
import numpy as np
from prophet import Prophet
from prophet.serialize import model_to_json, model_from_json
from src.config import settings

class ProphetModelWrapper:
    def __init__(self, fuel_type: str = settings.FUEL_TYPE, regressors: Optional[List[str]] = None):
        self.fuel_type = fuel_type
        self.regressors = regressors or []
        self.model: Optional[Prophet] = None
        self.is_fitted: bool = False

    def build_model(self) -> Prophet:
        m = Prophet(
            changepoint_prior_scale=settings.PROPHET_CHANGEPOINT_PRIOR_SCALE,
            seasonality_prior_scale=settings.PROPHET_SEASONALITY_PRIOR_SCALE,
            interval_width=settings.PROPHET_INTERVAL_WIDTH,
            daily_seasonality=False,
            weekly_seasonality=True,
            yearly_seasonality=True,
        )
        for reg in self.regressors:
            m.add_regressor(reg, prior_scale=10.0, mode="additive")
        return m

    def fit(self, df: pd.DataFrame) -> "ProphetModelWrapper":
        if len(df) < 30:
            raise ValueError(f"Insufficient training rows ({len(df)}). Minimum required is 30.")
        self.model = self.build_model()
        self.model.fit(df)
        self.is_fitted = True
        return self

    def project_future_regressors(
        self,
        historical_df: pd.DataFrame,
        horizon_days: int,
        manual_regressors: Optional[Dict[str, List[float]]] = None,
        strategy: str = settings.FUTURE_REGRESSOR_STRATEGY
    ) -> pd.DataFrame:
        """
        Creates the future dataframe and transparently constructs future regressor assumptions.
        """
        if not self.is_fitted or self.model is None:
            raise RuntimeError("Model is not fitted yet.")

        future = self.model.make_future_dataframe(periods=horizon_days, freq="D")
        
        for reg in self.regressors:
            if manual_regressors and reg in manual_regressors:
                provided = manual_regressors[reg]
                if len(provided) < horizon_days:
                    raise ValueError(f"Manual regressor '{reg}' has {len(provided)} values, requires {horizon_days}.")
                # Assign historical actuals + manual futures
                future.loc[:len(historical_df)-1, reg] = historical_df[reg].values
                future.loc[len(historical_df):, reg] = provided[:horizon_days]
            else:
                last_val = historical_df[reg].dropna().iloc[-1]
                future.loc[:len(historical_df)-1, reg] = historical_df[reg].values
                if strategy == "persistence":
                    future.loc[len(historical_df):, reg] = last_val
                elif strategy == "linear_drift":
                    rolling_drift = historical_df[reg].diff().tail(14).mean()
                    drift_steps = np.arange(1, horizon_days + 1) * (0.0 if np.isnan(rolling_drift) else rolling_drift)
                    future.loc[len(historical_df):, reg] = last_val + drift_steps
        
        return future

    def predict(
        self,
        historical_df: pd.DataFrame,
        horizon_days: int,
        manual_regressors: Optional[Dict[str, List[float]]] = None
    ) -> pd.DataFrame:
        if not self.is_fitted or self.model is None:
            raise RuntimeError("Model is not fitted yet.")

        future_df = self.project_future_regressors(
            historical_df=historical_df,
            horizon_days=horizon_days,
            manual_regressors=manual_regressors
        )
        forecast = self.model.predict(future_df)
        return forecast[["ds", "yhat", "yhat_lower", "yhat_upper"]].tail(horizon_days).reset_index(drop=True)

    def save(self, directory: Optional[Path] = None) -> Path:
        if not self.is_fitted or self.model is None:
            raise RuntimeError("Cannot save unfitted model.")
        save_dir = (directory or settings.MODELS_DIR) / self.fuel_type
        save_dir.mkdir(parents=True, exist_ok=True)
        model_path = save_dir / "prophet_model.json"
        meta_path = save_dir / "prophet_metadata.json"

        with open(model_path, "w") as f:
            f.write(model_to_json(self.model))
        
        with open(meta_path, "w") as f:
            json.dump({"fuel_type": self.fuel_type, "regressors": self.regressors}, f)
        
        return model_path

    @classmethod
    def load(cls, directory: Optional[Path] = None, fuel_type: str = settings.FUEL_TYPE) -> "ProphetModelWrapper":
        load_dir = (directory or settings.MODELS_DIR) / fuel_type
        model_path = load_dir / "prophet_model.json"
        meta_path = load_dir / "prophet_metadata.json"

        if not model_path.exists() or not meta_path.exists():
            raise FileNotFoundError(f"Missing Prophet checkpoint at {load_dir}")

        with open(meta_path, "r") as f:
            meta = json.load(f)

        instance = cls(fuel_type=meta["fuel_type"], regressors=meta["regressors"])
        with open(model_path, "r") as f:
            instance.model = model_from_json(f.read())
        instance.is_fitted = True
        return instance