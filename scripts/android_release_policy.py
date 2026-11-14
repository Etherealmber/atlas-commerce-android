"""Semantic-version helpers shared by release automation and its unit tests."""

from __future__ import annotations

import re
from dataclasses import dataclass


_IDENTIFIER = r"(?:0|[1-9][0-9]*|[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*)"
_VERSION = re.compile(
    rf"^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"
    rf"(?:-({_IDENTIFIER}(?:\.{_IDENTIFIER})*))?"
    rf"(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?$"
)


@dataclass(frozen=True, order=True)
class ReleaseVersion:
    major: int
    minor: int
    patch: int
    prerelease: str | None = None
    build: str | None = None

    @property
    def is_stable(self) -> bool:
        return self.prerelease is None

    @property
    def supported_series(self) -> str:
        if not self.is_stable:
            raise ValueError("Prerelease versions do not define the supported stable line")
        return f"{self.major}.{self.minor}.x"

    @property
    def supported_floor(self) -> str:
        if not self.is_stable:
            raise ValueError("Prerelease versions do not define the supported stable line")
        return f"{self.major}.{self.minor}.0"

    @property
    def numeric(self) -> tuple[int, int, int]:
        return self.major, self.minor, self.patch


def parse_release_version(value: str) -> ReleaseVersion:
    """Parse a strict SemVer value; a leading v/V is accepted for Git tags."""
    candidate = value.strip()
    if candidate[:1].lower() == "v":
        candidate = candidate[1:]
    match = _VERSION.fullmatch(candidate)
    if not match:
        raise ValueError(f"Invalid semantic release version: {value!r}")
    major, minor, patch, prerelease, build = match.groups()
    return ReleaseVersion(int(major), int(minor), int(patch), prerelease, build)


def supported_series_for(value: str) -> str | None:
    version = parse_release_version(value)
    return version.supported_series if version.is_stable else None


def is_newer_stable(candidate: str, current: str) -> bool:
    """Compare stable release baselines without letting prereleases advance support."""
    proposed = parse_release_version(candidate)
    installed = parse_release_version(current)
    return proposed.is_stable and proposed.numeric > installed.numeric
