"""German grocery synonym / canonical-term dictionary.

Maps the many ways a shopper (or a retailer) writes a product to one canonical key so
that "Kartoffeln", "Speisekartoffeln" and "festkochende Kartoffeln" all rank together.

This is intentionally a small, curated, hand-maintained seed. In production it would be
extended from search logs and a proper product taxonomy, but a curated core covers the
common staples and demonstrates the mechanism. Keys are canonical; values are surface
forms / substrings to match (all lower-cased, umlauts kept).
"""
from __future__ import annotations

# canonical -> list of surface forms and telltale substrings
SYNONYMS: dict[str, list[str]] = {
    "kartoffeln": [
        "kartoffel", "kartoffeln", "speisekartoffel", "speisekartoffeln",
        "festkochend", "vorwiegend festkochend", "mehligkochend", "drillinge",
    ],
    "milch": ["milch", "vollmilch", "frischmilch", "h-milch", "haltbare milch", "1,5% fett"],
    "butter": ["butter", "deutsche markenbutter", "süßrahmbutter", "sauerrahmbutter"],
    "eier": ["eier", "ei", "freilandeier", "bodenhaltung", "bio-eier", "10er", "6er"],
    "tomaten": ["tomate", "tomaten", "rispentomaten", "cherrytomaten", "strauchtomaten"],
    "hackfleisch": ["hackfleisch", "gemischtes hack", "rinderhack", "gehacktes"],
    "haehnchen": ["hähnchen", "haehnchen", "hähnchenbrust", "hähnchenschenkel", "geflügel"],
    "apfel": ["apfel", "äpfel", "aepfel", "tafeläpfel"],
    "banane": ["banane", "bananen", "chiquita"],
    "brot": ["brot", "toast", "toastbrot", "vollkornbrot", "mischbrot"],
    "kaese": ["käse", "kaese", "gouda", "emmentaler", "mozzarella", "reibekäse"],
    "joghurt": ["joghurt", "jogurt", "naturjoghurt", "fruchtjoghurt"],
    "nudeln": ["nudeln", "pasta", "spaghetti", "penne", "fusilli", "hartweizen"],
    "reis": ["reis", "basmati", "langkornreis", "milchreis"],
    "mehl": ["mehl", "weizenmehl", "type 405", "dinkelmehl"],
    "zucker": ["zucker", "kristallzucker", "raffinade", "puderzucker"],
    "kaffee": ["kaffee", "röstkaffee", "bohnenkaffee", "gemahlener kaffee"],
    "bier": ["bier", "pils", "pilsener", "helles", "weizen", "radler"],
    "wasser": ["wasser", "mineralwasser", "tafelwasser", "sprudel", "still"],
    "zwiebeln": ["zwiebel", "zwiebeln", "speisezwiebeln"],
    "paprika": ["paprika", "paprikaschote"],
}

# reverse index: surface form -> canonical
_SURFACE_TO_CANON: dict[str, str] = {}
for canon, forms in SYNONYMS.items():
    _SURFACE_TO_CANON[canon] = canon
    for f in forms:
        _SURFACE_TO_CANON[f] = canon


def canonicalize(term: str) -> str:
    """Return the canonical key for a query/product term, or the cleaned term itself.

    Tries exact surface-form match first, then substring containment (so a long
    product name like "Speisekartoffeln festkochend 2kg" maps to "kartoffeln").
    """
    t = term.strip().lower()
    if t in _SURFACE_TO_CANON:
        return _SURFACE_TO_CANON[t]
    for surface, canon in _SURFACE_TO_CANON.items():
        if surface in t:
            return canon
    return t


def all_surface_forms(canon: str) -> list[str]:
    """All known surface forms for a canonical term (for query expansion)."""
    return [canon, *SYNONYMS.get(canon, [])]
