# Minecraft Server Activity Monitor

Poll Minecraft Servers for their online player count

## Features

- Monitor the **player activity** of multiple Minecraft Servers using [mcstatus.io](https://mcstatus.io) API
- Supports **multiple Storage Backends**: CSV, Databases coming soon
- Highly **configurable** with Environment Variables
- **Web UI** for Server Management and rudimentary Data Viewing
- Fully documented **REST API** to be integrated into your stack
- Easy Deployment using **Docker**

## Deployment

Run the Application using Docker:
```shell
docker run -d \
  --name mc-monitor \
  --restart unless-stopped \
  -p 8080:8080 \
  -e TZ=Europe/Berlin \
  -v $(pwd)/data:/app \
  ghcr.io/velyn-n/minecraft-server-activity-monitor:latest
```

Or Docker-Compose:
```yaml
services:
  mc-monitor:
    image: ghcr.io/velyn-n/minecraft-server-activity-monitor:latest
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - TZ=Europe/Berlin
    volumes:
      - "./data:/app"
```

Ensure the Files in `/app` is writable by the container

#### *!!! Always remember to set the Timezone !!!*

## Configuration

The following Configuration Properties can be used to configure the application:

| Property                      | Environment Variable          | Default Value              |
|-------------------------------|-------------------------------|----------------------------|
| storage.file.servers          | STORAGE_FILE_SERVERS          | /app/servers.csv           |
| storage.file.activity.records | STORAGE_FILE_ACTIVITY_RECORDS | /app/activity-records.csv  |
| scheduler.server.check.cron   | SCHEDULER_SERVER_CHECK_CRON   | 0 * * * * ? (every minute) |
|                               |                               |                            |


## API Documentation

OpenAPI and Swagger UI are included and available at runtime:

- OpenAPI (YAML): http://localhost:8080/api-docs/mc-activity-monitor
- OpenAPI (JSON): http://localhost:8080/api-docs/mc-activity-monitor?format=json
- Swagger UI: http://localhost:8080/swagger-ui
