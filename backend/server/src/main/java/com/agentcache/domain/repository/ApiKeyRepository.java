package com.agentcache.domain.repository;

import com.agentcache.domain.entity.ApiKey;
import com.agentcache.domain.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * API Key 数据访问接口。
 */
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findBySpaceIdAndStatus(Long spaceId, EntityStatus status);

    Optional<ApiKey> findByKeyHashAndStatus(String keyHash, EntityStatus status);

    List<ApiKey> findByKeyPrefixAndStatus(String keyPrefix, EntityStatus status);

    List<ApiKey> findByStatus(EntityStatus status);
}
