package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.model.enums.StreetType;
import br.com.eduardoreche.insureflow.domain.model.enums.Uf;

import java.util.Objects;

public record Address(
        StreetType streetType,
        String streetName,
        String number,
        String complement,
        String district,
        String townName,
        Uf state,
        String postCode) {

    public Address {
        Objects.requireNonNull(streetType, "streetType");
        Objects.requireNonNull(streetName, "streetName");
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(townName, "townName");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(postCode, "postCode");
    }
}
