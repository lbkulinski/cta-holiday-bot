# CTA Holiday Bot

Automated social-media updates for the Chicago Transit Authority Holiday Train using real-time CTA data.
This bot posts arrival updates, map snapshots, and route information to platforms such as Twitter/X, Bluesky, and Mastodon, using the cta4j SDK and media APIs.

## Features

### Real-Time CTA Tracking
- Fetches upcoming train arrivals using the CTA Train Tracker API
- Tracks the Holiday Train
- Supports run-number following (e.g. 1225 in the case of the Holiday Train)

### Map Snapshot Generation
- Creates map snapshots using Mapbox
- Generates PNG images (e.g., 512×512)
- Handles media upload for each platform

### Social Media Posting
| Platform  | Supported | Notes |
|-----------|-----------|-------|
| Twitter/X | Yes       |       |
| Bluesky   | Yes       |       |
| Mastodon  | Yes       |       |
| Threads   | No        | WIP   |

## Architecture Overview
- Java 21+
- Spring Boot 4
- Apache HttpClient 5
- Custom OAuth2 token-refresh interceptor for Twitter/X
- Deployed to AWS Lambda

## Getting Started

### 1. Clone the Repo
```bash
git clone https://github.com/lbkulinski/cta-holiday-bot.git
cd cta-holiday-bot
```

### 2. Environment Variables
| Variable                          | Description                                             |
|-----------------------------------|---------------------------------------------------------|
| APP_ROLLBAR_ENVIRONMENT           | The environment the code is running in (e.g. localhost) |
| APP_ROLLBAR_CODE_VERSION          | The version number of the code                          |
| APP_AWS_SECRETS_MANAGER_SECRET_ID | The ID of the secret to read from Secrets Manager       |
| APP_CTA_TRAIN_RUN                 | The CTA train run number to track                       |

### 3. AWS Secrets Manager
The application expects a JSON object in AWS Secrets Manager with the following structure:
```json
{
  "twitter": {
    "clientId": "...",
    "clientSecret": "...",
    "accessToken": "...",
    "refreshToken": "...",
    "expirationTime": "2025-11-08T23:34:46.250661Z"
  },
  "bluesky": {
    "identifier": "...",
    "appPassword": "..."
  },
  "mapbox": {
    "accessToken": "..."
  },
  "cta": {
    "trainApiKey": "..."
  },
  "rollbar": {
    "accessToken": "..."
  },
  "mastodon": {
    "accessToken": "..."
  }
}
```

For Twitter/X you will have to go through the OAuth2 flow to get the initial access and refresh tokens.

### 4. Run Locally
```bash
./mvnw spring-boot:run
```

### 5. Deploy to AWS Lambda (Optional)
- Package using Maven Shade
- Upload shaded JAR to AWS Lambda
- Use the LambdaHandler entrypoint
- Can also deploy via AWS CDK (Java)

## Example Output


## Future Enhancements
- Full Threads API integration

## Contributing
Contributions are welcome! Please open issues or submit pull requests for bug fixes and new features.

## License
Apache License 2.0
