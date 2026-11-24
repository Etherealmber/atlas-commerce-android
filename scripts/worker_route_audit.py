"""Helpers for matching Retrofit routes against Hono Worker route declarations."""

from __future__ import annotations

import re


_STRING_PATH_CONSTANT = re.compile(
    r"\b(?:export\s+)?const\s+([A-Z][A-Z0-9_]*)\s*=\s*(['\"])(/[^'\"]+)\2"
)
_ROUTE_TEMPLATE = re.compile(
    r"\b\w+\.(?:get|post|put|patch|delete)\(\s*`([^`]+)`",
    re.IGNORECASE,
)
_TEMPLATE_VARIABLE = re.compile(r"\$\{([A-Z][A-Z0-9_]*)\}")


def composed_worker_routes(source: str) -> set[str]:
    """Resolve path constants used in registered template-string route expressions.

    This intentionally does not treat constants, middleware paths, or arbitrary strings as
    endpoints: only the first argument of an HTTP route registration is considered.
    """
    constants = {name: path for name, _quote, path in _STRING_PATH_CONSTANT.findall(source)}
    resolved: set[str] = set()
    for expression in _ROUTE_TEMPLATE.findall(source):
        path = expression
        # Resolve nested constant templates without permitting unbounded expansion.
        for _ in range(len(constants) + 1):
            next_path = _TEMPLATE_VARIABLE.sub(
                lambda match: constants.get(match.group(1), match.group(0)),
                path,
            )
            if next_path == path:
                break
            path = next_path
        if "${" not in path and path.startswith("/"):
            resolved.add(path)
    return resolved


def worker_has_route(prefix: str, source: str) -> bool:
    """Retain existing literal-prefix matching and include statically composed routes."""
    if prefix in source:
        return True
    return any(route.startswith(prefix) for route in composed_worker_routes(source))
