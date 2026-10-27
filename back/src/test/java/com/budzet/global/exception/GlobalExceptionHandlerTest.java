package com.budzet.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.budzet.global.api.ApiResponse;
import jakarta.persistence.LockTimeoutException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ExceptionTestController.class)
@Import(GlobalExceptionHandler.class)
@ContextConfiguration(classes = GlobalExceptionHandlerTest.TestApplication.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("정상 응답을 한다.")
    void successResponseUsesCommonFormat() throws Exception {
        mockMvc.perform(get("/test/success-with-data"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(201))
                .andExpect(jsonPath("$.message").value("데이터를 생성하였습니다."))
                .andExpect(jsonPath("$.data").value("Hello, Budzet!"));
    }

    @Test
    @DisplayName("BusinessException 발생 시, 기본 메시지와 함께 에러 응답을 한다.")
    void businessExceptionDefaultMessage() throws Exception {
        mockMvc.perform(get("/test/business-exception-default-message"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value(404))
                .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").hasJsonPath())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("BusinessException 발생 시, 커스텀 메시지와 함께 에러 응답을 한다.")
    void businessExceptionCustomMessage() throws Exception {
        mockMvc.perform(get("/test/business-exception-custom-message"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value(404))
                .andExpect(jsonPath("$.message").value("테스트 리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("@Valid 에러 발생 시, 구체적인 필드 에러를 포함한다.")
    void validationExceptionIncludesFieldErrorsInData() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value(400))
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.data[0].field").value("name"))
                .andExpect(jsonPath("$.data[0].message").value("이름은 필수입니다."));
    }

    @Test
    @DisplayName("숫자 경로 변수에 문자열을 전달하면 공통 400 응답을 한다.")
    void pathVariableTypeMismatchReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/numeric-id/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(400))
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("제약 위반 예외의 속성 경로와 메시지를 필드 오류 목록으로 반환한다.")
    void constraintViolationIncludesPropertyPathAndMessage() throws Exception {
        mockMvc.perform(get("/test/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(400))
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].field").value("name"))
                .andExpect(jsonPath("$.data[0].message").value("이름은 필수입니다."));
    }

    @Test
    @DisplayName("Spring 잠금 실패 예외는 공통 429 응답으로 변환한다.")
    void pessimisticLockingFailureReturnsTooManyRequests() throws Exception {
        mockMvc.perform(get("/test/pessimistic-locking-failure"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(429))
                .andExpect(jsonPath("$.message").value("현재 다른 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("JPA 잠금 시간 초과 예외는 공통 429 응답으로 변환한다.")
    void lockTimeoutReturnsTooManyRequests() throws Exception {
        mockMvc.perform(get("/test/lock-timeout"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value(429))
                .andExpect(jsonPath("$.message").value("현재 다른 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("데이터 없는 성공 응답은 data 필드를 명시적 null로 반환한다.")
    void successResponsePreservesExplicitNullData() throws Exception {
        mockMvc.perform(get("/test/success-without-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value(200))
                .andExpect(jsonPath("$.message").value("처리되었습니다."))
                .andExpect(jsonPath("$.data").hasJsonPath())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("명시적으로 null을 지정한 예외 메시지는 기본 메시지로 대체하지 않는다.")
    void businessExceptionPreservesExplicitNullMessage() throws Exception {
        mockMvc.perform(get("/test/business-exception-null-message"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value(404))
                .andExpect(jsonPath("$.message").hasJsonPath())
                .andExpect(jsonPath("$.message").value(nullValue()))
                .andExpect(jsonPath("$.data").hasJsonPath())
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("기본 메시지 없는 필드 오류를 null 메시지로 변환하고 직렬화한다.")
    void fieldErrorPreservesMissingDefaultMessage() {
        FieldErrorResponse response = FieldErrorResponse.from(new FieldError("request", "name", null));
        var json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("field").asText()).isEqualTo("name");
        assertThat(json.has("message")).isTrue();
        assertThat(json.get("message").isNull()).isTrue();
    }

    // 테스트용 컨트롤러
    @RestController
    static class ExceptionTestController {

        @Autowired
        private Validator validator;

        @GetMapping("/test/success-with-data")
        ResponseEntity<ApiResponse<String>> successWithData() {
            ApiResponse<String> response = ApiResponse.success(
                    HttpStatus.CREATED, "데이터를 생성하였습니다.", "Hello, Budzet!");
            return ResponseEntity.status(response.getResultCode()).body(response);
        }

        @GetMapping("/test/success-without-data")
        ApiResponse<Void> successWithoutData() {
            return ApiResponse.success(HttpStatus.OK, "처리되었습니다.", null);
        }

        @GetMapping("/test/business-exception-null-message")
        void throwBusinessExceptionWithNullMessage() {
            throw new BusinessException(ErrorCode.NOT_FOUND, null);
        }

        @GetMapping("/test/numeric-id/{id}")
        void numericId(@PathVariable Long id) {
        }

        @GetMapping("/test/constraint-violation")
        void throwConstraintViolation() {
            // 실제 Bean Validation 결과로 공통 예외 처리의 변환을 검증한다.
            throw new ConstraintViolationException(validator.validate(new ValidationRequest("")));
        }

        @GetMapping("/test/pessimistic-locking-failure")
        void throwPessimisticLockingFailure() {
            throw new PessimisticLockingFailureException("테스트 잠금 실패");
        }

        @GetMapping("/test/lock-timeout")
        void throwLockTimeout() {
            throw new LockTimeoutException("테스트 잠금 시간 초과");
        }

        // BusinessException 기본 메시지로 발생시키기
        @GetMapping("/test/business-exception-default-message")
        void throwBusinessException() {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // BusinessException 커스텀 메시지로 발생시키기
        @GetMapping("/test/business-exception-custom-message")
        void throwBusinessExceptionWithCustomMessage() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "테스트 리소스를 찾을 수 없습니다.");
        }

        // @Valid 에러 발생시키기
        @PostMapping("/test/validation")
        void validate(@Valid @RequestBody ValidationRequest request) {
        }

        record ValidationRequest(
                @NotBlank(message = "이름은 필수입니다.")
                String name
        ) {
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            ExceptionTestController.class,
            GlobalExceptionHandler.class
    })
    static class TestApplication {
    }
}
