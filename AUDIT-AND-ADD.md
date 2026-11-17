@workspace You are auditing and extending an EXISTING Hospital Management System built with Java Spring Boot + MongoDB (running in Docker). The Admin interface (backend + frontend) is already partially built by someone else, and I am NOT 100% sure it works correctly.

Read the workspace yourself. Do NOT ask me to paste any file contents. Do NOT reference, read, or expect any other instruction file — everything you need is in this message.

STRICT RULES — READ FIRST
- Do NOT scaffold a new Spring Boot project.
- Do NOT regenerate pom.xml, application.properties, or Docker config unless a real bug requires it.
- Do NOT rewrite existing entities, controllers, services, repositories, or SecurityConfig without telling me WHICH file, WHY, and showing the exact diff.
- Do NOT create duplicate entities/controllers/services with similar names.
- Do NOT introduce new Maven dependencies unless strictly necessary — ask me first.
- Do NOT add a second password encoder, second JWT filter, or second exception handler — reuse what exists.
- Follow the EXISTING package structure, naming conventions, and code style.
- Spring rules: Spring Data MongoDB (@Document, MongoRepository) — NOT JPA/Hibernate. Constructor injection — not @Autowired fields. @PreAuthorize at method level for role checks. Return DTOs from controllers — never raw documents. Use ResponseEntity<> in controllers. Enforce ownership checks in the SERVICE layer, not just the controller.

PHASE 1 — AUDIT (do this FIRST, no code changes yet)

Step 1: Explore the workspace and map what exists.
- List every package under src/main/java (config, controller, service, repository, model/entity, dto, security, exception).
- For each package, list the class names.
- Read pom.xml, application.properties / application.yml, and docker-compose.yml.
- Read SecurityConfig, any JWT filter, any UserDetailsService, and the User document/entity.
- Read at least one existing Controller + Service + Repository triple to learn the code style.
- If a package or file mentioned above does not exist in the workspace, say so explicitly — do NOT invent it, do NOT assume it exists.

Step 2: Report a PASS / FAIL / UNCLEAR checklist for each:
- Does the app start? (missing beans, circular deps, bad Mongo URI for Docker)
- Is SecurityConfig permitting login/register and protecting everything else?
- Is the JWT filter registered in the security filter chain and does it validate tokens?
- Does the User document have a role field, and is it used in authorization?
- Are passwords BCrypt-hashed on register and checked on login?
- Are controllers returning DTOs, or leaking password hashes / internal fields?
- Any duplicate classes, dead code, or conflicting route mappings?
- Does the Mongo URI work inside Docker (host = service name, not localhost)?
- Is @PreAuthorize (or equivalent) actually enforced on admin-only endpoints?
- Bonus: any hardcoded secrets, missing validation, or obvious security holes.

Step 3: List what is MISSING relative to Phase 2 features below.

Step 4: Give me a prioritized FIX LIST (bugs first, then missing features). Then STOP and wait for my "go ahead".

PHASE 2 — ADD MISSING FEATURES (only after I approve Phase 1)

Reuse existing packages, classes, and patterns. No new architecture. No renaming. No duplicate classes. If something described below already exists, skip it and tell me — do not rebuild it.

A. Extend the EXISTING User document (check first — do not create a new one):
   - role enum: ADMIN | DOCTOR | NURSE | PATIENT
   - boolean active
   - optional staffId / patientId

B. Add to the EXISTING admin controller (do not create a second controller):
   - POST /api/admin/users — admin creates Doctor/Nurse/Patient (name, email, temp password, role)
   - PATCH /api/admin/users/{id} — edit role, reset password, activate/deactivate
   - DELETE /api/admin/users/{id} — soft-deactivate
   - Protect with the EXISTING authorization pattern (@PreAuthorize or whatever is used). Do not invent a new security mechanism.

C. Admin frontend — add to the EXISTING Admin interface (don't rebuild it):
   - One new "Manage Users" button on the existing admin dashboard
   - Opens a page/modal with users table + Create / Edit / Deactivate
   - Match existing UI components and styling — no new UI library

D. Self-service for Doctor / Nurse / Patient (new, but reuse existing JWT auth):
   - Each role logs in via the EXISTING login endpoint
   - GET /api/me returns only the caller's own profile (Authentication / Principal → fetch by id)
   - Patients see only their own records; doctors see only patients assigned to them
   - Enforce ownership at the SERVICE layer, not just the controller

OUTPUT FORMAT
- Phase 1: written report only. No code.
- Phase 2: only NEW files or exact diffs/edits to existing files. No full-file rewrites of working code.

Begin with Phase 1.
