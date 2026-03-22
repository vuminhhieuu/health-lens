// ============================================================
// content/data.js — Toàn bộ nội dung có thể chỉnh sửa
// ============================================================

// ── STACK INFO ────────────────────────────────────────────────
const STACK_TOOLS = [
  { icon:"📋", name:"Linear",         role:"Issues, Cycles, Sprint",  badge:"Free",              badgeClass:"green" },
  { icon:"🐙", name:"GitHub",         role:"Code, PR, CI/CD",         badge:"Free personal",     badgeClass:"green" },
  { icon:"💬", name:"Slack",          role:"Thông báo, channels",     badge:"Free 90 ngày",      badgeClass:"amber" },
  { icon:"🔔", name:"PullNotifier",   role:"PR → Slack",              badge:"Free mãi",          badgeClass:"green" },
  { icon:"🤖", name:"Geekbot",        role:"Daily standup",           badge:"Free ≤10 người",    badgeClass:"green" },
  { icon:"⚙️", name:"Apps Script",    role:"Automation, webhooks",    badge:"Free unlimited",    badgeClass:"green" },
  { icon:"📊", name:"Google Sheets",  role:"Data, dashboard",         badge:"Free",              badgeClass:"green" },
];

const DESIGN_DECISIONS = [
  { title:"GitHub Personal Account (không phải Org)", desc:"GitHub Free Org không có required reviewers trên private repo. Personal account có → phải dùng personal làm owner." },
  { title:"Apps Script thay Make.com", desc:"Make.com free giới hạn 1000 ops/tháng, hay expired connection. Apps Script free unlimited, ghi Sheets native." },
  { title:"PullNotifier thay Axolo", desc:"Axolo chỉ hỗ trợ GitHub Organization (trả phí). PullNotifier free forever, unlimited PRs, 1 message/PR tự update." },
  { title:"Geekbot xử lý OOO nội bộ", desc:"Geekbot API bị khóa free tier từ 2025 → không dùng webhook OOO. OOO xử lý hoàn toàn trong Slack, track qua standup_log." },
  { title:"Tại sao không dùng GitLab", desc:"GitLab Free chỉ soft enforcement cho required approval. GitHub Free personal hard-block merge nếu thiếu approval." },
  { title:"Bỏ Looker Studio / Screenful", desc:"Looker Studio setup phức tạp. Screenful hết free sau 14 ngày. Dùng Google Sheets + HTML Web App — data đã có, free forever." },
];

