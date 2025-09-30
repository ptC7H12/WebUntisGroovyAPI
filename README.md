

# WebUntis Groovy API

A Spring Boot REST API for WebUntis, written in Groovy. This API provides access to timetables, homework, substitutions, and other data from WebUntis systems.

## Features

- **Standard WebUntis API**: Basic functions like timetables, subjects, teachers, rooms
- **WebUntis 2017 API**: Extended functions with detailed information
  - Homework
  - Messages
  - Absences
  - Enhanced timetable data with master data
- **Automatic OTP generation**: Internal time-based one-time password generation from App Secret
- **Automatic data formatting**: Formatted times, weekdays, substitution information
- **Docker support**: Easy deployment with Docker and Docker Compose

## Technology Stack

- **Spring Boot 3.1.5**: Backend framework
- **Groovy**: Programming language
- **Gradle 8.13**: Build tool
- **Java 17**: Runtime environment
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

 Build with Gradle Wrapper
./gradlew build

# Start application
./gradlew bootRun
```
The API will be available at http://localhost:8080.
## Docker Deployment
```bash
# Build Docker image and start container
docker-compose up -d

# View logs
docker-compose logs -f

# Stop container
docker-compose down
```

## API Endpoints
### Standard API (Username/Password)
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
    "substitutionInfo": null,
    "originalTeachers": [],
    "originalRooms": [],
    "originalSubjects": [],
    "originalClasses": []
  }
]
```

