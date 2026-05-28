#!/usr/bin/env node

import { readdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const DEFAULT_MIGRATION_DIR = "apps/api/src/main/resources/db/migration";

const DOMAIN_RULES = [
  {
    domain: "identity/auth",
    signals: [
      "user",
      "users",
      "auth",
      "email_verification",
      "password_reset",
      "refresh_token",
      "totp",
      "admin_totp",
      "account_status",
      "deletion_token",
      "session_family",
    ],
  },
  {
    domain: "profile/privacy",
    signals: [
      "profile",
      "profiles",
      "consent",
      "privacy",
      "data_deletion",
      "deletion_request",
      "avatar",
      "sharing",
      "invitation",
      "allergies",
      "medical_history",
    ],
  },
  {
    domain: "health-record/OCR",
    signals: [
      "health_record",
      "health_records",
      "ocr",
      "upload",
      "pdf",
      "download",
      "failure_reason",
      "dead_letter",
      "dlq",
    ],
  },
  {
    domain: "reference/AI/RAG",
    signals: [
      "reference",
      "reference_range",
      "metric",
      "rag",
      "corpus",
      "citation",
      "online_rag",
      "llm",
      "approval_workflow",
      "change_set",
    ],
  },
  {
    domain: "audit/activity/notification",
    signals: [
      "audit",
      "activity",
      "notification",
      "notifications",
      "inbox",
      "read_state",
      "event",
      "events",
      "reminder",
      "follow_up",
      "correlation",
    ],
  },
];

function parseVersion(fileName) {
  const match = fileName.match(/^V([0-9][0-9._-]*)__(.+)\.sql$/i);
  if (!match) {
    return null;
  }

  return {
    raw: match[1],
    description: match[2],
    sortable: match[1]
      .split(/[._-]/)
      .map((part) => Number.parseInt(part, 10))
      .filter((part) => Number.isFinite(part)),
  };
}

function compareVersions(left, right) {
  const maxLength = Math.max(left.sortable.length, right.sortable.length);
  for (let index = 0; index < maxLength; index += 1) {
    const leftPart = left.sortable[index] ?? 0;
    const rightPart = right.sortable[index] ?? 0;
    if (leftPart !== rightPart) {
      return leftPart - rightPart;
    }
  }

  return left.fileName.localeCompare(right.fileName);
}

function normalizeSignalText(value) {
  return value.toLowerCase().replace(/[^a-z0-9_]+/g, "_");
}

function signalScore(value, signals) {
  return signals.reduce((total, signal) => {
    const signalPattern = new RegExp(`(?:^|_)${signal}(?:_|$)`, "g");
    const matches = value.match(signalPattern) ?? [];
    return total + matches.length * signal.replaceAll("_", "").length;
  }, 0);
}

function classifyMigration(fileName, sql) {
  const nameHaystack = normalizeSignalText(fileName);
  const sqlHaystack = normalizeSignalText(sql);
  const scores = DOMAIN_RULES.map((rule) => ({
    domain: rule.domain,
    nameScore: signalScore(nameHaystack, rule.signals),
    sqlScore: signalScore(sqlHaystack, rule.signals),
  })).map((result) => ({
    ...result,
    score:
      result.nameScore > 0
        ? result.nameScore * 100 + result.sqlScore
        : result.sqlScore,
  })).filter((result) => result.score > 0);

  if (scores.length === 0) {
    return "uncategorized";
  }

  scores.sort((left, right) => right.score - left.score);
  return scores[0].domain;
}

function summarizeSql(sql) {
  const tableMatches = [...sql.matchAll(/\b(?:create|alter)\s+table\s+(?:if\s+not\s+exists\s+)?([a-zA-Z0-9_".]+)/gi)];
  const tables = [
    ...new Set(
      tableMatches
        .map((match) => match[1].replaceAll('"', "").split(".").pop())
        .filter(Boolean),
    ),
  ];

  return {
    tableHints: tables.slice(0, 8),
    statementCount: sql
      .split(";")
      .map((statement) => statement.trim())
      .filter(Boolean).length,
  };
}

export async function analyzeMigrations({
  migrationDir = DEFAULT_MIGRATION_DIR,
} = {}) {
  const absoluteMigrationDir = path.resolve(process.cwd(), migrationDir);
  const entries = await readdir(absoluteMigrationDir, { withFileTypes: true });
  const migrations = [];

  for (const entry of entries) {
    if (!entry.isFile()) {
      continue;
    }

    const parsed = parseVersion(entry.name);
    if (!parsed) {
      continue;
    }

    const absolutePath = path.join(absoluteMigrationDir, entry.name);
    const sql = await readFile(absolutePath, "utf8");
    const summary = summarizeSql(sql);

    migrations.push({
      version: parsed.raw,
      description: parsed.description.replaceAll("_", " "),
      fileName: entry.name,
      relativePath: path.relative(process.cwd(), absolutePath),
      domain: classifyMigration(entry.name, sql),
      tableHints: summary.tableHints,
      statementCount: summary.statementCount,
      sortable: parsed.sortable,
    });
  }

  migrations.sort(compareVersions);

  const publicMigrations = migrations.map(({ sortable, ...migration }) => migration);
  const groups = [];
  for (const rule of DOMAIN_RULES) {
    const domainMigrations = publicMigrations.filter(
      (migration) => migration.domain === rule.domain,
    );
    if (domainMigrations.length > 0) {
      groups.push({
        domain: rule.domain,
        suggestedBaselineName: `baseline-${rule.domain.replaceAll("/", "-")}.sql`,
        migrations: domainMigrations,
      });
    }
  }

  const uncategorized = publicMigrations.filter(
    (migration) => migration.domain === "uncategorized",
  );
  if (uncategorized.length > 0) {
    groups.push({
      domain: "uncategorized",
      suggestedBaselineName: "baseline-uncategorized-review-required.sql",
      migrations: uncategorized,
    });
  }

  return {
    migrationDir: path.relative(process.cwd(), absoluteMigrationDir),
    migrationCount: migrations.length,
    migrations: publicMigrations,
    groups,
    warnings: [
      "Analysis-only report: do not edit, delete, or replace active Flyway migration files from this script output.",
      "Validate any future squash plan against a fresh DB that starts from an empty schema and runs the proposed baseline groups.",
      "Validate any future squash plan against an existing migrated DB with the current flyway_schema_history already applied.",
      "Do not enable or change baseline-on-migrate as part of this analysis; handle rollout configuration in a separate reviewed change.",
    ],
  };
}

function formatMigrationLine(migration) {
  const tableSuffix =
    migration.tableHints.length > 0
      ? `; tables: ${migration.tableHints.join(", ")}`
      : "";
  return `- ${migration.version} - ${migration.fileName} (${migration.domain}${tableSuffix})`;
}

export function formatTextReport(report) {
  const lines = [
    "Flyway Migration Squash Group Analysis",
    `Migration directory: ${report.migrationDir}`,
    `Migrations found: ${report.migrationCount}`,
    "",
    "Version order:",
    ...report.migrations.map(formatMigrationLine),
    "",
    "Suggested baseline groups:",
  ];

  for (const group of report.groups) {
    lines.push(
      `- ${group.domain}: ${group.migrations.length} migrations -> ${group.suggestedBaselineName}`,
    );
  }

  lines.push("", "Rollout warnings:", ...report.warnings.map((warning) => `- ${warning}`));

  return `${lines.join("\n")}\n`;
}

export function formatMarkdownReport(report) {
  const lines = [
    "# Flyway Migration Squash Group Analysis",
    "",
    `- Migration directory: \`${report.migrationDir}\``,
    `- Migrations found: ${report.migrationCount}`,
    "",
    "## Version Order",
    "",
    "| Version | File | Domain | Table Hints |",
    "| --- | --- | --- | --- |",
    ...report.migrations.map(
      (migration) =>
        `| ${migration.version} | \`${migration.fileName}\` | ${migration.domain} | ${
          migration.tableHints.join(", ") || "-"
        } |`,
    ),
    "",
    "## Suggested Baseline Groups",
    "",
    "The suggested squash shape is multiple logical baselines by domain. Treat uncategorized migrations as manual review items before any future squash.",
    "",
  ];

  for (const group of report.groups) {
    lines.push(
      `### ${group.domain}`,
      "",
      `Suggested file: \`${group.suggestedBaselineName}\``,
      "",
      ...group.migrations.map(
        (migration) => `- \`${migration.fileName}\` - ${migration.description}`,
      ),
      "",
    );
  }

  lines.push(
    "## Rollout Warnings",
    "",
    ...report.warnings.map((warning) => `- ${warning}`),
    "",
  );

  return `${lines.join("\n")}\n`;
}

function parseArgs(argv) {
  const options = {
    migrationDir: DEFAULT_MIGRATION_DIR,
    format: "text",
    output: null,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--migration-dir") {
      options.migrationDir = argv[++index];
    } else if (arg === "--format") {
      options.format = argv[++index];
    } else if (arg === "--output") {
      options.output = argv[++index];
    } else if (arg === "--help" || arg === "-h") {
      options.help = true;
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  if (!["text", "markdown", "json"].includes(options.format)) {
    throw new Error("--format must be one of: text, markdown, json");
  }

  return options;
}

function usage() {
  return `Usage: node scripts/db/analyze-flyway-squash-groups.mjs [options]

Options:
  --migration-dir <path>  Flyway migration directory
                          default: ${DEFAULT_MIGRATION_DIR}
  --format <format>      text, markdown, or json
                          default: text
  --output <path>        Optional report output path
  -h, --help             Show this help
`;
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    process.stdout.write(usage());
    return;
  }

  const report = await analyzeMigrations({ migrationDir: options.migrationDir });
  const formatted =
    options.format === "markdown"
      ? formatMarkdownReport(report)
      : options.format === "json"
        ? `${JSON.stringify(report, null, 2)}\n`
        : formatTextReport(report);

  if (options.output) {
    await writeFile(path.resolve(process.cwd(), options.output), formatted, "utf8");
  } else {
    process.stdout.write(formatted);
  }
}

const isCli = process.argv[1] === fileURLToPath(import.meta.url);

if (isCli) {
  main().catch((error) => {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  });
}