// ── TOOLS DATA (Tab Công cụ) ──────────────────────────────────
const TOOLS_DATA = [
  {
    id: "linear",
    name: "Linear",
    icon: "📋",
    tagline: "Issue tracker hiện đại, nhanh, Git-native",
    what: "Linear là công cụ quản lý issues và sprint (Cycles) cho team phần mềm. Được thiết kế để nhanh — mọi thao tác đều có keyboard shortcut, không cần chuột.",
    role: "Trung tâm quản lý công việc của team. Mỗi task là 1 issue trong Linear. Sprint = 1 Cycle. Linear tự động update trạng thái issue khi bạn tạo branch hoặc merge PR trên GitHub.",
    how: [
      "Tạo issue: nhấn <kbd>C</kbd> ở bất kỳ đâu",
      "Copy branch name: <kbd>Ctrl+Shift+.</kbd> — issue tự chuyển sang In Progress",
      "Filter issues: <kbd>F</kbd>",
      "Điều hướng: <kbd>G+I</kbd> → My Issues, <kbd>G+C</kbd> → Cycles",
      "Add vào Cycle: mở issue → Cycle → chọn sprint hiện tại",
      "Set priority: P → chọn level (Urgent/High/Medium/Low)",
    ],
    automation: "Khi bạn tạo branch với tên chứa issue ID (ví dụ feat/LIN-42-...), Linear tự chuyển issue → In Progress. Khi tạo PR có 'Fixes LIN-42', issue → In Review. Khi merge PR, issue → Done.",
    similar: [
      { name: "Jira", note: "Phổ biến nhất enterprise, phức tạp hơn, trả phí từ 8.15$/user/tháng" },
      { name: "GitHub Issues", note: "Tích hợp sẵn GitHub, đơn giản, phù hợp project nhỏ" },
      { name: "Notion", note: "All-in-one, không chuyên biệt cho dev workflow" },
      { name: "Asana", note: "Tốt cho non-technical team, thiếu Git integration" },
    ],
    docs: [
      { label: "Linear Docs", url: "https://linear.app/docs" },
      { label: "Git Automation", url: "https://linear.app/docs/github" },
      { label: "Keyboard Shortcuts", url: "https://linear.app/docs/keyboard-shortcuts" },
    ],
    tips: [
      "Luôn add issue vào Cycle trước khi bắt đầu làm — issue không có trong Cycle không hiện trong sprint report",
      "Estimate story points (T-shirt size hoặc số) trong sprint planning để track velocity",
      "Dùng label 'bug' chính xác (lowercase) — Apps Script filter theo label này để ghi bug_log Sheets tự động",
      "Bật email notification: Settings → Account → Notifications → Due date approaching",
    ],
  },
  {
    id: "github",
    name: "GitHub",
    icon: "🐙",
    tagline: "Git hosting + collaboration + CI/CD",
    what: "GitHub là nền tảng host code, quản lý Pull Request, và chạy CI/CD (GitHub Actions). Team dùng GitHub Free với personal account — không tạo Organization.",
    role: "Nơi lưu trữ và review code. Mọi thay đổi code đều qua Pull Request. Branch protection đảm bảo không ai push thẳng vào main và mọi PR đều cần ít nhất 1 approval + CI pass.",
    how: [
      "Tạo branch: <code>git checkout -b feat/LIN-42-ten-tinh-nang</code>",
      "Push: <code>git push origin feat/LIN-42-ten-tinh-nang</code>",
      "Tạo PR trên GitHub UI — điền description theo template, assign reviewer",
      "Review: vào tab Files changed → comment inline vào đúng dòng",
      "Merge: Squash and merge — giữ history gọn",
    ],
    automation: "Khi PR được tạo, GitHub gửi webhook đến Apps Script → DM reviewer. Khi PR được merge, Apps Script ghi data vào mr_stats tab trong Google Sheets (author, reviewer, review_time_hours).",
    similar: [
      { name: "GitLab", note: "Self-hosted option, CI/CD mạnh hơn, nhưng required reviewers cần paid plan" },
      { name: "Bitbucket", note: "Tích hợp tốt với Jira/Atlassian ecosystem" },
      { name: "Azure DevOps", note: "Enterprise Microsoft stack" },
    ],
    docs: [
      { label: "GitHub Docs", url: "https://docs.github.com" },
      { label: "Branch Protection", url: "https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches" },
      { label: "GitHub Actions", url: "https://docs.github.com/en/actions" },
      { label: "Auto-close Issues", url: "https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue" },
    ],
    tips: [
      "Dùng personal account làm owner repo — GitHub Free Org không có required reviewers trên private repo",
      "Enable 'Dismiss stale pull request approvals' — commit mới sau approve sẽ cần approve lại",
      "Squash merge thay vì regular merge — giữ history sạch, 1 PR = 1 commit trên main",
      "Branch tự động xóa sau merge: Settings → General → Automatically delete head branches",
    ],
  },
  {
    id: "slack",
    name: "Slack",
    icon: "💬",
    tagline: "Trung tâm thông báo và giao tiếp của team",
    what: "Slack là messaging platform cho team. Trong stack này, Slack là nơi tổng hợp tất cả thông báo từ Linear, GitHub, Geekbot, và Apps Script — không phải nơi để chat việc cá nhân.",
    role: "Aggregator thông báo. Mọi event quan trọng (PR tạo/merge, issue deadline, standup, sprint summary) đều chảy vào đúng channel. Team không cần check nhiều tool — chỉ cần theo dõi Slack.",
    how: [
      "#standup: đọc tổng hợp standup 9:00 mỗi sáng",
      "#dev-mr: theo dõi PR status, review nếu được tag",
      "#dev-tasks: biết ai đang làm gì, deadline nào gần",
      "#sprint: sprint summary cuối cycle, deadline warnings",
      "DM từ Sprint Bot: nhận khi có PR cần review hoặc PR của bạn được merge",
    ],
    automation: "Slack Free giới hạn 10 app slots. Hiện dùng 6: Linear, GitHub native, PullNotifier, Geekbot, Google Calendar, Sprint Bot. Còn 4 slots dự phòng.",
    similar: [
      { name: "Microsoft Teams", note: "Phổ biến trong enterprise Microsoft, tích hợp Office 365" },
      { name: "Discord", note: "Miễn phí không giới hạn, nhưng ít tích hợp dev tools hơn" },
      { name: "Mattermost", note: "Open-source, self-hosted" },
      { name: "Google Chat", note: "Tích hợp Google Workspace" },
    ],
    docs: [
      { label: "Slack Docs", url: "https://slack.com/help" },
      { label: "Slack API Apps", url: "https://api.slack.com/apps" },
      { label: "Notification Settings", url: "https://slack.com/help/articles/201355156-Configure-your-Slack-notifications" },
    ],
    tips: [
      "#standup và #dev-mr: set 'Every new message' — không bỏ lỡ thông báo quan trọng",
      "#dev-tasks và #general: set 'Only mentions' — tránh noise",
      "Dùng /sprint-summary trong #sprint để trigger sprint report thủ công (chỉ team lead)",
      "Slack Free giữ 90 ngày message history — dữ liệu quan trọng được backup vào Google Sheets",
    ],
  },
  {
    id: "geekbot",
    name: "Geekbot",
    icon: "🤖",
    tagline: "Async standup bot — không cần meeting",
    what: "Geekbot là Slack bot tự động hóa daily standup. Thay vì họp mỗi sáng, bot DM từng người 3 câu hỏi lúc 8:00 → tổng hợp tất cả reply → post vào #standup lúc 9:00.",
    role: "Thay thế standup meeting. Team không cần cùng online lúc 8:00 — reply bất kỳ lúc nào trước 9:00. Ai không reply bị tag cảnh báo trong message tổng hợp.",
    how: [
      "8:00 — Geekbot DM bạn 3 câu hỏi tiếng Việt",
      "Reply thẳng vào DM bot — không cần format đặc biệt",
      "OOO: nhắn 'OOO' vào DM bot → chọn ngày → bot skip bạn những ngày đó",
      "Hủy OOO: nhắn 'back' vào DM bot",
      "Admin: Geekbot Dashboard → Send now (để test), Broadcast now (để tổng hợp ngay)",
    ],
    automation: "Khi broadcast, Geekbot gửi webhook đến Apps Script → parse từng reply → ghi vào standup_log tab trong Google Sheets (date, member, yesterday, today, blocker, off_tomorrow).",
    similar: [
      { name: "Standuply", note: "Tương tự Geekbot, có video standup" },
      { name: "Range", note: "Kết hợp standup + goal tracking" },
      { name: "Status Hero", note: "Standup + team metrics" },
      { name: "Manual", note: "Post thủ công vào #standup — không có automation nhưng free hoàn toàn" },
    ],
    docs: [
      { label: "Geekbot Docs", url: "https://geekbot.com/docs/" },
      { label: "Geekbot Dashboard", url: "https://geekbot.com/dashboard/" },
      { label: "OOO Guide", url: "https://geekbot.com/blog/out-of-office/" },
    ],
    tips: [
      "Geekbot free ≤10 người mãi mãi — không cần trả phí dù dùng lâu dài",
      "Geekbot API bị khóa free tier → không có webhook OOO event. OOO tracking chỉ qua Geekbot nội bộ",
      "Câu hỏi optional thứ 4 'Ngày mai bạn có nghỉ không?' giúp team biết trước",
      "Non-responder alert 9:00 → tag người chưa reply trong #standup để nhắc nhở",
    ],
  },
  {
    id: "pullnotifier",
    name: "PullNotifier",
    icon: "🔔",
    tagline: "PR notifications vào Slack — free forever",
    what: "PullNotifier là Slack app kết nối GitHub và Slack để gửi thông báo về Pull Request. 1 message per PR — tự update trạng thái thay vì spam nhiều message.",
    role: "Thay thế Axolo (yêu cầu GitHub Org). Khi có PR mới, PullNotifier post vào #dev-mr. Message tự update khi PR được approve, CI pass/fail, merge. Reviewer nhận DM khi được assign.",
    how: [
      "pullnotifier.com → Sign in with Slack → Sign in with GitHub",
      "Dashboard → Team → User Mapping: map GitHub username → Slack user",
      "Dashboard → Repositories → chọn repo → map vào #dev-mr",
      "Notifications settings: bật PR opened, merged, closed, review requested, approved",
    ],
    automation: "PullNotifier post 1 message per PR vào #dev-mr. Message hiển thị title, author, reviewer, CI status, và tự update khi có thay đổi — không tạo message mới mỗi lần.",
    similar: [
      { name: "Axolo", note: "Tốt nhất cho GitHub Org — tạo thread riêng per PR, nhưng chỉ hỗ trợ Org (trả phí)" },
      { name: "GitHub native Slack app", note: "Built-in, miễn phí, nhưng nhiều noise hơn" },
      { name: "Zapier / Make", note: "Tự build workflow nhưng phức tạp và có giới hạn free" },
    ],
    docs: [
      { label: "PullNotifier Docs", url: "https://www.pullnotifier.com/docs" },
      { label: "Setup Guide", url: "https://www.pullnotifier.com/setup" },
    ],
    tips: [
      "User Mapping phải đúng (case-sensitive GitHub username) — sai thì mentions không hoạt động",
      "Free forever, unlimited PRs — không cần upgrade dù team grow",
      "Khác Axolo: không tạo channel riêng per PR, chỉ post vào 1 channel chung #dev-mr",
    ],
  },
  {
    id: "appsscript",
    name: "Google Apps Script",
    icon: "⚙️",
    tagline: "Automation engine — serverless, free, Google-native",
    what: "Google Apps Script là JavaScript runtime chạy trên Google Cloud, tích hợp native với Google Workspace (Sheets, Drive, Gmail). Deploy được Web App với URL public để nhận webhooks.",
    role: "Automation hub của toàn hệ thống. 1 Web App URL nhận webhook từ Geekbot, GitHub, Linear → xử lý → ghi vào Google Sheets. Cũng xử lý Slack slash command /sprint-summary.",
    how: [
      "Truy cập từ Google Sheets: Extensions → Apps Script",
      "Deploy Web App: Deploy → New deployment → Web app → Execute as Me → Anyone",
      "Update code không đổi URL: Deploy → Manage deployments → Edit → New version",
      "Xem logs: Executions panel (icon đồng hồ) → click execution",
      "Chạy function thủ công: chọn function → Run (để test)",
      "Triggers: setupTriggers() → tạo scheduled trigger mỗi thứ 6 17:00",
    ],
    automation: "4 workflows: W1 (standup→Sheets), W2 (PR merged→mr_stats), W3 (bug issue→bug_log), W4 (/sprint-summary→Slack+velocity). Tổng ≈350 operations/tháng — far below Google's free quota.",
    similar: [
      { name: "Make.com", note: "Visual workflow builder, giới hạn 1000 ops/tháng free" },
      { name: "Zapier", note: "Phổ biến nhất, giới hạn 100 tasks/tháng free" },
      { name: "n8n", note: "Open-source, self-hosted, không giới hạn" },
      { name: "AWS Lambda", note: "Mạnh nhất, cần biết AWS, có thể tốn phí" },
    ],
    docs: [
      { label: "Apps Script Docs", url: "https://developers.google.com/apps-script" },
      { label: "Web Apps Guide", url: "https://developers.google.com/apps-script/guides/web" },
      { label: "Sheets API", url: "https://developers.google.com/apps-script/reference/spreadsheet" },
      { label: "Quotas & Limits", url: "https://developers.google.com/apps-script/guides/services/quotas" },
    ],
    tips: [
      "Luôn dùng 'Edit deployment → New version' thay vì 'New deployment' để giữ URL không đổi",
      "Daily quota: 6h execution time, 20k URL fetch calls — team 4 người dùng ~350 ops/tháng, cách xa giới hạn",
      "Đặt try/catch trong doPost() — tránh webhook timeout làm Geekbot/GitHub retry liên tục",
      "Logger.log() để debug: xem trong Executions panel sau khi webhook trigger",
    ],
  },
  {
    id: "sheets",
    name: "Google Sheets",
    icon: "📊",
    tagline: "Long-term data storage + Dashboard source",
    what: "Google Sheets dùng làm database đơn giản cho team. 5 tabs lưu dữ liệu standup, sprint tasks, PR metrics, bug log, và velocity. Data từ Sheets được đọc để render Dashboard.",
    role: "Thay thế database thật cho team nhỏ. Slack Free chỉ giữ 90 ngày history — Sheets backup mãi mãi. Bất kỳ ai có link đều xem được data mà không cần setup tool.",
    how: [
      "5 tabs: standup_log, sprint_tasks, mr_stats, bug_log, velocity",
      "Row 1: header (frozen, bold, background đen)",
      "Data tự động ghi bởi Apps Script — không cần manual",
      "Dashboard: Extensions → Apps Script → deploy doGet() để render HTML Web App",
      "Filter/sort trong Sheets để phân tích ad-hoc",
    ],
    automation: "Apps Script ghi data vào Sheets sau mỗi event: standup reply → standup_log, PR merge → mr_stats, bug issue create/close → bug_log, /sprint-summary → velocity.",
    similar: [
      { name: "Airtable", note: "UX tốt hơn Sheets, có views đẹp, free 1000 records/base" },
      { name: "Notion Database", note: "Tích hợp với docs, nhưng API chậm hơn" },
      { name: "PostgreSQL / Supabase", note: "Database thật nếu team grow và cần query phức tạp" },
      { name: "BigQuery", note: "Nếu cần analytics-scale, tích hợp native với Sheets" },
    ],
    docs: [
      { label: "Google Sheets", url: "https://support.google.com/docs/topic/9054603" },
      { label: "Sheets API", url: "https://developers.google.com/sheets/api" },
    ],
    tips: [
      "Freeze row 1 (View → Freeze → 1 row) để header luôn hiển thị khi scroll",
      "Version history: File → Version history → See version history — khôi phục nếu ai xóa nhầm",
      "Share với team quyền Editor — ai cũng có thể xem và filter data",
      "Không edit data thủ công vào các tab — để Apps Script ghi tự động để tránh sai format",
    ],
  },
];

