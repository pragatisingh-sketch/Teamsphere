#  Git & SSH Setup Guide for Interns
### TeamSphere — VacoBinarySemantics LLP

---

## Table of Contents
1. [Install Git](#1-install-git)
2. [Configure Git Globals](#2-configure-git-globals)
3. [Generate SSH Key](#3-generate-ssh-key)
4. [Add SSH Key to GitHub](#4-add-ssh-key-to-github)
5. [Test SSH Connection](#5-test-ssh-connection)
6. [Clone the Repository](#6-clone-the-repository)
7. [Branch Strategy](#7-branch-strategy)
8. [Daily Workflow](#8-daily-workflow)
9. [Raising a Pull Request (PR)](#9-raising-a-pull-request-pr)
10. [Important Rules ⚠️](#10-important-rules)

---

## 1. Install Git

**macOS**
```bash
brew install git
```

**Ubuntu / Debian**
```bash
sudo apt update && sudo apt install git -y
```

**Windows**
Download from [https://git-scm.com/download/win](https://git-scm.com/download/win) and follow the installer.

Verify:
```bash
git --version
```

---

## 2. Configure Git Globals

Run these once after installing Git. Replace with your actual name and work email.

```bash
git config --global user.name "Your Name"
git config --global user.email "yourname@vacobs.com"
git config --global core.editor "code --wait"   # or nano / vim
git config --global init.defaultBranch main
```

Verify your config:
```bash
git config --list
```

---

## 3. Generate SSH Key

```bash
ssh-keygen -t ed25519 -C "yourname@vacobs.com"
```

When prompted:
- **File location** → Press `Enter` to accept default (`~/.ssh/id_ed25519`)
- **Passphrase** → Set a strong passphrase (recommended) or press `Enter` to skip

This creates two files:
| File | Purpose |
|------|---------|
| `~/.ssh/id_ed25519` | Private key — **never share this** |
| `~/.ssh/id_ed25519.pub` | Public key — this goes to GitHub |

### Start the SSH Agent

**macOS / Linux**
```bash
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
```

**Windows (Git Bash)**
```bash
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
```

---

## 4. Add SSH Key to GitHub

Copy your public key to clipboard:

**macOS**
```bash
pbcopy < ~/.ssh/id_ed25519.pub
```

**Linux**
```bash
xclip -selection clipboard < ~/.ssh/id_ed25519.pub
# or just print it:
cat ~/.ssh/id_ed25519.pub
```

**Windows (Git Bash)**
```bash
clip < ~/.ssh/id_ed25519.pub
```

Then on GitHub:
1. Go to **Settings** → **SSH and GPG keys**
2. Click **New SSH key**
3. Give it a title like `Work Laptop - 2025`
4. Paste your public key
5. Click **Add SSH key**

---

## 5. Test SSH Connection

```bash
ssh -T git@github.com
```

Expected output:
```
Hi <your-username>! You've successfully authenticated, but GitHub does not provide shell access.
```

If you see this, you're good to go! 

---

## 6. Clone the Repository

Always clone using SSH (not HTTPS).

```bash
git clone -b feature/<username>-<feature-name> --single-branch git@github.com:VacoBinarySemanticsLLP/teamsphere.git
```

> Replace `<username>` with your GitHub username and `<feature-name>` with the feature you've been assigned.

Example:
```bash
git clone -b feature/john-user-authentication --single-branch git@github.com:VacoBinarySemanticsLLP/teamsphere.git
```

Navigate into the project:
```bash
cd teamsphere
```

---

## 7. Branch Strategy

We follow a strict **GitFlow-style** branching model:

```
prod ──────────────────────────────────────── (production)
 |                  ^
 |           hotfix/<username>-<issue>   (cut from prod, PR back to prod)
 |
 └── develop ──────────────────────────────── (integration)
       └── feature/<username>-<feature>   (cut from develop, PR back to develop)
```

| Branch | Cut From | PR Target | Purpose |
|--------|----------|-----------|---------|
| `prod` | — | — | Production-ready code. Never commit directly. |
| `develop` | `prod` | — | Integration branch. All features merge here. |
| `feature/<username>-<n>` | `develop` | `develop` | Your working branch for new features. |
| `hotfix/<username>-<n>` | `prod` | `prod` | Urgent production bug fixes — cut directly from prod. |

### Naming Convention

```
feature/<username>-<feature-name>
  e.g. feature/john-login-page
  e.g. feature/priya-dashboard-api

hotfix/<username>-<issue-name>
  e.g. hotfix/john-fix-null-pointer
  e.g. hotfix/priya-payment-timeout
```

Use **lowercase** and **hyphens** (no underscores or spaces). Always prefix with your **GitHub username**.

### Creating a feature branch (from develop)

```bash
git checkout develop
git pull origin develop
git checkout -b feature/<username>-<feature-name>
git push -u origin feature/<username>-<feature-name>
```

### Creating a hotfix branch (from prod)

```bash
git checkout prod
git pull origin prod
git checkout -b hotfix/<username>-<issue-name>
git push -u origin hotfix/<username>-<issue-name>
```

> ⚠️ Hotfixes are **always cut from `prod`**, not `develop`. After the hotfix is merged to `prod` via PR, the lead will back-merge it into `develop`.

---

## 8. Daily Workflow

### Starting your day — sync with develop

```bash
git checkout feature/<username>-your-branch
git pull origin develop
```

> This keeps your feature branch up to date with the latest changes from `develop`.

### Working on your feature

```bash
# Check your current branch
git branch

# Stage your changes
git add .

# Commit with the required message format (see below)
git commit -m "A-John | R-Priya | feat: add login form validation"

# Push to remote
git push origin feature/<username>-your-branch
```

---

##  Commit Message Convention

Every commit **must** follow this exact format:

```
A-<Assignee-Name> | R-<Reviewer-Name> | <type>: <short description>
```

| Part | Meaning |
|------|---------|
| `A-<Assignee-Name>` | The person who wrote the code (you) |
| `R-<Reviewer-Name>` | The person who will review this work |
| `<type>: <description>` | What the commit does |

### Commit Types

```
feat     → new feature
fix      → bug fix
chore    → maintenance / config
docs     → documentation
refactor → code restructure, no feature change
test     → adding or updating tests
```

### Examples

```bash
git commit -m "A-John | R-Priya | feat: add user profile endpoint"
git commit -m "A-Priya | R-Rahul | fix: resolve null check in auth middleware"
git commit -m "A-Rahul | R-John | docs: update README with setup steps"
git commit -m "A-Sara | R-Priya | refactor: clean up dashboard API response"
git commit -m "A-John | R-Sara | chore: update env config for staging"
```

### ❌ Bad commit messages (don't do this)

```bash
git commit -m "fixed bug"               # missing A/R tags, vague
git commit -m "A-John | wip"            # missing reviewer, no type
git commit -m "feat: login page"        # missing A and R fields entirely
git commit -m "A-john | R-priya | stuff" # no type prefix, vague description
```

---

## 9. Raising a Pull Request (PR)

**Every change must go through a PR. Direct pushes to `develop` or `prod` are not allowed.**

### Feature PR (feature → develop)

1. Push your branch:
   ```bash
   git push origin feature/<username>-your-branch
   ```

2. Go to [VacoBinarySemanticsLLP/teamsphere](https://github.com/VacoBinarySemanticsLLP/teamsphere)

3. Click **"Compare & pull request"**

4. Set the PR details:
   | Field | Value |
   |-------|-------|
   | **Base branch** | `develop` |
   | **Compare branch** | `feature/<username>-your-branch` |
   | **Title** | Clear and descriptive |
   | **Description** | What was done, why, and any notes for the reviewer |

5. Assign a **reviewer** (your team lead or senior dev)

6. Click **Create pull request**

### Hotfix PR (hotfix → prod)

Same steps as above, but the base branch must point to `prod`:

| Field | Value |
|-------|-------|
| **Base branch** | `prod` ← important! |
| **Compare branch** | `hotfix/<username>-your-issue` |

> After the hotfix is merged to `prod`, your team lead will handle merging it back into `develop`.

### PR Checklist before raising

- [ ] Code is tested locally
- [ ] No `console.log` or debug code left in
- [ ] Feature branch is synced with `develop` (no conflicts)
- [ ] Hotfix branch is synced with `prod` (no conflicts)
- [ ] All commits follow the `A-<Name> | R-<Name> | type: message` format
- [ ] PR description is filled out
- [ ] Correct **base branch** is selected (`develop` for features, `prod` for hotfixes)

---

## 10. Important Rules ⚠️

| Rule | Detail |
|------|--------|
| ❌ Never use `git fetch --all` | This command is **prohibited** in this project |
| ❌ Never push directly to `develop` or `prod` | Always raise a PR |
| ❌ Never force push | `git push --force` is not allowed |
| ❌ Never cut a hotfix from `develop` | Hotfixes are **always cut from `prod`** |
| ❌ Never commit without A/R tags | Every commit must have Assignee and Reviewer |
| ✅ Always raise a PR | Even for small changes |
| ✅ Feature branches sync from `develop` | Pull `develop` before starting work |
| ✅ Hotfix branches sync from `prod` | Pull `prod` before creating your hotfix |
| ✅ Use SSH, not HTTPS | All clones and remotes must use SSH URLs |
| ✅ Branch names include your username | Always use `feature/<username>-<n>` format |

---

## Quick Reference Cheatsheet

```bash
# Initial setup (one-time)
git config --global user.name "Your Name"
git config --global user.email "you@vacobs.com"
ssh-keygen -t ed25519 -C "you@vacobs.com"
ssh -T git@github.com

# Clone your feature branch
git clone -b feature/<username>-<feature-name> --single-branch git@github.com:VacoBinarySemanticsLLP/teamsphere.git

# Create a new feature branch (from develop)
git checkout develop && git pull origin develop
git checkout -b feature/<username>-<feature-name>

# Create a hotfix branch (from prod — NOT develop)
git checkout prod && git pull origin prod
git checkout -b hotfix/<username>-<issue-name>

# Daily sync (feature)
git pull origin develop

# Commit & push — always use A/R format
git add .
git commit -m "A-<Your-Name> | R-<Reviewer-Name> | feat: your message"
git push origin feature/<username>-<feature-name>

# Raise PR on GitHub:
#   feature → base: develop
#   hotfix  → base: prod
```

---

>  If you face any issues, reach out to your team lead before trying workarounds. When in doubt, don't force push — ask first!
