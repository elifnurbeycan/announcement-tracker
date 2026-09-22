package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.response.AnnouncementResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.AnnouncementService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPortalControllerTest {

    @Mock
    private SubscriberService subscriberService;

    @Mock
    private AnnouncementService announcementService;

    @InjectMocks
    private UserPortalController controller;

    @Test
    @DisplayName("Kullanıcı duyuruları kişisel değil efektif site kapsamıyla filtrelenmeli")
    void getMyAnnouncements_UsesDepartmentAndPersonalEffectiveSites() {
        Set<SiteType> effectiveSites = Set.of(SiteType.EBELGE_GIB);
        SubscriberResponseDto user = SubscriberResponseDto.builder()
                .subscribedSites(Set.of(SiteType.EBELGE_GIB))
                .departmentSites(Set.of(SiteType.EBELGE_GIB))
                .effectiveSites(effectiveSites)
                .build();
        PageRequest pageable = PageRequest.of(0, 10);

        when(announcementService.getAnnouncementsForSites(effectiveSites, null, null, false, pageable))
                .thenReturn(Page.<AnnouncementResponseDto>empty(pageable));

        controller.getMyAnnouncements(user, null, null, false, pageable);

        verify(announcementService).getAnnouncementsForSites(effectiveSites, null, null, false, pageable);
    }
}