// ── CONVENTIONS DATA ──────────────────────────────────────────
const CONVENTIONS = {
  branch: {
    pattern: "prefix/ISSUE-ID-ten-ngan-gon-kebab-case",
    note: "ISSUE-ID là ID thực của issue trong Linear, ví dụ LIN-42, LIN-103. Không viết chữ XX.",
    prefixes: [
      { prefix: "feat/",   when: "Tính năng mới",                  example: "feat/LIN-42-user-login-page" },
      { prefix: "fix/",    when: "Sửa lỗi (bug fix)",               example: "fix/LIN-17-null-pointer-user-service" },
      { prefix: "chore/",  when: "Refactor, cleanup, deps update",  example: "chore/LIN-8-update-axios-deps" },
      { prefix: "docs/",   when: "Tài liệu, comment, README",       example: "docs/LIN-38-update-api-readme" },
      { prefix: "hotfix/", when: "Fix gấp trên production",         example: "hotfix/LIN-99-payment-crash" },
      { prefix: "test/",   when: "Thêm hoặc sửa test",              example: "test/LIN-55-add-auth-unit-tests" },
    ],
    good: [
      "feat/LIN-42-add-user-login-page",
      "fix/LIN-17-fix-null-pointer-user-service",
      "chore/LIN-8-update-axios-1.6.0",
    ],
    bad: [
      "LIN-42-login (thiếu prefix feat/ hay fix/)",
      "feature/add-login (thiếu ISSUE-ID — Linear không nhận diện được)",
      "feat/LIN-XX-login (XX không phải ID thật — Linear không map được issue)",
      "feat/LIN-42-Add-User-Login (camelCase — dùng kebab-case)",
      "my-feature (không có prefix lẫn issue ID)",
    ],
    warning: "Branch name phải có ISSUE-ID thật (ví dụ LIN-42) để Linear Git Automation nhận diện và tự chuyển state sang In Progress.",
    docs: [{ label: "Linear Git Automation docs", url: "https://linear.app/docs/github#git-automation" }],
  },
  commit: {
    pattern: "type(ISSUE-ID): mô tả ngắn gọn bằng tiếng Anh",
    note: "Theo Conventional Commits spec. ISSUE-ID là ID thật của issue (ví dụ LIN-42). Mô tả dùng tiếng Anh lowercase, không kết thúc bằng dấu chấm.",
    types: [
      { type: "feat",     when: "Thêm tính năng mới" },
      { type: "fix",      when: "Sửa bug" },
      { type: "chore",    when: "Refactor, update deps, build tools, cleanup" },
      { type: "docs",     when: "Thay đổi tài liệu, comment code" },
      { type: "test",     when: "Thêm hoặc sửa tests" },
      { type: "style",    when: "Format code, whitespace, không đổi logic" },
      { type: "perf",     when: "Tối ưu performance" },
      { type: "revert",   when: "Revert commit trước" },
      { type: "ci",       when: "Thay đổi CI/CD config" },
    ],
    good: [
      "feat(LIN-42): add user login page with JWT auth",
      "fix(LIN-17): fix null pointer in user service",
      "chore(LIN-8): update axios to 1.6.0",
      "docs(LIN-38): update API endpoint documentation",
    ],
    bad: [
      "fix bug (thiếu type đúng, thiếu ISSUE-ID)",
      "LIN-42: add login (thiếu type)",
      "Add login page (thiếu type và ISSUE-ID)",
      "feat(LIN-42): Add Login Page. (viết hoa, có dấu chấm cuối)",
    ],
    docs: [
      { label: "Conventional Commits spec", url: "https://www.conventionalcommits.org/" },
      { label: "Linear Git Automation", url: "https://linear.app/docs/github#git-automation" },
    ],
  },
  pr: {
    title_pattern: "type(ISSUE-ID): mô tả ngắn gọn",
    magic_words_intro: "GitHub có tính năng 'closing keywords' (magic words) trong PR description. Khi dùng đúng keyword + issue reference, GitHub và Linear tự động đóng/chuyển state issue khi PR được merge vào default branch.",
    magic_words: [
      { word: "Fixes",    note: "Dùng nhiều nhất — fix bug, close issue" },
      { word: "Closes",   note: "Tương tự Fixes — đóng issue" },
      { word: "Resolves", note: "Tương tự Fixes — resolve issue" },
    ],
    magic_words_format: [
      "Fixes LIN-42",
      "Fixes #42 (GitHub issue number)",
      "Closes LIN-42",
      "Resolves LIN-42, Fixes LIN-43 (nhiều issues)",
    ],
    magic_words_warning: "Keyword phải ở dạng 'Fixes ISSUE-ID' (không phải 'Fix', 'Fixed', 'fix'). Phải ở body PR, không phải title. Chỉ trigger khi merge vào default branch (main).",
    template: `## What
[Mô tả ngắn tính năng/fix này làm gì]

## Why
[Tại sao cần thay đổi này]

## How
[Approach chính, quyết định kỹ thuật nếu có]

## Fixes
Fixes LIN-42

## Test
- [ ] Unit tests pass
- [ ] Manual test trên local
- [ ] Không break tính năng hiện có

## Screenshots (nếu UI thay đổi)`,
    rules: [
      "Không ai được merge PR của chính mình",
      "Reviewer phải review và respond trong 24 giờ sau khi được assign",
      "CI phải pass trước khi merge",
      "Tất cả review comments phải được resolve hoặc reply trước khi merge",
      "Dùng Squash merge — 1 PR = 1 clean commit trên main",
      "Branch tự động xóa sau merge (configure trong Settings)",
      "Nếu có conflict với main: author tự rebase, không để reviewer rebase",
    ],
    docs: [
      { label: "GitHub closing keywords", url: "https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword" },
      { label: "Linear + GitHub PR", url: "https://linear.app/docs/github#pull-requests" },
    ],
    warning: "Dòng 'Fixes ISSUE-ID' BẮT BUỘC trong PR description body (không phải title). Thiếu dòng này thì Linear không tự chuyển state sang Done khi merge.",
  },
  labels: [
    { label: "feature",  color: "Xanh dương",  when: "Tính năng mới, enhancement",   automation: "—" },
    { label: "bug",      color: "Đỏ",           when: "Lỗi cần fix",                  automation: "✅ Apps Script ghi bug_log Sheets khi tạo và khi close" },
    { label: "chore",    color: "Xám",           when: "Tech debt, refactor, deps",    automation: "—" },
    { label: "docs",     color: "Xanh lá",       when: "Tài liệu, README, comment",   automation: "—" },
  ],
  standup: {
    q1: { label: "Hôm qua làm gì",
      good: "Hoan thanh LIN-42 user login page, review PR #15 cua Huy",
      bad:  "Lam phan login" },
    q2: { label: "Hôm nay làm gì",
      good: "Bat dau LIN-43 dashboard, tiep tuc LIN-44 neu con thoi gian",
      bad:  "Lam tiep" },
    q3: { label: "Blocker",
      good: "Dang cho Minh review PR #15 tu sang qua, chua co feedback\nKhong (nếu không có blocker)",
      bad:  "co van de (không cụ thể)" },
  },
  code_review: {
    title: "Code Review etiquette",
    items: [
      { rule: "Comment có tính xây dựng", desc: "Chỉ rõ vấn đề + gợi ý cách fix. Không comment kiểu 'cái này sai' mà không giải thích." },
      { rule: "Phân biệt blocking vs non-blocking", desc: "Dùng 'nit:' cho comment nhỏ không blocking (style, naming). Blocking comment = Request changes." },
      { rule: "Approve rõ ràng", desc: "Nếu ổn → bấm Approve, không chỉ comment 'LGTM'. Reviewer phải chủ động bấm Approve để unblock author." },
      { rule: "Không rewrite code của người khác", desc: "Nếu muốn refactor → tạo issue riêng, không sửa thẳng trong PR review." },
      { rule: "Context trước khi review", desc: "Đọc issue Linear và PR description trước khi xem code — hiểu why trước khi đánh giá how." },
    ],
  },
  release: {
    title: "Release & Tag naming",
    pattern: "v{MAJOR}.{MINOR}.{PATCH}",
    examples: ["v1.0.0 — Initial release", "v1.1.0 — New features added", "v1.1.1 — Bug fix release", "v2.0.0 — Breaking changes"],
    note: "Theo Semantic Versioning (semver.org). MAJOR: breaking change, MINOR: new features backward-compatible, PATCH: bug fixes.",
    docs: [{ label: "Semantic Versioning", url: "https://semver.org/" }],
  },
};

