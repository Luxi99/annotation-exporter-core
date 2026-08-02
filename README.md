# Annotation Exporter Core

[![](https://jitpack.io/v/Luxi99/annotation-exporter-core.svg)](https://jitpack.io/#Luxi99/annotation-exporter-core)

**Annotation Exporter Core** is the shared Java library that contains the core export logic used by the Annotation Exporter ecosystem. It is designed to be reused across multiple projects, including the QuPath extension and standalone QuPath scripts, avoiding code duplication and ensuring consistent behavior.

## Installation

The library is distributed through **JitPack** and can be added to any Gradle or Maven project.

For the latest version and installation instructions for different build automation tools (Gradle, Maven, SBT, Leiningen, etc.), visit the JitPack page:

https://jitpack.io/#Luxi99/annotation-exporter-core

## Scripting
This library can also be used in standalone scripts, more information on how to do that
can be found [here](examples/README.md). Also, in the examples folder you will find an annotation
extractor [script](examples/export_annotations.groovy) that uses this library and whose logic the
[QuPath Annotation Exporter Extension](https://github.com/Luxi99/qupath-extension-annotation-exporter)
is based on.

## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
