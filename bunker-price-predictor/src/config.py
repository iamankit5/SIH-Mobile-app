import os
from pathlib import Path
from typing import List, Literal
from pydantic_settings import BaseSettings

BASE_DIR = Path(__file__).resolve().parent.parent

class Settings(BaseSettings):
    # Core domain settings
    FUEL_TYPE: Literal["VLSFO", "HSFO", "MGO"] = "VLSFO"
    PROPHET_WEIGHT: float = 0.5
    XGBOOST_WEIGHT: float = 0.5

    # Forecasting horizons (in days)
    FORECAST_24H: int = 1
    FORECAST_7D: int = 7
    FORECAST_30D: int = 30

    # Paths
    BASE_DIR: Path = BASE_DIR
    DATA_DIR: Path = BASE_DIR / "data"
    MODELS_DIR: Path = BASE_DIR / "models"
    DATABASE_PATH: Path = DATA_DIR / "bunker.db"
    HISTORICAL_CSV_PATH: Path = DATA_DIR / "historical_prices.csv"

    # Regressor settings
    OPTIONAL_REGRESSORS: List[str] = [
        "brent_crude",
        "wti_crude",
        "usd_index",
        "natural_gas",
        "freight_index"
    ]
    FUTURE_REGRESSOR_STRATEGY: Literal["persistence", "linear_drift"] = "persistence"

    # Model training hyperparameters
    PROPHET_CHANGEPOINT_PRIOR_SCALE: float = 0.05
    PROPHET_SEASONALITY_PRIOR_SCALE: float = 10.0
    PROPHET_INTERVAL_WIDTH: float = 0.95

    XGB_N_ESTIMATORS: int = 200
    XGB_MAX_DEPTH: int = 4
    XGB_LEARNING_RATE: float = 0.03
    XGB_SUBSAMPLE: float = 0.8
    XGB_COLSAMPLE_BYTREE: float = 0.8

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"

settings = Settings()

# Ensure mandatory directories exist
settings.DATA_DIR.mkdir(parents=True, exist_ok=True)
settings.MODELS_DIR.mkdir(parents=True, exist_ok=True)