// ── CHECKLIST DATA ────────────────────────────────────────────
const CHECKLISTS = {
  lead: {
    title: "Checklist Team Lead — 7 Phases",
    items: [
      { text: "GitHub: tạo personal account (không phải Org)", note: "" },
      { text: "GitHub: tạo repos, visibility Private", note: "" },
      { text: "GitHub: invite members, quyền Write", note: "" },
      { text: "GitHub: branch ruleset Protect main active", note: "Test: push vào main phải bị block" },
      { text: "GitHub: PAT tạo xong, scope repo+read:user", note: "" },
      { text: "GitHub: CI/CD file commit vào tất cả repos", note: "" },
      { text: "Linear: workspace + team tạo xong", note: "" },
      { text: "Linear: 6 workflow states đúng thứ tự", note: "" },
      { text: "Linear: Cycles 2 tuần, bắt đầu thứ Hai", note: "" },
      { text: "Linear: 4 labels (feature bug chore docs)", note: "bug phải lowercase" },
      { text: "Linear: GitHub connected, 4 Git automation triggers", note: "" },
      { text: "Linear: branch format identifier-title", note: "" },
      { text: "Linear: API key tạo và lưu", note: "" },
      { text: "Slack: 5 channels tạo xong, notification đúng", note: "" },
      { text: "Slack: Sprint Bot app, Webhook + Bot Token", note: "" },
      { text: "Slack: PullNotifier connected, user map, repo map", note: "" },
      { text: "Slack: Google Calendar 4 người connect", note: "" },
      { text: "Geekbot: standup tạo xong, 3 câu hỏi tiếng Việt", note: "" },
      { text: "Geekbot: non-responder alert 9:00 → #standup", note: "" },
      { text: "Sheets: 5 tabs đúng tên, header frozen", note: "" },
      { text: "Apps Script: Code.gs paste, CONFIG điền đủ", note: "" },
      { text: "Apps Script: Dashboard.html + DashboardServer.gs", note: "" },
      { text: "Apps Script: Deploy Web App URL đã copy", note: "" },
      { text: "Apps Script: setupTriggers() đã chạy", note: "" },
      { text: "Apps Script: Slash command /sprint-summary setup", note: "" },
      { text: "Webhooks: GitHub + Linear + Geekbot đã paste URL", note: "" },
      { text: "SLACK_USER_MAP: tất cả thành viên đã điền", note: "" },
      { text: "E2E test: tất cả 7 test cases pass", note: "" },
    ]
  },
  member: {
    title: "Checklist Thành viên — Onboarding",
    items: [
      { text: "GitHub: tạo personal account", note: "" },
      { text: "GitHub: accept invite vào repos", note: "Kiểm tra email" },
      { text: "GitHub: config git user.name và user.email", note: "" },
      { text: "GitHub: clone ít nhất 1 repo về máy", note: "" },
      { text: "Đọc tab Conventions: branch, commit, PR, magic words", note: "Bắt buộc trước khi làm" },
      { text: "Linear: accept invite vào workspace", note: "" },
      { text: "Linear: set timezone Asia/Ho_Chi_Minh", note: "Settings → Account" },
      { text: "Linear: bật Move to In Progress khi copy branch", note: "Settings → Account → Preferences" },
      { text: "Linear: bật email notification due date", note: "" },
      { text: "Slack: join workspace, display name là tên thật", note: "" },
      { text: "Slack: set timezone Ho Chi Minh City", note: "" },
      { text: "Slack: join 5 channels, notification level đúng", note: "" },
      { text: "Slack: connect Google Calendar, daily agenda 8:00", note: "" },
      { text: "Slack: connect PullNotifier với GitHub", note: "DM PullNotifier bot" },
      { text: "Gửi Slack Member ID cho team lead", note: "Để điền SLACK_USER_MAP" },
      { text: "Geekbot: nhận DM, thử reply standup", note: "Team lead Send now" },
      { text: "Geekbot: thử set OOO và hủy (nhắn back)", note: "" },
      { text: "Đọc tab Workflow: quy trình task đầu đến cuối", note: "" },
    ]
  },
  sprint_start: {
    title: "Checklist Đầu sprint",
    items: [
      { text: "Tạo Cycle mới trong Linear với ngày cụ thể", note: "" },
      { text: "Cập nhật CURRENT_SPRINT trong Code.gs → deploy", note: "Sprint 1 → Sprint 2" },
      { text: "Sprint planning: kéo issues Backlog → Todo vào Cycle", note: "" },
      { text: "Estimate story points cho issues", note: "" },
      { text: "Thống nhất priority và ai làm gì", note: "" },
    ]
  },
  sprint_end: {
    title: "Checklist Cuối sprint",
    items: [
      { text: "Gõ /sprint-summary trong #sprint", note: "Chỉ team lead" },
      { text: "Review kết quả: completion rate, breakdown", note: "Mở Dashboard" },
      { text: "Retrospective: tốt, cần cải thiện, blockers", note: "" },
      { text: "Carry-over issues → add vào sprint mới", note: "" },
      { text: "Update CURRENT_SPRINT cho sprint tiếp theo", note: "" },
    ]
  },
  new_member: {
    title: "Checklist Thêm thành viên mới giữa chừng",
    items: [
      { text: "GitHub: invite vào tất cả repos → quyền Write", note: "" },
      { text: "Linear: invite vào workspace", note: "" },
      { text: "Slack: invite vào workspace + 5 channels", note: "" },
      { text: "PullNotifier: thêm user mapping GitHub → Slack", note: "Dashboard → Team → User Mapping" },
      { text: "Geekbot: thêm vào participants", note: "Dashboard → Daily Standup → Participants" },
      { text: "Cập nhật SLACK_USER_MAP trong Code.gs → deploy", note: "" },
      { text: "Thành viên hoàn thành Checklist Onboarding", note: "" },
    ]
  },
  new_repo: {
    title: "Checklist Thêm repo mới",
    items: [
      { text: "Tạo repo trên GitHub, visibility Private", note: "" },
      { text: "Invite tất cả members → quyền Write", note: "" },
      { text: "Setup branch protection ruleset Protect main", note: "Copy từ repo cũ" },
      { text: "Thêm .github/workflows/ci.yml", note: "" },
      { text: "GitHub webhook → Apps Script Web App URL", note: "Events: Pull requests" },
      { text: "Thêm tên repo vào GITHUB_REPOS trong Code.gs → deploy", note: "" },
      { text: "PullNotifier: thêm repo → map vào #dev-mr", note: "" },
    ]
  }
};

