from pathlib import Path

from fastapi.testclient import TestClient

from app import create_app
from train import EXPECTED_CATEGORIES, train_and_save


def test_health_and_valid_prediction(tmp_path: Path):
    model_path = tmp_path / "model.joblib"
    train_and_save(model_path)

    with TestClient(create_app(model_path)) as client:
        health = client.get("/health")
        prediction = client.post("/predict", json={
            "title": "VPN disconnected",
            "description": "Corporate VPN drops every five minutes",
        })

    assert health.status_code == 200
    assert health.json() == {"status": "UP"}
    assert prediction.status_code == 200
    assert prediction.json()["category"] in EXPECTED_CATEGORIES
    assert 0 <= prediction.json()["confidence"] <= 1


def test_blank_text_is_rejected(tmp_path: Path):
    model_path = tmp_path / "model.joblib"
    train_and_save(model_path)

    with TestClient(create_app(model_path)) as client:
        response = client.post("/predict", json={"title": "   ", "description": "details"})

    assert response.status_code == 422
