package com.yasarbilgi.announcementtracker.dto.session;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Sunucu tarafında oluşturulan opak uygulama oturum anahtarı.
 * HTTP yanıt gövdesine yazılmaz; yalnızca güvenli oturum çerezine aktarılır.
 */
public record SessionToken(@JsonIgnore String value) {
}
