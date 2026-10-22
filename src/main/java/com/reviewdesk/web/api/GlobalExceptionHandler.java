package com.reviewdesk.web.api;

import com.reviewdesk.domain.StaleReviewException;
import com.reviewdesk.service.ReviewNotFoundException;
import com.reviewdesk.service.ReviewVersionNotFoundException;
import com.reviewdesk.service.SubmissionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将审核业务异常转换为稳定的 REST 错误响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StaleReviewException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError staleReview(StaleReviewException exception) {
        return new ApiError("STALE_REVISION", exception.getMessage(),
                exception.getCurrentRevision());
    }

    @ExceptionHandler({
            SubmissionNotFoundException.class,
            ReviewNotFoundException.class,
            ReviewVersionNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(RuntimeException exception) {
        return ApiError.of("NOT_FOUND", exception.getMessage());
    }
}

