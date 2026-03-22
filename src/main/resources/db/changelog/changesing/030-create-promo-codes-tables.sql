CREATE TABLE promo_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL, -- ACTIVE, INACTIVE
    scope VARCHAR(20) NOT NULL, -- GLOBAL, PERSONAL
    target_person_id UUID, -- NULL if GLOBAL
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID,
    CONSTRAINT fk_promo_codes_target_person FOREIGN KEY (target_person_id) REFERENCES persons(id) ON DELETE CASCADE,
    CONSTRAINT fk_promo_codes_created_by FOREIGN KEY (created_by_id) REFERENCES persons(id) ON DELETE SET NULL
);

CREATE TABLE promo_code_discounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promo_code_id UUID NOT NULL,
    course_id UUID, -- NULL if ALL courses
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT, FIXED_PRICE
    discount_value NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_promo_discounts_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id) ON DELETE CASCADE,
    CONSTRAINT fk_promo_discounts_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE TABLE promo_code_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promo_code_id UUID NOT NULL,
    person_id UUID NOT NULL,
    course_id UUID, -- The course they applied it to
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promo_usages_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id) ON DELETE CASCADE,
    CONSTRAINT fk_promo_usages_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE,
    CONSTRAINT fk_promo_usages_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL,
    CONSTRAINT uq_promo_usage_person UNIQUE (promo_code_id, person_id) -- User can use the exactly same code only once across the entire site
);
