# OpenAPI Contract Generation

This script regenerates shared TypeScript OpenAPI contracts in:

```text
packages/shared/generated/openapi/
```

The generated folder is owned by `packages/shared` and must not contain hand-written application logic.

## Prerequisites

- Node and pnpm from the root `package.json`.
- Java available on `PATH`; `@openapitools/openapi-generator-cli` runs the pinned generator jar from `openapitools.json`.
- One OpenAPI source:
  - local API at `http://localhost:8080/v3/api-docs`, or
  - a readable OpenAPI JSON file passed with `--input`.

## Commands

Generate from the default local API:

```bash
pnpm openapi:generate
```

Generate from a captured OpenAPI JSON file:

```bash
pnpm openapi:generate --input ./tmp/openapi.json
```

Equivalent environment override:

```bash
OPENAPI_INPUT=./tmp/openapi.json pnpm openapi:generate
```

## Failure Behavior

The command fails before generation when:

- the local API URL is unreachable or returns a non-2xx response,
- the OpenAPI JSON file is not readable,
- the generator config is missing,
- Java or the OpenAPI generator process exits non-zero.

Before each non-dry-run generation, the script removes `packages/shared/generated/openapi/` and recreates it. This keeps deleted schemas from lingering and makes reruns deterministic except for explainable upstream changes in the OpenAPI source or generator version.

The generator is invoked with `--global-property models,supportingFiles=index.ts,modelDocs=false,modelTests=false` and `withoutRuntimeChecks: true`, so R2.2 produces DTO/model contracts only. The script overwrites the root `index.ts` with a deterministic barrel that exports `./models/index`; it does not generate a runtime HTTP client or migrate existing frontend API calls.
