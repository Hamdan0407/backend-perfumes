INSERT INTO users (email, password, first_name, last_name, phone_number, address, city, country, zip_code, role, active, created_at, updated_at) VALUES
('owner@perfume.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Priya', 'Sharma', '9876543210', '123 Business Park', 'Mumbai', 'India', '400001', 'ADMIN', 1, NOW(), NOW()),
('manager@perfume.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Rajesh', 'Kumar', '9876543211', '456 Corporate Ave', 'Bangalore', 'India', '560001', 'ADMIN', 1, NOW(), NOW()),
('warehouse1@perfume.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Amit', 'Singh', '9876543212', '789 Warehouse St', 'Delhi', 'India', '110001', 'USER', 1, NOW(), NOW()),
('warehouse2@perfume.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Vikram', 'Patel', '9876543213', '321 Storage Lane', 'Chennai', 'India', '600001', 'USER', 1, NOW(), NOW()),
('customer1@example.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'John', 'Doe', '9876543214', '555 Main St', 'New York', 'USA', '10001', 'USER', 1, NOW(), NOW()),
('customer2@example.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Jane', 'Smith', '9876543215', '777 Oak Ave', 'Los Angeles', 'USA', '90001', 'USER', 1, NOW(), NOW()),
('customer3@example.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Mike', 'Johnson', '9876543216', '999 Pine Rd', 'Chicago', 'USA', '60601', 'USER', 1, NOW(), NOW()),
('customer4@example.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Sarah', 'Williams', '9876543217', '111 Elm St', 'Houston', 'USA', '77001', 'USER', 0, NOW(), NOW());
