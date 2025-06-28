# 🚀 Crypto Exchange Monitor - One-Click Deployment Script (PowerShell)
# ===============================================================

param(
    [string]$Platform = "railway",  # railway, render, docker
    [switch]$Test = $false,         # Run tests
    [switch]$Build = $false,        # Build only, no deployment
    [switch]$Help = $false          # Show help
)

# Color output function
function Write-ColorOutput {
    param([string]$Message, [string]$Color = "White")
    switch ($Color) {
        "Red" { Write-Host $Message -ForegroundColor Red }
        "Green" { Write-Host $Message -ForegroundColor Green }
        "Yellow" { Write-Host $Message -ForegroundColor Yellow }
        "Blue" { Write-Host $Message -ForegroundColor Blue }
        "Magenta" { Write-Host $Message -ForegroundColor Magenta }
        "Cyan" { Write-Host $Message -ForegroundColor Cyan }
        default { Write-Host $Message -ForegroundColor White }
    }
}

# Show help
function Show-Help {
    Write-ColorOutput "🚀 Crypto Exchange Monitor - One-Click Deployment Script" "Cyan"
    Write-ColorOutput "=======================================================" "Cyan"
    Write-ColorOutput ""
    Write-ColorOutput "Usage:" "Yellow"
    Write-ColorOutput "  .\quick-deploy.ps1 [Parameters]" "White"
    Write-ColorOutput ""
    Write-ColorOutput "Parameters:" "Yellow"
    Write-ColorOutput "  -Platform <platform>  Deployment platform (railway, render, docker)" "White"
    Write-ColorOutput "  -Test                 Run tests" "White"
    Write-ColorOutput "  -Build                Build only, no deployment" "White"
    Write-ColorOutput "  -Help                 Show this help" "White"
    Write-ColorOutput ""
    Write-ColorOutput "Examples:" "Yellow"
    Write-ColorOutput "  .\quick-deploy.ps1 -Platform railway" "White"
    Write-ColorOutput "  .\quick-deploy.ps1 -Test -Build" "White"
    Write-ColorOutput "  .\quick-deploy.ps1 -Platform docker" "White"
    exit 0
}

# Check required tools
function Test-Dependencies {
    Write-ColorOutput "🔍 Checking required tools..." "Blue"
    
    $missingTools = @()
    
    # Check Java
    try {
        $javaVersion = java -version 2>&1 | Select-String "version"
        Write-ColorOutput "✅ Java: $($javaVersion.Line)" "Green"
    } catch {
        $missingTools += "Java"
    }
    
    # Check Maven
    try {
        $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven"
        Write-ColorOutput "✅ Maven: $($mavenVersion.Line)" "Green"
    } catch {
        $missingTools += "Maven"
    }
    
    # Check Git
    try {
        $gitVersion = git --version
        Write-ColorOutput "✅ Git: $gitVersion" "Green"
    } catch {
        $missingTools += "Git"
    }
    
    # Check Docker (if needed)
    if ($Platform -eq "docker") {
        try {
            $dockerVersion = docker --version
            Write-ColorOutput "✅ Docker: $dockerVersion" "Green"
        } catch {
            $missingTools += "Docker"
        }
    }
    
    if ($missingTools.Count -gt 0) {
        Write-ColorOutput "❌ Missing required tools: $($missingTools -join ', ')" "Red"
        Write-ColorOutput "Please install missing tools and try again" "Red"
        exit 1
    }
    
    Write-ColorOutput "✅ All required tools are installed" "Green"
}

# Check configuration files
function Test-Configuration {
    Write-ColorOutput "📋 Checking configuration files..." "Blue"
    
    if (-not (Test-Path "config.properties")) {
        if (Test-Path "src/main/resources/config.properties.example") {
            Write-ColorOutput "⚠️  config.properties not found, copying example file..." "Yellow"
            Copy-Item "src/main/resources/config.properties.example" "config.properties"
            Write-ColorOutput "❗ Please edit config.properties to fill in your API keys" "Red"
            Write-ColorOutput "Press any key to continue..." "Yellow"
            $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        } else {
            Write-ColorOutput "❌ Configuration file not found" "Red"
            exit 1
        }
    }
    
    Write-ColorOutput "✅ Configuration file check completed" "Green"
}

# Kill running Java processes
function Stop-JavaProcesses {
    Write-ColorOutput "🛑 Stopping running Java processes..." "Yellow"
    
    try {
        $javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
        if ($javaProcesses) {
            foreach ($process in $javaProcesses) {
                Write-ColorOutput "Stopping Java process (PID: $($process.Id))" "Yellow"
                Stop-Process -Id $process.Id -Force
            }
            Start-Sleep -Seconds 2
            Write-ColorOutput "✅ Java processes stopped" "Green"
        } else {
            Write-ColorOutput "ℹ️  No running Java processes found" "Cyan"
        }
    } catch {
        Write-ColorOutput "⚠️  Failed to stop some Java processes: $($_.Exception.Message)" "Yellow"
    }
}

# Run tests
function Invoke-Tests {
    Write-ColorOutput "🧪 Running tests..." "Blue"
    
    try {
        mvn clean test
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✅ All tests passed" "Green"
        } else {
            Write-ColorOutput "❌ Tests failed" "Red"
            exit 1
        }
    } catch {
        Write-ColorOutput "❌ Test execution failed: $($_.Exception.Message)" "Red"
        exit 1
    }
}

