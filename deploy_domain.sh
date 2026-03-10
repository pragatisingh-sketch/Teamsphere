#!/bin/bash

set -e

FRONTEND_SRC="/home/vaibhav_rajoriya/teamsphere/org-chart"
FRONTEND_DIST_NAME="org-chart"  # Adjust this if your Angular project name is different
FRONTEND_DEST="/opt/teamsphere/frontend"

BACKEND_SRC="/home/vaibhav_rajoriya/teamsphere/backend/capsAllocation"
JAR_NAME="teamsphere.jar"
JAR_DEST="/opt/teamsphere/bin"

ENV_FILE="$FRONTEND_SRC/src/environments/environment.ts"

echo "===> Updating Angular environment file..."
cat > "$ENV_FILE" <<EOL
export const environment = {
  production: true,
  apiUrl: 'https://api.teamsphere.in'
};
EOL

echo "===> Building frontend..."
cd "$FRONTEND_SRC"
npm install
npm run build --prod

echo "===> Copying frontend to Nginx directory..."
rm -rf "$FRONTEND_DEST"/*
cp -r "$FRONTEND_SRC"/dist/"$FRONTEND_DIST_NAME"/* "$FRONTEND_DEST"

echo "===> Building backend..."
cd "$BACKEND_SRC"
mvn clean install -DskipTests

echo "===> Deploying backend jar..."
cd target
if [ -f "$JAR_DEST/$JAR_NAME" ]; then
    echo "===> Backing up existing JAR..."
    mv "$JAR_DEST/$JAR_NAME" "$JAR_DEST/${JAR_NAME}.bkp"
fi
cp *.jar "$JAR_DEST/$JAR_NAME"

echo "===> Restarting backend service..."
systemctl restart teamsphere

echo "===> Reloading Nginx..."
nginx -t && systemctl restart nginx

echo "✅ Deployment completed successfully."

