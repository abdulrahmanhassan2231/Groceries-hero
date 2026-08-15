import os
import sys
import tempfile

# make `import app...` work when running pytest from backend/
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

# isolate the SQLite cache to a temp file for tests
os.environ.setdefault("DB_PATH", os.path.join(tempfile.gettempdir(), "grocery_test.db"))