#### Get Weekly Timetable
```http
POST /api/webuntis/timetable/week
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
Returns an array of timetable entries for the current week (Monday to Friday), same format as today's timetable.

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
**Response:**
Returns an array of timetable entries for the specified date range, same format as today's timetable.

#### Get Subjects
```http
POST /api/webuntis/subjects
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
    "id": 1,
    "name": "M",
    "longName": "Mathematics",
    "foreColor": "#000000",
    "backColor": "#FFFF00",
    "active": true
  },
  {
    "id": 2,
    "name": "E",
    "longName": "English",
    "foreColor": "#FFFFFF",
    "backColor": "#0000FF",
    "active": true
  }
]
```
#### Get Teachers
```http
POST /api/webuntis/teachers
Content-Type: application/json
```
**Request Body:**
```json{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com"
}
```
**Response:**
```json[
  {
    "id": 1,
    "name": "DOE",
    "foreName": "John",
    "longName": "John Doe",
    "active": true
  },
  {
    "id": 2,
    "name": "SMI",
    "foreName": "Jane",
    "longName": "Jane Smith",
    "active": true
  }
]
```

#### Get  Rooms
```http
POST /api/webuntis/rooms
Content-Type: application/json
```
**Request Body:**
```json{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com"
}
```
**Response:**
```json[
  {
    "id": 1,
    "name": "A101",
    "longName": "Room A101",
    "active": true
  },
  {
    "id": 2,
    "name": "B202",
    "longName": "Room B202",
    "active": true
  }
]
```

### WebUntis 2017 API (App Secret Required)
The 2017 API requires an App Secret token. The API automatically generates the required time-based one-time password (OTP) internally - you only need to provide the App Secret.
#### Get  Enhanced Timetable
```http
POST /api/webuntis/v2017/timetable
Content-Type: application/json
```
**Request Body:**
```json{
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
Element Types:
```
STUDENT - Student (default)
TEACHER - Teacher
CLASS - Class
ROOM - Room
```

**Response:**
```json{
  "status": "success",
  "format": "2017-standard",
  "dataCount": 25,
  "data": [
    {
      "id": 123456,
      "lessonId": 789012,
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
          "firstName": "John",
          "lastName": "Doe"
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
      "foreColor": "#000000",
      "backColor": "#FFFFFF",
      "isOnlinePeriod": false,
      "lessonText": null,
      "substitutionText": null,
      "info": null,
      "homeWorks": [],
      "substitutionInfo": null,
      "originalTeachers": [],
      "originalRooms": [],
      "originalSubjects": [],
      "originalClasses": [],
      "is2017Format": true
    }
  ]
}
```

#### Get  Homework
```http
POST /api/webuntis/v2017/homework
Content-Type: application/json
```
**Request Body:**
```json{
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
```json[
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
    "teachers": [
      {
        "id": 1,
        "name": "DOE",
        "firstName": "John",
        "lastName": "Doe"
      }
    ],
    "classes": [
      {
        "id": 1,
        "name": "10a",
        "longName": "Class 10a"
      }
    ],
    "attachments": []
  }
]
```
Status Values:
```
completed - Homework is done
overdue - Due date has passed
due_today - Due today
pending - Not yet due
```

#### Get  Messages
```http
POST /api/webuntis/v2017/messages
Content-Type: application/json
```
**Request Body:**
```json{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com",
  "appSecret": "XXXXXXXXXXXXXX",
  "date": "2025-09-29"
}
```
**Response:**
```json[
  {
    "id": 456,
    "subject": "School Event Reminder",
    "text": "Don't forget the parent-teacher conference tomorrow at 6 PM.",
    "isRead": false,
    "date": 20250929,
    "sender": "School Administration"
  }
]
```

#### Get  Absences
```http
POST /api/webuntis/v2017/absences
Content-Type: application/json
```
**Request Body:**
```json{
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
**Response:**
```json[
  {
    "id": 321,
    "startDate": 20250930,
    "startDateFormatted": "2025-09-30",
    "endDate": 20250930,
    "endDateFormatted": "2025-09-30",
    "startTime": 800,
    "startTimeFormatted": "08:00",
    "endTime": 945,
    "endTimeFormatted": "09:45",
    "timeRange": "08:00 - 09:45",
    "excused": true,
    "statusDescription": "Excused",
    "reason": "Medical appointment",
    "text": "Doctor's note provided"
  }
]
```

#### Get  User Data and Master Data
```http
POST /api/webuntis/v2017/userdata
Content-Type: application/json
```
**Request Body:**
```json{
  "school": "SchoolName",
  "username": "username",
  "password": "password",
  "server": "server.webuntis.com",
  "appSecret": "XXXXXXXXXXXXXX"
}
```
**Response:**
```json{
  "masterData": {
    "subjects": {
      "1": {
        "id": 1,
        "name": "M",
        "longName": "Mathematics",
        "departmentIds": [1],
        "foreColor": "#000000",
        "backColor": "#FFFF00",
        "active": true,
        "displayAllowed": true
      }
    },
    "teachers": {
      "1": {
        "id": 1,
        "name": "DOE",
        "firstName": "John",
        "lastName": "Doe",
        "departmentIds": [1],
        "foreColor": null,
        "backColor": null,
        "entryDate": "2020-09-01",
        "exitDate": null,
        "active": true,
        "displayAllowed": true
      }
    },
    "rooms": {
      "1": {
        "id": 1,
        "name": "A101",
        "longName": "Room A101",
        "departmentId": 1,
        "foreColor": null,
        "backColor": null,
        "active": true,
        "displayAllowed": true
      }
    },
    "klassen": {
      "1": {
        "id": 1,
        "name": "10a",
        "longName": "Class 10a",
        "departmentId": 1,
        "startDate": "2024-09-01",
        "endDate": "2025-07-31",
        "foreColor": null,
        "backColor": null,
        "active": true,
        "displayable": true
      }
    },
    "timeGrid": {
      "MONDAY": [
        {
          "label": "1",
          "startTime": "08:00",
          "endTime": "08:45"
        },
        {
          "label": "2",
          "startTime": "08:50",
          "endTime": "09:35"
        }
      ]
    },
    "schoolyears": [
      {
        "id": 1,
        "name": "2024/2025",
        "startDate": "2024-09-01",
        "endDate": "2025-07-31"
      }
    ],
    "holidays": [
      {
        "id": 1,
        "name": "Christmas Break",
        "longName": "Christmas Holidays 2024",
        "startDate": "2024-12-23",
        "endDate": "2025-01-06"
      }
    ],
    "departments": {
      "1": {
        "id": 1,
        "name": "Main",
        "longName": "Main Department"
      }
    },
    "absenceReasons": {
      "1": {
        "id": 1,
        "name": "Sick",
        "longName": "Illness",
        "active": true,
        "automaticNotificationEnabled": true
      }
    },
    "excuseStatuses": {
      "1": {
        "id": 1,
        "name": "Excused",
        "longName": "Excused Absence",
        "excused": true,
        "active": true
      }
    },
    "duties": {},
    "eventReasons": {},
    "teachingMethods": [],
    "timestamp": 1727616000000
  },
  "userData": {
    "elemType": "STUDENT",
    "elemId": 12345,
    "displayName": "John Doe",
    "schoolName": "Sample School",
    "departmentId": 1,
    "children": [],
    "klassenIds": [1],
    "rights": ["READ_TIMETABLE", "READ_HOMEWORK", "READ_MESSAGES"]
  }
}
```

### Generating an App Secret
To use the enhanced 2017 API, you need to generate an App Secret token:

1. Open the official WebUntis Mobile App
2. Go to your profile
3. Navigate to Sharing
4. Under "Untis mobile" show the QR Code
5. Copy the generated App Secret

**Important Notes:**
- The App Secret is school-specific and user-specific
- Once generated, save the App Secret securely - it cannot be retrieved again
- The API automatically generates time-based one-time passwords (OTP) from this secret
- You only need to provide the App Secret - no manual OTP generation required

## Configuration
### Environment Variables (docker-compose.yaml)
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - JAVA_OPTS=-Xmx512m -Xms256m
```
### Port Configuration
Default port: `8080`
Change in `docker-compose.yaml:`
```yaml
ports:
  - "8080:8080"  # Host:Container
```
### Architecture
```
src/main/groovy/de/c7h12/webuntis/
├── WebUntisApplication.groovy          # Spring Boot main class
├── client/
│   ├── WebUntisClient.groovy           # API client with OTP generation
│   ├── WebUntisSession.groovy          # Session management
│   └── WebUntisException.groovy        # Exception handling
├── service/
│   └── WebUntisService.groovy          # Business logic
└── controller/
    └── WebUntisController.groovy       # REST endpoints
```
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
- 5-minute cache validity - reduces API calls
- Automatic reloading on demand when cache expires
- Lazy loading - master data is only fetched when needed

### Substitution Detection
The API automatically detects substitutions and prepares detailed information:

- Substitute teachers - automatic detection and comparison
- Room changes - original vs. current room
- Subject changes - when lessons are replaced
- Additional information - substitution text, notes, and remarks

### Error Handling
The API returns structured error messages:
```json
{
  "error": "Error description"
}
```
## Common errors:

`Authentication failed:` Incorrect credentials or invalid App Secret
`appSecret is required:` 2017 API endpoint called without App Secret parameter
`Timetable request failed:` Problem fetching timetable data
`Invalid App Secret format:` App Secret must be in Base32 format

## Development
**Build Project**
`bash./gradlew clean build`
**Run Tests**
`bash./gradlew test`
### Code Style
The project follows Groovy conventions with Spring Boot best practices.
### Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

### Fork the project
1. Create your feature branch (git checkout -b feature/AmazingFeature)
2. Commit your changes (git commit -m 'Add some AmazingFeature')
3. Push to the branch (git push origin feature/AmazingFeature)
4. Open a Pull Request

### License
This project is licensed under the GNU General Public License v3.0 - see the LICENSE file for details.
GPL-3.0 Summary
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
## Disclaimer
This API is an unofficial project and is not affiliated with Untis GmbH. Use at your own risk. Please respect the terms of service of your WebUntis instance.


