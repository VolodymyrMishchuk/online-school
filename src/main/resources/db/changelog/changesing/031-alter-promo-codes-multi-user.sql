CREATE TABLE promo_code_target_persons (
    promo_code_id UUID NOT NULL,
    person_id UUID NOT NULL,
    PRIMARY KEY (promo_code_id, person_id),
    CONSTRAINT fk_pctp_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id) ON DELETE CASCADE,
    CONSTRAINT fk_pctp_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

-- Migrate existing target_person_id data
INSERT INTO promo_code_target_persons (promo_code_id, person_id)
SELECT id, target_person_id FROM promo_codes WHERE target_person_id IS NOT NULL;

-- Drop constraints and column
ALTER TABLE promo_codes DROP CONSTRAINT fk_promo_codes_target_person;
ALTER TABLE promo_codes DROP COLUMN target_person_id;
