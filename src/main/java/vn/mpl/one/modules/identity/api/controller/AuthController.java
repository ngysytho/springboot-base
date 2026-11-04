package vn.mpl.one.modules.identity.api.controller;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import vn.mpl.one.core.response.ApiResponse;
import vn.mpl.one.core.security.*;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final CurrentUserProvider users;
  public AuthController(CurrentUserProvider users) { this.users = users; }
  @GetMapping("/me") public ApiResponse<AuthenticatedUser> me() { return ApiResponse.ok(users.current()); }
  @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName(), "parameterName", token.getParameterName());
  }
}
