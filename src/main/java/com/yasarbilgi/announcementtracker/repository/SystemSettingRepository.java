package com.yasarbilgi.announcementtracker.repository;

import com.yasarbilgi.announcementtracker.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * SystemSetting varlıkları için veritabanı erişim arayüzü.
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
