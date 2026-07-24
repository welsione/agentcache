package com.agentcache.application.service;

import com.agentcache.application.service.ApiKeyService.AuthenticatedCaller;
import com.agentcache.common.exception.ResourceNotFoundException;
import com.agentcache.common.exception.UnauthorizedException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.common.util.FilenameSanitizer;
import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.entity.FileRecord;
import com.agentcache.domain.entity.Space;
import com.agentcache.domain.entity.SpaceMember;
import com.agentcache.domain.enums.ActorType;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.FileAccessAction;
import com.agentcache.domain.enums.FileVisibility;
import com.agentcache.domain.enums.SpaceMemberRole;
import com.agentcache.domain.enums.StorageType;
import com.agentcache.domain.port.FileStoragePort;
import com.agentcache.domain.repository.FileRecordRepository;
import com.agentcache.domain.repository.SpaceMemberRepository;
import com.agentcache.domain.repository.SpaceRepository;
import com.agentcache.infrastructure.storage.StorageRouter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestAttributeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.InputStream;
import java.util.UUID;

/**
 * 文件应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRecordRepository fileRecordRepository;
    private final SpaceMemberRepository memberRepository;
    private final SpaceRepository spaceRepository;
    private final StorageRouter storageRouter;
    private final FileAccessLogService accessLogService;

    /**
     * 上传文件。
     *
     * @param spaceId        空间 ID
     * @param caller         调用者
     * @param originalName   原始文件名
     * @param contentType    内容类型
     * @param size           文件大小
     * @param content        文件内容流
     * @param description    文件说明（可选）
     * @param visibility     文件可见性（可选，默认取空间设置）
     * @param expiresInHours 有效期小时数（可选，null 表示永久）
     * @return 文件记录
     */
    @Transactional
    public FileRecord upload(Long spaceId, AuthenticatedCaller caller, String originalName, String contentType,
                             long size, InputStream content, String description,
                             FileVisibility visibility, Integer expiresInHours) {
        SpaceMemberRole callerRole = requireSpaceAccess(spaceId, caller);
        requireWriteRole(callerRole);
        if (originalName == null || originalName.isBlank()) {
            throw new ValidationException("Original name is required");
        }
        String safeName = FilenameSanitizer.sanitize(originalName);
        String storedName = UUID.randomUUID().toString();
        String storagePath = spaceId + "/" + storedName;

        // 获取空间信息以确定存储后端和默认可见性
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + spaceId));
        StorageType storageType = space.getStorageType();
        FileStoragePort storagePort = storageRouter.resolve(storageType);

        storagePort.store(storagePath, content, size, contentType);

        FileRecord record = FileRecord.builder()
                .spaceId(spaceId)
                .name(storedName)
                .originalName(safeName)
                .contentType(contentType)
                .size(size)
                .storagePath(storagePath)
                .createdBy(caller.userId())
                .storageType(storageType)
                .description(description)
                .visibility(visibility != null ? visibility : space.getDefaultVisibility())
                .expiresAt(expiresInHours != null && expiresInHours > 0
                        ? TimeUtil.now().plusHours(expiresInHours) : null)
                .build();
        record = fileRecordRepository.save(record);

        // 记录审计日志
        accessLogService.log(record.getId(), spaceId, FileAccessAction.UPLOAD,
                caller.isApiKey() ? ActorType.API_KEY : ActorType.USER,
                caller.userId(), null, getCurrentRequest(),
                "originalName=" + safeName);

        return record;
    }

    public Page<FileRecord> list(Long spaceId, AuthenticatedCaller caller, String keyword, Pageable pageable) {
        requireSpaceAccess(spaceId, caller);
        if (keyword == null || keyword.isBlank()) {
            return fileRecordRepository.findBySpaceIdAndStatus(spaceId, EntityStatus.ACTIVE, pageable);
        }
        return fileRecordRepository.findBySpaceIdAndOriginalNameContainingAndStatus(
                spaceId, keyword, EntityStatus.ACTIVE, pageable);
    }

    public FileRecord get(Long spaceId, Long fileId, AuthenticatedCaller caller) {
        FileRecord record = findActiveFile(spaceId, fileId);
        checkExpiry(record);
        if (record.getVisibility() != FileVisibility.PUBLIC) {
            requireSpaceAccess(spaceId, caller);
        }
        // 记录查看日志
        accessLogService.log(fileId, spaceId, FileAccessAction.VIEW,
                determineActorType(caller), determineActorId(caller),
                null, getCurrentRequest(), null);
        return record;
    }

    public InputStream download(Long spaceId, Long fileId, AuthenticatedCaller caller) {
        FileRecord record = findActiveFile(spaceId, fileId);
        checkExpiry(record);
        if (record.getVisibility() != FileVisibility.PUBLIC) {
            requireSpaceAccess(spaceId, caller);
        }
        FileStoragePort storagePort = storageRouter.resolve(
                record.getStorageType() != null ? record.getStorageType() : StorageType.LOCAL);

        // 记录下载日志
        accessLogService.log(fileId, spaceId, FileAccessAction.DOWNLOAD,
                determineActorType(caller), determineActorId(caller),
                null, getCurrentRequest(), null);

        return storagePort.read(record.getStoragePath());
    }

    @Transactional
    public void delete(Long spaceId, Long fileId, AuthenticatedCaller caller) {
        SpaceMemberRole callerRole = requireSpaceAccess(spaceId, caller);
        requireWriteRole(callerRole);
        FileRecord record = findActiveFile(spaceId, fileId);

        // 记录删除日志
        accessLogService.log(fileId, spaceId, FileAccessAction.DELETE,
                caller.isApiKey() ? ActorType.API_KEY : ActorType.USER,
                caller.userId(), null, getCurrentRequest(),
                "originalName=" + record.getOriginalName());

        record.setStatus(EntityStatus.DELETED);
        fileRecordRepository.save(record);
        FileStoragePort storagePort = storageRouter.resolve(
                record.getStorageType() != null ? record.getStorageType() : StorageType.LOCAL);
        storagePort.delete(record.getStoragePath());
        log.info("Deleted file: {}", fileId);
    }

    @Transactional
    public FileRecord setVisibility(Long spaceId, Long fileId, FileVisibility visibility,
                                    AuthenticatedCaller caller) {
        SpaceMemberRole callerRole = requireSpaceAccess(spaceId, caller);
        requireWriteRole(callerRole);
        FileRecord record = findActiveFile(spaceId, fileId);
        FileVisibility oldVisibility = record.getVisibility();
        record.setVisibility(visibility);
        record = fileRecordRepository.save(record);

        // 记录可见性变更日志
        accessLogService.log(fileId, spaceId, FileAccessAction.VISIBILITY_CHANGE,
                caller.isApiKey() ? ActorType.API_KEY : ActorType.USER,
                caller.userId(), null, getCurrentRequest(),
                "from=" + oldVisibility + " to=" + visibility);

        return record;
    }

    /**
     * 修改文件说明。
     */
    @Transactional
    public FileRecord updateDescription(Long spaceId, Long fileId, String description, AuthenticatedCaller caller) {
        SpaceMemberRole role = requireSpaceAccess(spaceId, caller);
        requireWriteRole(role);
        FileRecord record = findActiveFile(spaceId, fileId);
        record.setDescription(description);
        return fileRecordRepository.save(record);
    }

    /**
     * 修改文件有效期。
     *
     * @param expiresInHours 有效期小时数，null 表示永久
     */
    @Transactional
    public FileRecord updateExpiry(Long spaceId, Long fileId, Integer expiresInHours, AuthenticatedCaller caller) {
        SpaceMemberRole role = requireSpaceAccess(spaceId, caller);
        requireWriteRole(role);
        FileRecord record = findActiveFile(spaceId, fileId);
        record.setExpiresAt(expiresInHours != null && expiresInHours > 0
                ? TimeUtil.now().plusHours(expiresInHours) : null);
        return fileRecordRepository.save(record);
    }

    /**
     * 获取 PUBLIC 文件（无需认证）。
     */
    public FileRecord getPublicFile(Long fileId) {
        FileRecord record = fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        if (record.getVisibility() != FileVisibility.PUBLIC || record.getStatus() != EntityStatus.ACTIVE) {
            throw new ResourceNotFoundException("File not found: " + fileId);
        }
        checkExpiry(record);
        return record;
    }

    /**
     * 校验调用者对目标空间具有访问权限（公开方法，供 Controller 调用）。
     *
     * @return 调用者在该空间中的角色
     */
    public SpaceMemberRole requireSpaceAccess(Long spaceId, AuthenticatedCaller caller) {
        if (caller == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (caller.isApiKey()) {
            if (caller.spaceId() == null || !caller.spaceId().equals(spaceId)) {
                throw new UnauthorizedException("API Key not bound to space: " + spaceId);
            }
            return caller.spaceRole();
        }
        if (caller.userId() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        SpaceMember member = memberRepository.findBySpaceIdAndUserId(spaceId, caller.userId())
                .orElseThrow(() -> new UnauthorizedException("No access to space: " + spaceId));
        return member.getRole();
    }

    private void requireWriteRole(SpaceMemberRole role) {
        if (role == SpaceMemberRole.READER) {
            throw new UnauthorizedException("Readers cannot modify files");
        }
    }

    /**
     * 查找活跃文件，不存在则抛异常。
     */
    private FileRecord findActiveFile(Long spaceId, Long fileId) {
        FileRecord record = fileRecordRepository.findByIdAndSpaceId(fileId, spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        if (record.getStatus() != EntityStatus.ACTIVE) {
            throw new ResourceNotFoundException("File not found: " + fileId);
        }
        return record;
    }

    /**
     * 检查文件是否过期。
     */
    private void checkExpiry(FileRecord record) {
        if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(TimeUtil.now())) {
            throw new ResourceNotFoundException("File expired: " + record.getId());
        }
    }

    private ActorType determineActorType(AuthenticatedCaller caller) {
        if (caller == null) {
            return ActorType.ANONYMOUS;
        }
        return caller.isApiKey() ? ActorType.API_KEY : ActorType.USER;
    }

    private Long determineActorId(AuthenticatedCaller caller) {
        if (caller == null) {
            return null;
        }
        return caller.userId();
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
