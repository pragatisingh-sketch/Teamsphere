#!/bin/bash

# Step 1: Extract external IP
EXTERNAL_IP=$(curl -s http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip -H "Metadata-Flavor: Google")
echo "Extracted External IP: $EXTERNAL_IP"

# Step 2: Update Nginx configuration
NGINX_CONFIG="/etc/nginx/sites-available/default"
sudo sed -i "s/server_name .*/server_name $EXTERNAL_IP;/" $NGINX_CONFIG
echo "Updated Nginx configuration with external IP."

# Step 3: Update allowedOrigins in SecurityConfig.java
SECURITY_CONFIG="/home/vbs_tms/teamsphere/backend/capsAllocation/src/main/java/com/vbs/capsAllocation/config/SecurityConfig.java"
sudo sed -i "s|setAllowedOrigins(Arrays.asList(\"http://.*\"))|setAllowedOrigins(Arrays.asList(\"http://$EXTERNAL_IP\"))|" $SECURITY_CONFIG
echo "Updated SecurityConfig.java with external IP."

# Step 3.1: Update URLs in GoogleSheetsService.java
GOOGLE_SHEETS_SERVICE="/home/vbs_tms/teamsphere/backend/capsAllocation/src/main/java/com/vbs/capsAllocation/service/GoogleSheetsService.java"
sudo sed -i "s|http://localhost:8080|http://$EXTERNAL_IP:8080|g" $GOOGLE_SHEETS_SERVICE
echo "Updated GoogleSheetsService.java with external IP."

# Step 4: Update SERVER_IP in env.js
ENV_JS="/home/vbs_tms/teamsphere/org-chart/src/assets/env.js"
sudo sed -i "s|SERVER_IP: \".*\"|SERVER_IP: \"$EXTERNAL_IP\"|" $ENV_JS
echo "Updated env.js with external IP."

# Step 5: Build and deploy frontend
cd /home/vbs_tms/teamsphere/org-chart
ng build --configuration production
sudo cp -r dist/org-chart/* /var/www/html
echo "Frontend built and deployed."

# Step 6: Build backend
cd /home/vbs_tms/teamsphere/backend/capsAllocation
mvn clean package
echo "Backend built successfully."

# Step 7: Restart services
sudo systemctl reload nginx
sudo systemctl restart nginx
sudo systemctl restart springboot.service
echo "Nginx and Spring Boot restarted successfully."

echo "Deployment completed successfully!"

