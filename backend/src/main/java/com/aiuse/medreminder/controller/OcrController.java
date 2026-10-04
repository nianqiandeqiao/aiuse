package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.common.BizException;
import com.aiuse.medreminder.dto.OcrBase64Request;
import com.aiuse.medreminder.service.BaiduOcrService;
import com.aiuse.medreminder.service.DrugDictionary;
import com.aiuse.medreminder.vo.OcrDrugResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * 拍照识药：图片 -> 百度 OCR 文字 -> 药品词典归一 -> 标准药名。
 */
@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    /** 百度要求图片不超过 4MB。 */
    private static final long MAX_IMAGE_BYTES = 4L * 1024 * 1024;

    private final BaiduOcrService ocrService;
    private final DrugDictionary drugDictionary;

    public OcrController(BaiduOcrService ocrService, DrugDictionary drugDictionary) {
        this.ocrService = ocrService;
        this.drugDictionary = drugDictionary;
    }

    /** 上传图片识别药名：multipart/form-data，字段名 file。 */
    @PostMapping("/drug")
    public ApiResponse<OcrDrugResult> recognizeDrug(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请上传药盒照片（字段名 file）");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw BizException.badRequest("读取上传文件失败: " + e.getMessage());
        }
        return ApiResponse.ok(recognize(bytes));
    }

    /** base64 方式识别：前端把图片转成 base64 字符串提交。 */
    @PostMapping("/base64")
    public ApiResponse<OcrDrugResult> recognizeBase64(@Valid @RequestBody OcrBase64Request request) {
        String base64 = request.imageBase64();
        int commaIndex = base64.indexOf(',');
        if (base64.startsWith("data:") && commaIndex > 0) {
            base64 = base64.substring(commaIndex + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw BizException.badRequest("imageBase64 不是合法的 base64 图片数据");
        }
        return ApiResponse.ok(recognize(bytes));
    }

    private OcrDrugResult recognize(byte[] imageBytes) {
        if (imageBytes.length == 0) {
            throw BizException.badRequest("图片内容为空");
        }
        if (imageBytes.length > MAX_IMAGE_BYTES) {
            throw BizException.badRequest("图片过大（" + (imageBytes.length / 1024)
                    + "KB），百度 OCR 要求不超过 4MB，请压缩后再上传");
        }
        List<String> lines = ocrService.recognizeLines(imageBytes);
        List<DrugDictionary.DrugHit> drugs = drugDictionary.match(String.join("\n", lines));
        return OcrDrugResult.of(lines, drugs);
    }
}

