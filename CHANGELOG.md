# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.1] - 2026-11-22

### Changed

- A failed Mapbox image no longer stops the post. `PostService` now logs the failure, reports it to Rollbar, and
  continues with a text-only post.
- A failed image upload no longer stops a post on any platform. The Twitter/X, Bluesky, and Mastodon publishers now log
  the failure, report it to Rollbar, and publish a text-only post. If only the Twitter/X alt text fails, the tweet is
  still posted with the image.
- `TwitterClient` now reports "Failed to set alt text" instead of "Failed to set altText text" when the alt text
  response cannot be parsed.
- The Twitter/X `Media` response no longer requires `media_key`, which was never used. Only `id` is read.
- `MastodonClient.postStatus` now rejects a `null` text up front, like the other client methods.
- The README now notes which secrets are read per request and which are read once at startup.

### Fixed

- The Mapbox download now has a 5 second connect timeout and a 10 second read timeout. Previously a stalled connection
  could block until the Lambda timeout.
- `MapboxException` no longer keeps the underlying download error as its cause. For a non-2xx response the JDK includes
  the full request URL in that error, and the URL contains the Mapbox access token, which could end up in the logs and
  Rollbar.

## [2.0.0] - 2026-11-21

### Added

- Unit test suite (JUnit 6 and Mockito) covering the post service, publishers, platform clients, DTOs, and
  configuration.
- JaCoCo coverage report, with a build failure on `mvn verify` if line coverage falls below 85%.
- Per-post alt text describing the map (train, destination, and station) on Twitter/X, Bluesky, and Mastodon.
  Previously only Bluesky had alt text, and it was a fixed string.
- Dependabot updates for GitHub Actions, grouped Spring updates, a 3-day cooldown, and a limit of 5 open Maven PRs.
- Coverage report upload (`jacoco-report`) from the pull request and merge workflows.

### Changed

- **Breaking:** Java 25 is now required (previously Java 21).
- **Breaking:** Twitter/X now authenticates with OAuth 1.0a through ScribeJava instead of OAuth 2.0 token refresh. The
  `twitter` secret now contains `consumerKey`, `consumerKeySecret`, `accessToken`, and `accessTokenSecret`.
- **Breaking:** `AWS_REGION` is now required, and `APP_ROLLBAR_CODE_VERSION` is no longer read; the code version comes
  from the build info.
- **Breaking:** The base package and Maven `groupId` changed from `app.cta4j` to `com.cta4j`.
- Bluesky and Mastodon now use Spring's `RestClient`, and the per-platform service classes were consolidated into
  `BlueskyClient`, `MastodonClient`, `TwitterClient`, and `MapboxClient`.
- `PostService.buildPost` now returns a nullable `Post` instead of an `Optional`, and the code is annotated with
  JSpecify nullness annotations.
- The secret is now read through the AWS Secrets Manager caching library (`aws-secretsmanager-caching-java`) instead of
  being loaded once at startup, and the application only reads it; it no longer writes to Secrets Manager.
- The post text now says "Holiday train" when the line is unknown instead of failing, and uses the 🎅🏻 emoji.
- The temporary Mapbox image file is now deleted if the download fails.
- The build workflows now run `mvn clean verify` (tests and the coverage check) on Corretto 25 with the Maven cache from
  `setup-java`, and the release workflow was updated to JDK 25 and current action versions.
- The release workflow now waits for the published Lambda version to become active (needed for SnapStart) and moves the
  `live` alias to it, creating the alias on the first release. Point the EventBridge rule at the alias.
- Bump `org.springframework.boot:spring-boot-starter-parent` from `4.0.0` to `4.2.0`.
- Bump `com.cta4j:cta4j-java-sdk` from `3.0.3` to `8.0.1`.
- Bump the AWS SDK BOM from `2.38.7` to `2.59.3`.
- Bump `org.springframework.cloud:spring-cloud-function-adapter-aws` from `4.3.0` to `5.0.5`, now managed by the
  Spring Cloud `2025.1.4` BOM.
- Bump `com.rollbar:rollbar-java` from `2.0.0` to `2.1.0`.
- Bump `commons-io:commons-io` from `2.21.0` to `2.22.0`.
- Bump `org.apache.maven.plugins:maven-shade-plugin` from `3.6.1` to `3.6.2`.
- Use the Lombok version managed by Spring Boot instead of pinning `1.18.42`.
- Update the project version to `2.0.0` and the license copyright year to 2026.
- Update the README for the new Twitter/X credentials, environment variables, deployment steps, and testing.

### Removed

- The Twitter/X OAuth 2.0 token refresh interceptor and service, including writing refreshed tokens back to Secrets
  Manager. The `secretsmanager:PutSecretValue` permission is no longer needed.
- The `org.apache.httpcomponents.client5:httpclient5`, `com.nimbusds:oauth2-oidc-sdk`,
  `com.fasterxml.jackson.datatype:jackson-datatype-jsr310`, and `com.amazonaws:aws-lambda-java-core` dependencies.

## [1.0.4] - 2025-11-28

### Changed

- Bump `com.cta4j:cta4j-java-sdk` from `3.0.2` to `3.0.3`.

## [1.0.3] - 2025-11-28

### Changed

- Bump `com.cta4j:cta4j-java-sdk` from `3.0.0` to `3.0.2`.

## [1.0.2] - 2025-11-28

### Changed

- Fix GitHub Actions release publishing.
- Bump `org.springframework.boot:spring-boot-starter-parent` from `3.5.7` to `4.0.0`.

## [1.0.1] - 2025-11-28

### Added

- Added LICENSE file.

### Changed

- Updated Release GitHub Action to include release notes.

## [1.0.0] - 2025-11-28

### Added

- Initial release of bot.

[Unreleased]: https://github.com/lbkulinski/cta-holiday-bot/compare/v2.0.1...HEAD
[2.0.1]: https://github.com/lbkulinski/cta-holiday-bot/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/lbkulinski/cta-holiday-bot/compare/v1.0.4...v2.0.0
[1.0.4]: https://github.com/lbkulinski/cta-holiday-bot/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/lbkulinski/cta-holiday-bot/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/lbkulinski/cta-holiday-bot/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/lbkulinski/cta-holiday-bot/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/lbkulinski/cta-holiday-bot/releases/tag/v1.0.0
