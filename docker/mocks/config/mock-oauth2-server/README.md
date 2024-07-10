# Mock OAuth2 Server

## Hente token

### Hente token vha cURL
```shell
curl -H "Content-Type: application/x-www-form-urlencoded" -F "grant_type=client_credentials" -v "http://localhost:8081/default/token"
```