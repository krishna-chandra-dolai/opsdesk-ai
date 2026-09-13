package com.opsdesk.ai;

import com.opsdesk.incident.Category;

public record AiPrediction(Category category, double confidence) {
}
