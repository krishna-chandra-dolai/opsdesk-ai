package com.opsdesk.incident;

import org.springframework.stereotype.Component;

@Component
public class PriorityCalculator {
    public Priority calculate(Impact impact, Urgency urgency) {
        return switch (impact) {
            case LOW -> switch (urgency) {
                case LOW, MEDIUM -> Priority.LOW;
                case HIGH -> Priority.MEDIUM;
            };
            case MEDIUM -> switch (urgency) {
                case LOW -> Priority.LOW;
                case MEDIUM -> Priority.MEDIUM;
                case HIGH -> Priority.HIGH;
            };
            case HIGH -> switch (urgency) {
                case LOW -> Priority.MEDIUM;
                case MEDIUM -> Priority.HIGH;
                case HIGH -> Priority.CRITICAL;
            };
        };
    }
}
