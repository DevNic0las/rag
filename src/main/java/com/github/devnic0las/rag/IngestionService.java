package com.github.devnic0las.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngestionService {

  private final VectorStore vectorStore;

  public int ingest(MultipartFile file) throws IOException {
    Resource resource = new ByteArrayResource(file.getBytes());

    List<Document> docs = new TikaDocumentReader(resource).get();

    TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(500)
            .build();

    List<Document> chunks = splitter.apply(docs);

    chunks.forEach(c -> c.getMetadata().put("source", file.getOriginalFilename()));

    vectorStore.add(chunks);
    return chunks.size();
  }
}