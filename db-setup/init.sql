create database inventory_db;

create user new_user with password 'newpass';

grant all privileges on database inventory_db to new_user;