import assert from "node:assert/strict";
import { mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { execFile } from "node:child_process";
import test from "node:test";
import { promisify } from "node:util";
import { fileURLToPath } from "node:url";

import {
  analyzeMigrations,
  formatMarkdownReport,
} from "./analyze-flyway-squash-groups.mjs";

const execFileAsync = promisify(execFile);
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const scriptPath = path.join(__dirname, "analyze-flyway-squash-groups.mjs");

async function withMigrationDir(files, testFn) {
  const root = await mkdtemp(path.join(tmpdir(), "healthlens-flyway-"));
  const migrationDir = path.join(root, "db", "migration");
  await mkdir(migrationDir, { recursive: true });

  for (const [name, content] of Object.entries(files)) {
    await writeFile(path.join(migrationDir, name), content, "utf8");
  }

  try {
    await testFn(migrationDir);
  } finally {
    await rm(root, { recursive: true, force: true });
  }
}

test("analyzeMigrations lists Flyway migrations in numeric version order", async () => {
  await withMigrationDir(
    {
      "V010__create_profiles_table.sql": "create table profiles (id uuid);",
      "V002__create_email_verification_tokens_table.sql":
        "create table email_verification_tokens (id uuid);",
      "V001__create_users_table.sql": "create table users (id uuid);",
    },
    async (migrationDir) => {
      const report = await analyzeMigrations({ migrationDir });

      assert.deepEqual(
        report.migrations.map((migration) => migration.version),
        ["001", "002", "010"],
      );
      assert.deepEqual(
        report.migrations.map((migration) => migration.fileName),
        [
          "V001__create_users_table.sql",
          "V002__create_email_verification_tokens_table.sql",
          "V010__create_profiles_table.sql",
        ],
      );
    },
  );
});

test("analyzeMigrations proposes multiple logical baseline groups", async () => {
  await withMigrationDir(
    {
      "V001__create_users_table.sql": "create table users (id uuid);",
      "V012__create_health_records_table.sql":
        "create table health_records (ocr_text text);",
      "V013__create_reference_data_tables.sql":
        "create table reference_ranges (id uuid); create table rag_corpus_versions (id uuid);",
      "V034__create_audit_logs_table.sql":
        "create table audit_logs (id uuid); create table notifications (id uuid);",
      "V049__personal_and_profile_health_context.sql":
        "alter table profiles add column allergies text;",
    },
    async (migrationDir) => {
      const report = await analyzeMigrations({ migrationDir });
      const domains = report.groups.map((group) => group.domain);

      assert.deepEqual(domains, [
        "identity/auth",
        "profile/privacy",
        "health-record/OCR",
        "reference/AI/RAG",
        "audit/activity/notification",
      ]);
      assert.equal(report.groups.length, 5);
      assert.ok(
        report.groups.every((group) => group.migrations.length >= 1),
        "each suggested baseline group should carry migrations",
      );
    },
  );
});

test("analyzeMigrations favors specific profile signals over generic users references", async () => {
  await withMigrationDir(
    {
      "V008__add_profile_fields_to_users.sql":
        [
          "alter table users add column full_name varchar(255);",
          "alter table users add column gender varchar(32);",
          "alter table users add column date_of_birth date;",
        ].join("\n"),
      "V043__create_user_activity_events.sql":
        "create table user_activity_events (id uuid, user_id uuid);",
      "V047__extend_user_activity_events_product_dimensions.sql":
        "alter table user_activity_events add column feature_area varchar(64);",
    },
    async (migrationDir) => {
      const report = await analyzeMigrations({ migrationDir });
      const domainsByFile = Object.fromEntries(
        report.migrations.map((migration) => [
          migration.fileName,
          migration.domain,
        ]),
      );

      assert.equal(
        domainsByFile["V008__add_profile_fields_to_users.sql"],
        "profile/privacy",
      );
      assert.equal(
        domainsByFile["V043__create_user_activity_events.sql"],
        "audit/activity/notification",
      );
      assert.equal(
        domainsByFile["V047__extend_user_activity_events_product_dimensions.sql"],
        "audit/activity/notification",
      );
    },
  );
});

test("formatMarkdownReport includes rollout warnings for fresh and existing databases", async () => {
  await withMigrationDir(
    {
      "V001__create_users_table.sql": "create table users (id uuid);",
      "V043__create_user_activity_events.sql":
        "create table user_activity_events (id uuid);",
    },
    async (migrationDir) => {
      const report = await analyzeMigrations({ migrationDir });
      const markdown = formatMarkdownReport(report);

      assert.match(markdown, /## Rollout Warnings/);
      assert.match(markdown, /fresh DB/i);
      assert.match(markdown, /existing migrated DB/i);
      assert.match(markdown, /analysis-only/i);
      assert.doesNotMatch(markdown, /single baseline file/i);
    },
  );
});

test("CLI supports markdown output", async () => {
  await withMigrationDir(
    {
      "V001__create_users_table.sql": "create table users (id uuid);",
      "V013__create_reference_data_tables.sql":
        "create table reference_ranges (id uuid);",
    },
    async (migrationDir) => {
      const { stdout } = await execFileAsync("node", [
        scriptPath,
        "--migration-dir",
        migrationDir,
        "--format",
        "markdown",
      ]);

      assert.match(stdout, /^# Flyway Migration Squash Group Analysis/m);
      assert.match(stdout, /V001__create_users_table\.sql/);
      assert.match(stdout, /identity\/auth/);
      assert.match(stdout, /reference\/AI\/RAG/);
    },
  );
});