# Build application
function Build-Application {
    Write-ColorOutput "🔨 Building application..." "Blue"
    
    # Stop Java processes first
    Stop-JavaProcesses
    
    # Clean target directory
    if (Test-Path "target") {
        Write-ColorOutput "🧹 Cleaning target directory..." "Yellow"
        Remove-Item -Path "target" -Recurse -Force -ErrorAction SilentlyContinue
    }
    
    try {
        mvn clean package -DskipTests
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✅ Build successful" "Green"
        } else {
            Write-ColorOutput "❌ Build failed" "Red"
            exit 1
        }
    } catch {
        Write-ColorOutput "❌ Build execution failed: $($_.Exception.Message)" "Red"
        exit 1
    }
}

# Git operations
function Invoke-GitOperations {
    Write-ColorOutput "📂 Git operations..." "Blue"
    
    # Check for uncommitted changes
    $gitStatus = git status --porcelain
    if ($gitStatus) {
        Write-ColorOutput "📝 Found uncommitted changes, committing..." "Yellow"
        git add .
        $commitMessage = "Auto deployment update - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        git commit -m $commitMessage
        Write-ColorOutput "✅ Changes committed: $commitMessage" "Green"
    }
    
    # Push to remote
    try {
        git push
        Write-ColorOutput "✅ Git push successful" "Green"
    } catch {
        Write-ColorOutput "⚠️  Git push failed, but continuing deployment..." "Yellow"
    }
}

# Railway deployment
function Deploy-Railway {
    Write-ColorOutput "🚂 Deploying to Railway..." "Blue"
    
    # Check Railway CLI
    try {
        railway --version | Out-Null
    } catch {
        Write-ColorOutput "❌ Railway CLI not installed" "Red"
        Write-ColorOutput "Please run: npm install -g @railway/cli" "Yellow"
        exit 1
    }
    
    # Check login status
    try {
        railway whoami | Out-Null
    } catch {
        Write-ColorOutput "❌ Please login to Railway first: railway login" "Red"
        exit 1
    }
    
    # Deploy
    try {
        railway up
        Write-ColorOutput "✅ Railway deployment successful" "Green"
    } catch {
        Write-ColorOutput "❌ Railway deployment failed: $($_.Exception.Message)" "Red"
        exit 1
    }
}

# Render deployment
function Deploy-Render {
    Write-ColorOutput "🎨 Deploying to Render..." "Blue"
    
    Write-ColorOutput "ℹ️  Render deployment is triggered automatically via Git webhook" "Cyan"
    Write-ColorOutput "📂 Please ensure GitHub connection is set up in Render" "Cyan"
    Write-ColorOutput "🔗 Deployment will start automatically after Git push" "Cyan"
}

# Docker deployment
function Deploy-Docker {
    Write-ColorOutput "🐳 Docker local deployment..." "Blue"
    
    # Stop existing containers
    try {
        docker-compose down
        Write-ColorOutput "🛑 Stopped existing containers" "Yellow"
    } catch {
        Write-ColorOutput "ℹ️  No running containers found" "Cyan"
    }
    
    # Build and start
    try {
        docker-compose up -d --build
        Write-ColorOutput "✅ Docker containers started successfully" "Green"
        
        # Show container status
        Write-ColorOutput "📊 Container status:" "Blue"
        docker-compose ps
        
        # Show logs
        Write-ColorOutput "📋 Real-time logs (Ctrl+C to exit):" "Blue"
        docker-compose logs -f
    } catch {
        Write-ColorOutput "❌ Docker deployment failed: $($_.Exception.Message)" "Red"
        exit 1
    }
}

# Main function
function Main {
    if ($Help) {
        Show-Help
    }
    
    Write-ColorOutput "🚀 Crypto Exchange Monitor - One-Click Deployment" "Cyan"
    Write-ColorOutput "=================================================" "Cyan"
    Write-ColorOutput "📅 Start time: $(Get-Date)" "White"
    Write-ColorOutput "🎯 Deployment platform: $Platform" "White"
    Write-ColorOutput ""
    
    # Check dependencies
    Test-Dependencies
    
    # Check configuration
    Test-Configuration
    
    # Run tests
    if ($Test) {
        Invoke-Tests
    }
    
    # Build application
    Build-Application
    
    # If build only, exit
    if ($Build) {
        Write-ColorOutput "✅ Build completed, skipping deployment" "Green"
        exit 0
    }
    
    # Git operations
    Invoke-GitOperations
    
    # Deploy based on platform
    switch ($Platform.ToLower()) {
        "railway" { Deploy-Railway }
        "render" { Deploy-Render }
        "docker" { Deploy-Docker }
        default {
            Write-ColorOutput "❌ Unsupported deployment platform: $Platform" "Red"
            Write-ColorOutput "Supported platforms: railway, render, docker" "Yellow"
            exit 1
        }
    }
    
    Write-ColorOutput ""
    Write-ColorOutput "🎉 Deployment completed!" "Green"
    Write-ColorOutput "📅 End time: $(Get-Date)" "White"
    
    # Show next steps
    Write-ColorOutput ""
    Write-ColorOutput "📋 Next steps:" "Yellow"
    switch ($Platform.ToLower()) {
        "railway" {
            Write-ColorOutput "  🔗 View app: railway open" "White"
            Write-ColorOutput "  📊 View logs: railway logs" "White"
            Write-ColorOutput "  📈 View status: railway status" "White"
        }
        "render" {
            Write-ColorOutput "  🔗 Go to Render Dashboard to check deployment status" "White"
            Write-ColorOutput "  📊 View logs and monitoring information" "White"
        }
        "docker" {
            Write-ColorOutput "  📊 View containers: docker-compose ps" "White"
            Write-ColorOutput "  📋 View logs: docker-compose logs -f" "White"
            Write-ColorOutput "  🛑 Stop services: docker-compose down" "White"
        }
    }
}

# Execute main function
Main 