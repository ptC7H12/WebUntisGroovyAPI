# WebUntis Groovy API

A comprehensive Spring Boot REST API for WebUntis, written in Groovy. This API provides access to timetables, homework, substitutions, and other data from WebUntis systems with additional QR-code extraction capabilities.

> 📝 **Note**: Most of this codebase was created with assistance from Claude (Anthropic), an AI assistant specialized in software development.

## Features

- **Standard WebUntis API**: Basic functions like timetables, subjects, teachers, rooms
- **WebUntis 2017 API**: Extended functions with detailed information
  - Homework with status tracking
  - Messages and notifications
  - Absences with excuse status
  - Enhanced timetable data with master data
  - Holidays and school year information
- **QR-Code Extraction**: Extract WebUntis login credentials from QR codes
  - Support for multiple image formats (JPEG, PNG, GIF, BMP)
  - Multiple upload methods (multipart, base64, binary)
  - Enhanced image processing for poor quality images
  - Automatic parsing of `untis://` and `otpauth://` URIs
  - One-shot authentication from QR code
- **Automatic OTP generation**: Internal time-based one-time password generation from App Secret
- **Automatic data formatting**: Formatted times, weekdays, substitution information
- **Master data caching**: 5-minute cache for subjects, teachers, rooms, and classes
- **Substitution detection**: Automatic detection of teacher, room, and subject changes
- **Docker support**: Easy deployment with Docker and Docker Compose

## Technology Stack

- **Spring Boot 3.1.5**: Backend framework
- **Groovy**: Programming language
- **Gradle 8.13**: Build tool
- **Java 17**: Runtime environment
- **ZXing**: QR-code processing library
- **Docker**: Containerization

## Installation

### Prerequisites

- Java 17 or higher
- Gradle 8.13 or higher (optional, as Gradle Wrapper is included)
- Docker and Docker Compose (for container deployment)

### Local Development

```bash
# Clone repository
git clone <repository-url>
cd UntisGroovy

# Build with Gradle Wrapper
./gradlew build

# Start application
./gradlew bootRun
```

The API will be available at http://localhost:8080.

### Docker Deployment

```bash
# Build Docker image and start container
docker-compose up -d

# View logs
docker-compose logs -f

# Stop container
docker-compose down
```

## API Endpoints

### Health Check

```http
GET /api/qrcode/health
```

Returns the service status and available endpoints.

---

### QR-Code Extraction

#### Extract from Image Upload

Extract WebUntis credentials from a QR-code image:

```http
POST /api/qrcode/extract
Content-Type: multipart/form-data
```

**Request:**
- `file`: Image file (JPEG, PNG, GIF, or BMP)

**Response:**
```json
{
  "success": true,
  "type": "untis_setschool",
  "url": "hepta.webuntis.com",
  "school": "my-school",
  "user": "username",
  "key": "OL6KK2YTPFCZ4JLU",
  "server": "https://hepta.webuntis.com",
  "schoolNumber": "1234567",
  "fileName": "qrcode.png",
  "fileSize": 45678,
  "timestamp": 1727616000000,
  "rawContent": "untis://setschool?url=..."
}
```

#### Extract from Base64

For n8n workflows and Telegram bot integration:

```http
POST /api/qrcode/extract/base64
Content-Type: application/json
```

**Request Body:**
```json
{
  "image": "data:image/png;base64,iVBORw0KGgoAAAANS..."
}
```

#### Extract from Binary Data

```http
POST /api/qrcode/extract/binary
Content-Type: application/octet-stream
Body: <binary image data>
```

#### Extract with Enhanced Processing

For images with poor quality or lighting:

```http
POST /api/qrcode/extract/enhanced
Content-Type: multipart/form-data
```

Uses grayscale conversion and contrast enhancement.

#### Extract and Authenticate (One-Shot)

```http
POST /api/qrcode/extract/authenticate
Content-Type: multipart/form-data
```

Extracts QR-code and provides authentication-ready data.

---

### Standard WebUntis API (Username/Password)

#### Get Today's Timetable

```http
POST /api/webuntis/timetable/today
Content-Type: application/json
```

**Request Body:**
```json
{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com"
}
```

**Response:**
```json
[
  {
    "id": 123456,
    "date": 20250929,
    "dateFormatted": "2025-09-29",
    "weekday": "Monday",
    "startTime": 800,
    "startTimeFormatted": "08:00",
    "endTime": 945,
    "endTimeFormatted": "09:45",
    "timeRange": "08:00 - 09:45",
    "durationMinutes": 105,
    "subjects": [
      {
        "id": 1,
        "name": "M",
        "longName": "Mathematics"
      }
    ],
    "teachers": [
      {
        "id": 1,
        "name": "DOE",
        "foreName": "John",
        "longName": "Doe"
      }
    ],
    "rooms": [
      {
        "id": 1,
        "name": "A101",
        "longName": "Room A101"
      }
    ],
    "classes": [
      {
        "id": 1,
        "name": "10a",
        "longName": "Class 10a"
      }
    ],
    "code": "REGULAR",
    "lessonStatus": {
      "status": "normal",
      "description": "Regular lesson",
      "color": "green"
    },
    "substitutionInfo": null
  }
]
```

