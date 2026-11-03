package vn.mpl.one.core.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Timestamp auditing only. Actor/security event logging belongs in a separate audit facility. */
@Configuration
@EnableJpaAuditing
public class AuditConfig {}
