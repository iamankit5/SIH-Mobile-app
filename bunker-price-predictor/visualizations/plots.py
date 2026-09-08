from pathlib import Path
from typing import Optional
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd
from src.database import fetch_prices_as_df
from src.config import settings
from src.forecasting import ForecastingPipeline
from src.xgboost_model import XGBoostModelWrapper

def generate_forecast_dashboard_plot(
    fuel_type: str = settings.FUEL_TYPE,
    horizon_days: int = 30,
    output_path: Optional[Path] = None
) -> Path:
    pipeline = ForecastingPipeline(fuel_type=fuel_type)
    forecast_data = pipeline.run_inference(horizon_days=horizon_days)
    hist_df = fetch_prices_as_df(fuel_type).tail(90)

    timeline = pd.DataFrame(forecast_data["timeline"])
    timeline["date"] = pd.to_datetime(timeline["date"])

    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(12, 10), gridspec_kw={"height_ratios": [2.5, 1]})

    ax1.plot(hist_df["timestamp"], hist_df["bunker_price"], label="Historical Actual ($/MT)", color="#3b82f6", lw=2)
    ax1.plot(timeline["date"], timeline["prophet"], label="Prophet Curve", color="#a855f7", linestyle="--", lw=1.5)
    ax1.plot(timeline["date"], timeline["xgboost"], label="XGBoost Recursive", color="#10b981", linestyle=":", lw=1.5)
    ax1.plot(timeline["date"], timeline["ensemble"], label="Ensemble Consensus", color="#f59e0b", lw=2.5)

    ax1.fill_between(
        timeline["date"],
        timeline["lower_bound"],
        timeline["upper_bound"],
        color="#f59e0b",
        alpha=0.18,
        label="95% Statistical Confidence Band"
    )

    ax1.set_title(f"Bunker Fuel Price Forecast ({fuel_type}) - {horizon_days}-Day Outlook", fontsize=14, fontweight="bold")
    ax1.set_ylabel("Price ($ / Metric Ton)", fontsize=11)
    ax1.grid(True, linestyle="--", alpha=0.5)
    ax1.legend(loc="upper left")

    try:
        xgb_wrapper = XGBoostModelWrapper.load(fuel_type=fuel_type)
        fi = xgb_wrapper.get_feature_importances()
        top_feats = dict(list(fi.items())[:6])
        ax2.barh(list(top_feats.keys()), list(top_feats.values()), color="#64748b")
        ax2.set_title("Top 6 Predictive Market Drivers (XGBoost)", fontsize=11)
        ax2.set_xlabel("Relative Importance Weight", fontsize=10)
        ax2.invert_yaxis()
    except Exception:
        ax2.text(0.5, 0.5, "Feature Importance unavailable", ha="center", va="center")

    plt.tight_layout()
    out = output_path or (settings.DATA_DIR / f"{fuel_type}_forecast.png")
    fig.savefig(out, dpi=200)
    plt.close(fig)
    return out