package com.agentcache.domain.repository;

import com.agentcache.domain.entity.SpaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 空间成员数据访问接口。
 */
public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {

    List<SpaceMember> findByUserId(Long userId);

    List<SpaceMember> findBySpaceId(Long spaceId);

    Optional<SpaceMember> findBySpaceIdAndUserId(Long spaceId, Long userId);

    boolean existsBySpaceIdAndUserId(Long spaceId, Long userId);

    void deleteBySpaceIdAndUserId(Long spaceId, Long userId);
}
