package com.mishchuk.onlineschool.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.mishchuk.onlineschool.repository.entity.PromoCodeEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PromoCodeRepositoryImpl implements PromoCodeRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public Page<PromoCodeEntity> findPaginatedPromoCodes(
            String search,
            String sortKey,
            String sortDir,
            String statusSort,
            Pageable pageable,
            java.util.UUID creatorId) {
        
        StringBuilder selectClause = new StringBuilder("SELECT pc.* FROM promo_codes pc ");

        StringBuilder whereClause = new StringBuilder("WHERE 1=1 ");
        if (search != null && !search.trim().isEmpty()) {
            whereClause.append("AND (pc.code ILIKE :search OR EXISTS (")
                       .append("SELECT 1 FROM promo_code_target_persons pctp ")
                       .append("JOIN persons p ON pctp.person_id = p.id ")
                       .append("WHERE pctp.promo_code_id = pc.id AND ")
                       .append("(p.first_name ILIKE :search OR p.last_name ILIKE :search OR p.email ILIKE :search)")
                       .append(")) ");
        }

        StringBuilder orderClause = new StringBuilder("ORDER BY ");
        
        // Priority 1: statusSort
        if ("top".equals(statusSort)) {
            orderClause.append("CASE WHEN pc.status = 'ACTIVE' THEN 0 ELSE 1 END ASC, ");
        } else if ("bottom".equals(statusSort)) {
            orderClause.append("CASE WHEN pc.status = 'ACTIVE' THEN 1 ELSE 0 END ASC, ");
        }

        // Priority 2: sortKey
        String dir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        if (sortKey != null && !sortKey.isEmpty()) {
            switch (sortKey) {
                case "code":
                    orderClause.append("pc.code ").append(dir).append(", ");
                    break;
                case "status":
                    orderClause.append("pc.status ").append(dir).append(", ");
                    break;
                case "person":
                    orderClause.append("(SELECT MIN(p.first_name) FROM persons p JOIN promo_code_target_persons ptp ON ptp.person_id = p.id WHERE ptp.promo_code_id = pc.id) ").append(dir).append(", ");
                    break;
                case "createdAt":
                    orderClause.append("pc.created_at ").append(dir).append(", ");
                    break;
            }
        }
        
        // Tie-breaker
        orderClause.append("pc.id ASC");

        if (creatorId != null) {
            whereClause.append("AND pc.created_by_id = :creatorId ");
        }

        String dataQueryStr = selectClause.toString() + whereClause + orderClause;
        String countQueryStr = "SELECT COUNT(pc.id) FROM promo_codes pc " + whereClause;

        Query dataQuery = em.createNativeQuery(dataQueryStr, PromoCodeEntity.class);
        Query countQuery = em.createNativeQuery(countQueryStr);

        if (creatorId != null) {
            dataQuery.setParameter("creatorId", creatorId);
            countQuery.setParameter("creatorId", creatorId);
        }

        if (search != null && !search.trim().isEmpty()) {
            String searchParam = "%" + search.trim() + "%";
            dataQuery.setParameter("search", searchParam);
            countQuery.setParameter("search", searchParam);
        }

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<PromoCodeEntity> result = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(result, pageable, total);
    }
}