#### Get Weekly Timetable

```http
POST /api/webuntis/timetable/week
Content-Type: application/json
```

Returns the timetable for the current week (Monday to Friday).

#### Get Timetable for Date Range

```http
POST /api/webuntis/timetable/range
Content-Type: application/json
```

**Request Body:**
```json
{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com",
  "startDate": "2025-09-29",
  "endDate": "2025-10-06"
}
```

#### Get Subjects

```http
POST /api/webuntis/subjects
Content-Type: application/json
```

Returns all subjects with colors and status.

#### Get Teachers

```http
POST /api/webuntis/teachers
Content-Type: application/json
```

Returns all teachers with full names.

#### Get Rooms

```http
POST /api/webuntis/rooms
Content-Type: application/json
```

Returns all rooms.

---

### WebUntis 2017 API (App Secret Required)

The 2017 API requires an App Secret token. The API automatically generates the required time-based one-time password (OTP) internally - you only need to provide the App Secret.

#### Generating an App Secret

To use the enhanced 2017 API, you need to generate an App Secret token:

1. Open the official WebUntis Mobile App
2. Go to your profile
3. Navigate to **Sharing**
4. Under "Untis mobile" show the QR Code
5. Use the QR-Code extraction endpoint or copy the generated App Secret

**Important Notes:**
- The App Secret is school-specific and user-specific
- Once generated, save the App Secret securely - it cannot be retrieved again
- The API automatically generates time-based one-time passwords (OTP) from this secret
- You only need to provide the App Secret - no manual OTP generation required

#### Get Enhanced Timetable

```http
POST /api/webuntis/v2017/timetable
Content-Type: application/json
```

**Request Body:**
```json
{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com",
  "appSecret": "XXXXXXXXXXXXXX",
  "startDate": "2025-09-29",
  "endDate": "2025-10-06",
  "elementType": "STUDENT"
}
```

**Element Types:**
- `STUDENT` - Student (default)
- `TEACHER` - Teacher
- `CLASS` - Class
- `ROOM` - Room

**Response includes:**
- Lesson IDs and detailed information
- Homework assignments attached to lessons
- Online period flags
- Foreground/background colors
- Substitution texts and info
- Original elements (for substitutions)

#### Get Homework

```http
POST /api/webuntis/v2017/homework
Content-Type: application/json
```

**Request Body:**
```json
{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com",
  "appSecret": "XXXXXXXXXXXXXX",
  "startDate": "2025-09-29",
  "endDate": "2025-10-06"
}
```

**Response:**
```json
[
  {
    "id": 789,
    "lessonId": 123456,
    "startDate": "2025-09-29",
    "endDate": "2025-10-06",
    "text": "Page 42, exercises 1-5",
    "remark": "Please show all calculation steps",
    "completed": false,
    "status": "pending",
    "statusDescription": "Pending",
    "subject": {
      "id": 1,
      "name": "M",
      "longName": "Mathematics"
    },
    "teachers": [...],
    "classes": [...],
    "attachments": []
  }
]
```

**Status Values:**
- `completed` - Homework is done
- `overdue` - Due date has passed
- `due_today` - Due today
- `pending` - Not yet due

#### Get Messages

```http
POST /api/webuntis/v2017/messages
Content-Type: application/json
```

**Request Body:**
```json
{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com",
  "appSecret": "XXXXXXXXXXXXXX",
  "date": "2025-09-29"
}
```

#### Get Absences

```http
POST /api/webuntis/v2017/absences
Content-Type: application/json
```

**Request Body:**
```json
{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com",
  "appSecret": "XXXXXXXXXXXXXX",
  "startDate": "2025-09-29",
  "endDate": "2025-10-06",
  "includeExcused": true,
  "includeUnexcused": true
}
```

#### Get Holidays

```http
POST /api/webuntis/v2017/holidays
Content-Type: application/json
```

**Request Body:**
```json
{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com",
  "appSecret": "XXXXXXXXXXXXXX"
}
```

**Response includes:**
- Holiday dates and duration
- Status (upcoming, current, past)
- Days until start/end
- Holiday type categorization (summer, christmas, easter, etc.)
- School year information

#### Get User Data and Master Data

```http
POST /api/webuntis/v2017/userdata
Content-Type: application/json
```

**Request Body:**
```json
{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com",
  "appSecret": "XXXXXXXXXXXXXX"
}
```

