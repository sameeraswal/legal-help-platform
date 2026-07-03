package com.legalhelp.common.llm;

import com.legalhelp.common.exception.ApiException;
import com.legalhelp.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class LlmException extends ApiException {
    public LlmException(String message) {
        super(ErrorCode.LLM_ERROR, HttpStatus.BAD_GATEWAY, message);
    }
}
