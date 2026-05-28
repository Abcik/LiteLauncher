# Bootstrap module

This module contains the public update/launch layer:

- manifest download/cache logic;
- launcher jar size/SHA-1 verification;
- atomic temp-file replacement;
- Java runtime download/extraction;
- main launcher process startup.

The official pixel progress UI is redacted and replaced by a console-safe entry point.
