-- Must be run in inventory_db;

grant usage, create on schema public to new_user;

alter default privileges in schema public grant all privileges on tables to new_user;

alter default privileges in schema public grant all privileges on sequences to new_user;