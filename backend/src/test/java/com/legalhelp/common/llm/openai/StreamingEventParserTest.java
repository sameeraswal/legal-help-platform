package com.legalhelp.common.llm.openai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingEventParserTest {

    private final StreamingEventParser parser = new StreamingEventParser();

    @Test
    void extractsPayloadFromDataLine() {
        Optional<String> payload = parser.extractDataPayload("data: {\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}");

        assertThat(payload).contains("{\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}");
    }

    @Test
    void toleratesMissingSpaceAfterColon() {
        Optional<String> payload = parser.extractDataPayload("data:{\"a\":1}");

        assertThat(payload).contains("{\"a\":1}");
    }

    @Test
    void ignoresTheDoneSentinel() {
        Optional<String> payload = parser.extractDataPayload("data: [DONE]");

        assertThat(payload).isEmpty();
    }

    @Test
    void isDoneRecognizesTheSentinelPayload() {
        assertThat(parser.isDone("[DONE]")).isTrue();
        assertThat(parser.isDone("{\"a\":1}")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", ": comment", "event: ping", "id: 42"})
    void ignoresNonDataLines(String line) {
        assertThat(parser.extractDataPayload(line)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isNullSafe(String line) {
        assertThat(parser.extractDataPayload(line)).isEmpty();
    }

    @Test
    void ignoresBlankDataPayload() {
        assertThat(parser.extractDataPayload("data:")).isEmpty();
        assertThat(parser.extractDataPayload("data:   ")).isEmpty();
    }
}
