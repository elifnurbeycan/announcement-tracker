package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.entity.Announcement;

import java.util.List;

public interface EmailService {

    /**
     * Sends notification email to subscribers for a list of new announcements.
     */
    void sendAnnouncementNotification(List<Announcement> newAnnouncements, List<String> recipientEmails);

    /**
     * Sends notification for a single announcement.
     */
    void sendSingleAnnouncementNotification(Announcement announcement, List<String> recipientEmails);
}
