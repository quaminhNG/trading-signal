#!/bin/bash

# Ensure postgres client is installed
sudo apt-get install -y postgresql-client

# Create database if it doesn't exist
export PGPASSWORD="234554326Qua"
psql -h trading-signal-db.cxggeo6cqe7h.ap-southeast-2.rds.amazonaws.com -U postgres -d postgres -tc "SELECT 1 FROM pg_database WHERE datname = 'trading_signal'" | grep -q 1 || psql -h trading-signal-db.cxggeo6cqe7h.ap-southeast-2.rds.amazonaws.com -U postgres -d postgres -c "CREATE DATABASE trading_signal;"

# Write systemd service
cat << 'EOF' | sudo tee /etc/systemd/system/trading-signal.service
[Unit]
Description=Trading Signal Spring Boot Application
After=network.target

[Service]
User=ubuntu
ExecStart=/usr/bin/java -Xmx400m -Xss256k -XX:+UseSerialGC -jar /home/ubuntu/signal-0.0.1-SNAPSHOT.jar
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://trading-signal-db.cxggeo6cqe7h.ap-southeast-2.rds.amazonaws.com:5432/trading_signal"
Environment="SPRING_DATASOURCE_USERNAME=postgres"
Environment="SPRING_DATASOURCE_PASSWORD=234554326Qua"
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Reload and start
sudo systemctl daemon-reload
sudo systemctl enable trading-signal
sudo systemctl restart trading-signal
sudo systemctl status trading-signal --no-pager
