package com.healthlens.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    @DisplayName("resolve uses first valid X-Forwarded-For IP")
    void resolve_usesForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "198.51.100.99, 10.0.0.10");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.99");
    }

    @Test
    @DisplayName("resolve falls back to remote addr when forwarded IP is invalid")
    void resolve_fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.44");
        request.addHeader("X-Forwarded-For", "not-an-ip");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.44");
    }

    @Test
    @DisplayName("resolve accepts compressed IPv6 forwarded IP")
    void resolve_acceptsCompressedIpv6() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "::1, 10.0.0.10");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("::1");
    }

    @Test
    @DisplayName("resolve accepts IPv4-mapped IPv6 forwarded IP")
    void resolve_acceptsIpv4MappedIpv6() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "::ffff:192.0.2.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("::ffff:192.0.2.1");
    }
}
