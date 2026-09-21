package com.yasarbilgi.announcementtracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminServiceTest {

    private KeycloakAdminService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new KeycloakAdminService();
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "keycloakServerUrl", "http://keycloak.test");
        ReflectionTestUtils.setField(service, "keycloakRealm", "announcement-tracker-realm");
        ReflectionTestUtils.setField(service, "adminClientId", "announcement-tracker-admin");
        ReflectionTestUtils.setField(service, "adminClientSecret", "test-secret");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void provisionSubscriberCreatesUserAndAssignsSubscriberRole() {
        server.expect(once(), requestTo("http://keycloak.test/realms/announcement-tracker-realm/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("grant_type=client_credentials")))
                .andRespond(withSuccess("{\"access_token\":\"service-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(containsString(
                        "http://keycloak.test/admin/realms/announcement-tracker-realm/users?email=new.user@example.com&exact=true")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak.test/admin/realms/announcement-tracker-realm/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"username\":\"new.user@example.com\"")))
                .andRespond(withCreatedEntity(java.net.URI.create(
                        "http://keycloak.test/admin/realms/announcement-tracker-realm/users/user-123")));

        server.expect(once(), requestTo(
                        "http://keycloak.test/admin/realms/announcement-tracker-realm/roles/ROLE_SUBSCRIBER"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"role-1\",\"name\":\"ROLE_SUBSCRIBER\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(
                        "http://keycloak.test/admin/realms/announcement-tracker-realm/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("ROLE_SUBSCRIBER")))
                .andRespond(withNoContent());

        service.provisionSubscriber("NEW.User@Example.com", "New User", true);

        server.verify();
    }

    @Test
    void disabledSynchronizationDoesNotCallKeycloak() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.provisionSubscriber("user@example.com", "User", true);

        assertThat(service.isEnabled()).isFalse();
        server.verify();
    }
}
