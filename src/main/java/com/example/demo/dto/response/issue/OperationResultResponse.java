package com.example.demo.dto.response.issue;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResultResponse {

    private String operation;
    private Long resourceId;
    private Integer statusCode;
    private Object result;
}
