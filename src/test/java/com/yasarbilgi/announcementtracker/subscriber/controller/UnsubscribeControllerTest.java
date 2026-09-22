package com.yasarbilgi.announcementtracker.subscriber.controller;

import com.yasarbilgi.announcementtracker.controller.UnsubscribeController;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnsubscribeControllerTest {

    @Mock
    private SubscriberService subscriberService;

    @InjectMocks
    private UnsubscribeController unsubscribeController;

    @Test
    void confirmationPage_ShouldNotChangeSubscription() {
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "csrf-value");

        ResponseEntity<String> response = unsubscribeController.confirmationPage("valid-token", csrfToken);

        assertThat(response.getBody()).contains("Abonelikten Çık", "csrf-value", "method='post'");
        verify(subscriberService, never()).unsubscribeByToken(anyString());
    }

    @Test
    void confirm_ShouldDeactivateSubscriber() {
        when(subscriberService.unsubscribeByToken("valid-token")).thenReturn(true);

        ResponseEntity<String> response = unsubscribeController.confirm("valid-token");

        assertThat(response.getBody()).contains("Abonelik İptal Edildi");
        verify(subscriberService).unsubscribeByToken("valid-token");
    }
}
