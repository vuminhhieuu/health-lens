# Huong dan handoff cho dev team (HealthLens)

Tai lieu nay giup dev khac vao lam theo cung mot flow, dua tren BMAD artifacts da co san.

## 1) Tai lieu can doc truoc khi code

- Epic + Story goc: `_bmad-output/planning-artifacts/epics.md`
- Backlog/trang thai sprint: `_bmad-output/implementation-artifacts/sprint-status.yaml`
- Config BMAD: `_bmad/bmm/config.yaml`

## 2) Nguyen tac phan cong

- Don vi giao viec la **story key** trong `sprint-status.yaml`.
- Moi dev nen nhan 1-2 stories/luc, uu tien theo thu tu backlog.
- Khong nhan story co dependency chua xong trong cung epic.

Quy uoc branch de de theo doi:

- `feature/<story-key>`
- Vi du: `feature/1-1-starter-template-setup`

## 3) Flow chuan cho moi story

### Buoc A - Chuyen story tu backlog sang ready-for-dev

1. Scrum/Lead chay `create-story` (hoac chi dinh ro story key).
2. He thong tao file context cho story tai:
   - `_bmad-output/implementation-artifacts/<story-key>.md`
3. `sprint-status.yaml` tu dong update story sang `ready-for-dev`.

### Buoc B - Dev implement story

1. Tao branch theo quy uoc.
2. Chay `dev-story` voi file story duoc giao.
3. Implement dung theo Acceptance Criteria trong story file.
4. Dam bao test/lint/build pass.
5. Khi xong, story duoc dua ve `review`.

### Buoc C - Review va dong story

1. Chay `code-review` (khuyen nghi dung model/nguoi review khac).
2. Neu co findings: quay lai sua tren cung branch.
3. Khi pass review va merge, cap nhat story sang `done`.
4. Dong bo lai `sprint-status.yaml` neu can.

## 4) Checklist giao viec cho tung dev

Khi giao 1 story, lead gui toi thieu:

- Story key (vi du: `3-2-mobile-camera-capture-ux`)
- File story context (`_bmad-output/implementation-artifacts/<story-key>.md`)
- Branch name can tao
- Deadline + owner
- Yeu cau test bat buoc (unit/integration/e2e neu co)

## 5) Dinh nghia Done (toi thieu)

Mot story chi duoc xem la xong khi:

- Tat ca AC (Given/When/Then) da dat
- Test lien quan pass
- Khong lam vo regression chinh
- Co PR + review pass
- `sprint-status.yaml` phan anh dung trang thai cuoi

## 6) De xuat van hanh theo sprint

- Dau sprint:
  - Chot nhom stories se lam (Sprint scope)
  - Gan owner tung story key
- Giua sprint:
  - Daily update theo `sprint-status.yaml`
- Cuoi sprint:
  - Chot story done/chua done
  - Danh dau `epic-X-retrospective` neu da retro

## 7) Khoi dong nhanh (goi y)

Thu tu an toan de bat dau:

1. `1-1-starter-template-setup`
2. `1-2-email-password-registration`
3. `1-3-secure-login-logout-session`

Sau khi Epic 1 on dinh, tiep tuc Epic 2 va Epic 3.
