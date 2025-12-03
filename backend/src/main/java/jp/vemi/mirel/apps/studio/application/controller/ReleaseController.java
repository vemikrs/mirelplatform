/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application.controller;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuRelease;
import jp.vemi.mirel.apps.studio.domain.service.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/studio/releases")
public class ReleaseController {

    @Autowired
    private ReleaseService releaseService;

    @GetMapping("/{modelId}")
    public List<StuRelease> getReleases(@PathVariable String modelId) {
        return releaseService.getReleases(modelId);
    }

    @PostMapping
    public StuRelease createRelease(@RequestBody Map<String, String> body) {
        String modelId = body.get("modelId");
        return releaseService.createRelease(modelId);
    }
}
