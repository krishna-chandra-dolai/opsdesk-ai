from __future__ import annotations

import csv
import json
from pathlib import Path

import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, precision_recall_fscore_support
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

BASE_DIR = Path(__file__).resolve().parent
DATA_PATH = BASE_DIR / "data" / "tickets.csv"
MODEL_PATH = BASE_DIR / "model.joblib"
EXPECTED_CATEGORIES = {"NETWORK", "HARDWARE", "SOFTWARE", "ACCESS", "EMAIL", "OTHER"}


def load_dataset(path: Path = DATA_PATH) -> tuple[list[str], list[str]]:
    with path.open(encoding="utf-8", newline="") as source:
        rows = list(csv.DictReader(source))
    texts = [row["text"] for row in rows]
    labels = [row["category"] for row in rows]
    if set(labels) != EXPECTED_CATEGORIES:
        raise ValueError("Dataset must contain all six supported categories")
    return texts, labels


def train_and_save(model_path: Path = MODEL_PATH) -> dict[str, float | int]:
    texts, labels = load_dataset()
    train_texts, test_texts, train_labels, test_labels = train_test_split(
        texts, labels, test_size=0.25, random_state=42, stratify=labels
    )
    model = Pipeline([
        ("tfidf", TfidfVectorizer(lowercase=True, ngram_range=(1, 2))),
        ("classifier", LogisticRegression(max_iter=1000, random_state=42)),
    ])
    model.fit(train_texts, train_labels)
    predictions = model.predict(test_texts)
    precision, recall, f1, _ = precision_recall_fscore_support(
        test_labels, predictions, average="macro", zero_division=0
    )
    metrics = {
        "dataset_size": len(texts),
        "training_size": len(train_texts),
        "test_size": len(test_texts),
        "accuracy": round(float(accuracy_score(test_labels, predictions)), 4),
        "macro_precision": round(float(precision), 4),
        "macro_recall": round(float(recall), 4),
        "macro_f1": round(float(f1), 4),
    }
    joblib.dump(model, model_path)
    return metrics


if __name__ == "__main__":
    print(json.dumps(train_and_save(), indent=2))
