from __future__ import annotations

import os
from contextlib import asynccontextmanager
from pathlib import Path

import joblib
from fastapi import FastAPI
from pydantic import BaseModel, Field, field_validator

BASE_DIR = Path(__file__).resolve().parent
DEFAULT_MODEL_PATH = Path(os.getenv("MODEL_PATH", BASE_DIR / "model.joblib"))


class PredictionRequest(BaseModel):
    title: str = Field(min_length=1, max_length=150)
    description: str = Field(min_length=1, max_length=5000)

    @field_validator("title", "description")
    @classmethod
    def reject_blank_text(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("must not be blank")
        return value


class PredictionResponse(BaseModel):
    category: str
    confidence: float


def create_app(model_path: Path = DEFAULT_MODEL_PATH) -> FastAPI:
    @asynccontextmanager
    async def lifespan(application: FastAPI):
        application.state.model = joblib.load(model_path)
        yield

    api = FastAPI(title="OpsDesk AI Classification Service", version="0.1.0", lifespan=lifespan)

    @api.get("/health")
    def health() -> dict[str, str]:
        return {"status": "UP"}

    @api.post("/predict", response_model=PredictionResponse)
    def predict(request: PredictionRequest) -> PredictionResponse:
        text = f"{request.title} {request.description}"
        probabilities = api.state.model.predict_proba([text])[0]
        best_index = int(probabilities.argmax())
        return PredictionResponse(
            category=str(api.state.model.classes_[best_index]),
            confidence=float(probabilities[best_index]),
        )

    return api


app = create_app()
