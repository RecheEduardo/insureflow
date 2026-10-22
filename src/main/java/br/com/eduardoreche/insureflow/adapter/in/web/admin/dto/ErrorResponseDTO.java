package br.com.eduardoreche.insureflow.adapter.in.web.admin.dto;

import java.util.List;

public record ErrorResponseDTO(List<ErrorDTO> errors) {

    public record ErrorDTO(String code, String title, String detail, String requestDateTime) {
    }
}
