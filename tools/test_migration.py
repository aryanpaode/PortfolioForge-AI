#!/usr/bin/env python3
"""Run the actual Kotlin migration SQL against SQLite with the uploaded v1 schema.
This validates SQL/data preservation, not the Android Room-generated schema validator.
The latter has a separate instrumentation test that must run on Android.
"""
from pathlib import Path
import sqlite3, re, unittest
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'app/src/main/java/com/aryanpaode/portfolioforgeai/data/db/DatabaseMigrations.kt'
class MigrationTests(unittest.TestCase):
 def setUp(self):
  self.db=sqlite3.connect(':memory:')
  self.db.executescript((ROOT/'app/src/androidTest/assets/v1_schema.sql').read_text())
  self.before={t:self.db.execute(f'SELECT * FROM {t}').fetchall() for t in ['profile','documents','career_items','skills']}
  self.sql=re.findall(r'db.execSQL\("(.*?)"\)',SOURCE.read_text())
  for sql in self.sql:self.db.execute(sql)
 def tearDown(self): self.db.close()
 def test_all_six_migration_statements_executed(self): self.assertEqual(6,len(self.sql))
 def test_existing_profile_preserved(self): self.assertEqual(self.before['profile'][0],self.db.execute('SELECT * FROM profile').fetchone()[:-1])
 def test_original_document_text_and_path_preserved(self): self.assertEqual(self.before['documents'][0],self.db.execute('SELECT * FROM documents').fetchone()[:-3])
 def test_original_publication_preserved(self): self.assertEqual(self.before['career_items'][0],self.db.execute('SELECT * FROM career_items').fetchone()[:-1])
 def test_original_skills_preserved(self): self.assertEqual(self.before['skills'][0],self.db.execute('SELECT * FROM skills').fetchone()[:-1])
 def test_new_doc_defaults(self): self.assertEqual(('', '', 0),self.db.execute('SELECT contentHash,ocrWarning,importedAt FROM documents').fetchone())
 def test_new_field_defaults(self):
  for table,column in [('profile','portfolioUrl'),('skills','evidence'),('career_items','sourceDocumentId')]:self.assertEqual('',self.db.execute(f'SELECT {column} FROM {table}').fetchone()[0])
 def test_unique_skill_name_retained(self):
  with self.assertRaises(sqlite3.IntegrityError): self.db.execute("INSERT INTO skills VALUES ('duplicate','Python','Programming','Manual','')")
 def test_zero_skill_conference_can_be_stored(self):
  self.db.execute("DELETE FROM skills")
  self.db.execute("INSERT INTO career_items VALUES ('conference','Conference','ICSCES','Example','Participant','September 2026','','doc-old')")
  self.assertEqual(('ICSCES',),self.db.execute("SELECT title FROM career_items WHERE type='Conference'").fetchone())
 def test_integrity_check(self):self.assertEqual(('ok',),self.db.execute('PRAGMA integrity_check').fetchone())
if __name__=='__main__':unittest.main(verbosity=2)
