package com.aiuse.medreminder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ElderCreateRequest(
        @NotBlank(message = "老人姓名不能为空")
        @Size(max = 50, message = "老人姓名不能超过50个字符")
        String name,

        @Min(value = 0, message = "年龄不能为负数")
        @Max(value = 130, message = "年龄不合理")
        Integer age,

        @Size(max = 10, message = "性别字段过长")
        String gender,

        @Size(max = 20, message = "电话不能超过20个字符")
        String phone,

        @Size(max = 255, message = "基础疾病不能超过255个字符")
        String chronicDisease,

        @Size(max = 500, message = "备注不能超过500个字符")
        String note) {
}

