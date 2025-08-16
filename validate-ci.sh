#!/bin/bash

# Simple script to validate GitHub Actions workflow configuration
# This can be run locally to verify YAML syntax and configuration

set -e

echo "🔍 Validating GitHub Actions workflow configuration..."
echo ""

# Check if required files exist
echo "📁 Checking required workflow files..."
REQUIRED_FILES=(
    ".github/workflows/ci.yml"
    ".github/workflows/security.yml"
    "sonar-project.properties"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ Found: $file"
    else
        echo "❌ Missing: $file"
        exit 1
    fi
done

echo ""

# Validate YAML syntax if yamllint is available
if command -v yamllint &> /dev/null; then
    echo "📝 Validating YAML syntax..."
    yamllint .github/workflows/ci.yml
    yamllint .github/workflows/security.yml
    echo "✅ YAML syntax validation passed"
else
    echo "⚠️ yamllint not available, skipping YAML syntax validation"
fi

echo ""

# Check if Maven can validate the project
echo "🔧 Validating Maven project configuration..."
if mvn validate -B -q; then
    echo "✅ Maven project validation passed"
else
    echo "❌ Maven project validation failed"
    exit 1
fi

echo ""

# Check if JaCoCo plugin is properly configured
echo "📊 Checking JaCoCo plugin configuration..."
if mvn help:describe -Dplugin=org.jacoco:jacoco-maven-plugin -Dbrief=true -q > /dev/null 2>&1; then
    echo "✅ JaCoCo plugin configuration valid"
else
    echo "❌ JaCoCo plugin configuration invalid"
    exit 1
fi

echo ""

# Check if SonarQube plugin is properly configured
echo "🔍 Checking SonarQube plugin configuration..."
if mvn help:describe -Dplugin=org.sonarsource.scanner.maven:sonar-maven-plugin -Dbrief=true -q > /dev/null 2>&1; then
    echo "✅ SonarQube plugin configuration valid"
else
    echo "❌ SonarQube plugin configuration invalid"
    exit 1
fi

echo ""
echo "🎉 All CI/CD configuration checks passed!"
echo ""
echo "📋 Next steps:"
echo "   1. Push changes to trigger GitHub Actions"
echo "   2. Configure repository secrets:"
echo "      - SONAR_TOKEN for SonarCloud integration"
echo "      - CODECOV_TOKEN for enhanced coverage reporting (optional)"
echo "   3. Check workflow results in GitHub Actions tab"
echo "   4. Verify badges appear correctly in README"
echo ""
echo "🔗 Useful links:"
echo "   • GitHub Actions: https://github.com/Tonypsilon/rankify-api/actions"
echo "   • SonarCloud: https://sonarcloud.io/project/overview?id=Tonypsilon_rankify-api"
echo "   • Codecov: https://codecov.io/gh/Tonypsilon/rankify-api"