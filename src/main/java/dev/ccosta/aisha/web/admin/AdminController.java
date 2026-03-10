package dev.ccosta.aisha.web.admin;

import dev.ccosta.aisha.application.entry.EntryCategoryModelManager;
import dev.ccosta.aisha.application.entry.EntryCategoryModelTrainingCoordinator;
import dev.ccosta.aisha.application.entry.EntryCategoryModelTrainingTrigger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Renders the first-level administration area and exposes operational actions for system configuration screens.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final EntryCategoryModelManager entryCategoryModelManager;
    private final EntryCategoryModelTrainingCoordinator entryCategoryModelTrainingCoordinator;

    public AdminController(
        EntryCategoryModelManager entryCategoryModelManager,
        EntryCategoryModelTrainingCoordinator entryCategoryModelTrainingCoordinator
    ) {
        this.entryCategoryModelManager = entryCategoryModelManager;
        this.entryCategoryModelTrainingCoordinator = entryCategoryModelTrainingCoordinator;
    }

    /**
     * Displays the administration landing page with the currently available operational sections.
     *
     * @return the administration page template
     */
    @GetMapping
    public String index() {
        return "admin/index";
    }

    /**
     * Displays the category model administration page with the current training status.
     *
     * @param model the UI model used to render the model administration page
     * @return the category model administration page template
     */
    @GetMapping("/models")
    public String models(
        @RequestParam(name = "manualTrainingRequested", defaultValue = "false") boolean manualTrainingRequested,
        Model model
    ) {
        fillCategoryModelStatus(model, manualTrainingRequested);
        return "admin/models";
    }

    /**
     * Refreshes the category suggestion model status card used by the administration screen.
     *
     * @param model the UI model used to render the status fragment
     * @return the status fragment for HTMX refreshes
     */
    @GetMapping("/fragments/category-model-status")
    public String categoryModelStatus(Model model) {
        fillCategoryModelStatus(model, false);
        return "admin/models :: categoryModelStatus";
    }

    /**
     * Requests a manual retraining cycle for the category suggestion model and returns the updated status fragment.
     *
     * @param model the UI model used to render the status fragment
     * @return the status fragment after scheduling retraining
     */
    @PostMapping("/category-model/retrain")
    public String retrainCategoryModel() {
        entryCategoryModelTrainingCoordinator.requestTraining(EntryCategoryModelTrainingTrigger.MANUAL);
        return "redirect:/admin/models?manualTrainingRequested=true";
    }

    private void fillCategoryModelStatus(Model model, boolean manualTrainingRequested) {
        model.addAttribute("categoryModelStatus", entryCategoryModelManager.status());
        model.addAttribute("manualTrainingRequested", manualTrainingRequested);
    }
}
