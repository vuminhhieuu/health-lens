import assert from "node:assert/strict";
import test from "node:test";

import { buildGeneratorArgs, isHttpUrl, parseArgs, runGenerator } from "./generate.mjs";

test("parseArgs uses documented defaults", () => {
  assert.deepEqual(parseArgs([], {}), {
    input: "http://localhost:8080/v3/api-docs",
    output: "packages/shared/generated/openapi",
    config: "scripts/openapi/openapi-generator.config.yaml",
    dryRun: false,
    skipClean: false,
    help: false,
  });
});

test("parseArgs accepts file input and dry-run mode", () => {
  assert.deepEqual(parseArgs(["--input", "./tmp/openapi.json", "--dry-run"], {}), {
    input: "./tmp/openapi.json",
    output: "packages/shared/generated/openapi",
    config: "scripts/openapi/openapi-generator.config.yaml",
    dryRun: true,
    skipClean: false,
    help: false,
  });
});

test("parseArgs rejects unknown flags", () => {
  assert.throws(() => parseArgs(["--unknown"], {}), /Unknown option: --unknown/);
});

test("buildGeneratorArgs keeps generated output model-only", () => {
  const args = buildGeneratorArgs(parseArgs(["--input=./openapi.json"], {}));

  assert.equal(args[0], "exec");
  assert.equal(args[1], "openapi-generator-cli");
  assert.match(args.join(" "), /--global-property models,supportingFiles=index\.ts,modelDocs=false,modelTests=false/);
  assert.match(args.join(" "), /--minimal-update/);
});

test("isHttpUrl only accepts http and https inputs", () => {
  assert.equal(isHttpUrl("http://localhost:8080/v3/api-docs"), true);
  assert.equal(isHttpUrl("https://example.test/openapi.json"), true);
  assert.equal(isHttpUrl("./openapi.json"), false);
});

test("runGenerator rejects output outside the shared generated OpenAPI folder", async () => {
  await assert.rejects(
    () => runGenerator(parseArgs(["--input", "./package.json", "--output", "/tmp/openapi-output"], {})),
    /OpenAPI output must remain packages\/shared\/generated\/openapi/,
  );
});
