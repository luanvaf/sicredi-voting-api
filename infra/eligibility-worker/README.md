# Eligibility Worker

This Cloudflare Worker is a small deterministic substitute for the external
associate eligibility service, whose URL in the original challenge is no
longer available.

The challenge defines both possible responses for a valid CPF but does not
provide a business rule for deciding between them. This substitute therefore
uses the challenge's own examples as deterministic test fixtures:

| CPF | Response |
| --- | --- |
| `19839091069` | `ABLE_TO_VOTE` |
| `62289608068` | `UNABLE_TO_VOTE` |
| Any other valid CPF | `ABLE_TO_VOTE` |

This mapping exists only to keep both integration outcomes reproducible. It is
not intended to model a real associate eligibility rule.

## Contract

Send an unformatted CPF to:

```http
GET /users/{cpf}
```

A valid CPF receives `200 OK` and one of these responses:

```json
{ "status": "ABLE_TO_VOTE" }
```

```json
{ "status": "UNABLE_TO_VOTE" }
```

The Worker validates both CPF check digits and rejects formatted,
repeated-digit, or otherwise invalid values with `404 Not Found`.

Unsupported paths also return `404 Not Found`. Methods other than `GET` return
`405 Method Not Allowed`.

## Run locally

From this directory, run:

```shell
npx wrangler dev
```

Then call the local URL printed by Wrangler:

```shell
curl http://localhost:8787/users/19839091069
```

## Deploy

Authenticate Wrangler with your Cloudflare account and run:

```shell
npx wrangler deploy
```
