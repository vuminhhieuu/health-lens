import pytest
import requests
from fastapi import HTTPException

import app


class _FakeSocket:
    def __init__(self, peer_ip):
        self._peer_ip = peer_ip

    def getpeername(self):
        return (self._peer_ip, 443)


class _FakeConnection:
    def __init__(self, peer_ip):
        self.sock = _FakeSocket(peer_ip)


class _FakeRaw:
    def __init__(self, peer_ip):
        self.connection = _FakeConnection(peer_ip)


class _FakeResponse:
    def __init__(self, status_code=200, headers=None, chunks=None, peer_ip="3.3.3.3"):
        self.status_code = status_code
        self.headers = headers or {}
        self._chunks = chunks if chunks is not None else [b"ok"]
        self.raw = _FakeRaw(peer_ip)

    def raise_for_status(self):
        if self.status_code >= 400:
            raise requests.exceptions.HTTPError("http error")

    def iter_content(self, chunk_size=8192):
        for chunk in self._chunks:
            yield chunk

    def close(self):
        return None


def test_blocks_ip_literal_even_if_allowed_domain_env(monkeypatch):
    monkeypatch.setattr(app, "ALLOWED_FETCH_DOMAINS", ("amazonaws.com",))
    with pytest.raises(HTTPException) as exc:
        app._validate_fetch_url("https://127.0.0.1/file.png")
    assert exc.value.status_code == 400


def test_blocks_private_dns_resolution(monkeypatch):
    monkeypatch.setattr(app, "ALLOWED_FETCH_DOMAINS", ("example.com",))
    monkeypatch.setattr(
        app.socket,
        "getaddrinfo",
        lambda *args, **kwargs: [(None, None, None, None, ("10.0.0.4", 0))],
    )
    with pytest.raises(HTTPException) as exc:
        app._validate_fetch_url("https://cdn.example.com/image.png")
    assert exc.value.status_code == 400


def test_allows_public_storage_domain(monkeypatch):
    monkeypatch.setattr(app, "ALLOWED_FETCH_DOMAINS", ("amazonaws.com",))
    monkeypatch.setattr(
        app.socket,
        "getaddrinfo",
        lambda *args, **kwargs: [(None, None, None, None, ("3.5.140.1", 0))],
    )
    host = app._validate_fetch_url("https://bucket.s3.amazonaws.com/image.png")
    assert host == "bucket.s3.amazonaws.com"


def test_redirect_target_is_revalidated_and_blocked(monkeypatch):
    monkeypatch.setattr(app, "ALLOWED_FETCH_DOMAINS", ("example.com", "amazonaws.com"))

    def fake_getaddrinfo(host, *args, **kwargs):
        if host.endswith("example.com"):
            return [(None, None, None, None, ("3.3.3.3", 0))]
        return [(None, None, None, None, ("127.0.0.1", 0))]

    monkeypatch.setattr(app.socket, "getaddrinfo", fake_getaddrinfo)

    responses = [
        _FakeResponse(status_code=302, headers={"Location": "https://127.0.0.1/secret.png"}),
    ]

    def fake_get(*args, **kwargs):
        return responses.pop(0)

    monkeypatch.setattr("requests.get", fake_get)

    with pytest.raises(HTTPException) as exc:
        app._download_remote_image("https://safe.example.com/image.png")
    assert exc.value.status_code == 400


def test_http_error_is_mapped_to_client_download_failure(monkeypatch):
    monkeypatch.setattr(app, "ALLOWED_FETCH_DOMAINS", ("example.com",))
    monkeypatch.setattr(
        app.socket,
        "getaddrinfo",
        lambda *args, **kwargs: [(None, None, None, None, ("3.3.3.3", 0))],
    )
    monkeypatch.setattr("requests.get", lambda *args, **kwargs: _FakeResponse(status_code=404))

    with pytest.raises(HTTPException) as exc:
        app._download_remote_image("https://safe.example.com/missing.png")
    assert exc.value.status_code == 400
    assert exc.value.detail == "Failed to download image"


def test_blocks_when_connected_peer_ip_is_private(monkeypatch):
    monkeypatch.setattr(app, "ALLOWED_FETCH_DOMAINS", ("example.com",))
    monkeypatch.setattr(
        app.socket,
        "getaddrinfo",
        lambda *args, **kwargs: [(None, None, None, None, ("3.3.3.3", 0))],
    )
    monkeypatch.setattr(
        "requests.get",
        lambda *args, **kwargs: _FakeResponse(status_code=200, peer_ip="127.0.0.1"),
    )

    with pytest.raises(HTTPException) as exc:
        app._download_remote_image("https://safe.example.com/file.png")
    assert exc.value.status_code == 400
