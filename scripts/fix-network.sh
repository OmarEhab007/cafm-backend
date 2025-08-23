#!/bin/bash

# Script to fix Docker network conflicts

echo "🔧 Fixing Docker network conflicts..."

# Check if the problematic network exists
if docker network ls | grep -q "cafm-backend_cafm-network"; then
    echo "📌 Found existing cafm-backend_cafm-network"
    
    # Stop any containers using this network
    echo "🛑 Stopping containers using the network..."
    docker compose down 2>/dev/null || docker-compose down 2>/dev/null
    
    # Remove the network
    echo "🗑️ Removing old network..."
    docker network rm cafm-backend_cafm-network 2>/dev/null || true
    
    echo "✅ Old network removed"
else
    echo "ℹ️ Network cafm-backend_cafm-network not found"
fi

# List networks using similar IP ranges that might conflict
echo ""
echo "📊 Checking for conflicting networks..."
docker network ls --format "table {{.Name}}\t{{.Driver}}" | grep -v "DRIVER"

# Check for networks with overlapping subnets
echo ""
echo "🔍 Inspecting network configurations..."
for network in $(docker network ls -q); do
    subnet=$(docker network inspect $network 2>/dev/null | grep -A 2 "Subnet" | grep -oE '([0-9]{1,3}\.){3}[0-9]{1,3}/[0-9]{1,2}' | head -1)
    if [[ ! -z "$subnet" ]]; then
        name=$(docker network inspect $network --format '{{.Name}}')
        echo "  • $name: $subnet"
    fi
done

echo ""
echo "🔄 Recreating network with automatic IP allocation..."

# Now let Docker automatically assign an IP range
if docker compose version &> /dev/null; then
    docker compose up -d
else
    docker-compose up -d
fi

echo ""
echo "✅ Network issue resolved! Services should be starting now."
echo ""
echo "Run 'docker compose ps' to verify all services are running."