// ── TROUBLESHOOTING DATA ──────────────────────────────────────
const TROUBLESHOOTING = [
  {
    category: "Linear & GitHub",
    items: [
      { q: "Linear không tự chuyển sang In Progress khi tạo branch",
        cause: "Branch name không có ISSUE-ID, hoặc Git automation chưa bật",
        fix: "1. Branch name phải có issue ID thật: feat/LIN-42-ten (không phải LIN-XX)\n2. Linear → Settings → Team → Workflow → Git automation → bật 'Branch created'\n3. GitHub integration phải connected" },
      { q: "PR tạo xong nhưng Linear không chuyển sang In Review",
        cause: "Thiếu closing keyword trong PR description body",
        fix: "Edit PR description → thêm dòng 'Fixes LIN-42' (số issue thật, không phải XX) → Linear tự chuyển sau vài giây" },
      { q: "Merge PR xong nhưng Linear vẫn In Review",
        cause: "'Fixes LIN-XX' sai số hoặc đặt ở title thay vì body",
        fix: "1. Closing keywords chỉ hoạt động trong PR body, không phải title\n2. Kiểm tra số đúng chưa (LIN-42 không phải LIN-042)\n3. Linear → Settings → Integrations → GitHub → Reconnect" },
      { q: "Push thẳng vào main được — branch protection không hoạt động",
        cause: "Ruleset chưa active hoặc target branch sai",
        fix: "Settings → Branches → Protect main ruleset → Enforcement status = Active. Target = 'main'" },
    ]
  },
  {
    category: "Apps Script & Sheets",
    items: [
      { q: "standup_log không có rows mới sau standup",
        cause: "Geekbot webhook chưa config hoặc URL sai",
        fix: "Geekbot dashboard → Integrations → Webhooks → xóa cũ → paste lại Apps Script Web App URL" },
      { q: "bug_log không ghi khi tạo issue label bug",
        cause: "LINEAR_BUG_LABEL_ID sai",
        fix: "Linear → Settings → Labels → click label 'bug' → copy UUID từ URL → paste vào CONFIG.LINEAR_BUG_LABEL_ID" },
      { q: "review_time_hours âm hoặc bằng 0",
        cause: "Timezone mismatch hoặc PR test tạo + merge quá nhanh",
        fix: "Kiểm tra Apps Script: File → Project settings → Time zone = GMT+7. PR test thì bình thường, PR thật sẽ có số dương" },
      { q: "/sprint-summary không hoạt động",
        cause: "Slash command chưa setup hoặc URL sai",
        fix: "api.slack.com/apps → Sprint Bot → Slash Commands → kiểm tra Request URL → Install lại app" },
      { q: "Deploy Apps Script mới nhưng webhook vẫn dùng code cũ",
        cause: "Tạo New deployment thay vì Edit deployment",
        fix: "Dùng 'Edit deployment → New version' — URL không đổi. 'New deployment' tạo URL mới cần cập nhật tất cả webhooks" },
    ]
  },
  {
    category: "Slack & Notifications",
    items: [
      { q: "PullNotifier không mention đúng tên trong #dev-mr",
        cause: "User mapping chưa config hoặc sai GitHub username",
        fix: "PullNotifier dashboard → Team → User Mapping → kiểm tra GitHub username (case-sensitive)" },
      { q: "Không nhận DM 'PR mới cần review' từ Sprint Bot",
        cause: "SLACK_USER_MAP sai hoặc Bot Token thiếu scope im:write",
        fix: "1. SLACK_USER_MAP: key = GitHub username, value = Slack Member ID (bắt đầu bằng U)\n2. api.slack.com/apps → Sprint Bot → OAuth → thêm scope im:write → reinstall" },
      { q: "Geekbot không gửi DM standup lúc 8:00",
        cause: "Timezone sai hoặc member không có trong participants",
        fix: "Geekbot dashboard → Daily Standup → Settings → Timezone = Asia/Ho_Chi_Minh → kiểm tra Participants" },
      { q: "Sprint summary hiển thị trên 1 dòng, không xuống dòng",
        cause: "Newline characters bị escape sai",
        fix: "Trong hàm triggerSprintSummaryAsync, đảm bảo message array dùng .join('\\n') với newline thật" },
    ]
  }
];

