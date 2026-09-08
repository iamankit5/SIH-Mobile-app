# procurement_engine.py

PORT_COORDINATES = {
    "Australia (Newcastle)": {"lat": -32.9283, "lon": 151.7817, "weather_risk": "Low", "congestion_risk": "Medium"},
    "Australia (Hay Point)": {"lat": -21.2858, "lon": 149.2997, "weather_risk": "Low", "congestion_risk": "Low"},
    "Indonesia (Samarinda)": {"lat": -0.5021, "lon": 117.1537, "weather_risk": "Medium", "congestion_risk": "Medium"},
    "Mozambique (Maputo)":   {"lat": -25.9692, "lon": 32.5732, "weather_risk": "Low", "congestion_risk": "Low"},
    "USA (New Orleans)":     {"lat": 29.9511, "lon": -90.0715, "weather_risk": "Medium", "congestion_risk": "High"},
    "Russia (Vladivostok)":  {"lat": 43.1155, "lon": 131.8855, "weather_risk": "High", "congestion_risk": "Low"},
    # Added Port Draft Constraints (Max Depth in meters)
    "Paradip":  {"lat": 20.2644, "lon": 86.6083, "waiting_days": 2.5, "draft_limit_m": 14.5},
    "Haldia":   {"lat": 22.0257, "lon": 88.0583, "waiting_days": 3.8, "draft_limit_m": 8.5}, # Shallow river port
    "Vizag":    {"lat": 17.6868, "lon": 83.2185, "waiting_days": 2.0, "draft_limit_m": 14.5},
    "Dhamra":   {"lat": 20.8317, "lon": 86.9583, "waiting_days": 1.8, "draft_limit_m": 18.0}  # Deep water port
}

ROUTES = {
    "Australia (Newcastle)": {"Paradip": 4500, "Haldia": 4650, "Vizag": 4400, "Dhamra": 4550},
    "Australia (Hay Point)": {"Paradip": 4300, "Haldia": 4450, "Vizag": 4200, "Dhamra": 4350},
    "Indonesia (Samarinda)": {"Paradip": 2200, "Haldia": 2350, "Vizag": 2100, "Dhamra": 2250},
    "Mozambique (Maputo)":   {"Paradip": 3900, "Haldia": 4050, "Vizag": 3750, "Dhamra": 3950},
    "USA (New Orleans)":     {"Paradip": 9800, "Haldia": 9950, "Vizag": 9650, "Dhamra": 9850},
    "Russia (Vladivostok)":  {"Paradip": 4800, "Haldia": 4950, "Vizag": 4650, "Dhamra": 4850}
}

# Added Vessel Draft requirements
VESSEL_SPECS = {
    "Handysize": {"avg_cap": 35000, "speed_knots": 13, "fuel_per_day": 20, "daily_hire_rate": 11500, "draft_m": 10.0, "availability": "4 vessels", "risk": "🟡 Medium"},
    "Supramax":  {"avg_cap": 55000, "speed_knots": 14, "fuel_per_day": 26, "daily_hire_rate": 14500, "draft_m": 11.5, "availability": "3 vessels", "risk": "🟡 Medium"},
    "Panamax":   {"avg_cap": 75000, "speed_knots": 14, "fuel_per_day": 32, "daily_hire_rate": 17000, "draft_m": 13.5, "availability": "8 vessels", "risk": "🟢 Low"},
    "Capesize":  {"avg_cap": 150000, "speed_knots": 13, "fuel_per_day": 46, "daily_hire_rate": 24000, "draft_m": 18.0, "availability": "1 vessel", "risk": "🔴 High"}
}

def evaluate_all_vessels(cargo_qty_mt, origin, destination, bunker_price, freight_multiplier=1.0):
    distance = ROUTES[origin][destination]
    dest_draft = PORT_COORDINATES[destination]["draft_limit_m"]
    evaluations = []

    for v_name, spec in VESSEL_SPECS.items():
        daily_dist = spec["speed_knots"] * 24
        voyage_days = distance / daily_dist
        
        fuel_cost = voyage_days * spec["fuel_per_day"] * bunker_price
        charter_cost = voyage_days * spec["daily_hire_rate"] * freight_multiplier
        port_charges = 35000
        demurrage_risk = 12000 if spec["risk"] == "🔴 High" else (6000 if spec["risk"] == "🟡 Medium" else 2500)
        canal_fees = 8500 if "USA" in origin else 2000
        
        total_voyage_cost = fuel_cost + charter_cost + port_charges + demurrage_risk + canal_fees
        cost_per_mt = total_voyage_cost / cargo_qty_mt if cargo_qty_mt > 0 else 0
        
        utilization = (cargo_qty_mt / spec["avg_cap"]) * 100
        util_penalty = abs(100 - utilization) * 0.5 if utilization <= 120 else (utilization - 100) * 1.8
        
        # Physical Constraint Check: Draft Penalty
        draft_penalty = 0
        feasibility = "🟢 Cleared"
        if spec["draft_m"] > dest_draft:
            draft_penalty = 50 # Massive penalty if the ship can't fit
            feasibility = f"🔴 Draft Exceeds Port Limit ({spec['draft_m']}m > {dest_draft}m)"
        
        base_score = 96 - util_penalty - (cost_per_mt * 0.05) - draft_penalty
        ai_score = max(0, min(99, round(base_score, 1)))

        evaluations.append({
            "vessel": v_name,
            "capacity": f"{spec['avg_cap'] // 1000}K MT",
            "voyage_days": round(voyage_days, 1),
            "fuel_burned_mt": round(voyage_days * spec["fuel_per_day"], 1),
            "total_cost_usd": round(total_voyage_cost, 0),
            "cost_per_mt": round(cost_per_mt, 2),
            "freight_pmt": round(charter_cost / cargo_qty_mt, 2),
            "fuel_pmt": round(fuel_cost / cargo_qty_mt, 2),
            "port_pmt": round((port_charges + canal_fees) / cargo_qty_mt, 2),
            "demurrage_pmt": round(demurrage_risk / cargo_qty_mt, 2),
            "utilization_pct": round(min(100.0, utilization), 1),
            "availability": spec["availability"],
            "risk": spec["risk"],
            "feasibility": feasibility,
            "ai_score": ai_score
        })

    evaluations.sort(key=lambda x: x["ai_score"], reverse=True)
    return evaluations

def get_route_risk_profile(origin, destination):
    orig = PORT_COORDINATES[origin]
    dest = PORT_COORDINATES[destination]
    return {
        "Origin Weather": orig["weather_risk"],
        "Port Congestion": orig["congestion_risk"],
        "Waiting Time at Dest": f"{dest['waiting_days']} Days",
        "Freight Volatility": "Medium",
        "Overall Route Risk": "🟡 LOW–MEDIUM"
    }