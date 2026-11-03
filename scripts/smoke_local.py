#!/usr/bin/env python3
"""Exercise the local demo realm's real authorization-code + PKCE flow and Lesson API.
Requires the local profile, seeded teacher account, and Keycloak. Never prints tokens.
"""
import base64
import hashlib
import html.parser
import http.cookiejar
import json
import os
import secrets
import urllib.error
import urllib.parse
import urllib.request

API = os.environ.get("SMOKE_API", "http://localhost:8081")
ISSUER = os.environ.get("SMOKE_ISSUER", "http://localhost:8180/realms/mpl-one")
ORG = "11111111-1111-1111-1111-111111111111"
CALLBACK = "http://localhost:8081/swagger-ui/oauth2-redirect.html"


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class LoginForm(html.parser.HTMLParser):
    action = None

    def handle_starttag(self, tag, attrs):
        values = dict(attrs)
        if tag == "form" and values.get("id") == "kc-form-login":
            self.action = values.get("action")


class LocalhostCookiePolicy(http.cookiejar.DefaultCookiePolicy):
    def return_ok_secure(self, cookie, request):
        # Browsers treat localhost as a trustworthy origin for Secure cookies.
        # Match that behavior only for localhost in this local-development test.
        if urllib.parse.urlparse(request.full_url).hostname == "localhost":
            return True
        return super().return_ok_secure(cookie, request)


opener = urllib.request.build_opener(
    urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar(LocalhostCookiePolicy())), NoRedirect()
)


def request(url, method="GET", body=None, headers=None):
    req = urllib.request.Request(url, data=body, headers=headers or {}, method=method)
    try:
        with opener.open(req, timeout=30) as response:
            return response.status, response.headers, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.headers, error.read()


def authenticate():
    verifier = secrets.token_urlsafe(48)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
    state = secrets.token_urlsafe(24)
    query = urllib.parse.urlencode({
        "client_id": "mpl-web", "redirect_uri": CALLBACK, "response_type": "code",
        "scope": "openid profile", "state": state,
        "code_challenge": challenge, "code_challenge_method": "S256",
    })
    status, _, page = request(ISSUER + "/protocol/openid-connect/auth?" + query)
    assert status == 200, f"Login page returned {status}"
    form = LoginForm()
    form.feed(page.decode())
    assert form.action, "Keycloak login form not found"
    credentials = urllib.parse.urlencode({
        "username": os.environ.get("DEMO_USERNAME", "teacher"),
        "password": os.environ.get("DEMO_PASSWORD", "demo-change-me"),
        "credentialId": "",
    }).encode()
    status, headers, _ = request(form.action, "POST", credentials,
                                 {"Content-Type": "application/x-www-form-urlencoded"})
    assert status == 302, f"Login did not complete: HTTP {status}"
    callback = urllib.parse.urlparse(headers["Location"])
    parameters = urllib.parse.parse_qs(callback.query)
    assert parameters.get("state") == [state], "OAuth state mismatch"
    assert "code" in parameters, "Authorization code missing"
    token_request = urllib.parse.urlencode({
        "grant_type": "authorization_code", "client_id": "mpl-web",
        "redirect_uri": CALLBACK, "code": parameters["code"][0], "code_verifier": verifier,
    }).encode()
    status, _, response = request(ISSUER + "/protocol/openid-connect/token", "POST",
                                  token_request, {"Content-Type": "application/x-www-form-urlencoded"})
    assert status == 200, f"Token exchange returned {status}"
    return json.loads(response)["access_token"]


def main():
    token = authenticate()
    headers = {"Authorization": "Bearer " + token, "X-Organization-Id": ORG,
               "Content-Type": "application/json"}

    def api(path, method="GET", data=None, expected=200):
        status, _, body = request(API + path, method,
                                  None if data is None else json.dumps(data).encode(), headers)
        assert status == expected, f"{method} {path} returned {status}, expected {expected}"
        return json.loads(body)["data"]

    user = api("/api/v1/me")
    assert user["subject"] == "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    lesson = api("/api/v1/lessons", "POST", {"title": "PKCE smoke test"}, 201)
    path = "/api/v1/lessons/" + lesson["id"]
    try:
        lesson = api(path, "PUT", {"title": "PKCE smoke revised", "version": lesson["version"]})
        lesson = api(path + "/publish", "POST", {"version": lesson["version"]})
        assert lesson["status"] == "PUBLISHED"
        page = api("/api/v1/lessons?" + urllib.parse.urlencode({"filter": "title:like:PKCE smoke"}))
        assert any(row["id"] == lesson["id"] for row in page["rows"])
    finally:
        current = api(path)
        api(path + "?version=" + str(current["version"]), "DELETE")
    print("PASS: Keycloak authorization code + PKCE, JWT audience/roles, /me, Lesson CRUD/publish/filter/delete")


if __name__ == "__main__":
    main()
