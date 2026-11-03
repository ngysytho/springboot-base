package vn.mpl.one;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.*;
import vn.mpl.one.config.SecurityConfig;

class JwtValidationTest {
  HttpServer server;
  RSAKey key;
  JwtDecoder decoder;

  @BeforeEach
  void setup() throws Exception {
    key = new RSAKeyGenerator(2048).keyID("test").generate();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/jwks",
        exchange -> {
          byte[] bytes = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (var out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });
    server.start();
    decoder =
        new SecurityConfig()
            .jwtDecoder(
                "https://issuer.example/realms/mpl-one",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks");
  }

  @AfterEach
  void close() {
    server.stop(0);
  }

  String token(String issuer, String audience, Instant expires, RSAKey signingKey)
      throws Exception {
    var jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").build(),
            new JWTClaimsSet.Builder()
                .subject("user")
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expires))
                .build());
    jwt.sign(new RSASSASigner(signingKey));
    return jwt.serialize();
  }

  @Test
  void validatesSignatureIssuerAudienceAndExpiry() throws Exception {
    assertThat(
            decoder
                .decode(
                    token(
                        "https://issuer.example/realms/mpl-one",
                        "mpl-api",
                        Instant.now().plusSeconds(300),
                        key))
                .getSubject())
        .isEqualTo("user");
    String
        wrongIssuer = token("https://evil.example", "mpl-api", Instant.now().plusSeconds(300), key),
        wrongAudience =
            token(
                "https://issuer.example/realms/mpl-one",
                "other",
                Instant.now().plusSeconds(300),
                key),
        expired =
            token(
                "https://issuer.example/realms/mpl-one",
                "mpl-api",
                Instant.now().minusSeconds(300),
                key),
        forged =
            token(
                "https://issuer.example/realms/mpl-one",
                "mpl-api",
                Instant.now().plusSeconds(300),
                new RSAKeyGenerator(2048).generate());
    String missingAudience =
        token("https://issuer.example/realms/mpl-one", null, Instant.now().plusSeconds(300), key);
    for (String invalid :
        new String[] {wrongIssuer, wrongAudience, missingAudience, expired, forged})
      assertThatThrownBy(() -> decoder.decode(invalid)).isInstanceOf(JwtException.class);
  }
}
