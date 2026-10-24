package br.com.eduardoreche.insureflow.adapter.in.web;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Insureflow")
                .version("v1")
                .description("""
                        API fictícia de apólices de seguro patrimonial, feita para um desafio técnico. \
                        A API administrativa é uma extensão interna sem equivalente no Open Insurance. \
                        A API de consulta é inspirada na InsurancePatrimonial, sem certificação nem \
                        aderência regulatória. Todos os dados são fictícios."""));
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Administrativa")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    GroupedOpenApi openInsuranceApi() {
        return GroupedOpenApi.builder()
                .group("open-insurance")
                .displayName("Open Insurance (consulta)")
                .pathsToMatch("/insurance-patrimonial/**")
                .build();
    }

    // Todo erro sai no envelope do ApiExceptionHandler; declarar aqui evita repetir o schema em cada @ApiResponse.
    @Bean
    GlobalOpenApiCustomizer errorEnvelope() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.components(new Components());
            }
            ModelConverters.getInstance().readAll(ErrorResponseDTO.class).forEach(openApi.getComponents()::addSchemas);
            openApi.getPaths().values().stream()
                    .flatMap(path -> path.readOperations().stream())
                    .flatMap(operation -> operation.getResponses().entrySet().stream())
                    .filter(response -> response.getKey().startsWith("4") || response.getKey().startsWith("5"))
                    .forEach(response -> response.getValue().setContent(errorContent()));
        };
    }

    @Bean
    GlobalOpenApiCustomizer interactionIdHeader() {
        return openApi -> openApi.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream())
                .forEach(operation -> {
                    operation.addParametersItem(new HeaderParameter()
                            .name(InteractionIdFilter.HEADER)
                            .description("UUID que correlaciona a chamada. Se ausente, a API gera um.")
                            .schema(new StringSchema().format("uuid")));
                    ApiResponse invalid = operation.getResponses().computeIfAbsent("400",
                            code -> new ApiResponse().content(errorContent()));
                    invalid.description(invalid.getDescription() == null
                            ? "x-fapi-interaction-id inválido (INVALID_INTERACTION_ID)."
                            : invalid.getDescription() + " Também para x-fapi-interaction-id inválido.");
                    operation.getResponses().values().forEach(response -> response.addHeaderObject(
                            InteractionIdFilter.HEADER, new Header()
                                    .description("O valor recebido ou, se ausente ou rejeitado, o gerado pela API.")
                                    .schema(new StringSchema().format("uuid"))));
                });
    }

    private static Content errorContent() {
        return new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponseDTO")));
    }
}
