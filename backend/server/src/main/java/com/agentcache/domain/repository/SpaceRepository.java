package com.agentcache.domain.repository;

import com.agentcache.domain.entity.Space;
import com.agentcache.domain.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 空间数据访问接口。
 */
public interface SpaceRepository extends JpaRepository<Space, Long> {

    List<Space> findByStatus(EntityStatus status);
}
