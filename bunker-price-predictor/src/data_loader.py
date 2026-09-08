import pandas as pd
import numpy as np
import logging
from pathlib import Path
from typing import Optional, List, Tuple
from datetime import datetime, timedelta
from src.config import settings
from src.database import insert_price, fetch_prices_as_df, init_db

logger = logging.getLogger(__name__)

def fetch_real_market_data(fuel_type: str = "VLSFO", days: int = 730) -> Optional[pd.DataFrame]:
    """
    Attempts to fetch genuine market data from Yahoo Finance / commodity market endpoints.
    Fetches Brent Crude, WTI, US Dollar Index, and Natural Gas to derive empirical bunker benchmarks.
    """
    try:
        import yfinance as yf
        start_date = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")
        end_date = datetime.now().strftime("%Y-%m-%d")

        logger.info(f"Attempting to fetch real commodity data from {start_date} to {end_date}...")
        tickers = {
            "brent_crude": "BZ=F",
            "wti_crude": "CL=F",
            "usd_index": "DX-Y.NYB",
            "natural_gas": "NG=F",
            "freight_index": "BDRY"
        }

        frames = {}
        for col_name, sym in tickers.items():
            try:
                hist = yf.download(sym, start=start_date, end=end_date, progress=False)
                if not hist.empty and "Close" in hist:
                    series = hist["Close"].squeeze()
                    if isinstance(series, pd.DataFrame):
                        series = series.iloc[:, 0]
                    frames[col_name] = series
            except Exception as ex:
                logger.warning(f"Could not download ticker {sym}: {ex}")

        if "brent_crude" in frames and len(frames["brent_crude"]) > 50:
            df = pd.DataFrame(frames)
            df.index = pd.to_datetime(df.index)
            full_idx = pd.date_range(start=df.index.min(), end=df.index.max(), freq="D")
            df = df.reindex(full_idx).ffill().bfill()

            crack_spread = 80.0 if fuel_type == "VLSFO" else (40.0 if fuel_type == "HSFO" else 120.0)
            # Standard conversion: Brent ($/bbl) * 7.33 bbl/MT + refinery crack spread ($/MT)
            df["bunker_price"] = np.round((df["brent_crude"] * 7.33) + crack_spread, 2)
            df["fuel_type"] = fuel_type
            df["timestamp"] = df.index.strftime("%Y-%m-%d")
            df["data_provenance"] = "REAL_MARKET_TELEMETRY"

            logger.info("✅ Successfully loaded REAL commodity market data for bunker fuel.")
            return df.reset_index(drop=True)
    except Exception as e:
        logger.warning(f"Failed to fetch live/real market data: {e}")

    return None

def generate_mock_csv_if_missing(file_path: Path, fuel_type: str = "VLSFO") -> None:
    file_path.parent.mkdir(parents=True, exist_ok=True)
    
    # 1. ALWAYS TRY REAL MARKET DATA FIRST
    real_df = fetch_real_market_data(fuel_type=fuel_type)
    if real_df is not None and not real_df.empty:
        real_df.to_csv(file_path, index=False)
        print(f"✅ Generated {file_path} using REAL commodity market feeds.")
        return

    # 2. IF UNAVAILABLE, FALLBACK TO SYNTHETIC WITH PROMINENT DISCLAIMER
    print("=" * 70)
    print("⚠️  [CRITICAL WARNING - DATA PROVENANCE NOTICE]  ⚠️")
    print("Could not reach real market data feeds (offline/API limits).")
    print("FALLING BACK TO SYNTHETIC DATA GENERATOR.")
    print("THIS FILE CONTAINS ESTIMATED / MATHEMATICALLY SIMULATED NUMBERS.")
    print("DO NOT PRESENT AS VERIFIED INDEPENDENT TERMINAL QUOTES.")
    print("=" * 70)

    dates = pd.date_range(end=pd.Timestamp.now().floor("D"), periods=730, freq="D")
    np.random.seed(42)
    t = np.arange(len(dates))
    brent_base = 75.0 + 10.0 * np.sin(2 * np.pi * t / 365.25) + np.cumsum(np.random.normal(0, 0.8, len(dates)))
    wti_base = brent_base - 4.5 + np.random.normal(0, 0.3, len(dates))
    usd_base = 102.0 - 0.05 * (brent_base - 75.0) + np.cumsum(np.random.normal(0, 0.1, len(dates)))
    natgas_base = 2.5 + 0.8 * np.sin(2 * np.pi * t / 180.0) + np.random.normal(0, 0.1, len(dates))
    freight_base = 1500.0 + 300.0 * np.sin(2 * np.pi * t / 365.25) + np.cumsum(np.random.normal(0, 15, len(dates)))
    
    crack_spread = 80.0 if fuel_type == "VLSFO" else (40.0 if fuel_type == "HSFO" else 120.0)
    bunker_metric_ton = (brent_base * 7.33) + crack_spread + np.cumsum(np.random.normal(0, 2.5, len(dates)))

    df = pd.DataFrame({
        "timestamp": dates.strftime("%Y-%m-%d"),
        "fuel_type": fuel_type,
        "bunker_price": np.round(bunker_metric_ton, 2),
        "brent_crude": np.round(brent_base, 2),
        "wti_crude": np.round(wti_base, 2),
        "usd_index": np.round(usd_base, 2),
        "natural_gas": np.round(natgas_base, 2),
        "freight_index": np.round(freight_base, 2),
        "data_provenance": "SYNTHETIC_SIMULATION_WARNING"
    })
    df.to_csv(file_path, index=False)

