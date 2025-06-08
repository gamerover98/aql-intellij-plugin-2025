<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# AQL IntelliJ Plugin Changelog

## [Unreleased]

### Changed

- Updated to the IntelliJ IDEA 2025.1

## [1.0.8] - 2021-12-06

### Added

- `WINDOW` keyword
- Remaining v3.7 and v3.8 functions:
    - `IPV4_FROM_NUMBER`, `REPLACE_NTH`, `NGRAM_MATCH`, `NGRAM_SIMILARITY`
    - `NGRAM_POSITIONAL_SIMILARITY`, `DATE_UTCTOLOCAL`, `DATE_LOCALTOUTC`
    - `BIT_AND`, `BIT_OR`, `BIT_XOR`, `BIT_NEGATE`, `BIT_TEST`, `BIT_POPCOUNT`, `BIT_SHIFT_LEFT`
    - `BIT_SHIFT_RIGHT`, `BIT_CONSTRUCT`, `BIT_DECONSTRUCT`, `BIT_TO_STRING`, `BIT_FROM_STRING`
    - `GEO_IN_RANGE`, `DATE_TIMEZONE`, `DATE_TIMEZONES`

### Fixed

- Syntax highlighting (+ function type "relaxing")

## [1.0.7] - 2021-11-26

### Added

- SSL connection support
- 3.8.x database support

### Fixed

- Bugfixes

## [1.0.6] - unknown-date

### Changed

- (No details provided)

## [1.0.5] - 2019-10-23

### Added

- New analyzers, keywords and functions (grammar/autocomplete)
- Function parameter hints (CTRL+P)
- Live template support

### Fixed

- Bug fixes

## [1.0.4] - 2019-10-12

### Added

- 2019.2+ support
- Use password manager to store passwords

## [1.0.3] - 2018-12-09

### Added

- Create SpringData Repository interface intention

### Fixed

- Bug fixes

## [1.0.2] - 2018-12-09

### Fixed

- Bug fixes

## [1.0.1] - 2018-12-05

### Fixed

- Bug fixes (syntax highlighting, documentation)

## [1.0.0] - 2018-12-03

### Added

- Initial release (syntax highlighting, autocompletion, refactorings, spring data query language injection)