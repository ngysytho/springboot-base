package vn.mpl.one.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/** The security context and authorized client live together in the shared session. */
@Configuration
@Profile("production")
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "mpl:one:sessions")
public class RedisSessionConfig {
  @org.springframework.context.annotation.Bean
  org.springframework.session.web.http.CookieSerializer cookieSerializer() {
    var cookie = new org.springframework.session.web.http.DefaultCookieSerializer();
    cookie.setCookieName("JSESSIONID");
    cookie.setCookiePath("/");
    cookie.setUseHttpOnlyCookie(true);
    cookie.setUseSecureCookie(true);
    cookie.setSameSite("Lax");
    return cookie;
  }
}
