package dev.ccosta.aisha.web.admin;

import dev.ccosta.aisha.application.backup.SystemBackupCoordinator;
import dev.ccosta.aisha.application.backup.SystemBackupSnapshot;
import dev.ccosta.aisha.security.AishaPrincipal;
import java.nio.file.Files;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Handles the administration screen for asynchronous system backup operations.
 */
@Controller
@RequestMapping("/admin/system-backup")
public class SystemBackupController {

    private final SystemBackupCoordinator backupCoordinator;

    public SystemBackupController(SystemBackupCoordinator backupCoordinator) {
        this.backupCoordinator = backupCoordinator;
    }

    /**
     * Displays the system backup administration page.
     *
     * @param backupRequested whether the current request follows a backup scheduling action
     * @param model the UI model used to render the page
     * @return the system backup template
     */
    @GetMapping
    public String index(
        @org.springframework.web.bind.annotation.RequestParam(name = "backupRequested", defaultValue = "false") boolean backupRequested,
        Model model
    ) {
        fillBackupStatus(model, backupRequested);
        return "admin/system-backup";
    }

    /**
     * Starts a system backup and redirects back to the status page.
     *
     * @param principal authenticated user requesting the backup
     * @return redirect to the backup status page
     */
    @PostMapping("/jobs")
    public String startBackup(@AuthenticationPrincipal AishaPrincipal principal) {
        String requestedBy = principal != null ? principal.getUsername() : "unknown";
        backupCoordinator.startBackup(requestedBy);
        return "redirect:/admin/system-backup?backupRequested=true";
    }

    /**
     * Refreshes the status card used by the backup screen.
     *
     * @param model the UI model used to render the status fragment
     * @return Thymeleaf fragment with current backup status
     */
    @GetMapping("/fragments/status")
    public String status(Model model) {
        fillBackupStatus(model, false);
        return "admin/system-backup :: systemBackupStatus";
    }

    /**
     * Downloads the latest generated backup archive when it is available.
     *
     * @return zip archive response or a not-found response when no backup exists
     */
    @GetMapping("/download")
    public ResponseEntity<FileSystemResource> download() {
        SystemBackupSnapshot snapshot = backupCoordinator.currentSnapshot();
        if (!snapshot.downloadable() || !Files.exists(snapshot.backupFile())) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(snapshot.backupFile());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(snapshot.backupFilename()).build().toString()
            )
            .body(resource);
    }

    private void fillBackupStatus(Model model, boolean backupRequested) {
        model.addAttribute("backupSnapshot", backupCoordinator.currentSnapshot());
        model.addAttribute("backupRequested", backupRequested);
    }
}
