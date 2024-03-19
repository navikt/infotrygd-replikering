
-- create schema authorization testtabeller;
create user testtabeller IDENTIFIED BY MyPassword;

create table testtabeller.testtabell(
    ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    OPPDATERT TIMESTAMP(6)
);

grant all privileges on TESTTABELLER.testtabell to test;
ALTER USER test QUOTA UNLIMITED on USERS;
ALTER USER testtabeller QUOTA UNLIMITED on USERS;