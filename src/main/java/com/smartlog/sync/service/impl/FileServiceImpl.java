package com.smartlog.sync.service.impl;

import com.smartlog.sync.dto.FileInfoDto;
import com.smartlog.sync.repository.FileInfoRepository;
import com.smartlog.sync.repository.entity.FileInfo;
import com.smartlog.sync.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final FileInfoRepository fileInfoRepository;

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            // HWP — 브라우저/OS마다 MIME이 다르게 잡힘
            "application/x-hwp",
            "application/haansofthwp",
            "application/vnd.hancom.hwp",
            "application/vnd.hancom.hwpx",
            "application/hwp"
    );

    // HWP/HWPX는 MIME이 불안정하므로 확장자로도 허용 여부 판단
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".hwp", ".hwpx"
    );

    @Override
    public FileInfoDto upload(Long repId, Long userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("파일이 비어있습니다.");

        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extLower = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase() : "";

        boolean allowed = ALLOWED_TYPES.contains(contentType) || ALLOWED_EXTENSIONS.contains(extLower);
        if (!allowed) {
            throw new IllegalArgumentException("PDF, 이미지(JPG·PNG·GIF), HWP 파일만 업로드 가능합니다.");
        }

        // 보고서 ID별 디렉터리에 저장
        Path dirPath = Paths.get(uploadDir, "report_" + repId);
        Files.createDirectories(dirPath);

        String ext = extLower.isEmpty() ? "" : extLower;
        String storedName = UUID.randomUUID().toString() + ext;

        Files.copy(file.getInputStream(), dirPath.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        log.info("[파일 업로드] repId={}, userId={}, 원본={}, 크기={}B", repId, userId, originalName, file.getSize());

        FileInfo saved = fileInfoRepository.save(FileInfo.builder()
                .repId(repId)
                .userId(userId)
                .originalName(originalName)
                .storedName(storedName)
                .fileSize(file.getSize())
                .contentType(contentType)
                .build());

        return FileInfoDto.from(saved);
    }

    @Override
    public void delete(Long fileId, Long userId) throws IOException {
        FileInfo fileInfo = fileInfoRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));

        if (!fileInfo.getUserId().equals(userId)) {
            throw new SecurityException("파일 삭제 권한이 없습니다.");
        }

        Path filePath = Paths.get(uploadDir, "report_" + fileInfo.getRepId(), fileInfo.getStoredName());
        Files.deleteIfExists(filePath);
        fileInfoRepository.delete(fileInfo);
        log.info("[파일 삭제] fileId={}", fileId);
    }

    @Override
    public List<FileInfoDto> getByRepId(Long repId) {
        return FileInfoDto.fromList(fileInfoRepository.findByRepIdOrderByRegDtAsc(repId));
    }

    @Override
    public Resource loadAsResource(Long fileId, Long userId) {
        FileInfo fileInfo = fileInfoRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));

        if (!fileInfo.getUserId().equals(userId)) {
            throw new SecurityException("파일 접근 권한이 없습니다.");
        }

        try {
            Path filePath = Paths.get(uploadDir, "report_" + fileInfo.getRepId(), fileInfo.getStoredName());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("파일을 읽을 수 없습니다: " + fileInfo.getOriginalName());
            }
            return resource;
        } catch (Exception e) {
            throw new RuntimeException("파일 로드 실패: " + e.getMessage());
        }
    }

    @Override
    public FileInfoDto getById(Long fileId) {
        return FileInfoDto.from(fileInfoRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다.")));
    }
}