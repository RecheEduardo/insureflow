package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy;

final class PolicyExamples {

    static final String ISSUE = """
            {
              "policyId": "AP-2026-000123",
              "documentType": "APOLICE_INDIVIDUAL",
              "issuanceType": "EMISSAO_PROPRIA",
              "issuanceDate": "2026-10-01",
              "termStartDate": "2026-10-01",
              "termEndDate": "2027-09-30",
              "maxLMG": "200000.00",
              "proposalId": "PROP-2026-000456",
              "productName": "Residencial Essencial",
              "insureds": [{
                "identification": "12345678901",
                "identificationType": "CPF",
                "name": "Maria Ficticia",
                "birthDate": "1990-05-17",
                "email": "maria@example.com",
                "address": {
                  "streetType": "RUA",
                  "streetName": "Rua Exemplo",
                  "number": "100",
                  "townName": "Sao Paulo",
                  "state": "SP",
                  "postCode": "01310100"
                }
              }],
              "insuredObjects": [{
                "identification": "IMOVEL-1",
                "propertyType": "CASA",
                "description": "Casa residencial",
                "postCode": "01310100",
                "coverages": [{
                  "branch": "0118",
                  "code": "INCENDIO",
                  "susepProcessNumber": "15414.900000/2020-11",
                  "lmi": "100000.00",
                  "termStartDate": "2026-10-01",
                  "termEndDate": "2027-09-30",
                  "mainCoverage": true,
                  "feature": "MASSIFICADOS",
                  "type": "REGULAR_COMUM",
                  "premiumPeriodicity": "ANUAL"
                }]
              }]
            }
            """;

    static final String ENDORSE = """
            {
              "productName": "Residencial Plus",
              "maxLMG": "250000.00"
            }
            """;

    static final String CANCEL = """
            {
              "reason": "PEDIDO_DO_SEGURADO"
            }
            """;

    private PolicyExamples() {
    }
}
