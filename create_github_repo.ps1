# Open GitHub "new repository" page with repo name prefilled
$repoName = "RWBOTANDROID"
$url = "https://github.com/new?name=$repoName"
Start-Process $url
Write-Host "Browser opened. Create repo (do not add README). Then run:"
Write-Host "  git remote add origin https://github.com/YOUR_USERNAME/$repoName.git"
Write-Host "  git push -u origin main"
