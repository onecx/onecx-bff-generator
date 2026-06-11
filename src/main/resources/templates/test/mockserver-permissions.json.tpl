[
  {
    "id": "2",
    "httpRequest": {
      "headers": {
        "apm-principal-token": [ "alice" ]
      },
      "path": "/v1/permissions/user/applications/${artifactId}"
    },
    "httpResponse": {
      "body": {
        "type": "JSON",
        "json": {
          "appId": "${artifactId}",
          "permissions": {
            ${alicePermissions},
            "permissions": ["admin-write", "admin-read"]
          }
        },
        "contentType": "application/json"
      }
    }
  },
  {
    "id": "3",
    "httpRequest": {
      "headers": {
        "apm-principal-token": [ "bob" ]
      },
      "path": "/v1/permissions/user/applications/${artifactId}"
    },
    "httpResponse": {
      "body": {
        "type": "JSON",
        "json": {
          "appId": "${artifactId}",
          "permissions": {
            ${bobPermissions},
            "permissions": ["admin-write", "admin-read"]
          }
        },
        "contentType": "application/json"
      }
    }
  }
]
