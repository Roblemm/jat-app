package com.jat.app.controller;

import com.jat.app.dto.jobimport.JobImportPreviewRequest;
import com.jat.app.dto.jobimport.JobImportPreviewResponse;
import com.jat.app.service.JobImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-imports")
@RequiredArgsConstructor
public class JobImportController {

    // Import endpoints create previews only; confirmed opportunities are saved through JobOpportunityController.
    private final JobImportService jobImportService;

    // Preview lets users correct extracted fields before anything is persisted.
    @PostMapping("/preview")
    public JobImportPreviewResponse preview(@Valid @RequestBody JobImportPreviewRequest request) {
        return jobImportService.preview(request);
    }
}
