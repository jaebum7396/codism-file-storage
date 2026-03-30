package com.codism.filestorage.domain.file.controller;

import com.codism.filestorage.domain.file.entity.FileStatus;
import com.codism.filestorage.domain.file.repository.FileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileController 통합 테스트
 *
 * 시나리오: 임시 업로드 → 확정 → 다운로드 → 그룹 조회 → 삭제
 * Edge Case: 빈 파일, 허용되지 않는 확장자, 존재하지 않는 파일
 */
@DisplayName("File API 테스트")
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileRepository fileRepository;

    @Value("${file.storage.temp-path}")
    private String tempPath;

    @Value("${file.storage.base-path}")
    private String basePath;

    private Long fileId;
    private String storedName;
    private final String GROUP_ID = "test-group-001";

    @AfterAll
    void cleanup() throws Exception {
        fileRepository.deleteAll();
        // 테스트 디렉토리 정리
        Path tempDir = Path.of(tempPath);
        Path baseDir = Path.of(basePath);
        deleteDirectoryRecursively(tempDir);
        deleteDirectoryRecursively(baseDir);
    }

    private void deleteDirectoryRecursively(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                        });
            }
        } catch (Exception ignored) {}
    }

    // ==================== 시나리오 테스트: 임시 업로드 → 확정 → 다운로드 → 그룹 조회 → 삭제 ====================

    @Test
    @Order(1)
    @DisplayName("1. 임시 파일 업로드")
    void uploadTemp() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "테스트문서.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF 파일 내용입니다.".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/files/temp").file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.originalName").value("테스트문서.pdf"))
                .andExpect(jsonPath("$.storedName").isNotEmpty())
                .andExpect(jsonPath("$.contentType").value(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(jsonPath("$.fileSize").isNumber())
                .andExpect(jsonPath("$.extension").value("pdf"))
                .andExpect(jsonPath("$.status").value("TEMP"))
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        fileId = json.get("id").asLong();
        storedName = json.get("storedName").asText();
    }

    @Test
    @Order(2)
    @DisplayName("2. 임시 파일 벌크 업로드")
    void uploadTempBulk() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "문서1.pdf", MediaType.APPLICATION_PDF_VALUE, "문서1 내용".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "문서2.doc", "application/msword", "문서2 내용".getBytes());

        mockMvc.perform(multipart("/api/files/temp/bulk").file(file1).file(file2))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].originalName").value("문서1.pdf"))
                .andExpect(jsonPath("$[0].status").value("TEMP"))
                .andExpect(jsonPath("$[1].originalName").value("문서2.doc"))
                .andExpect(jsonPath("$[1].status").value("TEMP"));
    }

    @Test
    @Order(3)
    @DisplayName("3. 파일 확정 (TEMP → CONFIRMED)")
    void confirmFile() throws Exception {
        // 벌크 업로드된 파일 ID들도 가져오기
        List<Long> allTempIds = fileRepository.findByIdInAndStatus(
                fileRepository.findAll().stream()
                        .filter(e -> e.getStatus() == FileStatus.TEMP)
                        .map(e -> e.getId())
                        .toList(),
                FileStatus.TEMP
        ).stream().map(e -> e.getId()).toList();

        String body = """
                {"fileIds":%s,"groupId":"%s"}
                """.formatted(objectMapper.writeValueAsString(allTempIds), GROUP_ID);

        mockMvc.perform(post("/api/files/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(allTempIds.size()))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].groupId").value(GROUP_ID))
                .andExpect(jsonPath("$[0].url").isNotEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("4. 파일 다운로드")
    void downloadFile() throws Exception {
        mockMvc.perform(get("/api/files/download/{storedName}", storedName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    @Order(5)
    @DisplayName("5. 그룹 ID로 파일 목록 조회")
    void getFilesByGroupId() throws Exception {
        mockMvc.perform(get("/api/files/group/{groupId}", GROUP_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].groupId", everyItem(is(GROUP_ID))))
                .andExpect(jsonPath("$[*].status", everyItem(is("CONFIRMED"))));
    }

    @Test
    @Order(6)
    @DisplayName("6. 존재하지 않는 그룹 ID 조회 - 빈 목록 반환")
    void getFilesByNonExistentGroupId() throws Exception {
        mockMvc.perform(get("/api/files/group/{groupId}", "non-existent-group"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(7)
    @DisplayName("7. 파일 삭제")
    void deleteFile() throws Exception {
        mockMvc.perform(delete("/api/files/{id}", fileId))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 삭제 후 그룹 조회 시 해당 파일 미포함 확인
        mockMvc.perform(get("/api/files/group/{groupId}", GROUP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(8)
    @DisplayName("8. 삭제된 파일 다운로드 시도 - 실패")
    void downloadDeletedFile() throws Exception {
        mockMvc.perform(get("/api/files/download/{storedName}", storedName))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ==================== Edge Case ====================

    @Nested
    @DisplayName("Edge Case")
    class EdgeCase {

        @Test
        @DisplayName("빈 파일 업로드 시 400 에러")
        void uploadEmptyFile() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    new byte[0]
            );

            mockMvc.perform(multipart("/api/files/temp").file(emptyFile))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("허용되지 않는 확장자 업로드 시 400 에러")
        void uploadInvalidExtension() throws Exception {
            MockMultipartFile exeFile = new MockMultipartFile(
                    "file",
                    "malware.exe",
                    "application/octet-stream",
                    "악성 파일 내용".getBytes()
            );

            mockMvc.perform(multipart("/api/files/temp").file(exeFile))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("확장자 없는 파일 업로드 시 400 에러")
        void uploadFileWithoutExtension() throws Exception {
            MockMultipartFile noExtFile = new MockMultipartFile(
                    "file",
                    "noextension",
                    "application/octet-stream",
                    "확장자 없는 파일".getBytes()
            );

            mockMvc.perform(multipart("/api/files/temp").file(noExtFile))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 파일 다운로드 시 400 에러")
        void downloadNonExistentFile() throws Exception {
            mockMvc.perform(get("/api/files/download/{storedName}", "non-existent-file.pdf"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 파일 삭제 시 400 에러")
        void deleteNonExistentFile() throws Exception {
            mockMvc.perform(delete("/api/files/{id}", 999999L))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("확정할 파일이 없는 경우 400 에러")
        void confirmWithNoTempFiles() throws Exception {
            String body = """
                    {"fileIds":[999998,999999],"groupId":"no-such-group"}
                    """;

            mockMvc.perform(post("/api/files/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("fileIds가 비어있는 경우 400 에러")
        void confirmWithEmptyFileIds() throws Exception {
            String body = """
                    {"fileIds":[],"groupId":"empty-ids"}
                    """;

            mockMvc.perform(post("/api/files/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}
