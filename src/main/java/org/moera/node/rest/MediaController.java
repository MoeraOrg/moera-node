package org.moera.node.rest;

import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.global.ApiController;
import org.moera.node.media.MediaPathNotSetException;
import org.moera.node.model.MediaFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/media")
public class MediaController {

    private static Logger log = LoggerFactory.getLogger(MediaController.class);

    @Inject
    private Config config;

    @PostConstruct
    public void init() throws Exception {
        if (StringUtils.isEmpty(config.getMedia().getPath())) {
            throw new MediaPathNotSetException("Path not set");
        }
        try {
            Path path = FileSystems.getDefault().getPath(config.getMedia().getPath());
            if (!Files.exists(path)) {
                throw new MediaPathNotSetException("Not found");
            }
            if (!Files.isDirectory(path)) {
                throw new MediaPathNotSetException("Not a directory");
            }
            if (!Files.isWritable(path)) {
                throw new MediaPathNotSetException("Not writable");
            }
            path = path.resolve("tmp");
            if (!Files.exists(path)) {
                try {
                    Files.createDirectory(path);
                } catch (FileAlreadyExistsException e) {
                    // ok
                } catch (Exception e) {
                    throw new MediaPathNotSetException("Cannot create tmp/ subdirectory: " + e.getMessage());
                }
            }
        } catch (InvalidPathException e) {
            throw new MediaPathNotSetException("Path is invalid");
        }
    }

    @PostMapping
    @Transactional
    public MediaFileInfo post(@RequestHeader("Content-Type") String contentType,
                              @RequestHeader(value = "Content-Length", required = false) Long contentLength,
                              InputStream in) {
        log.info("POST /media (Content-Type: {}, Content-Length: {})",
                LogUtil.format(contentType), LogUtil.format(contentLength));

        return new MediaFileInfo(); // TODO
    }

}
