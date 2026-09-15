package com.yasarbilgi.announcementtracker.site.controller;

import com.yasarbilgi.announcementtracker.controller.SiteController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SiteControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private SiteController siteController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(siteController).build();
    }

    @Test
    @DisplayName("GET /api/v1/sites - Tüm aktif duyuru kaynaklarını listeleme HTTP 200")
    void getAllSites_ShouldReturnSiteList() throws Exception {
        mockMvc.perform(get("/api/v1/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data[0].name", is("EBELGE_GIB")))
                .andExpect(jsonPath("$.data[1].name", is("KOSGEB")));
    }
}
