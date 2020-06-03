-- TODO: don't forget to set the password
CREATE USER cloudcard IDENTIFIED BY pick_a_password;
GRANT CONNECT TO cloudcard;
GRANT CREATE SESSION TO cloudcard;
GRANT UNLIMITED TABLESPACE TO cloudcard;

-- This is obviously a very simple table.
-- We suggest getting things configured with this table first,
-- then adding keys, constraints, triggers, etc.
CREATE TABLE CLOUDCARD.CLOUDCARD_PHOTOS (
    STUDENT_ID varchar2(256) not null,
    PHOTO blob not null,
    DATE_CREATED timestamp default CURRENT_TIMESTAMP not null
 );