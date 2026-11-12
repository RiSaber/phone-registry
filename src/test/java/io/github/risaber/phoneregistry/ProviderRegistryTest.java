package io.github.risaber.phoneregistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProviderRegistryTest {

    private static final Provider ALPHA = new Provider("alpha", "https://alpha.example", "alpha-key");
    private static final Provider BETA = new Provider("beta", "https://beta.example/v1", "beta-key");

    private MockRestServiceServer server;
    private ProviderRegistry registry;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        registry = new ProviderRegistry(builder);
    }

    @Test
    void routesRequestToProviderById() {
        registry.replaceAll(new ListProviderSource(List.of(ALPHA, BETA)));

        server.expect(requestTo("https://beta.example/v1/subscribers/77001234567"))
                .andExpect(header(ProviderClient.API_KEY_HEADER, "beta-key"))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://alpha.example/subscribers/77001234567"))
                .andExpect(header(ProviderClient.API_KEY_HEADER, "alpha-key"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(registry.isAvailable("beta", "77001234567")).isTrue();
        assertThat(registry.isAvailable("alpha", "77001234567")).isFalse();
        server.verify();
    }

    @Test
    void unknownProviderIsRejected() {
        registry.replaceAll(List.of(ALPHA));

        assertThatThrownBy(() -> registry.isAvailable("gamma", "77001234567"))
                .isInstanceOf(UnknownProviderException.class);
        assertThatThrownBy(() -> registry.isAvailable(null, "77001234567"))
                .isInstanceOf(UnknownProviderException.class);
    }

    @Test
    void replaceAllSwapsWholeSet() {
        registry.replaceAll(List.of(ALPHA));
        registry.replaceAll(List.of(BETA));

        assertThat(registry.providerIds()).containsExactly("beta");
        assertThatThrownBy(() -> registry.client("alpha"))
                .isInstanceOf(UnknownProviderException.class);
    }

    @Test
    void duplicateIdsKeepPreviousSet() {
        registry.replaceAll(List.of(ALPHA));
        Provider alphaCopy = new Provider("alpha", "https://other.example", "other-key");

        assertThatThrownBy(() -> registry.replaceAll(List.of(BETA, alphaCopy, ALPHA)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(registry.providerIds()).containsExactly("alpha");
    }
}