def load_data(file_path: Optional[Path] = None, fuel_type: Optional[str] = None) -> pd.DataFrame:
    path = file_path or settings.HISTORICAL_CSV_PATH
    selected_fuel = fuel_type or settings.FUEL_TYPE

    if not path.exists():
        generate_mock_csv_if_missing(path, selected_fuel)

    try:
        df = pd.read_csv(path)
    except Exception as e:
        raise ValueError(f"Failed to read CSV at {path}: {str(e)}")

    if "fuel_type" in df.columns:
        df = df[df["fuel_type"].str.upper() == selected_fuel.upper()].copy()
    
    return df

def clean_data(df: pd.DataFrame) -> pd.DataFrame:
    if df.empty:
        raise ValueError("Provided dataset is empty.")

    # Validate timestamp
    timestamp_col = next((col for col in ["timestamp", "date", "ds", "DateTime"] if col in df.columns), None)
    if not timestamp_col:
        raise ValueError("Missing chronological index column. Required: 'timestamp' or 'date'.")

    price_col = next((col for col in ["bunker_price", "price", "y", "close"] if col in df.columns), None)
    if not price_col:
        raise ValueError("Missing target column. Required: 'bunker_price'.")

    df = df.copy()
    df["timestamp"] = pd.to_datetime(df[timestamp_col], errors="coerce")
    df = df.dropna(subset=["timestamp"])
    df = df.rename(columns={price_col: "bunker_price"})

    # Ensure price is valid numeric
    df["bunker_price"] = pd.to_numeric(df["bunker_price"], errors="coerce")
    df = df.dropna(subset=["bunker_price"])
    df = df[df["bunker_price"] > 0]

    # Deduplicate timestamps
    df = df.sort_values("timestamp").drop_duplicates(subset=["timestamp"], keep="last")

    # Resample to strict daily timeline (forward-fill market holidays up to 4 days, then linear interpolate)
    df = df.set_index("timestamp").asfreq("D")
    df["bunker_price"] = df["bunker_price"].ffill(limit=4).interpolate(method="time")
    
    # Process regressors gracefully
    for reg in settings.OPTIONAL_REGRESSORS:
        if reg in df.columns:
            df[reg] = pd.to_numeric(df[reg], errors="coerce").ffill(limit=4).interpolate(method="time")

    df = df.reset_index()
    return df

def sync_csv_to_db(file_path: Optional[Path] = None, fuel_type: Optional[str] = None) -> int:
    init_db()
    fuel = fuel_type or settings.FUEL_TYPE
    raw = load_data(file_path, fuel)
    cleaned = clean_data(raw)
    
    count = 0
    for _, row in cleaned.iterrows():
        record = {
            "timestamp": row["timestamp"].strftime("%Y-%m-%d"),
            "fuel_type": fuel,
            "bunker_price": float(row["bunker_price"]),
            "brent_crude": float(row["brent_crude"]) if "brent_crude" in row and pd.notna(row["brent_crude"]) else None,
            "wti_crude": float(row["wti_crude"]) if "wti_crude" in row and pd.notna(row["wti_crude"]) else None,
            "usd_index": float(row["usd_index"]) if "usd_index" in row and pd.notna(row["usd_index"]) else None,
            "natural_gas": float(row["natural_gas"]) if "natural_gas" in row and pd.notna(row["natural_gas"]) else None,
            "freight_index": float(row["freight_index"]) if "freight_index" in row and pd.notna(row["freight_index"]) else None,
        }
        if insert_price(record):
            count += 1
    return count