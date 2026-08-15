"""Seed the demo offers for one or more PLZ regions.

Usage:
    python seed.py                     # seeds the default PLZ (Munich, 80331)
    python seed.py 85354               # seeds one PLZ
    python seed.py 80331 85354 90402   # seeds several

Note: with the server running in demo mode (DEMO_MODE=1) you don't need this at all —
searching any PLZ auto-seeds it. This script just pre-warms specific regions.
"""
import sys

from app.config import settings
from app.demo import seed_plz
from app.store import db


def main(argv: list[str]) -> None:
    db.init_db()
    plzs = [a.strip() for a in argv[1:]] or [settings.DEFAULT_PLZ]
    for plz in plzs:
        n = seed_plz(plz)
        print(f"seeded {n} offers for PLZ {plz}")


if __name__ == "__main__":
    main(sys.argv)
