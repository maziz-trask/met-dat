package cz.metdat.storage;

import cz.metdat.domain.Attachment;
import cz.metdat.repository.AttachmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** At start-up, (re)creates the synthetic placeholder file for every seeded attachment row whose bytes are missing. Idempotent. */
@Component
public class DemoAttachmentInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoAttachmentInitializer.class);
    private final AttachmentRepository attachments;
    private final FileStorage storage;

    public DemoAttachmentInitializer(AttachmentRepository attachments, FileStorage storage) {
        this.attachments = attachments;
        this.storage = storage;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        int created = 0;
        for (Attachment a : attachments.findBySyntheticTrue()) {
            if (storage.exists(a.getStorageLocation())) continue;
            byte[] data = SyntheticFiles.forFile(a.getOriginalFilename(), a.getTechnicalTest().getTestNumber());
            storage.store(a.getStorageLocation(), data);
            a.setFileSize(data.length);
            created++;
        }
        if (created > 0) log.info("Generated {} synthetic demo attachment files", created);
    }
}
