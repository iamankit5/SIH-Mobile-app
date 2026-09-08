import argparse
from typing import List
from src.config import settings
from src.data_loader import sync_csv_to_db, fetch_prices_as_df
from src.preprocessing import prepare_prophet_data
from src.prophet_model import ProphetModelWrapper
from src.xgboost_model import XGBoostModelWrapper
from src.backtesting import walk_forward_backtest
from src.database import get_connection
from datetime import datetime

def train_pipeline(fuel_type: str = settings.FUEL_TYPE, run_evaluation: bool = True):
    print(f"=== [1/5] Ingesting and Synchronizing Historical Data for {fuel_type} ===")
    sync_csv_to_db(fuel_type=fuel_type)
    df = fetch_prices_as_df(fuel_type=fuel_type)
    
    if len(df) < 60:
        raise ValueError(f"Insufficient samples ({len(df)}) for fuel type {fuel_type}.")

    # Isolate active exogenous regressors
    available_regressors = [r for r in settings.OPTIONAL_REGRESSORS if r in df.columns and df[r].notna().sum() > 30]
    print(f"Active Exogenous Regressors Detected: {available_regressors}")

    if run_evaluation:
        print("\n=== [2/5] Performing Time-Series Walk-Forward Backtesting ===")
        _, metrics = walk_forward_backtest(df, horizon_days=7, n_splits=4, fuel_type=fuel_type, regressors=available_regressors)
        print("\n" + "="*45)
        print(f" MODEL BACKTEST ACCURACY BENCHMARK (7-Day Horizon) ")
        print("="*45)
        print(f"{'Model':<12} | {'MAE ($/MT)':<10} | {'RMSE':<8} | {'MAPE (%)':<8}")
        print("-" * 45)
        for model_name, score in metrics.items():
            print(f"{model_name:<12} | ${score['MAE']:<9} | {score['RMSE']:<8} | {score['MAPE']}%")
            # Persist to database
            conn = get_connection()
            cur = conn.cursor()
            cur.execute("""
            INSERT INTO model_evaluations (evaluation_time, fuel_type, model_name, mae, rmse, mape, horizon_days)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (datetime.utcnow().isoformat(), fuel_type, model_name, score["MAE"], score["RMSE"], score["MAPE"], 7))
            conn.commit()
            conn.close()
        print("="*45)

    print("\n=== [3/5] Training Meta Prophet Model on Full Dataset ===")
    p_train = prepare_prophet_data(df, available_regressors=available_regressors)
    p_wrapper = ProphetModelWrapper(fuel_type=fuel_type, regressors=available_regressors)
    p_wrapper.fit(p_train)
    p_path = p_wrapper.save()
    print(f"Prophet serialized successfully to: {p_path}")

    print("\n=== [4/5] Training Autoregressive XGBoost Model on Full Dataset ===")
    x_wrapper = XGBoostModelWrapper(fuel_type=fuel_type, regressors=available_regressors)
    x_wrapper.fit(df)
    x_path = x_wrapper.save()
    print(f"XGBoost serialized successfully to: {x_path}")

    print("\n=== [5/5] XGBoost Feature Importance Breakdown ===")
    fi = x_wrapper.get_feature_importances()
    for feat, imp in list(fi.items())[:8]:
        print(f" - {feat:<20}: {imp:.4f}")
    
    print(f"\n[OK] Training complete. Models ready for deployment.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train Bunker Price Prediction Stack")
    parser.add_argument("--fuel", type=str, default=settings.FUEL_TYPE, choices=["VLSFO", "HSFO", "MGO"])
    parser.add_argument("--skip-eval", action="store_true", help="Skip walk-forward backtesting")
    args = parser.parse_args()

    train_pipeline(fuel_type=args.fuel, run_evaluation=not args.skip_eval)