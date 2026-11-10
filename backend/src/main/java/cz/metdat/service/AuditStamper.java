package cz.metdat.service;

import cz.metdat.domain.TechnicalTest;
import cz.metdat.domain.TestType;
import cz.metdat.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Sets Created/Modified by/at exclusively from the server-verified identity and server clock (never from request data). */
@Component
public class AuditStamper {
    private final CurrentUser user;
    private final Clock clock;

    public AuditStamper(CurrentUser user, Clock clock) { this.user = user; this.clock = clock; }

    /** PostgreSQL stores microseconds; truncating keeps API values identical before and after a round trip. */
    private Instant now() { return Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS); }

    public void created(TechnicalTest t) { t.setCreatedBy(user.email()); t.setCreatedAt(now()); }
    public void modified(TechnicalTest t) { t.setModifiedBy(user.email()); t.setModifiedAt(now()); }
    public void created(TestType t) { t.setCreatedBy(user.email()); t.setCreatedAt(now()); }
    public void modified(TestType t) { t.setModifiedBy(user.email()); t.setModifiedAt(now()); }
}