// ── MAINTENANCE DATA ──────────────────────────────────────────
const MAINTENANCE = {
  daily_tasks: [
    "8:00 — Đọc DM Google Calendar: hôm nay có meeting gì",
    "8:00 — Reply DM Geekbot standup (trước 9:00)",
    "9:00 — Đọc #standup: team làm gì, ai có blocker",
    "9:00+ — Check #dev-mr: PR nào đang chờ review (24h rule)",
    "Bắt đầu làm task trong Cycle hiện tại",
  ],
  sprint_start_tasks: [
    "Tạo Cycle mới trong Linear với ngày bắt đầu và kết thúc cụ thể",
    "Cập nhật CURRENT_SPRINT trong Code.gs (Sprint 1 → Sprint 2) → Deploy new version",
    "Sprint planning: cả team kéo issues từ Backlog → Todo vào Cycle",
    "Estimate story points cho từng issue",
    "Thống nhất ai làm gì, priority nào trước",
  ],
  sprint_end_tasks: [
    "Team lead gõ /sprint-summary trong #sprint",
    "Review kết quả cùng team: completion rate, carry-over, ai cần hỗ trợ",
    "Retrospective: gì tốt, gì cần cải thiện, blocker nào lặp lại",
    "Carry-over issues → add vào sprint tiếp theo",
    "Update CURRENT_SPRINT cho sprint mới",
  ],
  add_member: [
    "GitHub: invite vào tất cả repos → quyền Write",
    "Linear: invite vào workspace",
    "Slack: invite vào workspace + join 5 channels",
    "PullNotifier: thêm user mapping GitHub → Slack",
    "Geekbot: thêm vào participants",
    "Cập nhật SLACK_USER_MAP trong Code.gs → deploy new version",
    "Thành viên mới hoàn thành Checklist Onboarding",
  ],
  add_repo: [
    "Tạo repo trên GitHub, visibility Private",
    "Invite tất cả members → quyền Write",
    "Setup branch protection ruleset Protect main",
    "Thêm .github/workflows/ci.yml",
    "GitHub webhook → Apps Script Web App URL, events: Pull requests",
    "Thêm tên repo vào GITHUB_REPOS trong Code.gs → deploy new version",
    "PullNotifier: thêm repo → map vào #dev-mr",
  ],
  fallback: [
    { risk: "PullNotifier gặp sự cố", fallback: "Dùng GitHub native Slack app — cài thêm, subscribe repos vào #dev-mr" },
    { risk: "Apps Script lỗi",         fallback: "Xem Executions log → fix code → Edit deployment → New version → URL không đổi" },
    { risk: "Geekbot không gửi DM",    fallback: "Geekbot Dashboard → Send now để chạy thủ công" },
    { risk: "Linear ↔ GitHub lỗi",     fallback: "Kéo state thủ công trong Linear tạm thời" },
    { risk: "Sheets bị xóa nhầm",      fallback: "File → Version history → See version history → Restore" },
  ]
};

// ── PROJECT DOCS CONFIG ───────────────────────────────────────
// Thêm tài liệu: uncommment và điền đường dẫn, hoặc thêm object mới
const PROJECT_DOCS = [
  // Uncommment và điền đường dẫn file khi đã upload:
  { id:"prd",           label:"PRD",            file:"project/prd.md",           icon:"" },
  { id:"system-design", label:"System Design",  file:"project/architecture.md", icon:"" },
  { id:"dev-handofff",           label:"Dev Guideline",        file:"project/dev-handoff-guide.md",           icon:"" },
  { id:"ux-ui-design",      label:"UX UI Design", file:"project/ux-design-specification.md",      icon:"" },
  { id:"epcis",      label:"Epics", file:"project/epics.md",      icon:""}
];

const PROJECT_INFO = {
  name:        "Tên dự án của bạn",
  description: "Mô tả ngắn về dự án",
  version:     "v1.0.0",
  stack:       "Điền tech stack ở đây",
};