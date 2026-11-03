package vn.mpl.one.modules.identity.api.controller;

import org.springframework.web.bind.annotation.*;
import vn.mpl.one.core.response.ApiResponse;
import vn.mpl.one.core.security.*;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
  private final CurrentUserProvider users;

  public MeController(CurrentUserProvider users) {
    this.users = users;
  }

  @GetMapping
  public ApiResponse<AuthenticatedUser> me() {
    return ApiResponse.ok(users.current());
  }
}
