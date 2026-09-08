from pathlib import Path
from typing import List, Optional, Tuple, Dict, Any
import pandas as pd
import numpy as np
import joblib
from xgboost import XGBRegressor
from sklearn.preprocessing import StandardScaler
from src.config import settings
from src.preprocessing import build_xgboost_features

class XGBoostModelWrapper:
    def __init__(self, fuel_type: str = settings.FUEL_TYPE, regressors: Optional[List[str]] = None):
        self.fuel_type = fuel_type
        self.regressors = regressors or []
        self.model: Optional[XGBRegressor] = None
        self.scaler = StandardScaler()
        self.feature_names: List[str] = []
        self.is_fitted: bool = False

    def fit(self, df: pd.DataFrame) -> "XGBoostModelWrapper":
        featured_data, features = build_xgboost_features(df, available_regressors=self.regressors)
        # Drop rows with lag/rolling NaN warmup
        clean_train = featured_data.dropna(subset=features + ["bunker_price"]).reset_index(drop=True)

        if len(clean_train) < 30:
            raise ValueError(f"Insufficient training records after feature engineering ({len(clean_train)}).")

        X = clean_train[features]
        y = clean_train["bunker_price"].values

        self.feature_names = features
        X_scaled = self.scaler.fit_transform(X)

        self.model = XGBRegressor(
            n_estimators=settings.XGB_N_ESTIMATORS,
            max_depth=settings.XGB_MAX_DEPTH,
            learning_rate=settings.XGB_LEARNING_RATE,
            subsample=settings.XGB_SUBSAMPLE,
            colsample_bytree=settings.XGB_COLSAMPLE_BYTREE,
            random_state=42,
            n_jobs=-1
        )
        self.model.fit(X_scaled, y)
        self.is_fitted = True
        return self

    def predict_recursive(
        self,
        historical_df: pd.DataFrame,
        horizon_days: int,
        manual_regressors: Optional[Dict[str, List[float]]] = None
    ) -> pd.DataFrame:
        """
        Performs iterative forward multi-step prediction without data leakage.
        Forecasted prices at step t are iteratively used to construct lags for step t+1.
        """
        if not self.is_fitted or self.model is None:
            raise RuntimeError("XGBoost model is not fitted yet.")

        # Work on a running simulation dataframe
        sim_df = historical_df.copy().sort_values("timestamp").reset_index(drop=True)
        last_date = sim_df["timestamp"].iloc[-1]
        
        predictions: List[float] = []
        dates: List[pd.Timestamp] = []

        for step in range(1, horizon_days + 1):
            next_date = last_date + pd.Timedelta(days=step)
            dates.append(next_date)

            # Build fresh features from current historical + previously predicted rows
            featured_data, _ = build_xgboost_features(sim_df, available_regressors=self.regressors)
            last_engineered_row = featured_data.iloc[[-1]][self.feature_names]
            
            # Handle calendar feature adjustments for the exact target step
            last_engineered_row["day_of_week"] = next_date.dayofweek
            last_engineered_row["month"] = next_date.month
            last_engineered_row["day_of_year"] = next_date.dayofyear

            # Handle exogenous regressor updates
            for reg in self.regressors:
                lag_col = f"{reg}_lag_1"
                if lag_col in self.feature_names:
                    if manual_regressors and reg in manual_regressors and len(manual_regressors[reg]) >= step:
                        last_engineered_row[lag_col] = manual_regressors[reg][step - 1]
                    else:
                        last_engineered_row[lag_col] = sim_df[reg].iloc[-1]

            # Scale and infer
            X_step_scaled = self.scaler.transform(last_engineered_row)
            pred_val = float(self.model.predict(X_step_scaled)[0])
            predictions.append(pred_val)

            # Inject the prediction back into sim_df to compute recursive lags for step+1
            new_row = {"timestamp": next_date, "bunker_price": pred_val}
            for reg in self.regressors:
                if manual_regressors and reg in manual_regressors and len(manual_regressors[reg]) >= step:
                    new_row[reg] = manual_regressors[reg][step - 1]
                else:
                    new_row[reg] = sim_df[reg].iloc[-1]

            sim_df = pd.concat([sim_df, pd.DataFrame([new_row])], ignore_index=True)

        return pd.DataFrame({
            "ds": dates,
            "yhat_xgb": predictions
        })

    def get_feature_importances(self) -> Dict[str, float]:
        if not self.is_fitted or self.model is None:
            raise RuntimeError("Model is not fitted.")
        scores = self.model.feature_importances_
        return {feat: float(score) for feat, score in sorted(zip(self.feature_names, scores), key=lambda x: x[1], reverse=True)}

    def save(self, directory: Optional[Path] = None) -> Path:
        if not self.is_fitted or self.model is None:
            raise RuntimeError("Cannot save unfitted model.")
        save_dir = (directory or settings.MODELS_DIR) / self.fuel_type
        save_dir.mkdir(parents=True, exist_ok=True)
        checkpoint_path = save_dir / "xgboost_model.joblib"

        joblib.dump({
            "model": self.model,
            "scaler": self.scaler,
            "feature_names": self.feature_names,
            "regressors": self.regressors,
            "fuel_type": self.fuel_type
        }, checkpoint_path)
        return checkpoint_path

    @classmethod
    def load(cls, directory: Optional[Path] = None, fuel_type: str = settings.FUEL_TYPE) -> "XGBoostModelWrapper":
        load_dir = (directory or settings.MODELS_DIR) / fuel_type
        checkpoint_path = load_dir / "xgboost_model.joblib"
        if not checkpoint_path.exists():
            raise FileNotFoundError(f"Missing XGBoost checkpoint at {checkpoint_path}")

        data = joblib.load(checkpoint_path)
        instance = cls(fuel_type=data["fuel_type"], regressors=data["regressors"])
        instance.model = data["model"]
        instance.scaler = data["scaler"]
        instance.feature_names = data["feature_names"]
        instance.is_fitted = True
        return instance