# Pull request

## Summary

<!-- What changed and why. One or two sentences. -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Documentation
- [ ] Refactor / repository structure
- [ ] Firmware

## Checklist

- [ ] `python3 tools/validate_system.py` passes.
- [ ] `python3 tools/validate_firmware_contract.py` passes (firmware or
      `docs/PROTOCOL.md` changes).
- [ ] `python3 tools/validate_security.py` passes.
- [ ] `cd config && ./gradlew testAllModules detekt ktlintCheck` passes.
- [ ] New code, comments, and documentation are in English.
- [ ] No emoji, dead code, or migration markers were introduced.
