package com.github.devnic0las.rag;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/documents")
public class DocumentController {

  private final IngestionService service;

  public DocumentController(IngestionService service) {
    this.service = service;
  }

  @PostMapping
  public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
    return Map.of("chunks", service.ingest(file));
  }
}
