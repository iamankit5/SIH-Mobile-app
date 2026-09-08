import pandas as pd
import numpy as np
import yfinance as yf
from datetime import datetime

def fetch_and_build_dataset(start_date="2021-01-01", end_date=None):
    if end_date is None:
        end_date = datetime.today().strftime('%Y-%m-%d')
        
    print(f"Fetching market data from {start_date} to {end_date}...")

    tickers = {
        'Bunker_Oil': 'CL=F',
        'Dry_Bulk_Index': 'BDRY',
        'USD_INR': 'USDINR=X',
        'Commodity_Gold': 'GC=F'
    }
    
    data_frames = []
    for feature_name, ticker_sym in tickers.items():
        ticker_data = yf.download(ticker_sym, start=start_date, end=end_date, progress=False)
        if not ticker_data.empty:
            series = ticker_data['Close'].squeeze().rename(feature_name)
            data_frames.append(series)

    master_df = pd.concat(data_frames, axis=1)
    master_df.index = pd.to_datetime(master_df.index)

    full_calendar = pd.date_range(start=master_df.index.min(), end=master_df.index.max(), freq='D')
    master_df = master_df.reindex(full_calendar)
    
    master_df = master_df.ffill().bfill()

    # ZERO DATA LEAKAGE ENFORCEMENT:
    # Any rolling statistics or autoregressive features of the target (Dry_Bulk_Index)
    # MUST be strictly calculated on shifted history (.shift(1)) so row 't' contains
    # only information available at 't-1' and never peeks into the current day or future.
    shifted_bdi = master_df['Dry_Bulk_Index'].shift(1)
    master_df['BDI_Lag1'] = shifted_bdi
    master_df['BDI_Lag7'] = master_df['Dry_Bulk_Index'].shift(7)
    master_df['BDI_MA7'] = shifted_bdi.rolling(window=7, min_periods=4).mean()
    master_df['BDI_MA30'] = shifted_bdi.rolling(window=30, min_periods=15).mean()
    master_df['Oil_Daily_Return'] = master_df['Bunker_Oil'].pct_change().shift(1)
    
    master_df = master_df.dropna()

    return master_df

if __name__ == "__main__":
    df = fetch_and_build_dataset()
    print("\n--- Processed Dataset Sample ---")
    print(df.tail())
    
    df.to_csv("processed_freight_training_data.csv")
    print("\nDataset successfully saved to 'processed_freight_training_data.csv'")