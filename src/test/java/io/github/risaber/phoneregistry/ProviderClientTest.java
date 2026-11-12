package io.github.risaber.phoneregistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Сетевой вызов подменён MockRestServiceServer: реального HTTP-запроса нет.
 */
class ProviderClientTest {

    private static final String URL = "https://alpha.example/subscribers/77001234567";

    private MockRestServiceServer server;
    private ProviderClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ProviderClient(new Provider("alpha", "https://alpha.example/", "alpha-key"), builder);
    }

    @Test
    void okResponseMeansAvailable() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ProviderClient.API_KEY_HEADER, "alpha-key"))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThat(client.isAvailable("77001234567")).isTrue();
        server.verify();
    }

    @Test
    void notFoundMeansUnavailable() {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.isAvailable("77001234567")).isFalse();
        server.verify();
    }

    @Test
    void serverErrorIsPropagated() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.isAvailable("77001234567"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void otherClientErrorIsPropagated() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.isAvailable("77001234567"))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void networkErrorIsPropagated() {
        server.expect(requestTo(URL)).andRespond(withException(new IOException("connection reset")));

        assertThatThrownBy(() -> client.isAvailable("77001234567"))
                .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    void okStatusWithUnexpectedBodyIsAnError() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"status\":\"blocked\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.isAvailable("77001234567"))
                .isInstanceOf(UnexpectedProviderResponseException.class);
    }

    @Test
    void okBodyIsAcceptedWithAnyContentType() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.TEXT_PLAIN));

        assertThat(client.isAvailable("77001234567")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json", "[]", ""})
    void okStatusWithMalformedBodyIsAnError(String body) {
        server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.isAvailable("77001234567"))
                .isInstanceOf(UnexpectedProviderResponseException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 204, 302})
    void statusOtherThan200Or404IsAnError(int status) {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.valueOf(status))
                        .body("{\"status\":\"ok\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.isAvailable("77001234567"))
                .isInstanceOf(UnexpectedProviderResponseException.class)
                .hasMessageContaining(String.valueOf(status));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", ".", ".."})
    void invalidPhoneIsRejectedWithoutRequest(String phone) {
        assertThatThrownBy(() -> client.isAvailable(phone))
                .isInstanceOf(IllegalArgumentException.class);
        // Ни одного запроса к поставщику не ушло
        server.verify();
    }

    @Test
    void phoneIsEncodedAsSinglePathSegment() {
        server.expect(requestTo("https://alpha.example/subscribers/%2B7700%2F123"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.isAvailable("+7700/123")).isFalse();
        server.verify();
    }
}
