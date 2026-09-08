from fastapi import FastAPI, HTTPException, Query, BackgroundTasks
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any, Literal
from datetime import datetime
import pandas as pd
from src.config import settings
from src.database import get_connection, insert_price, fetch_prices_as_df
from src.forecasting import ForecastingPipeline
from train import train_pipeline

app = FastAPI(
    title="Intelligent Marine Bunker Fuel Price Prediction API",
    description="Enterprise API providing Prophet + XGBoost ensemble forecasts for maritime fuels (VLSFO, HSFO, MGO).",
    version="2.0.0"
)

class PriceIngestionRequest(BaseModel):
    timestamp: str = Field(..., example="2026-09-04", description="ISO Date (YYYY-MM-DD)")
    fuel_type: Literal["VLSFO", "HSFO", "MGO"] = "VLSFO"
    bunker_price: float = Field(..., gt=0, example=585.50)
    brent_crude: Optional[float] = Field(None, example=78.20)
    wti_crude: Optional[float] = Field(None, example=73.50)
    usd_index: Optional[float] = Field(None, example=103.10)
    natural_gas: Optional[float] = Field(None, example=2.45)
    freight_index: Optional[float] = Field(None, example=1620.0)

class RetrainRequest(BaseModel):
    fuel_type: Literal["VLSFO", "HSFO", "MGO"] = "VLSFO"
    skip_eval: bool = False

@app.get("/health", tags=["System"])
def health_check():
    return {
        "status": "healthy",
        "system_time": datetime.utcnow().isoformat(),
        "default_fuel": settings.FUEL_TYPE,
        "database_connected": settings.DATABASE_PATH.exists()
    }

@app.get("/latest-price", tags=["Market Data"])
def get_latest_price(fuel: Literal["VLSFO", "HSFO", "MGO"] = "VLSFO"):
    df = fetch_prices_as_df(fuel)
    if df.empty:
        raise HTTPException(status_code=404, detail=f"No price records found for {fuel}")
    latest = df.iloc[-1]
    return {
        "fuel": fuel,
        "latest_price": float(latest["bunker_price"]),
        "timestamp": latest["timestamp"].strftime("%Y-%m-%d"),
        "associated_brent": float(latest["brent_crude"]) if pd.notna(latest.get("brent_crude")) else None
    }

@app.get("/forecast", tags=["Forecasting"])
def get_forecast(
    fuel: Literal["VLSFO", "HSFO", "MGO"] = "VLSFO",
    days: int = Query(7, ge=1, le=90, description="Forecast horizon in days")
):
    try:
        pipeline = ForecastingPipeline(fuel_type=fuel)
        result = pipeline.run_inference(horizon_days=days)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Forecasting engine error: {str(e)}")

@app.get("/dashboard-payload", tags=["Dashboard"])
def get_dashboard_payload(fuel: Literal["VLSFO", "HSFO", "MGO"] = "VLSFO"):
    """
    Consolidated payload engineered specifically for client-side single-page dashboards.
    """
    try:
        pipeline = ForecastingPipeline(fuel_type=fuel)
        
        f24 = pipeline.run_inference(horizon_days=1)
        f7d = pipeline.run_inference(horizon_days=7)
        f30 = pipeline.run_inference(horizon_days=30)
        
        hist_df = fetch_prices_as_df(fuel).tail(60)
        history = [
            {"date": r["timestamp"].strftime("%Y-%m-%d"), "price": float(r["bunker_price"])}
            for _, r in hist_df.iterrows()
        ]

        # Fetch latest backtest audit scores
        conn = get_connection()
        cur = conn.cursor()
        cur.execute("""
        SELECT model_name, mae, rmse, mape FROM model_evaluations
        WHERE fuel_type = ? ORDER BY id DESC LIMIT 3
        """, (fuel,))
        evals = {row["model_name"]: {"mae": row["mae"], "rmse": row["rmse"], "mape": row["mape"]} for row in cur.fetchall()}
        conn.close()

        return {
            "fuel": fuel,
            "current_price": f24["latest_price"],
            "as_of_date": f24["as_of_date"],
            "trend": f7d["trend"],
            "price_change_7d_pct": f7d["price_change_pct"],
            "summary_predictions": {
                "24h": {"price": f24["point_forecast"], "lower": f24["lower_bound"], "upper": f24["upper_bound"]},
                "7d": {"price": f7d["point_forecast"], "lower": f7d["lower_bound"], "upper": f7d["upper_bound"]},
                "30d": {"price": f30["point_forecast"], "lower": f30["lower_bound"], "upper": f30["upper_bound"]},
            },
            "history_60d": history,
            "forecast_timeline_30d": f30["timeline"],
            "model_performance_benchmarks": evals
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Dashboard payload generation failed: {str(e)}")

@app.post("/price", tags=["Market Data"])
def add_new_price(payload: PriceIngestionRequest):
    try:
        success = insert_price(payload.dict())
        if not success:
            raise HTTPException(status_code=400, detail="Database insertion conflict.")
        return {"status": "success", "message": f"Recorded {payload.fuel_type} price for {payload.timestamp}"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/retrain", tags=["Model Operations"])
def trigger_retrain(payload: RetrainRequest, background_tasks: BackgroundTasks):
    background_tasks.add_task(train_pipeline, fuel_type=payload.fuel_type, run_evaluation=not payload.skip_eval)
    return {
        "status": "accepted",
        "message": f"Retraining task for {payload.fuel_type} dispatched asynchronously to worker pool."
    }

@app.get("/model-performance", tags=["Model Operations"])
def get_model_performance(fuel: Literal["VLSFO", "HSFO", "MGO"] = "VLSFO"):
    conn = get_connection()
    cur = conn.cursor()
    cur.execute("""
    SELECT evaluation_time, model_name, mae, rmse, mape, horizon_days
    FROM model_evaluations
    WHERE fuel_type = ?
    ORDER BY id DESC LIMIT 15
    """, (fuel,))
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()
    return {"fuel": fuel, "evaluations": rows}