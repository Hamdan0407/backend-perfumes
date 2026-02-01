# GitHub Integration Setup Guide

## Quick Start

### Step 1: Download Git (First Time Only)
```powershell
# Download from: https://git-scm.com/download/win
# Run the installer with default options
```

### Step 2: Setup Repository (First Time Only)
```powershell
powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\SETUP_GITHUB.ps1
```

This will:
✅ Initialize git repository
✅ Configure user settings
✅ Add remote origin
✅ Create .gitignore
✅ Create auto-push script

### Step 3: Initial Push (First Time Only)
```powershell
powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\INITIAL_PUSH.ps1
```

This pushes all current code to GitHub.

---

## Regular Usage (After Features)

### Push After Completing a Feature
```powershell
powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage "Feature: Chatbot real behavior fix"
```

### Examples:
```powershell
# After implementing chatbot
powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage "Feature: Real chatbot behavior with auto-focus"

# After bug fixes
powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage "Fix: Input focus issue in chatbot"

# After adding database features
powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage "Feature: Database integration for products"

# After security updates
powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage "Security: Updated authentication middleware"
```

---

## What Gets Pushed

✅ **Includes**:
- All source code
- Configuration files
- Documentation
- Test scripts

❌ **Excludes** (in .gitignore):
- node_modules/
- target/ (Maven build)
- .env files
- IDE settings
- Log files
- OS files (Thumbs.db, .DS_Store)

---

## Verify Everything Worked

### View Your Repo Online:
```
https://github.com/Hamdan0407/Perfume
```

### View Commits:
```
https://github.com/Hamdan0407/Perfume/commits
```

### View Recent Changes:
```
https://github.com/Hamdan0407/Perfume/compare
```

---

## Troubleshooting

### Problem: "Git command not found"
**Solution**: Download and install Git from https://git-scm.com/download/win

### Problem: "Authentication failed"
**Solution 1**: Use Personal Access Token instead of password
1. Go to https://github.com/settings/tokens
2. Create new token with repo permissions
3. Use token as password when prompted

**Solution 2**: Use SSH key
1. Generate SSH key: `ssh-keygen -t ed25519`
2. Add to GitHub: https://github.com/settings/keys
3. Change remote URL: `git remote set-url origin git@github.com:Hamdan0407/Perfume.git`

### Problem: "Repository not found"
**Solution**: Verify the URL is correct:
```powershell
git remote -v
# Should show: origin  https://github.com/Hamdan0407/Perfume.git
```

### Problem: "Nothing to commit"
**Solution**: This means no changes were made. Make code changes first, then push.

---

## Manual Commands (If Needed)

If scripts don't work, use these commands directly in PowerShell:

```powershell
cd c:\Users\Hamdaan\Documents\maam

# Check status
git status

# Stage all changes
git add .

# View what will be committed
git diff --cached

# Create commit
git commit -m "Feature: Your description"

# Push to GitHub
git push

# View commit history
git log --oneline
```

---

## After First Push - Future Workflow

1. **Complete a major feature** (e.g., chatbot fix, database integration, etc.)
2. **Test it thoroughly** (make sure all tests pass)
3. **Run the auto-push**:
   ```powershell
   powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage "Feature: Describe what you did"
   ```
4. **Verify on GitHub**: Check https://github.com/Hamdan0407/Perfume/commits

---

## What Happens With Each Push

✅ **Auto-push script**:
1. Stages all changes (`git add .`)
2. Creates commit with your message
3. Pushes to GitHub main branch
4. Shows success/failure status

---

## Repository Structure

```
Perfume/
├── frontend/          # React UI
├── src/               # Spring Boot backend
├── pom.xml           # Maven config
├── docker-compose.yml # Docker setup
├── .gitignore        # Files to ignore
└── auto-push.ps1     # Your push script
```

---

## Quick Reference

| Command | What It Does |
|---------|------------|
| `SETUP_GITHUB.ps1` | First time setup (one time only) |
| `INITIAL_PUSH.ps1` | First push to GitHub (one time only) |
| `auto-push.ps1 -CommitMessage "..."` | Push after features (use regularly) |
| `git status` | Check what changed |
| `git log --oneline` | View commit history |

---

## Schedule

**After Each Major Feature:**
1. Complete feature ✅
2. Run tests ✅
3. Push with: `powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage "Feature: ..."`

**Major Features to Push:**
- Chatbot improvements
- Backend features
- Database changes
- Security updates
- Performance improvements
- Bug fixes
- Documentation updates

---

## Need Help?

1. Check GitHub: https://github.com/Hamdan0407/Perfume
2. View commits: https://github.com/Hamdan0407/Perfume/commits
3. Check issues: https://github.com/Hamdan0407/Perfume/issues

---

**Status**: ✅ Ready to use!
