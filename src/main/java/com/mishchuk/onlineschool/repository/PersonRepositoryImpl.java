package com.mishchuk.onlineschool.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PersonRepositoryImpl implements PersonRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public Page<PersonEntity> findPaginatedUsers(
            String search,
            String sortKey,
            String sortDir,
            String blockedSort, // "top", "bottom", or null
            String adminSort,   // "top", "bottom", or null
            Pageable pageable) {
        
        StringBuilder selectClause = new StringBuilder("SELECT p.* FROM persons p ");
        
        StringBuilder joinClause = new StringBuilder();
        joinClause.append("LEFT JOIN ( ");
        joinClause.append("  SELECT e.student_id, ");
        joinClause.append("         COUNT(e.id) as enrollments_count, ");
        joinClause.append("         MIN(c.name) as first_course_name, ");
        joinClause.append("         MIN(e.created_at) as earliest_enrollment, ");
        joinClause.append("         MAX(e.created_at) as latest_enrollment, ");
        // compute expiry: if access_duration is null, default to 0 to prevent null addition
        joinClause.append("         MIN(e.created_at + make_interval(days => CAST(COALESCE(c.access_duration, 0) AS int))) as earliest_expiry ");
        joinClause.append("  FROM enrollments e ");
        joinClause.append("  JOIN courses c ON e.course_id = c.id ");
        joinClause.append("  GROUP BY e.student_id ");
        joinClause.append(") stats ON p.id = stats.student_id ");

        StringBuilder whereClause = new StringBuilder("WHERE 1=1 ");
        if (search != null && !search.trim().isEmpty()) {
            whereClause.append("AND (p.first_name ILIKE :search OR p.last_name ILIKE :search OR p.email ILIKE :search) ");
        }

        StringBuilder orderClause = new StringBuilder("ORDER BY ");
        
        // Priority 1: blockedSort
        if ("top".equals(blockedSort)) {
            orderClause.append("CASE WHEN p.status = 'BLOCKED' THEN 0 ELSE 1 END ASC, ");
        } else if ("bottom".equals(blockedSort)) {
            orderClause.append("CASE WHEN p.status = 'BLOCKED' THEN 1 ELSE 0 END ASC, ");
        }

        // Priority 2: adminSort
        if ("top".equals(adminSort)) {
            orderClause.append("CASE WHEN p.role IN ('ADMIN', 'FAKE_ADMIN') THEN 0 ELSE 1 END ASC, ");
        } else if ("bottom".equals(adminSort)) {
            orderClause.append("CASE WHEN p.role IN ('ADMIN', 'FAKE_ADMIN') THEN 1 ELSE 0 END ASC, ");
        }

        // Priority 3: sortKey
        String dir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        if (sortKey != null && !sortKey.isEmpty()) {
            switch (sortKey) {
                case "name":
                    orderClause.append("p.first_name ").append(dir).append(", p.last_name ").append(dir).append(", ");
                    break;
                case "contacts":
                    orderClause.append("p.email ").append(dir).append(", ");
                    break;
                case "role":
                    orderClause.append("p.role ").append(dir).append(", ");
                    break;
                case "status":
                    orderClause.append("p.status ").append(dir).append(", ");
                    break;
                case "createdAt":
                    orderClause.append("p.created_at ").append(dir).append(", ");
                    break;
                case "enrollments":
                    orderClause.append("COALESCE(stats.enrollments_count, 0) ").append(dir).append(", ");
                    break;
                case "enrollment_name":
                    orderClause.append("stats.first_course_name ").append(dir).append(" NULLS LAST, ");
                    break;
                case "enrollment_date":
                    if ("asc".equalsIgnoreCase(sortDir)) {
                        orderClause.append("stats.earliest_enrollment ASC NULLS LAST, ");
                    } else {
                        orderClause.append("stats.latest_enrollment DESC NULLS LAST, ");
                    }
                    break;
                case "enrollment_timeLeft":
                    orderClause.append("stats.earliest_expiry ").append(dir).append(" NULLS LAST, ");
                    break;
            }
        }
        
        // Tie-breaker
        orderClause.append("p.id ASC");

        String dataQueryStr = selectClause.toString() + joinClause + whereClause + orderClause;
        String countQueryStr = "SELECT COUNT(p.id) FROM persons p " + whereClause;

        Query dataQuery = em.createNativeQuery(dataQueryStr, PersonEntity.class);
        Query countQuery = em.createNativeQuery(countQueryStr);

        if (search != null && !search.trim().isEmpty()) {
            String searchParam = "%" + search.trim() + "%";
            dataQuery.setParameter("search", searchParam);
            countQuery.setParameter("search", searchParam);
        }

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<PersonEntity> result = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(result, pageable, total);
    }
}
