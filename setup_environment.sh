#!/bin/bash
echo "Checking for Homebrew..."
if ! command -v brew &> /dev/null; then
    echo "Homebrew not found. Please install Homebrew first."
    exit 1
fi

echo "Installing MySQL and Redis..."
brew install mysql redis

echo "Starting Services..."
brew services start mysql
brew services start redis

echo "Waiting 10 seconds for services to initialize..."
sleep 10

echo "Creating Databases..."
mysql -u root -ppassword -e "CREATE DATABASE IF NOT EXISTS airline_flight_db;" || echo "Warning: Could not create Flight DB (Check password)"
mysql -u root -ppassword -e "CREATE DATABASE IF NOT EXISTS airline_user_db;" || echo "Warning: Could not create User DB (Check password)"

echo "Environment Setup Complete! You can now restart the Spring Boot applications."
