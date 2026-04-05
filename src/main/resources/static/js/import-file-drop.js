document.addEventListener("DOMContentLoaded", () => {
    function syncConditionalInput(selectId, inputId) {
        const select = document.getElementById(selectId);
        const input = document.getElementById(inputId);
        if (!(select instanceof HTMLSelectElement) || !(input instanceof HTMLInputElement)) {
            return;
        }

        const toggleVisibility = () => {
            const shouldShow = select.value === "OTHER";
            input.hidden = !shouldShow;
        };

        toggleVisibility();
        select.addEventListener("change", toggleVisibility);
    }

    syncConditionalInput("separator-option", "separator-other");
    syncConditionalInput("date-format-option", "date-format-other");
    syncConditionalInput("amount-format-option", "amount-format-other");

    const dropPage = document.querySelector("[data-import-drop-input]");
    if (!dropPage) {
        return;
    }

    const inputId = dropPage.getAttribute("data-import-drop-input");
    const fileInput = inputId ? document.getElementById(inputId) : null;
    if (!(fileInput instanceof HTMLInputElement) || fileInput.type !== "file") {
        return;
    }

    let dragDepth = 0;

    function isFileDrag(event) {
        return Array.from(event.dataTransfer?.types ?? []).includes("Files");
    }

    function setDragging(isDragging) {
        document.body.classList.toggle("import-drag-active", isDragging);
    }

    function assignFiles(files) {
        if (!files || files.length === 0) {
            return;
        }

        const dataTransfer = new DataTransfer();
        Array.from(files).forEach((file) => dataTransfer.items.add(file));
        fileInput.files = dataTransfer.files;
        fileInput.dispatchEvent(new Event("change", { bubbles: true }));
    }

    window.addEventListener("dragenter", (event) => {
        if (!isFileDrag(event)) {
            return;
        }

        event.preventDefault();
        dragDepth += 1;
        setDragging(true);
    });

    window.addEventListener("dragover", (event) => {
        if (!isFileDrag(event)) {
            return;
        }

        event.preventDefault();
        if (event.dataTransfer) {
            event.dataTransfer.dropEffect = "copy";
        }
        setDragging(true);
    });

    window.addEventListener("dragleave", (event) => {
        if (!isFileDrag(event)) {
            return;
        }

        event.preventDefault();
        dragDepth = Math.max(0, dragDepth - 1);
        if (dragDepth === 0) {
            setDragging(false);
        }
    });

    window.addEventListener("drop", (event) => {
        if (!isFileDrag(event)) {
            return;
        }

        event.preventDefault();
        dragDepth = 0;
        setDragging(false);
        assignFiles(event.dataTransfer?.files);
    });
});
