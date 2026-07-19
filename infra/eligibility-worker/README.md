# Eligibility Worker

This Cloudflare Worker is a small deterministic substitute for the external
associate eligibility service.

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

The result is deterministic: a CPF whose last digit is even is able to vote,
and a CPF whose last digit is odd is unable to vote. The Worker validates both
CPF check digits and rejects formatted, repeated-digit, or otherwise invalid
values with `404 Not Found`.

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
