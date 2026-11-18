package org.amupoti.pandemic.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalNetworkTest {

    @Test
    void onlyListsLocalNetworkAddressesWithThePort() {
        assertThat(LocalNetwork.urls(8080)).allSatisfy(url -> {
            assertThat(url).matches("http://(10|172|192)\\.[0-9.]+:8080/");
            assertThat(url).doesNotContain("127.0.0.1");
        });
    }
}
