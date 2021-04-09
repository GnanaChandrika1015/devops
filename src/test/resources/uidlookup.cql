CREATE TABLE IF NOT EXISTS UPUI (
    UPUIShort TEXT, 
    createdDate TIMESTAMP, 
    expiryDate TIMESTAMP, 
    FID TEXT, 
    parentAUI TEXT, 
    primaryRepositoryID TEXT, 
    state INT, 
    updatedDate TIMESTAMP, 
    UPUILong TEXT,
    VALFLAG INT,
    IsImport INT,
    ImportStatus INT,
    MessageTimeLong TIMESTAMP,
 PRIMARY KEY (UPUIShort))
 WITH comment = 'UPUI lookup table for pack level UI related validations';
CREATE INDEX ON UPUI (parentAUI);
CREATE TABLE IF NOT EXISTS AUI (
    AUI TEXT, 
    createdDate TIMESTAMP, 
    FID TEXT, 
    parentAUI TEXT, 
    primaryRepositoryID TEXT, 
    state INT, 
    updatedDate TIMESTAMP,
    VALFLAG INT,
    IsImport INT,
    ImportStatus INT,
    MessageTimeLong TIMESTAMP,
 PRIMARY KEY (AUI)) 
 WITH COMMENT='AUI lookup table for aggregated level UI related validations';
CREATE INDEX ON AUI (parentAUI);
CREATE TABLE IF NOT EXISTS primaryRepositoryID_lookup (
    primaryRepositoryID TEXT, 
    inserted_on TIMESTAMP, 
 PRIMARY KEY (primaryRepositoryID))
 WITH COMMENT='Lookup table for primaryRepositoryID or Code of the messages';
 
 CREATE TABLE IF NOT EXISTS message_hash_lookup (
    messageHash TEXT, 
    inserted_on TIMESTAMP, 
    primaryRepositoryID TEXT, 
    resubmissionFlag BOOLEAN, 
 PRIMARY KEY (messageHash))
 WITH COMMENT='Lookup table for message hashes';
 
CREATE TABLE IF NOT EXISTS recallhistory (
    primaryrepositoryid text PRIMARY KEY,
    creationdate timestamp,
    recallprimaryrepositoryid text)
WITH COMMENT='Lookup table for Recall History';
CREATE INDEX ON recallHistory (recallPrimaryRepositoryID);
 
