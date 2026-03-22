-- Liquibase formatted SQL
-- changeset volodymyr:009-add-lesson-relationship-to-files

-- Додати foreign key до lessons
ALTER TABLE files 
ADD COLUMN lesson_id UUID REFERENCES lessons(id) ON DELETE CASCADE;

-- Створити індекс для швидкого пошуку файлів по уроку
CREATE INDEX idx_files_lesson_id ON files(lesson_id);

-- Мігрувати існуючі файли з relatedEntityType='LESSON'
-- Перенести їх до lesson_id для зворотної сумісності
UPDATE files 
SET lesson_id = related_entity_id 
WHERE related_entity_type = 'LESSON';
