## 参数ip
```
from(bucket: "mini_seed")
|> range(start: v.timeRangeStart, stop: v.timeRangeStop)
|> keyValues(keyColumns:["ip"])
|> group()
|> keep(columns:["ip"])
|> distinct(column:"ip")
```
## 参数channel
```
from(bucket: "mini_seed")
|> range(start: v.timeRangeStart, stop: v.timeRangeStop)
|> filter(fn: (r) => r["_measurement"] == "sensor_data")
|> filter(fn: (r) => r["ip"] =~ /^${ip}$/)
|> keyValues(keyColumns:["channel"])
|> group()
|> keep(columns:["channel"])
|> distinct(column:"channel")
```
## 参数station
```
from(bucket: "mini_seed")
|> range(start: v.timeRangeStart, stop: v.timeRangeStop)
|> filter(fn: (r) => r["_measurement"] == "sensor_data")
|> filter(fn: (r) => r["ip"] =~ /^${ip}$/)
|> filter(fn: (r) => r["channel"] =~ /^${channels}$/)
|> keyValues(keyColumns:["station"])
|> group()
|> keep(columns:["station"])
|> distinct(column:"station")
```
## 查询
```
from(bucket: "mini_seed")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "sensor_data")
  |> filter(fn: (r) => r["_field"] == "channel" or r["_field"] == "network" or r["_field"] == "new_channel" or r["_field"] == "time_stamp" or r["_field"] == "v")
  |> filter(fn:(r) => r.channel=~ /${channels}/)
  |> filter(fn:(r) => r.station=~ /${station}/)

```
### json model
```
{
  "annotations": {
    "list": [
      {
        "builtIn": 1,
        "datasource": {
          "type": "grafana",
          "uid": "-- Grafana --"
        },
        "enable": true,
        "hide": true,
        "iconColor": "rgba(0, 211, 255, 1)",
        "name": "Annotations & Alerts",
        "target": {
          "limit": 100,
          "matchAny": false,
          "tags": [],
          "type": "dashboard"
        },
        "type": "dashboard"
      }
    ]
  },
  "editable": true,
  "fiscalYearStartMonth": 0,
  "graphTooltip": 0,
  "id": 1,
  "iteration": 1654851107566,
  "links": [],
  "liveNow": false,
  "panels": [
    {
      "datasource": {
        "type": "influxdb",
        "uid": "P951FEA4DE68E13C5"
      },
      "description": "",
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisLabel": "Response",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 0,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "lineInterpolation": "linear",
            "lineStyle": {
              "fill": "solid"
            },
            "lineWidth": 1,
            "pointSize": 6,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "auto",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "red",
                "value": 5000
              }
            ]
          }
        },
        "overrides": [
          {
            "__systemRef": "hideSeriesFrom",
            "matcher": {
              "id": "byNames",
              "options": {
                "mode": "exclude",
                "names": [
                  "v {channel=\"BHZ\", station=\"00\"}"
                ],
                "prefix": "All except:",
                "readOnly": true
              }
            },
            "properties": [
              {
                "id": "custom.hideFrom",
                "value": {
                  "legend": true,
                  "tooltip": false,
                  "viz": false
                }
              }
            ]
          }
        ]
      },
      "gridPos": {
        "h": 20,
        "w": 24,
        "x": 0,
        "y": 0
      },
      "id": 2,
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom"
        },
        "tooltip": {
          "mode": "multi",
          "sort": "asc"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "influxdb",
            "uid": "P951FEA4DE68E13C5"
          },
          "query": "from(bucket: \"mini_seed\")\r\n  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)\r\n  |> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\")\r\n  |> filter(fn: (r) => r[\"_field\"] == \"channel\" or r[\"_field\"] == \"network\" or r[\"_field\"] == \"new_channel\" or r[\"_field\"] == \"time_stamp\" or r[\"_field\"] == \"v\")\r\n  |> filter(fn:(r) => r.channel=~ /${channels}/)\r\n  |> filter(fn:(r) => r.station=~ /${station}/)\r\n",
          "refId": "A"
        }
      ],
      "title": "监测数据",
      "type": "timeseries"
    }
  ],
  "refresh": "5s",
  "schemaVersion": 36,
  "style": "dark",
  "tags": [],
  "templating": {
    "list": [
      {
        "current": {
          "isNone": true,
          "selected": false,
          "text": "None",
          "value": ""
        },
        "datasource": {
          "type": "influxdb",
          "uid": "P951FEA4DE68E13C5"
        },
        "definition": "from(bucket: \"mini_seed\")\r\n|> range(start: v.timeRangeStart, stop: v.timeRangeStop)\r\n|> keyValues(keyColumns:[\"ip\"])\r\n|> group()\r\n|> keep(columns:[\"ip\"])\r\n|> distinct(column:\"ip\")\r\n",
        "hide": 0,
        "includeAll": false,
        "label": "ip",
        "multi": false,
        "name": "ip",
        "options": [],
        "query": "from(bucket: \"mini_seed\")\r\n|> range(start: v.timeRangeStart, stop: v.timeRangeStop)\r\n|> keyValues(keyColumns:[\"ip\"])\r\n|> group()\r\n|> keep(columns:[\"ip\"])\r\n|> distinct(column:\"ip\")\r\n",
        "refresh": 1,
        "regex": "",
        "skipUrlSync": false,
        "sort": 1,
        "type": "query"
      },
      {
        "current": {
          "isNone": true,
          "selected": false,
          "text": "None",
          "value": ""
        },
        "datasource": {
          "type": "influxdb",
          "uid": "P951FEA4DE68E13C5"
        },
        "definition": "from(bucket: \"mini_seed\")\r\n|> range(start: v.timeRangeStart, stop: v.timeRangeStop)\r\n|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\")\r\n|> filter(fn: (r) => r[\"ip\"] =~ /^${ip}$/)\r\n|> keyValues(keyColumns:[\"channel\"])\r\n|> group()\r\n|> keep(columns:[\"channel\"])\r\n|> distinct(column:\"channel\")\r\n",
        "hide": 0,
        "includeAll": false,
        "label": "channel",
        "multi": false,
        "name": "channels",
        "options": [],
        "query": "from(bucket: \"mini_seed\")\r\n|> range(start: v.timeRangeStart, stop: v.timeRangeStop)\r\n|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\")\r\n|> filter(fn: (r) => r[\"ip\"] =~ /^${ip}$/)\r\n|> keyValues(keyColumns:[\"channel\"])\r\n|> group()\r\n|> keep(columns:[\"channel\"])\r\n|> distinct(column:\"channel\")\r\n",
        "refresh": 1,
        "regex": "",
        "skipUrlSync": false,
        "sort": 1,
        "type": "query"
      },
      {
        "current": {
          "isNone": true,
          "selected": false,
          "text": "None",
          "value": ""
        },
        "datasource": {
          "type": "influxdb",
          "uid": "P951FEA4DE68E13C5"
        },
        "definition": "from(bucket: \"mini_seed\")\r\n|> range(start: v.timeRangeStart, stop: v.timeRangeStop)\r\n|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\")\r\n|> filter(fn: (r) => r[\"ip\"] =~ /^${ip}$/)\r\n|> filter(fn: (r) => r[\"channel\"] =~ /^${channels}$/)\r\n|> keyValues(keyColumns:[\"station\"])\r\n|> group()\r\n|> keep(columns:[\"station\"])\r\n|> distinct(column:\"station\")\r\n\r\n",
        "hide": 0,
        "includeAll": false,
        "label": "station",
        "multi": false,
        "name": "station",
        "options": [],
        "query": "from(bucket: \"mini_seed\")\r\n|> range(start: v.timeRangeStart, stop: v.timeRangeStop)\r\n|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\")\r\n|> filter(fn: (r) => r[\"ip\"] =~ /^${ip}$/)\r\n|> filter(fn: (r) => r[\"channel\"] =~ /^${channels}$/)\r\n|> keyValues(keyColumns:[\"station\"])\r\n|> group()\r\n|> keep(columns:[\"station\"])\r\n|> distinct(column:\"station\")\r\n\r\n",
        "refresh": 1,
        "regex": "",
        "skipUrlSync": false,
        "sort": 3,
        "type": "query"
      }
    ]
  },
  "time": {
    "from": "now-15m",
    "to": "now"
  },
  "timepicker": {},
  "timezone": "",
  "title": "监测数据",
  "uid": "57pHkZwnz",
  "version": 27,
  "weekStart": ""
}


```