**Response includes complete master data:**
- Subjects with colors and departments
- Teachers with entry/exit dates
- Rooms with assignments
- Classes with date ranges
- Time grid for all weekdays
- School years
- Holidays
- Departments
- Absence reasons and excuse statuses
- User data and rights

---

## Configuration

### Environment Variables (docker-compose.yaml)

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - JAVA_OPTS=-Xmx512m -Xms256m
```

### Port Configuration

Default port: `8080`

Change in `docker-compose.yaml`:
```yaml
ports:
  - "8080:8080"  # Host:Container
```

### File Upload Limits

Configure in `application.properties`:
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## Architecture

```
src/main/groovy/de/c7h12/webuntis/
├── WebUntisApplication.groovy          # Spring Boot main class
├── client/
│   ├── WebUntisClient.groovy           # API client with OTP generation
│   ├── WebUntisSession.groovy          # Session management
│   └── WebUntisException.groovy        # Exception handling
├── service/
│   ├── WebUntisService.groovy          # Business logic
│   └── QRCodeService.groovy            # QR-code extraction logic
└── controller/
    ├── WebUntisController.groovy       # REST endpoints for WebUntis
    └── QRCodeController.groovy         # REST endpoints for QR-codes
```

---

## Features in Detail

### Automatic OTP Generation (2017 API)

The API implements complete TOTP (Time-based One-Time Password) generation internally:
- **Base32 decoding** of the App Secret
- **HMAC-SHA1** based OTP generation
- **30-second time window** synchronization
- **Automatic token refresh** - no manual intervention required

You only need to provide the App Secret once - the API handles all OTP generation and refresh cycles automatically.

### Master Data Caching

For optimal performance, master data (subjects, teachers, rooms, etc.) is automatically cached:
- **5-minute cache validity** - reduces API calls
- **Automatic reloading** on demand when cache expires
- **Lazy loading** - master data is only fetched when needed

### QR-Code Processing

The QR-code extraction service supports:
- **Multiple encodings** (UTF-8, ISO-8859-1, Windows-1252)
- **Image enhancement** for poor quality images
- **URI parsing** for `untis://setschool` and `otpauth://totp` formats
- **Automatic normalization** of server URLs

### Substitution Detection

The API automatically detects substitutions and prepares detailed information:
- **Substitute teachers** - automatic detection and comparison
- **Room changes** - original vs. current room
- **Subject changes** - when lessons are replaced
- **Additional information** - substitution text, notes, and remarks

---

## Error Handling

The API returns structured error messages:

```json
{
  "error": "Error description"
}
```

### Common Errors

- `Authentication failed` - Incorrect credentials or invalid App Secret
- `appSecret is required` - 2017 API endpoint called without App Secret parameter
- `Timetable request failed` - Problem fetching timetable data
- `Invalid App Secret format` - App Secret must be in Base32 format
- `Kein QR-Code im Bild gefunden` - No QR-code detected in image
- `Ungültiges Bildformat` - Invalid image format

---

## Troubleshooting

### QR-Code Cannot Be Read

1. **Try enhanced processing:**
   ```http
   POST /api/qrcode/extract/enhanced
   ```

2. **Check image quality:**
   - Ensure good lighting
   - Image should be at least 200x200 pixels
   - QR-code should fill most of the image
   - Avoid blurry or rotated images

3. **Supported formats:**
   - JPEG (.jpg, .jpeg)
   - PNG (.png)
   - GIF (.gif)
   - BMP (.bmp)

### Authentication Issues

1. **Standard API (Username/Password):**
   - Verify credentials are correct
   - Check if school name matches exactly
   - Ensure server URL is correct (without https://)

2. **2017 API (App Secret):**
   - Verify App Secret is valid and not expired
   - App Secret must be in Base32 format
   - Generate a new App Secret if needed

### Special Characters in School Name

If your school name contains special characters (spaces, umlauts, etc.):
- The API handles URL encoding automatically
- Do NOT manually encode the school name
- Use the exact school name as shown in WebUntis

### Master Data Not Loading

The API automatically loads master data when needed. If issues occur:
- Check if the user has permissions to view subjects/teachers/rooms
- Some WebUntis instances restrict access to certain data
- The API logs "no permission" warnings - these are informational, not errors

---

## Development

### Build Project

```bash
./gradlew clean build
```

### Run Tests

```bash
./gradlew test
```

### Code Style

The project follows Groovy conventions with Spring Boot best practices.

---

## License

This project is licensed under the GNU General Public License v3.0 - see the LICENSE file for details.

### GPL-3.0 Summary

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

---

## Acknowledgments

- Most of this codebase was created with assistance from **Claude** (Anthropic), an AI assistant
- Built with Spring Boot and Groovy
- Uses ZXing for QR-code processing
- Inspired by the WebUntis community

---

## Disclaimer

This API is an unofficial project and is not affiliated with Untis GmbH. Use at your own risk. Please respect the terms of service of your WebUntis instance.
