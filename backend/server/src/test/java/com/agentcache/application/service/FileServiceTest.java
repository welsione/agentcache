package com.agentcache.application.service;

import com.agentcache.application.service.ApiKeyService.AuthenticatedCaller;
import com.agentcache.common.exception.UnauthorizedException;
import com.agentcache.domain.entity.FileRecord;
import com.agentcache.domain.entity.Space;
import com.agentcache.domain.entity.SpaceMember;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.FileVisibility;
import com.agentcache.domain.enums.SpaceMemberRole;
import com.agentcache.domain.enums.StorageType;
import com.agentcache.domain.port.FileStoragePort;
import com.agentcache.domain.repository.FileRecordRepository;
import com.agentcache.domain.repository.SpaceMemberRepository;
import com.agentcache.domain.repository.SpaceRepository;
import com.agentcache.infrastructure.storage.StorageRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * FileService 角色权限测试。
 */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRecordRepository fileRecordRepository;

    @Mock
    private SpaceMemberRepository memberRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private StorageRouter storageRouter;

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private FileAccessLogService accessLogService;

    @InjectMocks
    private FileService fileService;

    private AuthenticatedCaller user(long userId) {
        return new AuthenticatedCaller(userId, false, null, null);
    }

    private AuthenticatedCaller apiKey(long spaceId, SpaceMemberRole role) {
        return new AuthenticatedCaller(null, true, spaceId, role);
    }

    /** 构建一个默认的 Space mock 返回值。 */
    private Space defaultSpace(Long spaceId) {
        return Space.builder()
                .id(spaceId)
                .name("test-space")
                .ownerId(1L)
                .storageType(StorageType.LOCAL)
                .defaultVisibility(FileVisibility.PRIVATE)
                .build();
    }

    @Test
    void readerCannotUpload() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L).role(SpaceMemberRole.READER).build()));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> fileService.upload(1L, user(10L), "f.txt", "text/plain", 1,
                        new ByteArrayInputStream(new byte[]{1}), null, null, null));
        assertTrue(ex.getMessage().toLowerCase().contains("reader"));
        verify(fileStoragePort, never()).store(any(), any(), anyLong(), any());
    }

    @Test
    void memberCanUpload() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L).role(SpaceMemberRole.MEMBER).build()));
        when(spaceRepository.findById(1L)).thenReturn(Optional.of(defaultSpace(1L)));
        when(storageRouter.resolve(StorageType.LOCAL)).thenReturn(fileStoragePort);
        when(fileRecordRepository.save(any(FileRecord.class))).thenAnswer(inv -> {
            FileRecord r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        FileRecord record = fileService.upload(1L, user(10L), "f.txt", "text/plain", 1,
                new ByteArrayInputStream(new byte[]{1}), null, null, null);
        assertEquals(99L, record.getId());
        assertEquals("f.txt", record.getOriginalName());
        verify(fileStoragePort).store(any(), any(), anyLong(), any());
    }

    @Test
    void readerCannotDelete() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L).role(SpaceMemberRole.READER).build()));

        assertThrows(UnauthorizedException.class,
                () -> fileService.delete(1L, 100L, user(10L)));
        verify(fileRecordRepository, never()).save(any());
    }

    @Test
    void managerCanDelete() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L).role(SpaceMemberRole.MANAGER).build()));
        when(fileRecordRepository.findByIdAndSpaceId(100L, 1L))
                .thenReturn(Optional.of(FileRecord.builder().id(100L).spaceId(1L).status(EntityStatus.ACTIVE)
                        .visibility(FileVisibility.PRIVATE).storagePath("1/x").storageType(StorageType.LOCAL).build()));
        when(storageRouter.resolve(StorageType.LOCAL)).thenReturn(fileStoragePort);

        fileService.delete(1L, 100L, user(10L));
        verify(fileRecordRepository).save(any(FileRecord.class));
        verify(fileStoragePort).delete("1/x");
    }

    @Test
    void apiKeyForOtherSpaceIsRejected() {
        assertThrows(UnauthorizedException.class,
                () -> fileService.upload(1L, apiKey(2L, SpaceMemberRole.MANAGER), "f.txt", "text/plain", 1,
                        new ByteArrayInputStream(new byte[]{1}), null, null, null));
    }

    @Test
    void apiKeyManagerCanUploadInOwnSpace() {
        when(spaceRepository.findById(2L)).thenReturn(Optional.of(defaultSpace(2L)));
        when(storageRouter.resolve(StorageType.LOCAL)).thenReturn(fileStoragePort);
        when(fileRecordRepository.save(any(FileRecord.class))).thenAnswer(inv -> {
            FileRecord r = inv.getArgument(0);
            r.setId(7L);
            return r;
        });

        FileRecord record = fileService.upload(2L, apiKey(2L, SpaceMemberRole.MEMBER), "f.txt", "text/plain", 1,
                new ByteArrayInputStream(new byte[]{1}), null, null, null);
        assertEquals(7L, record.getId());
        assertNull(record.getCreatedBy()); // API Key 场景下 createdBy 为 null
    }

    @Test
    void apiKeyReaderCannotUploadInOwnSpace() {
        assertThrows(UnauthorizedException.class,
                () -> fileService.upload(2L, apiKey(2L, SpaceMemberRole.READER), "f.txt", "text/plain", 1,
                        new ByteArrayInputStream(new byte[]{1}), null, null, null));
        verify(fileStoragePort, never()).store(any(), any(), anyLong(), any());
    }

    @Test
    void managerCanSetVisibility() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L)
                        .role(SpaceMemberRole.MANAGER).build()));
        FileRecord record = FileRecord.builder().id(100L).spaceId(1L)
                .visibility(FileVisibility.PRIVATE).status(EntityStatus.ACTIVE).build();
        when(fileRecordRepository.findByIdAndSpaceId(100L, 1L)).thenReturn(Optional.of(record));
        when(fileRecordRepository.save(record)).thenReturn(record);

        FileRecord updated = fileService.setVisibility(1L, 100L, FileVisibility.PUBLIC, user(10L));

        assertSame(record, updated);
        assertEquals(FileVisibility.PUBLIC, updated.getVisibility());
        verify(fileRecordRepository).save(record);
    }

    @Test
    void readerCannotSetVisibility() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L)
                        .role(SpaceMemberRole.READER).build()));

        assertThrows(UnauthorizedException.class,
                () -> fileService.setVisibility(1L, 100L, FileVisibility.PUBLIC, user(10L)));

        verify(fileRecordRepository, never()).findByIdAndSpaceId(anyLong(), anyLong());
        verify(fileRecordRepository, never()).save(any());
    }
}
