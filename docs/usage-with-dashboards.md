# Usage with Dashboards

## gethomepage.dev Widget

You can use the "Custom API" Widget type to add some key insights to your Dashboard:

```yaml
    - Mc-Monitor:
        href: https://your-installation.example.com
        icon: sh-minecraft
        description: Minecraft Server Activity Monitoring
        siteMonitor: https://your-installation.example.com
        widget:
          type: customapi
          url: https://your-installation.example.com/rest/statistics
          mappings:
            - field: serverCount
              label: Servers
              format: number
            - field: recordedActivitiesCount
              label: Data Points
              format: number
            - field: recordedDaysCount
              label: Recorded Days
              format: number
```
