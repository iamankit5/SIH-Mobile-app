#!/usr/bin/env python3
"""
Automated Safety-Net Test Suite for Ship-Matching and Draft Logic
Verifies physical port constraints, capacity fit, cost breakdowns, and draft compliance.
"""

import unittest
from procurement_engine import (
    evaluate_all_vessels,
    get_route_risk_profile,
    PORT_COORDINATES,
    ROUTES,
    VESSEL_SPECS
)

class TestShipMatchingLogic(unittest.TestCase):

    def test_haldia_shallow_port_rejects_deep_draft_ships(self):
        """
        Critical safety check: Haldia is a shallow river port (8.5m draft limit).
        Giant ships (Capesize with 18m draft, Panamax with 13.5m draft) must NOT be cleared.
        """
        results = evaluate_all_vessels(
            cargo_qty_mt=75000,
            origin="Australia (Newcastle)",
            destination="Haldia",
            bunker_price=615.0
        )
        by_name = {v["vessel"]: v for v in results}

        # Capesize (18.0m) vs Haldia (8.5m)
        self.assertIn("🔴 Draft Exceeds", by_name["Capesize"]["feasibility"])
        self.assertLess(by_name["Capesize"]["ai_score"], 50.0, "Capesize should be heavily penalized at Haldia")

        # Panamax (13.5m) vs Haldia (8.5m)
        self.assertIn("🔴 Draft Exceeds", by_name["Panamax"]["feasibility"])
        self.assertLess(by_name["Panamax"]["ai_score"], 50.0, "Panamax should be heavily penalized at Haldia")

    def test_dhamra_deep_water_port_clears_all_vessels(self):
        """
        Dhamra has 18.0m draft capacity and can berth Capesize bulk carriers.
        """
        results = evaluate_all_vessels(
            cargo_qty_mt=150000,
            origin="Australia (Newcastle)",
            destination="Dhamra",
            bunker_price=615.0
        )
        by_name = {v["vessel"]: v for v in results}

        self.assertEqual(by_name["Capesize"]["feasibility"], "🟢 Cleared")
        self.assertEqual(by_name["Panamax"]["feasibility"], "🟢 Cleared")
        self.assertEqual(results[0]["vessel"], "Capesize", "150k MT to Dhamra should rank Capesize #1")

    def test_75k_parcel_selects_panamax(self):
        """
        Standard Newcastle-Paradip 75,000 MT parcel should rank Panamax as #1 optimal choice.
        """
        results = evaluate_all_vessels(
            cargo_qty_mt=75000,
            origin="Australia (Newcastle)",
            destination="Paradip",
            bunker_price=615.0
        )
        optimal = results[0]
        self.assertEqual(optimal["vessel"], "Panamax")
        self.assertEqual(optimal["feasibility"], "🟢 Cleared")
        self.assertEqual(optimal["utilization_pct"], 100.0)
        self.assertGreaterEqual(optimal["ai_score"], 80.0)

    def test_35k_parcel_selects_handysize(self):
        """
        Small parcel of 35,000 MT should choose Handysize.
        """
        results = evaluate_all_vessels(
            cargo_qty_mt=35000,
            origin="Indonesia (Samarinda)",
            destination="Vizag",
            bunker_price=600.0
        )
        optimal = results[0]
        self.assertEqual(optimal["vessel"], "Handysize")
        self.assertEqual(optimal["feasibility"], "🟢 Cleared")
        self.assertEqual(optimal["utilization_pct"], 100.0)

    def test_all_routes_have_valid_distances(self):
        """
        Ensure every route entry in ROUTES has positive nautical mileage.
        """
        for origin, dests in ROUTES.items():
            for dest, distance in dests.items():
                self.assertGreater(distance, 500, f"Distance from {origin} to {dest} must be realistic (>500 NM)")

    def test_cost_breakdown_positivity(self):
        """
        Ensure all computed cost components are strictly positive numbers.
        """
        results = evaluate_all_vessels(
            cargo_qty_mt=60000,
            origin="Mozambique (Maputo)",
            destination="Paradip",
            bunker_price=620.0
        )
        for v in results:
            self.assertGreater(v["total_cost_usd"], 0)
            self.assertGreater(v["cost_per_mt"], 0)
            self.assertGreater(v["freight_pmt"], 0)
            self.assertGreater(v["fuel_pmt"], 0)
            self.assertGreater(v["voyage_days"], 0)

if __name__ == "__main__":
    print("Running ship-matching and port-constraint automated test suite...")
    unittest.main(verbosity=2)
