-- Version-1 DDL taken from the uploaded generated AppDatabase_Impl.java.
-- Synthetic seeded values only. This is not a user database.
CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, `branch` TEXT NOT NULL, `semester` TEXT NOT NULL, `cgpa` TEXT NOT NULL, `githubUrl` TEXT NOT NULL, `linkedInUrl` TEXT NOT NULL, `targetRole` TEXT NOT NULL, `summary` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE TABLE IF NOT EXISTS `documents` (`id` TEXT NOT NULL, `fileName` TEXT NOT NULL, `localPath` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `category` TEXT NOT NULL, `extractedText` TEXT NOT NULL, `verifiedText` TEXT NOT NULL, `verified` INTEGER NOT NULL, `cloudUrl` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`));
CREATE TABLE IF NOT EXISTS `career_items` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `description` TEXT NOT NULL, `date` TEXT NOT NULL, `link` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE TABLE IF NOT EXISTS `skills` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE UNIQUE INDEX IF NOT EXISTS `index_skills_name` ON `skills` (`name`);

INSERT INTO profile VALUES (1,'Test Student','student@example.invalid','1234567890','Computer Engineering','9','3.49','https://github.com/test','https://linkedin.com/in/test','Developer','Original summary');
INSERT INTO documents VALUES ('doc-old','original.pdf','/original/private/path.pdf','application/pdf','Certification','Original extracted text','Original verified text',1,'',12345);
INSERT INTO career_items VALUES ('old-pub','Publication','Original research topic','Original publisher','Original description','2025','https://example.org/paper');
INSERT INTO skills VALUES ('old-skill','Python','Programming','Manual');
PRAGMA user_version=1;
