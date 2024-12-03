# Wiremock

## hentIdenter
```json
{
  "variables": {
    "ident": "01017012345",
    "historisk": true
  },
  "query": "query($ident: ID!, $historisk: Boolean) {\n    hentIdenter(ident: $ident, historikk: $historisk) {\n        identer {\n            ident\n            gruppe\n            historisk\n        }\n    }\n}"
}
```
