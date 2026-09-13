package com.opsdesk.incident;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityCalculatorTest {
    private final PriorityCalculator calculator = new PriorityCalculator();

    @ParameterizedTest
    @MethodSource("priorityMatrix")
    void calculatesEveryMatrixCombination(Impact impact, Urgency urgency, Priority expected) {
        assertThat(calculator.calculate(impact, urgency)).isEqualTo(expected);
    }

    private static Stream<Arguments> priorityMatrix() {
        return Stream.of(
                Arguments.of(Impact.LOW, Urgency.LOW, Priority.LOW),
                Arguments.of(Impact.LOW, Urgency.MEDIUM, Priority.LOW),
                Arguments.of(Impact.LOW, Urgency.HIGH, Priority.MEDIUM),
                Arguments.of(Impact.MEDIUM, Urgency.LOW, Priority.LOW),
                Arguments.of(Impact.MEDIUM, Urgency.MEDIUM, Priority.MEDIUM),
                Arguments.of(Impact.MEDIUM, Urgency.HIGH, Priority.HIGH),
                Arguments.of(Impact.HIGH, Urgency.LOW, Priority.MEDIUM),
                Arguments.of(Impact.HIGH, Urgency.MEDIUM, Priority.HIGH),
                Arguments.of(Impact.HIGH, Urgency.HIGH, Priority.CRITICAL));
    }
}
