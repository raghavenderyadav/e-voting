package uk.dsxt.voting.common.datamodel;

import lombok.Getter;

public enum AnswerType {
    FOR("1"),
    AGAINST("2"),
    ABSTAIN("3");

    @Getter
    private final String code;

    private AnswerType(String code) {
        this.code=code;
    }

    public static AnswerType getType(String code) {
        for (AnswerType type: AnswerType.values()) {
            if (type.getCode().equals(code))
                return type;
        }
        return null;
    }
}
