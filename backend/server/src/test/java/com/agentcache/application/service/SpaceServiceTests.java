package com.agentcache.application.service;

import com.agentcache.application.service.ApiKeyService.AuthenticatedCaller;
import com.agentcache.common.exception.UnauthorizedException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.entity.Space;
import com.agentcache.domain.entity.SpaceMember;
import com.agentcache.domain.enums.SpaceMemberRole;
import com.agentcache.domain.repository.SpaceMemberRepository;
import com.agentcache.domain.repository.SpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SpaceService 测试。
 */
@ExtendWith(MockitoExtension.class)
class SpaceServiceTests {

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceMemberRepository memberRepository;

    @InjectMocks
    private SpaceService spaceService;

    private static AuthenticatedCaller user(long userId) {
        return new AuthenticatedCaller(userId, false, null, null);
    }

    private static AuthenticatedCaller apiKey(Long spaceId, SpaceMemberRole role) {
        return new AuthenticatedCaller(null, true, spaceId, role);
    }

    @Test
    void shouldCreateSpaceAndAddOwnerAsManager() {
        when(spaceRepository.save(any(Space.class))).thenAnswer(invocation -> {
            Space space = invocation.getArgument(0);
            space.setId(1L);
            return space;
        });

        Space space = spaceService.createSpace(user(100L), "team-a", "desc");

        assertEquals("team-a", space.getName());
        verify(memberRepository).save(any(SpaceMember.class));
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(ValidationException.class, () -> spaceService.createSpace(user(100L), "  ", null));
    }

    @Test
    void shouldDenyAccessWhenNotMember() {
        when(spaceRepository.findById(1L)).thenReturn(Optional.of(Space.builder().id(1L).ownerId(100L).build()));
        when(memberRepository.existsBySpaceIdAndUserId(1L, 200L)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> spaceService.getSpace(1L, user(200L)));
    }

    @Test
    void shouldListSpacesByUserMembership() {
        SpaceMember m = SpaceMember.builder().spaceId(1L).userId(100L).role(SpaceMemberRole.MEMBER).build();
        when(memberRepository.findByUserId(100L)).thenReturn(List.of(m));
        when(spaceRepository.findAllById(List.of(1L))).thenReturn(List.of(Space.builder().id(1L).name("s").build()));

        List<Space> spaces = spaceService.listSpacesByUser(user(100L));

        assertEquals(1, spaces.size());
    }

    @Test
    void shouldListOnlyBoundSpaceForApiKey() {
        when(spaceRepository.findAllById(List.of(7L)))
                .thenReturn(List.of(Space.builder().id(7L).name("apikey-space").build()));

        List<Space> spaces = spaceService.listSpacesByUser(apiKey(7L, SpaceMemberRole.MEMBER));

        assertEquals(1, spaces.size());
        assertEquals("apikey-space", spaces.get(0).getName());
        verify(memberRepository, never()).findByUserId(any());
    }

    @Test
    void shouldRejectApiKeyWithoutSpaceId() {
        assertThrows(UnauthorizedException.class,
                () -> spaceService.listSpacesByUser(apiKey(null, SpaceMemberRole.MEMBER)));
    }
}