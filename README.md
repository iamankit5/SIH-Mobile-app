<div align="center">
# 🚢 SAIL Freight Intelligence
### AI-Powered Vessel Chartering & Bulk Cargo Procurement for India's East Coast
 
**Smart India Hackathon 2026 · Problem Statement SIH26006**
**Ministry of Steel · SAIL (Steel Authority of India Limited)**
 
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#)
[![Python](https://img.shields.io/badge/ML%20Pipeline-Python-3776AB?style=for-the-badge&logo=python&logoColor=white)](#)
[![FastAPI](https://img.shields.io/badge/API-FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)](#)
[![Status](https://img.shields.io/badge/Honesty%20Audit-Self--Verifying-blueviolet?style=for-the-badge)](#-the-model-honesty-report-our-real-differentiator)
 
*Every port. Every route. Every ship. One decision engine.*
 
</div>
---
 
## 📌 The Problem We're Solving
 
SAIL procures massive volumes of raw material — coal, iron ore, coking coal — shipped in
from Australia, the US, Mozambique, Russia, and Indonesia to ports on India's East Coast.
Today, that process runs on **daily manual market scanning**: someone checks freight rates,
someone else checks which ship is free, and the charter decision gets made reactively —
after the best window may already have passed.
 
The result, as stated in the official problem brief: **missed cost-savings opportunities,
suboptimal vessel-to-cargo matching, and increased vessel idle time**, driven by the sheer
volatility of global freight markets and the absence of a forward-looking decision system.
 
**SAIL Freight Intelligence** is our answer: a mobile decision-support system that turns
that daily manual scramble into a single, live dashboard — built to actually be used by a
procurement officer, not just demoed once.
 
---
 
## 🧭 What It Actually Does
 
| Capability | What happens under the hood |
|---|---|
| 🚛 **Vessel–Port Matching** | Every candidate vessel (Handysize → Capesize) is checked against real port draft constraints, ranked by total landed cost, and flagged if it physically cannot berth |
| 🌍 **Interactive Route Globe** | Visualizes the shipping lane from origin to destination port with live distance and transit-time context |
| 🌦️ **Route & Weather Intelligence** | Live weather conditions along the route and at the destination port, pulled from a real weather API — not a static assumption |
| 📈 **Bunker Fuel Forecasting** | A Prophet + XGBoost ensemble forecasts VLSFO bunker fuel price movement, with confidence bands, not a single false-precision number |
| ⏱️ **Charter Timing Analysis** | Surfaces whether current conditions favor chartering now or waiting, based on live and historical price signal |
| 🧮 **What-If Simulator** | Lets a procurement officer change cargo size, route, or vessel class and instantly see the cost impact |
| 🛰️ **Live Data Provenance Badges** | Every number on screen is tagged **🟢 LIVE SYNC** or **🟡 ESTIMATED BACKUP** — the app tells you, visibly, whether you're looking at a live market feed or a fallback estimate |
 
---
 
## 🏗️ System Architecture
 
```
┌─────────────────────────────────────────────────────────────────┐
│                     ANDROID APPLICATION (Kotlin)                 │
│                  Jetpack Compose · MVVM · Coroutines              │
│                                                                     │
│   MainScreen ─┬─ VesselSuitabilitySection                         │
│               ├─ RouteAndWeatherSection ── WeatherService (live)  │
│               ├─ BunkerIntelligenceSection                        │
│               ├─ TimingAnalysisSection                            │
│               ├─ WhatIfSimulatorSection                           │
│               ├─ InteractiveGlobe                                 │
│               └─ DataProvenanceSection ── LiveMarketService (live)│
│                                                                     │
│         ProcurementEngine.kt  ◄──── CsvDataLoader.kt (bundled      │
│         ForecastingEngine.kt         historical training data)     │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PYTHON ML & DATA PIPELINE                      │
│                                                                     │
│  freight_pipeline.py      → builds leak-free freight training set │
│  procurement_engine.py    → vessel/port/route decision logic      │
│  test_ship_matching.py    → 6 automated correctness tests         │
│  model_honesty_check.py   → independent audit of every model      │
│                                                                     │
│  bunker-price-predictor/                                          │
│    ├─ Prophet + XGBoost ensemble forecasting                      │
│    ├─ FastAPI microservice (api/main.py)                          │
│    └─ Real-data-first loader with disclosed synthetic fallback    │
└─────────────────────────────────────────────────────────────────┘
```
 
---
 
## 🛰️ Live Data — Not a Static Demo
 
Most hackathon prototypes fake their "live" data. Ours doesn't. `LiveMarketService.kt`
makes real, on-device network calls every time the app runs:
 
- **USD/INR exchange rate** — fetched live from `open.er-api.com`, with an automatic
  fallback to `frankfurter.app` if the primary source is unreachable
- **WTI Crude, Brent Crude, Natural Gas, US Dollar Index** — fetched live from Yahoo
  Finance's public market chart API
- **Live weather** at origin/destination ports, via `WeatherService.kt`
If a live call fails, the app doesn't crash or silently show a stale number — it falls
back to a documented estimate and **visibly labels it as such** in the UI. Nothing on
screen claims to be live unless it actually is.
 
---
 
## 🔍 The Model Honesty Report — Our Real Differentiator
 
Here's the part most teams skip, and the part we think should actually matter most to
judges: **we built a tool that independently audits our own models**, and we're
publishing exactly what it found — including the parts that aren't flattering.
 
`model_honesty_check.py` runs three checks against every forecasting model in this
project, with zero shortcuts:
 
1. **Data leakage audit** — verifies every rolling/lagged feature is strictly built from
   information available *before* the day being predicted, with automated detection if a
   stale or improperly-built dataset ever slips back in
2. **Naive persistence baseline** — the oldest, simplest trick in forecasting: does the
   model actually beat "tomorrow will look like today"? If it doesn't, that's reported,
   not hidden
3. **Overfit/underfit diagnosis** — a real train-vs-test comparison, not a hardcoded
   "looks good" label
**Here's what it currently finds, verbatim:**
 
| Model | Naive Baseline RMSE | Model RMSE | Verdict |
|---|---|---|---|
| Bunker Fuel (AR Momentum) | 24.63 | 24.61 | Roughly matches persistence (+0.08%) |
| Freight Rate Index (Random Forest) | 0.21 | 0.28 | **Does not yet beat persistence** (−32%) |
 
Data leakage: **cleared** on both models. Generalization: **healthy** on both — neither
model is overfit, they're just not there yet on raw predictive edge for freight.
 
**Why we're showing you this instead of hiding it:** the freight index is dominated by
strong day-to-day autocorrelation, which makes "beat a naive guess" a genuinely hard bar
to clear — and we'd rather demonstrate that we can *measure that honestly* than paper
over it with a number we can't defend under questioning. The next engineering milestone,
already identified, is reframing the model to predict the *change* in the index rather
than its raw level — isolating the signal the model can actually learn instead of letting
it coast on autocorrelation. Full methodology and numbers: [`MODEL_HONESTY_REPORT.md`](./MODEL_HONESTY_REPORT.md).
 
---
 
## ✅ Verified, Not Just Claimed
 
Every claim in this README is backed by something you can run yourself:
 
```bash
# Vessel-matching & procurement logic — 6/6 passing
python -m unittest test_ship_matching.py -v
 
# Full model honesty audit — regenerates MODEL_HONESTY_REPORT.md
python model_honesty_check.py
```
 
| Check | Result |
|---|---|
| Vessel draft-constraint safety (e.g. Capesize correctly blocked at shallow Haldia) | ✅ Passing |
| Deep-water port clears all vessel classes (Dhamra) | ✅ Passing |
| Invalid/negative cargo quantity rejected | ✅ Passing |
| Cost breakdown always positive | ✅ Passing |
| Route distance table integrity | ✅ Passing |
| Optimal vessel ranking correctness | ✅ Passing |
 
---
 
## 🌐 Data Sources & Honest Provenance
 
| Data | Source | Status |
|---|---|---|
| USD/INR, WTI, Brent, Natural Gas, USD Index | Live market APIs | 🟢 Real-time |
| Port weather | Live weather API | 🟢 Real-time |
| Bunker fuel price | Derived from live crude oil price via a disclosed conversion formula | 🟡 Real-input, estimated-output |
| Route distances, port drafts, vessel hire rates | Typical of published maritime industry benchmarks | 🟡 Estimated — not individually fact-checked per route |
| Freight index training history | Historical BDRY (dry bulk shipping ETF) via Yahoo Finance | 🟢 Real historical market data |
 
We'd rather a judge find this table than find out the hard way during Q&A.
 
---
 
## 🛠️ Tech Stack
 
**Mobile App:** Kotlin · Jetpack Compose · MVVM · Kotlin Coroutines · Navigation Compose
**ML Pipeline:** Python · pandas · scikit-learn · Prophet · XGBoost
**Backend Service:** FastAPI · SQLite
**Live Data:** REST APIs (Yahoo Finance, open.er-api.com, frankfurter.app, live weather)
 
---
 
## 🚀 Running the Project
 
### Android App
```bash
# 1. Open the project root in Android Studio
# 2. Create a .env file (see .env.example) with your GEMINI_API_KEY
# 3. Sync Gradle and run on an emulator or device
```
 
### Python ML Pipeline
```bash
pip install -r requirements.txt
python freight_pipeline.py          # regenerate leak-free freight training data
python model_honesty_check.py       # run the full self-audit
python -m unittest test_ship_matching.py
```
 
### Bunker Price Predictor (FastAPI)
```bash
cd bunker-price-predictor
pip install -r requirements.txt
python train.py                     # trains Prophet + XGBoost ensemble
uvicorn api.main:app --reload
```
 
---
 
## 🗺️ Roadmap
 
- [ ] Reframe freight forecasting target from raw index level → daily change, to isolate
      genuine predictive signal from autocorrelation
- [ ] Integrate commercial-grade freight/bunker data feeds (Baltic Exchange, S&P Global
      Platts) as enterprise adapter hooks, replacing the current open-data proxies
- [ ] Expand port and vessel database beyond the current four-port, four-class demo set
- [ ] Live fleet availability integration (AIS/MarineTraffic-style feed)
---
 
<div align="center">
**Built for SIH 2026 · Problem Statement 26006 · Ministry of Steel (SAIL)**
 
*We'd rather show you a model that's honestly 68% of the way there than one that's
100% claimed and 0% verifiable.*
 
</div>
 