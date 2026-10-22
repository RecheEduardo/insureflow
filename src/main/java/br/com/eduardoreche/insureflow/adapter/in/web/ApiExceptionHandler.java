package br.com.eduardoreche.insureflow.adapter.in.web;

import br.com.eduardoreche.insureflow.adapter.in.web.admin.IdempotencyKey;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.IfMatch;
import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.exception.DomainException;
import br.com.eduardoreche.insureflow.domain.exception.DuplicatePolicyException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyCancelledException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyVersionMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Map<String, String> DETAILS = Map.ofEntries(
            Map.entry("POLICY_NOT_FOUND", "Apólice não encontrada."),
            Map.entry("POLICY_DUPLICATE", "Já existe uma apólice com este policyId."),
            Map.entry("POLICY_CANCELLED", "Apólice cancelada não aceita endosso."),
            Map.entry("POLICY_VERSION_MISMATCH", "A apólice foi alterada desde a última leitura. Consulte-a novamente."),
            Map.entry("ENDORSEMENT_WITHOUT_CHANGES", "Informe ao menos um campo para alterar."),
            Map.entry("IDEMPOTENCY_KEY_REUSED", "Esta Idempotency-Key já foi usada com outro corpo de requisição."),
            Map.entry("ISSUANCE_IN_FUTURE", "A data de emissão não pode ser futura."),
            Map.entry("POLICY_WITHOUT_INSURED", "A apólice precisa de ao menos um segurado."),
            Map.entry("POLICY_WITHOUT_OBJECT", "A apólice precisa de ao menos um objeto segurado."),
            Map.entry("GROUP_CERTIFICATE_ID_MISMATCH", "groupCertificateId é obrigatório somente para CERTIFICADO."),
            Map.entry("OBJECT_DUPLICATED", "Objeto segurado repetido na apólice."),
            Map.entry("LMI_ABOVE_LMG", "O LMI de uma cobertura excede o LMG da apólice."),
            Map.entry("COVERAGE_OUTSIDE_TERM", "A vigência da cobertura deve estar dentro da vigência da apólice."),
            Map.entry("INSURED_BIRTH_DATE_REQUIRED", "A data de nascimento é obrigatória para este tipo de segurado."),
            Map.entry("OBJECT_WITHOUT_COVERAGE", "O objeto segurado precisa de ao menos uma cobertura."),
            Map.entry("OBJECT_WITHOUT_MAIN_COVERAGE", "O objeto segurado precisa de ao menos uma cobertura principal."),
            Map.entry("COVERAGE_DUPLICATED", "Cobertura repetida no objeto segurado."),
            Map.entry("COVERAGE_DESCRIPTION_REQUIRED", "A descrição da cobertura é obrigatória para OUTRAS."),
            Map.entry("TERM_INVALID", "O fim da vigência não pode ser anterior ao início."),
            Map.entry("MONEY_NEGATIVE", "O valor monetário não pode ser negativo."),
            Map.entry("MONEY_SCALE", "O valor monetário aceita no máximo duas casas decimais."),
            Map.entry("MONEY_CURRENCY_MISMATCH", "Moedas diferentes não podem ser comparadas."));

    private final Clock clock;

    ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponseDTO> domain(DomainException e) {
        HttpStatus status = switch (e) {
            case PolicyNotFoundException _ -> HttpStatus.NOT_FOUND;
            case PolicyCancelledException _, DuplicatePolicyException _ -> HttpStatus.CONFLICT;
            case PolicyVersionMismatchException _ -> HttpStatus.PRECONDITION_FAILED;
            case BusinessRuleViolationException _ -> HttpStatus.UNPROCESSABLE_CONTENT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return respond(status, e.getCode(), DETAILS.getOrDefault(e.getCode(), "Regra de negócio violada."));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponseDTO> concurrentUpdate() {
        return respond(HttpStatus.PRECONDITION_FAILED, "POLICY_VERSION_MISMATCH",
                DETAILS.get("POLICY_VERSION_MISMATCH"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ErrorResponseDTO> missingHeader(MissingRequestHeaderException e) {
        if ("If-Match".equalsIgnoreCase(e.getHeaderName())) {
            return respond(HttpStatus.PRECONDITION_REQUIRED, "IF_MATCH_REQUIRED",
                    "Informe o cabeçalho If-Match com o ETag da apólice.");
        }
        if ("Idempotency-Key".equalsIgnoreCase(e.getHeaderName())) {
            return respond(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
                    "Informe o cabeçalho Idempotency-Key com um identificador único da emissão.");
        }
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Cabeçalho obrigatório ausente: " + e.getHeaderName());
    }

    @ExceptionHandler(IfMatch.InvalidIfMatchException.class)
    ResponseEntity<ErrorResponseDTO> invalidIfMatch() {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_IF_MATCH", "O If-Match deve conter o ETag devolvido pela apólice.");
    }

    @ExceptionHandler(IdempotencyKey.InvalidIdempotencyKeyException.class)
    ResponseEntity<ErrorResponseDTO> invalidIdempotencyKey() {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                "A Idempotency-Key deve ter de 1 a 255 caracteres ASCII visíveis.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponseDTO> invalidBody(MethodArgumentNotValidException e) {
        String fields = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField())
                .distinct()
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("corpo");
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Campos inválidos: " + fields + ".");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponseDTO> unreadableBody() {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Corpo inválido: JSON malformado, campo desconhecido ou valor fora do formato.");
    }

    private ResponseEntity<ErrorResponseDTO> respond(HttpStatus status, String code, String detail) {
        String requestDateTime = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS).toString();
        ErrorResponseDTO.ErrorDTO error = new ErrorResponseDTO.ErrorDTO(code, title(status), detail, requestDateTime);
        return ResponseEntity.status(status).body(new ErrorResponseDTO(List.of(error)));
    }

    private static String title(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso não encontrado";
            case CONFLICT -> "Conflito";
            case PRECONDITION_FAILED -> "Pré-condição falhou";
            case PRECONDITION_REQUIRED -> "Pré-condição obrigatória";
            case UNPROCESSABLE_CONTENT -> "Regra de negócio violada";
            case BAD_REQUEST -> "Requisição inválida";
            default -> "Erro interno";
        };
    }
}
