import sqlite3
from pathlib import Path
import pandas as pd
from typing import Optional, Dict, Any
from src.config import settings

def get_connection(db_path: Optional[Path] = None) -> sqlite3.Connection:
    path = db_path or settings.DATABASE_PATH
    conn = sqlite3.connect(str(path))
    conn.row_factory = sqlite3.Row
    return conn

def init_db(db_path: Optional[Path] = None) -> None:
    conn = get_connection(db_path)
    cursor = conn.cursor()
    
    # Table for recorded daily/spot prices
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS bunker_prices (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp TEXT NOT NULL,
        fuel_type TEXT NOT NULL,
        bunker_price REAL NOT NULL,
        brent_crude REAL,
        wti_crude REAL,
        usd_index REAL,
        natural_gas REAL,
        freight_index REAL,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(timestamp, fuel_type)
    );
    """)

    # Table for storing historical inference runs and metrics
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS forecast_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        execution_time TEXT NOT NULL,
        fuel_type TEXT NOT NULL,
        target_timestamp TEXT NOT NULL,
        horizon_days INTEGER NOT NULL,
        prophet_pred REAL,
        xgboost_pred REAL,
        ensemble_pred REAL NOT NULL,
        lower_bound REAL,
        upper_bound REAL
    );
    """)

    # Table for tracking trained model performance audits
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS model_evaluations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        evaluation_time TEXT NOT NULL,
        fuel_type TEXT NOT NULL,
        model_name TEXT NOT NULL,
        mae REAL NOT NULL,
        rmse REAL NOT NULL,
        mape REAL NOT NULL,
        horizon_days INTEGER NOT NULL
    );
    """)

    conn.commit()
    conn.close()

def insert_price(record: Dict[str, Any], db_path: Optional[Path] = None) -> bool:
    conn = get_connection(db_path)
    cursor = conn.cursor()
    query = """
    INSERT INTO bunker_prices (
        timestamp, fuel_type, bunker_price, brent_crude,
        wti_crude, usd_index, natural_gas, freight_index
    ) VALUES (
        :timestamp, :fuel_type, :bunker_price, :brent_crude,
        :wti_crude, :usd_index, :natural_gas, :freight_index
    )
    ON CONFLICT(timestamp, fuel_type) DO UPDATE SET
        bunker_price=excluded.bunker_price,
        brent_crude=COALESCE(excluded.brent_crude, bunker_prices.brent_crude),
        wti_crude=COALESCE(excluded.wti_crude, bunker_prices.wti_crude),
        usd_index=COALESCE(excluded.usd_index, bunker_prices.usd_index),
        natural_gas=COALESCE(excluded.natural_gas, bunker_prices.natural_gas),
        freight_index=COALESCE(excluded.freight_index, bunker_prices.freight_index);
    """
    try:
        cursor.execute(query, record)
        conn.commit()
        return True
    finally:
        conn.close()

def fetch_prices_as_df(fuel_type: str, db_path: Optional[Path] = None) -> pd.DataFrame:
    conn = get_connection(db_path)
    query = """
    SELECT timestamp, bunker_price, brent_crude, wti_crude, usd_index, natural_gas, freight_index
    FROM bunker_prices
    WHERE fuel_type = ?
    ORDER BY timestamp ASC;
    """
    df = pd.read_sql_query(query, conn, params=(fuel_type,))
    conn.close()
    if not df.empty:
        df["timestamp"] = pd.to_datetime(df["timestamp"])
    return df