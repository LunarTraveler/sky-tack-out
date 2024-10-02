package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("admin/common")
@Slf4j
@RequiredArgsConstructor
@Api(tags = "通用接口")
public class CommonController {

    private final AliOssUtil aliOssUtil;

    /**
     * 上传文件
     * @param file
     * @return
     */
    @ApiOperation("上传文件")
    @PostMapping("upload")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传 {}", file.getOriginalFilename());

        try {
            String originalFilename = file.getOriginalFilename();
            int lastIndex = originalFilename.lastIndexOf(".");
            // 为了防止名称一致而导致的文件覆盖
            String objectName = UUID.randomUUID() + originalFilename.substring(lastIndex);
            // 返回给前端的图片访问地址
            String fileUrl = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(fileUrl);
        } catch (IOException e) {
            log.error("文件上传失败 {}", e.getMessage());
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

}
