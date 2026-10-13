# Architectural & Implementation Decisions Log

This document records architectural, design, and implementation decisions made during the build process, following Rule 1.3 and Section 20 of `BUILD_PROMPT.md`.

---

### Decision 001: Workspace Structure & Legacy Prototype Preservation
- **Date:** 2026-09-30
- **Question:** How to handle the existing prototype and files present in `Project-Resource-Allocation-Task-Optimization-System--main`?
- **Decision:** Archive all pre-existing prototype files, presentations (`.pptx`), and runbook spreadsheets (`.xlsx`) safely into a subfolder `legacy_prototype/`. Build the clean, full-scale Spring Boot 3 + MySQL 8 + Vanilla JS application directly at the root of `Project-Resource-Allocation-Task-Optimization-System--main`.
- **Reason:** Preserves 100% of previous artifacts for viva and academic reference with zero data loss, while giving the production Spring Boot build clean paths (`backend/`, `frontend/`, `database/`, `docker-compose.yml`) matching the specification without legacy file collisions or Maven build interference.

---
