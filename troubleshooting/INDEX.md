# Troubleshooting

Something broken? Start here.

Pick the stage where it failed, then follow the path.

## Build Stage

Problems before the app runs.

```
troubleshooting/build/
  (reserved)
```

## Runtime Stage

Problems while the app is running.

```
troubleshooting/runtime/
  (reserved)
```

## Compatibility

Problems caused by environment differences.

```
troubleshooting/compatibility/
  (reserved)
```

## Network

Problems with connectivity, mirrors, endpoints.

```
troubleshooting/network/
  ENDPOINT_PROBER.md                Network probing & endpoint health
```

## Quick Reference

| Symptom | Go to |
|---|---|
| Build fails in CI | `build/` |
| Gradle error | `build/` |
| App crashes on launch | `runtime/` |
| White screen | `runtime/` |
| WebView incompatibility | `compatibility/` |
| API unreachable | `network/` |
| Mirror not working | `network/` |

---

Not here? Check [docs/INDEX.md](../docs/INDEX.md) for feature docs.
