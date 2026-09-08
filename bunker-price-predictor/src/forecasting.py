from typing import Dict, Any, Optional, List
import pandas as pd
from datetime import datetime
from src.config import settings
from src.database import fetch_prices_as_df, get_connection
from src.prophet_model import ProphetModelWrapper
from src.xgboost_model import XGBoostModelWrapper
from src.ensemble import EnsembleForecaster
from src.preprocessing import prepare_prophet_data

class ForecastingPipeline:
    def __init__(self, fuel_type: str = settings.FUEL_TYPE):
        self.fuel_type = fuel_type
        self.prophet_model: Optional[ProphetModelWrapper] = None
        self.xgboost_model: Optional[XGBoostModelWrapper] = None
        self.ensemble = EnsembleForecaster()

    def load_models(self) -> None:
        self.prophet_model = ProphetModelWrapper.load(fuel_type=self.fuel_type)
        self.xgboost_model = XGBoostModelWrapper.load(fuel_type=self.fuel_type)

    def run_inference(
        self,
        horizon_days: int = 7,
        manual_regressors: Optional[Dict[str, List[float]]] = None
    ) -> Dict[str, Any]:
        if not self.prophet_model or not self.xgboost_model:
            self.load_models()

        hist_df = fetch_prices_as_df(self.fuel_type)
        if hist_df.empty:
            raise ValueError(f"No database price records found for {self.fuel_type}.")

        p_hist = prepare_prophet_data(hist_df, available_regressors=self.prophet_model.regressors)
        
        # Inferences
        p_forecast = self.prophet_model.predict(
            p_hist,
            horizon_days=horizon_days,
            manual_regressors=manual_regressors
        )
        x_forecast = self.xgboost_model.predict_recursive(
            hist_df,
            horizon_days=horizon_days,
            manual_regressors=manual_regressors
        )

        combined = self.ensemble.combine(p_forecast, x_forecast)
        latest_price = float(hist_df["bunker_price"].iloc[-1])
        latest_timestamp = hist_df["timestamp"].iloc[-1]

        # Log predictions into SQLite
        conn = get_connection()
        cur = conn.cursor()
        now_iso = datetime.utcnow().isoformat()
        for _, row in combined.iterrows():
            cur.execute("""
            INSERT INTO forecast_records (
                execution_time, fuel_type, target_timestamp, horizon_days,
                prophet_pred, xgboost_pred, ensemble_pred, lower_bound, upper_bound
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                now_iso, self.fuel_type, row["ds"].strftime("%Y-%m-%d"), horizon_days,
                row["yhat"], row["yhat_xgb"], row["ensemble_yhat"],
                row["ensemble_lower"], row["ensemble_upper"]
            ))
        conn.commit()
        conn.close()

        final_pred = float(combined["ensemble_yhat"].iloc[-1])
        pct_change = round(((final_pred - latest_price) / latest_price) * 100.0, 2)
        trend = "BULLISH" if pct_change > 0.5 else ("BEARISH" if pct_change < -0.5 else "SIDEWAYS")

        return {
            "fuel": self.fuel_type,
            "as_of_date": latest_timestamp.strftime("%Y-%m-%d"),
            "latest_price": latest_price,
            "horizon_days": horizon_days,
            "point_forecast": round(final_pred, 2),
            "lower_bound": round(float(combined["ensemble_lower"].iloc[-1]), 2),
            "upper_bound": round(float(combined["ensemble_upper"].iloc[-1]), 2),
            "price_change_pct": pct_change,
            "trend": trend,
            "timeline": [
                {
                    "date": r["ds"].strftime("%Y-%m-%d"),
                    "prophet": round(r["yhat"], 2),
                    "xgboost": round(r["yhat_xgb"], 2),
                    "ensemble": round(r["ensemble_yhat"], 2),
                    "lower_bound": round(r["ensemble_lower"], 2),
                    "upper_bound": round(r["ensemble_upper"], 2),
                }
                for _, r in combined.iterrows()
            ]
        }