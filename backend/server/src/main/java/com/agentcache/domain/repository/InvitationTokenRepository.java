package com.agentcache.domain.repository;

import com.agentcache.domain.entity.InvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 邀请令牌数据访问接口。
 */
public interface InvitationTokenRepository extends JpaRepository<InvitationToken, Long> {

    Optional<InvitationToken> findByToken(String token);
}
